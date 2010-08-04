/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.tools.script;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author mfiume
 */
public class Script {

    private String path;
    List<ScriptInputOutput> inputs;
    List<ScriptInputOutput> outputs;
    List<ScriptArgument> arguments;

    public Script() {
        this(
                "",
                new ArrayList<ScriptInputOutput>(),
                new ArrayList<ScriptInputOutput>(),
                new ArrayList<ScriptArgument>()
                );
    }

    public Script(
            String path,
            List<ScriptInputOutput> inputs,
            List<ScriptInputOutput> outputs,
            List<ScriptArgument> arguments) {
        this.path = path;
        this.inputs = inputs;
        this.outputs = outputs;
        this.arguments = arguments;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<ScriptInputOutput> getInputs() {
        return this.inputs;
    }

    public List<ScriptInputOutput> getOutputs() {
        return this.outputs;
    }

    public List<ScriptArgument> getArguments() {
        return this.arguments;
    }

    public String getPath() {
        return this.path;
    }

    public void addArgument(ScriptArgument arg) {
        this.arguments.add(arg);
    }

    public void removeArgument(ScriptArgument arg) {
        this.arguments.remove(arg);
    }

    public void addInput(ScriptInputOutput input) {
        this.inputs.add(input);
    }

    public void addOutput(ScriptInputOutput output) {
        this.outputs.add(output);
    }

    public void removeInput(ScriptInputOutput input) {
        this.inputs.remove(input);
    }

    public void removeOutput(ScriptInputOutput output) {
        this.outputs.remove(output);
    }

}
