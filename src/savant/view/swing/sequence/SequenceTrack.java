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

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.adapter.RangeAdapter;
import savant.data.sources.DataSource;
import savant.data.types.Record;
import savant.data.types.SequenceRecord;
import savant.exception.SavantTrackCreationCancelledException;
import savant.file.SavantROFile;
import savant.settings.ColourSettings;
import savant.util.*;
import savant.view.swing.Track;


/**
 *
 * @author mfiume
 */
public class SequenceTrack extends Track {

    private static Log log = LogFactory.getLog(SequenceTrack.class);

    SavantROFile dFile;
    //Genome genome;
    //String path;

    public SequenceTrack(DataSource dataTrack) throws SavantTrackCreationCancelledException {
        super(dataTrack, new SequenceTrackRenderer());

        setColorScheme(getDefaultColorScheme());
        notifyControllerOfCreation();
    }

    private ColorScheme getDefaultColorScheme()
    {
        ColorScheme c = new ColorScheme();

        /* add settings here */
        c.addColorSetting("A", ColourSettings.getA());
        c.addColorSetting("C", ColourSettings.getC());
        c.addColorSetting("G", ColourSettings.getG());
        c.addColorSetting("T", ColourSettings.getT());
        c.addColorSetting("N", ColourSettings.getN());
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
    protected synchronized List<Record> retrieveData(String reference, RangeAdapter range, Resolution resolution) {

        SequenceRecord subsequence = null;
        try {
            List<Record> result = super.retrieveData(reference, range, resolution);
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
    public void prepareForRendering(String reference, Range range) {

        if (range == null) { return; }

        Resolution r = getResolution(range);

        if (r == Resolution.VERY_HIGH) {
            renderer.addInstruction(DrawingInstruction.PROGRESS, "Loading sequence track...");
            requestData(reference, range);
        } else {
            renderer.addInstruction(DrawingInstruction.ERROR, "Zoom in to see data");
            saveNullData();
        }

        renderer.addInstruction(DrawingInstruction.RESOLUTION, r);
        renderer.addInstruction(DrawingInstruction.AXIS_RANGE, AxisRange.initWithRanges(range, getDefaultYRange()));
        renderer.addInstruction(DrawingInstruction.REFERENCE_EXISTS, containsReference(reference));
        renderer.addInstruction(DrawingInstruction.SELECTION_ALLOWED, false);

        if (r == Resolution.VERY_HIGH) {
            renderer.addInstruction(DrawingInstruction.RANGE, range);
            renderer.addInstruction(DrawingInstruction.COLOR_SCHEME, this.getColorScheme());
        }
    }

    private Range getDefaultYRange() {
        return new Range(0, 1);
    }
}
