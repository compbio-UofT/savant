/**
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package savant.geneontology;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 * @author Nirvana Nursimulu
 */
public class XTree {
    
    /**
     * Number of distinct parents that are shared.
     */
    public static List<String> parents = new ArrayList<String>();
    
    /**
     * Fake root of the tree.  Useful since this is actually a forest of trees.
     */
    XNode fakeRoot;    
    
    /**
     * Dictionary from an identifier to a node.
     */
    HashMap<String, XNode> identifierToNode;
    
    /**
     * Map from GO ID to locations
     */
    HashMap<String, ArrayList<ArrayList<String>>> goToLocs;
    
    /**
     * The file containing genome locations.
     */
    public String fileGenLocation;
    
    /**
     * Constructor
     * @param fileGenLocation the location of the file mapping GO IDs to 
     * genome locations.
     */
    public XTree(String fileGenLocation) throws Exception{
        
        this.fileGenLocation = fileGenLocation;
        parents = new ArrayList<String>();
        fakeRoot = new XNode("ROOT");
        identifierToNode = new HashMap<String, XNode>();
        identifierToNode.put("ROOT", fakeRoot);
        
        // Map GO ID to locations on genome.
        goToLocs = mapIDtoLocations(fileGenLocation);
        
        // Make map of GO ids to RefSeq ids and genome locations.
//        makeMapRefSeqToLoc(goToLocs);
    }
    
    
    public int getSize(){
        return identifierToNode.keySet().size();
    }
    
    /**
     * Get the root nodes of this tree.
     * @return the actual roots of this forest (XNode objects).
     */
    public Set<XNode> getRootNodes(){
        
        return fakeRoot.getChildren();
    }
    
    /**
     * Get the children of the node with the provided identifier.
     * @param identifier the identifier of the parent.
     * @return the TreeSet of children nodes of this parent.
     */
    public TreeSet<XNode> getChildrenNodes(String identifier){
        
        return identifierToNode.get(identifier).getChildren();
    }
    
    /**
     * Mark the locations of this node.
     * @param node the node whose locations are to be marked.
     */
    private void markLocations(XNode node){
        
        // If the locations have already been marked, do not do anything.
        if (node.getLocs() != null){
            return;
        }
        ArrayList<ArrayList<String>> locs = 
                goToLocs.get(node.getIdentifier().replace(':', '_'));
        if (locs == null){
            
            locs = new ArrayList< ArrayList<String> >();
        }
        node.setLocs(locs);
    }
    
    /**
     * Add this "node" to the tree.  Assumes that this method is called only 
     * once for each child node.
     * @param child the child in this relationship.
     * @param parentID the parent's identifier in this relationship.
     */
    public void addNode(XNode child, List<String> parentIDs){
        
        // First of all, get the child node if it already exists from the 
        // dictionary.
        // If the child node does not already exist, add to the list of nodes
        // to watch out for.  Note that the information in this node should
        // be complete since this node is being encountered as a child.
        XNode childref = identifierToNode.get(child.getIdentifier());
        
        if (childref != null){
            
            child.copyInfoExceptChildrenTo(childref);
        }
        else{
            
            // Put into dictionary if node not seen yet.
            childref = child;
            identifierToNode.put(childref.getIdentifier(), childref);            
        }
        
        markLocations(childref);
        
        // For each parent, mark this as their child.
        for (String parentID: parentIDs){
        
            // If the parent has already been encountered, retrieve the record.
            // Otherwise, create a record in the dictionary.
            XNode parentRef = identifierToNode.get(parentID);
            if (parentRef == null){
                
                parentRef = new XNode(parentID);
                identifierToNode.put(parentID, parentRef);
            }
            
            // Mark the child.  This is the easy part.
            parentRef.addChild(childref);
            
            if (parentIDs.size() > 1){
                parents.add(parentID);
            }
                        
        }

    }
    
    /**
     * Add a (true) root to this tree.
     * @param root 
     */
    public void addRoot(XNode root){
        
        // See if we already have this root in a map.  If so, use it; otherwise,
        // put the root into the dictionary.
        XNode rootRef = identifierToNode.get(root.getIdentifier());
        if (rootRef == null){
            
            rootRef = root;
            identifierToNode.put(rootRef.getIdentifier(), rootRef);
        }
        else{
            root.copyInfoExceptChildrenTo(rootRef);
            root = rootRef;
        }
        
        // Add this node as a child to the fake root.
        fakeRoot.addChild(root);
        
        // Mark the location of this node.
        markLocations(root);
    }
    
        /**
     * Map GO ID to locations.
     * @param filename the name of the file containing the information.
     * @return a map of GO ID to Uniprot ID. 
     */
    public static HashMap<String, ArrayList<ArrayList<String>>> 
            mapIDtoLocations(String filename) throws Exception{
        
        HashMap<String, ArrayList<ArrayList<String>>> idToLocations = 
                new HashMap<String, ArrayList<ArrayList<String>>>();
        
        String line;
        
        // Get file.
        // File is assumed to have on each line, "uniprotID\tX" where X may be
        // a GO ID.
        File file = new File(filename);
        
            
        FileReader fileReader = new FileReader(file);
        BufferedReader buffer = new BufferedReader(fileReader);


        // While there is still something to be read...
        while ((line = buffer.readLine()) != null){

            String[] split = line.split("\t");

            // Get the key
            String key = split[0];
            key = key.replace(':', '_');

            // Get the value
            ArrayList<ArrayList<String>> value = idToLocations.get(key);

            // If the key has not been put yet, create a map.
            if (value == null){

                value = new ArrayList<ArrayList<String>>();
                idToLocations.put(key, value);
            }

            ArrayList<String> location = new ArrayList<String>();

            for (int i = 1; i < split.length; i++){

                location.add(split[i]);
            }
            value.add(location);
        }

        fileReader.close();
        buffer.close();

        return idToLocations;
    }
    
    /**
     * Use this for debugging?
     * @param dict the map of GO IDs to locations.
     */
    public static void makeMapRefSeqToLoc
            (HashMap<String, ArrayList<ArrayList<String>>> dict){
        
        // Create a map from the refseq ID to locations.
        HashMap<String, HashSet<ArrayList <String>> > map = 
                new HashMap<String, HashSet<ArrayList <String>> >();
        
        // Outer array.
        for (ArrayList<ArrayList<String>> array: dict.values()){
            
            // Inner array.
            for (ArrayList<String> innerArray: array){
                
                String key = innerArray.get(0);
                
                HashSet<ArrayList <String>> set = map.get(key);
                
                if (set == null){
                
                    set = new HashSet<ArrayList <String>>();
                    map.put(key, set);
                }
                
                ArrayList<String> newArray = new ArrayList<String>();
                newArray.add(innerArray.get(1));
                newArray.add(innerArray.get(2));
                newArray.add(innerArray.get(3));
                
                set.add(newArray);
            }
        }
        
    }

}
