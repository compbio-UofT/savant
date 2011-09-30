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

package savant.data.event;

import java.awt.image.BufferedImage;
import savant.util.Range;

/**
 *
 * @author Andrew
 */
public class ExportEvent {

    Range range;
    BufferedImage image;
    

    /**
     * Constructor when export is ready
     * @param range the range of the current export
     */
    public ExportEvent(Range range, BufferedImage image) {
        this.range = range;
        this.image = image;
    }

    public Range getRange(){
        return range;
    }

    public BufferedImage getImage(){
        return image;
    }
}
