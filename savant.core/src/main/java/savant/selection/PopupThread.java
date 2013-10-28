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
package savant.selection;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import savant.settings.InterfaceSettings;
import savant.view.swing.Frame;
import savant.view.swing.GraphPane;

/**
 * Thread which hangs around and checks to see if the mouse is stationary over a position in the GraphPane.
 *
 * @author AndrewBrook
 */
public class PopupThread implements Runnable {

    private GraphPane graphPane;

    public PopupThread(GraphPane gp) {
        graphPane = gp;
        graphPane.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                graphPane.popupThread.interrupt();
            }
        });
    }

    @Override
    public void run() {
        boolean retry = false;
        while (true) {
            try {
                //sleep until interrupted (basically)
                Thread.sleep(999999999);
            } catch (InterruptedException ex) {
                //sleep has been interrupted
                retry = true;
                while (retry) {
                    retry = false;
                    //Point p1 = gp.getMousePosition();
                    Frame f = (Frame)graphPane.getParentFrame();
                    Point p1 = f.getLayeredPane().getMousePosition();
                    if (p1 != null) {
                        p1.y += graphPane.getVerticalScrollBar().getValue();
                    }

                    try {
                        //sleep for 1 sec and then compare
                        Thread.sleep(1000);
                        //Point p2 = gp.getMousePosition();
                        Point p2 = f.getLayeredPane().getMousePosition();
                        if (p2 != null) {
                            p2.y += graphPane.getVerticalScrollBar().getValue();
                        }

                        if (p1 != null && p2 != null && p1.equals(p2) && !InterfaceSettings.arePopupsDisabled()) {
                            graphPane.tryPopup(p2);
                        }
                    } catch (InterruptedException ignored) {
                        //mouse is still moving
                        retry = true;
                    }
                }
            }
        }
    }

    
}
