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

package savant.view.swing;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
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
import savant.util.DataFormatUtils;


/**
 *
 * @author AndrewBrook
 */
public class ExportImageDialog extends javax.swing.JDialog {

    private Object bean;
    private List<JCheckBox> checkBoxes = new ArrayList<JCheckBox>();

    /** Creates new customizer ExportImageDialog */
    public ExportImageDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);

        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setTitle("Export Frames as Image");

        final List<Frame> frames = FrameController.getInstance().getFrames();
        setLayout(new BorderLayout());
        JLabel instructions = new JLabel("  Select which tracks you would like to include:  ");
        instructions.setPreferredSize(new Dimension(300,40));
        this.add(instructions, BorderLayout.NORTH);


        JPanel listPanel = new JPanel();
        listPanel.setLayout(new GridLayout(frames.size(), 1));
        for(int i = 0; i <frames.size(); i++){
            JCheckBox checkBox = new javax.swing.JCheckBox();
            checkBox.setPreferredSize(new Dimension(300,30));
            checkBox.setBackground(Color.white);
            checkBox.setText(frames.get(i).getTracks().get(0).getName());
            checkBox.setSelected(true);
            listPanel.add(checkBox, java.awt.BorderLayout.CENTER);
            checkBoxes.add(checkBox);
        }
        this.add(listPanel, BorderLayout.CENTER);
        
        JButton exportButton = new JButton("Export...");
        exportButton.addMouseListener(new MouseListener() {
            public void mouseClicked(MouseEvent e) {                

                List<BufferedImage> images = new ArrayList<BufferedImage>();
                int totalWidth = 0;
                int totalHeight = 45;
                for(int i = 0; i <frames.size(); i++){
                    if(checkBoxes.get(i).isSelected()){
                        BufferedImage im = frames.get(i).frameToImage();
                        images.add(im);
                        totalWidth = Math.max(totalWidth, im.getWidth());
                        totalHeight += im.getHeight();
                    }
                }
                //no frames selected
                if(images.size() == 0) return;

                BufferedImage out = new BufferedImage(totalWidth, totalHeight, BufferedImage.TYPE_INT_RGB);


                //write range at top
                RangeController range = RangeController.getInstance();
                int start = range.getRangeStart();
                int end = range.getRangeEnd();
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
            public void mousePressed(MouseEvent e) {}
            public void mouseReleased(MouseEvent e) {}
            public void mouseEntered(MouseEvent e) {}
            public void mouseExited(MouseEvent e) {}
        });
        JPanel exportLabel = new JPanel();
        exportLabel.setPreferredSize(new Dimension(300,40));
        exportButton.setPreferredSize(new Dimension(100,30));
        exportLabel.add(exportButton);
        this.add(exportLabel, BorderLayout.SOUTH);//add(exportLabel);

        //add(exportButton);

        pack();

    }    

    public void setObject(Object bean) {
        this.bean = bean;
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

                        String extension = DataFormatUtils.getExtension(f.getAbsolutePath());
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
                exit();
            } catch (IOException ex) {
                String message = "Screenshot unsuccessful";
                String title = "Uh oh...";
                // display the JOptionPane showConfirmDialog
                JOptionPane.showConfirmDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
                exit();
            }
        }

        return null;
    }

    private void exit(){
        this.dispose();
    }


    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the FormEditor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setPreferredSize(new java.awt.Dimension(500, 500));
        setLayout(new java.awt.BorderLayout());
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables

}
