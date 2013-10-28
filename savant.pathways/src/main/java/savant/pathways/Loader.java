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

    public void setMessageGeneInfo(float progress){
        setMessage(GENEINFO_MESSAGE + "  " + (int)(progress * 100) + "%");
    }

}
