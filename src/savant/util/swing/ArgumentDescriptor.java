/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.util.swing;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 *
 * @author mfiume
 */
public class ArgumentDescriptor extends JPanel {

    private String title;
    private String flag;
    private String description;

    private  JCheckBox cb;
    private JLabel flagLabel;
    private JLabel titleLabel;

    public ArgumentDescriptor(String title, String flag, String description, boolean isOptional, boolean isEnabled) {
        this.description = description;
        this.title = title;
        this.flag = flag;
        this.titleLabel = new JLabel(title);
        this.flagLabel = new JLabel(flag);
        cb = new JCheckBox();
        cb.setSelected(isEnabled);
        cb.setEnabled(isOptional);

        this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        this.add(cb);
        this.add(Box.createHorizontalStrut(5));
        this.add(this.flagLabel);
        this.add(Box.createHorizontalStrut(5));
        this.add(this.titleLabel);
        this.add(Box.createHorizontalStrut(5));
        this.titleLabel.setToolTipText(description);
    }

    public boolean getFieldEnabled() {
        return cb.isSelected();
    }

    public String getFieldName() {
        return title;
    }

    public String getFieldFlag() {
        return flag;
    }

    public String getFieldDescription() {
        return description;
    }
}
