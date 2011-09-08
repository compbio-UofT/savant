/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package savant.util.swing;

import java.awt.Dimension;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

/**
 *
 * @author mfiume
 */
public class DelimiterGroup extends JPanel {

    JRadioButton tabButton;
    JRadioButton commaButton;
    JRadioButton spaceButton;
    JRadioButton otherButton;
    JTextField otherTextField;

    public enum Delimiter {

        TAB, COMMA, SPACE, OTHER
    }

    public DelimiterGroup(Delimiter delimiter) {

        ButtonGroup g = new ButtonGroup();

        tabButton = new JRadioButton("Tab");
        commaButton = new JRadioButton("Comma");
        spaceButton = new JRadioButton("Space");
        otherButton = new JRadioButton("Other:");
        otherTextField = new JTextField();
        otherTextField.setPreferredSize(new Dimension(30,20));
        otherTextField.addFocusListener(new FocusListener() {

            @Override
            public void focusGained(FocusEvent e) {
                otherButton.setSelected(true);
            }

            @Override
            public void focusLost(FocusEvent e) {
            }

        });

        g.add(tabButton);
        g.add(commaButton);
        g.add(spaceButton);
        g.add(otherButton);

        this.add(tabButton);
        this.add(commaButton);
        this.add(spaceButton);
        this.add(otherButton);

        this.add(otherTextField);

        switch (delimiter) {
            case TAB:
                tabButton.setSelected(true);
                break;
            case COMMA:
                commaButton.setSelected(true);
                break;
            case SPACE:
                spaceButton.setSelected(true);
                break;
            case OTHER:
                otherButton.setSelected(true);
                break;

        }

    }

    public char getDelimiter() {
        if (tabButton.isSelected()) {
            return '\t';
        } else if (commaButton.isSelected()) {
            return ',';
        } else if (spaceButton.isSelected()) {
            return ' ';
        } else {
            return otherTextField.getText().charAt(0);
        }
    }
}
