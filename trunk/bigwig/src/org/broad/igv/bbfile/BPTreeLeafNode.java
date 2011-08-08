package org.broad.igv.bbfile;

import org.apache.log4j.Logger;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: martind
 * Date: Dec 17, 2009
 * Time: 12:28:30 PM
 * To change this template use File | Settings | File Templates.
 */
/*
*   Container class for B+ Tree Leaf Node.
*
*   Note: Key is a property of node items and BPTreeNode methods
*       getLowestKeyItem() and getHighestKeyItem() can be used
*       to check node key range.
* */
public class BPTreeLeafNode implements BPTreeNode{

    private static Logger log = Logger.getLogger(BPTreeLeafNode.class);
    private final boolean mIsLeafNode = true;

    private long mNodeIndex;    // index for node in B+ tree organization
    private BPTreeNode mParent; // parent node
    String mLowestChromKey;     // lowest chromosome/contig key name
    String mHighestChromKey;    // highest chromosome/contig key name
    int mLowestChromID;         // lowest chromosome ID corresponds to lowest key
    int mHighestChromID;        // highest chromosome ID corresponds to highest key
    private ArrayList<BPTreeLeafNodeItem> mLeafItems; // array for leaf items

    /*
    *   Constructor for the B+ tree leaf (terminal) node.
    *
    *   Parameters:
    *       nodeIndex - index assigned to the node
    *       parent - parent node (object)
    *
    *   Note: Inserted leaf items contain associated name key/chromosome ID.
    * */
    public BPTreeLeafNode(long nodeIndex, BPTreeNode parent){

        mNodeIndex = nodeIndex;
        mParent = parent;
        mLeafItems = new ArrayList<BPTreeLeafNodeItem>();
    }

    /*
    *   Method returns the node index in the B+ tree organization.
    *
    *   Returns:
    *       node index in B+ tree
    * */
    public long getNodeIndex(){
         return mNodeIndex;
    }

    /*
    *   Method identifies the node as a leaf node or a child (non-leaf) node.
    *
    *   Returns:
    *       true, if leaf node; false if child node
    * */
    public boolean isLeaf() {
        return mIsLeafNode;
    }

    /*
    *   Method inserts the node item appropriate to the item's key value.
    *
    *   Returns:
    *       Node item inserted successfully.
    * */
    public boolean insertItem(BPTreeNodeItem item){

         // Quick implementation: assumes all keys are inserted in rank order
        // todo: verify if need to compare key and insert at rank location
        mLeafItems.add((BPTreeLeafNodeItem)item );

        // Note: assumes rank order insertions
        if(mLeafItems.size() == 1 ){
            mLowestChromKey = item.getChromKey();
            mLowestChromID = ((BPTreeLeafNodeItem)item).getChromID();
        }
        else {
           mHighestChromKey = item.getChromKey();
           mHighestChromID = ((BPTreeLeafNodeItem)item).getChromID();
        }

        // success
        return true;
    }

    /*
    *   Method deletes the node item appropriate to the item's index.
    *
    *   Returns:
    *       Node item deleted successfully.
    * */
    public boolean deleteItem(int index){

        // unacceptable index
        if(index < 0 || index >= getItemCount())
           return false;

        mLeafItems.remove(index);
        return true;  // success
    }

    /*
    *   Method returns the number of items assigned to the node.
    *
    *   Returns:
    *       Count of node items contained in the node
    * */
    public int getItemCount() {
        return mLeafItems.size();
    }

    /*
    *   Method returns the indexed node item.
    *
    *   Returns:
    *       Indexed node item.
    * */
    public  BPTreeNodeItem getItem(int index){
        if(getItemCount() > 0 && index < getItemCount())
            return mLeafItems.get(index);
        else
            return null;
    }

    /*
    *   Method returns the lowest key value belonging to the node.
    *
    *   Returns:
    *       Lowest key contig/chromosome name value
    * */
    public  String getLowestChromKey(){
       if(mLeafItems.size() > 0)
           return mLowestChromKey;
       else
           return null;
    }

    /*
    *   Method returns the highest key value belonging to the node.
    *
    *   Returns:
    *       Highest key contig/chromosome name value
    * */
    public  String getHighestChromKey(){
       if(mLeafItems.size() > 0)
           return mHighestChromKey;
       else
           return null;
    }

     /*
    *   Method returns the lowest chromosome ID belonging to the node.
    *
    *   Returns:
    *       Lowest key contig/chromosome ID; or -1 if no node items
    * */
    public  int getLowestChromID(){
        if(mLeafItems.size() > 0)
            return mLowestChromID;
        else
            return -1;
    }

    /*
    *   Method returns the highest chromosome ID belonging to the node.
    *
    *   Returns:
    *       Highest key contig/chromosome ID; or -1 if no node items
    * */
    public  int getHighestChromID(){
        if(mLeafItems.size() > 0)
            return mHighestChromID;
        else
            return -1;
    }

    /*
    *   Method prints the nodes items and sub-node items.
    *       Node item deleted successfully.
    * */
    public void printItems(){
        int  itemCount = getItemCount();

        log.info("Leaf node " + mNodeIndex +  "contains " + itemCount + " leaf items:");
        for(int item = 0; item < itemCount; ++item){
            mLeafItems.get(item).print();
        }
    }

    // ************** BPTreeLeafNode specific methods ***********
    /*
    *   Method returns all leaf items mContained by this leaf node.
    *
    *   Returns:
    *       List of leaf items contained by this node
    * */
    public  ArrayList<BPTreeLeafNodeItem> getLeafItems() {
        return mLeafItems;
    }

}