/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.plugin;

import java.awt.BorderLayout;
import java.io.IOException;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.jdom.JDOMException;
import savant.tools.program.Program;
import savant.tools.program.ProgramArgument;
import savant.tools.program.ProgramArgumentGrid;
import savant.xml.XMLProgram;

/**
 *
 * @author mfiume
 */
public class XMLTool extends Tool {

    public static void writeTool(ProgramInformation info, List<ProgramArgument> argus, String path) {
        System.out.println("Should be writing tool:");
        System.out.println(info);
        for (ProgramArgument a : argus) {
            System.out.println(a);
        }
    }

    private Program p;

    public XMLTool(String pathToXML) throws IOException, JDOMException {
        p = (new XMLProgram(pathToXML)).getProgram();
    }

    @Override
    public ProgramInformation getToolInformation() {
        return p.getInformation();
    }

    ProgramArgumentGrid grid;
    @Override
    public JComponent getCanvas() {
        JPanel pan = new JPanel();
        pan.setLayout(new BorderLayout());
        List<ProgramArgument> args = p.getArguments();
        args.add(0, new ProgramArgument("Path", "Path to executable", "", "Program", "", ProgramArgument.Requirement.Mandatory, true, ProgramArgument.Type.File));
        grid = new ProgramArgumentGrid(args);
        pan.add(grid, BorderLayout.CENTER);
        return pan;
    }

    @Override
    public void runTool() throws InterruptedException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected void doStart() throws Exception {
    }

    @Override
    protected void doStop() throws Exception {
    }


}
