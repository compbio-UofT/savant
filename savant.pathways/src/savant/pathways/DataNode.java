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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author AndrewBrook
 */
public class DataNode implements Comparable {

    public static final String[] elementNames = {"Attribute", "Xref", "Graphics"};

    private String tagName;
    private Map<String,Map<String,String>> attributes = new HashMap<String,Map<String,String>>();
    private boolean geneInfoSet = false;
    private Gene gene;

    public DataNode(Element node){

        this.tagName = node.getTagName();

        NamedNodeMap nnm = node.getAttributes();
        attributes.put(tagName, new HashMap<String,String>());
        for (int j = 0; j < nnm.getLength(); j++) {
            attributes.get(tagName).put(nnm.item(j).getNodeName(), nnm.item(j).getNodeValue());
        }

        //TODO: just iterate through all children instead of using predefined list of names?
        for (int j = 0; j < elementNames.length; j++) {
            NodeList elements = node.getElementsByTagName(elementNames[j]);
            if (elements == null || elements.getLength() == 0) {
                continue;
            } else {
                attributes.put(elementNames[j], new HashMap<String,String>());
            }
            Element el = (Element) (elements.item(0));

            String currentTag = el.getTagName();
            nnm = el.getAttributes();
            for (int k = 0; k < nnm.getLength(); k++) {
                attributes.get(currentTag).put(nnm.item(k).getNodeName(), nnm.item(k).getNodeValue());
            }
        }    
    }

    public void setGeneInfo(Element node){
        //check to make sure ids match
        NodeList idList = node.getElementsByTagName("Id");
        if(!idList.item(0).getTextContent().equals(this.getAttribute("Xref", "ID"))) return;

        gene = new Gene();

        NodeList items = node.getElementsByTagName("Item");
        for(int i = 0; i < items.getLength(); i++){
            Node n = items.item(i);
            String name = ((Element)n).getAttribute("Name");
            if(name.equals("GenomicInfo")){
                NodeList subItems = ((Element)n).getElementsByTagName("Item");
                for(int j = 0; j < subItems.getLength(); j++){
                    Node m = subItems.item(j);
                    String subName = ((Element)m).getAttribute("Name");
                    if (subName.equals("ChrStart")){
                        gene.setStart(m.getTextContent());
                    } else if (subName.equals("ChrStop")){
                        gene.setEnd(m.getTextContent());
                    }
                }
            } else if (name.equals("Chromosome")){
                gene.setChromosome(n.getTextContent());
            } else if (name.equals("Name")){
                gene.setName(n.getTextContent());
            } else if (name.equals("Description")){
                gene.setDescription(n.getTextContent());
            }
        }

        geneInfoSet = true;
    }

    //print formatted info in no particular order
    public String getInfoString(){
        String s = "<HTML>";

        //gene info
        if(geneInfoSet){
            s += "<B>Entrez Gene</B><BR>";
            String name = gene.getName();
            if(name != null) s += "Name: " + name + "<BR>";
            String description = gene.getDescription();
            if(description != null) s += "Description: " + description + "<BR>";
            String chromosome = gene.getChromosome();
            if(chromosome != null) s += "Chromosome: " + chromosome + "<BR>";
            s += "Start: " + gene.getStart() + "<BR>";
            s += "End: " + gene.getEnd() + "<BR>";
            s += "<BR>";
        }

        Iterator it = attributes.keySet().iterator();
        while(it.hasNext()){
            String node = (String)it.next();
            s += "<B>" + node + "</B><BR>";
            Iterator currentIt = attributes.get(node).keySet().iterator();
            while(currentIt.hasNext()){
                String key = (String) currentIt.next();
                s += key + ": " + attributes.get(node).get(key) + "<BR>";
            }
            s += "<BR>";
        }
        s += "</HTML>";
        return s;
    }

    public String getAttribute(String subNodeName, String name){
        Map<String,String> map = attributes.get(subNodeName);
        if(map == null) return null;
        return map.get(name);
    }

    public boolean hasSubNode(String name){
        return attributes.get(name) != null;
    }

    @Override
    public int compareTo(Object o) {
        if(o.getClass() != this.getClass()) return -1;
        DataNode other = (DataNode)o;
        if(this.getType().equals(other.getType())){
            return this.getLabel().toLowerCase().compareTo(other.getLabel().toLowerCase());
        } else {
            return this.getType().compareTo(other.getType());
        }
    }

    @Override
    public String toString(){
        return this.getLabel();
    }

    public String getType(){
        return getAttribute(this.tagName, "Type");
    }

    public String getLabel(){
        return getAttribute(this.tagName, "TextLabel");
    }

    public boolean hasGene(){
        return this.geneInfoSet;
    }

    public Gene getGene(){
        return this.gene;
    }

    public boolean hasWikiPathway(){
        if(getType().equals("Pathway")){
            String db = getAttribute("Xref", "Database");
            return db != null && db.equals("WikiPathways");
        }
        return false;
    }

    public String getWikiPathway(){
        String db = getAttribute("Xref", "Database");
        if(db == null || !db.equals("WikiPathways")) return null;
        return getAttribute("Xref", "ID");
    }

    public boolean hasLinkOut(){
        //TODO
        return this.geneInfoSet;
    }

    public String getLinkOut(){
        //TODO
        //if(getAttribute(""))
        return null;
    }




}
