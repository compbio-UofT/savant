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
import javax.swing.JOptionPane;
import javax.swing.JSeparator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import savant.controller.event.viewtrack.ViewTrackAddedListener;
import savant.controller.event.viewtrack.ViewTrackAddedOrRemovedEvent;
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

    private final String FILENAME = ".recent_tracks";
    private final int NUM_RECENTS_TO_SAVE = 10;

    JMenu menu;
    LinkedList<String> queue;
    
    private File f;

    public RecentTracksController() throws IOException {
        ViewTrackController.getInstance().addTracksAddedListener(this);
        f = new File(FILENAME);
        if (!f.exists()) { f.createNewFile(); }
        queue = new LinkedList<String>();
        loadRecents(f);
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

        String path = t.getURI().toASCIIString();
        if (path == null) { return; }

        if(t.getURI().getScheme().equals("file")){
            path = MiscUtils.getNeatPathFromURI(t.getURI());
        }

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
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(Savant.getInstance(), "Error opening track from file " + s);
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
        f.delete();
        f.createNewFile();
        BufferedWriter w = new BufferedWriter(new FileWriter(f));
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
