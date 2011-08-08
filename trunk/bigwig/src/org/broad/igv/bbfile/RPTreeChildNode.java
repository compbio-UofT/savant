package org.broad.igv.bbfile;

import org.apache.log4j.Logger;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: martind
 * Date: Dec 20, 2009
 * Time: 11:14:49 PM
 * To change this template use File | Settings | File Templates.
 */

/**
 *
 *   Container class for R+ tree leaf or child node format
  *  Note: RPTreeNode interface supports leaf and child node formats
 */
public class RPTreeChildNode implements RPTreeNode{

    private static Logger log = Logger.getLogger(RPTreeChildNode.class);

    private long mNodeIndex;        // index for node in R+ tree organization
    private RPTreeNode m_parent;    // parent node
     private RPChromosomeRegion mChromosomeBounds;  // chromosome bounds for entire node
    private ArrayList<RPTreeChildNodeItem> mChildItems; // array for child items

    public RPTreeChildNode(long nodeIndex, RPTreeNode parent){

        mNodeIndex = nodeIndex;
        m_parent = parent;
        mChildItems = new ArrayList<RPTreeChildNodeItem>();

        // Note: Chromosome bounds are null until a valid region is specified
    }

     // *** BPTreeNode interface implementation ***
     public long getNodeIndex(){
        return mNodeIndex;
    }

    public RPChromosomeRegion getChromosomeBounds(){
         return mChromosomeBounds;
    }

    public int compareRegions(RPChromosomeRegion chromosomeRegion){

        // test leaf item bounds for hit
        int value = mChromosomeBounds.compareRegions(chromosomeRegion);
        return value;
    }
    
    public boolean isLeaf() {
        return false;
    }

    public int getItemCount() {
        return mChildItems.size();
    }

    public RPTreeNodeItem getItem(int index){

       if(index < 0 || index >= mChildItems.size())
            return null;
       else{
            RPTreeChildNodeItem item = mChildItems.get(index);
            return (RPTreeNodeItem) item;
       }
    }

   public boolean insertItem(RPTreeNodeItem item){

       RPTreeChildNodeItem newItem =  (RPTreeChildNodeItem)item;

       // Quick implementation: assumes all keys are inserted in rank order
       // todo: or compare key and insert at rank location
       mChildItems.add(newItem );

       // Update node bounds or start node chromosome bounds with first entry
       if(mChromosomeBounds == null)
            mChromosomeBounds = new RPChromosomeRegion(newItem.getChromosomeBounds());
       else
            mChromosomeBounds = mChromosomeBounds.getExtremes(newItem.getChromosomeBounds());

       // success
       return true;
   }

    public boolean deleteItem(int index){

        int itemCount = getItemCount();

        // unacceptable index  - reject
        if(index < 0 || index >= itemCount)
            return false;

        // delete indexed entry
        mChildItems.remove(index);

        // successful delete
        return true;
    }

    public void printItems(){

        log.info("Child node " + mNodeIndex + " contains "
                + mChildItems.size() + " items:");

        for(int item = 0; item < mChildItems.size(); ++item){
            mChildItems.get(item).print();
        }
    }


}
