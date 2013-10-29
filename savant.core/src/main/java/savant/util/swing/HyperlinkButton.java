/**
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package savant.util.swing;

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
