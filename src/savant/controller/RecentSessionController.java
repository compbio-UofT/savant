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

/**
 *
 * @author mfiume
 */
public class RecentSessionController {

    private final String FILENAME = ".recent_tracks";
    private final int NUM_RECENTS_TO_SAVE = 10;

    JMenu menu;
    LinkedList<String> queue;
    File f;
    
    private static RecentSessionController instance;

    public static RecentSessionController getInstance() throws IOException {
        if (instance == null) {
            instance = new RecentSessionController();
        }
        return instance;
    }

    public RecentSessionController() throws IOException {
        f = new File(FILENAME);
        if (!f.exists()) { f.createNewFile(); }
        queue = new LinkedList<String>();
        menu = new JMenu();
        menu.setText("Session ...");
        loadRecents(f);
        updateMenuList();
    }

    public void addSessionFile(String filename) {
        queue.remove(filename);
        resizeQueue(queue, NUM_RECENTS_TO_SAVE);
        queue.add(0,filename);
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

    private void resizeQueue(LinkedList queue, int size) {
        while (queue.size() > size) {
            queue.removeLast();
        }
    }

     private void loadRecents(File f) throws IOException {
        BufferedReader r = new BufferedReader(new FileReader(f));
        String line = "";
        while ((line = r.readLine()) != null) {
            queue.add(line);
        }
        r.close();
    }

    public JMenu getMenu() {
        return menu;
    }

     private void updateMenuList() {
        menu.removeAll();
        for (final String s : queue) {
            JMenuItem item = new JMenuItem();
            item.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    SessionController.getInstance().loadSession(s, true);
                }
            });
            item.setText(s);
            menu.add(item);
        }
    }

}
