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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import savant.file.DataFormat;
import savant.util.*;
import savant.util.ColorScheme;
import savant.util.DrawingInstructions;
import savant.view.swing.TrackRenderer;
import savant.view.swing.ViewTrack;

import java.awt.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import savant.api.adapter.RangeAdapter;
import savant.controller.ViewTrackController;
import savant.data.sources.FASTAFileDataSource;
import savant.data.types.Record;
import savant.data.types.SequenceRecord;
import savant.file.SavantROFile;

import savant.settings.ColourSettings;

/**
 *
 * @author mfiume
 */
public class SequenceViewTrack extends ViewTrack {

    private static Log log = LogFactory.getLog(SequenceViewTrack.class);

    SavantROFile dFile;
    //Genome genome;
    //String path;

    public SequenceViewTrack(String name, FASTAFileDataSource dataTrack)
    {
        super(name, DataFormat.SEQUENCE_FASTA, dataTrack);
        //setGenome(g);
        //path = g.getFilename();
        setColorScheme(getDefaultColorScheme());
        this.notifyViewTrackControllerOfCreation();
    }

    private ColorScheme getDefaultColorScheme()
    {
        ColorScheme c = new ColorScheme();

        /* add settings here */
        c.addColorSetting("A", ColourSettings.getA());
        c.addColorSetting("C", ColourSettings.getC());
        c.addColorSetting("G", ColourSettings.getG());
        c.addColorSetting("T", ColourSettings.getT());
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
    @Override
    public List<Record> retrieveData(String reference, RangeAdapter range, Resolution resolution)
    {

        SequenceRecord subsequence = null;
        try {
            List<Record> result = getDataSource().getRecords(reference, range, resolution);
            if (result == null || result.isEmpty()) { return null; }
            subsequence = (SequenceRecord)getDataSource().getRecords(reference, range, resolution).get(0);
        } catch (IOException ex) {
            log.error("Error: getting sequence data");
        }

        List<Record> result = new ArrayList<Record>();
        result.add(subsequence);

        // return result
        return result;
    }

    @Override
    public Resolution getResolution(RangeAdapter range)
    {
        long length = range.getLength();

        if (length > 50000) { return Resolution.VERY_LOW; }
        return Resolution.VERY_HIGH;
    }

    @Override
    public void prepareForRendering(String reference, Range range) throws Throwable {

        if (range == null) { return; }

        Resolution r = getResolution(range);
        List<Record> data = null;

        if (r == Resolution.VERY_HIGH) {
            data = retrieveAndSaveData(reference, range);
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
