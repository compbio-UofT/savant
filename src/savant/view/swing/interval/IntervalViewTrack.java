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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import savant.data.types.GenericIntervalRecord;
import savant.data.types.Interval;
import savant.exception.SavantTrackCreationCancelledException;
import savant.file.DataFormat;
import savant.util.*;
import savant.data.sources.file.GenericIntervalFileDataSource;
import savant.util.ColorScheme;
import savant.util.DrawingInstructions;
import savant.util.Mode;
import savant.view.swing.TrackRenderer;
import savant.view.swing.ViewTrack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import savant.api.adapter.ModeAdapter;
import savant.api.adapter.RangeAdapter;
import savant.data.sources.DataSource;
import savant.data.types.Record;
import savant.settings.ColourSettings;

/**
 *
 * @author mfiume
 */
public class IntervalViewTrack extends ViewTrack {

    private static Log LOG = LogFactory.getLog(IntervalViewTrack.class);

    public enum DrawingMode { SQUISH, PACK, ARC };

    private static final Mode SQUISH_MODE = Mode.fromObject(DrawingMode.SQUISH, "All on one line");
    private static final Mode PACK_MODE = Mode.fromObject(DrawingMode.PACK, "Minimum number of lines");
    private static final Mode ARC_MODE = Mode.fromObject(DrawingMode.ARC, "Arcs");

    public IntervalViewTrack(DataSource intervalTrack) throws SavantTrackCreationCancelledException {
        super(intervalTrack);
        setColorScheme(getDefaultColorScheme());
        setDrawModes(getDefaultDrawModes());
        setDrawMode(PACK_MODE);
        this.notifyViewTrackControllerOfCreation();
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

    public Resolution getResolution(RangeAdapter range, ModeAdapter mode) {
        if (mode.getName().equals("SQUISH")) {
            return getSquishModeResolution(range);
        } else if (mode.getName().equals("ARC")) {
            return getArcModeResolution(range);
        } else if (mode.getName().equals("PACK")) {
            return getDefaultModeResolution(range);
        } else {
            LOG.warn("Unrecognized draw mode " + mode.getName());
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
    
    public final List<ModeAdapter> getDefaultDrawModes() {
        List<ModeAdapter> modes = new ArrayList<ModeAdapter>();

        modes.add(SQUISH_MODE);
        modes.add(PACK_MODE);
        modes.add(ARC_MODE);
        return modes;
    }

    /**
     * getData
     *     Get data in the specified range at the specified resolution
     */
    @Override
    public List<Record> retrieveData(String reference, RangeAdapter range, Resolution resolution) throws IOException {
        return getDataSource().getRecords(reference, range, resolution);
    }

    @Override
    public void prepareForRendering(String reference, Range range) throws IOException {

        Resolution r = getResolution(range);

        List<Record> data = null;
        switch (r) {
            case VERY_HIGH:
                data = retrieveAndSaveData(reference, range);
                break;
            default:
                break;
        }

        for (TrackRenderer renderer : getTrackRenderers()) {
            boolean contains = (this.getDataSource().getReferenceNames().contains(reference) || this.getDataSource().getReferenceNames().contains(MiscUtils.homogenizeSequence(reference)));
            renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.RESOLUTION, r);
            renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.COLOR_SCHEME, this.getColorScheme());
            renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.REFERENCE_EXISTS, contains);

            if (getDrawMode().getName().equals("ARC")) {
                int maxDataValue = getMaxValue(data);
                renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.AXIS_RANGE, AxisRange.initWithRanges(range, new Range(0,(int)Math.round(Math.log(maxDataValue)))));
            }
            else renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.AXIS_RANGE, AxisRange.initWithRanges(range, getDefaultYRange()));
            renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.MODE, getDrawMode());
            renderer.getDrawingInstructions().addInstruction(DrawingInstructions.InstructionName.SELECTION_ALLOWED, true);
            renderer.setData(data);
        }

    }

    private Range getDefaultYRange() {
        return new Range(0, 1);
    }

    private int getMaxValue(List<Record> data) {
        double max = 0;
        for (Record r: data) {
            Interval interval = ((GenericIntervalRecord)r).getInterval();
            double val = interval.getLength();
            if (val > max) max = val;
        }
        return (int)Math.ceil(max);
    }

    @Override
    public Mode getDefaultDrawMode() {
        return PACK_MODE;
    }
}
