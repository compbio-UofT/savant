/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.util;

import savant.view.swing.Savant;

import java.awt.AWTException;
import java.awt.FileDialog;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;

import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class ScreenShot
{
    private static int saveCount = 0;

    public static BufferedImage take() {
        try {
            Robot robot = new Robot();
            BufferedImage screenShot = robot.createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
            return screenShot;
        } 
        catch (AWTException e) {}
        return null;
    }
    
    public static void takeAndSave() {
        Savant.log("Take");
        BufferedImage screen = take();
        Savant.log("Save");
        save(screen);
        Savant.log("Done screenshot");
    }

    public static void takeAndSaveWithoutAsking() {
        BufferedImage screen = take();
        saveWithoutAsking(screen);
    }

    private static void save(BufferedImage screen) {

        JFrame jf = new JFrame();
        FileDialog fd = new FileDialog(jf, "Output File", FileDialog.SAVE);
        fd.setVisible(true);

        // get the path (null if none selected)
        String selectedFileName = fd.getFile();

        // set the genome
        if (selectedFileName != null) {
            try {
                selectedFileName = fd.getDirectory() + selectedFileName;
                ImageIO.write(screen, "PNG", new File(selectedFileName));
            } catch (IOException ex) {
                String message = "Screenshot unsuccessful";
                String title = "Uh oh...";
                // display the JOptionPane showConfirmDialog
                JOptionPane.showConfirmDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static void saveWithoutAsking(BufferedImage screen) {
        try {
                String selectedFileName = ++saveCount + ".png";
                ImageIO.write(screen, "PNG", new File(selectedFileName));
            } catch (IOException ex) {
                String message = "Screenshot unsuccessful";
                String title = "Uh oh...";
                // display the JOptionPane showConfirmDialog
                JOptionPane.showConfirmDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
            }
    }

}