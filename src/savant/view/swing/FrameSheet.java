/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.view.swing;

import savant.controller.FrameController;
import savant.controller.event.frame.*;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.util.Hashtable;
import java.util.List;

/**
 *
 * @author mfiume
 */
class FrameSheet implements FrameAddedListener, FrameRemovedListener {

    Savant parent;
    JPanel panel;
    JList list;
    //private Hashtable frame2track;
    private JTree tree;
    private DefaultMutableTreeNode rootNode;
    private DefaultTreeModel treeModel;

    public FrameSheet(Savant parent, JPanel panel) {
        this.panel = panel;
        this.parent = parent;
        initFrameSheet();
        updateFrameSheet();
        //FrameController.getInstance().addFrameAddedListener(this);
        //FrameController.getInstance().addFrameRemovedListener(this);
    }

    private void updateFrameSheetList() {
        DefaultListModel model = (DefaultListModel) this.list.getModel();
        for (int i = 0; i < FrameController.getInstance().getFrames().size(); i++) {
            model.add(i, FrameController.getInstance().getFrames().get(i).getTracks().get(0).getName());
        }
    }

    private void updateFrameSheet() {
        addObject("New Node");
    }

    public DefaultMutableTreeNode addObject(Object child) {
        DefaultMutableTreeNode parentNode = null;
        TreePath parentPath = tree.getSelectionPath();

        if (parentPath == null) {
            //There is no selection. Default to the root node.
            parentNode = rootNode;
        } else {
            parentNode = (DefaultMutableTreeNode)
                         (parentPath.getLastPathComponent());
        }

        return addObject(parentNode, child, true);
    }

    public DefaultMutableTreeNode addObject(DefaultMutableTreeNode parent,
                                            Object child,
                                            boolean shouldBeVisible) {
        DefaultMutableTreeNode childNode =
                new DefaultMutableTreeNode(child);

        treeModel.insertNodeInto(childNode, parent,
                                 parent.getChildCount());

        //Make sure the user can see the lovely new node.
        if (shouldBeVisible) {
            tree.scrollPathToVisible(new TreePath(childNode.getPath()));
        }
        return childNode;
    }

    private Hashtable getFrameTree() {

        List<Frame> frames = FrameController.getInstance().getFrames();
        Hashtable framesTree = new Hashtable();

        for (int j = 0; j < frames.size(); j++) {
            Frame fr = frames.get(j);
            String[] trackNames = new String[fr.getTracks().size()];
            for (int i = 0; i < fr.getTracks().size(); i++) {
                trackNames[i] = fr.getTracks().get(i).getName();
            }
            framesTree.put("Frame " + (j+1),trackNames);
        }

        printMap(framesTree);

        return framesTree;
    }

    public void frameChangedReceived(FrameChangedEvent event) {
        updateFrameSheet();
    }

    private void printMap(Hashtable framesTree) {

        for (Object key : framesTree.keySet()) {
            String k = (String) key;
            //System.out.println(k);

            String[] value = (String[]) framesTree.get(key);
            for (String val : value) {
                //System.out.println("\t" + val);
            }
        }
    }

    private void initFrameSheetList() {
        list = new JList();
        list.setModel(new DefaultListModel());
        this.panel.add(list, BorderLayout.CENTER);
    }

    private void initFrameSheet() {
        rootNode = new DefaultMutableTreeNode("Root Node");
        treeModel = new DefaultTreeModel(rootNode);
        treeModel.addTreeModelListener(new FrameTreeModelListener());

        tree = new JTree(treeModel);
        tree.setEditable(true);
        tree.getSelectionModel().setSelectionMode
                (TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.setShowsRootHandles(true);

        this.panel.add(tree,BorderLayout.CENTER);
    }

    public void frameAddedReceived(FrameAddedEvent event) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void frameRemovedReceived(FrameRemovedEvent event) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
