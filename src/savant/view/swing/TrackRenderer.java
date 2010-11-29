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

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.net.URI;
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
import savant.data.types.GenericContinuousRecord;
import savant.data.types.Record;
import savant.file.DataFormat;
import savant.util.DrawingInstructions;
import savant.util.Mode;
import savant.util.Range;
import savant.view.swing.continuous.ContinuousTrackRenderer;

/**
 *
 * @author mfiume, AndrewBrook
 */
public abstract class TrackRenderer {

    private List<Record> data;
    private DrawingInstructions instructions;
    private DataFormat dataType;
    private URI fileURI;
    protected Map<Record, Shape> recordToShapeMap = new HashMap<Record, Shape>();

    public TrackRenderer(DrawingInstructions instructions) {
        this.instructions = instructions;
    }

    public void setURI(URI uri) {
        this.fileURI = uri;
    }

    public List<Record> getData() {
        return data == null ? null : Collections.unmodifiableList(data);
    }

    public DrawingInstructions getDrawingInstructions() { return this.instructions; }
    public DataFormat getDataType() { return this.dataType; }

    public abstract void render(Graphics g, GraphPane gp);

    public abstract boolean isOrdinal();

    public abstract Range getDefaultYRange();

    public boolean selectionAllowed(){
        Object instruction = this.instructions.getInstruction(DrawingInstructions.InstructionName.SELECTION_ALLOWED);
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
        Mode instruction = (Mode) getDrawingInstructions().getInstruction(DrawingInstructions.InstructionName.MODE);
        if(instruction != null && instruction.getName().equals("MATE_PAIRS")){
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
            SelectionController.getInstance().addMultipleSelections(fileURI, toAdd);

        return repaint;
    }

    

    // GLOBAL SELECTED

    public void addToSelected(Record i){
        if(selectionAllowed()){
            SelectionController.getInstance().toggleSelection(fileURI, i);
        }
    }


    // CURRENT SELECTED

    public List<Shape> getCurrentSelectedShapes(GraphPane gp){
        List<Shape> shapes = new ArrayList<Shape>();
        List<Record> currentSelected = SelectionController.getInstance().getSelectedFromList(fileURI, RangeController.getInstance().getRange(), data);
        for(int i = 0; i < currentSelected.size(); i++){
            if(this.getClass().equals(ContinuousTrackRenderer.class)){
                shapes.add(continuousRecordToEllipse(gp, currentSelected.get(i)));
            } else {
                Shape s = this.recordToShapeMap.get(currentSelected.get(i));
                if(s != null){
                    shapes.add(s);
                }
            }
        }
        return shapes;
    }

    public static Shape continuousRecordToEllipse(GraphPane gp, Record o){
        GenericContinuousRecord rec = (GenericContinuousRecord) o;
        Double x = gp.transformXPos(rec.getPosition()) + (gp.getUnitWidth()/2) -4;
        Double y = gp.transformYPos(rec.getValue().getValue()) -4;// + (this.getUnitWidth()/2);
        Shape s = new Ellipse2D.Double(x, y, 8, 8);
        return s;
    }

}
