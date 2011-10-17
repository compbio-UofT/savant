package org.broad.igv.bbfile;

import org.apache.log4j.Logger;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: martind
 * Date: Jan 13, 2010
 * Time: 11:33:41 AM
 * To change this template use File | Settings | File Templates.
 */

/*
*   Container class for B+ Tree Child (Non-Leaf) Node.
*
*   Note: Key is a property of node items and BPTreeNode methods
*       getLowestKeyItem() and getHighestKeyItem() can be used
*       to check node key range.
*
* */
public class BPTreeChildNode implements BPTreeNode{

    private static Logger log = Logger.getLogger(BPTreeChildNode.class);
    private final boolean mIsLeafNode = false;

    private long mNodeIndex;    // index for node in B+ tree organization
    private BPTreeNode mParent; // parent node
    String mLowestChromKey;     // lowest chromosome/contig key name
    String mHighestChromKey;         // highest chromosome/contig key name
    int mLowestChromID;         // lowest chromosome ID corresponds to lowest key
    int mHighestChromID;        // highest chromosome ID corresponds to highest key
    private ArrayList<BPTreeChildNodeItem> mChildItems; // child node items

    /*
    *   Constructor for the B+ tree child (non-leaf) node.
    *
    *   Parameters:
    *       nodeIndex - index assigned to the node
    *       parent - parent node (object)
    *
    *   Note: Inserted child items contain child/leaf nodes assigned.
    * */
    public BPTreeChildNode(long nodeIndex, BPTreeNode parent){

        mNodeIndex = nodeIndex;
        mParent = parent;
        mChildItems = new ArrayList<BPTreeChildNodeItem>();
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
        mChildItems.add((BPTreeChildNodeItem)item );

        BPTreeNode childNode = ((BPTreeChildNodeItem)item).getChildNode();

        // Note: assumes rank order insertions
        if(mChildItems.size() == 1 ){
            mLowestChromKey = childNode.getLowestChromKey();
            mLowestChromID = childNode.getLowestChromID();
        }
        else {
            mHighestChromKey = childNode.getHighestChromKey();
            mHighestChromID = childNode.getHighestChromID();
        }


        return true;    // success
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

        mChildItems.remove(index);
        return true;    // success
    }

    /*
    *   Method returns the number of items assigned to the node.
    *
    *   Returns:
    *       Count of node items contained in the node
    * */
    public int getItemCount() {
        return mChildItems.size();
    }

    /*
    *   Method returns the indexed node item.
    *
    *   Returns:
    *       node index in B+ tree
    * */
    public BPTreeNodeItem getItem(int index){
        int itemCount = getItemCount();

        if(index >= itemCount)
            return null;

        return mChildItems.get(index);
    }

    /*
    *   Method returns the lowest chromosome key value belonging to the node.
    *
    *   Returns:
    *       Lowest contig/chromosome name key value; or null if no node items
    * */
    public  String getLowestChromKey(){
        if(mChildItems.size() > 0)
            return mLowestChromKey;
        else
            return null;
    }
    
    /*
    *   Method returns the highest chromosome key value belonging to the node.
    *
    *   Returns:
    *       Highest contig/chromosome name key value; or null if no node items
    * */
    public  String getHighestChromKey(){
        if(mChildItems.size() > 0)
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
        if(mChildItems.size() > 0)
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
        if(mChildItems.size() > 0)
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

        log.info("Child node " + mNodeIndex + " contains " + itemCount + " child items:");
        for(int item = 0; item < itemCount; ++item){

            // recursively will print all node items below this node
            mChildItems.get(item).print();
        }
    }

    // *********** BPTreeChildNode specific methods *************
    /*
    *   Method returns all child items mContained by this child node.
    *
    *   Returns:
    *       List of child items contained by this node
    * */
    public ArrayList<BPTreeChildNodeItem> getChildItems(){
        return mChildItems;
    }

}
