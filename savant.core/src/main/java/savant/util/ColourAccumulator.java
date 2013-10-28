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
package savant.util;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Path2D;
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
    private final ColourScheme scheme;
    private final Map<Color, Path2D> areas = new HashMap<Color, Path2D>();
    
    public ColourAccumulator(ColourScheme cs) {
        scheme = cs;
    }

    public ColourScheme getScheme() {
        return scheme;
    }

    /**
     * Add a coloured rectangle to our accumulated visual representation.  Assumes that
     * the colour scheme has been set and that the key is in the scheme.
     */
    public void addShape(ColourKey col, Shape shape) {
        addShape(scheme.getColor(col), shape);
    }
    
    /**
     * Add a coloured rectangle for a base to our accumulated visual representation.
     * Assumes that the colour scheme has been set and that the key is in the scheme.
     */
    public void addBaseShape(char baseChar, Shape shape) {
        addShape(scheme.getBaseColor(baseChar), shape);
    }
    
    public void addShape(Color col, Shape shape) {
        if (col != null) {
            if (!areas.containsKey(col)) {
                areas.put(col, new Path2D.Double());
            }
            areas.get(col).append(shape.getPathIterator(null), false);
        }
    }

    /**
     * Fill the accumulated areas with their associated colors.
     */
    public void fill(Graphics2D g2) {
        for (Color c: areas.keySet()) {
            g2.setColor(c);
            g2.fill(areas.get(c));
        }
    }

    /**
     * Draw frames around the accumulated areas in the current color.
     */
    public void draw(Graphics2D g2) {
        for (Color c: areas.keySet()) {
            g2.draw(areas.get(c));
        }
    }
}
