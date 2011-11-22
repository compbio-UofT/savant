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

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataListener;

import savant.api.adapter.TrackAdapter;
import savant.api.data.DataFormat;
import savant.api.event.LocationChangedEvent;
import savant.api.event.TrackEvent;
import savant.api.util.Listener;
import savant.api.util.TrackUtils;
import savant.controller.LocationController;

/**
 * The panel on which the tool's user interface is presented.
 *
 * @author tarkvara
 */
class ToolSettingsPanel extends JPanel {
    private final Tool tool;
    private JLabel commandLine;
    private ActionListener executeListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent ae) {
            tool.execute();
        }
    };

    ToolSettingsPanel(Tool t) {
        tool = t;
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weightx = 1.0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.gridy = 0;
        try {
            tool.parseDescriptor();
            commandLine = new JLabel();
            commandLine.setFont(new Font("Serif", Font.PLAIN, 14));
            commandLine.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), BorderFactory.createEmptyBorder(5, 5, 5, 5)));
            gbc.insets = new Insets(10, 10, 10, 10);
            add(commandLine, gbc);

            JButton executeButton = new JButton("Execute");
            executeButton.addActionListener(executeListener);
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.gridy = 1;
            add(executeButton, gbc);

            for (ToolArgument a: tool.arguments) {
                addArgumentToPanel(a, ++gbc.gridy);
            }
            gbc.gridx = 0;
            gbc.gridy++;
            gbc.weighty = 1.0;
            add(new JPanel(), gbc);

            tool.displayCommandLine(commandLine);

        } catch (Exception x) {
            add(new JLabel(String.format("<html>Unable to load <i>%s</i><br>%s</html>", tool.getDescriptor().getFile(), x)), gbc);
        }
    }

    private void addArgumentToPanel(ToolArgument arg, int row) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = row;
        JCheckBox enablerCheck = null;
        if (!arg.required) {
            enablerCheck = new JCheckBox();
            gbc.gridx = 0;
            add(enablerCheck, gbc);
        }
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.EAST;
        JLabel nameLabel = new JLabel(arg.name + ":");
        add(nameLabel, gbc);
        
        gbc.gridx = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1.0;
        JComponent widget = null;
        switch (arg.type) {
            case OUTPUT_FILE:
                gbc.fill = GridBagConstraints.HORIZONTAL;
                widget = new JTextField((String)arg.value);
                RangeUpdater ignored = new RangeUpdater(arg, (JTextField)widget);
                ((JTextField)widget).addActionListener(executeListener);
                break;
            case RANGE:
            case BARE_RANGE:
                gbc.fill = GridBagConstraints.HORIZONTAL;
                widget = new JTextField();
                LocationController.getInstance().addListener(new RangeUpdater(arg, (JTextField)widget));
                ((JTextField)widget).addActionListener(executeListener);
                break;
            case LIST:
                widget = new StringCombo(arg);
                break;
            case BAM_INPUT_FILE:
                widget = new TrackCombo(arg, DataFormat.INTERVAL_BAM);
                TrackUtils.addTrackListener((TrackCombo)widget);
                break;
            case FASTA_INPUT_FILE:
                widget = new TrackCombo(arg, DataFormat.SEQUENCE_FASTA);
                TrackUtils.addTrackListener((TrackCombo)widget);
                break;
        }
        if (widget != null) {
            add(widget, gbc);
            if (enablerCheck != null) {
                enablerCheck.addActionListener(new EnablerCheckListener(arg, nameLabel, widget));
            }
        }
    }
    
    private class EnablerCheckListener implements ActionListener {
        private final ToolArgument argument;
        private final JComponent[] widgets;

        EnablerCheckListener(ToolArgument arg, JComponent... widgets) {
            argument = arg;
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
            argument.enabled = enabled;
            tool.displayCommandLine(commandLine);
        }
    }

    /**
     * Combo-box which lets user select tracks of a particular type.
     */
    private class TrackCombo extends JComboBox implements Listener<TrackEvent> {
        private final ToolArgument argument;
        private final DataFormat format;

        TrackCombo(ToolArgument arg, DataFormat df) {
            argument = arg;
            format = df;
            handleEvent((TrackEvent)null);
        }

        @Override
        public final void handleEvent(TrackEvent event) {
            setModel(new TrackComboModel(TrackUtils.getTracks(format)));
            if (getItemCount() > 0) {
                setSelectedIndex(0);
            }
        }

        private class TrackComboModel implements ComboBoxModel {
            private TrackAdapter selection;
            private final TrackAdapter[] tracks;
            
            private TrackComboModel(TrackAdapter[] t) {
                tracks = t;
            }

            @Override
            public void setSelectedItem(Object o) {
                selection = (TrackAdapter)o;
                argument.value = selection;
                tool.displayCommandLine(commandLine);
            }

            @Override
            public Object getSelectedItem() {
                return selection;
            }

            @Override
            public int getSize() {
                return tracks.length;
            }

            @Override
            public Object getElementAt(int i) {
                return tracks[i];
            }

            @Override
            public void addListDataListener(ListDataListener ll) {
            }

            @Override
            public void removeListDataListener(ListDataListener ll) {
            }
        }
    }
    
    /**
     * Combo-box which lets user select between a number of string arguments.
     */
    private class StringCombo extends JComboBox {
        private final ToolArgument argument;

        private StringCombo(ToolArgument arg) {
            super(arg.choices);
            argument = arg;
            super.setSelectedItem(arg.value);
        }
        
        @Override
        public void setSelectedItem(Object o) {
            super.setSelectedItem(o);
            argument.value = (String)o;
            tool.displayCommandLine(commandLine);
        }
    }
    
    private class RangeUpdater implements Listener<LocationChangedEvent> {
        private final ToolArgument argument;
        private final JTextField field;

        RangeUpdater(ToolArgument arg, JTextField f) {
            argument = arg;
            field = f;
            field.getDocument().addDocumentListener(new DocumentListener() {

                @Override
                public void insertUpdate(DocumentEvent de) {
                    argument.value = field.getText();
                    tool.displayCommandLine(commandLine);
                }

                @Override
                public void removeUpdate(DocumentEvent de) {
                    argument.value = field.getText();
                    tool.displayCommandLine(commandLine);
                }

                @Override
                public void changedUpdate(DocumentEvent de) {
                    argument.value = field.getText();
                    tool.displayCommandLine(commandLine);
                }
            });
        }

        @Override
        public void handleEvent(LocationChangedEvent event) {
            field.setText(String.format("%s:%d-%d", event.getReference(), event.getRange().getFrom(), event.getRange().getTo()));
        }
    }
}
