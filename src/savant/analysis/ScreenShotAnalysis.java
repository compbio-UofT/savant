/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.analysis;

import savant.view.swing.util.ScreenShot;

/**
 *
 * @author mfiume
 */
public class ScreenShotAnalysis extends Analysis {

    public void runAnalysis(AnalyzeEvent event) {
        ScreenShot.takeAndSaveWithoutAsking();
    }

}
