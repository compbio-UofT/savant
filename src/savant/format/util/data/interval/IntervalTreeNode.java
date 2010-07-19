/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package savant.format.util.data.interval;

import savant.util.Range;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author mfiume
 */
/**
 * A IntervalTreeNode (representing a bin) in an IntervalSearchTree
 */
public class IntervalTreeNode implements Comparable{

    // number of intervals in this bin
    public int size;
    // number of intervals in this subtree (this node inclusive)
    public int subtreeSize;
    // the range of the bin
    public Range range;
    // where in the data structure intervals in this bin
    // start from
    public long startByte;
    // the index of this node
    public int index;
    // list of children
    public List<IntervalTreeNode> children;
    // parent
    public IntervalTreeNode parent;

    /**
     * Constructor
     * @param index The index to assign to this node
     * @param r The range of this bin
     */
    public IntervalTreeNode(Range r, int index, IntervalTreeNode parent) {
        this.range = r;
        this.size = 0;
        this.subtreeSize = 0;
        this.startByte = -1;
        this.children = new ArrayList<IntervalTreeNode>();
        this.parent = parent;
        this.index = index;
    }

    public IntervalTreeNode(Range r, int index) {
        this(r, index, null);
    }

    /**
     * Create an IntervalTreeNode with the given range and parent
     * @param r The range for this node
     * @param parent The parent of this node
     */
    public IntervalTreeNode(Range r, IntervalTreeNode parent) {
        this(r,-1, parent);
    }

    @Override
    public String toString() {
        String s = "#" + this.index + "(" + this.range.toString() + ")";
        return s;
    }

    /**
     * Returns whether or not this node is a leaf.
     * @return
     */
    boolean isLeaf() {
        /**
         * Since objects in the children list may be null
         * we have to look at each one instead of simply returning
         * whether or not the size of children is 0
         */

        // look at each child
        for (IntervalTreeNode c : children) {

            // if any child is non-null, then this is not a leaf
            if (c != null) { return false; }
        }

        // all children are null, this is a leaf
        return true;
    }

    /**
     * Compare this interval tree node to another
     *      Comparison is by ** index **
     * @param o An interval tree node to compare with this
     * @return -1 if this < o; 0 if this == o, 1 if this > o
     */
    public int compareTo(Object o) {
        IntervalTreeNode n = (IntervalTreeNode) o;
        if(this.index < n.index) { return -1; }
        else if (this.index == n.index) { return 0; }
        else { return 1; }
    }
}
