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

import java.net.URI;
import savant.file.FileFormat;
import savant.model.view.DrawingInstructions;
import savant.util.Range;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.JPanel;
import javax.swing.JViewport;
import savant.controller.RangeController;
import savant.controller.SelectionController;
import savant.data.types.ContinuousRecord;
import savant.data.types.GenericContinuousRecord;
import savant.model.view.Mode;
import savant.view.swing.continuous.ContinuousTrackRenderer;

/**
 *
 * @author mfiume
 */
public abstract class TrackRenderer {

    private List<Object> data;
    private DrawingInstructions instructions;
    private FileFormat dataType;
    private URI fileURI;

    private Color overlayColor = Color.RED;
    private Color selectedColor = Color.GREEN;
    //private List<Object> currentSelected = new ArrayList<Object>();
    //private List<Object> tempSelected = new ArrayList<Object>();
    protected Map<Object, Shape> objectToShapeMap = new HashMap<Object, Shape>();

    public TrackRenderer(DrawingInstructions instructions) {
        this.instructions = instructions;
    }

    public void setURI(URI uri) {
        this.fileURI = uri;
    }

    public List<Object> getData() { 
        if(data == null) return null;
        return Collections.unmodifiableList(this.data);
    }
    public DrawingInstructions getDrawingInstructions() { return this.instructions; }
    public FileFormat getDataType() { return this.dataType; }

    public abstract void render(Graphics g, GraphPane gp);

    public abstract boolean isOrdinal();

    public abstract Range getDefaultYRange();

    public boolean selectionAllowed(){
        Object instruction = this.instructions.getInstruction(DrawingInstructions.InstructionName.SELECTION_ALLOWED);
        if(instruction == null || instruction.equals(false)) return false;
        return true;
    }

    public void setData(List<Object> data) {
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
        int h2 = frame.getFrameLandscape().getHeight();
        int h3 = gp.getHeight();
        if(h1 != h3){
            gp.revalidate();
            gp.setPreferredSize(((JViewport)gp.getParent().getParent()).getSize());
            frame.getFrameLandscape().setPreferredSize(((JViewport)gp.getParent().getParent()).getSize());
        }
    }

    public void setIntervalMode(String mode){};


    
    // SHAPES
    // access shapes for current view

    public void clearShapes(){
        this.objectToShapeMap.clear();
    }

    public boolean hasMappedValues(){
        return !this.objectToShapeMap.isEmpty();
    }

    public Map<Object, Shape> searchPoint(Point p){

        if(!selectionAllowed() || !hasMappedValues() || data == null) return null;
        
        //check for arcMode
        boolean isArc = false;
        Mode instruction = (Mode) getDrawingInstructions().getInstruction(DrawingInstructions.InstructionName.MODE);
        if(instruction != null && instruction.getName().equals("MATE_PAIRS")){
            isArc = true;
        }
        
        for(int i = 0; i < data.size(); i++){
            if(data.get(i) == null) continue;
            Shape s = this.objectToShapeMap.get(data.get(i));
            if(s == null) continue;
            //if(contains AND (notArc OR (isEdge...))
            if(s.contains(p.x, p.y) &&
                (!isArc || (!s.contains(p.x-1, p.y) ||
                    !s.contains(p.x+1, p.y) ||
                    !s.contains(p.x, p.y-1)))){
                Map<Object, Shape> map = new HashMap<Object, Shape>();
                map.put(data.get(i), s);
                return map;
            }
        }
        return null;
    }

    public boolean rectangleSelect(Rectangle2D rect){

        if(!selectionAllowed() || !hasMappedValues()) return false;

        boolean repaint = false;

        Iterator it = this.objectToShapeMap.keySet().iterator();
        while(it.hasNext()){
            Object o = it.next();
            Shape s = this.objectToShapeMap.get(o);
            if(s == null) continue;
            if(s.intersects(rect)){
                this.addMultipleToSelected(o);
                repaint = true;
            }
        }
        SelectionController.getInstance().doneAdding(fileURI);

        return repaint;
    }

    

    // GLOBAL SELECTED
    // comparisons for globally selected objects

    public void addToSelected(Object i){
        if(selectionAllowed()){
            SelectionController.getInstance().toggleSelection(fileURI, (Comparable)i);
        }
    }

    public void addMultipleToSelected(Object i){
        if(selectionAllowed()){
            SelectionController.getInstance().addSelection(fileURI, (Comparable)i);
        }
    }


    // CURRENT SELECTED
    // controls selections in current view

    public List<Shape> getCurrentSelectedShapes(GraphPane gp){
        List<Shape> shapes = new ArrayList<Shape>();
        List<Object> currentSelected = SelectionController.getInstance().getSelectedInRange(fileURI, RangeController.getInstance().getRange(), data);
        for(int i = 0; i < currentSelected.size(); i++){
            if(this.getClass().equals(ContinuousTrackRenderer.class)){
                shapes.add(continuousObjectToEllipse(gp, currentSelected.get(i)));
            } else {
                Shape s = this.objectToShapeMap.get(currentSelected.get(i));
                if(s != null){
                    shapes.add(s);
                }
            }
        }
        return shapes;
    }

    public static Shape continuousObjectToEllipse(GraphPane gp, Object o){
        GenericContinuousRecord rec = (GenericContinuousRecord) o;
        Double x = gp.transformXPos(rec.getPosition()) + (gp.getUnitWidth()/2) -4;
        Double y = gp.transformYPos(rec.getValue().getValue()) -4;// + (this.getUnitWidth()/2);
        Shape s = new Ellipse2D.Double(x, y, 8, 8);
        return s;
    }


    // TEMP SELECTED
    // controls shapes for temporary selection, ie. selecting rows in Table View

    /*public void addMultipleToTempSelected(Object i){
        if(this.selectionAllowed() && !this.tempSelected.contains(i)){
            this.tempSelected.add(i);
        }
    }

    public boolean hasTempSelected(){
        return !this.tempSelected.isEmpty();
    }

    public void clearTempSelected(){
        this.tempSelected.clear();
    }

    public List<Object> getTempSelected(){
        return this.tempSelected;
    }

    public List<Shape> getTempSelectedShapes(){
        List<Shape> shapes = new ArrayList<Shape>();
        for(int i = 0; i < this.tempSelected.size(); i++){
            Shape s = this.objectToShapeMap.get(tempSelected.get(i));
            if(s != null){
                shapes.add(s);
            }
        }
        return shapes;
    }*/

}
