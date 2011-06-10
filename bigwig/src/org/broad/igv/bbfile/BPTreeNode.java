package org.broad.igv.bbfile;

/**
 * Created by IntelliJ IDEA.
 * User: martind
 * Date: Jan 13, 2010
 * Time: 11:30:36 AM
 * To change this template use File | Settings | File Templates.
 */

 /*
 *  Interface defining the B+ Tree Node behavior
*
*   Note: Key is a property of node items and BPTreeNode methods
*       getLowestChromKey() and getHighestChromKey() can be used
*       to check node key range.
* */
public interface BPTreeNode {

    /*
    *   Method returns the node index in the B+ tree organization.
    *
    *   Returns:
    *       node index in B+ tree
    * */
    public long getNodeIndex();


    /*
    *   Method identifies the node as a leaf node or a child (non-leaf) node.
    *
    *   Returns:
    *       true, if leaf node; false if child node
    * */
    public boolean isLeaf();

    /*
    *   Method inserts the node item appropriate to the item's key value.
    *
    *   Returns:
    *       Node item inserted successfully.
    * */
    public boolean insertItem(BPTreeNodeItem item);

    /*
    *   Method deletes the node item appropriate to the item's index.
    *
    *   Returns:
    *       Node item deleted successfully.
    * */
    public boolean deleteItem(int index);

    /*
    *   Method returns the number of items assigned to the node.
    *
    *   Returns:
    *       Count of node items contained in the node
    * */
    public int getItemCount();

    /*
    *   Method returns the indexed node item.
    *
    *   Returns:
    *       Indexed node item.
    * */
    public  BPTreeNodeItem getItem(int index);

    /*
    *   Method returns the lowest chromosome name key belonging to the node.
    *
    *   Returns:
    *       Lowest contig/chromosome name key; or null for no node items.
    * */
    public  String getLowestChromKey();

    /*
    *   Method returns the highest chromosome name key belonging to the node.
    *
    *   Returns:
    *       Highest contig/chromosome name key; or null for no node items.
    * */
    public  String getHighestChromKey();

    /*
    *   Method returns the lowest chromosome ID belonging to the node.
    *
    *   Returns:
    *       Lowest contig/chromosome ID; or -1 for no node items.
    * */
    public  int getLowestChromID();

    /*
    *   Method returns the highest chromosome ID  belonging to the node.
    *
    *   Returns:
    *       Highest contig/chromosome ID; or -1 for no node items.
    * */
    public  int getHighestChromID();

    /*
    *   Method prints the nodes items and sub-node items.
    *       Node item deleted successfully.
    * */
    public void printItems();

}
