/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package savant.plugin;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import savant.api.data.DataFormat;


/**
 * 
 * @author tarkvara
 */
public class Tool extends SavantPanelPlugin {
    private String command;
    private JLabel commandLine;
    private List<ToolArgument> args = new ArrayList<ToolArgument>();

    @Override
    public void init(JPanel panel) {
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        panel.setBackground(Color.MAGENTA);
        gbc.weightx = 1.0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        try {
            parseDescriptor();
            commandLine = new JLabel(command);
            commandLine.setFont(panel.getFont().deriveFont(Font.ITALIC));
            gbc.insets = new Insets(10, 10, 20, 10);
            panel.add(commandLine, gbc);
            for (ToolArgument a: args) {
                addArgumentToPanel(a, panel);
            }
            gbc.weighty = 1.0;
            gbc.fill = GridBagConstraints.BOTH;
            JPanel p = new JPanel();
            p.setBackground(Color.YELLOW);
            panel.add(p, gbc);
        } catch (Exception x) {
            panel.add(new JLabel(String.format("<html>Unable to load <i>%s</i><br>%s</html>", getDescriptor().getFile(), x)), gbc);
        }
    }
    
    /**
     * The tool's arguments are contained in the associated plugin.xml file.
     */
    private void parseDescriptor() throws XMLStreamException, FileNotFoundException {
        XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(new FileInputStream(getDescriptor().getFile()));
        do {
            switch (reader.next()) {
                case XMLStreamConstants.START_ELEMENT:
                    String elemName = reader.getLocalName().toUpperCase();
                    if (elemName.equals("TOOL")) {
                        command = reader.getElementText();
                    } else if (elemName.equals("ARG")) {
                        // There's lots of crud in the XML file; we're just interested in the <arg> elements.
                        args.add(new ToolArgument(reader));
                    }
                    break;
                case XMLStreamConstants.END_DOCUMENT:
                    reader.close();
                    reader = null;
                    break;
            }
        } while (reader != null);
    }
    
    private void addArgumentToPanel(ToolArgument arg, JPanel panel) {
        GridBagConstraints gbc = new GridBagConstraints();
        JCheckBox enablerCheck = null;
        if (!arg.required) {
            enablerCheck = new JCheckBox();
            panel.add(enablerCheck, gbc);
        }
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.EAST;
        JLabel nameLabel = new JLabel(arg.name);
        panel.add(nameLabel, gbc);
        
        gbc.gridx = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1.0;
        JComponent widget = null;
        switch (arg.type) {
            case FILE:
                gbc.fill = GridBagConstraints.HORIZONTAL;
                widget = new JTextField();
                break;
            case RANGE:
                gbc.fill = GridBagConstraints.HORIZONTAL;
                widget = new JTextField();
                break;
            case LIST:
                widget = new JComboBox(arg.choices);
                ((JComboBox)widget).setSelectedItem(arg.value);
                break;
            case SEQUENCE_TRACK:
                widget = new TrackCombo(DataFormat.SEQUENCE_FASTA);
                break;
            case ALIGNMENT_TRACK:
                widget = new TrackCombo(DataFormat.INTERVAL_BAM);
                break;
        }
        if (widget != null) {
            panel.add(widget, gbc);
            if (enablerCheck != null) {
                enablerCheck.addActionListener(new EnablerCheckListener(nameLabel, widget));
            }
        }
    }
    
    private class EnablerCheckListener implements ActionListener {
        final JComponent[] widgets;

        EnablerCheckListener(JComponent... widgets) {
            this.widgets = widgets;
            for (JComponent w: this.widgets) {
                w.setEnabled(false);
            }
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            boolean enabled = ((JCheckBox)ae.getSource()).isSelected();
            for (JComponent w: widgets) {
                w.setEnabled(enabled);
            }
        }
    }
    
    private class TrackCombo extends JComboBox {
        DataFormat format;

        TrackCombo(DataFormat df) {
            super(new String[] { "Foo", "Bar", "Baz" });
            format = df;
        }
    }
}
