/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package savant.view.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;
import savant.settings.BrowserSettings;

public class FrameControl extends JPanel implements MouseListener {

    private Image image;
    private int dim = 15;
    private boolean isInside;
    //private boolean isText = false;
    private final String text;

    private int bend = 15;
    private int buffer = 4;
    private Font textFont = BrowserSettings.getTrackFont();
            

    public FrameControl(Image image) {
        this(image,null);
    }

    public FrameControl(String text) {
        this(null,text);
    }

    private FrameControl(Image image, String text) {
        setImage(image);
        this.text = text;
        this.addMouseListener(this);
        setSize();
    }

    private boolean isText() {
        return image == null;
    }

    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color bgcolor = new Color(255, 255, 255, 150);
        Color outlineColor = new Color(0,0,0,0);// Color.darkGray;
        Color textColor = Color.darkGray;
        if (isInside) {
            bgcolor = new Color(200, 200, 200, 150);
        }

        g2.setColor(bgcolor);

        if (isText()) {

            g2.setFont(textFont);
            g2.fillRoundRect(0, 0, this.getWidth(), dim, bend, bend);
            g2.setColor(outlineColor);
            g2.drawRoundRect(0, 0, this.getWidth(), dim, bend, bend);
            g2.setColor(textColor);
            g2.drawString(text, buffer, dim-buffer);

        } else {
            g2.fillOval(0, 0, dim - 1, dim - 1);
            g2.setColor(outlineColor);
            g2.drawOval(0, 0, dim - 1, dim - 1);

            int imwidth = dim-buffer-1;
            int imheight = dim-buffer-1;

            //Image temp1 = image.getScaledInstance(imwidth, imheight, Image.SCALE_SMOOTH);
            //g2.drawImage(temp1, this.getWidth()/2-imwidth/2, this.getHeight()/2-imheight/2, null);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            //g2.drawImage(image, buffer, buffer, dim-1-2*buffer, dim-1-2*buffer, this);
            g2.drawImage(image, (this.getWidth())/2-imwidth/2, (this.getHeight())/2-imheight/2, imwidth, imheight, this);


        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        isInside = true;
    }

    @Override
    public void mouseExited(MouseEvent e) {
        isInside = false;
    }

    private void setImage(Image image) {

        this.image = image;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    private void setSize() {
        if (this.isText()) {
            BufferedImage bi = new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = bi.createGraphics();
            FontMetrics fm = g2.getFontMetrics();
            int w = fm.stringWidth(text) + 2*buffer + 14;
            Rectangle2D r = fm.getStringBounds(text, g2);
            Dimension d = new Dimension(w, dim);
            this.setMinimumSize(d);
            this.setPreferredSize(d);
            this.setMaximumSize(d);
        } else {
            this.setPreferredSize(new Dimension(dim, dim));
            this.setMaximumSize(new Dimension(dim, dim));
        }
    }

}
