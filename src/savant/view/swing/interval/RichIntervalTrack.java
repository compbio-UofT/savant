/*
 *    Copyright 2010-2011 University of Toronto
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

package savant.view.swing.interval;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.adapter.RangeAdapter;
import savant.data.sources.DataSource;
import savant.exception.SavantTrackCreationCancelledException;
import savant.settings.ColourSettings;
import savant.util.*;
import savant.view.swing.Track;

/**
 * View track for a BED interval file (containing BED Interval Records)
 * 
 * @author vwilliams
 */
public class RichIntervalTrack extends Track {

    private static final Log LOG = LogFactory.getLog(RichIntervalTrack.class);
    private boolean itemRGBEnabled = false;
    private boolean scoreEnabled = false;

    /*
    public enum DrawingMode {
        STANDARD,
        SQUISH
    };
     * 
     */



    public RichIntervalTrack(DataSource bedSource) throws SavantTrackCreationCancelledException {
        super(bedSource, new RichIntervalTrackRenderer());
        setColorScheme(getDefaultColorScheme());
        setValidDrawingModes(renderer.getDrawingModes());
        setDrawingMode(renderer.getDefaultDrawingMode());
        notifyControllerOfCreation();
    }


    @Override
    public void prepareForRendering(String reference, Range range) {
        Resolution r = getResolution(range);
        renderer.addInstruction(DrawingInstruction.PROGRESS, "Loading track...");
        if (r == Resolution.VERY_HIGH || r == Resolution.HIGH){
            requestData(reference, range);
        } else {
            renderer.addInstruction(DrawingInstruction.ERROR, "Zoom in to see data");
        }
        
        renderer.addInstruction(DrawingInstruction.RANGE, range);
        renderer.addInstruction(DrawingInstruction.RESOLUTION, r);
        renderer.addInstruction(DrawingInstruction.COLOR_SCHEME, getColorScheme());
        renderer.addInstruction(DrawingInstruction.AXIS_RANGE, AxisRange.initWithRanges(range, getDefaultYRange()));
        renderer.addInstruction(DrawingInstruction.REFERENCE_EXISTS, containsReference(reference));
        renderer.addInstruction(DrawingInstruction.MODE, getDrawingMode());
        renderer.addInstruction(DrawingInstruction.SELECTION_ALLOWED, true);
        renderer.addInstruction(DrawingInstruction.ITEMRGB, this.itemRGBEnabled);
        renderer.addInstruction(DrawingInstruction.SCORE, this.scoreEnabled);
    }

    /*
    @Override
    public Resolution getResolution(RangeAdapter range) {
        return getResolution(range, getDrawMode());
    }

    public Resolution getResolution(RangeAdapter range, String mode)
    {
        return getDefaultModeResolution(range);
    }

    public Resolution getDefaultModeResolution(RangeAdapter range)
    {
        long length = range.getLength();

        if (length > 1000000) { return Resolution.LOW; }
        else { return Resolution.VERY_HIGH; }
    }
     * 
     */

    private ColorScheme getDefaultColorScheme() {
        ColorScheme c = new ColorScheme();

        /* add settings here */
        c.addColorSetting("Forward Strand", ColourSettings.getForwardStrand());
        c.addColorSetting("Reverse Strand", ColourSettings.getReverseStrand());
        c.addColorSetting("Translucent Graph", ColourSettings.getTranslucentGraph());
        c.addColorSetting("Line", ColourSettings.getLine());
        c.addColorSetting("Text", ColourSettings.getText());

        return c;
    }

    @Override
    public void resetColorScheme() {
        setColorScheme(getDefaultColorScheme());
    }
    
    private Range getDefaultYRange() {
        return new Range(0, 1);
    }

    public void toggleItemRGBEnabled(){
        this.itemRGBEnabled = !this.itemRGBEnabled;
        renderer.addInstruction(DrawingInstruction.ITEMRGB, this.itemRGBEnabled);
    }

    public void toggleScoreEnabled(){
        this.scoreEnabled = !this.scoreEnabled;
        renderer.addInstruction(DrawingInstruction.SCORE, this.scoreEnabled);
    }

    @Override
    public Resolution getResolution(RangeAdapter range) {
        return IntervalTrack.getDefaultModeResolution((Range)range);
    }
 
}
