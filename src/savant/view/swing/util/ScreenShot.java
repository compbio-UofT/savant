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

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import savant.util.MiscUtils;
import savant.view.swing.Savant;


public class ScreenShot {
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
        BufferedImage screen = take();
        showCompletionDialog(save(screen));
    }

    public static void takeAndSaveWithoutAsking() {
        BufferedImage screen = take();
        showCompletionDialog(saveWithoutAsking(screen));
    }

    private static String save(BufferedImage screen) {

        File selectedFile = DialogUtils.chooseFileForSave(Savant.getInstance(), "Output File", "Screenshot.png", new FileFilter() {
            @Override
            public boolean accept(File f) {
                if (f.isDirectory()) {
                    return true;
                }

                String extension = MiscUtils.getExtension(f.getAbsolutePath());
                if (extension != null) {
                    return extension.equalsIgnoreCase("png");
                }
                return false;
            }

            @Override
            public String getDescription() {
                return "Image files (*.png)";
            }
        });


        // set the genome
        if (selectedFile != null) {
            try {
                ImageIO.write(screen, "PNG", selectedFile);
                return selectedFile.getName();
            } catch (IOException ex) {
                String message = "Screenshot unsuccessful";
                String title = "Uh oh...";
                DialogUtils.displayError(title, message);
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
                DialogUtils.displayError(title, message);
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
            "Screenshot Taken",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.INFORMATION_MESSAGE,
            null,
            options,
            options[0]);

        if (n == 1) {
            showCompletionDialog = false;
        }
    }
}