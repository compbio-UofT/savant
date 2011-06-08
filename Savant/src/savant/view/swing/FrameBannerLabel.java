/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.view.swing;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Rectangle2D;
import javax.swing.JPanel;
import savant.settings.BrowserSettings;

/**
 *
 * @author mfiume
 */
class FrameBannerLabel extends JPanel implements MouseListener, MouseMotionListener {

    String text;
    Font f = BrowserSettings.getTrackFont();
    
    public FrameBannerLabel() {
        //this.addMouseMotionListener(this);
        //this.addMouseListener(this);
        this.setToolTipText("Click and drag to reorder track");
    }

    int padding = 2;
    int buffer = 2;

    boolean isInside = false;
    boolean lastInside = !isInside;

    public void paintComponent(Graphics g) {

        if (text == null) { return; }

        //if (lastInside == isInside) { return; }
        //lastInside = isInside;

        System.out.println("Painting Frame Banner " + this);

        g.setFont(f);

        FontMetrics fm = g.getFontMetrics();
        Rectangle2D r = fm.getStringBounds(text, g);

        this.setBounds(0,0,fm.stringWidth(text)+padding+2*buffer+1,(int)Math.ceil(r.getHeight())+padding+buffer+3);

        Graphics2D g2 = (Graphics2D)g;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (isInside) {
            g2.setColor(new Color(212, 241, 255));
           //g2.setColor(new Color(212, 241, 255,100));
        }
        else {
            g2.setColor(new Color(255,255,255));
            //g2.setColor(new Color(255,255,255,100));
        }
        g2.fillRoundRect(padding, padding, (int) r.getWidth()+2*buffer, (int)fm.getHeight()+2*buffer, 10,10);

        g2.setColor(Color.darkGray);
        g2.drawRoundRect(padding, padding, (int) r.getWidth()+2*buffer, (int)fm.getHeight()+2*buffer, 10,10);
        g2.drawString(text, padding+buffer, padding+buffer+(int)fm.getHeight()-fm.getMaxDescent());

        //g.drawString(text, padding, (int) r.getHeight() - fm.getMaxDescent() + padding);
    }

    void setText(String name) {
        this.text = name;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
                this.isInside = true;
                //repaint();
    }

    @Override
    public void mouseExited(MouseEvent e) {
        this.isInside = false;
        //repaint();
    }
}
