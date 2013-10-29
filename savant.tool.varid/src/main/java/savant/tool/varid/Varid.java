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
package savant.tool.varid;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import savant.plugin.PluginAdapter;
import savant.plugin.ToolInformation;
import savant.plugin.ToolPlugin;
import savant.swing.component.AlignedComponent;
import savant.swing.component.ArgumentField;
import savant.swing.component.StringArgumentField;
import savant.swing.component.TrackField;
import savant.view.tools.ToolRunInformation;

/**
 *
 * @author mfiume
 */
public class Varid extends ToolPlugin {

    List<ArgumentField> argumentFields;

    @Override
    public void init(PluginAdapter pluginAdapter) {
        argumentFields = new ArrayList<ArgumentField>();
        argumentFields.add(new StringArgumentField("Alignment file", "--alignments", "Some decription", true, true, "default value"));
    }

    @Override
    public ToolInformation getToolInformation() {
        return new ToolInformation(
                "VARiD",
                "Genetic Variation Discovery",
                "VARiD is a Hidden Markov Model for SNP and indel identification with AB-SOLiD color-space as well as regular letter-space reads. VARiD combines both types of data in a single framework which allows for accurate predictions. VARiD was developed at the University of Toronto Computational Biology Lab.",
                "1.0",
                "Adrian Dalca",
                "http://compbio.cs.utoronto.ca/varid/");
    }

    @Override
    public JComponent getCanvas() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));

        TrackField tf = new TrackField(true);
        
        p.add(new AlignedComponent(tf, BorderLayout.WEST.toString()));

        for (ArgumentField f : argumentFields) {
            p.add(new AlignedComponent(f, BorderLayout.WEST.toString()));
        }

        return p;
    }

    @Override
    public void runTool() throws InterruptedException {

        ToolRunInformation runInfo = this.getRunInformation();

        System.out.println("Running VARID");

        for (ArgumentField a : argumentFields) {
            System.out.println(a.getArgumentName() + " " + a.getValue());

        }

        /*
        int maxCount = 1000000;

        for (int i = 1; i <= maxCount; i++) {

            if (i % 10000 == 0) {
                getOutputStream().println(i);
            }

            runInfo.setProgress(i * 100 / maxCount);
            runInfo.setStatus("on " + i + " of " + maxCount);

            //System.out.println(i);
            terminateIfInterruped();
        }
         *
         */
    }

    @Override
    protected void doStart() throws Exception {
    }

    @Override
    protected void doStop() throws Exception {
    }
}
