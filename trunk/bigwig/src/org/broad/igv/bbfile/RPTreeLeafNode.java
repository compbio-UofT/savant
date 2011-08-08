package org.broad.igv.bbfile;

import org.apache.log4j.Logger;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: martind
 * Date: Jan 14, 2010
 * Time: 3:37:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class RPTreeLeafNode implements RPTreeNode{

    private static Logger log = Logger.getLogger(RPTreeLeafNode.class);

    private long mNodeIndex;        // index for node in R+ tree organization
    private RPTreeNode m_parent;   // parent node
    private RPChromosomeRegion mChromosomeBounds;    //  bounds for entire node
    private ArrayList<RPTreeLeafNodeItem> mLeafItems;   // array for leaf items

    public RPTreeLeafNode(long nodeIndex, RPTreeNode parent){

        mNodeIndex = nodeIndex;
        m_parent = parent;
        mLeafItems = new ArrayList<RPTreeLeafNodeItem>();

        // init with null bounds
        mChromosomeBounds = new RPChromosomeRegion();
    }

     public long getNodeIndex(){
        return mNodeIndex;
    }

    public boolean isLeaf() {
        return true;
    }

    public RPChromosomeRegion getChromosomeBounds(){
         return mChromosomeBounds;
    }
    
    public int compareRegions(RPChromosomeRegion chromosomeRegion){
        
        int value = mChromosomeBounds.compareRegions(chromosomeRegion);
        return value;
    }

    public int getItemCount() {
        return mLeafItems.size();
    }

    public RPTreeNodeItem getItem(int index){

       if(index < 0 || index >= mLeafItems.size())
            return null;
       else
            return mLeafItems.get(index);
    }

    public boolean insertItem(RPTreeNodeItem item){

         RPTreeLeafNodeItem newItem =  (RPTreeLeafNodeItem)item;

        // Note: assumes all keys are inserted in rank order
        mLeafItems.add(newItem);

        // todo: compare region and insert at appropriate indexed rank location
        //   mLeafHitItem.add( index, (RPTreeLeafNodeItem)item );

        // update leaf node chromosome bounds - use extremes
        // Update node bounds or start node chromosome bounds with first entry
       if(mChromosomeBounds == null)
            mChromosomeBounds = new RPChromosomeRegion(newItem.getChromosomeBounds());
       else
            mChromosomeBounds = mChromosomeBounds.getExtremes(newItem.getChromosomeBounds());

        // successful insert
         return true;
    }

    public boolean deleteItem(int index){

        int itemCount = getItemCount();

        // unacceptable index  - reject
        if(index < 0 || index >= itemCount)
            return false;

        // delete indexed entry
        mLeafItems.remove(index);

        // successful delete
        return true;
    }

    public void printItems(){

        log.info("Leaf Node contains " +  mLeafItems.size() + " items:");

        for(int item = 0; item < mLeafItems.size(); ++item){
            mLeafItems.get(item).print();
        }
    }

}
