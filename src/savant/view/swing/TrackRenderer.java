/*
 *    Copyright 2009-2010 University of Toronto
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.JPanel;
import javax.swing.JViewport;

import savant.controller.RangeController;
import savant.controller.SelectionController;
import savant.data.types.Record;
import savant.exception.RenderingException;
import savant.file.DataFormat;
import savant.util.DrawingInstruction;
import savant.util.Mode;
import savant.util.Range;

/**
 *
 * @author mfiume, AndrewBrook, tarkvara
 */
public abstract class TrackRenderer {

    protected List<Record> data;
    protected final EnumMap<DrawingInstruction, Object> instructions = new EnumMap<DrawingInstruction, Object>(DrawingInstruction.class);
    protected final DataFormat dataType;
    protected String trackName;

    protected Map<Record, Shape> recordToShapeMap = new HashMap<Record, Shape>();

    protected TrackRenderer(DataFormat dataType) {
        this.dataType = dataType;
        instructions.put(DrawingInstruction.TRACK_DATA_TYPE, dataType);
    }

    public void setTrackName(String name) {
        this.trackName = name;
    }

    public List<Record> getData() {
        return data == null ? null : Collections.unmodifiableList(data);
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

    public DataFormat getDataType() {
        return dataType;
    }

    public abstract void render(Graphics g, GraphPane gp) throws RenderingException;

    public abstract boolean isOrdinal();

    public abstract Range getDefaultYRange();

    public boolean selectionAllowed(){
        Object instruction = instructions.get(DrawingInstruction.SELECTION_ALLOWED);
        if(instruction == null || instruction.equals(false)) return false;
        return true;
    }

    public void setData(List<Record> data) {
        this.data = data;
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

        if(!selectionAllowed() || !hasMappedValues() || data == null) return null;
        
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

        if(!selectionAllowed() || !hasMappedValues()) return false;

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
        if(selectionAllowed()){
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
