/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.swing.component;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 *
 * @author mfiume
 */
public class StringArgumentField extends ArgumentField {

    JTextField tf;

    public StringArgumentField(String title, String flag, String description) {
        this(title, flag, description, true, true, "");
    }

    public StringArgumentField(String title, String flag, String description, boolean isOptional, boolean isEnabled, String value) {

        tf = new JTextField();
        tf.setText(value);

        ArgumentDescriptor ad = new ArgumentDescriptor(title, flag, description, isOptional, isEnabled);
        this.setArgumentDescriptor(ad);

        this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        this.add(ad);
        this.add(tf);
        
    }

    @Override
    public String getValue() {
        return tf.getText();
    }

}
