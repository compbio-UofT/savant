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
package savant.view.swing;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

import savant.api.util.DialogUtils;
import savant.controller.FrameController;
import savant.controller.GenomeController;
import savant.controller.GraphPaneController;
import savant.controller.LocationController;
import savant.util.FileExtensionFilter;
import savant.util.swing.TrackChooser;
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

        TrackChooser tc = new TrackChooser(DialogUtils.getMainWindow(), true, "Select Tracks to Export", true, defaultBase);
        tc.setVisible(true);
        String[] trackNames = tc.getSelectedTracks();
        int base = tc.getBaseSelected();

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


        //write lc at top
        LocationController lc = LocationController.getInstance();
        int start = lc.getRangeStart();
        int end = lc.getRangeEnd();
        String toWrite = "Genome:  " + GenomeController.getInstance().getGenome().getName() + "    Reference:  " + lc.getReferenceName() + "    Range:  " + start + " - " + end;
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

    private void save(BufferedImage screen) {

        File selectedFile = DialogUtils.chooseFileForSave("Output File", "Untitled.png", new FileExtensionFilter("Image files", "png"), null);

        // set the genome
        if (selectedFile != null) {
            String selectedPath = selectedFile.getAbsolutePath();
            if (!MiscUtils.getExtension(selectedPath).equals("png")) {
                selectedFile = new File(selectedPath + ".png");
            }
            try {
                ImageIO.write(screen, "PNG", selectedFile);
            } catch (IOException ex) {
                DialogUtils.displayException("Sorry", "Unable to take screenshot.", ex);
            }
        }
    }
}
