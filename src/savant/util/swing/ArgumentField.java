/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.util.swing;

import javax.swing.JPanel;

/**
 *
 * @author mfiume
 */
public class ArgumentField extends JPanel {

    private ArgumentDescriptor fd;

    /*
    public ArgumentDescriptor getArgumentDescriptor() {
        return this.fd;
    }
     *
     */

    public void setArgumentDescriptor(ArgumentDescriptor fd) {
        this.fd = fd;
    }

    public Object getValue() {
        return null;
    }

    public boolean isFieldEnabled() {
        return fd.getFieldEnabled();
    }

    public String getArgumentName() {
        return fd.getName();
    }

    public String getArgumentDescription() {
        return fd.getFieldDescription();
    }

    public String getArgumentFlag() {
        return fd.getFieldFlag();
    }

}
