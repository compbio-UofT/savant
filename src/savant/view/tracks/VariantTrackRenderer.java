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

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import savant.api.adapter.GraphPaneAdapter;
import savant.api.data.Record;
import savant.api.data.VariantRecord;
import savant.exception.RenderingException;
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
    VariantTrackRenderer() {
    }

    @Override
    public void render(Graphics2D g2, GraphPaneAdapter gp) throws RenderingException {

        DrawingMode mode = (DrawingMode)instructions.get(DrawingInstruction.MODE);

        switch (mode) {
            case STANDARD:
                renderStandardMode(g2, gp);
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

        // To make things confusing, the VCF track is rendered sideways, so meanings of X and Y are swapped.
        double unitWidth = gp.transformYPos(1.0);
        double unitHeight = gp.getUnitWidth();
        
        ColourScheme cs = (ColourScheme)instructions.get(DrawingInstruction.COLOUR_SCHEME);
        ColourAccumulator accumulator = new ColourAccumulator(cs);
        List<Rectangle2D> insertions = new ArrayList<Rectangle2D>();

        double i = 0.0;
        for (Record rec: data) {
    
            VariantRecord varRec = (VariantRecord)rec;
            Rectangle2D rect = new Rectangle2D.Double(0.0, i * unitHeight, unitWidth, varRec.getInterval().getLength() * unitHeight);            
            switch (varRec.getType()) {
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
