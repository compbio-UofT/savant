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
package savant.controller;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import savant.data.sources.file.FileDataSource;

import savant.data.types.Genome;
import savant.exception.SavantEmptySessionException;
import savant.exception.SavantTrackCreationCancelledException;
import savant.file.SavantFileNotFormattedException;
import savant.file.SavantUnsupportedVersionException;
import savant.util.Bookmark;
import savant.util.MiscUtils;
import savant.util.Range;
import savant.view.swing.Savant;
import savant.view.swing.TrackFactory;
import savant.view.swing.Track;
import savant.view.swing.interval.BAMCoverageTrack;
import savant.view.swing.util.DialogUtils;

/**
 *
 * @author mfiume
 */
public class ProjectController {

    private static final Log LOG = LogFactory.getLog(ProjectController.class);
    private static final String KEY_GENOME = "GENOME";
    private static final String KEY_GENOMEPATH = "GENOMEPATH";
    private static final String KEY_SEQUENCESET = "SEQUENCESET";
    private static final String KEY_BOOKMARKS = "BOOKMARKS";
    private static final String KEY_TRACKS = "TRACKS";
    private static final String KEY_REFERENCE = "REFERENCE";
    private static final String KEY_RANGE = "RANGE";

    private static ProjectController instance;

    public static ProjectController getInstance() {
        if (instance == null) {
            instance = new ProjectController();
        }
        return instance;
    }

    private ProjectController() {
    }

    private void addToPersistenceMap(Persistent[] plist, Map<String, Object> persistentMap) {
        for (Persistent p : plist) {
            addToPersistenceMap(p, persistentMap);
        }
    }

    private void addToPersistenceMap(Persistent p, Map<String, Object> persistentMap) {
        persistentMap.put(p.key, p.value);
    }

    private Map<String, Object> getCurrentPersistenceMap() throws SavantEmptySessionException {
        Map<String, Object> persistentMap = new HashMap<String, Object>();

        addToPersistenceMap(getGenomePersistence(), persistentMap);
        addToPersistenceMap(getRangePersistence(), persistentMap);
        addToPersistenceMap(getTrackPersistence(), persistentMap);
        addToPersistenceMap(getBookmarkPersistence(), persistentMap);

        return persistentMap;
    }

    private void writeMap(Map<String, Object> persistentMap, String filename) throws IOException {
        ObjectOutputStream outStream = null;
        try {
            outStream = new ObjectOutputStream(new FileOutputStream(filename));
            for (String key : persistentMap.keySet()) {
                outStream.writeObject(key);
                outStream.writeObject(persistentMap.get(key));
            }
        } finally {
            try {
                outStream.close();
            } catch (Exception ignored) {
            }
        }
    }

    private Map<String, Object> readMap(String filename) throws ClassNotFoundException, IOException {

        Map<String, Object> map = new HashMap<String, Object>();

        ObjectInputStream inStream = null;
        try {
            inStream = new ObjectInputStream(new FileInputStream(filename));
            while (true) {
                try {
                    String key = (String) inStream.readObject();
                    if (key == null) {
                        break;
                    }
                    Object value = inStream.readObject();
                    map.put(key, value);
                } catch (EOFException e) {
                    break;
                }
            }
        } finally {
            try {
                inStream.close();
            } catch (Exception ignored) {
            }
        }

        return map;
    }

    private void clearExistingProject() {
        // close tracks, clear bookmarks
        TrackController.getInstance().closeTracks();
        BookmarkController.getInstance().clearBookmarks(); //TODO: check if bookmark UI is actually updated by this
    }

    public class Persistent {

        public String key;
        public Object value;

        public Persistent(String k, Object o) {
            this.key = k;
            this.value = o;
        }
    }

    public void saveProjectAs(File f) throws IOException, SavantEmptySessionException {
        saveProjectAs(f.getAbsolutePath());
    }

    public void saveProjectAs(String filename) throws IOException, SavantEmptySessionException {
        Map<String, Object> persistentMap = getCurrentPersistenceMap();
        writeMap(persistentMap, filename);
        RecentProjectsController.getInstance().addProjectFile(filename);
    }

    public boolean isProjectOpen() {
        return ReferenceController.getInstance().isGenomeLoaded();
    }

    public void loadProjectFrom(String filename) throws ClassNotFoundException, IOException, URISyntaxException, SavantFileNotFormattedException, SavantUnsupportedVersionException {

        clearExistingProject();

        Map<String, Object> m = readMap(filename);

        Boolean isSequenceSet = (Boolean) m.get(KEY_SEQUENCESET);
        Genome genome = (Genome) m.get(KEY_GENOME);
        String genomePath = (String) m.get(KEY_GENOMEPATH);
        List bookmarks = (List) m.get(KEY_BOOKMARKS);
        List trackpaths = (List) m.get(KEY_TRACKS);
        String referencename = (String) m.get(KEY_REFERENCE);
        Range range = (Range) m.get(KEY_RANGE);

        String genomeName;
        if (isSequenceSet) {
            trackpaths.remove(genomePath);
            try {
                try {
                    genome = Track.createGenome(TrackFactory.createTrackSync(new URI(genomePath)).get(0));
                } catch (SavantTrackCreationCancelledException ex) {
                    DialogUtils.displayMessage("Sorry", "Problem loading project.");
                    return;
                }
            } catch (URISyntaxException usx) {
                try {
                    // A common cause of URISyntaxExceptions is a file-path containing spaces.
                    genome = Track.createGenome(TrackFactory.createTrackSync(new File(genomePath).toURI()).get(0));
                } catch (SavantTrackCreationCancelledException ex) {
                    DialogUtils.displayMessage("Sorry", "Problem loading project.");
                    return;
                }
            }
            genomeName = genome.getDataSource().getURI().toString();
        } else {
            genomeName = genome.getName();
        }
        Savant.getInstance().setGenome(genomeName, genome, null);

        ReferenceController.getInstance().setReference(referencename);
        RangeController.getInstance().setRange(range);
        for (Object path : trackpaths) {
            Savant.getInstance().addTrackFromFile((String) path);
        }
        BookmarkController bc = BookmarkController.getInstance();
        for (Object bkmk : bookmarks) {
            bc.addBookmark((Bookmark) bkmk);
        }

        RecentProjectsController.getInstance().addProjectFile(filename);
    }

    private Persistent[] getRangePersistence() {

        Persistent[] result = new Persistent[2];

        result[0] = new Persistent(KEY_REFERENCE, ReferenceController.getInstance().getReferenceName());
        result[1] = new Persistent(KEY_RANGE, RangeController.getInstance().getRange());

        return result;
    }

    private Persistent getTrackPersistence() {
        List<String> trackpaths = new ArrayList<String>();
        for (Track t : TrackController.getInstance().getTracks()) {
            if (!(t instanceof BAMCoverageTrack) && (t.getDataSource() instanceof FileDataSource)) {
                if (((FileDataSource) t.getDataSource()).getURI() != null) {
                    trackpaths.add(MiscUtils.getNeatPathFromURI(((FileDataSource) t.getDataSource()).getURI()));
                }
            }
        }
        return new Persistent(KEY_TRACKS, trackpaths);
    }

    private Persistent getBookmarkPersistence() {
        return new Persistent(KEY_BOOKMARKS, BookmarkController.getInstance().getBookmarks());
    }

    private void writeHeader(String header, ObjectOutputStream outStream) throws IOException {
        outStream.writeObject(header);
    }

    private Persistent[] getGenomePersistence() throws SavantEmptySessionException {

        Persistent[] result = new Persistent[2];

        if (ReferenceController.getInstance().getGenome() == null) {
            throw new SavantEmptySessionException();
        }

        boolean isSequenceSet = ReferenceController.getInstance().getGenome().isSequenceSet();
        result[0] = new Persistent(KEY_SEQUENCESET, isSequenceSet);

        if (isSequenceSet) {
            result[1] = new Persistent(KEY_GENOMEPATH, MiscUtils.getNeatPathFromURI(ReferenceController.getInstance().getGenome().getDataSource().getURI()));
        } else {
            result[1] = new Persistent(KEY_GENOME, ReferenceController.getInstance().getGenome());
        }

        return result;
    }
}
