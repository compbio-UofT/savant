/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.chrome;

import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JPanel;
import org.biojava.bio.chromatogram.Chromatogram;
import org.biojava.bio.chromatogram.graphic.ChromatogramGraphic;

/**
 *
 * @author mfiume
 */
class ChromePanel extends JPanel {

    Chromatogram c;

    public ChromePanel(Chromatogram c) {
        this.c = c;
        this.setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        ChromatogramGraphic cg = (new ChromatogramGraphic(c));
        cg.setHeight(this.getHeight());
        cg.setWidth(this.getWidth()*10);
        cg.drawTo((Graphics2D) g);
    }
}
