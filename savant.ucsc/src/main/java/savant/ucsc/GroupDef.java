/*
 *    Copyright 2011 University of Toronto
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

package savant.ucsc;

import java.util.ArrayList;
import java.util.List;

/**
 * Class which keeps track of information about a given track grouping in the UCSC database.
 * 
 * @author tarkvara
 */
public class GroupDef {
    final String name;
    List<TrackDef> tracks = new ArrayList<TrackDef>();

    GroupDef(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public TrackDef[] getTracks() {
        return tracks.toArray(new TrackDef[0]);
    }
}
