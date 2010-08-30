/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.plugin;

import java.awt.BorderLayout;
import java.io.IOException;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.jdom.JDOMException;
import savant.tools.program.Program;
import savant.tools.program.ProgramArgumentGrid;
import savant.tools.program.ProgramXMLFileReader;

/**
 *
 * @author mfiume
 */
public class XMLTool extends Tool {

    private Program p;

    public XMLTool(String pathToXML) throws IOException, JDOMException {
        p = (new ProgramXMLFileReader(pathToXML)).getProgram();
    }

    @Override
    public ProgramInformation getToolInformation() {
        return p.getInformation();
    }

    @Override
    public JComponent getCanvas() {
        JPanel pan = new JPanel();
        pan.setLayout(new BorderLayout());
        pan.add(new ProgramArgumentGrid(p.getArguments()), BorderLayout.CENTER);
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
