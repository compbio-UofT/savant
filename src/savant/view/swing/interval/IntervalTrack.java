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

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.adapter.DataSourceAdapter;
import savant.api.adapter.RangeAdapter;
import savant.data.types.GenericIntervalRecord;
import savant.data.types.Interval;
import savant.data.types.Record;
import savant.exception.SavantTrackCreationCancelledException;
import savant.settings.ColourSettings;
import savant.settings.TrackResolutionSettings;
import savant.util.*;
import savant.view.swing.Track;


/**
 *
 * @author mfiume
 */
public class IntervalTrack extends Track {

    private static Log LOG = LogFactory.getLog(IntervalTrack.class);

    public IntervalTrack(DataSourceAdapter intervalTrack) throws SavantTrackCreationCancelledException {
        super(intervalTrack, new IntervalTrackRenderer());
        setColorScheme(getDefaultColorScheme());
        setValidDrawingModes(renderer.getDrawingModes());
        setDrawingMode(renderer.getDefaultDrawingMode());
        this.notifyControllerOfCreation();
    }

    private ColorScheme getDefaultColorScheme() {
        ColorScheme c = new ColorScheme();

        /* add settings here */
        //c.addColorSetting("Background", BrowserDefaults.colorGraphMain);
        c.addColorSetting("Translucent Graph", ColourSettings.getTranslucentGraph());
        c.addColorSetting("Opaque Graph", ColourSettings.getOpaqueGraph());
        c.addColorSetting("Line", ColourSettings.getPointLine());

        return c;
    }

    @Override
    public void resetColorScheme() {
        setColorScheme(getDefaultColorScheme());
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
    public AxisType getYAxisType(Resolution r) {
        return getDrawingMode() == DrawingMode.ARC ? AxisType.INTEGER : AxisType.INTEGER_GRIDLESS;
    }

    public static Resolution getDefaultModeResolution(Range range) {
        return range.getLength() > TrackResolutionSettings.getIntervalLowToHighThresh() ? Resolution.LOW : Resolution.HIGH;
    }

    public static Resolution getArcModeResolution(Range range) {
        return range.getLength() > TrackResolutionSettings.getIntervalLowToHighThresh() ? Resolution.LOW : Resolution.HIGH;
    }

    public static Resolution getSquishModeResolution(Range range) {
        return range.getLength() > TrackResolutionSettings.getIntervalLowToHighThresh() ? Resolution.LOW : Resolution.HIGH;
    }

    @Override
    public void prepareForRendering(String reference, Range range) {

        Resolution r = getResolution(range);

        switch (r) {
            case HIGH:
                renderer.addInstruction(DrawingInstruction.PROGRESS, "Loading track...");
                requestData(reference, range);
                break;
            default:
                renderer.addInstruction(DrawingInstruction.ERROR, ZOOM_MESSAGE);
                break;
        }

        renderer.addInstruction(DrawingInstruction.RESOLUTION, r);
        renderer.addInstruction(DrawingInstruction.RANGE, range);
        renderer.addInstruction(DrawingInstruction.COLOR_SCHEME, getColorScheme());
        renderer.addInstruction(DrawingInstruction.REFERENCE_EXISTS, containsReference(reference));

        if (getDrawingMode() != DrawingMode.ARC) {
            renderer.addInstruction(DrawingInstruction.AXIS_RANGE, AxisRange.initWithRanges(range, getDefaultYRange()));
        }
        renderer.addInstruction(DrawingInstruction.MODE, getDrawingMode());
        renderer.addInstruction(DrawingInstruction.SELECTION_ALLOWED, true);
    }

    private Range getDefaultYRange() {
        return new Range(0, 1);
    }

    public static int getMaxValue(List<Record> data) {
        double max = 0;
        for (Record r: data) {
            Interval interval = ((GenericIntervalRecord)r).getInterval();
            double val = interval.getLength();
            if (val > max) max = val;
        }
        return (int)Math.ceil(max);
    }
}
