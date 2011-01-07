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
package savant.view.swing.interval;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.adapter.RangeAdapter;
import savant.data.sources.DataSource;
import savant.data.types.GenericIntervalRecord;
import savant.data.types.Interval;
import savant.data.types.Record;
import savant.exception.SavantTrackCreationCancelledException;
import savant.settings.ColourSettings;
import savant.util.*;
import savant.view.swing.Track;


/**
 *
 * @author mfiume
 */
public class IntervalTrack extends Track {

    private static Log LOG = LogFactory.getLog(IntervalTrack.class);

    public enum DrawingMode { SQUISH, PACK, ARC };

    public IntervalTrack(DataSource intervalTrack) throws SavantTrackCreationCancelledException {
        super(intervalTrack, new IntervalTrackRenderer());
        setColorScheme(getDefaultColorScheme());
        setDrawModes(this.renderer.getRenderingModes());
        setDrawMode(this.renderer.getDefaultRenderingMode());
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
    public Resolution getResolution(RangeAdapter range) { return getResolution(range, getDrawMode()); }

    public Resolution getResolution(RangeAdapter range, String mode) {
        if (mode.equals(IntervalTrackRenderer.SQUISH_MODE)) {
            return getSquishModeResolution(range);
        } else if (mode.equals(IntervalTrackRenderer.ARC_MODE)) {
            return getArcModeResolution(range);
        } else if (mode.equals(IntervalTrackRenderer.PACK_MODE)) {
            return getDefaultModeResolution(range);
        } else {
            LOG.warn("Unrecognized draw mode " + mode);
            return getDefaultModeResolution(range);
        }
    }

    public Resolution getDefaultModeResolution(RangeAdapter range) {
        return Resolution.VERY_HIGH;
    }

    public Resolution getArcModeResolution(RangeAdapter range)
    {
        return Resolution.VERY_HIGH;
    }

    public Resolution getSquishModeResolution(RangeAdapter range) {
        return Resolution.VERY_HIGH;
    }

    @Override
    public void prepareForRendering(String reference, Range range) {

        Resolution r = getResolution(range);

        switch (r) {
            case VERY_HIGH:
                renderer.addInstruction(DrawingInstruction.PROGRESS, "Loading track...");
                requestData(reference, range);
                break;
            default:
                break;
        }

        renderer.addInstruction(DrawingInstruction.RESOLUTION, r);
        renderer.addInstruction(DrawingInstruction.RANGE, range);
        renderer.addInstruction(DrawingInstruction.COLOR_SCHEME, getColorScheme());
        renderer.addInstruction(DrawingInstruction.REFERENCE_EXISTS, containsReference(reference));

        if (!getDrawMode().equals(IntervalTrackRenderer.ARC_MODE)) {
            renderer.addInstruction(DrawingInstruction.AXIS_RANGE, AxisRange.initWithRanges(range, getDefaultYRange()));
        }
        renderer.addInstruction(DrawingInstruction.MODE, getDrawMode());
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
