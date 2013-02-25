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

package savant.selection;

import savant.api.event.SelectionChangedEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import savant.api.data.Record;
import savant.util.Controller;
import savant.util.Range;

/**
 *
 * @author AndrewBrook
 */
public class SelectionController extends Controller<SelectionChangedEvent> {

    private static SelectionController instance;
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
        map = Collections.synchronizedMap(new HashMap<String, List<Record>>());
        currentMap = new HashMap<String, List<Record>>();
        rangeMap = new HashMap<String, Range>();
    }

    public boolean toggleSelection(String name, Record o) {
        ensureKey(name);
        int pos = binarySearchSelected(name, o);
        if (pos == -1) {
            map.get(name).add(o);
            addToCurrentSelected(name, o);
            Collections.sort(map.get(name));
        } else {
            map.get(name).remove(o);
            removeFromCurrentSelected(name, o);
        }
        fireEvent(new SelectionChangedEvent());
        return pos == -1;
    }

    //this forces element to be added (never removed) and fires event
    public void addSelection(String name, Record o) {
        addSelectionWithoutEvent(name, o);
        fireEvent(new SelectionChangedEvent());
    }

    /*
     * This forces element to be added (never removed) and does not fire event.
     * Should be used when adding a large amount of selection, but event must be
     * called when done.
     */
    private void addSelectionWithoutEvent(String name, Record o) {
        ensureKey(name);
        int pos = binarySearchSelected(name, o);
        if (pos == -1) {
            map.get(name).add(o);
            Collections.sort(map.get(name));
            addToCurrentSelected(name, o);
        }
    }
    
    public void removeSelectionWithoutEvent(String name, Record o) {
        map.get(name).remove(o);
        removeFromCurrentSelected(name, o);
    }

    public void removeSelection(String name, Record o) {
        removeSelectionWithoutEvent(name, o);
        fireEvent(new SelectionChangedEvent());
    }

    public void removeAll(String name) {
        if (map.containsKey(name)) {
            map.get(name).clear();
            currentMap.get(name).clear();
            rangeMap.remove(name);
            fireEvent(new SelectionChangedEvent());
        }
    }

    public void removeAll() {
        map.clear();
        currentMap.clear();
        rangeMap.clear();
        fireEvent(new SelectionChangedEvent());
    }

    public List<Record> getSelections(String name) {
        if (map.get(name) == null) return null;
        return Collections.unmodifiableList(map.get(name));
    }

    public boolean hasSelected(String name) {
        return map.containsKey(name) && !map.get(name).isEmpty();
    }

    private int binarySearchSelected(String name, Comparable key) {

        List<Record> selected = map.get(name);
        if (selected == null) return -1;

        int start = 0;
        int end = selected.size();

        while (start < end) {
            int mid = (start + end) / 2;
            int compare = key.compareTo((Comparable) (selected.get(mid)));
            if (compare < 0) {
                end = mid;
            } else if (compare > 0) {
                start = mid + 1;
            } else {
                return mid;
            }
        }
        return -1;    // Failed to find key
    }

    private List<Record> getSelectedFromList(String name, List<Record> data) {

       //TODO: change so that we perform binary search of
       //larger list for each element of smaller list. Cannot do this currently
       //because data may not be sorted as Comparable expects.

       List<Record> list = new ArrayList<Record>();
       if (hasSelected(name)) {

           for(int i = 0; i < data.size(); i++) {
               Record o = data.get(i);
               int pos = binarySearchSelected(name, o);
               if (pos != -1) list.add(o);
           }
       }
       
       return list;
   }

    /**
    * This function is equivalent to getSelectedFromList(URI uri, List<Record> data)
    * but it uses range to check whether anything has changed since the last
    * request. If nothing has changed, return the same list. 
    */
    public List<Record> getSelectedFromList(String name, Range range, List<Record> data) {
       
       if (range.equals(rangeMap.get(name)) && currentMap.get(name) != null) {
           return currentMap.get(name);
       }

       rangeMap.put(name, range);
       List<Record> currentSelected = getSelectedFromList(name, data);
       currentMap.put(name, currentSelected);

       return currentSelected;

   }

   private void addToCurrentSelected(String name, Record o) {
       if (!currentMap.containsKey(name)) {
           currentMap.put(name, new ArrayList<Record>());
       }
       currentMap.get(name).add(o);
       rangeMap.remove(name);
   }

   private void removeFromCurrentSelected(String name, Record o) {
       if (currentMap.containsKey(name)) {
           currentMap.get(name).remove(o);
           rangeMap.remove(name);
       }
   }

    private void ensureKey(String name) {
        if (!map.containsKey(name)) {
            map.put(name, new ArrayList<Record>());
        }
    }

   public void addMultipleSelections(String name, List<Record> list) {
       for(int i = 0; i < list.size(); i++) {
           addSelectionWithoutEvent(name, list.get(i));
       }
       ensureKey(name);
       Collections.sort(map.get(name));
       fireEvent(new SelectionChangedEvent());
   }
   
   /*
    * If any of group is not selected, select all. 
    * Otherwise unselect all. 
    */
   public void toggleGroup(String name, List<Record> group) {
       ensureKey(name);
       
       boolean notSelected = false;
       for (Record r : group) {
           if (binarySearchSelected(name, r) == -1) {
               notSelected = true;
               break;
           }
       }
       
       if (notSelected) { //select all
           for(Record r : group) addSelectionWithoutEvent(name, r);
       } else {         //deselect all
           for(Record r : group) removeSelectionWithoutEvent(name, r);
       }
       
       fireEvent(new SelectionChangedEvent());
   }
}
