/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

//ftp://ftp.uniprot.org/pub/databases/uniprot/current_release/knowledgebase/idmapping/
// Use idmapping.dat.gz to map to RefSeq, then, use UCSC to map to genomic positions.

// See http://www.java2s.com/Code/Java/File-Input-Output/GZipwithGZIPOutputStream.htm
package savant.geneontology;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;
import savant.api.util.NavigationUtils;

/**
 *
 * @author Nirvana Nursimulu
 * Contains methods to display a tree with parents and children.
 */
public class ParentChildrenTree extends JPanel 
implements TreeSelectionListener, ListSelectionListener, ActionListener, 
HyperlinkListener, WindowListener, PropertyChangeListener{
    
    /**
     * Tree, as user-defined, that can be displayed.
     */
    private XTree tree;
    
    /**
     * Actual JTree
     */
    private JTree jTree;
    
    /**
     * HTML panel.
     */
    private JEditorPane htmlPane;
    
    /**
     * JList object for displaying locations (RefSeqs).
     */
    private JList locList;
    
    /**
     * A URL to look at, at present.
     */
    private URL url;
    
    /**
     * The actual root of the tree.
     */
    private DefaultMutableTreeNode actualRoot;
    
    /**
     * Dialog panel.
     */
    private JSplitPane optionPane;
    
    /**
     * Actual dialog.
     */
    JDialog dialog;
    
    /**
     * Use to set the page.
     */
    private SettingPage setter;
    
    
    /**
     * The node which has currently been selected.
     */
    private XNode currentlySelected;
    
    /**
     * The frame to contain this panel
     */
    private JFrame frameContainer;
    
    /**
     * The list within the dialog box.
     */
    private JTable dialogTable;
    
    /**
     * The button to ask to map to the genome.
     */
    private JButton buttonMapToGenome;
    
    /**
     * The button that will allow moving to the previous page in the browser.
     */
    private JButton backButton;
    
    /**
     * The button that will allow moving to the next page in the browser.
     */
    private JButton forwardButton;
    
    /**
     * Linked list keeping track of the pages visited.
     */
    private LinkedList pagesVisited;
    
    /**
     * Progress bar to display the progress in loading the page in question.
     */
    private JProgressBar progressBar;
    
     /**
     * Returns a tree in a GUI (for Savant)
     * @param mapFile the location of the file mapping locations.
     */
    public static ParentChildrenTree getTree(String mapFile) throws Exception{
        
        ParentChildrenTree gui_tree = new ParentChildrenTree
                (XMLontology.makeTree(mapFile));
        return gui_tree;
    }
    
    
    /**
     * Initializing steps all done in constructor.
     * @param tree the tree upon which this UI will be made. 
     */
    public ParentChildrenTree(XTree tree){
        
        // The current tree.
        this.tree = tree;
        
        // To keep track of the pages which have been visited.
        pagesVisited = new LinkedList();
        
        // Set layout manager
        this.setLayout(new BorderLayout());
        
        // Root of the tree (dummy).
        XNode root = new XNode("...", false);
        root.setDescription("...");
        root.setLocs(new ArrayList< ArrayList<String> >());
        root.setURL("http://amigo.geneontology.org/cgi-bin/amigo/go.cgi");
        
        actualRoot = new DefaultMutableTreeNode(root);
        
        // Create and add the rest of the nodes in the tree.
        addNodes();
        
        // Add the root to the actual jTree, and make tree responsive to selection.
        this.jTree = new JTree(actualRoot);
        jTree.getSelectionModel().setSelectionMode
                (TreeSelectionModel.SINGLE_TREE_SELECTION);
        jTree.addTreeSelectionListener(this);
        // Allows you to see the nodes that have been selected and the currently
        // selected node.
        jTree.setDropMode(DropMode.INSERT);
        // Put the tree in a scroll pane.
        JScrollPane treeView = new JScrollPane(jTree);
        // Create the HTML viewing pane (in the middle).
        JPanel center = new JPanel(new BorderLayout());
        htmlPane = new JEditorPane();
        htmlPane.setEditable(false);
        htmlPane.addHyperlinkListener(this); 
//        htmlPane.setContentType("text/html");
        JScrollPane htmlView = new JScrollPane(htmlPane);
        center.add(htmlView, BorderLayout.CENTER); 
        
        // The back and forward buttons for navigating the browser.
        Box buttonBox = new Box(BoxLayout.X_AXIS);
        buttonBox.setBorder(new EmptyBorder(new Insets(5, 10, 5, 10)));
        center.add(buttonBox, BorderLayout.NORTH);
        // http://java.sun.com/developer/technicalArticles/GUI/swing/wizard/
        backButton = new JButton("<Back");
        backButton.addActionListener(new BackAndForwardListener("<Back", this));
        forwardButton = new JButton("Next>");
        forwardButton.addActionListener(new BackAndForwardListener("Next>", this));
        buttonBox.add(backButton);
        buttonBox.add(Box.createHorizontalStrut(10));
        buttonBox.add(forwardButton);
        buttonBox.add(Box.createHorizontalStrut(10));
        
        // Add the progress bar for when loading a page to the browser.
        progressBar = new JProgressBar();
        buttonBox.add(progressBar);
        progressBar.setStringPainted(false);
        
        // At the beginning, the buttons cannot be clicked upon.
        backButton.setEnabled(false);
        forwardButton.setEnabled(false);
        
        
        // Create the jList for viewing RefSeq IDs.
        locList = new JList();
        JScrollPane locView = new JScrollPane(locList);
        locList.addListSelectionListener(this);
        // Make it such that can select only one selection at a time.
        locList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Set the different components at different portions on the screen.
        this.add(treeView, BorderLayout.WEST);
        this.add(center, BorderLayout.CENTER);
        this.add(locView, BorderLayout.EAST);
        
        
        // Set components related to the dialog box.
        frameContainer = new JFrame();
        frameContainer.add(this);
        optionPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        dialog = new JDialog(frameContainer, "Map to genome");
        dialog.setContentPane(optionPane);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setAlwaysOnTop(true);
        dialog.setMinimumSize(new Dimension(400, 150));
        dialog.setResizable(false);
        dialog.addWindowListener(this);
        // The button to map to the genome location.
        buttonMapToGenome = new JButton("Find in genome...");
        buttonMapToGenome.addActionListener(this);
        buttonMapToGenome.setPreferredSize(new Dimension(400, 10));
        // The top component will be a JScrollPane which we cannot see currently
        // Set bottom component to be a button.
        optionPane.setBottomComponent(buttonMapToGenome);         
    }
    
    
    /**
     * Add the nodes other than the root to the tree. Move in a breadth-first
     * fashion.
     */
    private void addNodes(){
        
        // To contain the roots of the tree.
        Set<XNode> roots = tree.getRootNodes();
        
        // Get the name of the children while going down the tree.
        TreeSet<XNode> children;
        
        // The child in consideration in context.
        DefaultMutableTreeNode child;
        
        // To contain the parent nodes (to be used when displaying) in question.
        List<DefaultMutableTreeNode> parentNodes = 
                new ArrayList<DefaultMutableTreeNode>();
        
        
        // To contain the children nodes in question.
        List<DefaultMutableTreeNode> childrenNodes = 
                new ArrayList<DefaultMutableTreeNode>();
        
        
        // Add all roots to the tree.
        for (XNode root: roots){
        
            // Connect the root to its children.
            child = new DefaultMutableTreeNode(root);
            actualRoot.add(child);
            
            // The future parents to be considered.
            parentNodes.add(child);
        }
        
        // While we still have children nodes...
        while(!parentNodes.isEmpty()){
            
            // Go through the tree in a breadth-first manner.
            for (DefaultMutableTreeNode parent: parentNodes){

                // Get the set of children, and have the parents accept their
                // children.
                children = tree.getChildrenNodes
                        (((XNode)parent.getUserObject()).getIdentifier());

                for (XNode child2: children){

                    child = new DefaultMutableTreeNode(child2);
                    childrenNodes.add(child);
                    parent.add(child);
                }
            }

            // Now have the children become parents.
            parentNodes = getCopy(childrenNodes);
            childrenNodes.clear();           
        }
    } // addNode function
    
    /**
     * Returns a copy of a list containing DefaultMutableTreeNode objects.
     * @param source from where to copy
     * @return the copy
     */
    private List<DefaultMutableTreeNode> getCopy(List<DefaultMutableTreeNode> source){
        
        List<DefaultMutableTreeNode> dest = 
                new ArrayList<DefaultMutableTreeNode>();
        
        for (DefaultMutableTreeNode node: source){
            
            dest.add(node);
        }
        return dest;
    } // getCopy function

    
    
    /**
     * Updates the list that is displayed in the frame (using a thread).
     */
    private void updateList(ArrayList<ArrayList<String>> locs){
                
        // Get the first element of each location into an array so as to update the list.
        TreeSet<String> set = new TreeSet<String>();

        for (ArrayList<String> loc: locs){

            set.add(loc.get(0));
        }
               
        locList.setListData(set.toArray());  
    } // updateList function
    
    
    /**
     * Displays the URL using a thread.
     */
    private void displayURL(String urlStr) {
        
        // If a null object, don't do anything.
        if (urlStr == null){

            return;
        }

        try{
            url = new URL(urlStr);
            if (url != null){

                setter = new SettingPage(url, this);
                setter.execute();
            } 
            // null URL
            else{ 
                htmlPane.setText("File Not Found");
            }
            // Add to the list of pages visited.
            pagesVisited.addNode(urlStr);
        } 
        catch (IOException e) {
            System.err.println("Attempted to read a bad URL: " + url);
        }
    }
    
    
    
    /**
     * Show me the tree. This is for visualising GUI not on Savant.  Did not 
     * "optimize" it since still need to get the name of the mapFile.
     * @param mapFile the file mapping GO IDs to genomic locations.
     */
    public static void showTree(String mapFile) throws Exception{
        
        // Create frame
        JFrame frame = new JFrame("GO Ontology Hierarchy");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // Add the tree to the window
        frame.add( new ParentChildrenTree(XMLontology.makeTree(mapFile)) );
        
        // Display the window.
        frame.pack();
        frame.setVisible(true);
    }


    /*
     * A WHOLE BUNCH OF LISTENERS
     */

    @Override
    /**
     * To deal with the user having clicked on the button to map to genome 
     * location.
     */
    public void actionPerformed(ActionEvent e) {
        
        int row = dialogTable.getSelectedRow();

        String chromosome = (String) dialogTable.getValueAt(row, 0);
        int start = Integer.parseInt( (dialogTable.getValueAt(row, 1) + "").trim() );
        int end = Integer.parseInt( (dialogTable.getValueAt(row, 2) + "").trim() );

        NavigationUtils.navigateTo(chromosome, NavigationUtils.createRange(start, end));
    }

    @Override
    /**
     * Respond to clicking on URLs.
     */
    public void hyperlinkUpdate(HyperlinkEvent e) {
        
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED){
            
            displayURL(e.getURL().toString());
        }
    }

    @Override
    // Just needed to implement for WindowsListener interface.
    public void windowOpened(WindowEvent e) {
    }

    @Override
    // Just needed to implement for WindowsListener interface.
    public void windowClosing(WindowEvent e) {
    }

    @Override
    /**
     * Whenever the window (dialog) is closed, clear the selected element 
     * in the location list.
     */
    public void windowClosed(WindowEvent e) {
        
        locList.clearSelection();
        dialog.setVisible(false);
    }

    @Override
    // Just needed to implement for WindowsListener interface.
    public void windowIconified(WindowEvent e) {
    }

    @Override
    // Just needed to implement for WindowsListener interface.
    public void windowDeiconified(WindowEvent e) {     
    }

    @Override
    // Just needed to implement for WindowsListener interface.
    public void windowActivated(WindowEvent e) {
    }

    @Override
    // Just needed to implement for WindowsListener interface.
    public void windowDeactivated(WindowEvent e){
    }
    
    @Override
    /**
     * Responds to changes in the loading of the page.
     */
    public void propertyChange(PropertyChangeEvent evt) {
                    
        if ("progress".equals(evt.getPropertyName()) && 
                (Integer)(evt.getNewValue()) == 100){
            
            progressBar.setIndeterminate(false);
        }   
        else if ("state".equals(evt.getPropertyName()) && 
                "STARTED".equals(evt.getNewValue() + "")){
            
            progressBar.setIndeterminate(true);
        }
    }
    
    /**
     * Listener for back and forward buttons for the browser.
     */
    class BackAndForwardListener implements ActionListener{
        
        String nameButton;
        
        PropertyChangeListener p;
        
        /**
         * Constructor.
         * @param nameButton the name of the button
         */
        BackAndForwardListener(String nameButton, PropertyChangeListener p){
            
            this.nameButton = nameButton;
            this.p = p;
        }

        /**
         * When a back or forward button is clicked, do the necessary.
         * @param e 
         */
        @Override
        public void actionPerformed(ActionEvent e) {
            
            // If the next button has been clicked...
            if (nameButton.equals("Next>")){
                
                String next = pagesVisited.moveUp();

                
                // If the next page does not exist, do not do anything.
                if (next == null){
                    return;
                }
                
                // Set the new page.
                try {
                    setter = new SettingPage(new URL(next), p);
                    setter.execute();
                } catch (Exception ex) {
                    System.out.println(ex);
                }

            }
            // If the back button has been clicked...
            else if (nameButton.equals("<Back")){
             
                String back = pagesVisited.moveBack();

                
                // If the next page does not exist, do not do anything.
                if (back == null){
                    return;
                }
                
                // Set the new page.
                try {
                    setter = new SettingPage(new URL(back), p);
                    setter.execute();
                } catch (Exception ex) {
                    System.out.println(ex);
                }
                
            }
            // Nothing is done if this listener is for another button.
            else
                ;
        }
        
    }
    
    @Override
    /**
     * Returns the last path element of the selection.
     * This method is useful only when the selection model allows a single 
     * selection.
     */
    public void valueChanged(TreeSelectionEvent e) {

        // Last component that has been selected.
        DefaultMutableTreeNode component = (DefaultMutableTreeNode)
                           jTree.getLastSelectedPathComponent();

        // If nothing has been selected.
        if (component == null){
            
            return;
        }
        
        // Put the dialog out of sight.
        dialog.setVisible(false); 
        
        // Display the url of interest.
        XNode node = (XNode) component.getUserObject();
        
        // Keep track of the node which has been selected.
        currentlySelected = node;

        // update the jList object that is shown.
        updateList(node.getLocs());
        
        displayURL(node.getURL());
    }
        
     @Override
    /**
     * Display dialog when an element in the list has been selected.
     */
    public void valueChanged(ListSelectionEvent e) {
        
        // Continue if and only if the user is still manipulating the selection.
        if (!e.getValueIsAdjusting()){
            
            return;
        }
        
        // The value which has been selected
        String located = ((String)locList.getSelectedValue());

        // The genome locations that ought to be displayed.
        TreeSet<String> locations = new TreeSet<String>();
        
        // For each location, find the one that we want.
        for (ArrayList<String> loc : currentlySelected.getLocs()){
            
            // When we find something that we want...
            // Make sure the chromosome name has no underscores 
            // (o/w, we do not have records for this anyway).
            if (loc.get(0).equals(located) && !loc.get(1).matches(".*_.*")){
                
                int endPos = Integer.parseInt(loc.get(3).trim()) - 1;
                String acc = loc.get(1) + "\t" + loc.get(2) + "\t" + endPos;
                locations.add(acc);
            }
        }

        String[][] data = new String[locations.size()][3];
        
        Iterator<String> iterator = locations.iterator();
        
        for (int i = 0; i < locations.size(); i++){
            
            String[] acc = iterator.next().split("\t");
            data[i] = acc;
        }
        
        // Create the table and add as the top component of the dialog.
        String[] headers = {"Chromosome", "Start position", "End position"};
        dialogTable = new JTable(data, headers);
        dialogTable.setVisible(true);
        dialogTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        JScrollPane paneForTable = new JScrollPane(dialogTable);
        paneForTable.setPreferredSize(new Dimension(400, 70));
        

        optionPane.setTopComponent(paneForTable);
        
        // Ask to honour size that we set for bottom component.
        optionPane.setDividerLocation(-1); 
        
        dialog.setVisible(true);
    }
        
     // A BUNCH OF INTERNAL CLASSES.
        
     /**
     * Linked list implementation; modified stack.  Basically will allow moving 
     * forward and backward in a list.
     */
    class LinkedList{
        
        /**
         * A node to store information in the linked list.
         */
        class Node{
            
            /**
             * THe identifier of this node.
             */
            String identifier;
            
            /**
             * The node that comes next in the linked list.
             */
            Node next;
            
            /**
             * The node that is just before this one in the linked list.
             */
            Node back;
            
            /**
             * A string identifying the present node.
             * @param identifier 
             */
            Node(String identifier){
                
                this.identifier = identifier;                
                this.back = null;
                this.next = null;
            }
        } // End of Node class.
        
        /**
         * The size of the linked list.
         */
        int size;
        
        /**
         * The current node.
         */
        Node currentNode;
        
        /**
         * The number of nodes from the end of the list.
         */
        int numNodesFromEnd;
        
        /**
         * Constructor for an empty linked list.
         */
        LinkedList(){
         
            this.size = 0;
            currentNode = null;
            this.numNodesFromEnd = 0;
        }
        
        /**
         * Clear the list.
         */
        void clear(){
            
            this.size = 0;
            currentNode = null;
        }
        
        /**
         * The string to add to the linked list.
         * @param toAdd the string in question.
         */
        void addNode(String toAdd){
            
            // If no node has been put in the linked list yet...
            if (size == 0){
                
                currentNode = new Node(toAdd);
            }
            // Otherwise, just add to the current node.
            else{
                
                // Add the links.
                Node nodeToAdd = new Node(toAdd);
                currentNode.next = nodeToAdd;
                nodeToAdd.back = currentNode;
                // Move the "cursor".
                currentNode = nodeToAdd;
                
                // Print as much as can; use for debugging.
//                Node prev = currentNode;
//                System.out.println();
//                while (prev != null){
//                    System.out.print(prev.identifier + "\t");
//                    prev = prev.back;
//                }
                
                // we can go back, but not forward
                backButton.setEnabled(true);
                forwardButton.setEnabled(false);
            }
            
            // Increment size but...
            // If there are nodes which have now gone, do something about that.
            size = size + 1 - numNodesFromEnd;
            numNodesFromEnd = 0;
            
        } // addNode function
        
        /**
         * Move pointer down the list 
         * and return the string representing the new current node.
         */
        String moveBack(){
            
            // If there is nothing in the list, there is nothing to do.
            if (size == 0){
                
                return null;
            }
            // If we are already at the beginning of the list, not do anything.
            else if (currentNode.back == null){
                
                return null;
            }
            // Otherwise, do move back.
            else{
                
                numNodesFromEnd = numNodesFromEnd + 1;
                currentNode = currentNode.back;
                
                // Set the back button not enabled if we cannot go further back
                if (currentNode.back == null){
                
                    backButton.setEnabled(false);
                }
                
                // But always say that we can go forward.
                forwardButton.setEnabled(true);
                
                return currentNode.identifier;
            }

        } // moveBack function
        
        /**
         * Move pointer up the list
         * and return the string representing the new current node.
         */
        String moveUp(){
            
            // If there is nothing in the list, there is nothing to do.
            if (size == 0){
                
                return null;
            }
            // If we are already at the end of the list, don't do anything.
            else if (currentNode.next == null){
                
                return null;
            }
            // Otherwise, do move up.
            else{
                
                numNodesFromEnd = numNodesFromEnd - 1;
                currentNode = currentNode.next;
                
                // Set the back button enabled always.
                backButton.setEnabled(true); 
                
                // Set the forward button disabled if no more nodes to reach.
                if (currentNode.next == null){
                
                    forwardButton.setEnabled(false);
                }
                
                
                return currentNode.identifier;
            }

        } // moveUp function
        
    } // end of LinkedList inner class.
    
    /**
     * SwingWorker for setting the URL page (takes some time to load).
     */
    class SettingPage extends SwingWorker{
        
        /**
         * The URL to be used here.
         */
        URL urlToUse;
        
        PropertyChangeListener p;
        
        SettingPage(URL urlToUse, PropertyChangeListener p){
            
            this.urlToUse = urlToUse; 
            this.addPropertyChangeListener(p);
        }
        
        /**
         * Set the URL to use.
         * @param newURL 
         */
        public void setURL(URL newURL){
            
            this.urlToUse = newURL;
        }
        

        @Override
        protected Object doInBackground() {
            
            
            // Start the process of reading the page (or trying to)
            try {
                htmlPane.setPage(urlToUse);

            } 
            catch (Exception ex) {
                System.out.println(ex);
            }
            // Say that we got a problemo or we are done loading the page.
            setProgress(100);
            return null;  
            
        }
        
    } // end of SettingPage class.
    
}  // End of ParentChildren class.
