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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.io.UnsupportedEncodingException;
import java.util.List;

import savant.data.types.Record;
import savant.data.types.SequenceRecord;
import savant.file.DataFormat;
import savant.util.AxisRange;
import savant.util.ColorScheme;
import savant.util.DrawingInstructions;
import savant.util.Range;
import savant.view.swing.GraphPane;
import savant.view.swing.TrackRenderer;
import savant.view.swing.util.GlassMessagePane;


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
        this.dataType = DataFormat.SEQUENCE_FASTA;
    }

    @Override
    public void render(Graphics g, GraphPane gp) {

        Graphics2D g2 = (Graphics2D) g;

        gp.setIsOrdinal(true);

        Boolean refexists = (Boolean) this.getDrawingInstructions().getInstruction(DrawingInstructions.InstructionName.REFERENCE_EXISTS);
        if (!refexists) {
            GlassMessagePane.draw(g2, gp, "no data for reference", GLASS_PANE_WIDTH);
            return;
        }

        double unitWidth = gp.getUnitWidth();
        double unitHeight = gp.getUnitHeight();


        List<Record> data = getData();
        
        // Don't display sequence if data is too high resolution to see.
        if (data == null || unitWidth < 0.2) {
            // display informational glass pane
            renderGlassPane(g2, gp);
            return;
        }

        byte[] sequence = ((SequenceRecord)getData().get(0)).getSequence();

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

        AxisRange axisRange = (AxisRange) getDrawingInstructions().getInstruction(DrawingInstructions.InstructionName.AXIS_RANGE);

        int len = sequence.length;
        for (int i = 0; i < len; i++) {
            double x = gp.transformXPos(axisRange.getXMin()+i);
            double y = 0;//gp.transformYPos(1);
            double w = gp.getUnitWidth();
            double h = gp.getUnitHeight();

            Rectangle2D.Double rect = new Rectangle2D.Double(x, y, w, h);

            ColorScheme colorScheme = (ColorScheme)getDrawingInstructions().getInstruction(DrawingInstructions.InstructionName.COLOR_SCHEME);
            Color c = colorScheme.getColor("Background");
            switch (sequence[i]) {

                case 'A':
                    c = colorScheme.getColor("A");
                    break;
                case 'T':
                    c = colorScheme.getColor("T");
                    break;
                case 'G':
                    c = colorScheme.getColor("G");
                    break;
                case 'C':
                    c = colorScheme.getColor("C");
                    break;
                case 'N':
                    c = colorScheme.getColor("N");
                    break;
                default:
                    break;
            }

            g2.setColor(c);
            g2.fill(rect);

            if (w > 5) {
                g2.draw(rect);
            }

            try {
                String base = new String(sequence, i, 1, "ISO-8859-1");

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
