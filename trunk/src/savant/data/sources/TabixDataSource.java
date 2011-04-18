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

package savant.data.sources;

import java.io.IOException;
import java.util.List;
import savant.api.adapter.RangeAdapter;
import savant.data.types.TabixIntervalRecord;
import savant.file.DataFormat;
import savant.util.Resolution;

/**
 *
 * @author mfiume
 */
public abstract class TabixDataSource implements DataSource<TabixIntervalRecord> {

    @Override
    public abstract List<TabixIntervalRecord> getRecords(String reference, RangeAdapter range, Resolution resolution) throws IOException;

    @Override
    public final DataFormat getDataFormat() {
        return DataFormat.TABIX;
    }

    @Override
    public Object getExtraData() {
        return null;
    }
}
