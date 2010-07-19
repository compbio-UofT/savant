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

/*
 * ContinuousTrack.java
 * Created on Jan 11, 2010
 */

package savant.model.data.continuous;

import savant.model.ContinuousRecord;
import savant.model.Resolution;
import savant.model.data.RecordTrack;
import savant.util.Range;

import java.util.List;

/**
 * TODO:
 * @author vwilliams
 */
public abstract class ContinuousTrack implements RecordTrack<ContinuousRecord> {

    public abstract List<ContinuousRecord> getRecords(String reference, Range range, Resolution resolution);
}
