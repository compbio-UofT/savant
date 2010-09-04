/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.model.session;

import java.awt.Component;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import savant.controller.RangeController;
import savant.controller.ReferenceController;
import savant.controller.ViewTrackController;
import savant.data.types.Genome;
import savant.settings.DirectorySettings;
import savant.util.MiscUtils;
import savant.util.Range;
import savant.view.swing.ViewTrack;

/**
 *
 * @author mfiume
 */
public class Session {

    Genome g;
    String reference;
    Range range;
    List<String> tracknames;

    public static void SaveSession(Component parent) {
        JFileChooser jfc = new JFileChooser();
        jfc.setCurrentDirectory(new File(DirectorySettings.getSavantDirectory()));
        jfc.setDialogTitle("Save Session");
        jfc.setDialogType(JFileChooser.SAVE_DIALOG);
        jfc.setSelectedFile(new File("savant." + MiscUtils.now().replace(", ", "_").replace(" ", "_").replace(":", "-") + ".session"));
        if (jfc.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
            try {
                SaveSession(jfc.getSelectedFile());
            } catch (IOException ex) {
                ex.printStackTrace();
                if (JOptionPane.showInternalConfirmDialog(parent,
                        "Error saving session to "
                        + jfc.getSelectedFile().getAbsolutePath()
                        + ". Try another location?") == JOptionPane.YES_OPTION){
                    SaveSession(parent);
                }

            } catch (Exception e) {
                JOptionPane.showMessageDialog(parent,"Error saving session.");
                e.printStackTrace();
            }
        }
    }

    public static void SaveSession(File f) throws IOException {

        Element root = new Element("savantsession");

        if (ReferenceController.getInstance().getGenome() != null) {

            Element genome = new Element("genome");
            if (ReferenceController.getInstance().getGenome().isSequenceSet()) {
                Element gtrack = new Element("fromfile");
                gtrack.setText(ReferenceController.getInstance().getGenome().getName());
                genome.addContent(gtrack);
            } else {
                Element ginfo = new Element("bylength");
                ginfo.setText(ReferenceController.getInstance().getGenome().getName());
                genome.addContent(ginfo);
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

        } catch (java.lang.NullPointerException e) {}

        if (ViewTrackController.getInstance().getTracks().size() > 0) {
            Element tracks = new Element("tracks");

            for (ViewTrack t : ViewTrackController.getInstance().getTracks()) {
                Element track = new Element("track");

                tracks.addContent(track);
            }

            root.addContent(tracks);
        }
        
        Document d = new Document(root);

        XMLOutputter serializer = new XMLOutputter();
        serializer.output(d, new FileOutputStream(f));
    }
}
