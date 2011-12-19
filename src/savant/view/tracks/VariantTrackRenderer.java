/*
 *    Copyright 2011 University of Toronto
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
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import savant.api.adapter.GraphPaneAdapter;
import savant.api.data.Record;
import savant.api.data.VariantRecord;
import savant.api.event.DataRetrievalEvent;
import savant.controller.LocationController;
import savant.exception.RenderingException;
import savant.util.AxisRange;
import savant.util.ColourAccumulator;
import savant.util.ColourKey;
import savant.util.ColourScheme;
import savant.util.DrawingInstruction;
import savant.util.DrawingMode;
import savant.util.MiscUtils;
import savant.util.Range;


/**
 * Renderer for variant tracks.
 *
 * @author tarkvara
 */
class VariantTrackRenderer extends TrackRenderer {
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
                AxisRange axis = (AxisRange)instructions.get(DrawingInstruction.AXIS_RANGE);
                addInstruction(DrawingInstruction.AXIS_RANGE, new AxisRange(axis.getXMin(), axis.getXMax(), 0, evt.getData().size()));
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
        }
        
        Range r = (Range)instructions.get(DrawingInstruction.RANGE);
        String locStr = LocationController.getInstance().getReferenceName() + ":" + r;
        Rectangle2D locRect = g2.getFont().getStringBounds(locStr, g2.getFontRenderContext());
        locRect = new Rectangle2D.Double(5.0, 5.0, locRect.getWidth() + 5.0, locRect.getHeight() + 5.0);
        
        g2.setColor(Color.WHITE);
        g2.fill(locRect);
        g2.setColor(Color.BLACK);
        g2.setStroke(ONE_STROKE);
        g2.draw(locRect);
        g2.setFont(LEGEND_FONT);
        MiscUtils.drawMessage(g2, locStr, locRect);
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
        List<Rectangle2D> insertions = new ArrayList<Rectangle2D>();

        double i = 0.0;
        for (Record rec: data) {
    
            VariantRecord varRec = (VariantRecord)rec;
            Rectangle2D rect = new Rectangle2D.Double(0.0, i * h, w, h);            
            switch (varRec.getVariantType()) {
                case INSERTION:
                    insertions.add(rect);
                    break;
                case DELETION:
                    accumulator.addShape(ColourKey.DELETED_BASE, rect);
                    break;
                case SNP:
                    accumulator.addBaseShape(varRec.getAltBases().charAt(0), rect);
                    break;
            }
            i++;
        }
        accumulator.render(g2);

        for (Rectangle2D ins: insertions) {
            drawInsertion(g2, ins.getX(), ins.getY(), ins.getWidth(), ins.getHeight());
        }
    }
}
