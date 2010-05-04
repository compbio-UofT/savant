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
    
    String ext = "savant";

    @Override
    public boolean accept(File dir, String name) {
        return name.endsWith(ext);
    }

}
