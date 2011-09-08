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


package savant.settings;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import com.jidesoft.dialog.*;
import com.jidesoft.plaf.UIDefaultsLookup;
import com.jidesoft.swing.PartialEtchedBorder;


public class SettingsDialog extends MultiplePageDialog {

    public SettingsDialog(Window parent, String title, Section... sections) throws HeadlessException {
        super((Frame)parent, title);
        setStyle(MultiplePageDialog.LIST_STYLE);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        PageList model = new PageList();
        for (Section s: sections) {
            model.append(s);
        }
        setPageList(model);

        pack();

        for(int i = 0; i < model.getPageCount(); i++){
            ((Section)model.getPage(i)).populate();
        }

        getOkButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (getApplyButton().isEnabled()){
                    applySectionChanges();
                }
            }
        });
        getApplyButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                applySectionChanges();
            }
        });

        setLocationRelativeTo(parent);
    }

    /**
     * Default constructor for our main settings dialog.
     *
     * @param parent the Savant main window
     * @throws HeadlessException
     */
    public SettingsDialog(Window parent) throws HeadlessException {
        this(parent, "Preferences", new ColourSchemeSettingsSection(), new GeneralSettingsSection(), new InterfaceSection(), new RemoteFilesSettingsSection(), new ResolutionSettingsSection());
    }

    private static Border createSeparatorBorder() {
        return new PartialEtchedBorder(EtchedBorder.LOWERED, PartialEtchedBorder.NORTH);
    }

    public static JComponent getHeader(String title) {

        JPanel headerPanel = new JPanel(new BorderLayout(4, 4));
        JLabel label = new JLabel(title);
        headerPanel.add(label, BorderLayout.NORTH);
        JPanel panel = new JPanel();
        panel.setBorder(createSeparatorBorder());
        headerPanel.add(panel, BorderLayout.CENTER);

        return headerPanel;
    }

    @Override
    protected void initComponents() {
        super.initComponents();
        getContentPanel().setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));

        JComponent indexPanel = getIndexPanel();
        if (indexPanel != null) {
            indexPanel.setOpaque(true);
            JLabel label = new JLabel("Category");
            indexPanel.add(label, BorderLayout.BEFORE_FIRST_LINE);
        }
        getButtonPanel().setBorder(BorderFactory.createEmptyBorder(20, 10, 10, 10));
        getPagesPanel().setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
    }

    @Override
    public ButtonPanel createButtonPanel() {
        ButtonPanel buttonPanel = super.createButtonPanel();
        AbstractAction okAction = new AbstractAction(UIDefaultsLookup.getString("OptionPane.okButtonText")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                setDialogResult(RESULT_AFFIRMED);
                setVisible(false);
                dispose();
            }
        };
        AbstractAction cancelAction = new AbstractAction(UIDefaultsLookup.getString("OptionPane.cancelButtonText")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                setDialogResult(RESULT_CANCELLED);
                setVisible(false);
                dispose();
            }
        };
        ((JButton) buttonPanel.getButtonByName(ButtonNames.OK)).setAction(okAction);
        ((JButton) buttonPanel.getButtonByName(ButtonNames.CANCEL)).setAction(cancelAction);
        setDefaultCancelAction(cancelAction);
        setDefaultAction(okAction);
        return buttonPanel;
    }

    @Override
    public JComponent createIndexPanel() {
        if (getPageList().getPageCount() > 1) {
            return super.createIndexPanel();
        }
        return null;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(750, 500);
    }

    private void applySectionChanges() {
        for (int i = 0; i < getPageList().getPageCount(); i++){
            ((Section)getPageList().getPage(i)).applyChanges();
        }
    }
}
