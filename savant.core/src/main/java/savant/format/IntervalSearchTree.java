/**
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package savant.format;

import java.util.ArrayList;
import java.util.List;

import savant.util.Range;


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
     */
    public IntervalSearchTree(Range r) {

        this.nodes = new ArrayList<IntervalTreeNode>();

        // Construct the tree
        root = createNode(r, null);
    }
    
    public IntervalSearchTree(List<IntervalTreeNode> nodes) {
        this(nodes,0);
    }

    public IntervalSearchTree(List<IntervalTreeNode> nodes, int indexOfRoot) {
        this(nodes,nodes.get(indexOfRoot));
    }

    public IntervalSearchTree(List<IntervalTreeNode> nodes, IntervalTreeNode root) {
        this.nodes = nodes;
        this.root = root;
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
                IntervalTreeNode child = createNode(childRange, n);
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
    private int numcreated = 0;
    private IntervalTreeNode createNode(Range r, IntervalTreeNode p) {
        return createNode(r, numcreated, p);
    }

    private IntervalTreeNode createNode(Range r, int index, IntervalTreeNode parent) {
        //System.out.print("C " + index );
        //if (parent != null) { System.out.print(" < " + parent.index); } else { System.out.println(); }
        //System.out.println("\t" + r);
        IntervalTreeNode n = new IntervalTreeNode(r, index, parent);
        this.nodes.add(n);
        numcreated++;
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
        int nonnullnodes = 0;
        for (IntervalTreeNode n : this.nodes) {
            if (n!=null) {
                nonnullnodes++;
            }
        }
        return nonnullnodes;
    }

    /**
     * Get the arity (branching) of this tree
     *
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
        int childRangeLength = n.range.getLength() / arity;

        for (int i = 0; i < arity; i++) {
            int childRangeStart = i * childRangeLength + n.range.getFrom();
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

    public IntervalTreeNode getNodeWithSmallestMax() {
        return getNodeWithSmallestMax(this.root);
    }

    private IntervalTreeNode getNodeWithSmallestMax(IntervalTreeNode node) {

        if (node.isLeaf()) {

            //System.out.println("[ " + node.index + " ]");
            return node;
        }

        //System.out.print(node.index + " >> ");

        int indexofsmallestmax = -1;
        int smallestmax = Integer.MAX_VALUE;

        for (int i = 0; i < node.children.size(); i++) {
            IntervalTreeNode c = node.children.get(i);
            if (c.range.getTo() < smallestmax) {
                smallestmax = c.range.getTo();
                indexofsmallestmax = i;
            }
        }
        return getNodeWithSmallestMax(node.children.get(indexofsmallestmax));
    }

    public void removeNode(IntervalTreeNode node) {

        //System.out.println("removing node that has: " + node.children.size() + " children");

        this.nodes.set(node.index, null);

        if (node.parent != null) {
            node.parent.children.remove(node);
        } else {
            this.root = null;
        }

        
        //System.gc();
    }

}
