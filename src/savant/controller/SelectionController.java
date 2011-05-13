/*
 *    Copyright 2010-2011 University of Toronto
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
    private List<SelectionChangedListener> selectionChangedListeners;
    private Map<String, List<Record>> map;
    private Map<String, List<Record>> currentMap;
    private Map<String, Range> rangeMap;

    public static synchronized SelectionController getInstance() {
        if (instance == null) {
            instance = new SelectionController();
        }
        return instance;
    }

    private SelectionController() {
        selectionChangedListeners = new ArrayList();
        map = Collections.synchronizedMap(new HashMap<String, List<Record>>());
        currentMap = new HashMap<String, List<Record>>();
        rangeMap = new HashMap<String, Range>();
    }

    /**
     * Fire the SelectionChangedEvent
     */
    private synchronized void fireSelectionChangedEvent() {
        SelectionChangedEvent evt = new SelectionChangedEvent(this);
        for (SelectionChangedListener l: selectionChangedListeners) {
            l.selectionChanged(evt);
        }
    }

    public synchronized void addSelectionChangedListener(SelectionChangedListener l) {
        selectionChangedListeners.add(l);
    }

    public synchronized void removeSelectionChangedListener(SelectionChangedListener l) {
        selectionChangedListeners.remove(l);
    }

    public boolean toggleSelection(String name, Record o){
        if(this.map.get(name) == null) addKey(name);
        int pos = binarySearchSelected(name, o);
        if(pos == -1){
            this.map.get(name).add(o);
            this.addToCurrentSelected(name, o);
            Collections.sort(this.map.get(name));
        } else {
            this.map.get(name).remove(o);
            this.removeFromCurrentSelected(name, o);
        }
        this.fireSelectionChangedEvent();
        return (pos == -1);
    }

    //this forces element to be added (never removed) and fires event
    public void addSelection(String name, Record o){
        addSelectionWithoutEvent(name, o);
        this.fireSelectionChangedEvent();
    }

    /*
     * This forces element to be added (never removed) and does not fire event.
     * Should be used when adding a large amount of selection, but event must be
     * called when done.
     */
    private void addSelectionWithoutEvent(String name, Record o) {
        if(this.map.get(name) == null) addKey(name);
        int pos = binarySearchSelected(name, o);
        if(pos == -1){
            this.map.get(name).add(o);
            this.addToCurrentSelected(name, o);
        }
    }

    public void removeSelection(String name, Record o){
        map.get(name).remove(o);
        this.removeFromCurrentSelected(name, o);
        this.fireSelectionChangedEvent();
    }

    public void removeAll(String name){
        if(map.get(name) == null) return;
        map.get(name).clear();
        currentMap.get(name).clear();
        rangeMap.remove(name);
        this.fireSelectionChangedEvent();
    }

    public void removeAll(){
        map.clear();
        currentMap.clear();
        rangeMap.clear();
        this.fireSelectionChangedEvent();
    }

    public List<Record> getSelections(String name){
        if(map.get(name) == null) return null;
        return Collections.unmodifiableList(map.get(name));
    }

    public boolean hasSelected(String name){
        return map.get(name) != null && !map.get(name).isEmpty();
    }

    private int binarySearchSelected(String name, Comparable key){

        List<Record> selected = this.map.get(name);
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

   private List<Record> getSelectedFromList(String name, List<Record> data){

       //TODO: change so that we perform binary search of
       //larger list for each element of smaller list. Cannot do this currently
       //because data may not be sorted as Comparable expects.

       List<Record> list = new ArrayList<Record>();
       if (map.get(name) == null || map.get(name).isEmpty()) return list;

       for(int i = 0; i < data.size(); i++){
           Record o = data.get(i);
           int pos = binarySearchSelected(name, o);
           if(pos != -1) list.add(o);
       }
       
       return list;
   }

   /*
    * This function is equivalent to getSelectedFromList(URI uri, List<Record> data)
    * but it uses range to check whether anything has changed since the last
    * request. If nothing has changed, return the same list. 
    */
   public List getSelectedFromList(String name, Range range, List<Record> data){
       
       if(range.equals(rangeMap.get(name)) && currentMap.get(name) != null){
           return currentMap.get(name);
       }

       rangeMap.put(name, range);
       List<Record> currentSelected = this.getSelectedFromList(name, data);
       currentMap.put(name, currentSelected);

       return currentSelected;

   }

   public Map<String, List<Record>> getAllSelected(){
       return map;
   }

   private void addToCurrentSelected(String name, Record o){
       if (currentMap.get(name) == null) currentMap.put(name, new ArrayList<Record>());
       currentMap.get(name).add(o);
   }

   private void removeFromCurrentSelected(String name, Record o){
       if (currentMap.get(name) == null) currentMap.put(name, new ArrayList<Record>());
       currentMap.get(name).remove(o);
   }

   private void addKey(String name){
       map.put(name, new ArrayList<Record>());
   }

   public void addMultipleSelections(String name, List<Record> list){
       for(int i = 0; i < list.size(); i++){
           addSelectionWithoutEvent(name, list.get(i));
       }
       if (map.get(name) == null) addKey(name);
       Collections.sort(map.get(name));
       this.fireSelectionChangedEvent();
   }
}
