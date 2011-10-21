/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package savant.plugin;

import javax.xml.stream.XMLStreamReader;

/**
 * Represents a single command-line argument for a plugin tool.
 *
 * @author tarkvara
 */
public class ToolArgument {
    public enum Type {
        LIST,
        RANGE,
        FILE,
        SEQUENCE_TRACK,
        ALIGNMENT_TRACK,
    }
    
    final String name;
    final String flag;
    final Type type;
    final boolean required;
    
    /** For LIST arguments, the list of possible values. */
    String[] choices;
    
    /** Default value of this argument. */
    String value;

    public ToolArgument(XMLStreamReader reader) {
        String attr;
        name = reader.getAttributeValue(null, "name");
        flag = reader.getAttributeValue(null, "flag");
        type = Enum.valueOf(Type.class, reader.getAttributeValue(null, "type"));
        required = Boolean.parseBoolean(reader.getAttributeValue(null, "required"));
        
        switch (type) {
            case LIST:
                attr = reader.getAttributeValue(null, "choices");
                choices = attr.split("[\\s,]+");
                value = reader.getAttributeValue(null, "default");
                if (value == null) {
                    value = choices[0];
                }
                break;
        }
    }
}
