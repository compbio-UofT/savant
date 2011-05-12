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

package savant.view.swing;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.table.TableCellRenderer;

import com.jidesoft.action.CommandBar;
import com.jidesoft.converter.*;
import com.jidesoft.grid.*;
import com.jidesoft.swing.JideSwingUtilities;
import com.jidesoft.swing.JideTitledBorder;
import com.jidesoft.swing.PartialEtchedBorder;
import com.jidesoft.swing.PartialSide;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.BorderFactory;

import savant.view.swing.interval.BAMTrackRenderer;

/**
 *
 * @author AndrewBrook
 */
public class IntervalDialog extends JDialog {

    protected static final Color BACKGROUND1 = new Color(253, 253, 244);
    protected static final Color BACKGROUND2 = new Color(255, 255, 255);

    private PropertyTableModel model;

    private static PropertyTable table;

    private static Track track;

    private static TrackRenderer renderer;

    public IntervalDialog(Track t) {
        setPreferredSize(new Dimension(300,500));
        setMinimumSize(new Dimension(300,500));
        setModal(true);
        setTitle("Change Interval Parameters");

        JPanel panel = new JPanel(new BorderLayout(12, 12));
        PropertyPane p = new PropertyPane(createTable()) {
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

        panel.add(p, BorderLayout.CENTER);
        add(panel);

        track = t;
        p.setBorder(new JideTitledBorder(new PartialEtchedBorder(PartialEtchedBorder.LOWERED, PartialSide.NORTH), t.getName(), JideTitledBorder.LEADING, JideTitledBorder.ABOVE_TOP));

        renderer = track.getRenderer();

        addProperty("Interval Height", "Interval Height", "Interval Height Settings", Integer.class);
        map.put("Interval Height", renderer.getIntervalHeight());

        model.expandAll();
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

                if((Integer)value == null || (Integer)value < 0){
                    return;
                }

                if(name.equals("Interval Height")){
                    if((Integer)value < 1) return;
                    renderer.setIntervalHeight((Integer)value);
                }

                track.getFrame().getGraphPane().setRenderForced();
                track.getFrame().getGraphPane().repaint();

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
            if(((SampleProperty) model.getOriginalProperties().get(j)).getName().equals(name)){
                result = j;
                break;
            }
        }
        return result;
    }

}
