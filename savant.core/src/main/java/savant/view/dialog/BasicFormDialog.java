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
package savant.view.dialog;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import savant.util.swing.SpringUtilities;
import savant.view.swing.Savant;

/**
 *
 * @author jim
 */
public class BasicFormDialog extends JDialog{    
    private JTextField[] fields;
    private static final int INIT_X = 6;
    private static final int INIT_Y = 6;
    private static final int XPAD = 6;
    private static final int YPAD = 6;    
    private boolean wasCancelled = false;
    public String getText(int index){
        return fields[index].getText();
    }
    
    public BasicFormDialog(String[] names, String[] defaultValues, int[] widths){
        this(Savant.getInstance(), names, defaultValues, widths);
    }
        
    public BasicFormDialog(Window parent, String[] names, String[] defaultValues, int[] widths) {
        super(parent);                                       
        JPanel mainPanel =  new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        
        JPanel p = new JPanel(new SpringLayout());
    
        this.fields = new JTextField[names.length];
        for(int i = 0; i < names.length; ++i){
            String name = names[i];
            String defaultValue = ((defaultValues != null) && (i < defaultValues.length)) ? defaultValues[i] : "";
            
            if(name.toLowerCase().contains("password")){                
                fields[i] = new JPasswordField(defaultValue);
            }else{
                fields[i] = new JTextField(defaultValue);
            }                                    
            if(widths != null && i < widths.length){
                fields[i].setColumns(widths[i]);
            }
            JLabel l = new JLabel(name);
            p.add(l);
            l.setLabelFor(fields[i]);
            p.add(fields[i]);                            
        }
        
        SpringUtilities.makeCompactGrid(p,
        names.length, 2,
        INIT_X, INIT_Y,        
        XPAD, YPAD);
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

        JButton OKButton = new JButton("OK");
        OKButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                dispose();
                String[] values = new String[fields.length];
                for(int i = 0; i < fields.length; ++i){
                    values[i] = fields[i].getText();
                }
                submit(values);
            }
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                wasCancelled = true;
                dispose();
            }
        });

        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(OKButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(Box.createHorizontalGlue());
        mainPanel.add(p);
        mainPanel.add(buttonPanel);
        setContentPane(mainPanel);
        pack();
        setLocationRelativeTo(parent);
    }
    
    
    public boolean wasCancelled(){
        return wasCancelled;
    }
    public void submit(String[] values){}
}
