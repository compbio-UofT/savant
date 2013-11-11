/**
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package savant.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.StringTokenizer;
import javax.swing.filechooser.FileFilter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.event.BookmarksChangedEvent;
import savant.api.event.LocationChangedEvent;
import savant.api.event.TrackEvent;
import savant.api.util.DialogUtils;
import savant.api.util.Listener;
import savant.controller.event.ProjectEvent;
import savant.data.types.Genome;
import savant.file.Project;
import savant.settings.DirectorySettings;
import savant.util.Controller;
import savant.util.FileExtensionFilter;
import savant.util.NetworkUtils;
import savant.util.SavantHTTPAuthenticator;
import savant.view.swing.Savant;


/**
 *
 * @author mfiume, tarkvara
 */
public class ProjectController extends Controller {
    private static final Log LOG = LogFactory.getLog(ProjectController.class);

    private static final FileFilter PROJECT_FILTER = new FileExtensionFilter("Savant project files", "svp");
    private static final File UNTITLED_PROJECT_FILE = new File("Untitled Project.svp");

    private static ProjectController instance;

    private boolean projectSaved = true;
    private File currentProjectFile = null;
    private List<String> pendingTracks = null;

    public static ProjectController getInstance() {
        if (instance == null) {            
            instance = new ProjectController();
        }
        return instance;
    }

    private ProjectController() {
        // Set up listeners so that when the project's state changes, we know that we have something to save.
        BookmarkController.getInstance().addListener(new Listener<BookmarksChangedEvent>() {
            @Override
            public void handleEvent(BookmarksChangedEvent event) {
                setProjectSaved(false);
            }
        });
        LocationController.getInstance().addListener(new Listener<LocationChangedEvent>() {
            @Override
            public void handleEvent(LocationChangedEvent event) {
                if (pendingTracks == null) {
                    setProjectSaved(false);
                }
            }
        });
        TrackController.getInstance().addListener(new Listener<TrackEvent>() {
            @Override
            public void handleEvent(TrackEvent event) {
                if (event.getType() == TrackEvent.Type.ADDED || event.getType() == TrackEvent.Type.REMOVED) {
                    if (pendingTracks != null) {
                        // Track added as part of loading a project.
                        pendingTracks.remove(NetworkUtils.getNeatPathFromURI(event.getTrack().getDataSource().getURI()));
                        if (pendingTracks.isEmpty()) {
                            pendingTracks = null;
                            fireEvent(new ProjectEvent(ProjectEvent.Type.LOADED, getCurrentFile()));
                            setProjectSaved(true);
                        }
                    } else {
                        // A real addition/removal, initiated by the user.
                        setProjectSaved(false);
                    }
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
        if (isProjectOpen() && Savant.getInstance().isStandalone()) {
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
        return saveProjectAs(currentProjectFile);
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
        pendingTracks = proj.getInitialTracks();
        proj.load();
    }

    public void loadProjectFromURL(String urlString) throws Exception {

        URL url  = new URL(urlString);
        InputStream is = url.openStream();
        FileOutputStream fos=null;

        String localName = null;
        StringTokenizer st = new StringTokenizer(url.getFile(), "/");
        while (st.hasMoreTokens()) {
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
            pendingTracks = proj.getInitialTracks();
            proj.load();
        }
    }
}
