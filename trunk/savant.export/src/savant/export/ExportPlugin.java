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
import savant.data.event.ExportEvent;
import savant.plugin.PluginAdapter;
import javax.swing.*;
import savant.plugin.SavantPanelPlugin;
import java.awt.*;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
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
import savant.util.Bookmark;
import savant.util.Range;
import savant.view.swing.GraphPane;
import savant.view.swing.Track;
import savant.controller.FrameController;
import savant.controller.RangeController;
import savant.controller.ReferenceController;
import savant.data.event.ExportEventListener;
import savant.swing.component.PathField;
import savant.view.swing.Frame;

public class ExportPlugin extends SavantPanelPlugin {

    //interface
    private PathField pf;
    private JLabel outputLabel;

    //file string
    private String baseFolder = "";
    private String baseFile = "";
    private String indexFile = "";
    
    //export variables
    private int currentBookmark;
    private int numBookmarks;
    private int numFrames;
    private int framesDone;
    private List<Bookmark> bookmarks;
    private BufferedImage[] images;

    //these will be stored to return to later
    private String currentReference;
    private Range currentRange;

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
        JLabel htmlLabel = new JLabel("<html>Choose folder to save files.<br>An html index file will be created here.</html>");
        gbc.gridwidth = 2;
        gbc.gridx = 1;
        gbc.gridy = 1;
        panel.add(htmlLabel, gbc);
        pf = new PathField(JFileChooser.OPEN_DIALOG, false, true);
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

        //create output label
        outputLabel = new JLabel();
        Font f = outputLabel.getFont();
        outputLabel.setFont(f.deriveFont(f.getStyle() ^ Font.BOLD));
        gbc.gridx = 2;
        gbc.gridy = 3;
        panel.add(outputLabel, gbc);

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

        //store current location
        currentReference = ReferenceController.getInstance().getReferenceName();
        currentRange = RangeController.getInstance().getRange();
    
        //output init
        baseFolder = pf.getPath();
        if(baseFolder.equals("")){
            outputLabel.setText("     Please enter a filename for index.");
            return;
        }

        DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
        Date date = new Date();
        baseFile =  baseFolder + System.getProperty("file.separator") + dateFormat.format(date);
        indexFile = baseFile + ".html";
        boolean createdIndex = this.initIndex();
        if(!createdIndex){
            endRun(false);
            return;
        }


        //init bookmarks
        bookmarks = BookmarkController.getInstance().getBookmarks();
        if(bookmarks.isEmpty() || FrameController.getInstance().getFrames().isEmpty()){
            outputLabel.setText("     There is nothing to export.");
            return;
        }

        //store export info
        numFrames = FrameController.getInstance().getFrames().size();
        numBookmarks = bookmarks.size() -1;
        currentBookmark = 0;

        run();
    }

    /*
     * Update the progress of export
     */
    private void updateRunLabel(){
        outputLabel.setText("     Running: " + (currentBookmark+1) + "/" + (numBookmarks+1));
    }

    private void run(){

        updateRunLabel();

        framesDone = 0;
        images = new BufferedImage[numFrames];

        final Bookmark bm = bookmarks.get(currentBookmark);
        final String reference = bm.getReference();
        RangeAdapter rangeAd = bm.getRange();
        final Range range = new Range(rangeAd.getFrom(), rangeAd.getTo());
        String annotation = bm.getAnnotation();

        //TODO: multiple tracks per frame??
        for(int i = 0; i < numFrames; i++){
            final int index = i;
            Frame frame = FrameController.getInstance().getFrames().get(i);

            //track info
            Track t = frame.getTracks()[0];
            final GraphPane gp = frame.getGraphPane();

            ExportEventListener eel = new ExportEventListener() {

                @Override
                public void exportCompleted(ExportEvent evt) {
                    if(evt.getRange().equals(range)){
                        images[index] = evt.getImage();
                        gp.removeExportListener(this);
                        nextBookmark();
                    }
                }
            };
            gp.addExportEventListener(eel);
            
        }
        RangeController.getInstance().setRange(reference, range);
    }

    /*
     * If another bookmark exists, begin exporting it
     */
    public void nextBookmark(){
        framesDone++;
        if(framesDone == numFrames){
            createImage(images, currentBookmark, bookmarks.get(currentBookmark));
            currentBookmark++;
            if(currentBookmark > numBookmarks){
                closeIndex();
                return;
            }
            run();
        }
    }

    public void createImage(BufferedImage[] images, int increment, Bookmark bm){

        int totalWidth = 0;
        int totalHeight = 45;
        for(int i = 0; i <images.length; i++){
            totalWidth = Math.max(totalWidth, images[i].getWidth());
            totalHeight += images[i].getHeight();
        }

         //create output image
        BufferedImage out = new BufferedImage(totalWidth, totalHeight, BufferedImage.TYPE_INT_RGB);

        //write range at top
        RangeAdapter range = bm.getRange();
        long start = range.getFrom();
        long end = range.getTo();
        String toWrite = "Reference: " + bm.getReference() + "    Range:  " + start + " - " + end;
        if(!bm.getAnnotation().equals("")){
            toWrite = toWrite + "    Annotation: " + bm.getAnnotation();
        }
        Graphics2D g = out.createGraphics();
        g.setColor(Color.white);
        g.setFont(new Font(null, Font.BOLD, 13));
        g.drawString(toWrite, 2, 17);

        //copy images to output image
        int outX = 0;
        int outY = 25;
        for(int k = 0; k < images.length; k++){
            BufferedImage current = images[k];
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
        toWrite = "Generated using the Savant Genome Browser - http://genomesavant.com";
        g.setColor(Color.white);
        g.setFont(new Font(null, Font.BOLD, 10));
        g.drawString(toWrite, 2, outY+14);

        //save image
        boolean success = save(out, increment, bm);
        endRun(success);
       
    }
    
    private boolean save(BufferedImage image, int increment, Bookmark bm){
        String filename = this.baseFile + increment + ".png";

        File file = new File(filename);
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
        new File(baseFolder).mkdirs();
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

    private void endRun(boolean success){
        //Return browser to original location
        RangeController.getInstance().setRange(currentReference, currentRange);

        //reset output
        if(success){
            outputLabel.setText("     Created file " + indexFile);
        } else {
            outputLabel.setText("     Export failed. Make sure output path is correct.");
        }
    }

}
