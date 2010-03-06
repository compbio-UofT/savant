/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package savant.format.util.data.interval;

import savant.format.DebutFile;
import savant.model.IntervalRecord;
import savant.util.Range;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Interval BST
 *  A Binary Search Tree for intervals
 *      The binary tree is complete. Each node represents a "bin", which
 *      has a unique range in the tree. An interval i is stored in bin b
 *      if i is contained in b and there is no smaller bin in the tree
 *      which contains i.
 * @author mfiume
 */
public class IntervalSearchTree {

    /**
     * The root of this IntervalSearchTree
     */
    private IntervalTreeNode root;
    /**
     * The node list
     */
    private List<IntervalTreeNode> nodes;

    /**
     * The arity of this tree
     */
    private int arity = 5;

    /**
     * The minimum bin size for this tree
     */
    private int minBinSize = 10000;

    /**
     * Constructor for IntervalSearchTree
     *      Creates the data structure, which is a complete BST with height
     *      floor(ln(r.getLength) - ln(minBinSize)). The specifics:
     *          - a node has exactly 2 children iff its range is > minBinSize
     *          - the left child represents the first half of its parent's range
     *          - the right child represents the second half of its parent's range
     * @param r The range (which should encompass all intervals to be formatted)
     * @param minBinSize The minimum bin size
     */
    public IntervalSearchTree(Range r) {

        this.nodes = new ArrayList<IntervalTreeNode>();
        // Construct the tree
        root = createNode(r);
        //constructSubtree(root, arity, minBinSize);
    }
    
    public IntervalSearchTree(List<IntervalTreeNode> nodes) {
        this.nodes = nodes;
        this.root = nodes.get(0);
    }

    public IntervalTreeNode insert(Range r) {
        return insertAtNode(r, this.root);
    }

    public IntervalTreeNode insertAtNode(Range r, IntervalTreeNode n) {

        n.subtreeSize += 1;

        // CASE 1: contained within an existing child
        for (IntervalTreeNode c : n.children) {
            if (contains(c.range,r)) {
                return insertAtNode(r, c);
            }
        }

        // CASE 2: contained within a new child, and we havent hit min bin size
        if (n.range.getLength() > this.minBinSize) {
            Range childRange = getRangeOfContainingChild(n,r);
            if (childRange != null) {
                IntervalTreeNode child = createNode(childRange);
                n.children.add(child);

                return insertAtNode(r, child);
            }
        }

        // CASE 3: contained here
        n.size += 1;
        return n;

    }

    /**
     * Create and return a node, while keeping track
     * of the total number of nodes.
     * @param r The range of the bin being represented by the new node
     * @return A node representing the bin with range r
     */
    private IntervalTreeNode createNode(Range r) {
        //System.out.println("IntervalTreeNode at index " + this.getNumNodes() + " has range " + r);
        return createNode(r, this.getNumNodes());
    }

    private IntervalTreeNode createNode(Range r, int index) {
        //System.out.println("IntervalTreeNode at index " + this.getNumNodes() + " has range " + r);
        IntervalTreeNode n = new IntervalTreeNode(r, index);
        this.nodes.add(n);
        return n;
    }

    /**
     * Get the root
     * @return The root
     */
    public IntervalTreeNode getRoot() {
        return this.root;
    }

    /**
     * Get the number of nodes in this IntervalSearchTree
     * @return The number of nodes in this Interval BST
     */
    public int getNumNodes() {
        return this.nodes.size();
    }

    /**
     * Get the arity (branching) of this tree
     * @return The arity of this tree
     */
    public int getArity() {
        return this.arity;
    }

    private boolean intervalFitsInNode(IntervalTreeNode node, Range r) {
        return node.range.getFrom() <= r.getFrom() && node.range.getTo() >= r.getTo();
    }

        private boolean contains(Range container, Range contained) {
        if (container.getFrom() <= contained.getFrom() && container.getTo() >= contained.getTo()) { return true; }
        return false;
    }

    private Range getRangeOfContainingChild(IntervalTreeNode n, Range r) {
        int childRangeLength = n.range.getLength()/this.arity;

        for (int i = 0; i < this.arity; i++) {
            int childRangeStart = i*childRangeLength + n.range.getFrom();
            int childRangeEnd = childRangeStart + childRangeLength;
            Range childRange = new Range(childRangeStart, childRangeEnd);
            if (this.contains(childRange, r)) {
                return childRange;
            }
        }
        return null;
    }

    public List<IntervalTreeNode> getNodes() {
        return this.nodes;
    }

    public static void printIntervalSearchTreeIndex(DebutFile dFile, IntervalSearchTree intervalBSTIndex) throws IOException {
        for (IntervalTreeNode n : intervalBSTIndex.getNodes()) {
                System.out.println(n.index + " " + n.size + " " +  n.startByte);
                List<IntervalRecord> recs = IntervalRecordGetter.getRecordsInBin(dFile,n);
                for (IntervalRecord r : recs) {
                    System.out.println("\t" + r.getInterval());
                }
            }
    }
}
