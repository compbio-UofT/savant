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

import java.util.ArrayList;
import java.util.TreeSet;

/**
 *
 * @author Nirvana Nursimulu
 */
public class XNode implements Comparable{
    
        /**
     * String that is to be used at the beginning of the URL of any node.
     */
    public static final String beginURL = 
            "http://amigo.geneontology.org/cgi-bin/amigo/term-details.cgi?term=";
    
    /**
     * Child of this node.
     */
    private TreeSet<XNode> children;
    
    /**
     * Identifier of this node. (Acts as some kind of key)
     */
    private String identifier;
    
    /**
     * Description of this node.
     */
    private String description;
    
    /**
     * The URL associated with this node.
     */
    private String url;
    
    /**
     * Keep track of the locations associated with this node, if any.
     */
    private ArrayList<ArrayList<String>> locs;    
    
    /**
     * Useful for the GUI implementation; says if this node has already been 
     * selected earlier.
     */
    private boolean hasBeenSelected;
    
    /**
     * Do we include the identifier of this node in its toString description?
     */
    private boolean includeIdentifierInDescription;
    
    
    /**
     * Constructor of this node given an identifier.
     * @param identifier  "key" of this node.
     */
    public XNode(String identifier, boolean includeIdentifierInDescription){
        
        this.children = new TreeSet<XNode>();
        this.identifier = identifier;
        this.url = getURL(identifier);
        this.description = null;
        this.locs = null;
        this.hasBeenSelected = false;
        this.includeIdentifierInDescription = includeIdentifierInDescription;
    }
    
    /**
     * By default, include identifier in description.
     * @param identifier 
     */
    public XNode(String identifier){
        this(identifier, true);
    }
    
    
    public void setLocs(ArrayList<ArrayList<String>> locs){
        
        this.locs = locs;
    }
    
    public ArrayList<ArrayList<String>> getLocs(){
        
        return this.locs;
    }
    
        
    /**
     * Adds a child to this node.
     * @param child 
     */
    public void addChild(XNode child){
        
        this.children.add(child);
    }
    
    
    /**
     * Removes a certain child.
     * @param child the child to be removed.
     * @return true iff the child was removed.
     */
    public boolean removeChild(XNode child){
        
        // If this is not the root node, this operation is not enabled.
        if (!this.getIdentifier().equals("ROOT")){
            
            return false;
        }
        else{
            
            return this.children.remove(child);
        }
    }
    
    /**
     * Returns the child of this node.
     * @return 
     */
    public TreeSet<XNode> getChildren(){
        
        return this.children;
    }
    
    /**
     * Copy info from this node into the designated node 
     * (except for children info)
     * @param node node to copy info to. 
     */
    public void copyInfoExceptChildrenTo(XNode node){
        
        node.description = this.description;
        node.identifier = this.identifier;
        node.locs = this.locs;
        node.url = this.url;
    }
    
    /**
     * Get the identifier of this node.
     * @return the identifier of this node.
     */
    public String getIdentifier(){
        
        return this.identifier;
    }
    
    /**
     * Sets the identifier of this node.
     * @param identifier 
     */
    public void setIdentifier(String identifier){
        
        this.identifier = identifier;
    }
    
    /**
     * Set the description of this node.
     * @param description 
     */
    public void setDescription(String description){
        
        this.description = description;
    }
    
    /**
     * Get the description of this node.
     * @return description.
     */
    public String getDescription(){
        
        return this.description;
    }
    
    @Override
    public String toString(){
        if (this.includeIdentifierInDescription){
            return this.description + " [" + this.identifier + "]";
        }
        else{
            return this.description;
        }
    }
    
    /**
     * Return the URL of a node given an identifier of the node.
     * @param iden the identifier of the node
     * @return the URL of the node.
     */
    private static String getURL(String iden){
        
        // Use the ID to get the url.
        String thisurl = beginURL + iden;
        
        return thisurl;
    }
    
    public void setURL(String url){
        
        this.url = url;
    }
    
    /**
     * Return the url associated with this node.
     * @return 
     */
    public String getURL(){
        
        return url;
    }

    
    /**
     * Select this node; useful in the GUI implementation
     */
    public void select(){
        
        this.hasBeenSelected = true;
    }
    
    /**
     * Says if this node has been selected.
     * @return true iff this node has been selected.
     */
    public boolean isSelected(){
        
        return this.hasBeenSelected;
    }

    @Override
    /**
     * How does this node compare to this other object, which is potentially a 
     * node.
     */
    public int compareTo(Object o) {
        
        try{
            XNode node = (XNode)o;
            return this.description.compareTo(node.description);
        }
        catch(Exception e){
            
            return 0;
        }
    }
    
}
