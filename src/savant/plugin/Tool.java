/*
 *    Copyright 2011 University of Toronto
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package savant.plugin;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.adapter.TrackAdapter;
import savant.api.data.DataFormat;
import savant.api.util.DialogUtils;
import savant.settings.DirectorySettings;
import savant.util.BackgroundWorker;
import savant.util.Bookmark;
import savant.util.MiscUtils;
import savant.util.export.TrackExporter;


/**
 * 
 * @author tarkvara
 */
public class Tool extends SavantPanelPlugin {
    private static final Log LOG = LogFactory.getLog(Tool.class);

    /** Portion of tool execution which is devoted to preparing files. */
    private static final double PREP_PORTION = 0.25;

    /** Portion of tool execution which is devoted to actual execution. */
    private static final double WORK_PORTION = 0.75;

    private String baseCommand;
    private Pattern progressRegex;
    private Pattern errorRegex;
    private JTextArea console;

    List<ToolArgument> arguments = new ArrayList<ToolArgument>();

    /**
     * Keep track of all files which have been downloaded and exported to a local file.
     * We look them up by the requested range.
     */
    private static Map<ExportedTrackKey, File> exportedFiles = new HashMap<ExportedTrackKey, File>();

    private JPanel mainPanel;
    
    // The wait panel
    private JProgressBar progressBar;
    private JLabel progressInfo;
    private JButton cancelButton;
    
    private Bookmark workingLoc;

    @Override
    public void init(JPanel panel) {
        mainPanel = panel;
        panel.setLayout(new CardLayout());
        
        panel.add(new ToolSettingsPanel(this), "Settings");
        
        JPanel waitCard = new JPanel();
        waitCard.setLayout(new GridBagLayout());

        // Left side filler.
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 100, 0, 100);
        waitCard.add(new JLabel(getDescriptor().getName()), gbc);
        
        progressBar = new JProgressBar();
        progressBar.setPreferredSize(new Dimension(240, progressBar.getPreferredSize().height));
        waitCard.add(progressBar, gbc);
        
        progressInfo = new JLabel();
        progressInfo.setAlignmentX(1.0f);
        Font f = progressInfo.getFont();
        f = f.deriveFont(f.getSize() - 2.0f);
        progressInfo.setFont(f);
        waitCard.add(progressInfo, gbc);
        
        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                showCard("Settings");
            }
        });
        gbc.fill = GridBagConstraints.NONE;
        waitCard.add(cancelButton, gbc);

        // Console output at the bottom.
        console = new JTextArea();
        console.setFont(f);
        console.setLineWrap(false);  
        console.setEditable(false);

        JScrollPane consolePane = new JScrollPane(console);
        consolePane.setPreferredSize(new Dimension(600, 200));
        gbc.weighty = 1.0;
        gbc.insets = new Insets(30, 5, 5, 5);
        gbc.fill = GridBagConstraints.BOTH;
        waitCard.add(consolePane, gbc);

        panel.add(waitCard, "Progress");
    }
    
    /**
     * The tool's arguments are contained in the associated plugin.xml file.
     */
    void parseDescriptor() throws XMLStreamException, FileNotFoundException {
        XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(new FileInputStream(getDescriptor().getFile()));
        do {
            switch (reader.next()) {
                case XMLStreamConstants.START_ELEMENT:
                    String elemName = reader.getLocalName().toLowerCase();
                    if (elemName.equals("tool")) {
                        baseCommand = reader.getElementText();
                    } else if (elemName.equals("arg")) {
                        // There's lots of crud in the XML file; we're just interested in the <arg> elements.
                        arguments.add(new ToolArgument(reader));
                    } else if (elemName.equals("progress")) {
                        progressRegex = Pattern.compile(reader.getElementText());
                    } else if (elemName.equals("error")) {
                        errorRegex = Pattern.compile(reader.getElementText());
                    }
                    break;
                case XMLStreamConstants.END_DOCUMENT:
                    reader.close();
                    reader = null;
                    break;
            }
        } while (reader != null);
    }
    
    void displayCommandLine(JLabel l) {
        String command = "<html>";
        command += baseCommand;
        for (ToolArgument a: arguments) {
            if (a.value == null) {
                if (a.required) {
                    command += "<font color=\"red\"> " + a.flag + "</font>";
                }
            } else if (a.enabled) {
                try {
                    command += " " + a.flag + " " + getStringValue(a);
                } catch (ParseException px) {
                    // An invalid range specification.
                    command += "<font color=\"red\"> " + a.flag + " " + a.value + "</font>";
                }
            }
        }
        command += "</html>";
        l.setText(command);
    }

    void checkCommandLine() {
        for (ToolArgument a: arguments) {
            if (a.value == null) {
                if (a.required) {
                    throw new IllegalArgumentException(String.format("Required argument %s (%s) does not have a value.", a.flag, a.name));
                }
            } else if (a.enabled) {
                switch (a.type) {
                    case RANGE:
                    case BARE_RANGE:
                        // We assume that a tool will only have a single RANGE argument.
                        // TODO: Handle empty range and just a chromosome.
                        try {
                            // This will set workingLoc, so we can assume it's already set in buildCommandLine
                            getStringValue(a);
                        } catch (ParseException px) {
                            throw new IllegalArgumentException(String.format("Unable to parse \"%s\" as a valid range.", a.value));
                        }
                        break;
                }
            }
        }
    }

    List<String> buildCommandLine() {
        List<String> commandLine = new ArrayList<String>();
        commandLine.addAll(Arrays.asList(baseCommand.split("\\s")));
        
        // If we're launching a .jar, we may want to look for it in the plugins directory.
        if (commandLine.get(0).equals("java")) {
            for (int i = 2; i < commandLine.size(); i++) {
                String arg = commandLine.get(i);
                if (arg.endsWith(".jar")) {
                    // If it's just a .jar name (i.e. no path slashes), assume it's in the plugins directory.
                    if (!arg.contains("/")) {
                        commandLine.set(i, new File(DirectorySettings.getPluginsDirectory(), arg).getAbsolutePath());
                    }
                    break;
                }
            }
        }

        for (ToolArgument a: arguments) {
            if (a.value == null) {
                if (a.required) {
                    throw new IllegalArgumentException(String.format("Required argument %s (%s) does not have a value.", a.flag, a.name));
                }
            } else if (a.enabled) {
                commandLine.add(a.flag);
                try {
                    switch (a.type) {
                        case BAM_INPUT_FILE:
                        case FASTA_INPUT_FILE:
                            // If the argument is a track, we want the local file which contains our data.
                            commandLine.add(exportedFiles.get(new ExportedTrackKey((TrackAdapter)a.value, workingLoc)).getAbsolutePath());
                            break;
                        default:
                            commandLine.add(getStringValue(a));
                            break;
                    }
                } catch (ParseException ignored) {
                    // Shouldn't happen because we've already successfully passed checkCommandLine.
                }
            }
        }
        return commandLine;
    }

        
    /**
     * Interpret this argument's value in a form suitable for appearing on a command line.
     */
    public String getStringValue(ToolArgument a) throws ParseException {
        switch (a.type) {
            case BAM_INPUT_FILE:
            case FASTA_INPUT_FILE:
                return MiscUtils.getNeatPathFromURI(((TrackAdapter)a.value).getDataSource().getURI());
            case RANGE:
                workingLoc = new Bookmark((String)a.value);
                return workingLoc.getLocationText();
            case BARE_RANGE:
                workingLoc = new Bookmark((String)a.value);
                return String.format("%s:%d-%d", MiscUtils.homogenizeSequence(workingLoc.getReference()), workingLoc.getFrom(), workingLoc.getTo());
        } 
        return a.value.toString();
    }

    /**
     * Get the local file which contains the data for the given argument.
     * @param t the track which is providing the data
     * @param loc string specifying ref and range to be processed
     * @param canUseDirectly for bam files, Savant uses them natively, so we may be able to use a local file directly
     */
    private File getLocalFile(TrackAdapter t, Bookmark loc, boolean canUseDirectly) throws IOException {
        ExportedTrackKey key = new ExportedTrackKey(t, loc);
        if (exportedFiles.containsKey(key)) {
            return exportedFiles.get(key);
        }
        
        // If the data source is a local bam file, we can just use it.
        if (canUseDirectly) {
            URI uri = t.getDataSource().getURI();
            if (uri.getScheme().equals("file")) {
                File f = new File(uri);
                exportedFiles.put(key, f);
                return f;
            }
        }
        
        // Track is remote.  We'll need to download it.
        File f = File.createTempFile("tool", getExportExtension(t.getDataFormat()), DirectorySettings.getCacheDirectory());
        TrackExporter.getExporter(t).exportRange(loc.getReference(), loc.getRange(), f);
        exportedFiles.put(key, f);
        return f;
    }

    /**
     * Given the type of a Savant track, determine the appropriate extension for the exported file.
     * @param df
     * @return 
     */
    private static String getExportExtension(DataFormat df) {
        switch (df) {
            case SEQUENCE_FASTA:
                return ".fa";
            case INTERVAL_BAM:
                return ".bam";
            default:
                return null;
        }
    }

    void execute() {
        // Before we do anything else, make sure all the required parameters have been specified.
        try {
            checkCommandLine();
            showCard("Progress");
            new ToolWorker().execute();
        } catch (IllegalArgumentException x) {
            DialogUtils.displayMessage(x.getMessage());
        }
    }
    
    private void showCard(String card) {
        ((CardLayout)mainPanel.getLayout()).show(mainPanel, card);
    }

    private class ExportedTrackKey {
        TrackAdapter track;
        Bookmark location;
        
        ExportedTrackKey(TrackAdapter t, Bookmark loc) {
            track = t;
            location = loc;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof ExportedTrackKey) {
                ExportedTrackKey that = (ExportedTrackKey)o;
                return track == that.track && location.equals(that.location);
            }
            return false;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 43 * hash + (this.track != null ? this.track.hashCode() : 0);
            hash = 43 * hash + (this.location != null ? this.location.hashCode() : 0);
            return hash;
        }
    }
    
    private class ToolWorker extends BackgroundWorker<File> {
        private String errorMessage;

        @Override
        protected void showProgress(double fraction) {
            progressBar.setValue((int)(fraction * 100.0));
        }

        @Override
        protected File doInBackground() throws Exception {
            showProgress(0.0);
            cancelButton.setText("Cancel");
            console.setText("");

            progressInfo.setText("Preparing input files…");
            prepareInputs();

            progressInfo.setText("Running tool…");
            runTool();

            progressInfo.setText("");
            return null;
        }

        @Override
        protected void showSuccess(File result) {
            cancelButton.setText("Done");
        }

        /**
         * The first stage of the process may involve copying the track data into
         * local files so that the tool can operate on it.
         */
        private void prepareInputs() throws ParseException, IOException {
            List<ToolArgument> fileArgs = new ArrayList<ToolArgument>();
            for (ToolArgument a: arguments) {
                if (a.enabled) {
                    switch (a.type) {
                        case BAM_INPUT_FILE:
                        case FASTA_INPUT_FILE:
                            // Add these to the list of files which need data.
                            fileArgs.add(a);
                            break;
                    }
                }
            }
            int i = 0;
            for (ToolArgument a: fileArgs) {
                getLocalFile((TrackAdapter)a.value, workingLoc, a.type == ToolArgument.Type.BAM_INPUT_FILE);
                showProgress(++i * PREP_PORTION / fileArgs.size());
            }
        }

        private void runTool() throws IOException {
            List<String> commandLine = buildCommandLine();
            ProcessBuilder builder = new ProcessBuilder(commandLine);
            builder.redirectErrorStream(true);
            Process proc = builder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                line += "\n";
                console.append(line);
                Matcher m = errorRegex.matcher(line);
                if (m.find()) {
                    errorMessage = m.group(1);
                    LOG.info("Retrieved error message \"" + errorMessage + "\".");
                } else {
                    m = progressRegex.matcher(line);
                    if (m.find()) {
                        String progress = m.group(1);
                        LOG.info("Retrieved progress message \"" + progress + "\".");
                    }
                }
            }
            // We're done.  We may have picked up an error message along the way.
            if (errorMessage != null) {
                throw new IOException(errorMessage);
            }
        }
    }
}
