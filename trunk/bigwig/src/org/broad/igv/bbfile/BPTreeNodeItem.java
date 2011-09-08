
/**
 * Created by IntelliJ IDEA.
 * User: martind
 * Date: Dec 23, 2009
 * Time: 11:43:43 AM
 * To change this template use File | Settings | File Templates.
 */
package org.broad.igv.bbfile;

import java.lang.Comparable;

/*
*   BPTreeNodeItem interface for storage of B+ tree node item information.
*
*   Note: The alpha-numeric key string is used for positional insertion of
*    node items and searching of the B+ tree.
* */

interface BPTreeNodeItem  {

    // Returns the child node item or leaf item index in the B+ tree.
    long getItemIndex();

    // Identifies the item as a leaf item or a child node item.
    boolean isLeafItem();

    // Returns key used to position the item in parent node item list.
    String getChromKey();

    // Returns true if keys match, returns false if keys do not match.
    boolean chromKeysMatch(String chromKey);

    // Prints the tree node items.
    void print();
}
