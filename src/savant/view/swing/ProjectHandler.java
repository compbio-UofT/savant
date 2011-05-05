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

package savant.view.swing;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.filechooser.FileFilter;

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
    private static final FileFilter PROJECT_FILTER = new FileFilter() {
        @Override
        public boolean accept(File f) {
            if (f.isDirectory()) {
                return true;
            }

            String extension = MiscUtils.getExtension(f.getAbsolutePath());
            if (extension != null) {
                return extension.equalsIgnoreCase("svp");
            }
            return false;
        }

        @Override
        public String getDescription() {
            return "Savant project files (*.svp)";
        }
    };
    private static final File UNTITLED_PROJECT_PATH = new File(DirectorySettings.getProjectsDirectory(), "UntitledProject.svp");


    private static ProjectHandler instance;
    private boolean isOpenProjectSaved = true;
    private File openProjectPath = null;
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
        File path = getCurrentPath();
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
                int result = DialogUtils.askYesNoCancel("Save current session?");
                if (result == DialogUtils.YES) {
                    promptUserToSaveSession();
                } else if (result == DialogUtils.CANCEL) {
                    return;
                }
            }
        }

        File f = DialogUtils.chooseFileForOpen("Open Project File", PROJECT_FILTER, DirectorySettings.getProjectsDirectory());
        if (f != null) {
            loadProjectFrom(f);
        }
    }

    public boolean promptUserToSaveProjectAs() {

        if (!ProjectController.getInstance().isProjectOpen()) {
            DialogUtils.displayMessage("No project to save.");
            return false;
        }

        File selectedFile = DialogUtils.chooseFileForSave("Save Project", getCurrentPath().getName(), PROJECT_FILTER, DirectorySettings.getProjectsDirectory());

        if (selectedFile != null) {
            try {
                int result = DialogUtils.YES;
                if (selectedFile.exists()) {
                    result = DialogUtils.askYesNo("Overwrite existing file?");
                }
                if (result == DialogUtils.YES) {
                    ProjectController.getInstance().saveProjectAs(selectedFile);
                    openProjectPath = selectedFile;
                    setProjectSaved(true);
                    return true;
                }
            } catch (IOException ex) {
                LOG.error(ex);
                if (DialogUtils.askYesNo("Error saving project to " + selectedFile.getAbsolutePath() + ". Try another location?") == DialogUtils.YES) {
                    return promptUserToSaveProjectAs();
                }
            } catch (Exception ex) {
                LOG.error(ex);
                DialogUtils.displayError("Error saving project.");
            }
        }
        return false;
    }

    /**
     * Prompt the user to save the current project.
     *
     * @return true if the project was saved
     */
    boolean promptUserToSaveSession() {
        if (openProjectPath == null) {
            return promptUserToSaveProjectAs();
        }
        try {
            ProjectController.getInstance().saveProjectAs(getCurrentPath());
            openProjectPath = getCurrentPath();
            setProjectSaved(true);
            return true;
        } catch (IOException ex) {
            LOG.error(String.format("Unable to save %s.", openProjectPath), ex);
            if (DialogUtils.askYesNo("Error saving project to " + getCurrentPath() + ". Try another location?") == DialogUtils.YES) {
                return promptUserToSaveProjectAs();
            }
        } catch (SavantEmptySessionException ex) {
            LOG.error(String.format("Unable to save %s.", openProjectPath), ex);
        }
        return false;
    }

    private File getCurrentPath() {
        return openProjectPath != null ? openProjectPath : UNTITLED_PROJECT_PATH;
    }

    public void loadProjectFrom(File f) {
        isLoading = true;
        openProjectPath = f;
        try {
            ProjectController.getInstance().loadProjectFrom(f);
            isLoading = false;
            setProjectSaved(true);
        } catch (Exception x) {
            LOG.error("Error loading project: " + f, x);
            DialogUtils.displayException("Error Loading Project", String.format("Unable to load %s.", f), x);
        }
    }

    public void loadProjectFromUrl(String urlString){

        String localFile = null;
        boolean success = false;
        try{
            URL url  = new URL(urlString);
            InputStream is = url.openStream();
            FileOutputStream fos=null;

            StringTokenizer st=new StringTokenizer(url.getFile(), "/");
            while (st.hasMoreTokens()){
                localFile=st.nextToken();
            }
            //TODO: where should we store this? currently in tmp dir
            localFile = DirectorySettings.getTmpDirectory() + System.getProperty("file.separator") + localFile;
            fos = new FileOutputStream(localFile);

            int oneChar, count=0;
            while ((oneChar=is.read()) != -1)
            {
                fos.write(oneChar);
                count++;
            }
            is.close();
            fos.close();
            success = true;
            
        }catch (MalformedURLException e){
            System.err.println(e.toString());
        }catch (IOException e){
            System.err.println(e.toString());
        }

        if(success){
            File f = new File(localFile);
            loadProjectFrom(f);
        }
    }
}
