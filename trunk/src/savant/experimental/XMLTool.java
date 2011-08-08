/*
 *    Copyright 2010-2011 University of Toronto
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
package savant.experimental;

import java.awt.BorderLayout;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
    private static final Log LOG = LogFactory.getLog(XMLTool.class);

    public static void writeTool(ProgramInformation info, List<ProgramArgument> argus, File path) {
        LOG.info("Should be writing tool:");
        LOG.info(info);
        for (ProgramArgument a : argus) {
            LOG.info(a);
        }
    }

    private Program p;

    public XMLTool(File pathToXML) throws IOException, JDOMException {
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
}
