/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package savant.controller;

import java.awt.Component;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFileChooser;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import savant.data.types.Genome;
import savant.file.SavantUnsupportedVersionException;
import savant.settings.DirectorySettings;
import savant.util.Range;
import savant.view.dialog.GenomeLengthForm;
import savant.view.dialog.GenomeLengthForm.BuildInfo;
import savant.view.dialog.GenomeLengthForm.SpeciesInfo;
import savant.view.swing.ViewTrack;
import javax.swing.JOptionPane;
import savant.controller.event.bookmark.BookmarksChangedEvent;
import savant.controller.event.bookmark.BookmarksChangedListener;
import savant.controller.event.range.RangeChangedEvent;
import savant.controller.event.range.RangeChangedListener;
import savant.controller.event.viewtrack.ViewTrackListChangedEvent;
import savant.controller.event.viewtrack.ViewTrackListChangedListener;
import savant.model.session.Session;
import savant.view.swing.Savant;

/**
 *
 * @author mfiume
 */
public class SessionController implements
        BookmarksChangedListener,
        RangeChangedListener,
        ViewTrackListChangedListener {

    private boolean saved;
    private String currentSessionPath;
    private static SessionController instance;

    public SessionController() {
        saved = true;
        addListeners();
    }

    
    public static SessionController getInstance() {
        if (instance == null) {
            instance = new SessionController();
        }
        return instance;
    }

    public boolean isSessionSaved() {
        return saved;

    }

    private void setSessionSaved(boolean arg) {
        saved = arg;
    }

    @Override
    public void rangeChangeReceived(RangeChangedEvent event) {
        setSessionSaved(false);
    }

    @Override
    public void viewTrackListChangeReceived(ViewTrackListChangedEvent event) {
        setSessionSaved(false);
    }

    @Override
    public void bookmarksChangeReceived(BookmarksChangedEvent event) {
        setSessionSaved(false);
    }

    public void saveCurrentSession() {
        if (!isSessionSaved()) {
            System.out.println("Saving current session");
            int result = JOptionPane.showConfirmDialog(Savant.getInstance(), "Save current session?");
            if (result == JOptionPane.YES_OPTION) {
                saveSession(Savant.getInstance());
            }
        }
    }

    private void clearExistingSession() {

        System.out.println("Clearing existin session");

        // close tracks, clear bookmarks
        ViewTrackController.getInstance().closeTracks();
        BookmarkController.getInstance().clearBookmarks(); //TODO: check if bookmark UI is actually updated by this
    }

    public void loadSession(Component c, boolean askFirst) {

        if (askFirst) {
            saveCurrentSession();
        }

        System.out.println("Loading session");

        JFileChooser jfc = new JFileChooser();
        jfc.setCurrentDirectory(new File(DirectorySettings.getSessionsDirectory()));
        int result = jfc.showOpenDialog(c);
        if (result == JFileChooser.APPROVE_OPTION) {
            String filename = jfc.getSelectedFile().getAbsolutePath();
            loadSession(filename, false);
        }
    }

    public void setSession(Session s) {

        System.out.println("Setting session");
        
        Savant.getInstance().setGenome(s.genomeName, s.genome);
        ReferenceController.getInstance().setReference(s.reference);
        RangeController.getInstance().setRange(s.range);
        for (String trackPath : s.trackPaths) {
            try {
                Savant.getInstance().addTrackFromFile(trackPath);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(Savant.getInstance(), "Error opening track from " + trackPath);
            }
        }
    }

    public void loadSession(String filename, boolean askToSaveCurrentSessionFirst){
        try {
            if (askToSaveCurrentSessionFirst) {
                saveCurrentSession();
            }
            clearExistingSession();
            Document d = new SAXBuilder().build(new File(filename));
            Element root = d.getRootElement();
            String genomeFile = getGenomeTrack(root.getChild("tracks"));
            List<String> trackFiles = getTracks(root.getChild("tracks"));
            String reference = root.getChild("range").getChildText("ref");
            int start = Integer.parseInt(root.getChild("range").getChildText("start"));
            int end = Integer.parseInt(root.getChild("range").getChildText("end"));
            Element gel = root.getChild("genome");
            Genome genome = null;
            String genomeName = null;
            // genome has sequence
            if (genomeFile != null) {
                genome = new Genome(genomeFile);
                genomeName = genome.getFilename();
                // genome does not have sequence
            } else {
                String name = gel.getChild("nosequence").getText();
                genomeName = name;
                if (name.equals("user specified")) {
                    Long len = Long.parseLong(gel.getChild("length").getText());
                    genome = new Genome(name, len);
                } else {
                    List<SpeciesInfo> speciesInfo = GenomeLengthForm.getGenomeInformation();
                    boolean done = false;
                    for (SpeciesInfo si : speciesInfo) {
                        for (BuildInfo bi : si.builds) {
                            if (bi.name.equals(name)) {
                                genome = new Genome(bi);
                                done = true;
                                break;
                            }
                        }
                        if (done) {
                            break;
                        }
                    }
                }
            }
            setSession(new Session(genomeName, genome, trackFiles, reference, new Range(start, end), null));
            this.currentSessionPath = filename;
            this.setSessionSaved(true);

        } catch (SavantUnsupportedVersionException ex) {
            JOptionPane.showMessageDialog(Savant.getInstance(), "Problem loading session");
        } catch (JDOMException ex) {
            JOptionPane.showMessageDialog(Savant.getInstance(), "Problem loading session");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(Savant.getInstance(), "Problem loading session");
        }
    }

    public void saveSession(Component parent) {

        if (!ReferenceController.getInstance().isGenomeLoaded()
                || ViewTrackController.getInstance().getTracks().isEmpty()) {
            JOptionPane.showMessageDialog(parent, "No session to be saved.");
            return;
        }

        JFileChooser jfc = new JFileChooser();
        jfc.setCurrentDirectory(new File(DirectorySettings.getSavantDirectory()));
        jfc.setDialogTitle("Save Session");
        jfc.setDialogType(JFileChooser.SAVE_DIALOG);
        if (this.currentSessionPath == null) {
            jfc.setSelectedFile(new File(DirectorySettings.getSessionsDirectory() + System.getProperty("file.separator") + "NewSession.ssn"));
        } else {
            jfc.setSelectedFile(new File(currentSessionPath));
        }
        if (jfc.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
            try {
                saveSession(jfc.getSelectedFile());
            } catch (IOException ex) {
                if (JOptionPane.showInternalConfirmDialog(parent,
                        "Error saving session to "
                        + jfc.getSelectedFile().getAbsolutePath()
                        + ". Try another location?") == JOptionPane.YES_OPTION) {
                    saveSession(parent);
                }

            } catch (Exception e) {
                JOptionPane.showMessageDialog(parent, "Error saving session.");
            }
        }
    }

    public boolean saveSession(File f) throws IOException {

        if (!ReferenceController.getInstance().isGenomeLoaded()) {
            return false;
        }

        Element root = new Element("savantsession");

        if (ReferenceController.getInstance().getGenome() != null) {

            Element genome = new Element("genome");
            if (ReferenceController.getInstance().getGenome().isSequenceSet()) {
                Element gtrack = new Element("hassequence");
                gtrack.setText(ReferenceController.getInstance().getGenome().getName());
                genome.addContent(gtrack);
            } else {
                Element ginfo = new Element("nosequence");
                String gname = ReferenceController.getInstance().getGenome().getName();
                ginfo.setText(gname);
                genome.addContent(ginfo);
                if (gname.equals("user defined")) {
                    Element linfo = new Element("length");
                    linfo.setText(ReferenceController.getInstance().getGenome().getLength() + "");
                    genome.addContent(linfo);
                }
            }

            root.addContent(genome);
        }

        try {
            Element range = new Element("range");
            Element ref = new Element("ref");
            ref.setText(ReferenceController.getInstance().getReferenceName());
            Element start = new Element("start");
            start.setText(RangeController.getInstance().getRangeStart() + "");
            Element end = new Element("end");
            end.setText(RangeController.getInstance().getRangeEnd() + "");

            range.addContent(ref);
            range.addContent(start);
            range.addContent(end);

            root.addContent(range);

        } catch (java.lang.NullPointerException e) {
        }

        if (ViewTrackController.getInstance().getTracks().size() > 0) {
            Element tracks = new Element("tracks");

            for (ViewTrack t : ViewTrackController.getInstance().getTracks()) {
                Element track;
                if (ReferenceController.getInstance().getGenome().getName().equals(t.getName())) {
                    track = new Element("genome");
                } else {
                    track = new Element("track");
                }
                System.out.println("Filename: " + t.getPath());

                // BAM Coverage tracks have null paths
                if (t.getPath() != null) {
                    track.addContent(t.getPath());
                    tracks.addContent(track);
                }
            }

            root.addContent(tracks);
        }

        Document d = new Document(root);

        XMLOutputter serializer = new XMLOutputter();
        FileOutputStream fout = new FileOutputStream(f);
        serializer.output(d, fout);
        fout.close();

        this.currentSessionPath = f.getAbsolutePath();
        this.setSessionSaved(true);
        RecentSessionController.getInstance().addSessionFile(f.getAbsolutePath());

        return true;
    }

    private String getGenomeTrack(Element tracks) {
        Element g = tracks.getChild("genome");
        if (g == null) {
            return null;
        }
        return g.getText();
    }

    private List<String> getTracks(Element tracks) {
        List<Element> trackList = tracks.getChildren("track");
        List<String> trackPaths = new ArrayList<String>();
        for (Element t : trackList) {
            trackPaths.add(t.getText());
        }
        return trackPaths;
    }

    private void addListeners() {
        BookmarkController.getInstance().addFavoritesChangedListener(this);
        RangeController.getInstance().addRangeChangedListener(this);
        ViewTrackController.getInstance().addTracksChangedListener(this);
    }
}
