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

import savant.api.adapter.DataSourceAdapter;
import savant.api.adapter.RangeAdapter;
import savant.api.util.Resolution;
import savant.exception.SavantTrackCreationCancelledException;
import savant.util.*;


/**
 *
 * @author mfiume
 */
public class PointTrack extends Track {

    public PointTrack(DataSourceAdapter dataSource) throws SavantTrackCreationCancelledException {
        super(dataSource, new PointTrackRenderer());
    }

    @Override
    public ColourScheme getDefaultColourScheme() {
        return new ColourScheme(ColourKey.POINT_FILL, ColourKey.POINT_LINE);
    }

    @Override
    public Resolution getResolution(RangeAdapter range) {
        return range.getLength() > 100000 ? Resolution.LOW : Resolution.HIGH;
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
                saveNullData(r);
                break;
        }

        renderer.addInstruction(DrawingInstruction.RESOLUTION, res);
        renderer.addInstruction(DrawingInstruction.COLOUR_SCHEME, this.getColourScheme());
        renderer.addInstruction(DrawingInstruction.AXIS_RANGE, new AxisRange(r, new Range(0, 1)));
        renderer.addInstruction(DrawingInstruction.REFERENCE_EXISTS, containsReference(ref));
        renderer.addInstruction(DrawingInstruction.SELECTION_ALLOWED, true);
    }
}
