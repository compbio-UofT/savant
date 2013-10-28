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

import java.awt.Point;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.Timer;

/**
 * Utility class which fires an action event when the mouse has sat for more than a specified
 * time in the current location.
 *
 * @author tarkvara
 */
public abstract class Hoverer extends MouseAdapter implements ActionListener {
    private static final double HOVER_THRESHOLD = 2.0;
    private static final int HOVER_INTERVAL = 1000;

    protected Point hoverPos = null;
    private final Timer timer;

    public Hoverer() {
        timer = new Timer(HOVER_INTERVAL, this);
        timer.setRepeats(false);
    }

    @Override
    public void mouseMoved(MouseEvent evt) {
        Point pt = evt.getPoint();
        if (hoverPos != null) {
            if (!isHoverable(pt)) {
                hoverPos = pt;
                timer.restart();
            }
        } else {
            hoverPos = pt;
            timer.start();
        }
    }
                
    @Override
    public void mouseExited(MouseEvent me) {
        timer.stop();
        hoverPos = null;
    }

    public boolean isHoverable(Point pt) {
        if (hoverPos != null) {
            return pt.distanceSq(hoverPos) <= HOVER_THRESHOLD;
        }
        return false;
    }
}
