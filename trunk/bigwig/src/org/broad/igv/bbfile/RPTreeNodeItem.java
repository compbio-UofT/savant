package org.broad.igv.bbfile;

/**
 * Created by IntelliJ IDEA.
 * User: martind
 * Date: Jan 6, 2010
 * Time: 4:29:53 PM
 * To change this template use File | Settings | File Templates.
 */
/*
*   RPTreeNodeItem interface for storage of R+ tree node item information.
*
*   Note: The bounding 1D rectangle defined by:
*       (mStartBase mChromosome, mStartBase base) to (mEndBase mChromosome, mEndBase base)
*    is used as a key for insertion and searches on the R+ tree.
*
* */

interface RPTreeNodeItem  {

    // Returns the items index in the parent node list.
    long getItemIndex();

    // Identifies the item as a leaf item or a child node item.
    boolean isLeafItem();

    // returns the chromosome boundary for the item
    public RPChromosomeRegion getChromosomeBounds();

    // Note: compareRegions returns the following values:
     //   -2 indicates chromosome region is completely below node region
     //   -1 indicates that chromosome region intersect node region from below
     //  0 means that chromosome region is inclusive to node region
     //  1 indicates chromosome region intersects node region from above
     //  2 indicates that this region is completely above that region
    public int compareRegions(RPChromosomeRegion chromosomeRegion);

    // Prints the tree node item.
    void print();
}
