package savant.view.swing;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import savant.controller.BookmarkController;
import savant.controller.ProjectController;
import savant.controller.RangeController;
import savant.controller.ViewTrackController;
import savant.controller.event.bookmark.BookmarksChangedEvent;
import savant.controller.event.bookmark.BookmarksChangedListener;
import savant.controller.event.range.RangeChangedEvent;
import savant.controller.event.range.RangeChangedListener;
import savant.controller.event.viewtrack.ViewTrackListChangedEvent;
import savant.controller.event.viewtrack.ViewTrackListChangedListener;
import savant.exception.SavantEmptySessionException;
import savant.file.SavantFileNotFormattedException;
import savant.file.SavantUnsupportedVersionException;
import savant.settings.DirectorySettings;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author mfiume
 */
public class ProjectHandler implements
        BookmarksChangedListener,
        RangeChangedListener,
        ViewTrackListChangedListener {

    private static ProjectHandler instance;
    private boolean isOpenProjectSaved = true;
    private String openProjectPath = null;
    private String untitledProjectPath = DirectorySettings.getProjectsDirectory() + System.getProperty("file.separator") + "UntitledProject.svp";
    private final String UNSAVEDINDICATOR = "*";
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
        ViewTrackController.getInstance().addTracksChangedListener(this);
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
        } else if (!isOpenProjectSaved && !isLoading) {
            Savant.getInstance().setTitle(base + path + " " + UNSAVEDINDICATOR);
        } else {
            Savant.getInstance().setTitle(base + path);
        }
    }

    @Override
    public void rangeChangeReceived(RangeChangedEvent event) {
        setProjectSaved(false);
    }

    @Override
    public void viewTrackListChangeReceived(ViewTrackListChangedEvent event) {
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
            try {
                String filename = jfc.getSelectedFile().getAbsolutePath();
                loadProjectFrom(filename);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(Savant.getInstance(), "Error loading project.");
            }
        }
    }

    public void promptUserToSaveProjectAs() {

        if (!ProjectController.getInstance().isProjectOpen()) {
            JOptionPane.showMessageDialog(Savant.getInstance(), "No project to save.");
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
                ex.printStackTrace();
                if (JOptionPane.showConfirmDialog(Savant.getInstance(),
                        "Error saving project to "
                        + getCurrentPath()
                        + ". Try another location?") == JOptionPane.YES_OPTION) {
                    promptUserToSaveProjectAs();
                }
                Logger.getLogger(ProjectHandler.class.getName()).log(Level.SEVERE, null, ex);
            } catch (SavantEmptySessionException ex) {
                Logger.getLogger(ProjectHandler.class.getName()).log(Level.SEVERE, null, ex);
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

    public void loadProjectFrom(String filename) throws IOException, URISyntaxException, SavantFileNotFormattedException, SavantUnsupportedVersionException {
        isLoading = true;
        openProjectPath = filename;
        ProjectController.getInstance().loadProjectFrom(filename);
        isLoading = false;
        this.setProjectSaved(true);
    }
}
