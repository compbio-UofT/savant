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
package savant.model.data;

import savant.model.Resolution;
import savant.util.Range;

import java.util.List;
import savant.controller.property.SampleProperty;

/**
 * Interface to define a data track which returns records.
 *
 * @author vwilliams
 * @param <E>
 */
public interface RecordTrack<E> extends Track {

    /**
     * Get all records in the given range at the given resolution
     *
     * @param range
     * @param resolution
     * @return an ordered list of records
     */
    public List<E> getRecords(Range range, Resolution resolution);

    /**
     * Close this track.
     */
    public void close();
}
