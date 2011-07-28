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
package savant.file;

import java.io.*;
import java.net.URI;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.xml.stream.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.adapter.DataSourceAdapter;
import savant.controller.*;
import savant.controller.event.GenomeChangedEvent;
import savant.data.types.Genome;
import savant.data.types.Genome.ReferenceInfo;
import savant.exception.SavantEmptySessionException;
import savant.util.Bookmark;
import savant.util.MiscUtils;
import savant.util.Range;
import savant.view.swing.Savant;
import savant.view.swing.Track;
import savant.view.swing.interval.BAMCoverageTrack;

/**
 * Class which represents a Savant project file.
 *
 * @author tarkvara
 */
public class Project {

    private static final Log LOG = LogFactory.getLog(Project.class);
    private static final int FILE_VERSION = 1;

    private enum XMLElement {
        savant,
        genome,
        reference,
        track,
        bookmark
    };

    private enum XMLAttribute {
        version,
        name,
        description,
        uri,
        range,
        length,
        cytoband
    };
    private Genome genome;
    private List<Bookmark> bookmarks;
    private List<String> trackPaths;
    private String reference;
    private Range range;
    private String genomePath;

    private static XMLStreamWriter writer;
    private static XMLStreamReader reader;

    public Project(File f) throws Exception {
        try {
            createFromSerialization(new FileInputStream(f));
        } catch (StreamCorruptedException x) {
            try {
                // Not a serialization stream.  Let's try opening it as a compressed XML file.
                createFromXML(new GZIPInputStream(new FileInputStream(f)));
            } catch (IOException x2) {
                createFromXML(new FileInputStream(f));
            }
        }
    }

    /**
     * Create a fresh Project object for the given published genome.  May include a sequence
     * track as well as additional auxiliary tracks.
     */
    public Project(Genome genome, URI[] trackURIs) {
        reference = genome.getReferenceNames().iterator().next();
        range = new Range(1, 1000);
        this.genome = genome;
        trackPaths = new ArrayList<String>(trackURIs.length);
        for (URI u : trackURIs) {
            trackPaths.add(u.toString());
        }
    }

    /**
     * Read old-style project files stored using Java serialization.
     * @param input
     */
    private void createFromSerialization(InputStream input) throws ClassNotFoundException, IOException {
        ObjectInputStream objectStream = null;
        try {
            objectStream = new ObjectInputStream(input);
            while (true) {
                try {
                    String key = (String) objectStream.readObject();
                    if (key == null) {
                        break;
                    }
                    Object value = objectStream.readObject();
                    if (key.equals("GENOME")) {
                        genome = (Genome) value;
                    } else if (key.equals("BOOKMARKS")) {
                        bookmarks = (List<Bookmark>) value;
                    } else if (key.equals("TRACKS")) {
                        trackPaths = (List<String>) value;
                    } else if (key.equals("REFERENCE")) {
                        reference = (String) value;
                    } else if (key.equals("RANGE")) {
                        range = (Range) value;
                    }
                } catch (EOFException e) {
                    break;
                }
            }
        } finally {
            try {
                objectStream.close();
            } catch (Exception ignored) {
            }
        }
    }

    private void createFromXML(InputStream input) throws XMLStreamException, ParseException, IOException {
        trackPaths = new ArrayList<String>();
        bookmarks = new ArrayList<Bookmark>();
        String genomeName = null;
        String genomeDesc = null;
        String cytobandPath = null;
        List<ReferenceInfo> references = new ArrayList<ReferenceInfo>();

        reader = XMLInputFactory.newInstance().createXMLStreamReader(input);
        int version = -1;
        boolean done = false;
        do {
            switch (reader.next()) {
                case XMLStreamConstants.START_ELEMENT:
                    switch (readElement()) {
                        case savant:
                            version = Integer.valueOf(readAttribute(XMLAttribute.version));
                            Bookmark r = new Bookmark(readAttribute(XMLAttribute.range));
                            range = (Range) r.getRange();
                            reference = r.getReference();
                            LOG.info("Reading project version " + version);
                            break;
                        case genome:
                            genomeName = readAttribute(XMLAttribute.name);
                            genomeDesc = readAttribute(XMLAttribute.description);
                            genomePath = readAttribute(XMLAttribute.uri);
                            cytobandPath = readAttribute(XMLAttribute.cytoband);
                            break;
                        case reference:
                            references.add(new ReferenceInfo(readAttribute(XMLAttribute.name), Integer.valueOf(readAttribute(XMLAttribute.length))));
                            break;
                        case track:
                            // If the file is well-formed, the track will have exactly one attribute, the URI.
                            trackPaths.add(readAttribute(XMLAttribute.uri));
                            break;
                        case bookmark:
                            Bookmark b = new Bookmark(readAttribute(XMLAttribute.range));
                            b.setAnnotation(reader.getElementText());
                            bookmarks.add(b);
                            break;
                    }
                    break;
                case XMLStreamConstants.END_DOCUMENT:
                    reader.close();
                    done = true;
                    break;
            }
        } while (!done);

        if (cytobandPath != null) {
            genome = new Genome(genomeName, genomeDesc, URI.create(cytobandPath), null);
        } else if (references.size() > 0) {
            genome = new Genome(genomeName, genomeDesc, references.toArray(new ReferenceInfo[0]));
        }
    }

    private static XMLElement readElement() {
        String elemName = reader.getLocalName();
        return Enum.valueOf(XMLElement.class, elemName);
    }

    private static String readAttribute(XMLAttribute attr) {
        return reader.getAttributeValue(null, attr.toString());
    }

    public static void saveToFile(File f) throws IOException, SavantEmptySessionException, XMLStreamException {
        GZIPOutputStream output = new GZIPOutputStream(new FileOutputStream(f));
        writer = XMLOutputFactory.newInstance().createXMLStreamWriter(output, "UTF-8");
        writer.writeStartDocument();
        writeStartElement(XMLElement.savant, "");
        writeAttribute(XMLAttribute.version, Integer.toString(FILE_VERSION));

        LocationController locationController = LocationController.getInstance();
        Range r = locationController.getRange();
        writeAttribute(XMLAttribute.range, String.format("%s:%d-%d", locationController.getReferenceName(), r.getFrom(), r.getTo()));

        writeStartElement(XMLElement.genome, "  ");
        Genome g = GenomeController.getInstance().getGenome();
        writeAttribute(XMLAttribute.name, g.getName());
        URI cytobandURI = g.getCytobandURI();
        if (cytobandURI != null) {
            writeAttribute(XMLAttribute.cytoband, cytobandURI.toString());
        }

        if (g.isSequenceSet()) {
            writeAttribute(XMLAttribute.uri, MiscUtils.getNeatPathFromURI(g.getDataSource().getURI()));
        } else {
            for (String ref : g.getReferenceNames()) {
                writeEmptyElement(XMLElement.reference, "    ");
                writeAttribute(XMLAttribute.name, ref);
                writeAttribute(XMLAttribute.length, Integer.toString(g.getLength(ref)));
            }
        }
        writer.writeCharacters("\r\n  ");
        writer.writeEndElement();

        for (Track t : TrackController.getInstance().getTracks()) {
            if (!(t instanceof BAMCoverageTrack)) {
                DataSourceAdapter ds = t.getDataSource();
                URI uri = ds.getURI();
                if (uri != null) {
                    writeEmptyElement(XMLElement.track, "  ");
                    writeAttribute(XMLAttribute.uri, MiscUtils.getNeatPathFromURI(uri));
                }
            }
        }

        for (Bookmark b : BookmarkController.getInstance().getBookmarks()) {
            writeStartElement(XMLElement.bookmark, "  ");
            writeAttribute(XMLAttribute.range, b.getLocationText());
            writer.writeCharacters(b.getAnnotation());
            writer.writeEndElement();
        }

        writer.writeCharacters("\r\n");
        writer.writeEndElement();
        writer.writeEndDocument();
        output.finish();
        writer.close();
    }

    private static void writeEmptyElement(XMLElement elem, String indent) throws XMLStreamException {
        writer.writeCharacters("\r\n" + indent);
        writer.writeEmptyElement(elem.toString());
    }

    private static void writeStartElement(XMLElement elem, String indent) throws XMLStreamException {
        writer.writeCharacters("\r\n" + indent);
        writer.writeStartElement(elem.toString());
    }

    private static void writeAttribute(XMLAttribute attr, String value) throws XMLStreamException {
        writer.writeAttribute(attr.toString(), value);
    }

    /**
     * Load the project's settings into Savant.
     */
    public void load() throws Exception {
        GenomeController genomeController = GenomeController.getInstance();
        genomeController.setGenome(genome);
        LocationController.getInstance().setLocation(reference, range);

        if (genome == null) {
            // Genome is in the form of a sequence track.  Load it first so that the other tracks
            // will already have a genome in place.
            genomeController.addListener(new Listener<GenomeChangedEvent>() {
                @Override
                public void handleEvent(GenomeChangedEvent event) {
                    GenomeController.getInstance().removeListener(this);
                    LOG.info("Received genomeChanged");
                    for (String path: trackPaths) {
                        if (!path.equals(genomePath)) {
                            LOG.info("Adding ordinary track for " + path);
                            Savant.getInstance().addTrackFromPath(path);
                        }
                    }
                }
            });
            LOG.info("Adding sequence track for " + genomePath);
            Savant.getInstance().addTrackFromPath(genomePath);
        } else {
            // Genome in place, so just load the tracks.
            for (String path : trackPaths) {
                Savant.getInstance().addTrackFromPath(path);
            }
        }
        if (bookmarks != null && bookmarks.size() > 0) {
            BookmarkController.getInstance().addBookmarks(bookmarks);
        }
    }
}
