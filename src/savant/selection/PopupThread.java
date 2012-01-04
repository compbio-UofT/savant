/*
 *    Copyright 2010-2012 University of Toronto
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

                        if (p1 != null && p2 != null && p1.equals(p2) && !InterfaceSettings.isPopupsDisabled()) {
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
