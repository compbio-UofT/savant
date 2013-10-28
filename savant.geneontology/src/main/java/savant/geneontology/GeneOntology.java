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
package savant.geneontology;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.*;

import savant.plugin.SavantPanelPlugin;

/**
 * Plugin to access the Gene Ontology website.
 *
 * @author nnursimulu
 */
public class GeneOntology extends SavantPanelPlugin {
    
    /**
     * Progress bar for when loading info.
     */
    JProgressBar progressBar;
    JPanel container;
    
    class Task extends SwingWorker{
        
        // Add listener.
        PropertyChangeListener listener = new Listener();     
        ParentChildrenTree tree;
        
        Task(){
            // add a listener to this task to monitor what to do about progress bar.
            this.addPropertyChangeListener(listener);
        }

        @Override
        protected Object doInBackground() throws Exception {

            // Create the file containing the mapping of GO IDs to RefSeq IDs and 
            // genomic locations at a destination.
            try{

                // Get the location of the mapping file.
                String destination = CreateMappings.getMappings();
                // create the tree, and add it to the frame.
                tree = ParentChildrenTree.getTree(destination);
                container.add(tree, BorderLayout.CENTER);
            }
            catch(Exception e){

                container.add(new JLabel("Could not load Gene Ontology plug-in"), BorderLayout.CENTER);
                System.out.println(e);
            }
            // say when we are done
            setProgress(100);
            return null;
            
        }
        
    }
    
    /**
     * To listen on to progress with the task and to determine what to do with 
     * the progress bar.
     */
    class Listener implements PropertyChangeListener{

        @Override
        /**
         * Detects a property change.
         */
        public void propertyChange(PropertyChangeEvent evt) {
            
            // If we are done, we are done.
            if ("progress".equals(evt.getPropertyName()) && 
                    (Integer)(evt.getNewValue()) == 100){
                
                progressBar.setIndeterminate(false);
                progressBar.setVisible(false);
            }
            // If we are not done, we are not done.
            else if ("state".equals(evt.getPropertyName()) && 
                    "STARTED".equals(evt.getNewValue() + "")){

                progressBar.setIndeterminate(true);
                // Once we are done, add the tree to the canvas.
                // We don't need the progress bar anymore.
                progressBar.setVisible(true); 
            }            
        }
        
    }

    @Override
    public void init(JPanel canvas) {


        // Create the progress bar.
        progressBar = new JProgressBar();
//        progressBar.setStringPainted(false);
        container = new JPanel(new BorderLayout());
        container.add(progressBar, BorderLayout.NORTH);
        canvas.add(container); 
        
        // Start the task.
        Task task = new Task();
        task.execute(); 
    }

    protected void doStart() throws Exception {

    }

    protected void doStop() throws Exception {

    }

    @Override
    public String getTitle() {
        return "Gene Ontology Plugin";
    }
}
