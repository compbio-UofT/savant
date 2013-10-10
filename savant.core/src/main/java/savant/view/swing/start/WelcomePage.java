/*
 *    Copyright 2011-2012 University of Toronto
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
package savant.view.swing.start;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.TextAttribute;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;

import com.jidesoft.swing.AutoResizingTextArea;
import javax.swing.border.Border;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

import savant.api.util.DialogUtils;
import savant.controller.ProjectController;
import savant.controller.RecentProjectsController;
import savant.settings.BrowserSettings;
import savant.settings.DirectorySettings;
import savant.util.NetworkUtils;
import savant.util.swing.HyperlinkButton;
import savant.view.icon.SavantIconFactory;

/**
 *
 * @author mfiume
 */
public class WelcomePage extends JPanel {

    private static final Color TEXT_COLOR = Color.BLACK;
    private static final Font TITLE_FONT = new Font("Arial", Font.BOLD, 13);
    private static final Font DATE_FONT = new Font("Arial", Font.ITALIC, 12);
    private static final Border INFO_BORDER = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.GRAY, 1), BorderFactory.createLineBorder(Color.WHITE, 10));

    public WelcomePage() {
        setLayout(new BorderLayout());

        this.setOpaque(true);

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);

        p.add(Box.createVerticalStrut(100));
        p.add(new SpiralPanel());
        p.add(align(0, createLabel("v" + BrowserSettings.VERSION + " " + BrowserSettings.BUILD)));
        p.add(Box.createVerticalStrut(20));
        p.add(align(-1, createBoldLabel("Recent Projects")));
        p.add(getRecentProjectsInnerPanel());

        p.add(Box.createVerticalGlue());

        add(getSidePanel(), BorderLayout.WEST);
        add(p, BorderLayout.CENTER);
        add(getSidePanel(), BorderLayout.EAST);
    }

    private static JPanel getSidePanel() {

        int width = (int) Math.round(java.awt.Toolkit.getDefaultToolkit().getScreenSize().getWidth() * .15);
        JPanel p = new JPanel();
        p.setOpaque(false);

        Dimension d = new Dimension(width, 1);
        p.setMinimumSize(d);
        p.setPreferredSize(d);
        return p;
    }

    private JComponent align(int dir, JComponent c) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));

        Component glue = Box.createHorizontalGlue();
        switch (dir) {
            case -1:
                p.add(c);
                p.add(glue);
                break;
            case 0:
                p.add(glue);
                p.add(c);
                p.add(Box.createHorizontalGlue());
                break;
            case 1:
                p.add(glue);
                p.add(c);
                break;
            default:
                p.add(c);
                p.add(glue);
                break;
        }

        return p;
    }

    private JPanel getRecentProjectsInnerPanel() {

        JPanel p = new JPanel();
        p.setBorder(INFO_BORDER);
        p.setBackground(Color.WHITE);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        try {
            List<String> projects = RecentProjectsController.getInstance().getRecentProjects();
            for (final String t : projects) {

                p.add(align(-1, HyperlinkButton.createHyperlinkButton(t, Color.black, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        try {
                            ProjectController.getInstance().loadProjectFromFile(new File(t));
                        } catch (Exception x) {
                            DialogUtils.displayException("Project Error", String.format("<html>Unable to load project <i>%s</i>: %s.</html>", t, x), x);
                        }
                    }
                })));
            }

            if (projects.isEmpty()) {
                p.add(align(-1, createLabel("No recent projects")));
            }
        } catch (IOException ex) {
        }

        return p;
    }

    private JLabel createLabel(String lab) {
        JLabel l = new JLabel(lab);
        l.setOpaque(false);
        l.setForeground(Color.BLACK);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JLabel createBoldLabel(String lab) {
        JLabel l = createLabel(lab);
        l.setFont(l.getFont().deriveFont(Font.BOLD));
        return l;
    }

    private JLabel createSmallLabel(String lab) {
        JLabel l = createLabel(lab);
        l.setFont(l.getFont().deriveFont(Font.PLAIN, 11.0f));
        return l;
    }

    private static class SpiralPanel extends JPanel {

        private final Image img;

        public SpiralPanel() {
            img = SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.LOGO).getImage();
            this.setMinimumSize(new Dimension(999, 150));
            this.setPreferredSize(new Dimension(999, 150));
            this.setMaximumSize(new Dimension(999, 150));
        }

        @Override
        public void paintComponent(Graphics g) {
            //g.setColor(Color.black);
            //g.fillRect(0, 0, this.getWidth(), this.getHeight());
            g.drawImage(img, this.getWidth() / 2 - img.getWidth(null) / 2, this.getHeight() / 2 - img.getHeight(null) / 2, null);
        }
    }

    /*
     private JComponent getNewsInnerPanel() {

     JPanel p = new JPanel();

     try {
     File newsFile = NetworkUtils.downloadFile(BrowserSettings.NEWS_URL, DirectorySettings.getTmpDirectory(), null);
     p = parseNewsFile(newsFile);
     if (newsFile.exists()) { newsFile.delete(); }

     } catch (Exception ex) {
     p.add(this.createLabel("Problem getting news"));
     }

     JScrollPane scroll = new JScrollPane(p);
     scroll.setBorder(INFO_BORDER);
     scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

     return scroll;

     }
     */
    private JPanel parseNewsFile(File newsFile) {
        JPanel p = null;

        try {
            p = new JPanel();
            p.setBackground(Color.WHITE);
            p.setOpaque(true);
            //p.setBackground(Color.red);
            BoxLayout bl = new BoxLayout(p, BoxLayout.Y_AXIS);
            p.setLayout(bl);

            Document d = new SAXBuilder().build(newsFile);
            Element root = d.getRootElement();

            List<Element> newsEntries = root.getChildren("entry");

            Map<TextAttribute, Object> underlining = new HashMap<TextAttribute, Object>();
            underlining.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_LOW_ONE_PIXEL);
            Font underlined = getFont().deriveFont(underlining);

            for (Element e : newsEntries) {

                final String text = e.getChildText("description");

                AutoResizingTextArea ta = new AutoResizingTextArea(text);
                ta.setMaximumSize(new Dimension(99999, 1));
                ta.setForeground(TEXT_COLOR);
                ta.setLineWrap(true);
                //ta.setHighlighter(new BlankHighlighter());
                ta.addMouseListener(new MouseListener() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        e.consume();
                    }

                    @Override
                    public void mousePressed(MouseEvent e) {
                        e.consume();
                    }

                    @Override
                    public void mouseReleased(MouseEvent e) {
                        e.consume();
                    }

                    @Override
                    public void mouseEntered(MouseEvent e) {
                        e.consume();
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        e.consume();
                    }
                });
                ta.setOpaque(false);
                ta.setWrapStyleWord(true);
                ta.setEditable(false);

                JLabel title = new JLabel(e.getChildText("title"));
                title.setFont(TITLE_FONT);
                title.setForeground(TEXT_COLOR);

                JLabel date = new JLabel(e.getChildText("date"));
                date.setFont(DATE_FONT);
                date.setForeground(TEXT_COLOR);

                p.add(Box.createVerticalStrut(10));

                title.setAlignmentX(Component.LEFT_ALIGNMENT);
                p.add(title);
                date.setAlignmentX(Component.LEFT_ALIGNMENT);
                p.add(date);
                ta.setAlignmentX(Component.LEFT_ALIGNMENT);
                p.add(ta);

                final String more = e.getChildText("more");
                if (more != null && more.length() > 0) {
                    JLabel link = new JLabel("More...");
                    link.setForeground(TEXT_COLOR);
                    link.setFont(underlined);
                    link.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            try {
                                Desktop.getDesktop().browse(URI.create(more));
                            } catch (Exception x) {
                            }
                        }
                    });
                    p.add(link);
                }
            }

            p.add(Box.createVerticalGlue());

        } catch (Exception e) {
            JLabel l = new JLabel("Problem getting news");
            l.setForeground(TEXT_COLOR);
            p.add(l);
        }

        return p;
    }
}
