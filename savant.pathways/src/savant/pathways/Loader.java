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
import java.awt.Font;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 *
 * @author AndrewBrook
 */
public class Loader extends JPanel {

    private JLabel label;
    
    private static final String ERROR_MESSAGE = "There was an error processing your request...";
    private static final String LOADING_MESSAGE = "Your request is being processed...";
    private static final String GENEINFO_MESSAGE = "Getting gene information...";
    

    public Loader(){

        label = new JLabel("", SwingConstants.CENTER);
        label.setVerticalTextPosition(SwingConstants.CENTER);
        label.setHorizontalTextPosition(SwingConstants.CENTER);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 12));

        this.setLayout(new BorderLayout());
        this.add(label, BorderLayout.CENTER);
    }

    /*
     * Change the message. 
     */
    public void setMessage(String message){
        label.setText(message);
    }
    
    public void setMessageAndShow(String message){
        label.setText(message);
        this.setVisible(true);
    }
    
    //CONVENIENCE FUNCTIONS
    
    public void setMessageLoading(){
        setMessage(LOADING_MESSAGE);
    }
    
    public void setMessageError(){
        setMessage(ERROR_MESSAGE);
    }

    public void setMessageGeneInfo(){
        setMessage(GENEINFO_MESSAGE);
    }

}
