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

import java.awt.*;
import java.awt.event.*;
import java.util.Collection;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;

import com.jidesoft.grid.*;
import com.jidesoft.plaf.UIDefaultsLookup;
import com.jidesoft.swing.NullJideButton;
import com.jidesoft.swing.NullLabel;
import com.jidesoft.swing.NullPanel;
import org.java.plugin.registry.PluginDescriptor;

import savant.controller.PluginController;

/**
 * Class which lets user select which plugins are available.
 *
 * @author mfiume, vwilliams
 */
public class PluginBrowser {
    private TableModel _programTableModel;

    public Component getPluginListPanel() {
        HierarchicalTable table = createTable();
        JScrollPane pane = new JScrollPane(table);
        pane.getViewport().setBackground(Color.WHITE);
        return pane;
    }

    // create property table
    private HierarchicalTable createTable() {
        _programTableModel = new ProgramTableModel();
        final HierarchicalTable table = new HierarchicalTable() {
            @Override
            public TableModel getStyleModel() {
                return _programTableModel; // designate it as the style model
            }
        };
        table.setModel(_programTableModel);
        table.setPreferredScrollableViewportSize(new Dimension(600, 400));
        table.setHierarchicalColumn(-1);
        table.setSingleExpansion(true);
        table.setName("Program Table");
        table.setShowGrid(false);
        table.setRowHeight(24);
        table.getTableHeader().setPreferredSize(new Dimension(0, 0));
        table.getColumnModel().getColumn(0).setPreferredWidth(500);
        table.getColumnModel().getColumn(1).setPreferredWidth(30);
        table.getColumnModel().getColumn(1).setMaxWidth(30);
        table.getColumnModel().getColumn(2).setPreferredWidth(150);
        table.getColumnModel().getColumn(2).setMaxWidth(150);
        table.getColumnModel().getColumn(0).setCellRenderer(new ProgramCellRenderer());
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setComponentFactory(new HierarchicalTableComponentFactory() {
            @Override
            public Component createChildComponent(HierarchicalTable table, Object value, int row) {
                if (value instanceof PluginStub) {
                    return new ProgramPanel((PluginStub) value);
                }
                return null;
            }

            @Override
            public void destroyChildComponent(HierarchicalTable table, Component component, int row) {
            }
        });
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                int row = table.getSelectedRow();
                if (row != -1) {
                    table.expandRow(row);
                }
            }
        });
        return table;
    }

    static class ProgramPanel extends JPanel {
        PluginStub program;

        public ProgramPanel(PluginStub program) {
            this.program = program;
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createEmptyBorder(2, 2, 3, 2));
            add(createTextPanel());
            add(createControlPanel(), BorderLayout.AFTER_LINE_ENDS);
            setBackground(UIDefaultsLookup.getColor("Table.selectionBackground"));
            setForeground(UIDefaultsLookup.getColor("Table.selectionForeground"));
        }

        private JComponent createTextPanel() {
            NullPanel panel = new NullPanel(new GridLayout(2, 1, 5, 0));

            panel.add(new NullLabel(PluginController.getInstance().getPluginName(program.id), null, JLabel.LEADING));
            //panel.add(new NullLabel(program.id, SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.PLUGIN), JLabel.LEADING));
            panel.add(new NullPanel());
            return panel;
        }

        private JComponent createControlPanel() {
            NullPanel panel = new NullPanel(new GridLayout(2, 2, 5, 0));
            Component add = panel.add(new NullLabel("Version:", NullLabel.TRAILING));
            NullJideButton versionButton = new NullJideButton(program.version);
            versionButton.setHorizontalAlignment(SwingConstants.TRAILING);
            versionButton.setButtonStyle(NullJideButton.HYPERLINK_STYLE);
            //versionButton.addActionListener(new ClickAction(program, "Version", versionButton));
            panel.add(versionButton);

            panel.add(new NullPanel());
            final JButton removeButton = new JButton("Uninstall");
            removeButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    removeButton.setEnabled(false);
                    PluginController.getInstance().queuePluginForUnInstallation(PluginController.getInstance().getPluginDescriptor(program.id));
                }
            });
            if (PluginController.getInstance().isPluginQueuedForDeletion(program.id)) {
                removeButton.setEnabled(false);
            }
            panel.add(removeButton);
            return panel;
        }
    }

    static class ClickAction implements ActionListener {
        PluginStub program;
        String buttonName;
        AbstractButton button;

        public ClickAction(PluginStub program, String buttonName, AbstractButton button) {
            this.program = program;
            this.buttonName = buttonName;
            this.button = button;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JOptionPane.showMessageDialog(button, "\"" + buttonName + "\" in Program \"" + program.id + "\" is clicked.");
        }
    }

    static class ProgramTableModel extends AbstractTableModel implements HierarchicalTableModel, StyleModel {

        public ProgramTableModel() {
            refreshPrograms();
        }

        @Override
        public int getRowCount() {
            return pluginStubs.length;
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            PluginStub program = pluginStubs[rowIndex];
            switch (columnIndex) {
                case 0:
                    return PluginController.getInstance().getPluginName(program.id);
                case 1:
                    return "";
                case 2:
                    return "";
            }
            return "";
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
            return pluginStubs[row];
        }

        @Override
        public boolean isCellStyleOn() {
            return true;
        }

        private final static CellStyle _cellStyle = new CellStyle();

        static {
            _cellStyle.setHorizontalAlignment(SwingConstants.TRAILING);
        }

        @Override
        public CellStyle getCellStyleAt(int rowIndex, int columnIndex) {
            if (columnIndex == 2) {
                return _cellStyle;
            }
            return null;
        }
    }

    static class FitScrollPane extends JScrollPane implements ComponentListener {
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
            getViewport().getView().addComponentListener(this);
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
        public void componentResized(ComponentEvent e) {
            setSize(getSize().width, getPreferredSize().height);
        }

        @Override
        public void componentMoved(ComponentEvent e) {
        }

        @Override
        public void componentShown(ComponentEvent e) {
        }

        @Override
        public void componentHidden(ComponentEvent e) {
        }

        @Override
        public Dimension getPreferredSize() {
            getViewport().setPreferredSize(getViewport().getView().getPreferredSize());
            return super.getPreferredSize();
        }
    }

    static class PluginStub {
        String version;
        String id;

        public PluginStub(String id, String version) {
            this.version = version;
            this.id = id;
        }
    }

    static PluginStub[] pluginStubs;

    private static void refreshPrograms() {
        Collection<PluginDescriptor> pds = PluginController.getInstance().getPluginDescriptors();
        pluginStubs = new PluginStub[pds.size()];
        int i = 0;
        for (PluginDescriptor pd: pds) {
            pluginStubs[i++] = new PluginStub(pd.getUniqueId(), pd.getVersion().toString());
        }
    }

    class ProgramCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value instanceof PluginStub) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, ((PluginStub) value).id, isSelected, hasFocus, row, column);
                
                /*
                label.setIcon(FileSystemView.getFileSystemView().getSystemIcon(
                        new File(
                            PluginController.getInstance().getPluginPath(
                                ((PluginStub) value).id)
                                )));
                 * 
                 */
                return label;
            }
            else {
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
        }
    }
}
