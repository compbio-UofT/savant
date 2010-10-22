/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.view.swing;

import java.util.ArrayList;
import java.util.List;
import savant.controller.FrameController;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.swing.filechooser.FileFilter;
import savant.controller.RangeController;
//import savant.util.DataFormatUtils;

/**
 *
 * @author AndrewBrook
 */
public class ExportImage {

    public ExportImage(){

        String[] tracks = Savant.getInstance().getSelectedTracks(true, "Select Tracks to Export");
        if(tracks == null) return;

        List<Frame> frames = FrameController.getInstance().getFrames();

        List<BufferedImage> images = new ArrayList<BufferedImage>();
        int totalWidth = 0;
        int totalHeight = 45;
        for(int i = 0; i <frames.size(); i++){
            for(int j = 0; j <tracks.length; j++){
                //if(frames.get(i).getTracks().get(0).getName().equals(tracks[j])){
                if(frames.get(i).getName().equals(tracks[j])){
                    BufferedImage im = frames.get(i).frameToImage();
                    images.add(im);
                    totalWidth = Math.max(totalWidth, im.getWidth());
                    totalHeight += im.getHeight();
                    tracks[j] = null;
                    break;
                }
            }
        }
        //no frames selected
        if (images.isEmpty()) {
            return;
        }

        BufferedImage out = new BufferedImage(totalWidth, totalHeight, BufferedImage.TYPE_INT_RGB);


        //write range at top
        RangeController range = RangeController.getInstance();
        long start = range.getRangeStart();
        long end = range.getRangeEnd();
        String toWrite = "Range:  " + start + " - " + end;
        Graphics2D g = out.createGraphics();
        g.setColor(Color.white);
        g.setFont(new Font(null, Font.BOLD, 13));
        g.drawString(toWrite, 2, 17);



        int outX = 0;
        int outY = 25;
        for(int i = 0; i < images.size(); i++){
            BufferedImage current = images.get(i);
            for(int y = 0; y < current.getHeight(); y++){
                for(int x = 0; x < current.getWidth(); x++){
                    int color = current.getRGB(x, y);
                    out.setRGB(outX, outY, color);
                    outX++;
                }
                outX = 0;
                outY++;
            }
        }

        //write message at bottom
        toWrite = "Generated using the Savant Genome Browser - http://compbio.cs.toronto.edu/savant/";
        g.setColor(Color.white);
        g.setFont(new Font(null, Font.BOLD, 10));
        g.drawString(toWrite, 2, outY+14);

        save(out);

    }

    private String save(BufferedImage screen) {

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



                        String path = f.getAbsolutePath();
                        String extension = "";
                        int indexOfDot = path.lastIndexOf(".");
                        if (indexOfDot == -1 || indexOfDot == path.length() - 1) {
                            extension = "";
                        } else {
                            extension = path.substring(indexOfDot + 1);
                        }

                        //String extension = DataFormatUtils.getExtension(f.getAbsolutePath());
                        if (extension != null) {
                            if (extension.equals("png")) {
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
            } catch (IOException ex) {
                String message = "Screenshot unsuccessful";
                String title = "Uh oh...";
                // display the JOptionPane showConfirmDialog
                JOptionPane.showConfirmDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
            }
        }

        return null;
    }

}
