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
import savant.view.swing.Savant;
import savant.view.swing.Track;

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

    public List<Track> getSelectedTracks() {
        StringTokenizer st = new StringTokenizer(this.f.getText(), ",");
        List<Track> tracks = new ArrayList<Track>();

        while (st.hasMoreElements()) {
            String tname = st.nextToken();
            Track vt = TrackController.getInstance().getTrack(tname);
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
