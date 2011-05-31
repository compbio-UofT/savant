/*
 *    Copyright 2011 University of Toronto
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
package savant.pathways;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Dimension2D;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.batik.swing.JSVGCanvas;
import org.apache.batik.swing.gvt.GVTTreeRendererAdapter;
import org.apache.batik.swing.gvt.GVTTreeRendererEvent;
import org.apache.batik.swing.svg.SVGDocumentLoaderEvent;
import org.apache.batik.swing.svg.SVGDocumentLoaderListener;
import org.w3c.dom.*;
import org.xml.sax.SAXException;
import savant.controller.RangeController;
import savant.settings.DirectorySettings;

/**
 *
 * @author AndrewBrook
 */
public class Viewer extends JPanel {

    private JScrollPane scrollPane;
    private JPanel infoPanel;
    private JLabel infoLabel;
    private ExtendedJSVGCanvas svgCanvas;
    private Document gpmlDoc;
    private Node pathway;
    private NodeList comments;
    private NodeList dataNodes;
    private NodeList lines;
    private ArrayList<Rectangle> recs = new ArrayList<Rectangle>();
    private String version;

    Viewer() {

        this.setLayout(new BorderLayout());

        //scrollPane
        scrollPane = new JScrollPane();
        this.add(scrollPane, BorderLayout.CENTER);

        //svgCanvas
        svgCanvas = new ExtendedJSVGCanvas();
        svgCanvas.setDocumentState(JSVGCanvas.ALWAYS_DYNAMIC);
        scrollPane.getViewport().add(svgCanvas);

        //infoPanel
        infoPanel = new JPanel();
        infoPanel.setMinimumSize(new Dimension(200, 10));
        infoPanel.setMaximumSize(new Dimension(300, 999999));
        this.add(infoPanel, BorderLayout.EAST);

        //infoLabel
        infoLabel = new JLabel();
        infoPanel.add(infoLabel);

        svgCanvas.addGVTTreeRendererListener(new GVTTreeRendererAdapter() {

            @Override
            public void gvtRenderingCompleted(GVTTreeRendererEvent e) {
                revalidate();
            }
        });

        svgCanvas.addSVGDocumentLoaderListener(new SVGDocumentLoaderListener() {

            @Override
            public void documentLoadingStarted(SVGDocumentLoaderEvent svgdle) {
            }

            @Override
            public void documentLoadingCompleted(SVGDocumentLoaderEvent svgdle) {
            }

            @Override
            public void documentLoadingCancelled(SVGDocumentLoaderEvent svgdle) {
            }

            @Override
            public void documentLoadingFailed(SVGDocumentLoaderEvent svgdle) {
            }
        });


        svgCanvas.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
                tryClick(e.getPoint());
            }

            public void mousePressed(MouseEvent e) {
            }

            public void mouseReleased(MouseEvent e) {
            }

            public void mouseEntered(MouseEvent e) {
            }

            public void mouseExited(MouseEvent e) {
            }
        });

    }

    public void setPathway(URI svgUri, URI gpmlUri) {
        svgCanvas.setURI(svgUri.toString());      
        getGPML(gpmlUri);
    }

    private void getGPML(URI uri) {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            gpmlDoc = db.parse(uri.toString());
        } catch (ParserConfigurationException pce) {
            System.out.println("ERROR");
            //TODO
        } catch (SAXException se) {
            System.out.println("ERROR");
            //TODO
        } catch (IOException ioe) {
            System.out.println("ERROR");
            //TODO
        }

        parseGPML();
        generateLinks();
    }

    private void parseGPML() {

        gpmlDoc.getDocumentElement().normalize();

        NodeList nodes = gpmlDoc.getElementsByTagName("Pathway");
        pathway = nodes.item(0);

        version = ((Element) pathway).getAttribute("xmlns");
        if(version == null || version.equals("")){
            version = "unknown";
        } else if (version.indexOf("2008") != -1){
            version = "2008";
        } else if (version.indexOf("2010") != -1){
            version = "2010";
        } else {
            version = "unknown";
        }

        comments = ((Element) pathway).getElementsByTagName("Comment");
        dataNodes = ((Element) pathway).getElementsByTagName("DataNode");
        lines = ((Element) pathway).getElementsByTagName("Line");
    }

    private void generateLinks() {

        double scale = 1.0;
        if(version.equals("2008")){
            scale = 0.0667;
        }

        recs.clear();
        for (int i = 0; i < dataNodes.getLength(); i++) {

            Node node = dataNodes.item(i);
            NodeList graphics = ((Element) node).getElementsByTagName("Graphics");
            if (graphics.getLength() == 0) {
                recs.add(null);
                continue;
            }

            Element el = (Element) (graphics.item(0));
            float centerX = Float.valueOf(el.getAttribute("CenterX"));
            float centerY = Float.valueOf(el.getAttribute("CenterY"));
            float width = Float.valueOf(el.getAttribute("Width"));
            float height = Float.valueOf(el.getAttribute("Height"));
            recs.add(new Rectangle((int) (scale *(centerX - width / 2.0)),
                    (int) (scale *(centerY - height / 2.0)),
                    (int) (scale * width),
                    (int) (scale * height)));
        }
    }

    private void tryClick(Point p) {
        int i = searchShapes(p);
        if (i == -1) {
            return;
        }

        Element currentNode = (Element) (dataNodes.item(i));
        //jumpToGene(currentNode);
        fillInfo(currentNode);
    }

    private void fillInfo(Element node) {
        String s = "<HTML>";
        s += "<B>" + node.getTagName() + "</B><BR>";
        NamedNodeMap nnm = node.getAttributes();
        for (int j = 0; j < nnm.getLength(); j++) {
            s += nnm.item(j).getNodeName() + ": " + nnm.item(j).getNodeValue() + "<BR>";
        }
        s += "<BR>";

        String[] elementNames = {"Attribute", "Xref"};

        for (int j = 0; j < elementNames.length; j++) {
            NodeList elements = node.getElementsByTagName(elementNames[j]);
            if (elements == null || elements.getLength() == 0) {
                continue;
            }
            Element el = (Element) (elements.item(0));

            s += "<B>" + el.getTagName() + "</B><BR>";
            nnm = el.getAttributes();
            for (int k = 0; k < nnm.getLength(); k++) {
                s += nnm.item(k).getNodeName() + ": " + nnm.item(k).getNodeValue() + "<BR>";
            }
            s += "<BR>";
        }

        s += "</HTML>";
        infoLabel.setText(s);
    }

    private void jumpToGene(Element node) {
        NodeList Xrefs = node.getElementsByTagName("Xref");
        if (Xrefs == null || Xrefs.getLength() == 0) {
            return;
        }
        Element XrefElement = (Element) (Xrefs.item(0));
        String db = XrefElement.getAttribute("Database");
        String id = XrefElement.getAttribute("ID");

        if (db.equals("Entrez Gene") && !id.equals("")) {

            String filename = DirectorySettings.getTmpDirectory() + System.getProperty("file.separator") + "geneInfo_" + id + ".xml";

            //download file
            try {
                URL query = new URL("http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=gene&id=" + id + "&retmode=xml");
                OutputStream os = new BufferedOutputStream(
                        new FileOutputStream(filename));
                URLConnection uc = query.openConnection();
                InputStream is = uc.getInputStream();

                byte[] buf = new byte[153600];
                int totalBytesRead = 0;
                int bytesRead = 0;

                while ((bytesRead = is.read(buf)) > 0) {
                    os.write(buf, 0, bytesRead);
                    buf = new byte[153600];
                    totalBytesRead += bytesRead;
                }

                is.close();
                os.close();
            } catch (Exception e) {
                //TODO
                e.printStackTrace();
                return;
            }

            //find location
            parseGeneRange(new File(filename).toURI());

            //go to location

        }
    }

    private void parseGeneRange(URI uri) {

        //TODO improve speed, narrow search by incrementally navigating doc

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Document doc;
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            doc = db.parse(uri.toString());
        } catch (Exception e){
            System.out.println("ERROR");
            return;
            //TODO
        }

        doc.getDocumentElement().normalize();
        NodeList nodes = doc.getElementsByTagName("Gene-commentary");

        for(int i = 0; i < nodes.getLength(); i++){
            Element el = (Element)(nodes.item(i));
            NodeList headingList = el.getElementsByTagName("Gene-commentary_heading");
            if(headingList == null || headingList.getLength() == 0) continue;
            Node headingElement = el.getElementsByTagName("Gene-commentary_heading").item(0);
            /*NamedNodeMap nnm = typeElement.getAttributes();
            boolean found = false;
            for(int j = 0; j < nnm.getLength(); j++){
                if(nnm.item(j).getNodeName().equals("value") && nnm.item(j).getNodeValue().equals("genomic")){
                    found = true;
                }
            }
            if(found){*/
            if(headingElement.getTextContent().equals("RefSeqGene")){
                Node fromNode = el.getElementsByTagName("Seq-interval_from").item(0);
                Node toNode = el.getElementsByTagName("Seq-interval_to").item(0);
                int from = Integer.parseInt(fromNode.getTextContent());
                int to = Integer.parseInt(toNode.getTextContent());
                RangeController.getInstance().setRange(from, to);
                break;
            }
        }

        

    }

    private int searchShapes(Point p) {
        for (int i = 0; i < recs.size(); i++) {
            if (recs.get(i) != null && recs.get(i).contains(p)) {
                return i;
            }
        }
        return -1;
    }

    private class ExtendedJSVGCanvas extends JSVGCanvas {

        ExtendedJSVGCanvas() {
            super();
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(Color.red);
            for (Rectangle rec : recs) {
                if (rec == null) {
                    continue;
                }
                g.drawRect((int) rec.getX(), (int) rec.getY(), (int) rec.getWidth(), (int) rec.getHeight());
            }

        }
    }
}
