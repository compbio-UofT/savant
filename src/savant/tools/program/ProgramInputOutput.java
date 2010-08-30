/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.tools.program;

/**
 *
 * @author mfiume
 */
public class ProgramInputOutput {

    public enum Direction { INPUT, OUTPUT };
    public enum Type { Track, Tracks, File, Files, StandardInput, StandardOutput };
    //public enum Source { FIXED, SPECIFIED };

    private String name;
    private String description;
    private Direction direction;
    private Type type;
    private boolean isEditable;
    //private Source source;
    public String defaultValue;

    /*
    public ProgramInputOutput(Direction d) {
        setDirection(d);
    }

    public ProgramInputOutput(Direction d, Type t) {
        setDirection(d);
        this.type = t;
        this.source = Source.SPECIFIED;
    }
     * 
     */

    public ProgramInputOutput(Direction d, String name, String description, Type t, boolean isEditable, String defaultValue) {
        this.direction = d;
        this.name = name;
        this.description = description;
        this.type = t;
        this.isEditable = isEditable;
        this.defaultValue = defaultValue;
    }

    public String getName() { return this.name; }
    public String getDescription() { return this.description; }
    public Direction getDirection() { return this.direction; }
    public Type getType() { return type; }
    public boolean isEditable() { return isEditable; }
    public String getDefaultValue() { return this.defaultValue;}

    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setDirection(Direction d) { this.direction = d; }
    public void setType(Type t) { this.type = t; }
    public void setEditable(boolean s) { this.isEditable = s; }
    public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }

    @Override
    public String toString() {
        return name + " " + description + " " + direction + " " + type + " " + isEditable + " " + defaultValue;
    }
}
