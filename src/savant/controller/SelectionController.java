/*
 *    Copyright 2010 University of Toronto
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

package savant.controller;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import savant.controller.event.SelectionChangedEvent;
import savant.controller.event.SelectionChangedListener;
import savant.data.types.Record;
import savant.util.Range;

/**
 *
 * @author AndrewBrook
 */
public class SelectionController {

    private static SelectionController instance;
    private List selectionChangedListeners;
    private Map<URI, List<Record>> map;
    private Map<URI, List<Record>> currentMap;
    private Map<URI, Range> rangeMap;

    public static synchronized SelectionController getInstance() {
        if (instance == null) {
            instance = new SelectionController();
        }
        return instance;
    }

    private SelectionController() {
        selectionChangedListeners = new ArrayList();
        map = Collections.synchronizedMap(new HashMap<URI, List<Record>>());
        currentMap = new HashMap<URI, List<Record>>();
        rangeMap = new HashMap<URI, Range>();
    }

    /**
     * Fire the SelectionChangedEvent
     */
    private synchronized void fireSelectionChangedEvent() {
        SelectionChangedEvent evt = new SelectionChangedEvent(this);
        Iterator listeners = this.selectionChangedListeners.iterator();
        while (listeners.hasNext()) {
            ((SelectionChangedListener) listeners.next()).selectionChangeReceived(evt);
        }
    }

    public synchronized void addSelectionChangedListener(SelectionChangedListener l) {
        selectionChangedListeners.add(l);
    }

    public synchronized void removeSelectionChangedListener(SelectionChangedListener l) {
        selectionChangedListeners.remove(l);
    }

    public boolean toggleSelection(URI uri, Record o){
        if(this.map.get(uri) == null) addKey(uri);
        int pos = binarySearchSelected(uri, o);
        if(pos == -1){
            this.map.get(uri).add(o);
            this.addToCurrentSelected(uri, o);
            Collections.sort(this.map.get(uri));
        } else {
            this.map.get(uri).remove(o);
            this.removeFromCurrentSelected(uri, o);
        }
        this.fireSelectionChangedEvent();
        return (pos == -1);
    }

    //this forces element to be added (never removed) and fires event
    public void addSelection(URI uri, Record o){
        addSelectionWithoutEvent(uri, o);
        this.fireSelectionChangedEvent();
    }

    /*
     * This forces element to be added (never removed) and does not fire event.
     * Should be used when adding a large amount of selection, but event must be
     * called when done.
     */
    private void addSelectionWithoutEvent(URI uri, Record o) {
        if(this.map.get(uri) == null) addKey(uri);
        int pos = binarySearchSelected(uri, o);
        if(pos == -1){
            this.map.get(uri).add(o);
            this.addToCurrentSelected(uri, o);
        }
    }

    public void removeSelection(URI uri, Record o){
        map.get(uri).remove(o);
        this.removeFromCurrentSelected(uri, o);
        this.fireSelectionChangedEvent();
    }

    public void removeAll(URI uri){
        if(map.get(uri) == null) return;
        map.get(uri).clear();
        currentMap.get(uri).clear();
        rangeMap.remove(uri);
        this.fireSelectionChangedEvent();
    }

    public void removeAll(){
        map.clear();
        currentMap.clear();
        rangeMap.clear();
        this.fireSelectionChangedEvent();
    }

    public List<Record> getSelections(URI uri){
        if(map.get(uri) == null) return null;
        return Collections.unmodifiableList(map.get(uri));
    }

    public boolean hasSelected(URI uri){
        return map.get(uri) != null && !map.get(uri).isEmpty();
    }

    private int binarySearchSelected(URI uri, Comparable key){

        List<Record> selected = this.map.get(uri);
        if(selected == null) return -1;

        int start = 0;
        int end = selected.size();

        while (start < end) {
            int mid = (start + end) / 2;
            int compare = key.compareTo((Comparable) (selected.get(mid)));
            if(compare < 0){
                end = mid;
            } else if(compare > 0){
                start = mid + 1;
            } else {
                return mid;
            }
        }
        return -1;    // Failed to find key
    }

   private List<Record> getSelectedFromList(URI uri, List<Record> data){

       //TODO: change so that we perform binary search of
       //larger list for each element of smaller list. Cannot do this currently
       //because data may not be sorted as Comparable expects.

       List<Record> list = new ArrayList<Record>();
       if (map.get(uri) == null || map.get(uri).isEmpty()) return list;

       for(int i = 0; i < data.size(); i++){
           Record o = data.get(i);
           int pos = binarySearchSelected(uri, o);
           if(pos != -1) list.add(o);
       }
       
       return list;
   }

   /*
    * This function is equivalent to getSelectedFromList(URI uri, List<Record> data)
    * but it uses range to check whether anything has changed since the last
    * request. If nothing has changed, return the same list. 
    */
   public List getSelectedFromList(URI uri, Range range, List<Record> data){
       
       if(range.equals(rangeMap.get(uri)) && currentMap.get(uri) != null){
           return currentMap.get(uri);
       }

       rangeMap.put(uri, range);
       List<Record> currentSelected = this.getSelectedFromList(uri, data);
       currentMap.put(uri, currentSelected);

       return currentSelected;

   }

   public Map<URI, List<Record>> getAllSelected(){
       return map;
   }

   private void addToCurrentSelected(URI uri, Record o){
       if (currentMap.get(uri) == null) currentMap.put(uri, new ArrayList<Record>());
       currentMap.get(uri).add(o);
   }

   private void removeFromCurrentSelected(URI uri, Record o){
       if (currentMap.get(uri) == null) currentMap.put(uri, new ArrayList<Record>());
       currentMap.get(uri).remove(o);
   }

   private void addKey(URI uri){
       map.put(uri, new ArrayList<Record>());
   }

   public void addMultipleSelections(URI uri, List<Record> list){
       for(int i = 0; i < list.size(); i++){
           addSelectionWithoutEvent(uri, list.get(i));
       }
       if (map.get(uri) == null) addKey(uri);
       Collections.sort(map.get(uri));
       this.fireSelectionChangedEvent();
   }
}
