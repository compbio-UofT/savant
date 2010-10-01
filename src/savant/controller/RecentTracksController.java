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
import savant.controller.event.viewtrack.ViewTrackListChangedEvent;
import savant.controller.event.viewtrack.ViewTrackListChangedListener;
import savant.view.swing.Savant;
import savant.view.swing.ViewTrack;

/**
 *
 * @author mfiume
 */
public class RecentTracksController implements ViewTrackListChangedListener {

    private static RecentTracksController instance;

    private final String FILENAME = ".recent_tracks";
    private final int NUM_RECENTS_TO_SAVE = 10;

    JMenu menu;
    LinkedList<String> queue;
    
    private File f;

    public RecentTracksController() throws IOException {
        ViewTrackController.getInstance().addTracksChangedListener(this);
        f = new File(FILENAME);
        if (!f.exists()) { f.createNewFile(); }
        queue = new LinkedList<String>();
        menu = new JMenu();
        menu.setText("Track ...");
        loadRecents(f);
        updateMenuList();
    }

    public static RecentTracksController getInstance() throws IOException {
        if (instance == null) {
            instance = new RecentTracksController();
        }
        return instance;
    }

    public void viewTrackListChangeReceived(ViewTrackListChangedEvent event) {
        ViewTrack t = event.getTracks().get(event.getTracks().size()-1);
        String path = t.getURI().getPath();

        if (path == null) { return; }

        queue.remove(path);
        resizeQueue(queue, NUM_RECENTS_TO_SAVE);
        queue.add(0,path);
        updateMenuList();
        
        try { saveRecents(queue); } catch (IOException ex) {
            System.err.println("Could not save recents to file");
            ex.printStackTrace();
        }
    }

    private void resizeQueue(LinkedList queue, int size) {
        while (queue.size() > size) {
            queue.removeLast();
        }
    }

    public JMenu getMenu() {
        return menu;
    }

    private void updateMenuList() {
        menu.removeAll();
        for (final String s : queue) {
            JMenuItem item = new JMenuItem();
            item.addActionListener(new ActionListener() {

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
