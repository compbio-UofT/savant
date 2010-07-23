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

import savant.model.FileFormat;
import savant.model.view.DrawingInstructions;
import savant.util.Range;

import java.awt.*;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JViewport;

/**
 *
 * @author mfiume
 */
public abstract class TrackRenderer {

    private List<Object> data;
    private DrawingInstructions instructions;
    private FileFormat dataType;

    public TrackRenderer(DrawingInstructions instructions) {
        this.instructions = instructions;
    }

    public List<Object> getData() { return this.data; }
    public DrawingInstructions getDrawingInstructions() { return this.instructions; }
    public FileFormat getDataType() { return this.dataType; }

    public abstract void render(Graphics g, GraphPane gp);

    public abstract boolean isOrdinal();

    public abstract Range getDefaultYRange();

    public void setData(List<Object> data) { 
        this.data = data;
    }

    public boolean hasHorizontalGrid() {
        return false;
    }

    public JPanel arcLegendPaint(){
        return null;
    }

    public void resizeFrame(GraphPane gp){
        Frame frame = gp.getParentFrame();
        int h1 = ((JViewport)gp.getParent()).getHeight();
        int h2 = frame.getFrameLandscape().getHeight();
        if(h1 != h2){
            gp.revalidate();
            gp.setPreferredSize(((JViewport)gp.getParent()).getSize());
            frame.getFrameLandscape().setPreferredSize(((JViewport)gp.getParent()).getSize());
        }
    }

    public void setIntervalMode(String mode){};
}
