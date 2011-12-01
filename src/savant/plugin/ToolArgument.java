/*
 *    Copyright 2011 University of Toronto
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

package savant.plugin;

import javax.xml.stream.XMLStreamReader;

/**
 * Represents a single command-line argument for a plugin tool.
 *
 * @author tarkvara
 */
public class ToolArgument {
    public enum Type {
        INT,                  // An integer value.
        FLOAT,                // A floating poing value
        LIST,                 // A list of values, one of which can be selected.
        MULTI,                // A list of values, of which multiple ones can be selected.
        BARE_RANGE,           // A bare range (without any "chr" prefix)
        RANGE,                // A normal range (possibly with a "chr" prefix)
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
     * Usually a string, but could also be a track or a file.
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
