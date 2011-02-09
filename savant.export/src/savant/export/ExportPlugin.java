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

/*
 * @author AndrewBrook
 */

package savant.export;

import java.awt.event.MouseEvent;
import savant.plugin.PluginAdapter;

import javax.swing.*;
import savant.plugin.SavantPanelPlugin;

import java.awt.*;
import java.awt.event.MouseListener;
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
import savant.api.adapter.RangeAdapter;
import savant.controller.BookmarkController;
import savant.settings.DirectorySettings;
import savant.util.Bookmark;
import savant.util.Range;
import savant.view.swing.GraphPane;
import savant.view.swing.Track;
import savant.controller.FrameController;
import savant.controller.ReferenceController;
import savant.swing.component.PathField;
import savant.view.swing.Frame;

public class ExportPlugin extends SavantPanelPlugin {

    private PathField pf;
    private String baseFile = "";
    private String indexFile = "";

    @Override
    public void init(JPanel canvas, PluginAdapter pluginAdapter) {
        setupGUI(canvas);
    }

    @Override
    protected void doStart() throws Exception {

    }

    @Override
    protected void doStop() throws Exception {

    }

    @Override
    public String getTitle() {
        return "Export Plugin";
    }

    private void setupGUI(JPanel panel) {


        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc= new GridBagConstraints();

        //create padding
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        JPanel fill1 = new JPanel();
        fill1.setPreferredSize(new Dimension(10,10));
        panel.add(fill1, gbc);

        //create path chooser
        JLabel htmlLabel = new JLabel("<html>Choose file to save html index.<br>The index displays links to each saved file.</html>");
        gbc.gridwidth = 2;
        gbc.gridx = 1;
        gbc.gridy = 1;
        panel.add(htmlLabel, gbc);
        pf = new PathField(JFileChooser.OPEN_DIALOG, false, false);
        gbc.gridx = 1;
        gbc.gridy = 2;
        panel.add(pf, gbc);

        //create run button
        JButton runButton = new JButton("Run");
        runButton.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    runTool();
                } catch (InterruptedException ex) {
                    //TODO: deal with exception?
                    Logger.getLogger(ExportPlugin.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            public void mousePressed(MouseEvent e) {}
            public void mouseReleased(MouseEvent e) {}
            public void mouseEntered(MouseEvent e) {}
            public void mouseExited(MouseEvent e) {}
        });
        gbc.weightx = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.gridx = 1;
        gbc.gridy = 3;
        panel.add(runButton, gbc);

        //create padding
        JPanel fill2 = new JPanel();
        fill2.setPreferredSize(new Dimension(10,10));
        gbc.weightx = 1.0;
        gbc.gridwidth = 1;
        gbc.gridx = 2;
        gbc.gridy = 3;
        panel.add(fill2, gbc);

        JPanel fill3 = new JPanel();
        fill3.setPreferredSize(new Dimension(10,10));
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.gridwidth = 2;
        gbc.gridx = 0;
        gbc.gridy = 4;
        panel.add(fill3, gbc);
        
    
    }

    public void runTool() throws InterruptedException {

        //output init
        indexFile = pf.getPath();
        if(indexFile.equals("")){
            //this.getOutputStream().println("html index file required.");
            return;
        }
        //this.getOutputStream().println("Using index file: " + indexFile);
        DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
        Date date = new Date();
        baseFile = dateFormat.format(date);
       // this.getOutputStream().println("Base filename: " + baseFile);
        this.initIndex();

        

        List<Bookmark> bookmarks = BookmarkController.getInstance().getBookmarks();
        //Frame frames = FrameController.getInstance().getFrames().get(0);
        //List<Track> tracks = TrackController.getInstance().getTracks();
        //List<Frame> frames1 = FrameController.getInstance().getFrames();

        if(bookmarks.isEmpty() || FrameController.getInstance().getFrames().isEmpty()){
            //this.getOutputStream().println("No bookmarks or tracks loaded.");
            return;
        }

        for(int i = 0; i < bookmarks.size(); i++){

           // this.getOutputStream().print("Exporting bookmark: " + (i + 1) + " of " + bookmarks.size());

            //bookmark info
            Bookmark bm = bookmarks.get(i);
            String reference = bm.getReference();
            //Range range = bm.getRange();
            RangeAdapter rangeAd = bm.getRange();
            Range range = new Range(rangeAd.getFrom(), rangeAd.getTo());
            String annotation = bm.getAnnotation();

            //for output
            int totalWidth = 0;
            int totalHeight = 45;
            List<BufferedImage> images = new ArrayList<BufferedImage>();

            for(int j = 0; j < FrameController.getInstance().getFrames().size(); j++){

                //discard initial tracks if there are more than 1
                Frame frame = FrameController.getInstance().getFrames().get(j);
                if(frame.getTracks().length > 1){
                    for(int k = 0; k < frame.getTracks().length - 1; k++){
                        try {
                            frame.getTracks()[k].prepareForRendering(reference, range);
                        } catch (Throwable ex) {
                            Logger.getLogger(ExportPlugin.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        Graphics g = new BufferedImage(1,1,BufferedImage.TYPE_INT_RGB).createGraphics();
                        frame.getGraphPane().render(g, range);
                    }
                }

                //track info
                Track t = frame.getTracks()[frame.getTracks().length -1];
                GraphPane gp = frame.getGraphPane();



                //prepare for rendering
                if(!ReferenceController.getInstance().getReferenceName().equals(reference)){
                    ReferenceController.getInstance().setReference(reference);
                }
                try {
                    t.prepareForRendering(reference, range);
                } catch (Throwable ex) {
                    Logger.getLogger(ExportPlugin.class.getName()).log(Level.SEVERE, null, ex);
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
                g.drawString(frame.getTracks()[0].getName(), 2, 15);

                //modify out image size
                totalHeight += height;
                totalWidth = Math.max(totalWidth, width);

                images.add(bufferedImage);

            }

            //create output image
            BufferedImage out = new BufferedImage(totalWidth, totalHeight, BufferedImage.TYPE_INT_RGB);

            //write range at top
            long start = range.getFrom();
            long end = range.getTo();
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
               // this.getOutputStream().println(" - Could not save!");
            } else {
             //   this.getOutputStream().println(" - Done");
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
        //Range range = bm.getRange();
        RangeAdapter range = bm.getRange();
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
    
}
