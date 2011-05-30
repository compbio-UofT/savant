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
import java.util.List;
import java.util.StringTokenizer;
import javax.swing.filechooser.FileFilter;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.util.DialogUtils;
import savant.controller.event.BookmarksChangedEvent;
import savant.controller.event.BookmarksChangedListener;
import savant.controller.event.ProjectEvent;
import savant.controller.event.RangeChangedEvent;
import savant.controller.event.RangeChangedListener;
import savant.controller.event.TrackListChangedEvent;
import savant.controller.event.TrackListChangedListener;
import savant.data.types.Genome;
import savant.exception.SavantEmptySessionException;
import savant.model.Project;
import savant.settings.DirectorySettings;
import savant.util.MiscUtils;


/**
 *
 * @author mfiume, tarkvara
 */
public class ProjectController extends Controller implements BookmarksChangedListener, RangeChangedListener, TrackListChangedListener {
    private static final Log LOG = LogFactory.getLog(ProjectController.class);
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

    private static final File UNTITLED_PROJECT_FILE = new File(DirectorySettings.getProjectsDirectory(), "UntitledProject.svp");

    private static ProjectController instance;

    private boolean projectSaved = true;
    private File currentProjectFile = null;

    public static ProjectController getInstance() {
        if (instance == null) {
            instance = new ProjectController();
            BookmarkController.getInstance().addFavoritesChangedListener(instance);
            RangeController.getInstance().addRangeChangedListener(instance);
            TrackController.getInstance().addTrackListChangedListener(instance);
        }
        return instance;
    }

    private ProjectController() {
    }

    private static void clearExistingProject() {
        TrackController.getInstance().closeTracks();
        BookmarkController.getInstance().clearBookmarks();
    }

    public void saveProjectAs(File f) throws IOException, SavantEmptySessionException, XMLStreamException {
        fireEvent(new ProjectEvent(ProjectEvent.Type.SAVING, f));
        Project.saveToFile(f);
        currentProjectFile = f;
        setProjectSaved(true);
    }

    public static boolean isProjectOpen() {
        return ReferenceController.getInstance().isGenomeLoaded();
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

    @Override
    public void rangeChanged(RangeChangedEvent event) {
        setProjectSaved(false);
    }

    @Override
    public void trackListChanged(TrackListChangedEvent event) {
        setProjectSaved(false);
    }

    @Override
    public void bookmarksChanged(BookmarksChangedEvent event) {
        setProjectSaved(false);
    }

    public void promptUserToLoadProject() throws Exception {

        if (isProjectOpen()) {
            if (!projectSaved) {
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
            loadProjectFromFile(f);
        }
    }

    public boolean promptUserToSaveProjectAs() {

        if (!isProjectOpen()) {
            DialogUtils.displayMessage("No project to save.");
            return false;
        }

        File selectedFile = DialogUtils.chooseFileForSave("Save Project", getCurrentFile().getName(), PROJECT_FILTER, DirectorySettings.getProjectsDirectory());

        if (selectedFile != null) {
            try {
                int result = DialogUtils.YES;
                if (selectedFile.exists()) {
                    result = DialogUtils.askYesNo("Overwrite existing file?");
                }
                if (result == DialogUtils.YES) {
                    saveProjectAs(selectedFile);
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
    public boolean promptUserToSaveSession() throws Exception {
        if (currentProjectFile == null) {
            return promptUserToSaveProjectAs();
        }
        try {
            saveProjectAs(getCurrentFile());
            return true;
        } catch (SavantEmptySessionException ex) {
            LOG.error(String.format("Unable to save %s.", currentProjectFile), ex);
        } catch (Exception ex) {
            LOG.error(String.format("Unable to save %s.", currentProjectFile), ex);
            if (DialogUtils.askYesNo("Error saving project to " + getCurrentFile() + ". Try another location?") == DialogUtils.YES) {
                return promptUserToSaveProjectAs();
            }
        }
        return false;
    }

    private File getCurrentFile() {
        return currentProjectFile != null ? currentProjectFile : UNTITLED_PROJECT_FILE;
    }

    public void loadProjectFromFile(File f) {
        fireEvent(new ProjectEvent(ProjectEvent.Type.LOADING, f));
        currentProjectFile = f;
        try {
            clearExistingProject();
            Project proj = new Project(f);
            proj.load();
            fireEvent(new ProjectEvent(ProjectEvent.Type.LOADED, f));
            setProjectSaved(true);
        } catch (Exception x) {
            LOG.error("Error loading project: " + f, x);
            DialogUtils.displayException("Error Loading Project", String.format("Unable to load %s.", f), x);
        }
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

    public void setProjectFromGenome(Genome genome, URI sequenceURI, List<URI> auxURIs) {
        projectSaved = true;
        currentProjectFile = null;
        fireEvent(new ProjectEvent(ProjectEvent.Type.LOADING, getCurrentFile()));

        try {
            clearExistingProject();
            Project proj = new Project(genome, sequenceURI, auxURIs);
            proj.load();
            setProjectSaved(false); // Will fire UNSAVED
        } catch (Exception x) {
            LOG.error("Error initialising project from " + genome.getName(), x);
            DialogUtils.displayException("Error Loading Project", String.format("Unable to load %s.", genome.getName()), x);
        }
    }
}
