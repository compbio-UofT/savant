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

package savant.settings;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.table.TableCellRenderer;

import com.jidesoft.action.CommandBar;
import com.jidesoft.grid.*;

import savant.controller.TrackController;
import savant.util.ColourKey;
import savant.view.tracks.Track;

/**
 * SettingsDialog panel which lets the user screw up their colour settings.
 *
 * @author mfiume
 */
public class ColourSchemeSettingsSection extends Section {

    protected PropertyPane pane;
    protected PropertyTableModel model;
    protected EnumMap<ColourKey, Color> map = new EnumMap<ColourKey, Color>(ColourKey.class);

    @Override
    public String getTitle() {
        return "Colour Schemes";
    }

    @Override
    public void applyChanges() {
        // Only save anything if this panel has gone through lazy initialization.
        if (pane != null) {
            try {
                //nucleotides
                for (ColourKey k: map.keySet()) {
                   ColourSettings.setColor(k, map.get(k));
                }

                PersistentSettings.getInstance().store();

                // Modify existing colour schemes
                for (Track t: TrackController.getInstance().getTracks()) {
                    t.getFrame().forceRedraw();
                }
            } catch (IOException iox) {
                LOG.error("Unable to save colour settings.", iox);
            }
        }
    }

    @Override
    public void lazyInitialize() {
        GridBagConstraints gbc = getFullRowConstraints();
        add(SettingsDialog.getHeader(getTitle()), gbc);

        ArrayList<ColourProperty> list = new ArrayList<ColourProperty>();
        model = new PropertyTableModel<ColourProperty>(list);
        PropertyTable table = new PropertyTable(model) {
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
        pane = new PropertyPane(table) {
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

        pane.setShowDescription(false);

        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        add(pane, gbc);
    }

    @Override
    public void populate(){

        //nucleotides
        addProperty(ColourKey.A, "Nucleotide");
        addProperty(ColourKey.C, "Nucleotide");
        addProperty(ColourKey.G, "Nucleotide");
        addProperty(ColourKey.T, "Nucleotide");

        //interval
        addProperty(ColourKey.FORWARD_STRAND, "Interval");
        addProperty(ColourKey.REVERSE_STRAND, "Interval");
        addProperty(ColourKey.CONCORDANT_LENGTH, "Interval");
        addProperty(ColourKey.DISCORDANT_LENGTH, "Interval");
        addProperty(ColourKey.ONE_READ_INVERTED, "Interval");
        addProperty(ColourKey.EVERTED_PAIR, "Interval");
        addProperty(ColourKey.UNMAPPED_MATE, "Interval");

        //continuous
        addProperty(ColourKey.CONTINUOUS_FILL, "Continuous");
        addProperty(ColourKey.CONTINUOUS_LINE, "Continuous");

        //point
        addProperty(ColourKey.POINT_FILL, "Point");
        addProperty(ColourKey.POINT_LINE, "Point");

        model.expandAll();
    }

    public void addProperty(ColourKey key, String category) {
        int pos = findProperty(key);
        if (pos == -1) {
            ColourProperty property = new ColourProperty(key, key.getDescription(), Color.class, category);
            model.getOriginalProperties().add(property);
            model.refresh();
        }
        map.put(key, ColourSettings.getColor(key));
    }

    private int findProperty(ColourKey key) {
        int result = -1;
        for(int j = 0; j < model.getOriginalProperties().size(); j++){
            if(((ColourProperty)model.getOriginalProperties().get(j)).key == key){
                result = j;
                break;
            }
        }
        return result;
    }

    class ColourProperty extends Property {

        private final ColourKey key;

        public ColourProperty(ColourKey key, String description, Class type, String category) {
            super(key.getName(), description, type, category);
            this.key = key;
        }

        @Override
        public void setValue(Object value) {
            Object old = getValue();
            if (!old.equals(value)) {
                enableApplyButton();
                map.put(key, (Color)value);
            }
        }

        @Override
        public Object getValue() {
            return map.get(key);
        }

        @Override
        public boolean hasValue() {
            return map.get(key) != null;
        }
    }
}
