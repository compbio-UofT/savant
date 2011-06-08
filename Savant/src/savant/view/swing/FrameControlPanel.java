/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.view.swing;


import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 *
 * @author mfiume
 */
public class FrameControlPanel extends JPanel {

    private int buffer = 3;

    public FrameControlPanel(Frame parent) {
        this.setOpaque(false);
        this.setBorder(new EmptyBorder(buffer,buffer,buffer,buffer));
        this.setLayout(new BoxLayout(this,BoxLayout.X_AXIS));
        //this.add(Box.createHorizontalGlue());
    }

    public void addFrameControl(FrameControl f) {
        if (this.getComponentCount() != 0) { this.add(Box.createHorizontalStrut(buffer)); }
        this.add(f);
    }

}
