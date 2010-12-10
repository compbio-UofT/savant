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

package savant.view.swing;

import java.io.File;
import java.io.IOException;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.util.DialogUtils;
import savant.controller.BookmarkController;
import savant.controller.ProjectController;
import savant.controller.RangeController;
import savant.controller.TrackController;
import savant.controller.event.BookmarksChangedEvent;
import savant.controller.event.BookmarksChangedListener;
import savant.controller.event.RangeChangedEvent;
import savant.controller.event.RangeChangedListener;
import savant.controller.event.TrackListChangedEvent;
import savant.controller.event.TrackListChangedListener;
import savant.exception.SavantEmptySessionException;
import savant.settings.DirectorySettings;
import savant.util.MiscUtils;

/**
 *
 * @author mfiume
 */
public class ProjectHandler implements
        BookmarksChangedListener,
        RangeChangedListener,
        TrackListChangedListener {

    private static final Log LOG = LogFactory.getLog(ProjectHandler.class);

    private static ProjectHandler instance;
    private boolean isOpenProjectSaved = true;
    private String openProjectPath = null;
    private String untitledProjectPath = DirectorySettings.getProjectsDirectory() + System.getProperty("file.separator") + "UntitledProject.svp";
    private boolean isLoading = false;

    public ProjectHandler() {
        addListeners();
    }

    public static ProjectHandler getInstance() {
        if (instance == null) {
            instance = new ProjectHandler();
        }
        return instance;
    }

    private void addListeners() {
        BookmarkController.getInstance().addFavoritesChangedListener(this);
        RangeController.getInstance().addRangeChangedListener(this);
        TrackController.getInstance().addTrackListChangedListener(this);
    }

    public boolean isProjectSaved() {
        return isOpenProjectSaved;
    }

    private void setProjectSaved(boolean arg) {
        if (isOpenProjectSaved != arg) {
            isOpenProjectSaved = arg;
            setProjectInfo();
        }
    }

    public void setProjectInfo() {
        String path = getCurrentPath();
        String base = "Savant Genome Browser - ";
        if (isLoading) {
            Savant.getInstance().setTitle(base + "Opening " + path + " ...");
        } else {
            MiscUtils.setUnsavedTitle(Savant.getInstance(), base + path, !isOpenProjectSaved);
        }
    }

    @Override
    public void rangeChangeReceived(RangeChangedEvent event) {
        setProjectSaved(false);
    }

    @Override
    public void trackListChangeReceived(TrackListChangedEvent event) {
        setProjectSaved(false);
    }

    @Override
    public void bookmarksChangeReceived(BookmarksChangedEvent event) {
        setProjectSaved(false);
    }

    public void promptUserToLoadProject() {

        if (ProjectController.getInstance().isProjectOpen()) {
            if (!isOpenProjectSaved) {
                int result = JOptionPane.showConfirmDialog(Savant.getInstance(), "Save current session?");
                if (result == JOptionPane.YES_OPTION) {
                    promptUserToSaveSession();
                } else if (result == JOptionPane.CANCEL_OPTION) {
                    return;
                }
            }
        }

        JFileChooser jfc = new JFileChooser();
        jfc.setCurrentDirectory(new File(DirectorySettings.getProjectsDirectory()));
        int result = jfc.showOpenDialog(Savant.getInstance());
        if (result == JFileChooser.APPROVE_OPTION) {
            String filename = jfc.getSelectedFile().getAbsolutePath();
            loadProjectFrom(filename);
        }
    }

    public void promptUserToSaveProjectAs() {

        if (!ProjectController.getInstance().isProjectOpen()) {
            DialogUtils.displayMessage("No project to save.");
            return;
        }

        JFileChooser jfc = new JFileChooser();
        jfc.setCurrentDirectory(new File(DirectorySettings.getSavantDirectory()));
        jfc.setDialogTitle("Save Project");
        jfc.setDialogType(JFileChooser.SAVE_DIALOG);

        jfc.setSelectedFile(new File(this.getCurrentPath()));

        if (jfc.showSaveDialog(Savant.getInstance()) == JFileChooser.APPROVE_OPTION) {
            try {
                File selectedFile = jfc.getSelectedFile();
                int result = -1;
                if (selectedFile.exists()) {
                    result = JOptionPane.showConfirmDialog(Savant.getInstance(), "Overwrite existing file?");
                }
                if (result == -1 || result == JOptionPane.OK_OPTION) {
                    ProjectController.getInstance().saveProjectAs(selectedFile);
                    openProjectPath = selectedFile.getAbsolutePath();
                    setProjectSaved(true);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                if (JOptionPane.showConfirmDialog(Savant.getInstance(),
                        "Error saving project to "
                        + jfc.getSelectedFile().getAbsolutePath()
                        + ". Try another location?") == JOptionPane.YES_OPTION) {
                    promptUserToSaveProjectAs();
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(Savant.getInstance(), "Error saving project.");
                e.printStackTrace();
            }
        }
    }

    void promptUserToSaveSession() {
        if (this.openProjectPath == null) {
            this.promptUserToSaveProjectAs();
        } else {
            try {
                ProjectController.getInstance().saveProjectAs(getCurrentPath());
                openProjectPath = getCurrentPath();
                setProjectSaved(true);
            } catch (IOException ex) {
                LOG.error(String.format("Unable to save %s.", openProjectPath), ex);
                if (DialogUtils.askYesNo("Error saving project to " + getCurrentPath() + ". Try another location?") == DialogUtils.YES) {
                    promptUserToSaveProjectAs();
                }
            } catch (SavantEmptySessionException ex) {
                LOG.error(String.format("Unable to save %s.", openProjectPath), ex);
            }
        }
    }

    private String getCurrentPath() {
        if (this.openProjectPath == null) {
            return this.untitledProjectPath;
        } else {
            return this.openProjectPath;
        }
    }

    public void loadProjectFrom(String filename) {
        isLoading = true;
        openProjectPath = filename;
        try {
            ProjectController.getInstance().loadProjectFrom(filename);
            isLoading = false;
            setProjectSaved(true);
        } catch (Exception x) {
            DialogUtils.displayError("Error Loading Project", x.getMessage());
        }
    }
}
