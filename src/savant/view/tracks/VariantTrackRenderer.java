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
import java.awt.geom.Rectangle2D;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.adapter.GraphPaneAdapter;
import savant.api.data.Record;
import savant.api.data.VariantRecord;
import savant.api.event.DataRetrievalEvent;
import savant.exception.RenderingException;
import savant.util.AxisRange;
import savant.util.ColourAccumulator;
import savant.util.ColourKey;
import savant.util.ColourScheme;
import savant.util.DrawingInstruction;
import savant.util.DrawingMode;


/**
 * Renderer for variant tracks.
 *
 * @author tarkvara
 */
class VariantTrackRenderer extends TrackRenderer {
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
                if (evt.getData().size() > 0) {
                    int xMax = 1;
                    if (getInstruction(DrawingInstruction.MODE) == DrawingMode.MATRIX) {
                        VariantRecord rec0 = (VariantRecord)evt.getData().get(0);
                        xMax = rec0.getParticipantCount();
                    }
                    addInstruction(DrawingInstruction.AXIS_RANGE, new AxisRange(1, xMax, 1, evt.getData().size()));
                    LOG.info("DataRetrievalEvent set y-range to " + ((AxisRange)getInstruction(DrawingInstruction.AXIS_RANGE)).getYRange());
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

        switch (mode) {
            case STANDARD:
                renderStandardMode(g2, gp);
                break;
            case MATRIX:
                renderMatrixMode(g2, gp);
                break;
        }
    }
    
    /**
     * Render the data in a form similar to the BAM track's SNP mode, but condensed by removing non-variant bases.
     *
     * @param g2 the AWT graphics object to be rendered onto
     * @param gp the GraphPane which we're drawing into
     * @throws RenderingException 
     */
    private void renderStandardMode(Graphics2D g2, GraphPaneAdapter gp) throws RenderingException {

        double h = gp.getUnitHeight();
        double w = gp.getWidth();
        
        ColourScheme cs = (ColourScheme)instructions.get(DrawingInstruction.COLOUR_SCHEME);
        ColourAccumulator accumulator = new ColourAccumulator(cs);

        double i = 0.0;
        for (Record rec: data) {
    
            VariantRecord varRec = (VariantRecord)rec;
            Rectangle2D rect = new Rectangle2D.Double(0.0, i * h, w, h);            
            switch (varRec.getVariantType()) {
                case INSERTION:
                    // Because the scaling of a VCF track is not base-based, it doesn't make sense
                    // to draw insertions using the rhombus we employ elsewhere.
                    accumulator.addShape(ColourKey.INSERTED_BASE, rect);
                    break;
                case DELETION:
                    accumulator.addShape(ColourKey.DELETED_BASE, rect);
                    break;
                case SNP:
                    accumulator.addBaseShape(varRec.getAltBases().charAt(0), rect);
                    break;
            }
            recordToShapeMap.put(varRec, rect);
            i++;
        }
        accumulator.render(g2);
    }

    /**
     * Render the data with horizontal blocks for each participant.
     *
     * @param g2 the AWT graphics object to be rendered onto
     * @param gp the GraphPane which we're drawing into
     * @throws RenderingException 
     */
    private void renderMatrixMode(Graphics2D g2, GraphPaneAdapter gp) throws RenderingException {

        double h = gp.getUnitHeight();
        double w = gp.getUnitWidth();
        
        ColourScheme cs = (ColourScheme)instructions.get(DrawingInstruction.COLOUR_SCHEME);
        ColourAccumulator accumulator = new ColourAccumulator(cs);

        int participantCount = ((VariantRecord)data.get(0)).getParticipantCount();
        double i = 0.0;
        for (Record rec: data) {

            VariantRecord varRec = (VariantRecord)rec;
            
            for (int j = 0; j < participantCount; j++) {
                Rectangle2D rect = new Rectangle2D.Double(j * w, i * h, w, h);
                switch (varRec.getVariantForParticipant(j)) {
                    case INSERTION:
                        // Because the scaling of a VCF track is not base-based, it doesn't make sense
                        // to draw insertions using the rhombus we employ elsewhere.
                        accumulator.addShape(ColourKey.INSERTED_BASE, rect);
                        break;
                    case DELETION:
                        accumulator.addShape(ColourKey.DELETED_BASE, rect);
                        break;
                    case SNP:
                        accumulator.addBaseShape(varRec.getAltBases().charAt(0), rect);
                        break;
                }
            }
            recordToShapeMap.put(varRec, new Rectangle2D.Double(0, i * h, w * participantCount, h));
            i++;
        }
        accumulator.render(g2);
    }
}
