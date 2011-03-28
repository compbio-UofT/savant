
/**
 * Created by IntelliJ IDEA.
 * User: martind
 * Date: Dec 20, 2009
 * Time: 10:37:43 PM
 * To change this template use File | Settings | File Templates.
 */
 package org.broad.igv.bbfile;

import org.apache.log4j.Logger;

/*
 *   Container class for B+ tree leaf node.
* */
public class BPTreeLeafNodeItem implements BPTreeNodeItem {

    private static Logger log = Logger.getLogger(BPTreeLeafNodeItem.class);
    private final boolean mIsLeafItem = true;
    private long mLeafIndex;    // leaf index in B+ tree item list

    // B+ Tree Leaf Node Item entities - BBFile Table G
    private String mChromKey; // B+ tree node item is associated by key
    private int mChromID;      // numeric mChromosome/contig ID
    private int mChromSize;    // number of bases in mChromosome/contig

    /*
    *   Constructs a B+ tree leaf node item with the supplied information.
    *
    *   Parameters:
    *       leafIndex - leaf item index
    *       chromKey - chromosome/contig name key
    *       chromID - chromosome ID assigned to the chromosome name key
    *       chromsize - number of bases in the chromosome/contig
    * */
    public BPTreeLeafNodeItem(long leafIndex, String chromKey, int chromID, int chromSize){

        mLeafIndex = leafIndex;
        mChromKey = chromKey;
        mChromID = chromID;
        mChromSize = chromSize;
    }

    /*
    *   Method returns the index assigned to this node item.
    *
    *   Returns:
    *       index assigned to this node item
    * */
    public long getItemIndex(){
        return mLeafIndex;
    }

    /*
    *   Method returns if this node is a leaf item.
    *
    *   Returns:
    *       true because node is a leaf item
    * */
    public boolean isLeafItem() {
        return mIsLeafItem;
    }

    /*
    *   Method returns the chromosome name key  assigned to this node item.
    *
    *   Returns:
    *       chromosome name key assigned to this node item
    * */
    public String getChromKey() {
        return mChromKey;
    }

   /*
    *   Method compares supplied chromosome key with leaf node key.
    *
    *   Parameters:
    *       chromKey - chromosome name ley to compare
    *
    *   Returns:
    *       true, if keys are equal; false if keys are different
    * */
    public boolean chromKeysMatch(String chromKey) {
        String thisKey = mChromKey;
        String thatKey = chromKey;

        // Note: must have the same length to compare chromosome names
        int thisKeyLength = thisKey.length();
        int thatKeyLength = thatKey.length();

        // check if need to truncate the larger string
        if(thisKeyLength > thatKeyLength)
            thisKey = thisKey.substring(0,thatKeyLength);
        else if(thatKeyLength > thisKeyLength)
            thatKey = thatKey.substring(0,thisKeyLength);

        if (thisKey.compareTo(thatKey) == 0)
            return true;
        else
            return false;
   }

    public void print() {

       log.info("B+ tree leaf node item number " + mLeafIndex);
       log.info("Key value = " + mChromKey);
       log.info("ChromID = " + mChromID);
       log.info("Chromsize = " + mChromSize);
   }

    // *** BPTreeLeafNodeItem specific methods ***
    public int getChromID() {
       return mChromID;
   }

   public int getChromSize() {
       return mChromSize;
   }

}
