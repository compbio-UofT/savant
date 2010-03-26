/*
 *    Copyright 2009-2010 University of Toronto
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
package savant.model.data.point;

import savant.model.PointRecord;
import savant.model.Resolution;
import savant.model.data.RecordTrack;
import savant.util.Range;

import java.util.List;

/**
 * TODO:
 * @author vwilliams
 */
public abstract class PointTrack implements RecordTrack<PointRecord> {

    /**
     * Get all point items in a given range and resolution
     * @param range a Range
     * @param resolution a Resolution
     * @return a List of all point items in the range
     */
    public abstract List<PointRecord> getRecords(Range range, Resolution resolution);
}
