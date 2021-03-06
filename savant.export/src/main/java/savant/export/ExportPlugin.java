/**
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package savant.export;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.adapter.BookmarkAdapter;
import savant.api.adapter.RangeAdapter;
import savant.api.adapter.TrackAdapter;
import savant.api.event.ExportEvent;
import savant.api.util.BookmarkUtils;
import savant.api.util.Listener;
import savant.api.util.NavigationUtils;
import savant.api.util.RangeUtils;
import savant.controller.FrameController;
import savant.plugin.SavantPanelPlugin;
import savant.util.swing.PathField;
import savant.view.swing.ExportImage;
import savant.view.swing.Frame;
import savant.view.swing.GraphPane;

/**
 * @author AndrewBrook
 */
public class ExportPlugin extends SavantPanelPlugin {
    private static final Log LOG = LogFactory.getLog(ExportPlugin.class);

    //interface
    private PathField pf;
    private JLabel outputLabel;

    //file string
    private String baseFolder = "";
    private String baseFile = "";
    private String indexFile = "";
    
    //export variables
    private int currentBookmark;
    private int numBookmarks;
    private int numFrames;
    private int framesDone;
    private BookmarkAdapter[] bookmarks;

    //these will be stored to return to later
    private String currentReference;
    private RangeAdapter currentRange;

    //private Thread exportThread;
    private JDialog progressDialog;
    private Thread exportThread;
    private JOptionPane progressPanel;
    private boolean exportCancelled = false;

    @Override
    public void init(JPanel canvas) {
        setupGUI(canvas);
    }

    @Override
    public String getTitle() {
        return "Export Plugin";
    }

    private void setupGUI(JPanel panel) {


        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc= new GridBagConstraints();

        //create padding
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        JPanel fill1 = new JPanel();
        fill1.setPreferredSize(new Dimension(10,10));
        panel.add(fill1, gbc);

        //create path chooser
        JLabel htmlLabel = new JLabel("<html>Choose folder to save files.<br>An html index file will be created here.</html>");
        gbc.gridwidth = 2;
        gbc.gridx = 1;
        gbc.gridy = 1;
        panel.add(htmlLabel, gbc);
        pf = new PathField(JFileChooser.OPEN_DIALOG, false, true);
        gbc.gridx = 1;
        gbc.gridy = 2;
        panel.add(pf, gbc);

        //create runExport button
        JButton runButton = new JButton("Run");
        runButton.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {               
                    exportThread = new Thread("Export Plugin") {
                    @Override
                        public void run() {
                            try {
                                runTool();
                            } catch (InterruptedException ex) {
                                //TODO: deal with exception?
                                LOG.error("Export interrupted.", ex);
                            }
                        }
                    };
                    exportThread.start();

                    //create progress dialog
                    Object[] options = {"Cancel"};
                    progressPanel = new JOptionPane("     Running Export: 1");
                    progressPanel.setOptions(options);
                    progressDialog = progressPanel.createDialog("Export in progress");
                    progressDialog.setVisible(true);
                    if (progressPanel.getValue().equals("Cancel")) {
                        exportCancelled = true;
                    }

            }
        });
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridx = 1;
        gbc.gridy = 3;
        panel.add(runButton, gbc);

        //create output label
        outputLabel = new JLabel();
        Font f = outputLabel.getFont();
        outputLabel.setFont(f.deriveFont(f.getStyle() ^ Font.BOLD));
        gbc.gridx = 2;
        gbc.gridy = 3;
        panel.add(outputLabel, gbc);

        //create padding
        JPanel fill2 = new JPanel();
        fill2.setPreferredSize(new Dimension(10,10));
        gbc.weightx = 1.0;
        gbc.gridwidth = 1;
        gbc.gridx = 2;
        gbc.gridy = 3;
        panel.add(fill2, gbc);

        JPanel fill3 = new JPanel();
        fill3.setPreferredSize(new Dimension(10,10));
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.gridwidth = 2;
        gbc.gridx = 0;
        gbc.gridy = 4;
        panel.add(fill3, gbc);
    }

    public void runTool() throws InterruptedException {

        //store current location
        currentReference = NavigationUtils.getCurrentReferenceName();
        currentRange = NavigationUtils.getCurrentRange();
    
        //output init
        baseFolder = pf.getPath();
        if (baseFolder.equals("")) {
            outputLabel.setText("     Please enter a filename for index.");
            return;
        }

        //create index file
        DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
        Date date = new Date();
        baseFile =  baseFolder + System.getProperty("file.separator") + dateFormat.format(date);
        indexFile = baseFile + ".html";
        boolean createdIndex = this.initIndex();
        if (!createdIndex) {
            endRun(false);
            return;
        }

        //init bookmarks
        bookmarks = BookmarkUtils.getBookmarks();
        if (bookmarks.length == 0 || FrameController.getInstance().getFrames().isEmpty()) {
            outputLabel.setText("     There is nothing to export.");
            return;
        }

        //store export info
        numFrames = FrameController.getInstance().getFrames().size();
        numBookmarks = bookmarks.length - 1;
        currentBookmark = 0;

        runExport();
        
    }

    private void cancelExport() {
        closeIndex();
        for(int i = 0; i < FrameController.getInstance().getFrames().size(); i++) {
            Frame f = FrameController.getInstance().getFrames().get(i);
            ((GraphPane)f.getGraphPane()).removeExportListeners();
        }
        endRun(false);
        exportCancelled = false;
    }


    /*
     * Update the progress of export
     */
    private void updateRunLabel() {
        String message = "     Running Export: " + (currentBookmark+1) + "/" + (numBookmarks+1);
        outputLabel.setText("Export in progress");
        if (progressPanel != null) {
            progressPanel.setMessage(message);
        }
    }

    private void runExport() {

        if (exportCancelled) {
            cancelExport();
            return;
        }

        updateRunLabel();

        framesDone = 0;

        final BookmarkAdapter bm = bookmarks[currentBookmark];
        final String reference = bm.getReference();
        RangeAdapter rangeAd = bm.getRange();
        final RangeAdapter range = RangeUtils.createRange(rangeAd.getFrom(), rangeAd.getTo());
        String annotation = bm.getAnnotation();

        //TODO: multiple tracks per frame??
        for(int i = 0; i < numFrames; i++) {
            final int index = i;
            Frame frame = FrameController.getInstance().getFrames().get(i);

            //track info
            TrackAdapter t = frame.getTracks()[0];
            final GraphPane gp = (GraphPane)frame.getGraphPane();

            Listener<ExportEvent> eel = new Listener<ExportEvent>() {

                @Override
                public void handleEvent(ExportEvent evt) {
                    if (evt.getRange().equals(range)) {
                        gp.removeExportListener(this);
                        nextBookmark();
                    }
                }
            };
            gp.addExportEventListener(eel);
            
        }
        NavigationUtils.navigateTo(reference, range);
    }

    /*
     * If another bookmark exists, begin exporting it
     */
    public void nextBookmark() {
        framesDone++;
        if (framesDone == numFrames) {
            createImage(currentBookmark, bookmarks[currentBookmark]);
            currentBookmark++;
            if (currentBookmark > numBookmarks) {
                closeIndex();
                return;
            }
            runExport();
        }
    }

    public void createImage(int increment, BookmarkAdapter bm) {

        String[] trackNames = new String[FrameController.getInstance().getFrames().size()];
        for(int i = 0; i < trackNames.length; i++) {
            trackNames[i] = FrameController.getInstance().getFrames().get(i).getTracks()[0].getName();
        }

        BufferedImage out = ExportImage.beginExport(trackNames, increment);

        //save image
        boolean success = save(out, increment, bm);
        endRun(success);
    }
    
    private boolean save(BufferedImage image, int increment, BookmarkAdapter bm) {
        String filename = this.baseFile + increment + ".png";

        File file = new File(filename);
        try {
            ImageIO.write(image, "PNG", new File(filename));
        } catch (IOException ex) {
            return false;
        }

        String reference = bm.getReference();
        //Range range = bm.getRange();
        RangeAdapter range = bm.getRange();
        String annotation = bm.getAnnotation();
        String text = reference + "   " + range + "   " + annotation;
        addImageToIndex(filename, text);

        return true;
    }

    private boolean initIndex() {
        new File(baseFolder).mkdirs();
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(this.indexFile, false));
            String s = "<html>";
            out.write(s);
            out.close();
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    private boolean closeIndex() {
        if (progressDialog != null) {
            progressDialog.setVisible(false);
        }
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(this.indexFile, true));
            String s = "</html>";
            out.write(s);
            out.close();
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    private boolean addImageToIndex(String filename, String text) {
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(this.indexFile, true));
            String s = "<a href=\"" + "file:///" + filename + "\">" + text + "</a><br>";
            out.write(s);
            out.close();
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    private void endRun(boolean success) {
        //Return browser to original location
        NavigationUtils.navigateTo(currentReference, currentRange);

        //reset output
        if (success) {
            outputLabel.setText("     Created file " + indexFile);
        } else if (exportCancelled) {
            outputLabel.setText("     Export cancelled");
        } else {
            outputLabel.setText("     Export failed. Make sure output path is correct.");
        }
    }

}
