/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.view.swing;

import java.io.File;
import java.io.FilenameFilter;

/**
 *
 * @author mfiume
 */
public class SavantFileFilter implements FilenameFilter {
    
    String[] ext = {"savant","bam"};

    @Override
    public boolean accept(File dir, String name) {
        for (int i = 0; i < ext.length; i++) {
            if (name.endsWith(ext[i])) { return true; }
        }
        return false;
    }

}
