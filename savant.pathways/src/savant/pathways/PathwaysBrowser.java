/*
 *    Copyright 2011 University of Toronto
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package savant.pathways;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import org.bridgedb.bio.Organism;
import org.pathvisio.wikipathways.WikiPathwaysClient;
import org.pathvisio.wikipathways.webservice.WSPathwayInfo;
import org.pathvisio.wikipathways.webservice.WSSearchResult;
import savant.api.util.DialogUtils;
import savant.settings.DirectorySettings;

/**
 *
 * @author AndrewBrook
 */
public class PathwaysBrowser extends JPanel{

    private JLabel messageLabel;
    private JTable table;
    private WikiPathwaysClient wpclient;
    private Viewer svgPanel;
    private Loader loader;
    
    //info re download of files
    private String svgFilename;
    private String gpmlFilename;
    private boolean svgDownloaded = false;
    private boolean gpmlDownloaded = false;
    private int downloadErrorCount = 0;
    
    //label messages
    public static final String ALL_ORGANISMS = "All Organisms";
    private static final String SELECT_ORGANISM = "Select an organism to display pathways:";
    private static final String SELECT_PATHWAY = "Select a pathway to display:";
    private static final String SEARCH_RESULTS = "Search Results: ";

    //specifies which page/mode browser is in
    private enum location { ORGANISMS, PATHWAYS, SEARCH};
    private location loc = location.ORGANISMS;

    //true iff browser has ever shown results
    private boolean used = false;
    
    public PathwaysBrowser(WikiPathwaysClient client, Viewer svgPanel, Loader loader) {

        this.wpclient = client;
        this.svgPanel = svgPanel;
        this.loader = loader;

        setLayout(new BorderLayout());

        messageLabel = new JLabel(SELECT_ORGANISM);
        messageLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        add(messageLabel, BorderLayout.NORTH);

        table = new JTable();
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt){
                if (evt.getClickCount() == 2) {
                    int row = table.rowAtPoint(evt.getPoint());
                    row = table.getRowSorter().convertRowIndexToModel(row);
                    if(loc == location.ORGANISMS){
                        listPathways(((OrganismTableModel)table.getModel()).getEntry(row));
                    } else if (loc == location.PATHWAYS){
                        WSPathwayInfo selection = ((PathwayTableModel)table.getModel()).getEntry(row);
                        if(selection == null){
                            listOrganisms();
                        } else {
                            loadPathway(selection.getId());
                        }
                    } else if (loc == location.SEARCH){
                        WSSearchResult selection = ((SearchTableModel)table.getModel()).getEntry(row);
                        loadPathway(selection.getId());
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);

        scrollPane.getViewport().setBackground(Color.WHITE);
        add(scrollPane, BorderLayout.CENTER);
    }

    /*
     * True iff browser has ever shown results
     */
    public boolean hasBeenUsed(){
        return used;
    }

    /*
     * Begin browsing by organism
     */
    public void startBrowse(){
        used = true;
        listOrganisms();
    }

    /*
     * Search for pathways based on text query and organism name. 
     */
    public void startText(final String query, final String organismString){
        used = true;
        startLoad();
        final PathwaysBrowser instance = this;
        Thread thread = new Thread() {
            public void run() {
                try {
                    WSSearchResult[] search = null;
                    if(organismString == null || organismString.equals("") || organismString.equals("All")){
                        search = wpclient.findPathwaysByText(query);
                    } else {
                        search = wpclient.findPathwaysByText(query, Organism.fromLatinName(organismString));
                    }
                    if(search.length == 0){
                        JOptionPane.showMessageDialog(instance, "Your search returned no results. ", "No Results", JOptionPane.ERROR_MESSAGE);
                    }
                    table.setModel(new SearchTableModel(search));
                    messageLabel.setText(SEARCH_RESULTS);
                    loc = location.SEARCH;
                } catch (RemoteException ex) {
                    Logger.getLogger(PathwaysBrowser.class.getName()).log(Level.SEVERE, null, ex);
                }
                endLoad();
            }
        };
        thread.start();
    }

    /*
     * Retrieve names for all organisms in database and display in table. 
     */
    private void listOrganisms(){    
        startLoad();
        Thread thread = new Thread() {
            public void run() {
                String[] organisms = new String[0];
                try {
                    organisms = wpclient.listOrganisms();
                    table.setModel(new OrganismTableModel(organisms, false));
                    loc = location.ORGANISMS;
                    messageLabel.setText(SELECT_ORGANISM);
                } catch (RemoteException ex) {
                    DialogUtils.displayException("WikiPathways Error", "Unable to process request.", ex);
                }
                endLoad();
            }
        };
        thread.start();
    }

    /*
     * Retrieve all pathways for given organism and display in table. 
     */
    private void listPathways(final String organism){
        startLoad();
        Thread thread = new Thread() {
            public void run() {
                WSPathwayInfo[] pathways = new WSPathwayInfo[0];
                try {
                    if(organism.equals(ALL_ORGANISMS)){
                        pathways = wpclient.listPathways();
                    } else {
                        pathways = wpclient.listPathways(Organism.fromLatinName(organism));
                    }
                    table.setModel(new PathwayTableModel(pathways, true));
                    loc = location.PATHWAYS;
                    messageLabel.setText(SELECT_PATHWAY);
                } catch (RemoteException ex) {
                    DialogUtils.displayException("WikiPathways Error", "Unable to process request.", ex);
                }
                endLoad();
            }
        };
        thread.start();
    }
    
    /*
     * Try to load pathway from given id. 
     * Download corresponding svg and gpml files - threaded. 
     */
    public void loadPathway(String pathwayID){
        startLoad();
        
        setSVGDownloaded(false);
        setGPMLDownloaded(false);
        downloadErrorCount = 0;
        svgFilename = DirectorySettings.getTmpDirectory() + System.getProperty("file.separator") + pathwayID + ".svg";
        gpmlFilename = DirectorySettings.getTmpDirectory() + System.getProperty("file.separator") + pathwayID + ".gpml";
        
        (new GetPathwaySwingWorker(pathwayID, svgFilename, "svg")).execute();
        (new GetPathwaySwingWorker(pathwayID, gpmlFilename, "gpml")).execute();
    }
    
    /*
     * If necessary files successfully downloaded, display the pathway. 
     */
    private synchronized void setPathway(){
        if(!svgDownloaded || !gpmlDownloaded) return;
        
        svgPanel.setPathway(new File(svgFilename).toURI(), new File(gpmlFilename).toURI());
        setVisible(false);
        svgPanel.setVisible(true);
        loader.setVisible(false);
    }
    
    /*
     * Set whether the given file is current.
     */
    private void setDownloaded(String which, boolean value){
        if(which.equals("svg")){
            setSVGDownloaded(value);
        } else if (which.equals("gpml")){
            setGPMLDownloaded(value);
        }
    }
    
    private synchronized void setSVGDownloaded(boolean value){
        svgDownloaded = value;
    }
    
    private synchronized void setGPMLDownloaded(boolean value){
        gpmlDownloaded = value;
    }

    /*
     * Set up UI for load. 
     */
    private void startLoad(){
        loader.setMessageLoading();
        loader.setVisible(true);
        svgPanel.setVisible(false);
        this.setVisible(false);
    }

    /*
     * Set up UI for end of load. 
     */
    private void endLoad(){
        loader.setVisible(false);
        setVisible(true);
    }
    
    /*
     * Notify user that loading pathway has failed
     */
    private void loadFailed(String pathwayID){
        if(downloadErrorCount == 0){
            downloadErrorCount++;
        }   
        JOptionPane.showMessageDialog(this, "The pathway '" + pathwayID + "' could not be found.", "Error", JOptionPane.ERROR_MESSAGE);
        loader.setVisible(false);
        svgPanel.setVisible(false);
        setVisible(false);
        downloadErrorCount = 0;
    }
    
    /*
     * SwingWorker for retrieving files from WikiPathways. 
     */
    private class GetPathwaySwingWorker extends SwingWorker {
        
        private String filename;
        private String getPathwayAs;
        private String pathwayID;
        
        public GetPathwaySwingWorker (String pathwayID, String filename, String getPathwayAs){
            this.filename = filename;
            this.getPathwayAs = getPathwayAs;
            this.pathwayID = pathwayID;
        }
        
        @Override
        protected Object doInBackground() {  
            try {
                byte[] fileByte = wpclient.getPathwayAs(getPathwayAs, pathwayID, 0);
                OutputStream out;
                out = new FileOutputStream(filename);
                out.write(fileByte);
                out.close();  
                setDownloaded(getPathwayAs, true);
            } catch (Exception ex) {
                loadFailed(pathwayID);
            }
            return null; 
        }
        
        @Override
        protected void done() {
            setPathway();
        }      
    }
}
