/*
 *    Copyright 2009-2011 University of Toronto
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

package savant.view.swing;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JPanel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.controller.LocationController;
import savant.controller.SelectionController;
import savant.data.event.DataRetrievalEvent;
import savant.data.types.Record;
import savant.exception.RenderingException;
import savant.util.ColourKey;
import savant.util.ColourScheme;
import savant.util.DrawingInstruction;
import savant.util.DrawingMode;
import savant.util.Listener;
import savant.util.Resolution;


/**
 *
 * @author mfiume, AndrewBrook, tarkvara
 */
public abstract class TrackRenderer implements Listener<DataRetrievalEvent> {
    private static final Log LOG = LogFactory.getLog(TrackRenderer.class);

    private static final int MIN_TRANSPARENCY = 20;
    private static final int MAX_TRANSPARENCY = 255;

    protected static final Font SMALL_FONT = new Font("Sans-Serif", Font.PLAIN, 10);
    protected static final Stroke ONE_STROKE = new BasicStroke(1.0f);
    protected static final Stroke TWO_STROKE = new BasicStroke(2.0f);

    protected List<Record> data;
    protected final EnumMap<DrawingInstruction, Object> instructions = new EnumMap<DrawingInstruction, Object>(DrawingInstruction.class);
    protected String trackName;

    protected Map<Record, Shape> recordToShapeMap = new HashMap<Record, Shape>();
    protected Map<Record, Shape> artifactMap = new HashMap<Record, Shape>(); //meta info pointing to reads (ie. lines in read pair mode)

    protected TrackRenderer() {
    }

    /**
     * Set the track's name.  This is used only to provide a key when dealing with the SelectionController.
     *
     * @param name 
     */
    public void setTrackName(String name) {
        trackName = name;
    }

    /**
     * Sets the data to null so we know that there's nothing to render.
     *
     * @param evt describes the data being received
     */
    @Override
    public void handleEvent(DataRetrievalEvent evt) {
        switch (evt.getType()) {
            case STARTED:
                // Sets the data to null so we know that there's nothing to render.
                data = null;
                break;
            case COMPLETED:
                // Default handler just sets the renderer to have the newly-received data.
                LOG.debug("TrackRenderer received dataRetrievalCompleted, removing PROGRESS.");
                instructions.remove(DrawingInstruction.PROGRESS);
                data = evt.getData();
                break;
            case FAILED:
                // Data retrieval has failed for some reason.
                instructions.remove(DrawingInstruction.PROGRESS);
                instructions.put(DrawingInstruction.ERROR, new RenderingException(evt.getError().getLocalizedMessage(), 3));
                break;
        }
    }

    public boolean isWaitingForData() {
        return data == null && instructions.containsKey(DrawingInstruction.PROGRESS);
    }

    public void addInstruction(DrawingInstruction key, Object value) {
        instructions.put(key, value);
    }

    /**
     * Retrieve the value of a specific drawing instruction for this renderer.
     *
     * @param key key identifying the drawing instruction to be retrieved
     * @return the value of that drawing instruction
     */
    public Object getInstruction(DrawingInstruction key) {
        return instructions.get(key);
    }

    /**
     * Remove all instructions to allow for a fresh render.
     */
    public void clearInstructions() {
        instructions.clear();
    }

    public abstract void render(Graphics2D g2, GraphPane gp) throws RenderingException;

    /**
     * Before doing any actual rendering, derived classes should call this in their
     * render() methods in case we want to display a message instead.
     *
     * If graphPane is given and error is thrown, make sure graphPane is resized if necessary.
     *
     * @throws RenderingException
     */
    protected void renderPreCheck() throws RenderingException {

        // Clear away any shapes.
        recordToShapeMap.clear();
        artifactMap.clear();

        Boolean refexists = (Boolean)instructions.get(DrawingInstruction.REFERENCE_EXISTS);
        if (!refexists) {
            throw new RenderingException("No data for " + LocationController.getInstance().getReferenceName(), 1);
        }
        RenderingException error = (RenderingException)instructions.get(DrawingInstruction.ERROR);
        if (error != null) {
            throw error;
        }
        if (data == null || data.isEmpty()) {
            throw new RenderingException("No data in range", 1);
        }
    }

    /**
     * Check whether to perform selection for this track.
     *
     * @param checkRes if true, return true only if resolution is HIGH
     * @return whether or not to allow selection at this time
     */
    public boolean selectionAllowed(boolean checkRes) {
        Object instr_select = instructions.get(DrawingInstruction.SELECTION_ALLOWED);
        if (instr_select == null || instr_select.equals(false)) return false;
        if (checkRes) {
            Object instr_res = instructions.get(DrawingInstruction.RESOLUTION);
            if (instr_res == null || !instr_res.equals(Resolution.HIGH))
                 return false;
        }
        return true;
    }

    public boolean hasMappedValues() {
        return !recordToShapeMap.isEmpty();
    }

    public Map<Record, Shape> searchPoint(Point p) {

        if (!selectionAllowed(true) || !hasMappedValues() || data == null) return null;
        
        //check for arcMode
        boolean isArc = false;
        DrawingMode mode = (DrawingMode)instructions.get(DrawingInstruction.MODE);
        if (mode == DrawingMode.ARC_PAIRED) {
            isArc = true;
        }
        
        Map<Record, Shape> map = new HashMap<Record, Shape>();
        boolean found = false;
               
        Rectangle2D testIntersection = new Rectangle2D.Double(p.x-3, p.y-3, 7, 7);
        for(int i = 0; i < data.size(); i++) {
            if (data.get(i) == null) continue;
            Shape s = recordToShapeMap.get((Record)data.get(i));
            if (s == null) continue;

            //if (contains AND (notArc OR (isEdge...))          
            if ( (!isArc && s.contains(p)) || 
                (isArc && s.intersects(testIntersection) && (!s.contains(p.x-3, p.y-3) || !s.contains(p.x+3, p.y-3)))) {
                map.put((Record)data.get(i), s);
                found = true;
                continue;
            }
            
            //check other artifacts
            Shape artifact = artifactMap.get((Record)data.get(i));
            if (artifact != null && artifact.contains(p.x, p.y)) {
                map.put((Record)data.get(i), s);
                found = true;
            }
        }
        
        if (found) return map;
        else return null;
    }

    public boolean rectangleSelect(Rectangle2D rect) {

        if (!selectionAllowed(false) || !hasMappedValues()) return false;

        boolean repaint = false;
        List<Record> toAdd = new ArrayList<Record>();

        for (Record o: recordToShapeMap.keySet()) {
            Shape s = recordToShapeMap.get(o);
            if (s == null) continue;
            if (s.intersects(rect)) {
                toAdd.add(o);
                repaint = true;
            }
        }

        if (repaint) {
            SelectionController.getInstance().addMultipleSelections(trackName, toAdd);
        }

        return repaint;
    }

    

    // GLOBAL SELECTED

    public void addToSelected(Record i) {
        if (selectionAllowed(false)) {
            SelectionController.getInstance().toggleSelection(trackName, i);
        }
    }

    /**
     * Invoked when the user chooses "Select Pair" from the Bam popup menu.
     * @param i record to be selected
     */
    public void forceAddToSelected(Record i) {
        SelectionController.getInstance().addSelection(trackName, i);
    }


    /**
     * Current selected shapes.
     */
    public List<Shape> getCurrentSelectedShapes(GraphPane gp) {
        List<Shape> shapes = new ArrayList<Shape>();
        List<Record> currentSelected = SelectionController.getInstance().getSelectedFromList(trackName, LocationController.getInstance().getRange(), data);
        for(int i = 0; i < currentSelected.size(); i++) {
            Shape s = recordToShapeMap.get(currentSelected.get(i));
            if (s != null) {
                shapes.add(s);
            }
        }
        return shapes;
    }

    /**
     * For practical reasons, we never want alpha to be less than 20 or more than 255.
     */
    public static int getConstrainedAlpha(int alpha) {
        return alpha < MIN_TRANSPARENCY ? MIN_TRANSPARENCY : (alpha > MAX_TRANSPARENCY ? MAX_TRANSPARENCY : alpha);
    }
    
    public void toggleGroup(ArrayList<Record> recs) {
        if (selectionAllowed(false)) {
            SelectionController.getInstance().toggleGroup(trackName, recs);
        }
    }

    /**
     * Shared by BAMTrackRenderer and RichIntervalTrackRenderer to draw the white diamond
     * which indicates an insertion.
     */
    public Shape drawInsertion(Graphics2D g2,GraphPane gp, int xStart, int level, double unitHeight) {

        double unitWidth = gp.getUnitWidth();

        g2.setColor(Color.WHITE);
        double xCoordinate = gp.transformXPos(xStart);
        double yCoordinate = gp.transformYPos(0) - ((level + 1) * unitHeight) - gp.getOffset();
        double w = unitWidth * 0.5;

        Path2D.Double rhombus = new Path2D.Double();
        rhombus.moveTo(xCoordinate, yCoordinate);
        rhombus.lineTo(xCoordinate + w, yCoordinate + unitHeight * 0.5);
        rhombus.lineTo(xCoordinate, yCoordinate + unitHeight);
        rhombus.lineTo(xCoordinate - w, yCoordinate + unitHeight * 0.5);
        rhombus.closePath();
        g2.fill(rhombus);

        if (unitWidth > 16.0) {
            g2.setColor(((ColourScheme)instructions.get(DrawingInstruction.COLOR_SCHEME)).getColor(ColourKey.INTERVAL_LINE));
            g2.draw(new Line2D.Double(xCoordinate, yCoordinate, xCoordinate, yCoordinate + unitHeight));
        }

        return rhombus;
    }
    
    
    /**
     * Return the dimensions of the legend which this renderer currently requires.
     * If the derived renderer doesn't have a legend, it should return null.
     */
    public Dimension getLegendSize(DrawingMode mode) {
        return null;
    }
    
    /**
     * Draw the actual legend.
     */
    public void drawLegend(Graphics2D g2, DrawingMode mode) {
    }

    /**
     * Simplest kind of legend is just a list of coloured lines with names next to them.
     */
    protected void drawSimpleLegend(Graphics2D g2, ColourKey... keys) {
        ColourScheme cs = (ColourScheme)instructions.get(DrawingInstruction.COLOR_SCHEME);

        g2.setFont(SMALL_FONT);

        int x = 30;
        int y = 15;

        for (ColourKey k: keys) {
            String legendString = k.getName();
            g2.setColor(cs.getColor(k));
            g2.setStroke(TWO_STROKE );
            Rectangle2D stringRect = SMALL_FONT.getStringBounds(legendString, g2.getFontRenderContext());
            g2.drawLine(x-25, y-(int)stringRect.getHeight()/2, x-5, y-(int)stringRect.getHeight()/2);
            g2.setColor(cs.getColor(ColourKey.INTERVAL_LINE));
            g2.setStroke(ONE_STROKE);
            g2.drawString(legendString, x, y);

            y += stringRect.getHeight()+2;
        }
    }
}
