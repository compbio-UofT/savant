package org.broad.igv.bbfile;

/**
 * Created by IntelliJ IDEA.
 * User: martind
 * Date: Jan 14, 2010
 * Time: 11:16:03 AM
 * To change this template use File | Settings | File Templates.
 */
public interface RPTreeNode {

    // Returns the node index in the R+ tree organization.
    public long getNodeIndex();

    // Identifies the node as a leaf node or a child (non-leaf) node.
    public boolean isLeaf();

    // Returns the chromosome bounds belonging to the entire node.
    public RPChromosomeRegion getChromosomeBounds();

     // Note: compareRegions returns the following values:
     //   -2 indicates chromosome region is completely below node region
     //   -1 indicates that chromosome region intersect node region from below
     //  0 means that chromosome region is inclusive to node region
     //  1 indicates chromosome region intersects node region from above
     //  2 indicates that this region is completely above that region
    public int compareRegions(RPChromosomeRegion chromosomeRegion);

    // Returns the number of items assigned to the node.
    public int getItemCount();

    // Returns the indexed node item.
    public  RPTreeNodeItem getItem(int index);

    // Inserts new node item according to bounds rank
    public boolean insertItem(RPTreeNodeItem item);

    // Deletes indexed node item
    public boolean deleteItem(int index);

    // prints the node items
    public void printItems();
}
