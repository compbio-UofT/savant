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
package savant.util;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import savant.view.dialog.BasicFormDialog;

public class SavantHTTPAuthenticator extends Authenticator {   
    
    //If user clicked 'cancel' on authentication dialog, wait at least this
    //long (in ms) before showing the dialog again.
    private static final long SHOW_DIALOG_INTERVAL = 1000;
    private long cancelledTime = 0;
    @Override
    protected PasswordAuthentication getPasswordAuthentication() {
        if((System.currentTimeMillis() - cancelledTime) < SHOW_DIALOG_INTERVAL){
            return null;
        }
        String host = getRequestingHost();
        int port = getRequestingPort();

        BasicFormDialog bfd = new BasicFormDialog(new String[]{"Username", "Password"}, null, new int[]{25, 25});
        bfd.setTitle(host + ":" + port + " requires authentication");
        bfd.setModal(true);
        bfd.setVisible(true);
        if(bfd.wasCancelled()){            
            cancelledTime = System.currentTimeMillis();
            return null;
        }else{
            return new PasswordAuthentication(bfd.getText(0), bfd.getText(1).toCharArray());    
        }        
    }
}
