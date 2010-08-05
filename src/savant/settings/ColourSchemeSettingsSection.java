/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package savant.settings;

import com.jidesoft.action.CommandBar;
import com.jidesoft.converter.ConverterContext;
import com.jidesoft.docking.DockableFrame;
import com.jidesoft.grid.*;
import com.jidesoft.swing.JideSwingUtilities;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import javax.swing.table.TableCellRenderer;
import savant.controller.RangeController;
import savant.controller.ViewTrackController;
import savant.view.swing.Savant;
import savant.view.swing.ViewTrack;

/**
 *
 * @author mfiume
 */
public class ColourSchemeSettingsSection extends Section {

    private PropertyTable _table;
    private static PropertyTable table;
    private PropertyPane _pane;
    private PropertyTableModel model;
    static HashMap<String, Object> map = new HashMap<String, Object>();
    private JPanel panel;

    public ColourSchemeSettingsSection(){

        panel = new JPanel(new BorderLayout(12, 12));
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

        _pane.setShowDescription(false);

        panel.add(_pane, BorderLayout.CENTER);
    }

    public String getSectionName() {
        return "Colour Schemes";
    }

    public Icon getSectionIcon() {
        return null;
    }

    public void applyChanges() {

        //TODO: ISSUE A WARNING THAT CHANGES WILL BE OVERWRITTEN
        
        Iterator it = map.keySet().iterator();
        while(it.hasNext()){

            Object o = it.next();
            String name = (String) o;
            Color c = (Color)map.get(name);

            //nucleotides
            if(name.equals("A")) ColourSettings.A_COLOR = c;
            else if(name.equals("C")) ColourSettings.C_COLOR = c;
            else if(name.equals("G")) ColourSettings.G_COLOR = c;
            else if(name.equals("T")) ColourSettings.T_COLOR = c;

            //interval
            else if(name.equals("Forward Strand")) ColourSettings.forwardStrand = c;
            else if(name.equals("Reverse Strand")) ColourSettings.reverseStrand = c;
            else if(name.equals("Inverted Read")) ColourSettings.invertedRead = c;
            else if(name.equals("Inverted Mate")) ColourSettings.invertedMate = c;
            else if(name.equals("Everted Pair")) ColourSettings.evertedPair = c;
            else if(name.equals("Discordant Length")) ColourSettings.discordantLength = c;
            else if(name.equals("Line")) ColourSettings.line = c;

            //continuous
            else if(name.equals("Continuous Line")) ColourSettings.continuousLine = c;

            //point
            else if(name.equals("Point Fill")) ColourSettings.colorGraphMain = c;
            else if(name.equals("Point Line")) ColourSettings.colorAccent = c;

        }

        //modify existing colour schemes
        java.util.List<ViewTrack> viewTracks = ViewTrackController.getInstance().getTracks();
        for(int i = 0; i < viewTracks.size(); i++){
            viewTracks.get(i).resetColorScheme();
            viewTracks.get(i).getFrame().getGraphPane().setRenderRequired();
            try {
                viewTracks.get(i).getFrame().redrawTracksInRange();
            } catch (Exception ex) {
                Logger.getLogger(ColourSchemeSettingsSection.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void lazyInitialize() {
        setLayout(new BorderLayout());
        add(SettingsDialog.getHeader(getTitle()), BorderLayout.BEFORE_FIRST_LINE);
        this.add(this.panel);
    }

    public void populate(){

        //nucleotides
        addProperty("A", "Nucleotide A", "Nucleotide", Color.class);
        map.put("A", ColourSettings.A_COLOR);
        addProperty("C", "Nucleotide C", "Nucleotide", Color.class);
        map.put("C", ColourSettings.C_COLOR);
        addProperty("G", "Nucleotide G", "Nucleotide", Color.class);
        map.put("G", ColourSettings.G_COLOR);
        addProperty("T", "Nucleotide T", "Nucleotide", Color.class);
        map.put("T", ColourSettings.T_COLOR);

        //interval
        addProperty("Forward Strand", "Colour of forward strands", "Interval", Color.class);
        map.put("Forward Strand", ColourSettings.forwardStrand);
        addProperty("Reverse Strand", "Colour of reverse strands", "Interval", Color.class);
        map.put("Reverse Strand", ColourSettings.reverseStrand);
        addProperty("Inverted Read", "Colour of inverted reads", "Interval", Color.class);
        map.put("Inverted Read", ColourSettings.invertedRead);
        addProperty("Inverted Mate", "Colour of inverted mates", "Interval", Color.class);
        map.put("Inverted Mate", ColourSettings.invertedMate);
        addProperty("Everted Pair", "Colour of everted pairs", "Interval", Color.class);
        map.put("Everted Pair", ColourSettings.evertedPair);
        addProperty("Discordant Length", "Colour of discordant lengths", "Interval", Color.class);
        map.put("Discordant Length", ColourSettings.discordantLength);
        addProperty("Line", "Colour of lines", "Interval", Color.class);
        map.put("Line", ColourSettings.line);

        //continuous
        addProperty("Continuous Line", "Colour of continuous lines", "Continuous", Color.class);
        map.put("Continuous Line", ColourSettings.continuousLine);

        //point
        addProperty("Point Fill", "Colour of point fill", "Point", Color.class);
        map.put("Point Fill", ColourSettings.colorGraphMain);
        addProperty("Point Line", "Colour of point line", "Point", Color.class);
        map.put("Point Line", ColourSettings.colorAccent);

        model.expandAll();
    }

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

    private void addProperty(String name, String description, String category, Class type){
        int pos = findProperty(name);
        if(pos == -1){
            SampleProperty property = new SampleProperty(name, description, type, category, this);
            model.getOriginalProperties().add(property);
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

    static class SampleProperty extends Property {

        private ColourSchemeSettingsSection csss = null;

        public SampleProperty(String name, String description, Class type, String category, ConverterContext context) {
            super(name, description, type, category, context);
        }

        public SampleProperty(String name, String description, Class type, String category, ColourSchemeSettingsSection csss) {
            super(name, description, type, category);
            this.csss = csss;
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

        public void setValue(Object value) {
            Object old = getValue();
            if (!JideSwingUtilities.equals(old, value)) {
                csss.enableApplyButton();
                map.put(getFullName(), value);
            }
        }

        public Object getValue() {
            return map.get(getFullName());
        }

        @Override
        public boolean hasValue() {
            return map.get(getFullName()) != null;
        }
    }
}
