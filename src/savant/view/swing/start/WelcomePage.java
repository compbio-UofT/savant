package savant.view.swing.start;

import com.jidesoft.swing.AutoResizingTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
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
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
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
import savant.view.swing.start.StartPanel.BlankHighlighter;

/**
 *
 * @author mfiume
 */
public class WelcomePage extends JPanel {

    private Color TEXT_COLOR = Color.black;
    private Font TITLE_FONT = new Font("Arial",Font.BOLD,13);
    private Font DATE_FONT = new Font("Arial",Font.ITALIC,12);

    public WelcomePage() {
        //this.setBackground(Color.white);
        this.setLayout(new BorderLayout());

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
        p.setOpaque(false);

        p.add(Box.createVerticalStrut(70));
        p.add(new SpiralPanel());
        p.add(align(0,createLabel("v" + BrowserSettings.VERSION + " " + BrowserSettings.BUILD)));
        p.add(Box.createVerticalStrut(20));
        p.add(align(-1,createBoldLabel("Recent Projects")));
        p.add(stretch(-1,getRecentProjectsInnerPanel()));
        p.add(Box.createVerticalStrut(10));
        p.add(align(-1,createBoldLabel("Recent News")));
        p.add(getNewsInnerPanel());
        p.add(Box.createVerticalGlue());
        p.add(Box.createVerticalStrut(10));
        p.add(align(0,createSmallLabel("Developed by the Computational Biology Lab at University of Toronto")));
        p.add(Box.createVerticalStrut(70));

        this.add(getSidePanel(),BorderLayout.WEST);
        this.add(p,BorderLayout.CENTER);
        this.add(getSidePanel(),BorderLayout.EAST);
    }

    private static JPanel getSidePanel() {

        int width = (int) Math.round(java.awt.Toolkit.getDefaultToolkit().getScreenSize().getWidth()*.15);
        JPanel p = new JPanel();
        p.setOpaque(false);

        Dimension d = new Dimension(width,1);
        p.setMinimumSize(d);
        p.setPreferredSize(d);
        return p;
    }

    private JComponent stretch(int dir, JComponent c) {

        /*
        switch (dir) {
            case -1:
                c.setMinimumSize(new Dimension(999,1));
                break;
            case 1:
                c.setMinimumSize(new Dimension(1,999));
                break;
            default:
                c.setMinimumSize(new Dimension(999,1));
                break;
        }
         *
         */

        return c;
    }

    private JComponent align(int dir, JComponent c) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p,BoxLayout.X_AXIS));

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

        JPanel pan = new InfoPanel();
        //pan.setOpaque(false);
        pan.setLayout(new BoxLayout(pan, BoxLayout.Y_AXIS));

        try {
            List<String> projects = RecentProjectsController.getInstance().getRecentProjects();
            for (final String t : projects) {

                pan.add(align(-1,HyperlinkButton.createHyperlinkButton(t, Color.black, new ActionListener() {

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
                pan.add(align(-1,createLabel("No recent projects")));
            }
        } catch (IOException ex) {
        }

        return pan;
    }

    private JLabel createLabel(String lab) {
        JLabel l = new JLabel(lab);
        l.setOpaque(false);
        l.setForeground(Color.black);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);

        //l.setFont(new Font("Arial", Font.PLAIN, 12));
        return l;
    }

    private JLabel createBoldLabel(String lab) {
        JLabel l = createLabel(lab);
        Font f = l.getFont();
        l.setFont(new Font(f.getFamily(),Font.BOLD,f.getSize()));
        return l;
    }

    private JLabel createSmallLabel(String lab) {
        JLabel l = createLabel(lab);
        Font f = l.getFont();
        l.setFont(new Font(f.getFamily(),Font.PLAIN,11));
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

        public void paintComponent(Graphics g) {
            //g.setColor(Color.black);
            //g.fillRect(0, 0, this.getWidth(), this.getHeight());
            g.drawImage(img, this.getWidth()/2-img.getWidth(null)/2, this.getHeight()/2-img.getHeight(null)/2, null);
        }
    }

    private static class InfoPanel extends JPanel {
        public InfoPanel() {
            //this.setPreferredSize(new Dimension(400,10));
            this.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.gray, 1),
                    BorderFactory.createEmptyBorder(10,10,10,10)));
        }

        public void paintComponent(Graphics g) {
            g.setColor(Color.white);
            g.fillRect(0, 0, this.getWidth(), this.getHeight());
        }
    }

        private JComponent getNewsInnerPanel() {

        JPanel p = new InfoPanel();

        try {
            File newsFile = NetworkUtils.downloadFile(BrowserSettings.NEWS_URL, DirectorySettings.getTmpDirectory(), null);
            p = parseNewsFile(newsFile);
            if (newsFile.exists()) { newsFile.delete(); }

        } catch (Exception ex) {
            p.add(this.createLabel("Problem getting news"));
        }

        JScrollPane scroll = new JScrollPane(p);
        scroll.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.gray, 1),
                    BorderFactory.createEmptyBorder(10,10,10,10)));
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        return scroll;

    }

    private JPanel parseNewsFile(File newsFile) {
        JPanel p = null;

        try {
            p = new JPanel();
            p.setOpaque(false);
            //p.setBackground(Color.red);
            BoxLayout bl = new BoxLayout(p,BoxLayout.Y_AXIS);
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
                ta.setMaximumSize(new Dimension(99999,1));
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
