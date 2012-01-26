/*
 *    Copyright 2010-2012 University of Toronto
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
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.xml.stream.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.adapter.FrameAdapter;
import savant.api.adapter.TrackAdapter;
import savant.api.data.DataFormat;
import savant.api.event.GenomeChangedEvent;
import savant.api.util.Listener;
import savant.controller.*;
import savant.data.types.Genome;
import savant.data.types.Genome.ReferenceInfo;
import savant.exception.SavantEmptySessionException;
import savant.util.Bookmark;
import savant.util.DrawingMode;
import savant.util.NetworkUtils;
import savant.util.Range;
import savant.view.swing.Frame;
import savant.view.swing.variation.VariationController;
import savant.view.tracks.VariantTrack;


/**
 * Class which represents a Savant project file.
 *
 * @author tarkvara
 */
public class Project {

    private static final Log LOG = LogFactory.getLog(Project.class);

    // 1: original version
    // 2: added mode attribute to track element
    private static final int FILE_VERSION = 2;

    private enum XMLElement {
        savant,
        genome,
        reference,
        track,
        bookmark,
        control
    };

    private enum XMLAttribute {
        version,
        name,
        description,
        uri,
        range,
        length,
        cytoband,
        mode
    };
    private Genome genome;
    private List<Bookmark> bookmarks;
    private List<String> trackPaths;
    private List<DrawingMode> trackModes;
    private List<String> controls;
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
        trackModes = new ArrayList<DrawingMode>();
        bookmarks = new ArrayList<Bookmark>();
        controls = new ArrayList<String>();
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
                            trackPaths.add(readAttribute(XMLAttribute.uri));
                            try {
                                trackModes.add(DrawingMode.valueOf(readAttribute(XMLAttribute.mode)));
                            } catch (Exception x) {
                                // Mode attribute is invalid or missing.
                                trackModes.add(null);
                            }
                            break;
                        case bookmark:
                            Bookmark b = new Bookmark(readAttribute(XMLAttribute.range));
                            b.setAnnotation(reader.getElementText());
                            bookmarks.add(b);
                            break;
                        case control:
                            controls.add(reader.getElementText());
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
            writeAttribute(XMLAttribute.uri, NetworkUtils.getNeatPathFromURI(g.getDataSource().getURI()));
        } else {
            for (String ref : g.getReferenceNames()) {
                writeEmptyElement(XMLElement.reference, "    ");
                writeAttribute(XMLAttribute.name, ref);
                writeAttribute(XMLAttribute.length, Integer.toString(g.getLength(ref)));
            }
        }
        writer.writeCharacters("\r\n  ");
        writer.writeEndElement();

        for (FrameAdapter fr: FrameController.getInstance().getOrderedFrames()) {
            TrackAdapter t0 = fr.getTracks()[0];
            URI uri = t0.getDataSource().getURI();
            if (uri != null) {
                writeEmptyElement(XMLElement.track, "  ");
                writeAttribute(XMLAttribute.uri, NetworkUtils.getNeatPathFromURI(uri));
                writeAttribute(XMLAttribute.mode, t0.getDrawingMode().toString());
            }
        }

        for (Bookmark b: BookmarkController.getInstance().getBookmarks()) {
            writeStartElement(XMLElement.bookmark, "  ");
            writeAttribute(XMLAttribute.range, b.getLocationText());
            writer.writeCharacters(b.getAnnotation());
            writer.writeEndElement();
        }

        for (String p: VariationController.getInstance().getControls()) {
            writeStartElement(XMLElement.control, "   ");
            writer.writeCharacters(p);
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
                    for (int i = 0; i < trackPaths.size(); i++) {
                        String path = trackPaths.get(i);
                        DrawingMode mode = trackModes.get(i);
                        if (!path.equals(genomePath)) {
                            LOG.info("Adding ordinary track for " + path);
                            FrameController.getInstance().addTrackFromPath(path, null, mode);
                        }
                    }
                }
            });
            LOG.info("Adding sequence track for " + genomePath);
            FrameController.getInstance().addTrackFromPath(genomePath, DataFormat.SEQUENCE, null);
        } else {
            // Genome in place, so just load the tracks.
            for (int i = 0; i < trackPaths.size(); i++) {
                String path = trackPaths.get(i);
                DrawingMode mode = trackModes != null ? trackModes.get(i) : null;
                FrameController.getInstance().addTrackFromPath(path, null, mode);
            }
        }
        if (bookmarks != null && bookmarks.size() > 0) {
            BookmarkController.getInstance().addBookmarks(bookmarks);
        }
        if (controls != null) {
            VariationController.getInstance().setControls(controls);
        }
    }
}
