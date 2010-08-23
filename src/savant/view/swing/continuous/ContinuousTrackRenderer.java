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
 * ContinuousTrackRenderer.java
 * Created on Jan 19, 2010
 */

package savant.view.swing.continuous;

import savant.data.types.GenericContinuousRecord;
import savant.util.AxisRange;
import savant.model.view.ColorScheme;
import savant.model.view.DrawingInstructions;
import savant.util.Range;
import savant.view.swing.GraphPane;
import savant.view.swing.TrackRenderer;
import savant.view.swing.util.GlassMessagePane;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Class to render continuous tracks.
 * @author vwilliams
 */
public class ContinuousTrackRenderer extends TrackRenderer {

    public ContinuousTrackRenderer() {
        this(new DrawingInstructions());
    }
    public ContinuousTrackRenderer(DrawingInstructions drawingInstructions) {
        super(drawingInstructions);
    }

    @Override
    public void render(Graphics g, GraphPane gp) {

        DrawingInstructions di = this.getDrawingInstructions();
        Graphics2D g2 = (Graphics2D) g;
        this.clearShapes();

        Boolean refexists = (Boolean) di.getInstruction(DrawingInstructions.InstructionName.REFERENCE_EXISTS);

        if (!refexists) {
            GlassMessagePane.draw(g2, gp, "no data for reference", 500);
            return;
        }

        java.util.List<Object> data = this.getData();
        if (data == null) {
            // FIXME: a nasty hack to accommodate coverage; see BAMCoverageViewTrack
            String message = (String) di.getInstruction(DrawingInstructions.InstructionName.MESSAGE);
            if (message != null) {
                GlassMessagePane.draw(g2, gp, message, 500);

            }
            return;
        }
        
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        

        ColorScheme cs = (ColorScheme) di.getInstruction(DrawingInstructions.InstructionName.COLOR_SCHEME.toString());
        Color linecolor = cs.getColor("Line");
        AxisRange axisRange = (AxisRange) di.getInstruction(DrawingInstructions.InstructionName.AXIS_RANGE);

        gp.setIsOrdinal(false);
        gp.setXRange(axisRange.getXRange());
        gp.setYRange(axisRange.getYRange());

        int numdata = this.getData().size();

        int xPos;
        double yPos;
        GeneralPath path = new GeneralPath();
        double xFormXPos, xFormYPos;

        xFormXPos = 0;
        xFormYPos = gp.transformYPos(0.0);
        path.moveTo(xFormXPos, xFormYPos);

        
        GenericContinuousRecord continuousRecord;
        double maxData = 0;
        boolean first = true;
        for (int i=0; i<numdata; i++) {
            continuousRecord = (GenericContinuousRecord)data.get(i);
            xPos = continuousRecord.getPosition();
            yPos = continuousRecord.getValue().getValue();
            xFormXPos = gp.transformXPos(xPos)+gp.getUnitWidth()/2;
            xFormYPos = gp.transformYPos(yPos);
            if (yPos > maxData) maxData = yPos;
            //Rectangle2D rec = new Rectangle2D.Double(xFormXPos - (gp.getUnitWidth()/2),0,Math.max(gp.getUnitWidth(), 1),gp.getHeight());
            //Rectangle2D rec = new Rectangle2D.Double(xFormXPos - (gp.getUnitWidth()/2),0,Math.max(xFormXPos-path.getCurrentPoint().getX(), 1),gp.getHeight());
            Rectangle2D rec = new Rectangle2D.Double(xFormXPos - ((xFormXPos-path.getCurrentPoint().getX())/2),0,Math.max(xFormXPos-path.getCurrentPoint().getX(), 1),gp.getHeight());
            this.recordToShapeMap.put(continuousRecord, rec);
            path.lineTo(xFormXPos, xFormYPos);            
        }
        xFormYPos = gp.transformYPos(0.0);
        path.lineTo(xFormXPos, xFormYPos);
        path.closePath();
        
        g2.setColor(linecolor);
        g2.draw(path);
        g2.fill(path);

    }

    @Override
    public boolean isOrdinal() {
        return false;
    }

    @Override
    public Range getDefaultYRange() {
        return new Range(0, 1);
    }

    @Override
    public boolean hasHorizontalGrid() {
        return true;
    }
}
