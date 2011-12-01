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

package savant.util;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;

/**
 * In many cases, we're drawing a whole slew of shapes in close proximity to each other.
 * For performance reasons, and to reduce artefacts due to anti-aliasing, we accumulate
 * all the drawing and then dump it out at once.
 *
 * @author tarkvara
 */
public class ColourAccumulator {
    ColourScheme scheme;
    Map<Color, Path2D> areas = new HashMap<Color, Path2D>();
    
    public ColourAccumulator(ColourScheme cs) {
        scheme = cs;
    }

    /**
     * Add a coloured rectangle to our accumulated visual representation.
     */
    public void addShape(ColourKey col, Rectangle2D shape) {
        Color c = scheme.getColor(col);
        if (!areas.containsKey(c)) {
            areas.put(c, new Path2D.Double());
        }
        areas.get(c).append(shape.getPathIterator(null), false);
    }
    
    /**
     * Add a coloured rectangle for a base to our accumulated visual representation.
     */
    public void addBaseShape(char baseChar, Rectangle2D shape) {
        Color c = scheme.getBaseColor(baseChar);
        if (c != null) {
            if (!areas.containsKey(c)) {
                areas.put(c, new Path2D.Double());
            }
            areas.get(c).append(shape.getPathIterator(null), false);
        }
    }
    
    public void render(Graphics2D g2) {
        for (Color c: areas.keySet()) {
            g2.setColor(c);
            g2.fill(areas.get(c));
        }
    }
}