/*
 * PluginManagerDialog.java
 *
 * Created on Mar 9, 2010, 10:11:36 AM
 *
 *
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

package savant.view.dialog;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Window;
import java.io.File;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.util.DialogUtils;
import savant.controller.PluginController;
import savant.settings.BrowserSettings;
import savant.settings.DirectorySettings;
import savant.util.MiscUtils;
import savant.util.NetworkUtils;
import savant.view.dialog.tree.PluginRepositoryDialog;


/**
 *
 * @author mfiume
 */
public class PluginManagerDialog extends JDialog {

    private static Log LOG = LogFactory.getLog(PluginManagerDialog.class);

    private static PluginManagerDialog instance;

    private PluginBrowser browser;
    private PluginRepositoryDialog repositoryBrowser;

    public static PluginManagerDialog getInstance() {
        if (instance == null) {
            instance = new PluginManagerDialog(DialogUtils.getMainWindow());
        }
        return instance;
    }

    /** Creates new form PluginManager */
    private PluginManagerDialog(Window parent) {
        super(parent, "Plugin Manager", Dialog.ModalityType.APPLICATION_MODAL);
        initComponents();
        MiscUtils.registerCancelButton(closeButton);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(false);
        browser = new PluginBrowser();
        browserPanel.add(new JScrollPane(browser), BorderLayout.CENTER);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.JLabel pluginsCaption = new javax.swing.JLabel();
        javax.swing.JButton fromFileButton = new javax.swing.JButton();
        javax.swing.JButton fromRepositoryButton = new javax.swing.JButton();
        browserPanel = new javax.swing.JPanel();
        closeButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setIconImage(null);
        setIconImages(null);

        pluginsCaption.setText("Installed Plugins");

        fromFileButton.setText("Install from File");
        fromFileButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fromFileButtonActionPerformed(evt);
            }
        });

        fromRepositoryButton.setText("Install from Repository");
        fromRepositoryButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fromRepositoryButtonActionPerformed(evt);
            }
        });

        browserPanel.setBackground(new java.awt.Color(255, 255, 255));
        browserPanel.setLayout(new java.awt.BorderLayout());

        closeButton.setText("Cancel");
        closeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(browserPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 549, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(pluginsCaption)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 302, Short.MAX_VALUE))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(closeButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(fromRepositoryButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)))
                        .addComponent(fromFileButton)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(pluginsCaption)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(browserPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 243, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(fromFileButton)
                    .addComponent(closeButton)
                    .addComponent(fromRepositoryButton))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void fromFileButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fromFileButtonActionPerformed
        File selectedFile = DialogUtils.chooseFileForOpen("Select Plugin JAR", null, null);
        if (selectedFile != null) {
            try {
                PluginController.getInstance().installPlugin(selectedFile);
            } catch (Throwable x) {
                DialogUtils.displayException("Installation Error", String.format("<html>Unable to install plugin from <i>%s</i>: %s.</html>", selectedFile.getName(), x), x);
            }
        }
    }//GEN-LAST:event_fromFileButtonActionPerformed

    private void fromRepositoryButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fromRepositoryButtonActionPerformed
        try {
            if (repositoryBrowser == null) {
                File file = NetworkUtils.downloadFile(BrowserSettings.PLUGIN_URL, DirectorySettings.getTmpDirectory(), null);
                repositoryBrowser = new PluginRepositoryDialog(this, "Install Plugins", "Install", file);
            }
            repositoryBrowser.setVisible(true);
        } catch (Exception x) {
            DialogUtils.displayException("Installation Error", String.format("<html>Problem downloading file <i>%s</i>.</html>", BrowserSettings.PLUGIN_URL), x);
        }
    }//GEN-LAST:event_fromRepositoryButtonActionPerformed

    private void closeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeButtonActionPerformed
        setVisible(false);
    }//GEN-LAST:event_closeButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel browserPanel;
    private javax.swing.JButton closeButton;
    // End of variables declaration//GEN-END:variables

}
