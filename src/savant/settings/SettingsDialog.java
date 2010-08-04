package savant.settings;


import com.jidesoft.dialog.*;
import com.jidesoft.plaf.LookAndFeelFactory;
import com.jidesoft.plaf.UIDefaultsLookup;
import com.jidesoft.swing.JideSwingUtilities;

import com.jidesoft.swing.PartialEtchedBorder;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

public class SettingsDialog extends MultiplePageDialog {

    private static PageList model = new PageList();;

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
            public void actionPerformed(ActionEvent e) {
                setDialogResult(RESULT_AFFIRMED);
                setVisible(false);
                dispose();
            }
        };
        AbstractAction cancelAction = new AbstractAction(UIDefaultsLookup.getString("OptionPane.cancelButtonText")) {
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

    public static void showOptionsDialog() {

        final MultiplePageDialog dialog = new SettingsDialog(null, "Preferences");
        dialog.setStyle(MultiplePageDialog.LIST_STYLE);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        dialog.setPageList(model);

        dialog.pack();
        JideSwingUtilities.globalCenterWindow(dialog);
        dialog.setVisible(true);
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
