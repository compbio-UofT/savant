/**
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
import savant.settings.ResolutionSettings;
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
        return range.getLength() > ResolutionSettings.getIntervalLowToHighThreshold() ? Resolution.LOW : Resolution.HIGH;
    }

    public static Resolution getArcModeResolution(Range range) {
        return range.getLength() > ResolutionSettings.getIntervalLowToHighThreshold() ? Resolution.LOW : Resolution.HIGH;
    }

    public static Resolution getSquishModeResolution(Range range) {
        return range.getLength() > ResolutionSettings.getIntervalLowToHighThreshold() ? Resolution.LOW : Resolution.HIGH;
    }

    @Override
    public void prepareForRendering(String ref, Range r) {

        Resolution res = getResolution(r);

        switch (res) {
            case HIGH:
                renderer.addInstruction(DrawingInstruction.PROGRESS, "Retrieving data...");
                requestData(ref, r);
                break;
            default:
                renderer.addInstruction(DrawingInstruction.ERROR, ZOOM_MESSAGE);
                break;
        }

        renderer.addInstruction(DrawingInstruction.RESOLUTION, res);
        renderer.addInstruction(DrawingInstruction.RANGE, r);
        renderer.addInstruction(DrawingInstruction.COLOUR_SCHEME, getColourScheme());
        renderer.addInstruction(DrawingInstruction.REFERENCE_EXISTS, containsReference(ref));

        if (getDrawingMode() != DrawingMode.ARC) {
            renderer.addInstruction(DrawingInstruction.AXIS_RANGE, new AxisRange(r, new Range(0, 1)));
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
