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

package savant.view.swing.sequence;

import savant.model.view.ColorScheme;
import savant.model.view.DrawingInstructions;
import savant.util.Range;
import savant.view.swing.GraphPane;
import savant.view.swing.TrackRenderer;
import savant.view.swing.util.GlassMessagePane;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 * Class to draw a sequence track as alternating bars of colour with base letters in the center.
 * @author mfiume
 */
public class SequenceTrackRenderer extends TrackRenderer {

    private static final Font SMALL_FONT = new Font("Sans-Serif", Font.PLAIN, 12);
    private static final Font LARGE_FONT = new Font("Sans-Serif", Font.PLAIN, 36);

    private static final int GLASS_PANE_WIDTH = 300;
    private static final String GLASS_PANE_MESSAGE = "zoom in to see sequence";

    public SequenceTrackRenderer() { this(new DrawingInstructions());}

    public SequenceTrackRenderer(
            DrawingInstructions drawingInstructions) {
        super(drawingInstructions);
    }

    @Override
    public void render(Graphics g, GraphPane gp) {

        Graphics2D g2 = (Graphics2D) g;
        
        gp.setIsOrdinal(true);
        
        double unitWidth = gp.getUnitWidth();
        double unitHeight = gp.getUnitHeight();

        List<Object> data = this.getData();

        // Don't display sequence if data is too high resolution to see.
        if (data == null || unitWidth < 0.2) {

            // display informational glass pane
            renderGlassPane(g2, gp);
            return;
        }

        String sequence = (String) this.getData().get(0);

        // Set the font size, if base name is renderable at all.
        boolean baseRenderable = false;
        if (fontFits(LARGE_FONT, unitWidth, g2)) {
            g2.setFont(LARGE_FONT);
            baseRenderable = true;
        }
        else if (fontFits(SMALL_FONT, unitWidth, g2)) {
            g2.setFont(SMALL_FONT);
            baseRenderable = true;
        }

        int len = sequence.length();
        for (int i = 0; i < len; i++) {
            double x = (i*unitWidth);
            double y = 0;//gp.transformYPos(1);
            double w = unitWidth;
            double h = unitHeight;

            Rectangle2D.Double rect = new Rectangle2D.Double(x, y, w, h);

            ColorScheme colorScheme = (ColorScheme)getDrawingInstructions().getInstruction(DrawingInstructions.InstructionName.COLOR_SCHEME);
            Color c = colorScheme.getColor("BACKGROUND");
            switch(sequence.charAt(i)) {

                case 'A':
                    c = colorScheme.getColor("A_BACKGROUND");
                    break;
                case 'T':
                    c = colorScheme.getColor("T_BACKGROUND");
                    break;
                case 'G':
                    c = colorScheme.getColor("G_BACKGROUND");
                    break;
                case 'C':
                    c = colorScheme.getColor("C_BACKGROUND");
                    break;
                default:
                    break;
            }

            g2.setColor(c);
            g2.fill(rect);

            if (w > 5) {
                g2.draw(rect);
            }

            String base = Character.toString(sequence.charAt(i));

            if (baseRenderable) {
                float charX, charY;
                Font font = g2.getFont();
                Rectangle2D charRect = font.getStringBounds(base, g2.getFontRenderContext());
                // center the font rectangle in the coloured sequence rectangle
                charX = (float) (rect.getX() + (rect.getWidth() - charRect.getWidth())/2);
                charY = (float) (rect.getY() + charRect.getHeight() + (rect.getHeight() - charRect.getHeight())/2);
                // draw character
                g2.setColor(Color.black);
                g2.drawString(base, charX, charY);

            }
            
            //g.drawRect(x, y, w, h);
            //g.drawRect(i*10,i*10,i*100,i*100);
        }
    }

    private boolean fontFits(Font font, double width, Graphics2D g2) {

        String baseChar = "G";
        Rectangle2D charRect = font.getStringBounds(baseChar, g2.getFontRenderContext());
        if (charRect.getWidth() > width) return false;
        else return true;
    }

    private void renderGlassPane(Graphics2D g2, GraphPane gp) {

        GlassMessagePane.draw(g2, gp, GLASS_PANE_MESSAGE, GLASS_PANE_WIDTH);

    }

    @Override
    public boolean isOrdinal() {
        return true;
    }

    @Override
    public Range getDefaultYRange() {
        return new Range(0,1);
    }
}
