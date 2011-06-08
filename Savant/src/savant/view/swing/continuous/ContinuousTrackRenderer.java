/*
 *    Copyright 2010-2011 University of Toronto
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

package savant.view.swing.continuous;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import savant.controller.RangeController;
import savant.controller.SelectionController;
import savant.data.event.DataRetrievalEvent;
import savant.data.types.GenericContinuousRecord;
import savant.data.types.Record;
import savant.exception.RenderingException;
import savant.file.DataFormat;
import savant.util.AxisRange;
import savant.util.ColorScheme;
import savant.util.DrawingInstruction;
import savant.util.Range;
import savant.view.swing.GraphPane;
import savant.view.swing.TrackRenderer;


/**
 * Class to render continuous tracks.
 *
 * @author vwilliams
 */
public class ContinuousTrackRenderer extends TrackRenderer {

    public static final String STANDARD_MODE = "Standard";

    public ContinuousTrackRenderer() {
        super(DataFormat.CONTINUOUS_GENERIC);
    }

    @Override
    public void dataRetrievalCompleted(DataRetrievalEvent evt) {
        float[] extremes = ContinuousTrack.getExtremeValues(evt.getData());
        Range range = (Range)instructions.get(DrawingInstruction.RANGE);
        addInstruction(DrawingInstruction.AXIS_RANGE, AxisRange.initWithRanges(range, new Range(Math.min(0, (int) Math.floor(extremes[0]*1.05)), Math.max(0,(int) Math.ceil(extremes[1]*1.05)))));
        super.dataRetrievalCompleted(evt);
    }

    @Override
    public void render(Graphics g, GraphPane gp) throws RenderingException {

        Graphics2D g2 = (Graphics2D) g;
        clearShapes();

        renderPreCheck();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        ColorScheme cs = (ColorScheme)instructions.get(DrawingInstruction.COLOR_SCHEME);
        Color fillcolor = cs.getColor("Fill");
        Color linecolor = cs.getColor("Line");
        AxisRange axisRange = (AxisRange)instructions.get(DrawingInstruction.AXIS_RANGE);

        gp.setIsOrdinal(false);
        gp.setXRange(axisRange.getXRange());
        gp.setYRange(axisRange.getYRange());

        int numdata = data.size();

        int xPos;
        double yPos;
        GeneralPath path = new GeneralPath();
        double xFormXPos, xFormYPos;

        xFormXPos = 0;
        xFormYPos = gp.transformYPos(0.0);
        path.moveTo(xFormXPos, xFormYPos);

        
        double maxData = 0;
        for (int i=0; i<numdata; i++) {
            GenericContinuousRecord continuousRecord = (GenericContinuousRecord)data.get(i);
            xPos = continuousRecord.getPosition();
            yPos = continuousRecord.getValue();
            xFormXPos = gp.transformXPosExclusive(xPos);//+gp.getUnitWidth()/2;
            xFormYPos = gp.transformYPos(yPos);
            if (yPos > maxData) maxData = yPos;
            Rectangle2D rec = new Rectangle2D.Double(xFormXPos - ((xFormXPos-path.getCurrentPoint().getX())/2),0,Math.max(xFormXPos-path.getCurrentPoint().getX(), 1),gp.getHeight());
            this.recordToShapeMap.put(continuousRecord, rec);
            path.lineTo(xFormXPos, xFormYPos);         
        }
        xFormYPos = gp.transformYPos(0.0);
        path.lineTo(xFormXPos, xFormYPos);
        path.closePath();
        
        g2.setColor(fillcolor);
        g2.fill(path);
        g2.setColor(linecolor);
        g2.draw(path);

        if (axisRange.getYRange().getFrom() < 0) {
            g2.setColor(Color.darkGray);
            g2.drawLine(0, (int) gp.transformYPos(0), gp.getWidth(), (int) gp.transformYPos(0));
        }
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

    /**
     * Current selected shapes.
     */
    @Override
    public List<Shape> getCurrentSelectedShapes(GraphPane gp){
        List<Shape> shapes = new ArrayList<Shape>();
        List<Record> currentSelected = SelectionController.getInstance().getSelectedFromList(trackName, RangeController.getInstance().getRange(), data);
        for(int i = 0; i < currentSelected.size(); i++){
            shapes.add(continuousRecordToEllipse(gp, currentSelected.get(i)));
        }
        return shapes;
    }

    public static Shape continuousRecordToEllipse(GraphPane gp, Record o){
        GenericContinuousRecord rec = (GenericContinuousRecord) o;
        Double x = gp.transformXPosExclusive(rec.getPosition()) + (gp.getUnitWidth()/2) -4;
        Double y = gp.transformYPos(rec.getValue()) -4;// + (this.getUnitWidth()/2);
        Shape s = new Ellipse2D.Double(x, y, 8, 8);
        return s;
    }

    @Override
    public List<String> getRenderingModes() {
        List<String> modes = new ArrayList<String>();
        modes.add(STANDARD_MODE);
        return modes;
    }

    @Override
    public String getDefaultRenderingMode() {
        return STANDARD_MODE;
    }

}
