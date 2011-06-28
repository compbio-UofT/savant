package org.broad.igv.bbfile;

import org.apache.log4j.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: martind
 * Date: Jan 6, 2010
 * Time: 4:35:42 PM
 * To change this template use File | Settings | File Templates.
 */

/*
    Container class for R+ Tree Child format
 */
public class RPTreeChildNodeItem implements RPTreeNodeItem {

    private static Logger log = Logger.getLogger(RPTreeChildNodeItem.class);
    private final boolean mIsLeafItem = false;
    private long mItemIndex;       // child node item index for B+ tree child node

    // R+ child (non-leaf) node item entries: BBFile Table N
    private RPChromosomeRegion mChromosomeBounds; // chromosome bounds for item
    private RPTreeNode mChildNode;  // child node assigned to node item

    /*  Constructor for child node items.
    *
    *   Parameters:
    *       itemIndex - index of item belonging to a child node
    *       startChromID - starting chromosome/contig for item
    *       startBase - starting base for item
    *       endChromID - ending chromosome/contig for item
    *       endBase - ending base for item
    *       childNode - child node item assigned to child node
    *
    * */
    public RPTreeChildNodeItem(long itemIndex, int startChromID, int startBase,
                               int endChromID, int endBase, RPTreeNode childNode){

        mItemIndex = itemIndex;
        mChromosomeBounds = new RPChromosomeRegion(startChromID, startBase, endChromID, endBase);
        mChildNode = childNode;
    }

    public long getItemIndex() {
           return mItemIndex;
       }

    public boolean isLeafItem(){
        return mIsLeafItem;
    }

    public RPChromosomeRegion getChromosomeBounds() {
        return mChromosomeBounds;
    }

    public RPTreeNode getChildNode() {
        return mChildNode;
    }

    public int compareRegions(RPChromosomeRegion chromosomeRegion){

        int value = mChromosomeBounds.compareRegions(chromosomeRegion);
        return value;
    }

    public void print(){

        log.info("Child node item " + mItemIndex + ":\n");
        log.info(" StartChromID = " + mChromosomeBounds.getStartChromID() + "\n");
        log.info(" StartBase = " + mChromosomeBounds.getStartBase() + "\n");
        log.info(" EndChromID = " + mChromosomeBounds.getEndChromID() + "\n");
        log.info(" EndBase = " + mChromosomeBounds.getEndBase() + "\n");

        // child node specific entries
        mChildNode.printItems();
    }

}

