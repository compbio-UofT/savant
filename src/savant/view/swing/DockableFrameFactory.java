package savant.view.swing;

import com.jidesoft.docking.DockContext;
import com.jidesoft.docking.DockableFrame;
import com.jidesoft.icons.JideIconsFactory;
import com.jidesoft.swing.JideScrollPane;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import savant.controller.DockableFrameController;
import savant.controller.FrameController;
import savant.view.icon.SavantIconFactory;

/**
 */
public class DockableFrameFactory {

    public static DockableFrame createFrame(String name, int mode, int side) {
        DockableFrame frame = new DockableFrame(name, null);
        frame.getContext().setInitMode(mode);
        frame.getContext().setInitSide(side);
        frame.setSlidingAutohide(false);
        frame.add(new JPanel());
        frame.setPreferredSize(new Dimension(400, 400));
        return frame;
    }

    private static int numTracks = 0;

    public static DockableFrame createGUIPluginFrame(String name) {
        DockableFrame f = createFrame(name, DockContext.MODE_DOCKABLE, DockContext.DOCK_SIDE_SOUTH);
        f.setAvailableButtons(DockableFrame.BUTTON_AUTOHIDE | DockableFrame.BUTTON_FLOATING | DockableFrame.BUTTON_MAXIMIZE );
        return f;
    }

    public static DockableFrame createGenomeFrame(String name) {
        return createTrackFrame(name, true);
    }

    public static DockableFrame createTrackFrame(String name) {
        return createTrackFrame(name, true);
    }

    public static DockableFrame createTrackFrame(String name, boolean allowClose) {

        numTracks++;
        
        final DockableFrame frame = new DockableFrame(numTracks + ". " + name, SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.TRACK));
        frame.setInitIndex(numTracks);
        if (allowClose) {
            frame.setAvailableButtons(DockableFrame.BUTTON_AUTOHIDE | DockableFrame.BUTTON_FLOATING | DockableFrame.BUTTON_MAXIMIZE | DockableFrame.BUTTON_CLOSE );
        } else {
            frame.setAvailableButtons(DockableFrame.BUTTON_AUTOHIDE | DockableFrame.BUTTON_FLOATING | DockableFrame.BUTTON_MAXIMIZE );
        }
        
        frame.getContext().setInitMode(DockContext.STATE_FRAMEDOCKED);
        frame.getContext().setInitSide(DockContext.DOCK_SIDE_NORTH);
        frame.setSlidingAutohide(false);
        frame.add(new JPanel());
        frame.setPreferredSize(new Dimension(200, 200));

        frame.setCloseAction(new Action()
        {
            private boolean isEnabled = true;
            private Map<String,Object> map = new HashMap<String,Object>();

            public void actionPerformed(ActionEvent e)
            {
                DockableFrameController.getInstance().closeDockableFrame(frame,true);
            }

            public Object getValue(String key) {
                if (key.equals(Action.NAME)) { return "Close"; }
                else { return map.get(key); }
            }

            public void putValue(String key, Object value) {
                map.put(key, value);
            }

            public void setEnabled(boolean b) {
                this.isEnabled = b;
            }

            public boolean isEnabled() {
                return isEnabled;
            }

            public void addPropertyChangeListener(PropertyChangeListener listener) {}
            public void removePropertyChangeListener(PropertyChangeListener listener) {}
        });

        return frame;
    }

    public static JScrollPane createScrollPane(Component component) {
        JScrollPane pane = new JideScrollPane(component);
        pane.setFocusable(false);
        return pane;
    }
}
