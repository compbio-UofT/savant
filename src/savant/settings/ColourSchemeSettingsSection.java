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

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.*;
import javax.swing.table.TableCellRenderer;

import com.jidesoft.action.CommandBar;
import com.jidesoft.converter.ConverterContext;
import com.jidesoft.grid.*;
import com.jidesoft.swing.JideSwingUtilities;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.controller.ViewTrackController;
import savant.view.swing.ViewTrack;

/**
 * SettingsDialog panel which lets the user screw up their colour settings.
 *
 * @author mfiume
 */
public class ColourSchemeSettingsSection extends Section {

    private static final Log LOG = LogFactory.getLog(ColourSchemeSettingsSection.class);

    private PropertyPane pane;
    private PropertyTableModel model;
    static HashMap<String, Color> map = new HashMap<String, Color>();

    private static final String A_NAME = "A";
    private static final String C_NAME = "C";
    private static final String G_NAME = "G";
    private static final String T_NAME = "T";
    private static final String FORWARD_STRAND_NAME = "Forward Strand";
    private static final String REVERSE_STRAND_NAME = "Reverse Strand";
    private static final String INVERTED_READ_NAME = "Inverted Read";
    private static final String INVERTED_MATE_NAME = "Inverted Mate";
    private static final String EVERTED_PAIR_NAME = "Everted Pair";
    private static final String DISCORDANT_LENGTH_NAME = "Discordant Length";
    private static final String LINE_NAME = "Line";
    private static final String CONTINUOUS_LINE_NAME = "Continuous Line";
    private static final String POINT_LINE_NAME = "Point Line";
    private static final String POINT_FILL_NAME = "Point Fill";

    @Override
    public String getSectionName() {
        return "Colour Schemes";
    }

    @Override
    public Icon getSectionIcon() {
        return null;
    }

    @Override
    public void applyChanges() {
        // Only save anything if this panel has gone through lazy initialization.
        if (pane != null) {
            try {
            //nucleotides
                ColourSettings.setA(map.get(A_NAME));
                ColourSettings.setC(map.get(C_NAME));
                ColourSettings.setG(map.get(G_NAME));
                ColourSettings.setT(map.get(T_NAME));

                //interval
                ColourSettings.setForwardStrand(map.get(FORWARD_STRAND_NAME));
                ColourSettings.setReverseStrand(map.get(REVERSE_STRAND_NAME));
                ColourSettings.setInvertedRead(map.get(INVERTED_READ_NAME));
                ColourSettings.setInvertedMate(map.get(INVERTED_MATE_NAME));
                ColourSettings.setEvertedPair(map.get(EVERTED_PAIR_NAME));
                ColourSettings.setDiscordantLength(map.get(DISCORDANT_LENGTH_NAME));
                ColourSettings.setLine(map.get(LINE_NAME));

                //continuous
                ColourSettings.setContinuousLine(map.get(CONTINUOUS_LINE_NAME));

                //point
                ColourSettings.setPointFill(map.get(POINT_FILL_NAME));
                ColourSettings.setPointLine(map.get(POINT_LINE_NAME));

                PersistentSettings.getInstance().store();

                //modify existing colour schemes
                java.util.List<ViewTrack> viewTracks = ViewTrackController.getInstance().getTracks();
                for(int i = 0; i < viewTracks.size(); i++){
                    viewTracks.get(i).resetColorScheme();
                    viewTracks.get(i).getFrame().getGraphPane().setRenderRequired();
                    try {
                        viewTracks.get(i).getFrame().redrawTracksInRange();
                    } catch (Exception ex) {
                        LOG.error(null, ex);
                    }
                }
            } catch (IOException iox) {
                LOG.error("Unable to save colour settings.", iox);
            }
        }
    }

    @Override
    public void lazyInitialize() {
        setLayout(new BorderLayout());
        add(SettingsDialog.getHeader(getTitle()), BorderLayout.BEFORE_FIRST_LINE);
        JPanel panel = new JPanel(new BorderLayout(12, 12));

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

        panel.add(pane, BorderLayout.CENTER);
        add(panel);
    }

    @Override
    public void populate(){

        //nucleotides
        addProperty(A_NAME, "Nucleotide A", "Nucleotide", ColourSettings.getA());
        addProperty(C_NAME, "Nucleotide C", "Nucleotide", ColourSettings.getC());
        addProperty(G_NAME, "Nucleotide G", "Nucleotide", ColourSettings.getG());
        addProperty(T_NAME, "Nucleotide T", "Nucleotide", ColourSettings.getT());

        //interval
        addProperty(FORWARD_STRAND_NAME, "Colour of forward strands", "Interval", ColourSettings.getForwardStrand());
        addProperty(REVERSE_STRAND_NAME, "Colour of reverse strands", "Interval", ColourSettings.getReverseStrand());
        addProperty(INVERTED_READ_NAME, "Colour of inverted reads", "Interval", ColourSettings.getInvertedRead());
        addProperty(INVERTED_MATE_NAME, "Colour of inverted mates", "Interval", ColourSettings.getInvertedMate());
        addProperty(EVERTED_PAIR_NAME, "Colour of everted pairs", "Interval", ColourSettings.getEvertedPair());
        addProperty(DISCORDANT_LENGTH_NAME, "Colour of discordant lengths", "Interval", ColourSettings.getDiscordantLength());
        addProperty(LINE_NAME, "Colour of lines", "Interval", ColourSettings.getLine());

        //continuous
        addProperty(CONTINUOUS_LINE_NAME, "Colour of continuous lines", "Continuous", ColourSettings.getContinuousLine());

        //point
        addProperty(POINT_FILL_NAME, "Colour of point fill", "Point", ColourSettings.getPointFill());
        addProperty(POINT_LINE_NAME, "Colour of point line", "Point", ColourSettings.getPointLine());

        model.expandAll();
    }

    private void addProperty(String name, String description, String category, Color value){
        int pos = findProperty(name);
        if (pos == -1){
            ColourProperty property = new ColourProperty(name, description, Color.class, category, this);
            model.getOriginalProperties().add(property);
            model.refresh();
        }
        map.put(name, value);
    }

    private int findProperty(String name){
        int result = -1;
        for(int j = 0; j < model.getOriginalProperties().size(); j++){
            if(((ColourProperty) model.getOriginalProperties().get(j)).getName().equals(name)){
                result = j;
                break;
            }
        }
        return result;
    }

    static class ColourProperty extends Property {

        private ColourSchemeSettingsSection csss = null;

        public ColourProperty(String name, String description, Class type, String category, ConverterContext context) {
            super(name, description, type, category, context);
        }

        public ColourProperty(String name, String description, Class type, String category, ColourSchemeSettingsSection csss) {
            super(name, description, type, category);
            this.csss = csss;
        }

        public ColourProperty(String name, String description, Class type) {
            super(name, description, type);
        }

        public ColourProperty(String name, String description) {
            super(name, description);
        }

        public ColourProperty(String name) {
            super(name);
        }

        @Override
        public void setValue(Object value) {
            Object old = getValue();
            if (!JideSwingUtilities.equals(old, value)) {
                csss.enableApplyButton();
                map.put(getFullName(), (Color)value);
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
}
