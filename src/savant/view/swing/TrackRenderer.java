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

import savant.controller.LocationController;
import savant.controller.SelectionController;
import savant.data.event.DataRetrievalEvent;
import savant.data.event.DataRetrievalListener;
import savant.data.types.Record;
import savant.exception.RenderingException;
import savant.file.DataFormat;
import savant.settings.InterfaceSettings;
import savant.util.DrawingInstruction;
import savant.util.Range;
import savant.util.Resolution;
import savant.view.swing.interval.BAMTrackRenderer;

/**
 *
 * @author mfiume, AndrewBrook, tarkvara
 */
public abstract class TrackRenderer implements DataRetrievalListener {
    private static final Log LOG = LogFactory.getLog(TrackRenderer.class);

    protected List<Record> data;
    protected final EnumMap<DrawingInstruction, Object> instructions = new EnumMap<DrawingInstruction, Object>(DrawingInstruction.class);
    protected String trackName;
    private DataFormat dataType;

    protected Map<Record, Shape> recordToShapeMap = new HashMap<Record, Shape>();

    //specific to interval renderers
    private int intervalHeight = -1;
    private static final int[] AVAILABLE_INTERVAL_HEIGHTS = new int[] { 1, 4, 8, 12, 16, 20, 24, 28, 32, 36, 40};
    protected int offset = 0; //of scrollbar (interval only for now)

    protected TrackRenderer(DataFormat dataType) {
        this.dataType = dataType;
    }

    public void setTrackName(String name) {
        trackName = name;
    }

    public abstract List<String> getRenderingModes();
    public abstract String getDefaultRenderingMode();


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

    /**
     * Remove all instructions to allow for a fresh render.
     */
    public void clearInstructions() {
        instructions.clear();
    }

    public abstract void render(Graphics g, GraphPane gp) throws RenderingException;

    /**
     * Before doing any actual rendering, derived classes should call this in their
     * render() methods in case we want to display a message instead.
     *
     * @throws RenderingException
     */
    protected void renderPreCheck() throws RenderingException {
        Boolean refexists = (Boolean)instructions.get(DrawingInstruction.REFERENCE_EXISTS);
        if (!refexists) {
            throw new RenderingException("No data for reference");
        }
        String errorMessage = (String)instructions.get(DrawingInstruction.ERROR);
        if (errorMessage != null){
            throw new RenderingException(errorMessage);
        }
    }

    /**
     * Before doing any actual rendering, derived classes should call this in their
     * render() methods in case we want to display a message instead.
     *
     * If graphPane is given and error is thrown, make sure graphPane is resized if necessary.
     *
     * @throws RenderingException
     */
    protected void renderPreCheck(GraphPane gp) throws RenderingException {
        Boolean refexists = (Boolean)instructions.get(DrawingInstruction.REFERENCE_EXISTS);
        if (!refexists) {
            this.resizeFrame(gp);
            throw new RenderingException("No data for reference");
        }
        String errorMessage = (String)instructions.get(DrawingInstruction.ERROR);
        if (errorMessage != null){
            this.resizeFrame(gp);
            throw new RenderingException(errorMessage);
        }
    }

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
        String mode = (String)instructions.get(DrawingInstruction.MODE);
        if (mode != null && mode.equals(BAMTrackRenderer.ARC_PAIRED_MODE)){
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

    //INTERVAL SPECIFIC CODE

    protected int getIntervalHeight(){
        if (intervalHeight > 0) {
            return intervalHeight;
        }
        return InterfaceSettings.getIntervalHeight(dataType);
    }

    protected void setIntervalHeight(int height){
        intervalHeight = height;
    }

    /*
     * Resize frame if necessary
     * @return false if pane needs to be resized
     */
    protected boolean determineFrameSize(GraphPane gp, int numIntervals){
        
        int currentHeight = gp.getHeight();
        int currentWidth = gp.getParentFrame().getFrameLandscape().getWidth();
        int currentHeight1 = ((JViewport)gp.getParent().getParent()).getHeight();
        int expectedHeight = Math.max((int)((numIntervals * getIntervalHeight()) / 0.9), currentHeight1);

        if(expectedHeight != currentHeight || currentWidth != gp.getWidth()){
            gp.newHeight = expectedHeight;
            gp.setPaneResize(true);
            return false;
        }
        gp.setUnitHeight(getIntervalHeight());
        gp.setYRange(new Range(0,(int)Math.ceil(expectedHeight / getIntervalHeight())));

        return true;
    }

    //public int[] getAvailableIntervalHeights(){
    //    return new int[]{1,4,8,12,16,20,30,50,100};
    //}

    public int getIntervalHeightFromSlider(int slider){
        slider--; //starts at 1
        if(slider < 0) return AVAILABLE_INTERVAL_HEIGHTS[0];
        if(slider >= AVAILABLE_INTERVAL_HEIGHTS.length) return AVAILABLE_INTERVAL_HEIGHTS[AVAILABLE_INTERVAL_HEIGHTS.length - 1];
        return AVAILABLE_INTERVAL_HEIGHTS[slider];
    }

    public int getValueForIntervalSlider(){
        int newValue = 0;
        int diff = Math.abs(AVAILABLE_INTERVAL_HEIGHTS[0] - getIntervalHeight());
        for(int i = 1; i < AVAILABLE_INTERVAL_HEIGHTS.length; i++){
            int currVal = AVAILABLE_INTERVAL_HEIGHTS[i];
            int currDiff = Math.abs(currVal - getIntervalHeight());
            if(currDiff < diff){
                newValue = i;
                diff = currDiff;
            }
        }
        return newValue + 1; //can't be 0
    }

    public int getNumAvailableIntervalHeights(){
        return AVAILABLE_INTERVAL_HEIGHTS.length;
    }

    //END INTERVAL SPECIFIC CODE
}
