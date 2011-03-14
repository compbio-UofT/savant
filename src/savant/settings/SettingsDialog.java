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
import com.jidesoft.swing.JideSwingUtilities;
import com.jidesoft.swing.PartialEtchedBorder;


public class SettingsDialog extends MultiplePageDialog {

    private static PageList model = new PageList();

    public SettingsDialog(Frame owner, String title) throws HeadlessException {
        super(owner, title);
    }

    private static Border createSeparatorBorder() {
        return new PartialEtchedBorder(EtchedBorder.LOWERED, PartialEtchedBorder.NORTH);
    }

    public static JComponent getHeader(String title) {

        JPanel headerPanel = new JPanel(new BorderLayout(4, 4));
        JLabel label = new JLabel(title);
        headerPanel.add(label, BorderLayout.BEFORE_FIRST_LINE);
        JPanel panel = new JPanel();
        panel.setBorder(createSeparatorBorder());
        headerPanel.add(panel, BorderLayout.CENTER);

        return headerPanel;
    }

    @Override
    protected void initComponents() {
        super.initComponents();
        getContentPanel().setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        getIndexPanel().setOpaque(true);
        JLabel label = new JLabel("Category");
        getIndexPanel().add(label, BorderLayout.BEFORE_FIRST_LINE);
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
    public Dimension getPreferredSize() {
        return new Dimension(600, 500);
    }

    public static void showOptionsDialog(Frame f) {

        final SettingsDialog dialog = new SettingsDialog(f, "Preferences");
        dialog.setStyle(MultiplePageDialog.LIST_STYLE);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        
        dialog.setPageList(model);      
        dialog.pack();

        for(int i = 0; i < dialog.getPageList().getPageCount(); i++){
            ((Section)dialog.getPageList().getPage(i)).populate();
        }

        dialog.getOkButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (dialog.getApplyButton().isEnabled()){
                    dialog.applySectionChanges();
                }               
            }
        });
        dialog.getApplyButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.applySectionChanges();
            }
        });

        JideSwingUtilities.globalCenterWindow(dialog);
        dialog.setVisible(true);
    }

    private void applySectionChanges() {
        for (int i = 0; i < model.getPageCount(); i++){
            ((Section)getPageList().getPage(i)).applyChanges();
        }
    }

    public static void addSection(Section s) {
        model.append(s);
    }

        public static class ListOptionPage extends AbstractDialogPage {
        public ListOptionPage(String name) {
            super(name);
        }

        public ListOptionPage(String name, Icon icon) {
            super(name, icon);
        }

        @Override
        public void lazyInitialize() {
            initComponents();
        }

        public void initComponents() {
            JPanel headerPanel = new JPanel(new BorderLayout(4, 4));
            JLabel label = new JLabel(getTitle());
            headerPanel.add(label, BorderLayout.BEFORE_FIRST_LINE);
            JPanel panel = new JPanel();
            //panel.setBorder(createSeparatorBorder());
            headerPanel.add(panel, BorderLayout.CENTER);

            setLayout(new BorderLayout());
            add(headerPanel, BorderLayout.BEFORE_FIRST_LINE);
            add(new JLabel("This is just a demo. \"" + getFullTitle() + "\" page is not implemented yet.", JLabel.CENTER), BorderLayout.CENTER);
        }

    }
}
