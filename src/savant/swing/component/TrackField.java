/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package savant.swing.component;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import savant.controller.TrackController;
import savant.controller.ViewTrackController;
import savant.view.icon.SavantIconFactory;
import savant.view.swing.Savant;
import savant.view.swing.ViewTrack;
import savant.view.swing.util.DocumentViewer;

/**
 *
 * @author mfiume
 */
public class TrackField extends JPanel {

    JTextField f;
    JButton b;
    TrackChooser tc;

    public TrackField(boolean multiSelectAllowed) {
        f = new JTextField();
        b = new JButton("...");
        tc = new TrackChooser(Savant.getInstance(), multiSelectAllowed, (multiSelectAllowed) ? "Select track(s)" : "Select track");
        f.setMaximumSize(new Dimension(9999,22));
        f.setToolTipText((multiSelectAllowed) ? "Selected track(s)" : "Selected track");
        this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        this.add(f);
        this.add(b);

        b.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                tc.setVisible(true);
                String[] selectedTracks = tc.getSelectedTracks();
                if (selectedTracks != null) {
                   setSelectedTracks(selectedTracks);
                }
            }
        });



    }

    public List<ViewTrack> getSelectedTracks() {
        StringTokenizer st = new StringTokenizer(this.f.getText(), ",");
        List<ViewTrack> tracks = new ArrayList<ViewTrack>();

        while (st.hasMoreElements()) {
            String tname = st.nextToken();
            ViewTrack vt = ViewTrackController.getInstance().getTrack(tname);
            tracks.add(vt);
        }

        return tracks;
    }

    public void setSelectedTracks(String[] trackNames) {

        String s = "";
        for (String trackName : trackNames) {
            s += trackName + ", ";
        }
        if (s.endsWith(", ")) {
            s = s.substring(0,s.length()-2);
        }
        this.f.setText(s.trim());
    }

    public TrackChooser getFileChooser() {
        return this.tc;
    }
}
