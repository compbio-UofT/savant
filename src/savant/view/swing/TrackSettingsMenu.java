/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.view.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;

/**
 *
 * @author mfiume
 */
class TrackSettingsMenu extends JPanel {

    private static Dimension d = new Dimension(20,20);

    public TrackSettingsMenu() {
        this.setPreferredSize(d);
        this.setMinimumSize(d);
        this.setMaximumSize(d);
    }

    @Override
    public void paintComponent(Graphics g) {

        ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(255,255,255,230));
        g.fillOval(0, 0, this.getWidth(), this.getHeight());
        //g.fillRect(0, 0, this.getWidth(), this.getHeight());
        
    }



}
