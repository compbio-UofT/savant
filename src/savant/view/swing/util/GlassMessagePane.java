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
 * GlassMessagePane.java
 * Created on Jan 28, 2010
 */

package savant.view.swing.util;

import savant.settings.BrowserSettings;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import savant.settings.ColourSettings;

public class GlassMessagePane {

    public static void draw(Graphics2D g2, Component gp, String message, int width) {
        
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        Font font = g2.getFont();
        int arc = 0;

        int h = gp.getSize().height/3;
        int w = width;

        if (w > 500)
        {
            font = font.deriveFont(Font.PLAIN,48);
            arc = 20;
        }
        else if (w > 150)
        {
            font = font.deriveFont(Font.PLAIN,24);
            arc = 10;
        }
        else
        {
            font = font.deriveFont(Font.PLAIN,12);
            arc = 3;
        }
        g2.setFont(font);
        FontMetrics metrics = g2.getFontMetrics();

        Rectangle2D stringBounds = font.getStringBounds(message, g2.getFontRenderContext());

        int preferredWidth = (int)stringBounds.getWidth()+metrics.getHeight();
        int preferredHeight = (int)stringBounds.getHeight()+metrics.getHeight();

        w = Math.min(preferredWidth,w);
        h = Math.min(preferredHeight,h);

        int x = (gp.getSize().width - w) / 2;
        int y = (gp.getSize().height - h) / 2;

        //Color vColor = new Color(0, 105, 134, 196);

        //g2.setColor(vColor);
        //g2.fillRoundRect(x, y, w, h, arc, arc);

        g2.setColor(ColourSettings.colorGlassPaneBackground);
        x = (gp.getSize().width - (int)stringBounds.getWidth()) / 2;
        y = (gp.getSize().height / 2) + ((metrics.getAscent()- metrics.getDescent()) / 2);

        g2.drawString(message,x,y);

    }
}
