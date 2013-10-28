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
package savant.tool.export;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.*;

import savant.controller.BookmarkController;
import savant.plugin.PluginAdapter;
import savant.experimental.ToolInformation;
import savant.plugin.ToolPlugin;
import savant.settings.DirectorySettings;
import savant.util.Bookmark;
import savant.util.Range;
import savant.view.swing.GraphPane;
import savant.view.swing.Track;
import savant.controller.FrameController;
import savant.controller.ReferenceController;
import savant.swing.component.PathField;
import savant.view.swing.Frame;

/**
 *
 * @author AndrewBrook
 */
public class ExportFromBookmarks extends ToolPlugin {

    //private JTextField baseField;
    private PathField pf;
    private String baseFile = "";
    private String indexFile = "";

    @Override
    public void init(PluginAdapter pluginAdapter) {
        this.setEventSubscriptionEnabled(false);
    }

    @Override
    public ToolInformation getToolInformation() {
        return new ToolInformation(
                "Export Images from Bookmarks",
                "Export",
                "Create images for based on the current loaded bookmarks and open tracks. An html index file is generated to view images.",
                "1.3.2",
                "Andrew Brook",
                "http://andrewbrook.ca");
    }

    @Override
    public JComponent getCanvas() {
        JPanel container = new JPanel();

        //create padding
        container.setLayout(new BorderLayout());
        JPanel fill1 = new JPanel();
        fill1.setPreferredSize(new Dimension(10,10));
        container.add(fill1, BorderLayout.NORTH);
        JPanel fill2 = new JPanel();
        fill2.setPreferredSize(new Dimension(10,10));
        container.add(fill2, BorderLayout.WEST);
        JPanel fill3 = new JPanel();
        fill3.setPreferredSize(new Dimension(10,10));
        container.add(fill3, BorderLayout.EAST);

        //create inner container
        JPanel innerContainer = new JPanel(new BorderLayout());
        container.add(innerContainer, BorderLayout.CENTER);

        //create html file chooser
        JPanel pan1 = new JPanel();
        pan1.setLayout(new BorderLayout());
        JLabel htmlLabel = new JLabel("<html>Choose file to save html index.<br>The index displays links to each saved file.</html>");
        pan1.add(htmlLabel, BorderLayout.NORTH);
        pf = new PathField(JFileChooser.OPEN_DIALOG, true);
        pan1.add(pf, BorderLayout.SOUTH);
        innerContainer.add(pan1, BorderLayout.NORTH);

        //create base filename chooser
        /*JPanel pan2 = new JPanel();
        pan2.setLayout(new BorderLayout());
        JLabel baseLabel = new JLabel("<html><br>Choose a base name for output files.<br>If left blank, the current date/time will be used.<br><br> Filenames will be in the format:<br> base#.png, where # is the bookmark number. </html>");
        pan2.add(baseLabel, BorderLayout.NORTH);
        baseField = new JTextField();
        pan2.add(baseField, BorderLayout.SOUTH);
        innerContainer.add(pan2, BorderLayout.SOUTH);*/

        return container;
    }

    @Override
    public void runTool() throws InterruptedException {

        //output init
        indexFile = pf.getPath();
        if(indexFile.equals("")){
            this.getOutputStream().println("html index file required.");
            return;
        }
        this.getOutputStream().println("Using index file: " + indexFile);
        DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
        Date date = new Date();
        baseFile = dateFormat.format(date);
        this.getOutputStream().println("Base filename: " + baseFile);
        this.initIndex();


        List<Bookmark> bookmarks = BookmarkController.getInstance().getBookmarks();
        List<Frame> frames = FrameController.getInstance().getFrames();

        if(bookmarks.size() == 0 || frames.size() == 0){
            this.getOutputStream().println("No bookmarks or tracks loaded.");
            return;
        }

        for(int i = 0; i < bookmarks.size(); i++){

            this.getOutputStream().print("Exporting bookmark: " + (i + 1) + " of " + bookmarks.size());

            //bookmark info
            Bookmark bm = bookmarks.get(i);
            String reference = bm.getReference();
            Range range = bm.getRange();
            String annotation = bm.getAnnotation();

            //for output
            int totalWidth = 0;
            int totalHeight = 45;
            List<BufferedImage> images = new ArrayList<BufferedImage>();

            for(int j = 0; j < frames.size(); j++){

                //discard initial tracks if there are more than 1
                Frame frame = frames.get(j);
                if(frame.getTracks().size() > 1){
                    for(int k = 0; k < frame.getTracks().size() - 1; k++){
                        try {
                            frame.getTracks().get(k).prepareForRendering(reference, range);
                        } catch (Throwable ex) {
                            Logger.getLogger(ExportFromBookmarks.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        Graphics g = new BufferedImage(1,1,BufferedImage.TYPE_INT_RGB).createGraphics();
                        frame.getGraphPane().render(g, range);
                    }
                }

                //track info
                Track t = frame.getTracks().get(frame.getTracks().size() -1);
                GraphPane gp = frame.getGraphPane();



                //prepare for rendering
                if(!ReferenceController.getInstance().getReferenceName().equals(reference)){
                    ReferenceController.getInstance().setReference(reference);
                }
                try {
                    t.prepareForRendering(reference, range);
                } catch (Throwable ex) {
                    Logger.getLogger(ExportFromBookmarks.class.getName()).log(Level.SEVERE, null, ex);
                }

                //create image
                int width = gp.getWidth();
                int height = gp.getHeight();
                BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = bufferedImage.createGraphics();
                Dimension dim = gp.render(g, range);
                if(!dim.equals(new Dimension(width, height))){
                    width = dim.width;
                    height = dim.height;
                    bufferedImage = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_RGB);
                    g = bufferedImage.createGraphics();
                    gp.render(g, range);
                }

                //add file name
                g.setColor(Color.black);
                g.setFont(new Font(null, Font.BOLD, 13));
                g.drawString(frame.getTracks().get(0).getName(), 2, 15);

                //modify out image size
                totalHeight += height;
                totalWidth = Math.max(totalWidth, width);

                images.add(bufferedImage);

            }

            //create output image
            BufferedImage out = new BufferedImage(totalWidth, totalHeight, BufferedImage.TYPE_INT_RGB);

            //write range at top
            int start = range.getFrom();
            int end = range.getTo();
            String toWrite = "Reference: " + reference + "    Range:  " + start + " - " + end;
            if(!annotation.equals("")){
                toWrite = toWrite + "    Annotation: " + annotation;
            }
            Graphics2D g = out.createGraphics();
            g.setColor(Color.white);
            g.setFont(new Font(null, Font.BOLD, 13));
            g.drawString(toWrite, 2, 17);

            //copy images to output image
            int outX = 0;
            int outY = 25;
            for(int k = 0; k < images.size(); k++){
                BufferedImage current = images.get(k);
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

            //save image
            boolean success = save(out, i, bm);
            if(!success){
                this.getOutputStream().println(" - Could not save!");
            } else {
                this.getOutputStream().println(" - Done");
            }

        }
        this.closeIndex();
    }

    private boolean save(BufferedImage image, int increment, Bookmark bm){
        String outputDir = DirectorySettings.getPluginsDirectory();
        String fileSeparator = System.getProperty("file.separator");
        String filename = outputDir + fileSeparator + this.baseFile + increment + ".png";
        try {
            ImageIO.write(image, "PNG", new File(filename));
        } catch (IOException ex) {
            return false;
        }
        String reference = bm.getReference();
        Range range = bm.getRange();
        String annotation = bm.getAnnotation();
        String text = reference + "   " + range + "   " + annotation;
        addImageToIndex(filename, text);

        return true;
    }

    private boolean initIndex(){
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(this.indexFile, false));
            String s = "<html>";
            out.write(s);
            out.close();
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    private boolean closeIndex(){
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(this.indexFile, true));
            String s = "</html>";
            out.write(s);
            out.close();
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    private boolean addImageToIndex(String filename, String text){
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(this.indexFile, true));
            String s = "<a href=\"" + "file:///" + filename + "\">" + text + "</a><br>";
            out.write(s);
            out.close();
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    @Override
    protected void doStart() throws Exception {
    }

    @Override
    protected void doStop() throws Exception {
    }

}
