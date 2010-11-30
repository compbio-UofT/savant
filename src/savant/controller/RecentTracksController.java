/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.util.DialogUtils;
import savant.controller.event.ViewTrackAddedListener;
import savant.controller.event.ViewTrackAddedOrRemovedEvent;
import savant.settings.DirectorySettings;
import savant.util.MiscUtils;
import savant.view.swing.Savant;
import savant.view.swing.ViewTrack;

/**
 *
 * @author mfiume
 */
public class RecentTracksController implements ViewTrackAddedListener {
    private static final Log LOG = LogFactory.getLog(RecentTracksController.class);

    private static RecentTracksController instance;

    private static final String RECENT_TRACKS_FILE = ".recent_tracks";
    private final int NUM_RECENTS_TO_SAVE = 10;

    JMenu menu;
    LinkedList<String> queue;
    
    private File recentTracksFile;

    public RecentTracksController() throws IOException {
        ViewTrackController.getInstance().addTracksAddedListener(this);
        recentTracksFile = new File(DirectorySettings.getSavantDirectory(), RECENT_TRACKS_FILE);
        if (!recentTracksFile.exists()) { recentTracksFile.createNewFile(); }
        queue = new LinkedList<String>();
        loadRecents(recentTracksFile);
    }

    public static RecentTracksController getInstance() throws IOException {
        if (instance == null) {
            instance = new RecentTracksController();
        }
        return instance;
    }

    @Override
    public void viewTrackAddedEventReceived(ViewTrackAddedOrRemovedEvent event) {

        ViewTrack t = event.getTrack();

        if (t.getDataSource() == null) { return; }

        if (t.getURI() == null) { return; }

        String path = t.getURI().toASCIIString();
        if (path == null) { return; }

        path = MiscUtils.getNeatPathFromURI(t.getURI());
        
        queue.remove(path);
        resizeQueue(queue, NUM_RECENTS_TO_SAVE);
        queue.add(0,path);
        updateMenuList();
        
        try { saveRecents(queue); } catch (IOException ex) {
            LOG.error("Could not save recents to file", ex);
        }
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
                        Savant.getInstance().addTrackFromFile(s);
                    } catch (Exception ex) {
                        LOG.error("Unable to open file.", ex);
                        DialogUtils.displayError("Error opening track from file " + s);
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
