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

package savant.controller;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.adapter.DataSourceAdapter;
import savant.api.util.DialogUtils;
import savant.controller.event.TrackEvent;
import savant.settings.DirectorySettings;
import savant.util.Listener;
import savant.util.MiscUtils;
import savant.view.tracks.Track;
import savant.view.tracks.BAMCoverageTrack;

/**
 *
 * @author mfiume
 */
public class RecentTracksController {
    private static final Log LOG = LogFactory.getLog(RecentTracksController.class);

    private static RecentTracksController instance;

    private static final String RECENT_TRACKS_FILE = ".recent_tracks";
    private final int NUM_RECENTS_TO_SAVE = 10;

    JMenu menu;
    LinkedList<String> queue;
    
    private File recentTracksFile;

    public static RecentTracksController getInstance() throws IOException {
        if (instance == null) {
            instance = new RecentTracksController();
        }
        return instance;
    }

    private RecentTracksController() throws IOException {
        TrackController.getInstance().addListener(new Listener<TrackEvent>() {

            @Override
            public void handleEvent(TrackEvent event) {
                if (event.getType() == TrackEvent.Type.ADDED) {
                    Track t = event.getTrack();

                    if (t instanceof BAMCoverageTrack) { return; }

                    DataSourceAdapter ds = t.getDataSource();

                    if (ds == null || ds.getURI() == null) {
                        return;
                    }

                    String path = MiscUtils.getNeatPathFromURI(ds.getURI());

                    queue.remove(path);
                    resizeQueue(queue, NUM_RECENTS_TO_SAVE);
                    queue.add(0,path);
                    updateMenuList();

                    try { saveRecents(queue); } catch (IOException ex) {
                        LOG.error("Could not save recents to file", ex);
                    }
                }
            }
        });

        recentTracksFile = new File(DirectorySettings.getSavantDirectory(), RECENT_TRACKS_FILE);
        if (!recentTracksFile.exists()) { recentTracksFile.createNewFile(); }
        queue = new LinkedList<String>();
        loadRecents(recentTracksFile);
    }

    public List<String> getRecentTracks() {
        return queue;
    }

    private void resizeQueue(LinkedList queue, int size) {
        while (queue.size() > size) {
            queue.removeLast();
        }
    }

    public void populateMenu(JMenu m) {
        menu = m;
        updateMenuList();
    }

    private void updateMenuList() {
        menu.removeAll();
        for (final String s : queue) {
            JMenuItem item = new JMenuItem();
            item.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        FrameController.getInstance().addTrackFromPath(s, false);
                    } catch (Exception ex) {
                        LOG.error("Unable to open file.", ex);
                        DialogUtils.displayError("Problem opening track from file " + s);
                    }
                }
            });
            item.setText(s);
            menu.add(item);
        }

        menu.add(new JSeparator());

        JMenuItem item = new JMenuItem();
            item.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        RecentTracksController.getInstance().clearRecents();
                    } catch (Exception ex) {
                    }
                }
            });
            item.setText("Clear Recents");
            menu.add(item);
    }

    private void clearRecents() {
        while (!queue.isEmpty()) {
            queue.remove(0);
        }
        try { saveRecents(queue); } catch (IOException ex) {}
        updateMenuList();
    }

    private void saveRecents(LinkedList<String> queue) throws IOException {
        recentTracksFile.delete();
        recentTracksFile.createNewFile();
        BufferedWriter w = new BufferedWriter(new FileWriter(recentTracksFile));
        for (String s : queue) {
            w.write(s + "\n");
        }
        w.close();
    }

    private void loadRecents(File f) throws IOException {
        BufferedReader r = new BufferedReader(new FileReader(f));
        String line = "";
        while ((line = r.readLine()) != null) {
            queue.add(line);
        }
        r.close();
    }
}
