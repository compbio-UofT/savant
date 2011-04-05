/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.swing.component;

import com.jidesoft.swing.JideButton;
import java.awt.Color;
import java.awt.event.ActionListener;
import javax.swing.JComponent;
import javax.swing.SwingConstants;

/**
 *
 * @author mfiume
 */
public class HyperlinkButton {

    public static JComponent createHyperlinkButton(String name, Color color, ActionListener l) {
        final JideButton button = new JideButton(name);
        button.setButtonStyle(JideButton.HYPERLINK_STYLE);

        button.setForeground(color);
        button.setOpaque(false);

        button.setHorizontalAlignment(SwingConstants.LEADING);

        //button.setRequestFocusEnabled(true);
        //button.setFocusable(true);

        button.addActionListener(l);

        //button.setCursor(Cursor.getPredefinedCursor(Cursor.));
        return button;
    }

}
