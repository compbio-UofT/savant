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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.StringTokenizer;
import javax.swing.filechooser.FileFilter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.util.DialogUtils;
import savant.controller.event.BookmarksChangedEvent;
import savant.controller.event.BookmarksChangedListener;
import savant.controller.event.ProjectEvent;
import savant.controller.event.LocationChangedEvent;
import savant.controller.event.LocationChangedListener;
import savant.controller.event.TrackEvent;
import savant.data.types.Genome;
import savant.file.Project;
import savant.settings.DirectorySettings;
import savant.util.Controller;
import savant.util.FileExtensionFilter;
import savant.util.Listener;


/**
 *
 * @author mfiume, tarkvara
 */
public class ProjectController extends Controller {
    private static final Log LOG = LogFactory.getLog(ProjectController.class);

    private static final FileFilter PROJECT_FILTER = new FileExtensionFilter("Savant project files", "svp");
    private static final File UNTITLED_PROJECT_FILE = new File(DirectorySettings.getProjectsDirectory(), "UntitledProject.svp");

    private static ProjectController instance;

    private boolean projectSaved = true;
    private File currentProjectFile = null;

    public static ProjectController getInstance() {
        if (instance == null) {
            instance = new ProjectController();
        }
        return instance;
    }

    private ProjectController() {
        // Set up listeners so that when the project's state changes, we know that we have something to save.
        BookmarkController.getInstance().addBookmarksChangedListener(new BookmarksChangedListener() {
            @Override
            public void bookmarksChanged(BookmarksChangedEvent event) {
                setProjectSaved(false);
            }
        });
        LocationController.getInstance().addLocationChangedListener(new LocationChangedListener() {
            @Override
            public void locationChanged(LocationChangedEvent event) {
                setProjectSaved(false);
            }
        });
        TrackController.getInstance().addListener(new Listener<TrackEvent>() {
            @Override
            public void handleEvent(TrackEvent event) {
                if (event.getType() == TrackEvent.Type.ADDED || event.getType() == TrackEvent.Type.REMOVED) {
                    setProjectSaved(false);
                }
            }
        });
    }

    public void clearExistingProject() {
        TrackController.getInstance().closeTracks();
        BookmarkController.getInstance().clearBookmarks();
    }

    public boolean saveProjectAs(File f) throws Exception {
        try {
            fireEvent(new ProjectEvent(ProjectEvent.Type.SAVING, f));
            Project.saveToFile(f);
            currentProjectFile = f;
            setProjectSaved(true);
            return true;
        } catch (IOException ex) {
            LOG.error(ex);
            if (DialogUtils.askYesNo("<html>Error saving project to <i>" + f.getAbsolutePath() + "</i>. Try another location?</html>") == DialogUtils.YES) {
                return promptToSaveProjectAs();
            }
        }
        return false;
    }

    public static boolean isProjectOpen() {
        return GenomeController.getInstance().isGenomeLoaded();
    }

    public boolean isProjectSaved() {
        return projectSaved;
    }

    private void setProjectSaved(boolean saved) {
        if (projectSaved != saved) {
            projectSaved = saved;
            fireEvent(new ProjectEvent(saved ? ProjectEvent.Type.SAVED : ProjectEvent.Type.UNSAVED, getCurrentFile()));
        }
    }

    public void promptToLoadProject() throws Exception {
        if (promptToSaveChanges(false)) {
            File f = DialogUtils.chooseFileForOpen("Open Project File", PROJECT_FILTER, DirectorySettings.getProjectsDirectory());
            if (f != null) {
                loadProjectFromFile(f);
            }
        }
    }

    public boolean promptToSaveChanges(boolean quitting) throws Exception {
        if (isProjectOpen()) {
            if (!projectSaved) {
                int result = DialogUtils.askYesNoCancel(quitting ? "Save changes to current project before quitting?" : "Save changes to current project?");
                if (result == DialogUtils.YES) {
                    promptToSaveProject();
                } else if (result == DialogUtils.CANCEL) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Prompt the user to save the current project.
     *
     * @return true if the project was saved
     */
    public boolean promptToSaveProject() throws Exception {
        if (currentProjectFile == null) {
            return promptToSaveProjectAs();
        }
        return saveProjectAs(getCurrentFile());
    }


    public boolean promptToSaveProjectAs() throws Exception {

        if (!isProjectOpen()) {
            DialogUtils.displayMessage("No project to save.");
            return false;
        }

        File selectedFile = DialogUtils.chooseFileForSave("Save Project", getCurrentFile().getName(), PROJECT_FILTER, DirectorySettings.getProjectsDirectory());

        if (selectedFile != null) {
            return saveProjectAs(selectedFile);
        }
        return false;
    }

    private File getCurrentFile() {
        return currentProjectFile != null ? currentProjectFile : UNTITLED_PROJECT_FILE;
    }

    public void loadProjectFromFile(File f) throws Exception {
        fireEvent(new ProjectEvent(ProjectEvent.Type.LOADING, f));
        currentProjectFile = f;
        clearExistingProject();
        Project proj = new Project(f);
        proj.load();
        fireEvent(new ProjectEvent(ProjectEvent.Type.LOADED, f));
        setProjectSaved(true);
    }

    public void loadProjectFromURL(String urlString) throws Exception {

        URL url  = new URL(urlString);
        InputStream is = url.openStream();
        FileOutputStream fos=null;

        String localName = null;
        StringTokenizer st = new StringTokenizer(url.getFile(), "/");
        while (st.hasMoreTokens()){
            localName = st.nextToken();
        }
        //TODO: where should we store this? currently in tmp dir
        File localFile = new File(DirectorySettings.getTmpDirectory(), localName);
        fos = new FileOutputStream(localFile);

        int oneChar, count=0;
        while ((oneChar=is.read()) != -1) {
            fos.write(oneChar);
            count++;
        }
        is.close();
        fos.close();
        loadProjectFromFile(localFile);
    }

    public void setProjectFromGenome(Genome genome, URI[] trackURIs) throws Exception {
        if (promptToSaveChanges(false)) {
            projectSaved = false;
            currentProjectFile = null;
            fireEvent(new ProjectEvent(ProjectEvent.Type.LOADING, getCurrentFile()));

            clearExistingProject();
            Project proj = new Project(genome, trackURIs);
            proj.load();
            setProjectSaved(true);
        }
    }
}
