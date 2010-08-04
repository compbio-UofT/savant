/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.tools.script;

/**
 *
 * @author mfiume
 */
public class ScriptArgument {

    public enum Type { STRING, INTEGER, DOUBLE, FILE, FOLDER, COLOR };
    public enum Requirement { MANDATORY, OPTIONAL };
    
    private String name;
    private String description;
    private String initialValue;
    private Requirement requirement = Requirement.OPTIONAL;



    public ScriptArgument(String name, String description, String initialValue, Requirement r, Type t) {

    }

    public void setName(String arg) { this.name = arg; }
    public void setInitialValue(String arg) { this.initialValue = arg; }
    public void setDescription(String arg) { this.description = arg; }
    public void setRequirement(Requirement arg) { this.requirement = arg; }

}
