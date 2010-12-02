/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package savant.tools.program;

import com.jidesoft.action.CommandBar;
import com.jidesoft.converter.StringConverter;
import com.jidesoft.grid.*;
import com.jidesoft.swing.JideSwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.table.TableCellRenderer;
import savant.tools.program.ProgramArgument.Requirement;

/**
 *
 * @author mfiume
 */
public class ProgramArgumentGrid extends JPanel {

    static HashMap<String, Object> map = new HashMap<String, Object>();
    private static PropertyTable table;
    private PropertyTableModel model;
    private PropertyPane pane;

    public ProgramArgumentGrid(List<ProgramArgument> arguments) {
        this.setLayout(new BorderLayout());
        table = createTable();
        populateTable(arguments);
        createPropertyPane();
        //table.expandAll();
        this.add(pane, BorderLayout.CENTER);
    }

    private PropertyTable createTable() {

        ArrayList<ArgumentProperty> list = new ArrayList<ArgumentProperty>();

        model = new PropertyTableModel<ArgumentProperty>(list);
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
        table.setPreferredScrollableViewportSize(new Dimension(300, 300));
        table.setRowAutoResizes(true);
        PropertyTableSearchable searchable = new PropertyTableSearchable(table);
        searchable.setRecursive(true);
        table.setTableStyleProvider(new RowStripeTableStyleProvider());
        return table;
    }

     public void addProperty(String name, String description, String category, String type, boolean isEditable, int pos) {
        if  (findProperty(name) == -1) {
            ArgumentProperty property = new ArgumentProperty(name, description, type.toString(), category, isEditable);
            model.getOriginalProperties().add(pos,property);
            model.refresh();
        }
    }

    public void addProperty(String name, String description, String category, String type, boolean isEditable) {
        addProperty(name, description, category, type, isEditable, model.getOriginalProperties().size());
        /*
        int pos = findProperty(name);
        if (pos == -1) {
            ArgumentProperty property = new ArgumentProperty(name, description, type.toString(), category, isEditable);
            model.getOriginalProperties().add(property);
            model.refresh();
        }
         * 
         */
    }

    public ArgumentProperty getProperty(String name) {
        int i = findProperty(name);
        return (ArgumentProperty) model.getOriginalProperties().get(i);
    }

    public Object getPropertyValue(String name) {
        return getProperty(name).getValue();
    }

    private int findProperty(String name) {
        int result = -1;
        for (int j = 0; j < model.getOriginalProperties().size(); j++) {
            if (((ArgumentProperty) model.getOriginalProperties().get(j)).getName().equals(name)) {
                result = j;
                break;
            }
        }
        return result;
    }

    private void populateTable(List<ProgramArgument> arguments) {
        for (ProgramArgument a : arguments) {

            String flagString = (a.getFlag() != null) ? a.getFlag() + "\n" : "";
            String mandatoryString = "\n(Specifying this argument is " + (a.getRequirement() == Requirement.Mandatory ? "mandatory" : "optional") + ")";

            addProperty(a.getName(), flagString + a.getDescription() + mandatoryString, a.getCategory(), a.getType().toString(), a.isEditable());
            map.put(a.getName(), a.getInitialValue());
        }
    }

    private void createPropertyPane() {
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
    }

    public static class ArgumentProperty extends Property {

        public ArgumentProperty(String name, String description, String type, String category, boolean isEditable) {
            this.setName(name);
            this.setDescription(description);
            this.setCategory(category);

            //System.out.println("Setting type to " + type);

            // standard type
            if (type.equals("String")) {
                this.setType(String.class);
            }

            if (type.equals("Boolean")) {
                this.setType(Boolean.class);
                this.setEditorContext(BooleanCheckBoxCellEditor.CONTEXT);
            }

            /*
            || type.equals("Boolean")
            || type.equals("Integer")
            || type.equals("Double")
            || type.equals("Color")
            || type.equals("Font")

            ) {
            try {
            this.setType(Class.forName(type));
            } catch (ClassNotFoundException ex) { this.setType(String.class); }
            }
             */

            else if (type.equals("Color")) {
                this.setType(Color.class);
            }

            else if (type.equals("Font")) {
                this.setType(Font.class);
                //this.setEditorContext(new EditorContext("FontName"));
                //this.setConverterContext(new ConverterContext("FontName"));
                //this.setCategory("Appearance");
            }
            
            else if (type.equals("File")) {
                this.setType(String.class);
                this.setConverterContext(StringConverter.CONTEXT_FILENAME);
                this.setEditorContext(FileNameCellEditor.CONTEXT);
            }

            else if (type.equals("Folder")) {
                this.setType(String.class);
                this.setEditorContext(FolderNameCellEditor.CONTEXT);
            }

            this.setEditable(isEditable);
        }

        public void setValue(Object value) {
            Object old = getValue();
            if (!JideSwingUtilities.equals(old, value)) {
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
