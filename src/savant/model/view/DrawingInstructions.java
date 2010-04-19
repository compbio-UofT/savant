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
 * Class to represent drawing instructions to be set on a renderer.
 *
 * @author mfiume
 */
public class DrawingInstructions {

    // TODO: just use strings instead of letting enums proliferate?

    /**
     * Enum representing instruction types
     */
    public enum InstructionName {
        TRACK_DATA_TYPE, MODE, AXIS_RANGE, RESOLUTION, RANGE, COLOR_SCHEME, GENOME, ARC_MIN }

        Dictionary<String, Object> instructions;

    /**
     * Constructor
     */
    public DrawingInstructions()
    {
        instructions = new Hashtable<String, Object>();
    }

    /**
     * Get all instructions.
     *
     * @return Dictionary of all instructions
     */
    public Dictionary<String, Object> getInstructions() { return this.instructions; }

    /**
     * Set all instructions.
     *
     * @param ins new instructions
     */
    public void setInstructions(Dictionary<String, Object> ins) { this.instructions = ins; }

    /**
     * Get instruction keyed by name.
     *
     * @param key
     * @return an instruction object
     */
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

    /**
     * Get instruction keyed by enum
     *
     * @param name instruction name
     * @return instruction object
     */
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

    /**
     * Set instruction.
     *
     * @param key String name
     * @param value instruction value
     */
    public void setInstruction(String key, Object value)
    {
        instructions.put(key, value);
    }

    /**
     * Add instruction.
     *
     * @param key String name
     * @param value instruction value
     */
    public void addInstruction(String key, Object value)
    {
        setInstruction(key, value);
    }

    /**
     * Add instruction.
     *
     * @param key Instruction name as enum
     * @param value instruction object
     */
    public void addInstruction(InstructionName key, Object value)
    {
        instructions.put(key.toString(), value);
    }

    /**
     * Determine if given instruction exists.
     *
     * @param instructionName String name
     * @return true if instruction exists; false otherwise
     */
    public boolean isInstruction(String instructionName)
    {
        return instructions.get(instructionName) != null;
    }
}
