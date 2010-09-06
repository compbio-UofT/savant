/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.controller;

import com.jidesoft.docking.DockableFrame;
import com.jidesoft.docking.DockingManager;
import javax.swing.JOptionPane;

/**
 *
 * @author mfiume
 */
public class DockableFrameController {

    private static DockableFrameController instance;

    public static DockableFrameController getInstance() {
        if (instance == null) {
            instance = new DockableFrameController();
        }
        return instance;
    }

    public void closeAllDockableFrames(DockingManager dm, boolean askFirst) {
        if (askFirst) {
            int response = JOptionPane.showConfirmDialog(null, "Are you sure you want to close all tracks?", "Confirm",
                    JOptionPane.YES_NO_OPTION, JOptionPane.OK_CANCEL_OPTION);
            if (response != JOptionPane.YES_OPTION) {
                return;
            }
        }
        for (String frameName : dm.getAllFrames()) {
            DockableFrame f = dm.getFrame(frameName);
            closeDockableFrame(f,false);
        }
    }

    public void closeDockableFrame(DockableFrame frame, boolean askFirst) {

        if (askFirst) {
            int response = JOptionPane.showConfirmDialog(null, "Are you sure you want to close this track?", "Confirm",
                    JOptionPane.YES_NO_OPTION, JOptionPane.OK_CANCEL_OPTION);
            if (response != JOptionPane.YES_OPTION) {
                return;
            }
        }
        System.out.println("Removing " + frame.getName());
        frame.getDockingManager().removeFrame(frame.getName());
    }
}
