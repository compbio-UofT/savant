/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.tools.program;

/**
 *
 * @author mfiume
 */
public class ProgramArgument {

    public enum IOType { Input, Output, None };
    public enum Type { Boolean, String, Integer, Double, Color, Font, File, Files, Folder, Track, Tracks };
    //public enum Type { BOOLEAN, STRING, INTEGER, DOUBLE, FILE, FILES, TRACK, TRACKS, FOLDER, COLOR };
    public enum Requirement { Mandatory, Optional };
    
    private String name;
    private String flag;
    private String description;
    private String category;
    private IOType iotype;
    private boolean isEdittable;
    private Object initialValue;
    private Requirement requirement = Requirement.Optional;
    private Type type;

    public ProgramArgument(String name, String description, String flag, String category, Object initialValue, Requirement r, boolean isEdittable,  Type t) {
        this( name,  description,  flag,  category, IOType.None, initialValue, r, isEdittable, t);
    }

    public ProgramArgument(String name, String description, String flag, String category, IOType iot, Object initialValue, Requirement r, boolean isEdittable,  Type t) {
        setName(name);
        setDescription(description);
        setFlag(flag);
        setCategory(category);
        setIOType(iot);
        setEditable(isEdittable);
        setInitialValue(initialValue);
        setRequirement(r);
        setType(t);
    }

    public void setName(String arg) { this.name = arg; }
    public void setInitialValue(Object arg) { this.initialValue = arg; }
    public void setDescription(String arg) { this.description = arg; }
    public void setFlag(String arg) { this.flag = arg; }
    public void setEditable(boolean arg) { this.isEdittable = arg; }
    public void setCategory(String arg) { this.category = arg; }
    public void setIOType(IOType arg) { this.iotype = arg; }
    public void setRequirement(Requirement arg) { this.requirement = arg; }
    public void setType(Type t) { this.type = t; }

    public String getName() { return this.name; }
    public String getDescription() { return this.description; }
    public String getFlag() { return this.flag; }
    public String getCategory() { return this.category; }
    public IOType getIOType() { return this.iotype; }
    public boolean isEditable() { return this.isEdittable; }
    public Object getInitialValue() { return this.initialValue; }
    public Type getType() { return this.type; }
    public Requirement getRequirement() { return this.requirement; }

    @Override
    public String toString() {
        return getName() + " " +
               getDescription() + " " +
               getFlag() + " " +
               getCategory() + " " +
               getIOType() + " " +
               getType() + " " +
               isEditable() + " " +
               getRequirement() + " " +
               getInitialValue();
    }
}
