/*
 *    Copyright 2010-2011 University of Toronto
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
package savant.view.dialog;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseWheelListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import com.jidesoft.grid.HierarchicalTable;
import com.jidesoft.grid.HierarchicalTableComponentFactory;
import com.jidesoft.grid.HierarchicalTableModel;

import savant.controller.PluginController;
import savant.plugin.PluginDescriptor;


/**
 * Class which lets user select which plugins are available.
 *
 * @author mfiume, vwilliams
 */
public class PluginBrowser {
    private TableModel tableModel;
    private PluginController pluginController = PluginController.getInstance();

    public Component getPluginListPanel() {
        tableModel = new ProgramTableModel();
        final HierarchicalTable table = new HierarchicalTable() {
            @Override
            public TableModel getStyleModel() {
                return tableModel; // designate it as the style model
            }
        };
        table.setModel(tableModel);
        table.setPreferredScrollableViewportSize(new Dimension(600, 400));
        table.setHierarchicalColumn(-1);
        table.setSingleExpansion(true);
        table.setName("Program Table");
        table.setShowGrid(false);
        table.setRowHeight(24);
        table.getTableHeader().setPreferredSize(new Dimension(0, 0));
        table.getColumnModel().getColumn(0).setPreferredWidth(500);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        HierarchicalTableComponentFactory factory = new HierarchicalTableComponentFactory() {
            @Override
            public Component createChildComponent(HierarchicalTable table, Object value, int row) {
                if (value instanceof PluginDescriptor) {
                    return new ProgramPanel((PluginDescriptor)value);
                }
                return null;
            }

            @Override
            public void destroyChildComponent(HierarchicalTable table, Component component, int row) {
            }
        };
        table.setComponentFactory(factory);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                int row = table.getSelectedRow();
                if (row != -1) {
                    table.expandRow(row);
                }
            }
        });
        JScrollPane pane = new JScrollPane(table);
        return pane;
    }

    /**
     * Panel which displays full information about a single plugin.
     */
    class ProgramPanel extends JPanel {
        PluginDescriptor program;

        public ProgramPanel(PluginDescriptor program) {
            this.program = program;
            setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.anchor = GridBagConstraints.WEST;
            gbc.insets = new Insets(3, 3, 3, 3);
            gbc.weightx = 1.0;
            gbc.weighty = 1.0;
            gbc.gridheight = 2;
            add(new JLabel(program.getName()), gbc);

            gbc.gridx = 1;
            gbc.gridy = 0;
            gbc.gridheight = 1;
            gbc.weightx = 0.0;
            add(new JLabel("Version: " + program.getVersion()), gbc);

            gbc.gridy = 1;
            add(new JLabel("Status: " + pluginController.getPluginStatus(program.getID())), gbc);

            JButton removeButton = new JButton("Uninstall");
            removeButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    ((JButton)e.getSource()).setEnabled(false);
                    pluginController.queuePluginForRemoval(ProgramPanel.this.program.getID());
                }
            });
            if (pluginController.isPluginQueuedForRemoval(program.getID())) {
                removeButton.setEnabled(false);
            }
            gbc.gridx = 2;
            gbc.gridy = 0;
            gbc.gridheight = 2;
            add(removeButton, gbc);
        }
    }

    class ProgramTableModel extends AbstractTableModel implements HierarchicalTableModel {
        final PluginDescriptor[] descriptors;

        public ProgramTableModel() {
            descriptors = pluginController.getDescriptors().toArray(new PluginDescriptor[0]);
        }

        @Override
        public int getRowCount() {
            return descriptors.length;
        }

        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return descriptors[rowIndex].getName();
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        @Override
        public boolean hasChild(int row) {
            return true;
        }

        @Override
        public boolean isExpandable(int row) {
            return true;
        }

        @Override
        public boolean isHierarchical(int row) {
            return false;
        }

        @Override
        public Object getChildValueAt(int row) {
            return descriptors[row];
        }
    }

    static class FitScrollPane extends JScrollPane {
        public FitScrollPane() {
            initScrollPane();
        }

        public FitScrollPane(Component view) {
            super(view);
            initScrollPane();
        }

        public FitScrollPane(Component view, int vsbPolicy, int hsbPolicy) {
            super(view, vsbPolicy, hsbPolicy);
            initScrollPane();
        }

        public FitScrollPane(int vsbPolicy, int hsbPolicy) {
            super(vsbPolicy, hsbPolicy);
            initScrollPane();
        }

        private void initScrollPane() {
            setBorder(BorderFactory.createLineBorder(Color.GRAY));
            setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
            getViewport().getView().addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    setSize(getSize().width, getPreferredSize().height);
                }
            });
            removeMouseWheelListeners();
        }

        // remove MouseWheelListener as there is no need for it in FitScrollPane.
        private void removeMouseWheelListeners() {
            MouseWheelListener[] listeners = getMouseWheelListeners();
            for (MouseWheelListener listener : listeners) {
                removeMouseWheelListener(listener);
            }
        }

        @Override
        public void updateUI() {
            super.updateUI();
            removeMouseWheelListeners();
        }

        @Override
        public Dimension getPreferredSize() {
            getViewport().setPreferredSize(getViewport().getView().getPreferredSize());
            return super.getPreferredSize();
        }
    }
}
