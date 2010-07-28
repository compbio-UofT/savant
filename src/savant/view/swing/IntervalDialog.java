/*
 *    Copyright 2010 University of Toronto
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

package savant.view.swing;

import com.jidesoft.action.CommandBar;
import com.jidesoft.converter.*;
import com.jidesoft.grid.*;
import com.jidesoft.swing.JideSwingUtilities;
import com.jidesoft.swing.JideTitledBorder;
import com.jidesoft.swing.PartialEtchedBorder;
import com.jidesoft.swing.PartialSide;
import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import savant.view.swing.interval.BAMTrackRenderer;

/**
 *
 * @author AndrewBrook
 */
public class IntervalDialog extends JDialog {

    protected static final Color BACKGROUND1 = new Color(253, 253, 244);
    protected static final Color BACKGROUND2 = new Color(255, 255, 255);

    private PropertyTable _table;
    private PropertyPane _pane;
    private PropertyTableModel model;

    private static PropertyTable table;

    //private Savant parent;
    //private Container c;

    //private static DockingManager trackDockingManager;
    //private static Map<DockableFrame,Frame> dockFrameToFrameMap;

    private static Frame frame;
    private static ViewTrack viewTrack;

    private static BAMTrackRenderer btr;

   // private static Log log = LogFactory.getLog(BAMParametersDialog1.class);


    public IntervalDialog(){
        this.setPreferredSize(new Dimension(300,500));
        this.setMinimumSize(new Dimension(300,500));
        this.setModal(true);
        this.setTitle("Change Interval Parameters");
        Component panel = getDemoPanel();
        this.add(panel);
    }

    public void setFrame(Frame f){
        frame = f;
    }

    /*public Component getOptionsPanel() {

        System.out.println("getOptionsPanel");
        JPanel checkBoxPanel = new JPanel(new GridLayout(0, 1));
        JCheckBox paintMargin = new JCheckBox("Paint Margin");
        paintMargin.setSelected(true);
        paintMargin.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                _table.setPaintMarginBackground(e.getStateChange() == ItemEvent.SELECTED);
            }
        });
        JCheckBox paintMarginComponent = new JCheckBox("Paint the Selected Row Indicator");
        paintMarginComponent.setSelected(false);
        paintMarginComponent.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    _table.setMarginRenderer(new DefaultTableCellRenderer() {
                        @Override
                        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                            JLabel label = (JLabel) super.getTableCellRendererComponent(table, "", false, false, row, column);
                            label.setHorizontalAlignment(SwingConstants.CENTER);
                            label.setOpaque(false);
                            if (!((Property) value).hasChildren() && isSelected) {
                                label.setIcon(JideIconsFactory.getImageIcon(JideIconsFactory.Arrow.RIGHT));
                            }
                            else {
                                label.setIcon(null);
                            }
                            return label;
                        }
                    });
                }
                else {
                    _table.setMarginRenderer(null);
                }
            }
        });

        JCheckBox showDescriptionArea = new JCheckBox("Show Description Area");
        showDescriptionArea.setSelected(true);
        showDescriptionArea.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                _pane.setShowDescription(e.getStateChange() == ItemEvent.SELECTED);
            }
        });

        JCheckBox showToolBar = new JCheckBox("Show Tool Bar");
        showToolBar.setSelected(true);
        showToolBar.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                _pane.setShowToolBar(e.getStateChange() == ItemEvent.SELECTED);
            }
        });

        checkBoxPanel.add(paintMargin);
        checkBoxPanel.add(paintMarginComponent);
        //checkBoxPanel.add(showDescriptionArea);
        _pane.setShowDescription(false);
        checkBoxPanel.add(showToolBar);

        return checkBoxPanel;
    }*/

    public Component getDemoPanel() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        _table = createTable();
        _pane = new PropertyPane(_table) {
            @Override
            protected JComponent createToolBarComponent() {
                CommandBar toolBar = new CommandBar();
                toolBar.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
                toolBar.setFloatable(false);
                toolBar.setStretch(true);
                toolBar.setPaintBackground(false);
                toolBar.setChevronAlwaysVisible(false);
                return toolBar;
            }
        };

        //_pane.setShowDescription(false);


        /*JPanel quickSearchPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
        QuickTableFilterField filterField = new QuickTableFilterField(_table.getModel());
        filterField.setHintText("Type here to filter properties");
        filterField.setObjectConverterManagerEnabled(true);
        quickSearchPanel.add(filterField);
        quickSearchPanel.setBorder(new JideTitledBorder(new PartialEtchedBorder(PartialEtchedBorder.LOWERED, PartialSide.NORTH), "Filter Properties", JideTitledBorder.LEADING, JideTitledBorder.ABOVE_TOP));

        _table.setModel(filterField.getDisplayTableModel());
        panel.add(quickSearchPanel, BorderLayout.BEFORE_FIRST_LINE);*/

        _pane.setBorder(new JideTitledBorder(new PartialEtchedBorder(PartialEtchedBorder.LOWERED, PartialSide.NORTH), "PropertyPane", JideTitledBorder.LEADING, JideTitledBorder.ABOVE_TOP));
        panel.add(_pane, BorderLayout.CENTER);
        return panel;
    }

    // create property table
    private PropertyTable createTable() {

        ArrayList<SampleProperty> list = new ArrayList<SampleProperty>();

        model = new PropertyTableModel<SampleProperty>(list);
        table = new PropertyTable(model) {
            @Override
            public TableCellRenderer getCellRenderer(int row, int column) {
                Property property = getPropertyTableModel().getPropertyAt(row);
                if (property != null && "Text".equals(property.getName()) && column == 1) {
                    return new MultilineTableCellRenderer();
                }
                return super.getCellRenderer(row, column);
            }
        };
        table.setPreferredScrollableViewportSize(new Dimension(400, 600));
        table.expandFirstLevel();
        table.setRowAutoResizes(true);
        PropertyTableSearchable searchable = new PropertyTableSearchable(table);
        searchable.setRecursive(true);

        table.setTableStyleProvider(new RowStripeTableStyleProvider());
        return table;
    }

    static HashMap<String, Object> map = new HashMap<String, Object>();

    static class SampleProperty extends Property {

        public SampleProperty(String name, String description, Class type, String category, ConverterContext context) {
            super(name, description, type, category, context);
        }

        public SampleProperty(String name, String description, Class type, String category) {
            super(name, description, type, category);
        }

        public SampleProperty(String name, String description, Class type) {
            super(name, description, type);
        }

        public SampleProperty(String name, String description) {
            super(name, description);
        }

        public SampleProperty(String name) {
            super(name);
        }

        @Override
        public void setValue(Object value) {
            Object old = getValue();
            if (!JideSwingUtilities.equals(old, value)) {

                String name = this.getName();

                if(name.equals("Minimum Height")){
                    btr.setMinimumHeight((Integer)value);
                } else if(name.equals("Fixed Height")){
                    btr.setMaximumHeight((Integer)value);
                }

                viewTrack.getFrame().getGraphPane().setRenderRequired();
                viewTrack.getFrame().getGraphPane().repaint();

                map.put(getFullName(), value);
                firePropertyChange(PROPERTY_VALUE, old, value);
            }
        }

        @Override
        public Object getValue() {
            return map.get(getFullName());
        }

        @Override
        public boolean hasValue() {
            return map.get(getFullName()) != null;
        }
    }

    public void update(ViewTrack vt){

        viewTrack = vt;
        _pane.setBorder(new JideTitledBorder(new PartialEtchedBorder(PartialEtchedBorder.LOWERED, PartialSide.NORTH), vt.getName(), JideTitledBorder.LEADING, JideTitledBorder.ABOVE_TOP));

        btr = ((BAMTrackRenderer)(viewTrack.getTrackRenderers().get(0)));

        addProperty("Minimum Height", "When intervals cannot be displayed at/over the minimum height, they will switch to fixed height. ", "Interval Height Settings", Integer.class);
        map.put("Minimum Height", btr.getMinimumHeight());

        addProperty("Fixed Height", "When intervals cannot be displayed at/over the minimum height, they will switch to fixed height. ", "Interval Height Settings", Integer.class);
        map.put("Fixed Height", btr.getMaximumHeight());

        model.expandAll();
    }

    private void addProperty(String name, String description, String category, Class type){
        int pos = findProperty(name);
        if(pos == -1){
            SampleProperty property = new SampleProperty(name, description, type, category);
            model.getOriginalProperties().add(property);
            model.refresh();
        }
    }

    private void addProperty(String name, String description, String category, Class type, String[] strs, Object[] objs){
        int pos = findProperty(name);
        if(pos == -1){
            SampleProperty property = new SampleProperty(name, description, type, "Track Information");
            final EnumConverter displayModeConverter = new EnumConverter(name, Integer.class,
                objs, strs, map.get(name));
            property.setConverterContext(displayModeConverter.getContext());
            property.setEditorContext(new EditorContext(displayModeConverter.getName()));
            model.getOriginalProperties().add(property);
            model.refresh();
        }
    }

    private void removeProperty(String name){
        int pos = findProperty(name);
        if(pos != -1){
            model.getOriginalProperties().remove(pos);
            model.refresh();
        }
    }

    private int findProperty(String name){
        int result = -1;
        for(int j = 0; j < model.getOriginalProperties().size(); j++){
            if(((SampleProperty) model.getOriginalProperties().get(j)).getName() == name){
                result = j;
                break;
            }
        }
        return result;
    }

}
