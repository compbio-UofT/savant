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

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.util.*;
import javax.swing.JPanel;
import javax.swing.JViewport;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.controller.RangeController;
import savant.controller.SelectionController;
import savant.data.event.DataRetrievalEvent;
import savant.data.event.DataRetrievalListener;
import savant.data.types.Record;
import savant.exception.RenderingException;
import savant.file.DataFormat;
import savant.util.DrawingInstruction;
import savant.util.Mode;
import savant.util.Range;
import savant.util.Resolution;

/**
 *
 * @author mfiume, AndrewBrook, tarkvara
 */
public abstract class TrackRenderer implements DataRetrievalListener {
    private static final Log LOG = LogFactory.getLog(TrackRenderer.class);

    protected List<Record> data;
    protected final EnumMap<DrawingInstruction, Object> instructions = new EnumMap<DrawingInstruction, Object>(DrawingInstruction.class);
    protected String trackName;

    protected Map<Record, Shape> recordToShapeMap = new HashMap<Record, Shape>();

    protected TrackRenderer(DataFormat dataType) {
        instructions.put(DrawingInstruction.TRACK_DATA_TYPE, dataType);
    }

    public void setTrackName(String name) {
        trackName = name;
    }

    /**
     * Renderers don't currently care about data retrieval starting.
     *
     * @param evt ignored
     */
    @Override
    public void dataRetrievalStarted(DataRetrievalEvent evt) {
        data = null;
    }

    /**
     * Default handler just calls setData() with the newly-received data.
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
        instructions.put(DrawingInstruction.ERROR, evt.getError().getLocalizedMessage());
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

    public abstract void render(Graphics g, GraphPane gp) throws RenderingException;

    public abstract boolean isOrdinal();

    public abstract Range getDefaultYRange();

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
            if(instr_res == null || !instr_res.equals(Resolution.VERY_HIGH))
                 return false;
        }
        return true;
    }

    public boolean hasHorizontalGrid() {
        return false;
    }

    public JPanel arcLegendPaint(){
        return null;
    }

    /*
     * Fit the graphPane to the JViewport of its corresponding scrollPane.
     * Call if vertical scrolling may have been enabled on previous render.
     */
    public void resizeFrame(GraphPane gp){
        Frame frame = gp.getParentFrame();
        int h1 = ((JViewport)gp.getParent().getParent()).getHeight();
        int h2 = gp.getHeight();
        if(h1 != h2){
            gp.revalidate();
            gp.setPreferredSize(((JViewport)gp.getParent().getParent()).getSize());
            frame.getFrameLandscape().setPreferredSize(((JViewport)gp.getParent().getParent()).getSize());
        }
    }

    public void setIntervalMode(String mode){};

    // SHAPES
    // access shapes for current view

    public void clearShapes(){
        this.recordToShapeMap.clear();
    }

    public boolean hasMappedValues(){
        return !this.recordToShapeMap.isEmpty();
    }

    public Map<Record, Shape> searchPoint(Point p){

        if(!selectionAllowed(true) || !hasMappedValues() || data == null) return null;
        
        //check for arcMode
        boolean isArc = false;
        Mode instruction = (Mode)instructions.get(DrawingInstruction.MODE);
        if (instruction != null && instruction.getName().equals("MATE_PAIRS")){
            isArc = true;
        }
        
        for(int i = 0; i < data.size(); i++){
            if(data.get(i) == null) continue;
            Shape s = this.recordToShapeMap.get((Record)data.get(i));
            if(s == null) continue;
            //if(contains AND (notArc OR (isEdge...))
            if(s.contains(p.x, p.y) &&
                (!isArc || (!s.contains(p.x-1, p.y) ||
                    !s.contains(p.x+1, p.y) ||
                    !s.contains(p.x, p.y-1)))){
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


    /**
     * Current selected shapes.
     */
    public List<Shape> getCurrentSelectedShapes(GraphPane gp){
        List<Shape> shapes = new ArrayList<Shape>();
        List<Record> currentSelected = SelectionController.getInstance().getSelectedFromList(trackName, RangeController.getInstance().getRange(), data);
        for(int i = 0; i < currentSelected.size(); i++){
            Shape s = recordToShapeMap.get(currentSelected.get(i));
            if (s != null){
                shapes.add(s);
            }
        }
        return shapes;
    }
}
