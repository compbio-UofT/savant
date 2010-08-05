/*
 *    Copyright 2010 University of Toronto
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

/*
 * DialogUtils.java
 * Created on Aug 5, 2010
 */

package savant.view.swing.util;

import com.jidesoft.dialog.JideOptionPane;
import savant.util.MiscUtils;
import savant.view.swing.Savant;

import javax.swing.*;

/**
 * Some utility methods for displaying message dialogs to the user.
 *
 * @author vwilliams
 */
public class DialogUtils {

    public static void displayMessage(String message) {
        JOptionPane.showMessageDialog(Savant.getInstance(), message);
    }

    public static void displayException(String title, String message, Throwable t) {
        JideOptionPane optionPane = new JideOptionPane(message, JOptionPane.ERROR_MESSAGE, JideOptionPane.CLOSE_OPTION);
        optionPane.setTitle(title);
        JDialog dialog = optionPane.createDialog(Savant.getInstance(),"Error encountered");
        dialog.setResizable(true);
        String details = t.getMessage() + "\r\n" + MiscUtils.getStackTrace(t);
        optionPane.setDetails(details);
        //optionPane.setDetailsVisible(true);
        dialog.pack();
        dialog.setVisible(true);
    }
}
