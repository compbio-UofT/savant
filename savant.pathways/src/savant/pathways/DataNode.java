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
import org.w3c.dom.NodeList;

/**
 *
 * @author AndrewBrook
 */
public class DataNode implements Comparable {

    public static final String[] elementNames = {"Attribute", "Xref", "Graphics"};

    private String tagName;
    private Map<String,Map<String,String>> attributes = new HashMap<String,Map<String,String>>();

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

    //print formatted info in no particular order
    public String getInfoString(){
        String s = "<HTML>";
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


    
    //CONVENIENCE FUNCTIONS /////////////////////////////

    public String getType(){
        return getAttribute(this.tagName, "Type");
    }

    public String getLabel(){
        return getAttribute(this.tagName, "TextLabel");
    }



}
