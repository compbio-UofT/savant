/*
 *    Copyright 2010 University of Toronto
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

package savant.view.swing.util;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;

import savant.util.MiscUtils;
import savant.view.swing.Savant;


public class ScreenShot
{
    private static int saveCount = 0;
    private static boolean showCompletionDialog = true;

    public static BufferedImage take() {
        try {
            Robot robot = new Robot();
            BufferedImage screenShot = robot.createScreenCapture(Savant.getInstance().getBounds());
//                    new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
            return screenShot;
        } 
        catch (AWTException e) {}
        return null;
    }
    
    public static void takeAndSave() {
        Savant.log("Take");
        BufferedImage screen = take();
        Savant.log("Save");
        String name = save(screen);
        Savant.log("Done screenshot");
        showCompletionDialog(name);
    }

    public static void takeAndSaveWithoutAsking() {
        BufferedImage screen = take();
        String name = saveWithoutAsking(screen);
        showCompletionDialog(name );
    }

    private static String save(BufferedImage screen) {

        JFrame jf = new JFrame();


        String selectedFileName;
        if (Savant.mac) {
            FileDialog fd = new FileDialog(jf, "Output File", FileDialog.SAVE);
            fd.setVisible(true);
            jf.setAlwaysOnTop(true);
            // get the path (null if none selected)
            selectedFileName = fd.getFile();
            if (selectedFileName != null) {
                selectedFileName = fd.getDirectory() + selectedFileName;
            }
        }
        else {
            JFileChooser fd = new JFileChooser();
            fd.setDialogTitle("Output File");
            fd.setDialogType(JFileChooser.SAVE_DIALOG);
            fd.setFileFilter(new FileFilter() {

                @Override
                public boolean accept(File f) {
                        if (f.isDirectory()) {
                            return true;
                        }

                        String extension = MiscUtils.getExtension(f.getAbsolutePath());
                        if (extension != null) {
                            if (extension.equalsIgnoreCase("png")) {
                                return true;
                            } else {
                                return false;
                            }
                        }

                        return false;
                }

                @Override
                public String getDescription() {
                    return "Image files (*.png)";
                }

            });
            int result = fd.showSaveDialog(jf);
            if (result == JFileChooser.CANCEL_OPTION || result == JFileChooser.ERROR_OPTION ) return null;
            selectedFileName = fd.getSelectedFile().getPath();
        }

        // set the genome
        if (selectedFileName != null) {
            try {
                ImageIO.write(screen, "PNG", new File(selectedFileName));
                return selectedFileName;
            } catch (IOException ex) {
                String message = "Screenshot unsuccessful";
                String title = "Uh oh...";
                // display the JOptionPane showConfirmDialog
                JOptionPane.showConfirmDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
                return null;
            }
        }
        
        return null;
    }

    public static String saveWithoutAsking(BufferedImage screen) {
        try {
                String selectedFileName = ++saveCount + ".png";
                ImageIO.write(screen, "PNG", new File(selectedFileName));
                return selectedFileName;
            } catch (IOException ex) {
                String message = "Screenshot unsuccessful";
                String title = "Uh oh...";
                // display the JOptionPane showConfirmDialog
                JOptionPane.showConfirmDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
            }
        return null;
    }

    private static void showCompletionDialog(String name) {
        if (name == null || !showCompletionDialog) { return; }
        //Custom button text
        Object[] options = {"OK",
                            "Don't show again"};
        int n = JOptionPane.showOptionDialog(Savant.getInstance(),
            "Saved to " + name,
            "Sreenshot Taken",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.INFORMATION_MESSAGE,
            null,
            options,
            options[0]);

        if (n == 1) {
            showCompletionDialog = false;
        }
        //JOptionPane.showMessageDialog(Savant.getInstance(), "Saved to " + name, "Sreenshot Taken", JOptionPane.INFORMATION_MESSAGE);
    }

}