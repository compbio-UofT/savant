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

package savant.view.tracks;

import savant.api.util.Resolution;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.adapter.DataSourceAdapter;
import savant.api.adapter.RangeAdapter;
import savant.exception.SavantTrackCreationCancelledException;
import savant.util.*;
import savant.view.tracks.Track;

/**
 * View track for a BED interval file (containing BED Interval Records)
 * 
 * @author vwilliams
 */
public class RichIntervalTrack extends Track {

    private static final Log LOG = LogFactory.getLog(RichIntervalTrack.class);
    private boolean itemRGBEnabled = false;
    private boolean scoreEnabled = false;
    private boolean alternateName = false;

    public RichIntervalTrack(DataSourceAdapter bedSource) throws SavantTrackCreationCancelledException {
        super(bedSource, new RichIntervalTrackRenderer());
    }


    @Override
    public void prepareForRendering(String reference, Range range) {
        Resolution r = getResolution(range);
        if (r == Resolution.HIGH){
            renderer.addInstruction(DrawingInstruction.PROGRESS, "Retrieving data...");
            requestData(reference, range);
        } else {
            renderer.addInstruction(DrawingInstruction.ERROR, ZOOM_MESSAGE);
        }
        
        renderer.addInstruction(DrawingInstruction.RANGE, range);
        renderer.addInstruction(DrawingInstruction.RESOLUTION, r);
        renderer.addInstruction(DrawingInstruction.COLOUR_SCHEME, getColourScheme());
        renderer.addInstruction(DrawingInstruction.AXIS_RANGE, AxisRange.initWithRanges(range, getDefaultYRange()));
        renderer.addInstruction(DrawingInstruction.REFERENCE_EXISTS, containsReference(reference));
        renderer.addInstruction(DrawingInstruction.MODE, getDrawingMode());
        renderer.addInstruction(DrawingInstruction.SELECTION_ALLOWED, true);
        renderer.addInstruction(DrawingInstruction.ITEMRGB, itemRGBEnabled);
        renderer.addInstruction(DrawingInstruction.SCORE, scoreEnabled);
        renderer.addInstruction(DrawingInstruction.ALTERNATE_NAME, alternateName);
    }

    @Override
    public ColourScheme getDefaultColourScheme() {
        return new ColourScheme(ColourKey.FORWARD_STRAND, ColourKey.REVERSE_STRAND, ColourKey.INTERVAL_LINE, ColourKey.INTERVAL_TEXT);
    }

    @Override
    public DrawingMode[] getValidDrawingModes() {
        return new DrawingMode[] { DrawingMode.STANDARD, DrawingMode.SQUISH };
    }

    private Range getDefaultYRange() {
        return new Range(0, 1);
    }

    public void toggleItemRGBEnabled(){
        itemRGBEnabled = !itemRGBEnabled;
        renderer.addInstruction(DrawingInstruction.ITEMRGB, itemRGBEnabled);
    }

    public void toggleScoreEnabled(){
        scoreEnabled = !scoreEnabled;
        renderer.addInstruction(DrawingInstruction.SCORE, scoreEnabled);
    }

    public void toggleAlternateName() {
        alternateName = !alternateName;
        renderer.addInstruction(DrawingInstruction.ALTERNATE_NAME, alternateName);
    }

    @Override
    public Resolution getResolution(RangeAdapter range) {
        return IntervalTrack.getDefaultModeResolution((Range)range);
    }
 
}
