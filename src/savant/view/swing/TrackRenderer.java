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

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.util.*;
import javax.swing.JPanel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.controller.LocationController;
import savant.controller.SelectionController;
import savant.data.event.DataRetrievalEvent;
import savant.data.event.DataRetrievalListener;
import savant.data.types.Record;
import savant.exception.RenderingException;
import savant.util.DrawingInstruction;
import savant.util.DrawingMode;
import savant.util.Resolution;


/**
 *
 * @author mfiume, AndrewBrook, tarkvara
 */
public abstract class TrackRenderer implements DataRetrievalListener {
    private static final Log LOG = LogFactory.getLog(TrackRenderer.class);

    private static final int MIN_TRANSPARENCY = 20;
    private static final int MAX_TRANSPARENCY = 255;

    protected List<Record> data;
    protected final EnumMap<DrawingInstruction, Object> instructions = new EnumMap<DrawingInstruction, Object>(DrawingInstruction.class);
    protected String trackName;

    protected Map<Record, Shape> recordToShapeMap = new HashMap<Record, Shape>();

    protected TrackRenderer() {
    }

    public void setTrackName(String name) {
        trackName = name;
    }

    public abstract DrawingMode[] getDrawingModes();
    public abstract DrawingMode getDefaultDrawingMode();


    /**
     * Sets the data to null so we know that there's nothing to render.
     *
     * @param evt ignored
     */
    @Override
    public void dataRetrievalStarted(DataRetrievalEvent evt) {
        data = null;
    }

    /**
     * Default handler just calls sets the renderer to have the newly-received data.
     *
     * @param evt describes the data being received
     */
    @Override
    public void dataRetrievalCompleted(DataRetrievalEvent evt) {
        LOG.debug("TrackRenderer received dataRetrievalCompleted, removing PROGRESS.");
        instructions.remove(DrawingInstruction.PROGRESS);
        data = evt.getData();
    }

    public boolean isWaitingForData() {
        return data == null && instructions.containsKey(DrawingInstruction.PROGRESS);
    }

    /**
     * Data retrieval has failed for some reason.
     *
     * @param evt describes the error
     */
    @Override
    public void dataRetrievalFailed(DataRetrievalEvent evt) {
        instructions.remove(DrawingInstruction.PROGRESS);
        instructions.put(DrawingInstruction.ERROR, new RenderingException(evt.getError().getLocalizedMessage(), 3));
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
     * Remove a drawing instruction from this renderer.
     * 
     * @param key the drawing instruction to be removed
     */
    public void removeInstruction(DrawingInstruction key) {
        instructions.remove(key);
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

        Boolean refexists = (Boolean)instructions.get(DrawingInstruction.REFERENCE_EXISTS);
        if (!refexists) {
            throw new RenderingException("No data for " + LocationController.getInstance().getReferenceName(), 1);
        }
        RenderingException error = (RenderingException)instructions.get(DrawingInstruction.ERROR);
        if (error != null){
            throw error;
        }
        if (data == null || data.isEmpty()) {
            throw new RenderingException("No data in range", 1);
        }
    }

    /**
     * Check whether to perform selection for this track.
     *
     * @param checkRes if true, return true only if resolution is very high
     * @return whether or not to allow selection at this time
     */
    public boolean selectionAllowed(boolean checkRes){
        Object instr_select = instructions.get(DrawingInstruction.SELECTION_ALLOWED);
        if(instr_select == null || instr_select.equals(false)) return false;
        if(checkRes){
            Object instr_res = instructions.get(DrawingInstruction.RESOLUTION);
            if (instr_res == null || !instr_res.equals(Resolution.HIGH))
                 return false;
        }
        return true;
    }

    public JPanel arcLegendPaint(){
        return null;
    }

    public boolean hasMappedValues(){
        return !recordToShapeMap.isEmpty();
    }

    public Map<Record, Shape> searchPoint(Point p){

        if(!selectionAllowed(true) || !hasMappedValues() || data == null) return null;
        
        //check for arcMode
        boolean isArc = false;
        DrawingMode mode = (DrawingMode)instructions.get(DrawingInstruction.MODE);
        if (mode == DrawingMode.ARC_PAIRED){
            isArc = true;
        }
               
        Rectangle2D testIntersection = new Rectangle2D.Double(p.x-3, p.y-3, 7, 7);
        for(int i = 0; i < data.size(); i++){
            if(data.get(i) == null) continue;
            Shape s = recordToShapeMap.get((Record)data.get(i));
            if(s == null) continue;
            //if(contains AND (notArc OR (isEdge...))          
            if( (!isArc && s.contains(p)) || 
                (isArc && s.intersects(testIntersection) && (!s.contains(p.x-3, p.y-3) || !s.contains(p.x+3, p.y-3)))){
                Map<Record, Shape> map = new HashMap<Record, Shape>();
                map.put((Record)data.get(i), s);
                return map;
            }
        }
        return null;
    }

    public boolean rectangleSelect(Rectangle2D rect){

        if(!selectionAllowed(false) || !hasMappedValues()) return false;

        boolean repaint = false;
        List<Record> toAdd = new ArrayList<Record>();

        Iterator<Record> it = this.recordToShapeMap.keySet().iterator();
        while(it.hasNext()){
            Record o = it.next();
            Shape s = recordToShapeMap.get(o);
            if(s == null) continue;
            if(s.intersects(rect)){
                toAdd.add(o);
                repaint = true;
            }
        }

        if(repaint)
            SelectionController.getInstance().addMultipleSelections(trackName, toAdd);

        return repaint;
    }

    

    // GLOBAL SELECTED

    public void addToSelected(Record i){
        if(selectionAllowed(false)){
            SelectionController.getInstance().toggleSelection(trackName, i);
        }
    }
    
    public void forceAddToSelected(Record i){
        if(selectionAllowed(false)){
            SelectionController.getInstance().addSelection(trackName, i);
        }
    }


    /**
     * Current selected shapes.
     */
    public List<Shape> getCurrentSelectedShapes(GraphPane gp){
        List<Shape> shapes = new ArrayList<Shape>();
        List<Record> currentSelected = SelectionController.getInstance().getSelectedFromList(trackName, LocationController.getInstance().getRange(), data);
        for(int i = 0; i < currentSelected.size(); i++){
            Shape s = recordToShapeMap.get(currentSelected.get(i));
            if (s != null){
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
}
