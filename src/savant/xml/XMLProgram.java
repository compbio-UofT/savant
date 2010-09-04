/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package savant.xml;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.jdom.*;
import org.jdom.input.*;
import savant.plugin.ProgramInformation;
import savant.tools.program.Program;
import savant.tools.program.ProgramArgument;
import savant.tools.program.ProgramArgument.IOType;
import savant.tools.program.ProgramArgument.Requirement;

/**
 *
 * @author mfiume
 */
public class XMLProgram {

    Program p;

    public XMLProgram(String path) throws IOException, JDOMException {
        this(new File(path));
    }

    public XMLProgram(File f) throws IOException, JDOMException {
        Document d = new SAXBuilder().build(f);

        Element programRoot = d.getRootElement();

        ProgramInformation info = parseProgramInformation(programRoot.getChild("information"));
        //List<ProgramInputOutput> inputs = parseProgramInputOutputs(programRoot.getChild("inputs"), ProgramInputOutput.Direction.INPUT);
        //List<ProgramInputOutput> outputs = parseProgramInputOutputs(programRoot.getChild("outputs"), ProgramInputOutput.Direction.OUTPUT);
        List<ProgramArgument> arguments = parseProgramArguments(programRoot.getChild("arguments"));

        p = new Program("", info, arguments);

        /*
        System.out.println(info);

        for (ProgramArgument a : arguments) {
            System.out.println(a);
        }

        System.out.println("Inputs");
        for (ProgramInputOutput put : inputs) {
            System.out.println(put);
        }

        System.out.println("Outputs");
        for (ProgramInputOutput put : outputs) {
            System.out.println(put);
        }
         *
         */
    }

    public Program getProgram() { return p; }

    private ProgramInformation parseProgramInformation(Element inforoot) {
        return new ProgramInformation(
                inforoot.getChildText("name"),
                inforoot.getChildText("category"),
                inforoot.getChildText("description"),
                inforoot.getChildText("version"),
                inforoot.getChildText("author"),
                inforoot.getChildText("site"));
    }

    private List<ProgramArgument> parseProgramArguments(Element argumentsroot) {
        List<ProgramArgument> arguments = new ArrayList<ProgramArgument>();
        for (Object c : argumentsroot.getChildren("argument")) {
            Element child = (Element) c;
            arguments.add(parseProgramArgument(child));
        }
        return arguments;
    }

    private ProgramArgument parseProgramArgument(Element argroot) {

        ProgramArgument.Type t = getArgumentType(argroot.getChildText("type"));
        Object o = getDefaultValue(t, argroot.getChildText("default"));

        String iostr = argroot.getChildText("io");
        IOType iot = IOType.None;
        if (iostr.equals("Input")) {
            iot = IOType.Input;
        } else if (iostr.equals("Output")) {
            iot = IOType.Output;
        }

        return new ProgramArgument(
                argroot.getChildText("name"),
                argroot.getChildText("description"),
                argroot.getChildText("flag"),
                argroot.getChildText("category"),
                iot,
                o,
                argroot.getChildText("required").equals("Yes") ? Requirement.Mandatory : Requirement.Optional,
                argroot.getChildText("editable").equals("Yes") ? true : false,
                t);
    }

    private ProgramArgument.Type getArgumentType(String typeStr) {
        if (typeStr.equals("Boolean")) {
            return ProgramArgument.Type.Boolean;
        } else if (typeStr.equals("String")) {
            return ProgramArgument.Type.String;
        } else if (typeStr.equals("Integer")) {
            return ProgramArgument.Type.Integer;
        } else if (typeStr.equals("Double")) {
            return ProgramArgument.Type.Double;
        } else if (typeStr.equals("Color")) {
            return ProgramArgument.Type.Color;
        } else if (typeStr.equals("Font")) {
            return ProgramArgument.Type.Font;
        } else if (typeStr.equals("File")) {
            return ProgramArgument.Type.File;
        } else if (typeStr.equals("Files")) {
            return ProgramArgument.Type.Files;
        } else if (typeStr.equals("Folder")) {
            return ProgramArgument.Type.Folder;
        } else if (typeStr.equals("Track")) {
            return ProgramArgument.Type.Track;
        } else if (typeStr.equals("Tracks")) {
            return ProgramArgument.Type.Tracks;
        } else {
            return null;
        }
    }

    private Object getDefaultValue(ProgramArgument.Type type, String t) {
        switch (type) {
            case String:
                return t;
            case Boolean:
                return t.equals("Yes");
            default:
                throw new UnsupportedOperationException("Not yet supporting type " + type);
        }
    }

    /*
    private List<ProgramInputOutput> parseProgramInputOutputs(Element root, Direction direction) {
        List<ProgramInputOutput> puts = new ArrayList<ProgramInputOutput>();
        String s = (direction == Direction.INPUT) ? "input" : "output";
        for (Object c : root.getChildren(s)) {
            Element child = (Element) c;
            puts.add(parseProgramInputOutput(child, direction));
        }
        return puts;
    }

    private ProgramInputOutput parseProgramInputOutput(Element put, Direction direction) {

        return new ProgramInputOutput(
                    direction,
                    put.getChildText("name"),
                    put.getChildText("description"),
                    getInputOutputType(put.getChildText("type")),
                    put.getChildText("editable").equals("Yes") ? true : false,
                    put.getChildText("default")
                );
    }

    private ProgramInputOutput.Type getInputOutputType(String typeStr) {

        //System.out.println(typeStr);

        if (typeStr.equals("Track")) {
            return ProgramInputOutput.Type.Track;
        } else if (typeStr.equals("Tracks")) {
            return ProgramInputOutput.Type.Tracks;
        } else if (typeStr.equals("File")) {
            return ProgramInputOutput.Type.File;
        } else if (typeStr.equals("MultipleFiles")) {
            return ProgramInputOutput.Type.Files;
        } else if (typeStr.equals("StandardInput")) {
            return ProgramInputOutput.Type.StandardInput;
        } else if (typeStr.equals("StandardOutput")) {
            return ProgramInputOutput.Type.StandardOutput;
        } else {
            return null;
        }
    }
     * 
     */
}
