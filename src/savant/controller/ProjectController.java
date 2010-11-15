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

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.data.types.Genome;
import savant.exception.SavantEmptySessionException;
import savant.file.SavantFileNotFormattedException;
import savant.file.SavantUnsupportedVersionException;
import savant.util.Bookmark;
import savant.util.MiscUtils;
import savant.util.Range;
import savant.view.swing.Savant;
import savant.view.swing.ViewTrack;

/**
 *
 * @author mfiume
 */
public class ProjectController {
    private static final Log LOG = LogFactory.getLog(ProjectController.class);

    private static ProjectController instance;
    private String KEY_GENOME = "GENOME";
    private String KEY_GENOMEPATH = "GENOMEPATH";
    private String KEY_SEQUENCESET = "SEQUENCESET";
    private String KEY_BOOKMARKS = "BOOKMARKS";
    private String KEY_TRACKS = "TRACKS";
    private String KEY_REFERENCE = "REFERENCE";
    private String KEY_RANGE = "RANGE";

    public static ProjectController getInstance() {
        if (instance == null) {
            instance = new ProjectController();
        }

        return instance;
    }

    public ProjectController() {
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
        FileOutputStream fos = null;
        ObjectOutputStream outStream = null;
        try {
            fos = new FileOutputStream(filename);
            outStream = new ObjectOutputStream(fos);
            for (String key : persistentMap.keySet()) {
                outStream.writeObject(key);
                outStream.writeObject(persistentMap.get(key));
            }
        } finally {
            try {
                fos.close();
                outStream.close();
            } catch (IOException ex) {
            }
        }
    }

    private Map<String, Object> readMap(String filename) {

        Map<String, Object> map = new HashMap<String,Object>();

        FileInputStream fis = null;
        ObjectInputStream inStream = null;
        try {
            fis = new FileInputStream(filename);
            inStream = new ObjectInputStream(fis);
            String key = "";
            Object value;
            while(true) {
                try {
                    key = (String) inStream.readObject();
                    if (key == null) { break; }
                    value = inStream.readObject();
                    map.put(key, value);
                } catch (EOFException e) { break; }
            }
        } catch (ClassNotFoundException ex) {
            LOG.error("Unable to read map for " + filename, ex);
        } catch (IOException ex) {
            LOG.error("Unable to read map for " + filename, ex);
        }

        return map;
    }

    private void clearExistingProject() {
        // close tracks, clear bookmarks
        ViewTrackController.getInstance().closeTracks();
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
        writeMap(persistentMap,filename);
        RecentProjectsController.getInstance().addProjectFile(filename);
    }

    public boolean isProjectOpen() {
        return ReferenceController.getInstance().isGenomeLoaded();
    }

    public void loadProjectFrom(String filename) throws IOException, URISyntaxException, SavantFileNotFormattedException, SavantUnsupportedVersionException {

        clearExistingProject();

        Map<String, Object> m = readMap(filename);

        Boolean isSequenceSet = (Boolean) m.get(KEY_SEQUENCESET);
        Genome genome = (Genome) m.get(KEY_GENOME);
        String genomepath = (String) m.get(KEY_GENOMEPATH);
        List bookmarks = (List) m.get(KEY_BOOKMARKS);
        List trackpaths = (List) m.get(KEY_TRACKS);
        String referencename = (String) m.get(KEY_REFERENCE);
        Range range = (Range) m.get(KEY_RANGE);

        String genomeName;
        if (isSequenceSet) {
            trackpaths.remove(genomepath);
            genome = ViewTrack.createGenome(genomepath);
            genomeName = genome.getTrack().getURI().toString();
        } else {
            genomeName = genome.getName();
        }
        Savant.getInstance().setGenome(genomeName, genome);

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
        for (ViewTrack t : ViewTrackController.getInstance().getTracks()) {
            URI trackURI = t.getURI();
            if (trackURI != null) {
                String path = trackURI.toString();
                if(trackURI.getScheme().equals("file")){
                    path = MiscUtils.getNeatPathFromURI(trackURI);
                }
                trackpaths.add(path);
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
            String path = ReferenceController.getInstance().getGenome().getTrack().getURI().toString();
            if(ReferenceController.getInstance().getGenome().getTrack().getURI().getScheme().equals("file")){
                path = MiscUtils.getNeatPathFromURI(ReferenceController.getInstance().getGenome().getTrack().getURI());
            }
            //result[1] = new Persistent(KEY_GENOMEPATH, MiscUtils.getNeatPathFromURI(ReferenceController.getInstance().getGenome().getTrack().getURI()));
            result[1] = new Persistent(KEY_GENOMEPATH, path);
        } else {
            result[1] = new Persistent(KEY_GENOME, ReferenceController.getInstance().getGenome());
        }

        return result;
    }
}
