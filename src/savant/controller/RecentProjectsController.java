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
import savant.view.swing.Savant;

/**
 *
 * @author mfiume
 */
public class RecentProjectsController {

    private final String FILENAME = ".recent_projects";
    private final int NUM_RECENTS_TO_SAVE = 10;

    JMenu menu;
    LinkedList<String> queue;
    File f;
    
    private static RecentProjectsController instance;

    public static RecentProjectsController getInstance() throws IOException {
        if (instance == null) {
            instance = new RecentProjectsController();
        }
        return instance;
    }

    public RecentProjectsController() throws IOException {
        f = new File(FILENAME);
        if (!f.exists()) { f.createNewFile(); }
        queue = new LinkedList<String>();
        menu = new JMenu();
        menu.setText("Project ...");
        loadRecents(f);
        updateMenuList();
    }

    public void addProjectFile(String filename) {
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
                    try {
                        ProjectController.getInstance().loadProjectFrom(s);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(Savant.getInstance(), "Error opening project from file " + s);
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
                        RecentProjectsController.getInstance().clearRecents();
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

}
