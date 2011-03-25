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

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.data.sources.DataSource;
import savant.data.types.Genome;
import savant.exception.SavantEmptySessionException;
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
 * @author mfiume, tarkvara
 */
public class ProjectController {

    private static final Log LOG = LogFactory.getLog(ProjectController.class);

    private static final String GENOME_KEY = "GENOME";
    private static final String GENOMEPATH_KEY = "GENOMEPATH";
    private static final String SEQUENCESET_KEY = "SEQUENCESET";
    private static final String BOOKMARKS_KEY = "BOOKMARKS";
    private static final String TRACKS_KEY = "TRACKS";
    private static final String REFERENCE_KEY = "REFERENCE";
    private static final String RANGE_KEY = "RANGE";

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

    private void writeMap(Map<String, Object> persistentMap, File filename) throws IOException {
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

    private Map<String, Object> readMap(File filename) throws ClassNotFoundException, IOException {

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

    public void saveProjectAs(File filename) throws IOException, SavantEmptySessionException {
        Map<String, Object> persistentMap = getCurrentPersistenceMap();
        writeMap(persistentMap, filename);
        RecentProjectsController.getInstance().addProjectFile(filename);
    }

    public boolean isProjectOpen() {
        return ReferenceController.getInstance().isGenomeLoaded();
    }

    public void loadProjectFrom(File filename) throws ClassNotFoundException, IOException, URISyntaxException, SavantFileNotFormattedException, SavantUnsupportedVersionException {

        clearExistingProject();

        Map<String, Object> m = readMap(filename);

        Boolean isSequenceSet = (Boolean) m.get(SEQUENCESET_KEY);
        Genome genome = (Genome) m.get(GENOME_KEY);
        String genomePath = (String) m.get(GENOMEPATH_KEY);
        List bookmarks = (List) m.get(BOOKMARKS_KEY);
        List trackpaths = (List) m.get(TRACKS_KEY);
        String referencename = (String) m.get(REFERENCE_KEY);
        Range range = (Range) m.get(RANGE_KEY);

        String genomeName;
        if (isSequenceSet) {
            trackpaths.remove(genomePath);
            try {
                try {
                    genome = Track.createGenome(TrackFactory.createTrack(TrackFactory.createDataSource(new URI(genomePath))));
                    //genome = Track.createGenome(TrackFactory.createTrackSync(new URI(genomePath)).get(0));
                } catch (URISyntaxException usx) {
                    // A common cause of URISyntaxExceptions is a file-path containing spaces.
                    genome = Track.createGenome(TrackFactory.createTrack(TrackFactory.createDataSource(new File(genomePath).toURI())));
                    //genome = Track.createGenome(TrackFactory.createTrackSync(new File(genomePath).toURI()).get(0));
                }
            } catch (Exception ex) {
                DialogUtils.displayException("Sorry", "Problem loading project.", ex);
                return;
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
        BookmarkController.getInstance().addBookmarks(bookmarks);

        RecentProjectsController.getInstance().addProjectFile(filename);
    }

    private Persistent[] getRangePersistence() {

        Persistent[] result = new Persistent[2];

        result[0] = new Persistent(REFERENCE_KEY, ReferenceController.getInstance().getReferenceName());
        result[1] = new Persistent(RANGE_KEY, RangeController.getInstance().getRange());

        return result;
    }

    private Persistent getTrackPersistence() {
        List<String> trackpaths = new ArrayList<String>();
        for (Track t : TrackController.getInstance().getTracks()) {
            if (!(t instanceof BAMCoverageTrack)) {
                DataSource ds = t.getDataSource();
                URI uri = ds.getURI();
                if (uri != null) {
                    trackpaths.add(MiscUtils.getNeatPathFromURI(uri));
                }
            }
        }
        return new Persistent(TRACKS_KEY, trackpaths);
    }

    private Persistent getBookmarkPersistence() {
        return new Persistent(BOOKMARKS_KEY, BookmarkController.getInstance().getBookmarks());
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
        result[0] = new Persistent(SEQUENCESET_KEY, isSequenceSet);

        if (isSequenceSet) {
            result[1] = new Persistent(GENOMEPATH_KEY, MiscUtils.getNeatPathFromURI(ReferenceController.getInstance().getGenome().getDataSource().getURI()));
        } else {
            result[1] = new Persistent(GENOME_KEY, ReferenceController.getInstance().getGenome());
        }

        return result;
    }
}
