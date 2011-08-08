package org.broad.igv.bbfile;

import org.apache.log4j.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: martind
 * Date: Dec 20, 2009
 * Time: 11:21:49 PM
 * To change this template use File | Settings | File Templates.
 */
/*
    Container class for R+ tree leaf node data locator.
*
*   Note: Determination of data item as  BigWig data or BigBed data
*           depends on whether the file is BigWig of Table J format
*           or BigBed of Tble I format.
 */
public class RPTreeLeafNodeItem implements RPTreeNodeItem {

    private static Logger log = Logger.getLogger(RPTreeLeafNodeItem.class);
    private final boolean mIsLeafItem = true;
    private long mItemIndex;       // leaf item index in R+ tree leaf item list

    // R+ tree leaf node item entries: BBFile Table M
    private RPChromosomeRegion mChromosomeBounds; // chromosome bounds for item
    private long mDataOffset;      // file offset to data item
    private long mDataSize;        // size of data item

    /*  Constructor for leaf node items.
    *
    *   Parameters:
    *       itemIndex - index of item belonging to a leaf node
    *       startChromID - starting chromosome/contig for item
    *       startBase - starting base for item
    *       endChromID - ending chromosome/contig for item
    *       endBase - ending base for item
    *       dataOffset - file location for leaf chromosome/contig data
    *       dataSize - size of (compressed) leaf data region in bytes
    *
    * */
    public RPTreeLeafNodeItem(long itemIndex, int startChromID,  int startBase,
            int endChromID, int endBase, long dataOffset, long dataSize){

        mItemIndex = itemIndex;
        mChromosomeBounds = new RPChromosomeRegion(startChromID, startBase, endChromID, endBase);
        mDataOffset = dataOffset;
        mDataSize = dataSize;
    }

    // *** RPTreeNodeItem interface implementation  ***
    public long getItemIndex() {
           return mItemIndex;
       }

    public boolean isLeafItem(){
           return mIsLeafItem;
       }

    public RPChromosomeRegion getChromosomeBounds() {
        return mChromosomeBounds;
    }

    public int compareRegions(RPChromosomeRegion chromosomeRegion){

        // test leaf item bounds for hit
        int value = mChromosomeBounds.compareRegions(chromosomeRegion);
        return value;
    }

    public void print(){

       log.info("R+ tree leaf node data item " + mItemIndex);
       log.info("StartChromID = " + mChromosomeBounds.getStartChromID());
       log.info("StartBase = " + mChromosomeBounds.getStartBase());
       log.info("EndChromID = " + mChromosomeBounds.getEndChromID());
       log.info("EndBase = " +  mChromosomeBounds.getEndBase());

       // leaf node specific entries
       log.info("DataOffset = " +  mDataOffset);
       log.info("DataSize = " + mDataSize);
    }

    // *** RPTreeLeafNodeItem specific methods ***
     public long getDataOffset() {
        return mDataOffset;
    }

    public long geDataSize() {
        return mDataSize;
    }

}
