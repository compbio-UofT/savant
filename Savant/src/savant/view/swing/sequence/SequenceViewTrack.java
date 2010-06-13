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

package savant.view.swing.sequence;

import savant.model.FileFormat;
import savant.model.Genome;
import savant.model.Resolution;
import savant.model.view.AxisRange;
import savant.model.view.ColorScheme;
import savant.model.view.DrawingInstructions;
import savant.util.Range;
import savant.view.swing.BrowserDefaults;
import savant.view.swing.Savant;
import savant.view.swing.TrackRenderer;
import savant.view.swing.ViewTrack;

import java.awt.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import savant.controller.ReferenceController;

/**
 *
 * @author mfiume
 */
public class SequenceViewTrack extends ViewTrack {

    Genome genome;


    public SequenceViewTrack(String name, Genome g) throws FileNotFoundException
    {
        super(name, FileFormat.SEQUENCE_FASTA, null);
        setGenome(g);
        setColorScheme(getDefaultColorScheme());
    }

    private ColorScheme getDefaultColorScheme()
    {
        ColorScheme c = new ColorScheme();

        /* add settings here */
//        c.addColorSetting("A_BACKGROUND", new Color(160,93,153));
//        c.addColorSetting("C_BACKGROUND", new Color(131,255,0));
//        c.addColorSetting("G_BACKGROUND", new Color(255,255,0));
//        c.addColorSetting("T_BACKGROUND", new Color(255,192,0));
        c.addColorSetting("A_BACKGROUND", BrowserDefaults.A_COLOR);
        c.addColorSetting("C_BACKGROUND", BrowserDefaults.C_COLOR);
        c.addColorSetting("G_BACKGROUND", BrowserDefaults.G_COLOR);
        c.addColorSetting("T_BACKGROUND", BrowserDefaults.T_COLOR); 
        c.addColorSetting("LINE_COLOR", Color.black);
        c.addColorSetting("BACKGROUND", new Color(100,100,100,220));

        return c;
    }

    private void setGenome(Genome genome) { this.genome = genome; }
    private Genome getGenome() { return this.genome; }


    /*
     * getData
     *     Get data in the specified range at the specified resolution
     */
    public List<Object> retrieveData(String reference, Range range, Resolution resolution)
    {
        String subsequence = "";
        try {
            subsequence = getGenome().getSequence(reference, range);
        } catch (IOException ex) {
            Savant.log("Error: getting sequence data");
        }

        List<Object> result = new ArrayList<Object>();
        result.add(subsequence);

        // return result
        return result;
    }

    public Resolution getResolution(Range range)
    {
        int length = range.getLength();

        if (length > 50000) { return Resolution.VERY_LOW; }
        return Resolution.VERY_HIGH;
    }

    public void prepareForRendering(String reference, Range range) throws Throwable {

        if (range == null) { return; }

        Resolution r = getResolution(range);
        List<Object> data = null;

        if (r == Resolution.VERY_HIGH) {
            data = this.retrieveAndSaveData(reference, range);
        } else {
            this.saveNullData();
        }

        for (TrackRenderer renderer: getTrackRenderers()) {

            renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.RESOLUTION, r);
            renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.AXIS_RANGE, new AxisRange(range, getDefaultYRange()));

            if (r == Resolution.VERY_HIGH)
            {
                renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.RANGE, range);
                renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.COLOR_SCHEME, this.getColorScheme());
            }

            renderer.setData(data);

        }

    }

    private Range getDefaultYRange()
    {
        return new Range(0, 1);
    }

}
