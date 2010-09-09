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
 * Session.java
 * Created on Sep 9, 2010
 */

package savant.util;

import java.util.List;
import savant.data.types.Genome;
import savant.util.Range;

/**
 *
 * @author mfiume
 */
public class Session {

    public String genomeName;
    public Genome genome;
    public String reference;
    public Range range;
    public List<String> trackPaths;
    public String bookmarkPath;

    public Session(String genomeName, Genome g, List<String> tracknames, String reference, Range range, String bookmarks) {
        this.genomeName = genomeName;
        this.genome = g;
        this.trackPaths = tracknames;
        this.reference = reference;
        this.range = range;
        this.bookmarkPath = bookmarks;
    }


}