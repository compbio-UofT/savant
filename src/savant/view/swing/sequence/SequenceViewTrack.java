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

import savant.data.types.Genome;
import savant.file.FileFormat;
import savant.util.*;
import savant.util.ColorScheme;
import savant.util.DrawingInstructions;
import savant.view.swing.Savant;
import savant.view.swing.TrackRenderer;
import savant.view.swing.ViewTrack;

import java.awt.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import savant.controller.ViewTrackController;
import savant.data.sources.FASTAFileDataSource;
import savant.data.types.SequenceRecord;
import savant.file.SavantROFile;

import savant.settings.ColourSettings;

/**
 *
 * @author mfiume
 */
public class SequenceViewTrack extends ViewTrack {

    SavantROFile dFile;
    //Genome genome;
    //String path;

    public SequenceViewTrack(String name, FASTAFileDataSource dataTrack) throws FileNotFoundException
    {
        super(name, FileFormat.SEQUENCE_FASTA, dataTrack);
        //setGenome(g);
        //path = g.getFilename();
        setColorScheme(getDefaultColorScheme());
        this.notifyViewTrackControllerOfCreation();
    }

    private ColorScheme getDefaultColorScheme()
    {
        ColorScheme c = new ColorScheme();

        /* add settings here */
        c.addColorSetting("A", ColourSettings.A_COLOR);
        c.addColorSetting("C", ColourSettings.C_COLOR);
        c.addColorSetting("G", ColourSettings.G_COLOR);
        c.addColorSetting("T", ColourSettings.T_COLOR);
        c.addColorSetting("Line", Color.black);
        c.addColorSetting("Background", new Color(100,100,100,220));

        return c;
    }

    @Override
    public void resetColorScheme() {
        setColorScheme(getDefaultColorScheme());
    }

    //private void setGenome(Genome genome) { this.genome = genome; }
    //private Genome getGenome() { return this.genome; }


    /*
     * getData
     *     Get data in the specified range at the specified resolution
     */
    public List<Object> retrieveData(String reference, Range range, Resolution resolution)
    {

        String subsequence = "";
        try {
            List result = this.getDataSource().getRecords(reference, range, resolution);
            if (result == null || result.isEmpty()) { return null; }
            subsequence = ((SequenceRecord) this.getDataSource().getRecords(reference, range, resolution).get(0)).getSequence();
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
            boolean contains = (this.getDataSource().getReferenceNames().contains(reference) || this.getDataSource().getReferenceNames().contains(MiscUtils.homogenizeSequence(reference)));
            renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.RESOLUTION, r);
            renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.AXIS_RANGE, AxisRange.initWithRanges(range, getDefaultYRange()));
            renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.REFERENCE_EXISTS, this.getDataSource().getReferenceNames().contains(reference));
            renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.SELECTION_ALLOWED, false);

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
