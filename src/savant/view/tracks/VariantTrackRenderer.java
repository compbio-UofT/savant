/*
 *    Copyright 2011-2012 University of Toronto
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

package savant.view.tracks;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.adapter.GraphPaneAdapter;
import savant.api.data.Record;
import savant.api.data.VariantRecord;
import savant.api.data.VariantType;
import savant.api.util.Resolution;
import savant.exception.RenderingException;
import savant.settings.ColourSettings;
import savant.util.*;


/**
 * Renderer for variant tracks.
 *
 * @author tarkvara
 */
public class VariantTrackRenderer extends TrackRenderer {
    private static final Log LOG = LogFactory.getLog(VariantTrackRenderer.class);

    VariantTrackRenderer() {
    }
    
    @Override
    public void render(Graphics2D g2, GraphPaneAdapter gp) throws RenderingException {

        // Put up an error message if we don't want to do any rendering.
        renderPreCheck();

        DrawingMode mode = (DrawingMode)instructions.get(DrawingInstruction.MODE);

        AxisRange axisRange = (AxisRange)instructions.get(DrawingInstruction.AXIS_RANGE);
        gp.setXRange(axisRange.getXRange());
        gp.setYRange(axisRange.getYRange());

        if (gp.needsToResize()) return;

        switch (mode) {
            case MATRIX:
                renderMatrixMode(g2, gp);
                break;
            case FREQUENCY:
                renderFrequencyMode(g2, gp);
        }
    }
    
    /**
     * Render the data with horizontal blocks for each participant.
     *
     * @param g2 the AWT graphics object to be rendered onto
     * @param gp the GraphPane which we're drawing into
     * @throws RenderingException 
     */
    private void renderMatrixMode(Graphics2D g2, GraphPaneAdapter gp) throws RenderingException {

        double unitHeight = gp.getUnitHeight();
        double unitWidth = gp.getUnitWidth();
        
        ColourScheme cs = (ColourScheme)instructions.get(DrawingInstruction.COLOUR_SCHEME);
        ColourAccumulator accumulator = new ColourAccumulator(cs);

        int participantCount = ((AxisRange)instructions.get(DrawingInstruction.AXIS_RANGE)).getYMax();
        for (Record rec: data) {

            VariantRecord varRec = (VariantRecord)rec;
            double x = gp.transformXPos(varRec.getPosition());
            double y = (participantCount - 1) * unitHeight;
            double w = unitWidth;

            for (int j = 0; j < participantCount; j++) {
                accumulateZygoteShapes(varRec.getVariantsForParticipant(j), accumulator, new Rectangle2D.Double(x, y, w, unitHeight));
                y -= unitHeight;
            }
            recordToShapeMap.put(varRec, new Rectangle2D.Double(x, 0.0, w, unitHeight * participantCount));
        }
        accumulator.fill(g2);

        if (unitHeight > 16.0) {
            String[] participants = (String[])instructions.get(DrawingInstruction.PARTICIPANTS);
            double y = (participants.length - 0.5) * unitHeight + 4.0;
            g2.setColor(ColourSettings.getColor(ColourKey.INTERVAL_TEXT));
            for (int i = 0; i < participants.length; i++) {
                drawFeatureLabel(g2, participants[i], 0.0, y);
                y -= unitHeight;
            }
        }
    }
    
    /**
     * If we're homozygotic, accumulate a rectangle for this variant.  If we're heterozygotic, accumulate a triangle for each parent.
     * @param vars array of one or two variant types
     * @param accumulator a colour accumulator
     * @param rect bounding box used for rendering both zygotes
     */
    public static void accumulateZygoteShapes(VariantType[] vars, ColourAccumulator accumulator, Rectangle2D rect) {
        ColourScheme scheme = accumulator.getScheme();
        if (vars != null) {
            if (vars.length == 1) {
                accumulator.addShape(scheme.getVariantColor(vars[0]), rect);
            } else {
                Color color0 = scheme.getVariantColor(vars[0]);
                Color color1 = scheme.getVariantColor(vars[1]);
                Color blend;
                if (color0 == null) {
                    blend = new Color(color1.getRed(), color1.getGreen(), color1.getBlue(), 128);
                } else if (color1 == null) {
                    blend = new Color(color0.getRed(), color0.getGreen(), color0.getBlue(), 128);
                } else {
                    blend = new Color((color0.getRed() + color1.getRed()) / 2, (color0.getGreen() + color1.getGreen()) / 2, (color0.getBlue() + color1.getBlue()) / 2);
                }
                accumulator.addShape(blend, rect);
            }
        }
    }
    
    /**
     * Render the data as an allele frequency plot.
     *
     * @param g2 the AWT graphics object to be rendered onto
     * @param gp the GraphPane which we're drawing into
     * @throws RenderingException 
     */
    private void renderFrequencyMode(Graphics2D g2, GraphPaneAdapter gp) throws RenderingException {

        ColourScheme cs = (ColourScheme)instructions.get(DrawingInstruction.COLOUR_SCHEME);
        ColourAccumulator accumulator = new ColourAccumulator(cs);

        double unitHeight = gp.getUnitHeight() / (((VariantRecord)data.get(0)).getParticipantCount() * 2.0);
        double unitWidth = gp.getUnitWidth();
        
        for (int i = 0; i < data.size(); i++) {
            VariantRecord varRec = (VariantRecord)data.get(i);
            Pileup pile = calculatePileup(varRec);

            double bottom = gp.transformYPos(0);
            double x = gp.transformXPos(varRec.getPosition());
            recordToShapeMap.put(varRec, new Rectangle2D.Double(x, 0.0, unitWidth, gp.getHeight()));

            VariantType snpNuc;
            while ((snpNuc = pile.getLargestVariantType(VariantType.NONE)) != null) {
                double h = pile.getCoverage(snpNuc, null) * unitHeight;
                Rectangle2D rect = new Rectangle2D.Double(x, bottom - h, unitWidth, h);
                accumulator.addShape(cs.getVariantColor(snpNuc), rect);
                bottom -= h;
                pile.clearVariantType(snpNuc);
            }
        }

        accumulator.fill(g2);
    }
    
    /**
     * Simplified version calculation from AlleleFrequencyPlot, without the case/control distinction.
     */
    private Pileup calculatePileup(VariantRecord varRec) {
        Pileup pile = new Pileup(varRec.getPosition());
        for (int j = 0; j < varRec.getParticipantCount(); j++) {
            VariantType[] jVariants = varRec.getVariantsForParticipant(j);
            if (jVariants.length == 1) {
                pile.pileOn(jVariants[0], 1.0, null);
                pile.pileOn(jVariants[0], 1.0, null);
            } else {
                pile.pileOn(jVariants[0], 1.0, null);
                pile.pileOn(jVariants[1], 1.0, null);
            }
        }
        return pile;
    }
    
    @Override
    public Dimension getLegendSize(DrawingMode ignored) {
        Resolution res = (Resolution)getInstruction(DrawingInstruction.RESOLUTION);
        return res == Resolution.HIGH ? new Dimension(168, LEGEND_LINE_HEIGHT * 3 + 6) : null;
    }
    
    @Override
    public void drawLegend(Graphics2D g2, DrawingMode ignored) {
        int x = 6, y = 17;
        drawBaseLegend(g2, x, y, ColourKey.A, ColourKey.C, ColourKey.G, ColourKey.T);

        y += LEGEND_LINE_HEIGHT;
        g2.setColor(Color.BLACK);
        g2.fillRect(x, y - SWATCH_SIZE.height + 2, SWATCH_SIZE.width, SWATCH_SIZE.height);
        g2.setColor(Color.BLACK);
        g2.drawString("Deletion", x + SWATCH_SIZE.width + 3, y);
        x += 66;
        g2.setColor(Color.MAGENTA);
        g2.fillRect(x, y - SWATCH_SIZE.height + 2, SWATCH_SIZE.width, SWATCH_SIZE.height);
        g2.setColor(Color.BLACK);
        g2.drawString("Insertion", x + 12, y);
        y += LEGEND_LINE_HEIGHT;
        g2.drawString("Translucent = Heterozygous", 6, y);
    }
}
