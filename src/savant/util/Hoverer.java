/*
 *    Copyright 2012 University of Toronto
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
        System.out.println("mouseExited stopping timer");
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
