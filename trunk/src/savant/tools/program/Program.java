/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.tools.program;

import java.util.ArrayList;
import java.util.List;
import savant.experimental.ProgramInformation;
import savant.tools.program.ProgramArgument.IOType;

/**
 *
 * @author mfiume
 */
public class Program {

    private ProgramInformation information;
    private String path;
    //private List<ProgramInputOutput> inputs;
    //private List<ProgramInputOutput> outputs;
    private List<ProgramArgument> arguments;

    public Program(
            String path,
            ProgramInformation info,
            //List<ProgramInputOutput> inputs,
            //List<ProgramInputOutput> outputs,
            List<ProgramArgument> arguments) {
        this.information = info;
        this.path = path;
        //this.inputs = inputs;
        //this.outputs = outputs;
        this.arguments = arguments;
    }

    /*
    public void setInformation(ProgramInformation information) {
        this.information = information;
    }
     * 
     */

    public ProgramInformation getInformation() {
        return this.information;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<ProgramArgument> getArgumentsOfIOType(IOType t) {
        List<ProgramArgument> res = new ArrayList<ProgramArgument>();
        for (ProgramArgument r : arguments) {
            if (r.getIOType() == t) {
                res.add(r);
            }
        }
        return res;
    }

    /*
    public List<ProgramInputOutput> getInputs() {
        return this.inputs;
    }

    public List<ProgramInputOutput> getOutputs() {
        return this.outputs;
    }
     * 
     */

    public List<ProgramArgument> getArguments() {
        return this.arguments;
    }

    public String getPath() {
        return this.path;
    }

    public void addArgument(ProgramArgument arg) {
        this.arguments.add(arg);
    }

    public void removeArgument(ProgramArgument arg) {
        this.arguments.remove(arg);
    }
}
