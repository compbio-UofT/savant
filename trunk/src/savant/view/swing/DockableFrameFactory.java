/*
 *    Copyright 2010-2011 University of Toronto
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

package savant.view.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.jidesoft.docking.DockContext;
import com.jidesoft.docking.DockableFrame;
import com.jidesoft.swing.JideScrollPane;
import savant.api.util.DialogUtils;

import savant.controller.DockableFrameController;
import savant.view.icon.SavantIconFactory;

/**
 * Factory for creating dockable frames.  This come in two flavours: one for tracks
 * (including genome tracks), and the other for GUI plugins.
 */
public class DockableFrameFactory {

    public static DockableFrame createFrame(String name, int mode, int side) {
        DockableFrame frame = new DockableFrame(name, null);
        frame.setSlidingAutohide(true);
        frame.getContext().setInitMode(mode);
        frame.getContext().setInitSide(side);
        frame.add(new JPanel());
        frame.setPreferredSize(new Dimension(400, 400));
        frame.setAutohideWidth(400);
        frame.setAutohideHeight(400);
        return frame;
    }

    public static DockableFrame createGUIPluginFrame(String name) {
        DockableFrame f = createFrame(name, DockContext.STATE_AUTOHIDE, DockContext.DOCK_SIDE_SOUTH);
        f.setAvailableButtons(DockableFrame.BUTTON_AUTOHIDE | DockableFrame.BUTTON_FLOATING | DockableFrame.BUTTON_MAXIMIZE );
        return f;
    }

    public static Frame createTrackFrame() {
        return createTrackFrame(true);
    }

    public static Frame createTrackFrame(boolean allowClose) {

        final Frame frame = new Frame();
        
        //frame.setInitIndex(numTracks);
        if (allowClose) {
            frame.setAvailableButtons(DockableFrame.BUTTON_AUTOHIDE | DockableFrame.BUTTON_MAXIMIZE | DockableFrame.BUTTON_CLOSE );
        } else {
            frame.setAvailableButtons(DockableFrame.BUTTON_AUTOHIDE | DockableFrame.BUTTON_MAXIMIZE );
        }
        
        frame.setInitMode(DockContext.STATE_FRAMEDOCKED);
        frame.setInitSide(DockContext.DOCK_SIDE_NORTH);
        frame.setSlidingAutohide(false);
        //frame.setShowTitleBar(false);
        //frame.setShowGripper(true);
        //frame.setPreferredAutohideSide(DockContext.DOCK_SIDE_SOUTH);
        
        frame.add(new JPanel());
        frame.setPreferredSize(new Dimension(200, 200));

        frame.setCloseAction(new Action() {
            private boolean isEnabled = true;
            private Map<String,Object> map = new HashMap<String,Object>();

            @Override
            public void actionPerformed(ActionEvent e) {
                DockableFrameController.getInstance().closeDockableFrame(frame,true);
            }

            @Override
            public Object getValue(String key) {
                if (key.equals(Action.NAME)) { return "Close"; }
                else { return map.get(key); }
            }

            @Override
            public void putValue(String key, Object value) {
                map.put(key, value);
            }

            @Override
            public void setEnabled(boolean b) {
                this.isEnabled = b;
            }

            @Override
            public boolean isEnabled() {
                return isEnabled;
            }

            @Override
            public void addPropertyChangeListener(PropertyChangeListener listener) {}

            @Override
            public void removePropertyChangeListener(PropertyChangeListener listener) {}
        });


        // TODO: this seems cyclical. What's going on here?
        JPanel panel = (JPanel)frame.getContentPane();
        panel.setLayout(new BorderLayout());
        panel.add(frame.getFrameLandscape());

        return frame;
    }

    public static JScrollPane createScrollPane(Component component) {
        JScrollPane pane = new JideScrollPane(component);
        pane.setFocusable(false);
        return pane;
    }
}
