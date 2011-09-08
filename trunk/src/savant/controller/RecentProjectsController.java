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

import savant.api.util.DialogUtils;
import savant.controller.event.ProjectEvent;
import savant.settings.DirectorySettings;
import savant.util.Listener;


/**
 *
 * @author mfiume
 */
public class RecentProjectsController implements Listener<ProjectEvent> {

    private final String RECENT_PROJECTS_FILE = ".recent_projects";
    private final int NUM_RECENTS_TO_SAVE = 10;

    JMenu menu;
    LinkedList<String> queue;
    File recentProjectsFile;
    
    private static RecentProjectsController instance;

    public static RecentProjectsController getInstance() throws IOException {
        if (instance == null) {
            instance = new RecentProjectsController();
            ProjectController.getInstance().addListener(instance);
        }
        return instance;
    }

    private RecentProjectsController() throws IOException {
        recentProjectsFile = new File(DirectorySettings.getSavantDirectory(), RECENT_PROJECTS_FILE);
        if (!recentProjectsFile.exists()) { recentProjectsFile.createNewFile(); }
        queue = new LinkedList<String>();
        loadRecents(recentProjectsFile);
    }

    private void addProjectFile(File filename) {
        queue.remove(filename.getAbsolutePath());
        resizeQueue(queue, NUM_RECENTS_TO_SAVE);
        queue.add(0,filename.getAbsolutePath());
        try { saveRecents(queue); } catch (IOException ex) {}
        updateMenuList();
    }

    private void saveRecents(LinkedList<String> queue) throws IOException {
        recentProjectsFile.delete();
        recentProjectsFile.createNewFile();
        BufferedWriter w = new BufferedWriter(new FileWriter(recentProjectsFile));
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
                        if (ProjectController.getInstance().promptToSaveChanges(false)) {
                            ProjectController.getInstance().loadProjectFromFile(new File(s));
                        }
                    } catch (Exception ex) {
                        DialogUtils.displayException("Project Error", "Error opening project from file " + s, ex);
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


    public List<String> getRecentProjects() {
        return this.queue;
    }

    private void clearRecents() {
        while (!queue.isEmpty()) {
            queue.remove(0);
        }
        try { saveRecents(queue); } catch (IOException ex) {}
        updateMenuList();
    }

    /**
     * We listen for projects being loaded or saved, so that we can update our list.
     */
    @Override
    public void handleEvent(ProjectEvent event) {
        switch (event.getType()) {
            case LOADED:
            case SAVED:
                File f = new File(event.getPath());
                if (f.exists()) {
                    addProjectFile(f);
                }
                break;
        }
    }

}
