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

/**
 */
public class DockableFrameFactory {

    public static DockableFrame createFrame(String name, int mode, int side) {
        DockableFrame frame = new DockableFrame(name, JideIconsFactory.getImageIcon(JideIconsFactory.FileType.TEXT));
        frame.getContext().setInitMode(mode);
        frame.getContext().setInitSide(side);
        frame.add(new JPanel());
        frame.setPreferredSize(new Dimension(200, 200));
        return frame;
    }

    private static int numTracks = 0;

    public static DockableFrame createGenomeFrame(String name) {
        return createTrackFrame(name, true);
    }

    public static DockableFrame createTrackFrame(String name) {
        return createTrackFrame(name, true);
    }

    public static DockableFrame createTrackFrame(String name, boolean allowClose) {

        numTracks++;
        
        final DockableFrame frame = new DockableFrame(numTracks + ". " + name, JideIconsFactory.getImageIcon(JideIconsFactory.WindowMenu.NEW_HORIZONTAL_TAB));
        frame.setInitIndex(numTracks);
        if (allowClose) {
            frame.setAvailableButtons(DockableFrame.BUTTON_AUTOHIDE | DockableFrame.BUTTON_FLOATING | DockableFrame.BUTTON_MAXIMIZE | DockableFrame.BUTTON_CLOSE );
        } else {
            frame.setAvailableButtons(DockableFrame.BUTTON_AUTOHIDE | DockableFrame.BUTTON_FLOATING | DockableFrame.BUTTON_MAXIMIZE );
        }
        
        frame.getContext().setInitMode(DockContext.STATE_FRAMEDOCKED);
        frame.getContext().setInitSide(DockContext.DOCK_SIDE_NORTH);
        frame.add(new JPanel());
        frame.setPreferredSize(new Dimension(200, 200));

        frame.setCloseAction(new Action()
        {
            private boolean isEnabled = true;
            private Map<String,Object> map = new HashMap<String,Object>();

            public void actionPerformed(ActionEvent e)
            {
                int response = JOptionPane.showConfirmDialog(null, "Are you sure you want to close this track?", "Confirm",
                    JOptionPane.YES_NO_OPTION, JOptionPane.OK_CANCEL_OPTION);
                if (response == JOptionPane.YES_OPTION) {
                  System.out.println("Removing " + frame.getName());
                  frame.getDockingManager().removeFrame(frame.getName());
                }
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
