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
package savant.view.swing;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;

import savant.controller.FrameController;
import savant.controller.GraphPaneController;
import savant.controller.RangeController;
import savant.controller.ReferenceController;
import savant.swing.component.TrackChooser;
import savant.util.MiscUtils;

/**
 *
 * @author AndrewBrook
 */
public class ExportImage {

    public ExportImage(){

        int defaultBase = -1;
        if(GraphPaneController.getInstance().isPlumbing()){
            defaultBase = GraphPaneController.getInstance().getMouseXPosition();
        }

        TrackChooser tc = new TrackChooser(Savant.getInstance(), true, "Select Tracks to Export", true, defaultBase);
        tc.setVisible(true);
        String[] trackNames = tc.getSelectedTracks();
        int base = tc.getBaseSelected();

        //String[] trackNames = Savant.getInstance().getSelectedTracks(true, "Select Tracks to Export", true);
        if(trackNames == null) return;

        BufferedImage bf = beginExport(trackNames, base);
        if(bf == null) return;
        save(bf);
    }

    public static BufferedImage beginExport(String[] trackNames, int base){

        List<Frame> frames = FrameController.getInstance().getFrames();

        //generate images
        List<BufferedImage> images = new ArrayList<BufferedImage>();
        int totalWidth = 0;
        int totalHeight = 45;
        for(int j = 0; j <trackNames.length; j++){
            for(int i = 0; i <frames.size(); i++){
                if(frames.get(i).getName().equals(trackNames[j])){
                    BufferedImage im = frames.get(i).frameToImage(base);
                    images.add(im);
                    totalWidth = Math.max(totalWidth, im.getWidth());
                    totalHeight += im.getHeight();
                    trackNames[j] = null;
                    break;
                }
            }
        }
        
        //no frames selected
        if (images.isEmpty()) {
            return null;
        }

        BufferedImage out = new BufferedImage(totalWidth, totalHeight, BufferedImage.TYPE_INT_RGB);


        //write range at top
        RangeController range = RangeController.getInstance();
        int start = range.getRangeStart();
        int end = range.getRangeEnd();
        String toWrite = "Genome:  " + ReferenceController.getInstance().getGenome().getName() + "    Reference:  " + ReferenceController.getInstance().getReferenceName() + "    Range:  " + start + " - " + end;
        Graphics2D g = out.createGraphics();
        g.setColor(Color.white);
        g.setFont(new Font(null, Font.BOLD, 13));
        g.drawString(toWrite, 2, 17);

        //draw images
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
        toWrite = "Generated using the Savant Genome Browser - http://savantbrowser.com";
        g.setColor(Color.white);
        g.setFont(new Font(null, Font.BOLD, 10));
        g.drawString(toWrite, 2, outY+14);

        //save(out);
        return out;
    }

    private String save(BufferedImage screen) {

        JFrame jf = new JFrame();
        String selectedFileName;
        // TODO: Switch this to use DialogUtils.chooseFileForSave.
        if (MiscUtils.MAC) {
            FileDialog fd = new FileDialog(jf, "Output File", FileDialog.SAVE);
            fd.setVisible(true);
            jf.setAlwaysOnTop(true);
            // get the path (null if none selected)
            selectedFileName = fd.getFile();
            if (selectedFileName != null) {
                selectedFileName = fd.getDirectory() + selectedFileName;
                if(!selectedFileName.endsWith(".png")){
                    selectedFileName = selectedFileName + ".png";
                }
            }
        } else {
            JFileChooser fd = new JFileChooser(){
                @Override
                public void approveSelection(){
                    File f = getSelectedFile();
                    if(!f.getPath().endsWith(".png")){
                        f = new File(f.getPath() + ".png");
                    }
                    if(f.exists() && getDialogType() == SAVE_DIALOG){
                        int result = JOptionPane.showConfirmDialog(this,"The file exists, overwrite?","Existing file",JOptionPane.YES_NO_OPTION);
                        switch(result){
                            case JOptionPane.YES_OPTION:
                                super.approveSelection();
                                return;
                            case JOptionPane.NO_OPTION:
                                return;
                        }
                    }
                    super.approveSelection();
                }
            };
            fd.setDialogTitle("Output File");
            fd.setSelectedFile(new File("Untitled.png"));
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
            if(!selectedFileName.endsWith(".png")){
                selectedFileName = selectedFileName + ".png";
            }
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
