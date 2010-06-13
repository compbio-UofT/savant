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

    //public boolean hasLeftChild;
    //public boolean hasRightChild;
    /**
     * Constructor
     * @param index The index to assign to this node
     * @param r The range of this bin
     */
    public IntervalTreeNode(Range r, int index, IntervalTreeNode parent) {
        this(r, parent);
        this.index = index;
    }

    public IntervalTreeNode(Range r, int index) {
        this(r, index, null);
    }

    public IntervalTreeNode(Range r, IntervalTreeNode parent) {
        this.range = r;
        this.size = 0;
        this.subtreeSize = 0;
        this.startByte = -1;
        this.children = new ArrayList<IntervalTreeNode>();
        this.parent = parent;
    }

    @Override
    public String toString() {
        //String s = "Node: ";
        //s += "\tFrom: " + this.range.getFrom() + "\tTo: " + this.range.getTo() + "\tSize: " + this.size + "\tSubtreeSize: " + this.subtreeSize + "\tStartByte: " + this.startByte;
        String s = "#" + this.index + "(" + this.range.toString() + ")";
        return s;
    }

    boolean isLeaf() {
        for (IntervalTreeNode c : children) {
            if (c != null) {
                return false;
            }
        }
        return true;
    }

    public int compareTo(Object o) {
        IntervalTreeNode n = (IntervalTreeNode) o;
        if(this.index < n.index) { return -1; }
        else if (this.index == n.index) { return 0; }
        else { return 1; }
    }
}
