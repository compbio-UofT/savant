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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.border.Border;
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
import savant.controller.RangeController;
import savant.controller.ReferenceController;
import savant.util.Range;

/**
 *
 * @author AndrewBrook
 */
public class Viewer extends JSplitPane {

    private Loader loader;

    private JScrollPane scrollPane;
    private JScrollPane infoScroll;
    private JPanel infoPanel;
    private JLabel infoLabel;
    private JScrollPane treeScroll;
    private JSplitPane rightPanel;
    private JTree dataTree;
    private JButton jumpLocationButton;
    private JButton linkOutButton;
    private JButton jumpPathwayButton;
    private ExtendedJSVGCanvas svgCanvas;
    private Document gpmlDoc;
    private Node pathway;
    //private NodeList comments;
    //private NodeList dataNodes;
    //private NodeList lines;
    private ArrayList<DataNode> dataNodes = new ArrayList<DataNode>();
    private ArrayList<Rectangle> recs = new ArrayList<Rectangle>();
    private String version;

    private Gene jumpGene;
    private String jumpPathway;
    private String linkOutUrl;

    private Point start;
    private int initialVerticalScroll = 0;
    private int initialHorizontalScroll = 0;

    Viewer(Loader loader) {

        this.loader = loader;

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

        //treeScroll
        treeScroll = new JScrollPane();
        treeScroll.getViewport().setLayout(new FlowLayout(FlowLayout.LEFT));
        treeScroll.getViewport().setBackground(Color.white);
        treeScroll.setMaximumSize(new Dimension(200,100));

        //infoScroll
        infoScroll = new JScrollPane();
        //infoScroll.getViewport().setLayout(new FlowLayout(FlowLayout.LEFT));
        infoScroll.getViewport().setBackground(Color.white);
        infoScroll.setMaximumSize(new Dimension(100,100));
        infoScroll.setPreferredSize(new Dimension(100,100));

        //infoPanel
        infoPanel = new JPanel();
        infoPanel.setLayout(new GridBagLayout());
        infoPanel.setBackground(Color.white);
        infoScroll.getViewport().add(infoPanel);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.gridx = 0;
        gbc.gridy = 0;

        //jumpLocationButton
        jumpLocationButton = new JButton("Jump to Gene Location");
        jumpLocationButton.addMouseListener(new MouseListener() {
            @Override
            public void mousePressed(MouseEvent e) {
                jumpToGene();
            }
            public void mouseClicked(MouseEvent e) {}
            public void mouseReleased(MouseEvent e) {}
            public void mouseEntered(MouseEvent e) {}
            public void mouseExited(MouseEvent e) {}
        });
        jumpLocationButton.setMaximumSize(new Dimension(25,200));
        jumpLocationButton.setVisible(false);
        infoPanel.add(jumpLocationButton, gbc);

        //jumpPathwayButton
        jumpPathwayButton = new JButton("Jump to Pathway");
        jumpPathwayButton.addMouseListener(new MouseListener() {
            @Override
            public void mousePressed(MouseEvent e) {
                jumpToPathway();
            }
            public void mouseClicked(MouseEvent e) {}
            public void mouseReleased(MouseEvent e) {}
            public void mouseEntered(MouseEvent e) {}
            public void mouseExited(MouseEvent e) {}
        });
        jumpPathwayButton.setMaximumSize(new Dimension(25,200));
        jumpPathwayButton.setVisible(false);
        gbc.gridy = 1;
        infoPanel.add(jumpPathwayButton, gbc);

        //linkOutButton
        linkOutButton = new JButton("Link to Web Page");
        linkOutButton.addMouseListener(new MouseListener() {
            @Override
            public void mousePressed(MouseEvent e) {
                linkOut();
            }
            public void mouseClicked(MouseEvent e) {}
            public void mouseReleased(MouseEvent e) {}
            public void mouseEntered(MouseEvent e) {}
            public void mouseExited(MouseEvent e) {}
        });
        linkOutButton.setMaximumSize(new Dimension(25,200));
        linkOutButton.setVisible(false);
        gbc.gridy = 2;
        infoPanel.add(linkOutButton, gbc);
        
        //infoLabel
        infoLabel = new JLabel();
        infoLabel.setBackground(Color.white);
        Border paddingBorder = BorderFactory.createEmptyBorder(5,5,5,5);
        infoLabel.setBorder(BorderFactory.createCompoundBorder(paddingBorder,paddingBorder));
        gbc.gridy = 3;
        infoPanel.add(infoLabel, gbc);

        //filler
        JPanel filler = new JPanel();
        gbc.gridy = 4;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        infoPanel.add(filler, gbc);
        
        //rightPanel
        rightPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, treeScroll, infoScroll);
        rightPanel.setMinimumSize(new Dimension(200,20));
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
        getGeneInfo();
    }

    private void getGeneInfo(){

        loader.setMessage("Getting gene information");

        ArrayList<DataNode> entrezNodes = new ArrayList<DataNode>();

        //determine url
        String urlString = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=gene&id=";
        for(DataNode n : dataNodes){
            String db = n.getAttribute("Xref", "Database");
            String id = n.getAttribute("Xref", "ID");
            if(db == null || id == null) continue;
            if(db.equals("Entrez Gene")){
                urlString += id + ",";
                entrezNodes.add(n);
            }
        }
        urlString += "&retmode=xml";
        if(entrezNodes.isEmpty()) return;
        
        //get xml string
        BufferedReader reader;
        String xmlString = "";
        try {
            reader = new BufferedReader(new InputStreamReader(new URL(urlString).openStream()));
            String line = reader.readLine();
            while (line != null) {
                if(!line.startsWith("<?xml") && !line.startsWith("<!DOCTYPE")){
                    xmlString += line;
                }
                line = reader.readLine();
            }
        } catch (MalformedURLException e) {
            //todo
            return;
        } catch (IOException e) {
             //todo
            return;
        }
	
        //create document
        Element root = null;
        try {
            root = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(xmlString.getBytes())).getDocumentElement();
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(Viewer.class.getName()).log(Level.SEVERE, null, ex);
            return;
        } catch (SAXException ex) {
            Logger.getLogger(Viewer.class.getName()).log(Level.SEVERE, null, ex);
            return;
        } catch (IOException ex) {
            Logger.getLogger(Viewer.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }

        //TODO: can we really make assumptions about ordering of xml?
        NodeList docSumList = root.getElementsByTagName("DocSum");
        for(int i = 0; i < docSumList.getLength(); i++){
            entrezNodes.get(i).setGeneInfo((Element)docSumList.item(i));
        }

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

        if(dataNode.hasGene()){
            jumpLocationButton.setVisible(true);
            jumpGene = dataNode.getGene();
        } else {
            jumpLocationButton.setVisible(false);
            jumpGene = null;
        }

        if(dataNode.hasWikiPathway()){
            jumpPathwayButton.setVisible(true);
            jumpPathway = dataNode.getWikiPathway();
        } else {
            jumpPathwayButton.setVisible(false);
            jumpPathway = null;
        }

        if(dataNode.hasLinkOut()){
            linkOutButton.setVisible(true);
            
        } else {
            linkOutButton.setVisible(false);
            linkOutUrl = null;
        }
        


        
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
        treeScroll.getViewport().removeAll();
        treeScroll.getViewport().add(dataTree);

       // rightPanel.setDividerLocation(0.5);
    }

    private void jumpToGene(){
        if(jumpGene == null) return;
        int startGene = jumpGene.getStart();
        int endGene = jumpGene.getEnd();
        if(startGene > endGene){
            int temp = startGene;
            startGene = endGene;
            endGene = temp;
        }
        
        //TODO: what if references don't start with "chr"?
        RangeController.getInstance().setRange("chr" + jumpGene.getChromosome(), new Range(startGene, endGene));
    }

    private void jumpToPathway(){
        
    }

    private void linkOut(){
        
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
