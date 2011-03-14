/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.swing.component;

import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.JPanel;

/**
 *
 * @author mfiume
 */
public class AlignedComponent extends JPanel {

    public AlignedComponent(Component c, String borderLayoutPosition) {
        this.setLayout(new BorderLayout());
        this.add(c, borderLayoutPosition);
    }

}
