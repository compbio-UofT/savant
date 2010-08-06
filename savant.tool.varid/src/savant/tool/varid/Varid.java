/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package savant.tool.varid;

import javax.swing.JComponent;
import javax.swing.JPanel;
import savant.plugin.PluginAdapter;
import savant.plugin.ToolInformation;
import savant.plugin.ToolPlugin;
import savant.view.tools.ToolRunInformation;

/**
 *
 * @author mfiume
 */
public class Varid extends ToolPlugin {

    @Override
    public void init(PluginAdapter pluginAdapter) {
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
        return p;
    }

    @Override
    public void runTool() throws InterruptedException {

        ToolRunInformation runInfo = this.getRunInformation();

        System.out.println("Running VARID");
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
    }

    @Override
    protected void doStart() throws Exception {
    }

    @Override
    protected void doStop() throws Exception {
    }
}
