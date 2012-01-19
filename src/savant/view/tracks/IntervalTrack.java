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

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.adapter.DataSourceAdapter;
import savant.api.adapter.RangeAdapter;
import savant.api.data.Interval;
import savant.api.data.IntervalRecord;
import savant.api.data.Record;
import savant.api.util.Resolution;
import savant.exception.SavantTrackCreationCancelledException;
import savant.settings.TrackResolutionSettings;
import savant.util.*;


/**
 *
 * @author mfiume
 */
public class IntervalTrack extends Track {

    private static Log LOG = LogFactory.getLog(IntervalTrack.class);

    public IntervalTrack(DataSourceAdapter intervalTrack) throws SavantTrackCreationCancelledException {
        super(intervalTrack, new IntervalTrackRenderer());
        drawingMode = DrawingMode.PACK;
    }

    @Override
    public ColourScheme getDefaultColourScheme() {
        return new ColourScheme(ColourKey.TRANSLUCENT_GRAPH, ColourKey.OPAQUE_GRAPH, ColourKey.INTERVAL_LINE);
    }

    @Override
    public Resolution getResolution(RangeAdapter range) {
        return getResolution((Range)range, getDrawingMode());
    }

    public Resolution getResolution(Range range, DrawingMode mode) {
        switch (mode) {
            case SQUISH:
                return getSquishModeResolution(range);
            case ARC:
                return getArcModeResolution(range);
            case PACK:
                return getDefaultModeResolution(range);
            default:
                LOG.warn("Unrecognized draw mode " + mode);
                return getDefaultModeResolution(range);
        }
    }

    @Override
    public DrawingMode[] getValidDrawingModes() {
        return new DrawingMode[] { DrawingMode.PACK, DrawingMode.SQUISH, DrawingMode.ARC };
    }

    @Override
    public AxisType getYAxisType(Resolution r) {
        return getDrawingMode() == DrawingMode.ARC ? AxisType.INTEGER : AxisType.INTEGER_GRIDLESS;
    }

    public static Resolution getDefaultModeResolution(Range range) {
        return range.getLength() > TrackResolutionSettings.getIntervalLowToHighThreshold() ? Resolution.LOW : Resolution.HIGH;
    }

    public static Resolution getArcModeResolution(Range range) {
        return range.getLength() > TrackResolutionSettings.getIntervalLowToHighThreshold() ? Resolution.LOW : Resolution.HIGH;
    }

    public static Resolution getSquishModeResolution(Range range) {
        return range.getLength() > TrackResolutionSettings.getIntervalLowToHighThreshold() ? Resolution.LOW : Resolution.HIGH;
    }

    @Override
    public void prepareForRendering(String reference, Range range) {

        Resolution r = getResolution(range);

        switch (r) {
            case HIGH:
                renderer.addInstruction(DrawingInstruction.PROGRESS, "Retrieving data...");
                requestData(reference, range);
                break;
            default:
                renderer.addInstruction(DrawingInstruction.ERROR, ZOOM_MESSAGE);
                break;
        }

        renderer.addInstruction(DrawingInstruction.RESOLUTION, r);
        renderer.addInstruction(DrawingInstruction.RANGE, range);
        renderer.addInstruction(DrawingInstruction.COLOUR_SCHEME, getColourScheme());
        renderer.addInstruction(DrawingInstruction.REFERENCE_EXISTS, containsReference(reference));

        if (getDrawingMode() != DrawingMode.ARC) {
            renderer.addInstruction(DrawingInstruction.AXIS_RANGE, new AxisRange(range, new Range(0, 1)));
        }
        renderer.addInstruction(DrawingInstruction.MODE, getDrawingMode());
        renderer.addInstruction(DrawingInstruction.SELECTION_ALLOWED, true);
    }

    public static int getMaxValue(List<Record> data) {
        double max = 0;
        for (Record r: data) {
            Interval interval = ((IntervalRecord)r).getInterval();
            double val = interval.getLength();
            if (val > max) max = val;
        }
        return (int)Math.ceil(max);
    }
}
