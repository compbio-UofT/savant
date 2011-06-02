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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.batik.swing.JSVGCanvas;
import org.apache.batik.swing.gvt.GVTTreeRendererAdapter;
import org.apache.batik.swing.gvt.GVTTreeRendererEvent;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

/**
 *
 * @author AndrewBrook
 */
public class Viewer extends JSplitPane {

    private JScrollPane scrollPane;
    private JScrollPane infoPanel;
    private JLabel infoLabel;
    private JScrollPane treePanel;
    private JSplitPane rightPanel;
    private JTree dataTree;
    private ExtendedJSVGCanvas svgCanvas;
    private Document gpmlDoc;
    private Node pathway;
    //private NodeList comments;
    //private NodeList dataNodes;
    //private NodeList lines;
    private ArrayList<DataNode> dataNodes = new ArrayList<DataNode>();
    private ArrayList<Rectangle> recs = new ArrayList<Rectangle>();
    private String version;

    private Point start;
    private int initialVerticalScroll = 0;
    private int initialHorizontalScroll = 0;

    Viewer() {

        this.setOrientation(JSplitPane.HORIZONTAL_SPLIT);

        //scrollPane
        scrollPane = new JScrollPane();
        scrollPane.setMinimumSize(new Dimension(200,50));
        scrollPane.setPreferredSize(new Dimension(10000,10000));
        this.setLeftComponent(scrollPane);

        //svgCanvas
        svgCanvas = new ExtendedJSVGCanvas();
        svgCanvas.setDocumentState(JSVGCanvas.ALWAYS_DYNAMIC);
        scrollPane.getViewport().add(svgCanvas);

        //treePanel
        treePanel = new JScrollPane();
        treePanel.getViewport().setLayout(new FlowLayout(FlowLayout.LEFT));
        treePanel.getViewport().setBackground(Color.white);
        treePanel.setMaximumSize(new Dimension(200,100));

        //infoPanel
        infoPanel = new JScrollPane();
        infoPanel.getViewport().setLayout(new FlowLayout(FlowLayout.LEFT));
        infoPanel.getViewport().setBackground(Color.white);
        infoPanel.setMaximumSize(new Dimension(200,100));
        infoPanel.setPreferredSize(new Dimension(200,100));

        //infoLabel
        infoLabel = new JLabel();
        infoLabel.setBackground(Color.white);
        infoPanel.getViewport().add(infoLabel);
        
        //rightPanel
        rightPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, treePanel, infoPanel);
        rightPanel.setMinimumSize(new Dimension(200,20));
        rightPanel.setDividerLocation(0.5);
        this.setRightComponent(rightPanel);
        
        this.setDividerLocation(0.8);

        svgCanvas.addGVTTreeRendererListener(new GVTTreeRendererAdapter() {
            @Override
            public void gvtRenderingCompleted(GVTTreeRendererEvent e) {
                revalidate();
                setDividerLocation(0.8);
            }
        });

        svgCanvas.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                tryClick(e.getPoint());
            }
            public void mousePressed(MouseEvent e) {
                start = e.getLocationOnScreen();
                initialVerticalScroll = scrollPane.getVerticalScrollBar().getValue();
                initialHorizontalScroll = scrollPane.getHorizontalScrollBar().getValue();
            }
            public void mouseReleased(MouseEvent e) {}
            public void mouseEntered(MouseEvent e) {}
            public void mouseExited(MouseEvent e) {}
        });

        svgCanvas.addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent e) {
                int x = (int)(e.getLocationOnScreen().getX() - start.getX());
                int y = (int)(e.getLocationOnScreen().getY() - start.getY());
                int newVert = Math.max(Math.min(scrollPane.getVerticalScrollBar().getMaximum(), initialVerticalScroll - y), 0);
                int newHor = Math.max(Math.min(scrollPane.getHorizontalScrollBar().getMaximum(), initialHorizontalScroll - x), 0);
                scrollPane.getVerticalScrollBar().setValue(newVert);
                scrollPane.getHorizontalScrollBar().setValue(newHor);
            }
            @Override
            public void mouseMoved(MouseEvent e) {
                svgCanvas.setCursor(new Cursor(Cursor.HAND_CURSOR));
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
        createTree();
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

        //comments = ((Element) pathway).getElementsByTagName("Comment");
        dataNodes.clear();
        NodeList dataNodeList = ((Element) pathway).getElementsByTagName("DataNode");
        for(int i = 0; i < dataNodeList.getLength(); i++){
            dataNodes.add(new DataNode((Element) (dataNodeList.item(i))));
        }
        //lines = ((Element) pathway).getElementsByTagName("Line");
    }

    private void generateLinks() {

        double scale = 1.0;
        if(version.equals("2008")){
            scale = 0.0667;
        }

        recs.clear();

        for(int i = 0; i < dataNodes.size(); i++){
            
            DataNode node = dataNodes.get(i);
            if(!node.hasSubNode("Graphics")){
                recs.add(null);
                continue;
            }

            float centerX = Float.valueOf(node.getAttribute("Graphics", "CenterX"));
            float centerY = Float.valueOf(node.getAttribute("Graphics", "CenterY"));
            float width = Float.valueOf(node.getAttribute("Graphics", "Width"));
            float height = Float.valueOf(node.getAttribute("Graphics", "Height"));
            recs.add(new Rectangle((int) (scale *(centerX - width / 2.0)),
                    (int) (scale *(centerY - height / 2.0)),
                    (int) (scale * width),
                    (int) (scale * height)));
        }
    }

    private void tryClick(Point p) {
        int i = searchShapes(p);
        if (i == -1) return;
        fillInfo(dataNodes.get(i));     
    }

    private void fillInfo(DataNode dataNode){
        infoLabel.setText(dataNode.getInfoString());
        rightPanel.revalidate();
    }

    private void createTree(){

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("dataTree");

        Map<String, ArrayList<DataNode>> treeMap = new HashMap<String, ArrayList<DataNode>>();
        for(DataNode n : dataNodes){
            String type = n.getType();
            if(treeMap.get(type) == null){
                treeMap.put(type, new ArrayList<DataNode>());
            }
            treeMap.get(type).add(n);
        }
        Iterator it = treeMap.keySet().iterator();
        while(it.hasNext()){
            String key = (String)it.next();
            ArrayList<DataNode> list = treeMap.get(key);
            Collections.sort(list);

            DefaultMutableTreeNode node = new DefaultMutableTreeNode(key);
            for(DataNode n : list){
                node.add(new DefaultMutableTreeNode(n));
            }
            root.add(node);
        }

        dataTree = new JTree(root);
        dataTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        dataTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int selRow = dataTree.getRowForLocation(e.getX(), e.getY());
                TreePath selPath = dataTree.getPathForLocation(e.getX(), e.getY());
                if(selRow != -1) {
                    if(e.getClickCount() == 2) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode)selPath.getLastPathComponent();
                        if (node == null) return;
                        if (node.isLeaf()) {
                            DataNode dataNode = (DataNode)node.getUserObject();
                            fillInfo(dataNode);
                        }
                    }
                }
            }
        });

        dataTree.setRootVisible(false);
        treePanel.getViewport().removeAll();
        treePanel.getViewport().add(dataTree);

        rightPanel.setDividerLocation(0.5);
    }

    /*private void jumpToGene(Element node) {
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
    }*/

   /* private void parseGeneRange(URI uri) {

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
//            NamedNodeMap nnm = typeElement.getAttributes();
//            boolean found = false;
//            for(int j = 0; j < nnm.getLength(); j++){
//                if(nnm.item(j).getNodeName().equals("value") && nnm.item(j).getNodeValue().equals("genomic")){
//                    found = true;
//                }
//            }
//            if(found){
            if(headingElement.getTextContent().equals("RefSeqGene")){
                Node fromNode = el.getElementsByTagName("Seq-interval_from").item(0);
                Node toNode = el.getElementsByTagName("Seq-interval_to").item(0);
                int from = Integer.parseInt(fromNode.getTextContent());
                int to = Integer.parseInt(toNode.getTextContent());
                RangeController.getInstance().setRange(from, to);
                break;
            }
        }
    }*/

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
