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

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.adapter.GraphPaneAdapter;
import savant.api.data.Record;
import savant.api.data.VariantRecord;
import savant.api.data.VariantType;
import savant.api.event.DataRetrievalEvent;
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
    
    /**
     * We won't know our axis range until we've fetched our data.
     * @param evt 
     */
    @Override
    public void handleEvent(DataRetrievalEvent evt) {
        switch (evt.getType()) {
            case COMPLETED:
                if (evt.getData() != null && evt.getData().size() > 0) {
                    int yMax = ((VariantTrack)evt.getTrack()).getParticipantCount();
                    AxisRange oldRange = (AxisRange)getInstruction(DrawingInstruction.AXIS_RANGE);
                    addInstruction(DrawingInstruction.AXIS_RANGE, new AxisRange(oldRange.getXRange(), new Range(0, yMax)));
                }
                break;
        }
        super.handleEvent(evt);
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

        // Right now the only mode is matrix.
        renderMatrixMode(g2, gp);
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
            double x = gp.transformXPos(varRec.getInterval().getStart());
            double y = (participantCount - 1) * unitHeight;
            double w = unitWidth * varRec.getInterval().getLength();

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
        if (vars.length == 1) {
            accumulateShape(vars[0], accumulator, rect);
        } else {
//            accumulateShape(vars[0], accumulator, MiscUtils.createPolygon(rect.getX(), rect.getY(), rect.getMaxX(), rect.getY(), rect.getMaxX(), rect.getMaxY()));
//            accumulateShape(vars[1], accumulator, MiscUtils.createPolygon(rect.getX(), rect.getY(), rect.getMaxX(), rect.getMaxY(), rect.getX(), rect.getMaxY()));
            accumulateShape(vars[0], accumulator, new Rectangle2D.Double(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight() * 0.5));
            accumulateShape(vars[1], accumulator, new Rectangle2D.Double(rect.getX(), rect.getCenterY(), rect.getWidth(), rect.getHeight() * 0.5));
        }
    }

    public static void accumulateShape(VariantType var, ColourAccumulator accumulator, Shape shape) {
        switch (var) {
            case SNP_A:
                accumulator.addBaseShape('A', shape);
                break;
            case SNP_C:
                accumulator.addBaseShape('C', shape);
                break;
            case SNP_G:
                accumulator.addBaseShape('G', shape);
                break;
            case SNP_T:
                accumulator.addBaseShape('T', shape);
                break;
            case INSERTION:
                accumulator.addShape(ColourKey.INSERTED_BASE, shape);
                break;
            case DELETION:
                accumulator.addShape(ColourKey.DELETED_BASE, shape);
                break;
        }
    }
}
