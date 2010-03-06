/*
 *    Copyright 2009-2010 University of Toronto
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

package savant.model.view;

import java.util.Dictionary;
import java.util.Hashtable;

/**
 *
 * @author mfiume
 */
public class DrawingInstructions {

    public enum InstructionName {
        TRACK_DATA_TYPE, MODE, AXIS_RANGE, RESOLUTION, RANGE, COLOR_SCHEME, GENOME }

        Dictionary<String, Object> instructions;

        public DrawingInstructions()
        {
            instructions = new Hashtable<String, Object>();
        }

        public Dictionary<String, Object> getInstructions() { return this.instructions; }
        public void setInstructions(Dictionary<String, Object> ins) { this.instructions = ins; }

        public Object getInstruction(String key)
        {
            if (isInstruction(key))
            {
                return instructions.get(key);
            }
            else
            {
                return null;
            }
        }

        public Object getInstruction(InstructionName name)
        {
            if (isInstruction(name.toString()))
            {
                return instructions.get(name.toString());
            }
            else
            {
                return null;
            }
        }

        public void setInstruction(String key, Object value)
        {
            instructions.put(key, value);
        }

        public void addInstruction(String key, Object value)
        {
            setInstruction(key, value);
        }

        public void addInstruction(InstructionName key, Object value)
        {
            instructions.put(key.toString(), value);
        }
    
        public boolean isInstruction(String instructionName)
        {
            return instructions.get(instructionName) != null;
        }
}
