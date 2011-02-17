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

package savant.selection;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import savant.view.swing.GraphPane;

/**
 *
 * @author AndrewBrook
 */
public class PopupThread implements Runnable {

    private GraphPane gp;

    public PopupThread(GraphPane graphPane){
        this.gp = graphPane;
        this.gp.addMouseMotionListener(new MouseMotionListener() {
            public void mouseDragged(MouseEvent e) {}
            public void mouseMoved(MouseEvent e) {
                gp.popupThread.interrupt();
            }
        });
    }

    @Override
    public void run() {
        boolean retry = false;
        while(true){
            try {
                //sleep until interrupted (basically)
                Thread.sleep(999999999);
            } catch (InterruptedException ex) {
                //sleep has been interrupted
                retry = true;
                while(retry){
                    retry = false;
                    //Point p1 = gp.getMousePosition();
                    Point p1 = gp.getParentFrame().getLayeredPane().getMousePosition();

                    try {
                        //sleep for 1 sec and then compare
                        Thread.sleep(1000);
                        //Point p2 = gp.getMousePosition();
                        Point p2 = gp.getParentFrame().getLayeredPane().getMousePosition();

                        if(p1 != null && p2 != null && p1.equals(p2))
                            gp.tryPopup(p2);
                        /*if(p1 != null && p2 != null && p1.equals(p2) && !gp.getMouseWheel()){
                            gp.tryPopup(p2);
                        } else if (gp.getMouseWheel()){
                            gp.setMouseWheel(false);
                            retry = true;
                        }*/
                    } catch (InterruptedException ex1) {
                        //mouse is still moving
                        retry = true;
                    }
                }
            }
        }
    }

    
}
