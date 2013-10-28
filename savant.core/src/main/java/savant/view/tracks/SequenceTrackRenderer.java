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
package savant.view.tracks;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.io.UnsupportedEncodingException;

import savant.api.adapter.GraphPaneAdapter;
import savant.api.data.SequenceRecord;
import savant.api.util.Resolution;
import savant.exception.RenderingException;
import savant.util.*;


/**
 * Class to draw a sequence track as alternating bars of colour with base letters in the center.
 *
 * @author mfiume
 */
public class SequenceTrackRenderer extends TrackRenderer {

    private static final Font SMALL_BASE_FONT = new Font("Sans-Serif", Font.PLAIN, 12);
    private static final Font LARGE_BASE_FONT = new Font("Sans-Serif", Font.PLAIN, 36);

    public SequenceTrackRenderer() {
    }

    @Override
    public void render(Graphics2D g2, GraphPaneAdapter gp) throws RenderingException {

        renderPreCheck();
        
        double unitWidth = gp.getUnitWidth();

        byte[] sequence = ((SequenceRecord)data.get(0)).getSequence();

        // Set the font size, if base name is renderable at all.
        boolean baseRenderable = false;
        if (fontFits(LARGE_BASE_FONT, unitWidth, g2)) {
            g2.setFont(LARGE_BASE_FONT);
            baseRenderable = true;
        } else if (fontFits(SMALL_BASE_FONT, unitWidth, g2)) {
            g2.setFont(SMALL_BASE_FONT);
            baseRenderable = true;
        }

        AxisRange axisRange = (AxisRange)instructions.get(DrawingInstruction.AXIS_RANGE);
        ColourScheme cs = (ColourScheme)instructions.get(DrawingInstruction.COLOUR_SCHEME);
        ColourAccumulator accumulator = new ColourAccumulator(cs);

        int len = sequence.length;
        for (int i = 0; i < len; i++) {
            double x = gp.transformXPos(axisRange.getXMin() + i);
            accumulator.addBaseShape((char)sequence[i], new Rectangle2D.Double(x, 0.0, unitWidth, gp.getHeight()));
        }
        accumulator.fill(g2);
        
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(0.25f));
        for (int i = 0; i < len; i++) {
            double x = gp.transformXPos(axisRange.getXMin()+i);
            double y = 0.0;
            Rectangle2D.Double rect = new Rectangle2D.Double(x, y, unitWidth, gp.getHeight());
            if (unitWidth > 12.0) {
                g2.draw(rect);
            }

            try {
                String base = new String(sequence, i, 1, "ISO-8859-1");

                if (baseRenderable) {
                    Font font = g2.getFont();
                    Rectangle2D charRect = font.getStringBounds(base, g2.getFontRenderContext());
                    // center the font rectangle in the coloured sequence rectangle
                    float charX = (float) (rect.getX() + (rect.getWidth() - charRect.getWidth())/2);
                    float charY = (float) ((rect.getHeight()-5)/2 + g2.getFontMetrics().getAscent()/2);
                    // draw character
                    g2.drawString(base, charX, charY);

                }
            } catch (UnsupportedEncodingException ignored) {
            }
        }
    }

    private boolean fontFits(Font font, double width, Graphics2D g2) {

        String baseChar = "G";
        Rectangle2D charRect = font.getStringBounds(baseChar, g2.getFontRenderContext());
        if (charRect.getWidth() > width) return false;
        else return true;
    }
    
    @Override
    public Dimension getLegendSize(DrawingMode ignored) {
        Resolution res = (Resolution)getInstruction(DrawingInstruction.RESOLUTION);
        return res == Resolution.HIGH ? new Dimension(150, 24) : null;
    }
    
    @Override
    public void drawLegend(Graphics2D g2, DrawingMode ignored) {
        drawBaseLegend(g2, 6, 17, ColourKey.A, ColourKey.C, ColourKey.G, ColourKey.T, ColourKey.N);
    }
}
