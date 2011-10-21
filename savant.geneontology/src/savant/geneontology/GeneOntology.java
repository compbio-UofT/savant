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
package savant.geneontology;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.*;

import savant.api.SavantPanelPlugin;

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
