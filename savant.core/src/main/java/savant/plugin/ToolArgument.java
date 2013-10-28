/**
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
        BOOL,                 // A boolean value.
        INT,                  // An integer value.
        FLOAT,                // A floating poing value
        LIST,                 // A list of values, one of which can be selected.
        MULTI,                // A list of values, of which multiple ones can be selected.
        RANGE,                // A range (with or without a "chr" prefix)
        OUTPUT_FILE,
        BAM_INPUT_FILE,       // A local .bam file (corresponding to a Savant alignment track)
        FASTA_INPUT_FILE      // A local .fa file (corresponding to a Savant sequence track)
    }
    
    final String name;
    final String flag;
    final Type type;
    final boolean required;
    
    /** For LIST arguments, the list of possible values. */
    String[] choices;
    
    /**
     * Value of this argument (default may be provided by XML file).
     */
    String value;

    boolean enabled;

    public ToolArgument(XMLStreamReader reader) {
        String attr;
        name = reader.getAttributeValue(null, "name");
        flag = reader.getAttributeValue(null, "flag");
        type = Enum.valueOf(Type.class, reader.getAttributeValue(null, "type"));
        required = Boolean.parseBoolean(reader.getAttributeValue(null, "required"));
        enabled = required;
        
        switch (type) {
            case BOOL:
            case INT:
            case FLOAT:
                value = reader.getAttributeValue(null, "default");
                break;
            case LIST:
                attr = reader.getAttributeValue(null, "choices");
                choices = attr.split(",\\s*");
                value = reader.getAttributeValue(null, "default");
                if (value == null) {
                    value = choices[0];
                }
                break;
            case MULTI:
                attr = reader.getAttributeValue(null, "choices");
                choices = attr.split(",\\s*");
                break;
        }
    }
    
    public void setEnabled(boolean flag) {
        enabled = flag;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
}
