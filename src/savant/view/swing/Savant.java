/*
 *    Copyright 2009-2010 University of Toronto
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
package savant.view.swing;

import com.jidesoft.dialog.JideOptionPane;
import com.jidesoft.docking.*;
import com.jidesoft.docking.event.DockableFrameEvent;
import com.jidesoft.docking.event.DockableFrameListener;
import com.jidesoft.plaf.LookAndFeelFactory;
import com.jidesoft.plaf.UIDefaultsLookup;
import com.jidesoft.plaf.basic.ThemePainter;
import com.jidesoft.status.MemoryStatusBarItem;
import com.jidesoft.swing.JideSplitPane;
import java.beans.PropertyVetoException;
import org.java.plugin.ObjectFactory;
import org.java.plugin.PluginManager;
import org.java.plugin.registry.Extension;
import org.java.plugin.registry.ExtensionPoint;
import org.java.plugin.registry.PluginDescriptor;
import org.java.plugin.standard.StandardPluginLocation;
import savant.analysis.BatchAnalysisForm;
import savant.controller.*;
import savant.controller.event.bookmark.BookmarksChangedEvent;
import savant.controller.event.bookmark.BookmarksChangedListener;
import savant.controller.event.range.RangeChangedEvent;
import savant.controller.event.range.RangeChangedListener;
import savant.controller.event.rangeselection.RangeSelectionChangedEvent;
import savant.controller.event.rangeselection.RangeSelectionChangedListener;
import savant.controller.event.reference.ReferenceChangedEvent;
import savant.controller.event.reference.ReferenceChangedListener;
import savant.controller.event.track.TrackListChangedEvent;
import savant.controller.event.track.TrackListChangedListener;
import savant.format.header.FileType;
import savant.model.Genome;
import savant.plugin.GUIPlugin;
import savant.plugin.PluginAdapter;
import savant.settings.BrowserSettings;
import savant.settings.ColourSchemeSettingsSection;
import savant.settings.ColourSettings;
import savant.settings.SettingsDialog;
import savant.util.MiscUtils;
import savant.util.Range;
import savant.view.dialog.DataFormatForm;
import savant.view.dialog.GenomeLengthForm;
import savant.view.dialog.OpenURLDialog;
import savant.view.dialog.PluginDialog;
import savant.view.swing.sequence.SequenceViewTrack;
import savant.view.swing.util.DialogUtils;
import savant.view.swing.util.ScreenShot;
import savant.view.tools.ToolsModule;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import savant.plugin.ToolPlugin;
import savant.settings.TemporaryFilesSettingsSection;

/**
 * Main application Window (Frame).
 *
 * @author mfiume
 */
public class Savant extends javax.swing.JFrame implements ComponentListener, RangeSelectionChangedListener,
        RangeChangedListener, PropertyChangeListener, BookmarksChangedListener, 
        ReferenceChangedListener, TrackListChangedListener {

    private static boolean isDebugging = false;
    private DockingManager auxDockingManager;
    private JPanel masterPlaceholderPanel;
    private DockingManager trackDockingManager;
    private JPanel trackPanel;
    private JPanel menuPanel;

    private JButton button_genome;
    private JButton trackButton;
    private JButton goButton;

    private ToolsModule savantTools;

    private static boolean showNonGenomicReferenceDialog = true;
    private static boolean showBookmarksChangedDialog = true;

    public static String os = System.getProperty("os.name").toLowerCase();
    public static boolean mac = os.contains("mac");
    public static int osSpecificModifier = (mac ? java.awt.event.InputEvent.META_MASK : java.awt.event.InputEvent.CTRL_MASK);

    private static int groupNum = 0;
    private static Map<DockableFrame,Frame> dockFrameToFrameMap = new HashMap<DockableFrame,Frame>();
    private DockableFrame genomeFrame = null;

    private DataFormatForm dff;
    private boolean openAfterFormat;

    private OpenURLDialog urlDialog;

    
    private void addTrackFromFile(String selectedFileName) throws IOException {

        Savant.log("Loading track " + selectedFileName, Savant.LOGMODE.NORMAL);

        // Some types of track actually create more than one track per frame, e.g. BAM
        List<ViewTrack> tracks = ViewTrack.create(selectedFileName);

        if (tracks != null  && tracks.size() > 0) {
            Frame frame = null;
            DockableFrame df = DockableFrameFactory.createTrackFrame(MiscUtils.getFilenameFromPath(selectedFileName));
            JPanel panel = (JPanel) df.getContentPane();
            if (!tracks.isEmpty()) {

                //////////////////////////////////////////////////

                panel.setLayout(new BorderLayout());
                //JLayeredPane layers = new JLayeredPane();
                //layers.setLayout(new BorderLayout());
                //panel.add(layers);

                //////////////////////////////////////////////////

                
                frame = new Frame(tracks, df.getName());
                JLayeredPane layers = (JLayeredPane) frame.getFrameLandscape();
                panel.add(layers);
                frame.setDockableFrame(df);
                for (ViewTrack t : tracks) {
                    t.setFrame(frame);
                }
                dockFrameToFrameMap.put(df, frame);
            }
            FrameController.getInstance().addFrame(frame, panel);
            this.getTrackDockingManager().addFrame(df);

            Savant.log("Track loaded", Savant.LOGMODE.NORMAL);
        }
        
    }

    /** == [[ DOCKING ]] ==
     *  Components (such as frames, the Task Pane, etc.)
     *  can be docked to regions of the UI
     */
    private void initDocking() {

        masterPlaceholderPanel = new JPanel();
        masterPlaceholderPanel.setLayout(new BorderLayout());

        this.panel_main.setLayout(new BorderLayout());
        this.panel_main.add(masterPlaceholderPanel,BorderLayout.CENTER);

        auxDockingManager = new DefaultDockingManager(this,masterPlaceholderPanel);
        masterPlaceholderPanel.setBackground(ColourSettings.colorSplitter);
        //auxDockingManager.setSidebarRollover(false);
        auxDockingManager.getWorkspace().setBackground(ColourSettings.colorSplitter);
        auxDockingManager.setInitSplitPriority(DockingManager.SPLIT_EAST_SOUTH_WEST_NORTH);
        auxDockingManager.loadLayoutData();

        trackPanel = new JPanel();
        trackPanel.setLayout(new BorderLayout());

        auxDockingManager.getWorkspace().add(trackPanel,BorderLayout.CENTER);

        trackDockingManager = new DefaultDockingManager(this,trackPanel);
        trackPanel.setBackground(ColourSettings.colorSplitter);
        trackDockingManager.getWorkspace().setBackground(ColourSettings.colorSplitter);
        //trackDockingManager.setSidebarRollover(false);
        trackDockingManager.getWorkspace().setBackground(Color.red);
        trackDockingManager.setInitNorthSplit(JideSplitPane.VERTICAL_SPLIT);
        trackDockingManager.loadLayoutData();

        rangeSelector = new RangeSelectionPanel();
        rangeSelector.setPreferredSize(new Dimension(10000, 23));
        rangeSelector.setMaximumSize(new Dimension(10000, 23));
        rangeController.addRangeChangedListener(rangeSelector);
        rangeSelector.addRangeChangedListener(this);
        rangeSelector.setActive(false);
        rangeSelector.setVisible(false);
        //trackPanel.add(rangeSelector,BorderLayout.NORTH);


        ruler = new MiniRangeSelectionPanel();
        rangeController.addRangeChangedListener(ruler);
        ruler.addRangeChangedListener(this);
        ruler.setActive(false);
        ruler.setVisible(false);
        //trackPanel.add(ruler,BorderLayout.NORTH);

        JPanel selectorsContainer = new JPanel();
        selectorsContainer.setLayout(new BoxLayout(selectorsContainer, BoxLayout.Y_AXIS));

        selectorsContainer.add(rangeSelector);
        selectorsContainer.add(ruler);

        trackPanel.add(selectorsContainer, BorderLayout.NORTH);

        JPanel p = new JPanel();
        p.setBackground(Color.darkGray);
        trackDockingManager.getWorkspace().add(p);
        trackDockingManager.addDockableFrameListener(new DockableFrameListener(){

            public void dockableFrameAdded(DockableFrameEvent arg0) {
            }

            public void dockableFrameRemoved(DockableFrameEvent arg0) {
                FrameController.getInstance().closeFrame(dockFrameToFrameMap.get(arg0.getDockableFrame()));
            }

            public void dockableFrameShown(DockableFrameEvent arg0) {
            }

            public void dockableFrameHidden(DockableFrameEvent arg0) {
            }

            public void dockableFrameDocked(DockableFrameEvent arg0) {
            }

            public void dockableFrameFloating(DockableFrameEvent arg0) {
            }

            public void dockableFrameAutohidden(DockableFrameEvent arg0) {
            }

            public void dockableFrameAutohideShowing(DockableFrameEvent arg0) {
            }

            public void dockableFrameActivated(DockableFrameEvent arg0) {
                dockFrameToFrameMap.get(arg0.getDockableFrame()).setActiveFrame();
            }

            public void dockableFrameDeactivated(DockableFrameEvent arg0) {
                dockFrameToFrameMap.get(arg0.getDockableFrame()).setInactiveFrame();
            }

            public void dockableFrameTabShown(DockableFrameEvent arg0) {
            }

            public void dockableFrameTabHidden(DockableFrameEvent arg0) {
            }

            public void dockableFrameMaximized(DockableFrameEvent arg0) {
            }

            public void dockableFrameRestored(DockableFrameEvent arg0) {
            }

            public void dockableFrameTransferred(DockableFrameEvent arg0) {
            }

        });
        trackDockingManager.setAllowedDockSides(DockContext.DOCK_SIDE_HORIZONTAL);

        // make sure only one active frame
        addDockingManagerToGroup(auxDockingManager);
        addDockingManagerToGroup(trackDockingManager);
        //DockingManagerGroup dmg = new DockingManagerGroup();
        //dmg.add(auxDockingManager);
        //dmg.add(trackDockingManager);
    }

    /** Minimum and maximum dimensions of the browser form */
    static int minimumFormWidth = 500;
    static int minimumFormHeight = 500;

    /** The loaded genome */
    //private Genome loadedGenome;


    /** The log */
    private static JTextArea log;

    /**
     * Range Controls
     */
    /** Controls (buttons, text fields etc.) for chosing current viewable range */
    private List<JComponent> rangeControls;
    /** reference dropdown menu */
    private JComboBox referenceDropdown;
    /** From and To textboxes */
    private JTextField textboxFrom, textboxTo;
    /** Click and drag control for range selection */
    private RangeSelectionPanel rangeSelector;
    private MiniRangeSelectionPanel ruler;
    /** Length being displayed */
    private JLabel label_length;

    /** Information & Analysis Tabbed Pane (for plugin use) */
    private JTabbedPane auxTabbedPane;

    private PluginManager pluginManager;


    /**
     * Info
     */
    private static BookmarkSheet favoriteSheet;

    private RangeController rangeController = RangeController.getInstance();
    private BookmarkController favoriteController = BookmarkController.getInstance();

    private static Savant instance = null;

    public static synchronized Savant getInstance() {
        if (instance == null) {
            instance = new Savant();
        }

        return instance;
    }

    /** Creates new form Savant */
    private Savant() {
        try {

            UIManager.put("JideSplitPaneDivider.border", 5);
            UIManager.put("JideSplitPaneDivider.background", Color.red);

            // Set System L&F
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

            //LookAndFeelFactory.installJideExtension(LookAndFeelFactory.VSNET_STYLE);
            LookAndFeelFactory.installJideExtension(LookAndFeelFactory.VSNET_STYLE);

        } catch (Exception e) {
            // handle exception
        }
        instance = this;

        addComponentListener(this);
        initComponents();
        customizeUI();
        init();

        /*
        DocumentViewer v = new DocumentViewer();
        v.addDocument("C:\\Documents and Settings\\mfiume\\DataFormatter.html");
        v.addDocument("C:\\test.txt");
        v.setVisible(true);
         */
    }

    private void loadPlugins() {

        pluginManager = ObjectFactory.newInstance().createManager();

        File pluginsDir = new File("plugins");
        File[] plugins = pluginsDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.toLowerCase().endsWith(".jar");
            }
        });

        try {
            PluginManager.PluginLocation[] locations = new PluginManager.PluginLocation[plugins.length];

            for (int i=0; i<plugins.length; i++) {
                locations[i] = StandardPluginLocation.create(plugins[i]);
            }

            pluginManager.publishPlugins(locations);
        }
        catch (Exception e) {
            throw new RuntimeException(e); // TODO: fix this and handle properly
        }
    }
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        view_buttongroup = new javax.swing.ButtonGroup();
        panel_top = new javax.swing.JPanel();
        panelExtendedMiddle = new javax.swing.JPanel();
        panel_main = new javax.swing.JPanel();
        toolbar_bottom = new javax.swing.JToolBar();
        label_mouseposition_title = new javax.swing.JLabel();
        label_mouseposition = new javax.swing.JLabel();
        jSeparator7 = new javax.swing.JToolBar.Separator();
        label_memory = new javax.swing.JLabel();
        menuBar_top = new javax.swing.JMenuBar();
        menu_file = new javax.swing.JMenu();
        menu_load = new javax.swing.JMenu();
        menuitem_genome = new javax.swing.JMenuItem();
        menuitem_track = new javax.swing.JMenuItem();
        menuitem_trackURL = new javax.swing.JMenuItem();
        menuItemFormat = new javax.swing.JMenuItem();
        submenu_download = new javax.swing.JMenu();
        menuitem_preformatted = new javax.swing.JMenuItem();
        menuitem_ucsc = new javax.swing.JMenuItem();
        menuitem_thousandgenomes = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JSeparator();
        menuitem_screen = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JSeparator();
        menuitem_exit = new javax.swing.JMenuItem();
        menu_edit = new javax.swing.JMenu();
        menuitem_undo = new javax.swing.JMenuItem();
        jMenuItem5 = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        menuItemAddToFaves = new javax.swing.JMenuItem();
        menuitem_preferences = new javax.swing.JMenuItem();
        menu_view = new javax.swing.JMenu();
        menuItemPanLeft = new javax.swing.JMenuItem();
        menuItemPanRight = new javax.swing.JMenuItem();
        menuItemZoomIn = new javax.swing.JMenuItem();
        menuItemZoomOut = new javax.swing.JMenuItem();
        menuItemShiftStart = new javax.swing.JMenuItem();
        menuItemShiftEnd = new javax.swing.JMenuItem();
        jSeparator5 = new javax.swing.JSeparator();
        menuitem_view_plumbline = new javax.swing.JCheckBoxMenuItem();
        menuitem_view_spotlight = new javax.swing.JCheckBoxMenuItem();
        menu_window = new javax.swing.JMenu();
        menuItem_viewRangeControls = new javax.swing.JCheckBoxMenuItem();
        menuitem_genomeview = new javax.swing.JCheckBoxMenuItem();
        menuitem_ruler = new javax.swing.JCheckBoxMenuItem();
        menuitem_statusbar = new javax.swing.JCheckBoxMenuItem();
        jSeparator1 = new javax.swing.JSeparator();
        menuitem_tools = new javax.swing.JCheckBoxMenuItem();
        menu_bookmarks = new javax.swing.JCheckBoxMenuItem();
        menu_plugins = new javax.swing.JMenu();
        menuitem_pluginmanager = new javax.swing.JMenuItem();
        menu_help = new javax.swing.JMenu();
        menuitem_usermanual = new javax.swing.JMenuItem();
        menuitem_tutorials = new javax.swing.JMenuItem();
        jSeparator6 = new javax.swing.JSeparator();
        jMenuItem1 = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setBackground(new java.awt.Color(204, 204, 204));

        panel_top.setMaximumSize(new java.awt.Dimension(1000, 30));
        panel_top.setMinimumSize(new java.awt.Dimension(0, 0));
        panel_top.setPreferredSize(new java.awt.Dimension(0, 30));
        panel_top.setLayout(new java.awt.BorderLayout());

        panelExtendedMiddle.setBackground(new java.awt.Color(102, 102, 255));
        panelExtendedMiddle.setMinimumSize(new java.awt.Dimension(0, 0));
        panelExtendedMiddle.setPreferredSize(new java.awt.Dimension(990, 25));

        javax.swing.GroupLayout panelExtendedMiddleLayout = new javax.swing.GroupLayout(panelExtendedMiddle);
        panelExtendedMiddle.setLayout(panelExtendedMiddleLayout);
        panelExtendedMiddleLayout.setHorizontalGroup(
            panelExtendedMiddleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 808, Short.MAX_VALUE)
        );
        panelExtendedMiddleLayout.setVerticalGroup(
            panelExtendedMiddleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 30, Short.MAX_VALUE)
        );

        panel_top.add(panelExtendedMiddle, java.awt.BorderLayout.CENTER);

        panel_main.setBackground(new java.awt.Color(153, 153, 153));
        panel_main.setMaximumSize(new java.awt.Dimension(99999, 99999));
        panel_main.setMinimumSize(new java.awt.Dimension(500, 500));
        panel_main.setPreferredSize(new java.awt.Dimension(99999, 99999));

        javax.swing.GroupLayout panel_mainLayout = new javax.swing.GroupLayout(panel_main);
        panel_main.setLayout(panel_mainLayout);
        panel_mainLayout.setHorizontalGroup(
            panel_mainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 808, Short.MAX_VALUE)
        );
        panel_mainLayout.setVerticalGroup(
            panel_mainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 531, Short.MAX_VALUE)
        );

        toolbar_bottom.setFloatable(false);
        toolbar_bottom.setAlignmentX(1.0F);

        label_mouseposition_title.setText(" Position: ");
        toolbar_bottom.add(label_mouseposition_title);

        label_mouseposition.setText("mouse over track");
        toolbar_bottom.add(label_mouseposition);
        toolbar_bottom.add(jSeparator7);

        label_memory.setText(" Memory: ");
        toolbar_bottom.add(label_memory);

        menu_file.setText("File");

        menu_load.setText("Load...");

        menuitem_genome.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_G, java.awt.event.InputEvent.CTRL_MASK));
        menuitem_genome.setText("Genome");
        menuitem_genome.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuitem_genomeActionPerformed(evt);
            }
        });
        menu_load.add(menuitem_genome);

        menuitem_track.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_T, java.awt.event.InputEvent.CTRL_MASK));
        menuitem_track.setText("Track from File");
        menuitem_track.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuitem_trackActionPerformed(evt);
            }
        });
        menu_load.add(menuitem_track);

        menuitem_trackURL.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_U, java.awt.event.InputEvent.CTRL_MASK));
        menuitem_trackURL.setText("Track from URL");
        menuitem_trackURL.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuitem_trackURLActionPerformed(evt);
            }
        });
        menu_load.add(menuitem_trackURL);

        menu_file.add(menu_load);

        menuItemFormat.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, java.awt.event.InputEvent.CTRL_MASK));
        menuItemFormat.setText("Format");
        menuItemFormat.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemFormatActionPerformed(evt);
            }
        });
        menu_file.add(menuItemFormat);

        submenu_download.setText("Download");

        menuitem_preformatted.setText("Preformatted");
        menuitem_preformatted.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuitem_preformattedActionPerformed(evt);
            }
        });
        submenu_download.add(menuitem_preformatted);

        menuitem_ucsc.setText("UCSC");
        menuitem_ucsc.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuitem_ucscActionPerformed(evt);
            }
        });
        submenu_download.add(menuitem_ucsc);

        menuitem_thousandgenomes.setText("1000 Genomes");
        menuitem_thousandgenomes.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuitem_thousandgenomesActionPerformed(evt);
            }
        });
        submenu_download.add(menuitem_thousandgenomes);

        menu_file.add(submenu_download);
        menu_file.add(jSeparator3);

        menuitem_screen.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_T, java.awt.event.InputEvent.CTRL_MASK));
        menuitem_screen.setText("Screenshot");
        menuitem_screen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuitem_screenActionPerformed(evt);
            }
        });
        menu_file.add(menuitem_screen);

        jMenuItem2.setText("Export");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuitem_exportActionPerformed(evt);
            }
        });
        menu_file.add(jMenuItem2);
        menu_file.add(jSeparator4);

        menuitem_exit.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F4, java.awt.event.InputEvent.ALT_MASK));
        menuitem_exit.setText("Exit");
        menuitem_exit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuitem_exitActionPerformed(evt);
            }
        });
        menu_file.add(menuitem_exit);

        menuBar_top.add(menu_file);

        menu_edit.setText("Edit");

        menuitem_undo.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, java.awt.event.InputEvent.CTRL_MASK));
        menuitem_undo.setText("Undo Range Change");
        menuitem_undo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuitem_undoActionPerformed(evt);
            }
        });
        menu_edit.add(menuitem_undo);

        jMenuItem5.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Y, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItem5.setText("Redo Range Change");
        jMenuItem5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem5ActionPerformed(evt);
            }
        });
        menu_edit.add(jMenuItem5);
        menu_edit.add(jSeparator2);

        menuItemAddToFaves.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_D, java.awt.event.InputEvent.CTRL_MASK));
        menuItemAddToFaves.setText("Bookmark");
        menuItemAddToFaves.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemAddToFavesActionPerformed(evt);
            }
        });
        menu_edit.add(menuItemAddToFaves);

        menuitem_preferences.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, java.awt.event.InputEvent.CTRL_MASK));
        menuitem_preferences.setText("Preferences");
        menuitem_preferences.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuitem_preferencesActionPerformed(evt);
            }
        });
        menu_edit.add(menuitem_preferences);

        menuBar_top.add(menu_edit);

        menu_view.setText("View");

        menuItemPanLeft.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_LEFT, java.awt.event.InputEvent.SHIFT_MASK));
        menuItemPanLeft.setText("Pan Left");
        menuItemPanLeft.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemPanLeftActionPerformed(evt);
            }
        });
        menu_view.add(menuItemPanLeft);

        menuItemPanRight.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_RIGHT, java.awt.event.InputEvent.SHIFT_MASK));
        menuItemPanRight.setText("Pan Right");
        menuItemPanRight.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemPanRightActionPerformed(evt);
            }
        });
        menu_view.add(menuItemPanRight);

        menuItemZoomIn.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_UP, java.awt.event.InputEvent.SHIFT_MASK));
        menuItemZoomIn.setText("Zoom In");
        menuItemZoomIn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemZoomInActionPerformed(evt);
            }
        });
        menu_view.add(menuItemZoomIn);

        menuItemZoomOut.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DOWN, java.awt.event.InputEvent.SHIFT_MASK));
        menuItemZoomOut.setText("Zoom Out");
        menuItemZoomOut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemZoomOutActionPerformed(evt);
            }
        });
        menu_view.add(menuItemZoomOut);

        menuItemShiftStart.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_HOME, java.awt.event.InputEvent.SHIFT_MASK));
        menuItemShiftStart.setText("Shift to Start");
        menuItemShiftStart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemShiftStartActionPerformed(evt);
            }
        });
        menu_view.add(menuItemShiftStart);

        menuItemShiftEnd.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_END, java.awt.event.InputEvent.SHIFT_MASK));
        menuItemShiftEnd.setText("Shift to End");
        menuItemShiftEnd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemShiftEndActionPerformed(evt);
            }
        });
        menu_view.add(menuItemShiftEnd);
        menu_view.add(jSeparator5);

        menuitem_view_plumbline.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_K, java.awt.event.InputEvent.CTRL_MASK));
        menuitem_view_plumbline.setText("Plumbline");
        menuitem_view_plumbline.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuitem_view_plumblineActionPerformed(evt);
            }
        });
        menu_view.add(menuitem_view_plumbline);

        menuitem_view_spotlight.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, java.awt.event.InputEvent.CTRL_MASK));
        menuitem_view_spotlight.setText("Spotlight");
        menuitem_view_spotlight.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuitem_view_spotlightActionPerformed(evt);
            }
        });
        menu_view.add(menuitem_view_spotlight);

        menuBar_top.add(menu_view);

        menu_window.setText("Window");
        menu_window.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                menu_windowStateChanged(evt);
            }
        });

        menuItem_viewRangeControls.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        menuItem_viewRangeControls.setText("Navigation Controls");
        menuItem_viewRangeControls.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItem_viewRangeControlsMousePressed(evt);
            }
        });
        menu_window.add(menuItem_viewRangeControls);

        menuitem_genomeview.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        menuitem_genomeview.setText("Genome Context");
        menuitem_genomeview.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuitem_genomeviewActionPerformed(evt);
            }
        });
        menu_window.add(menuitem_genomeview);

        menuitem_ruler.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        menuitem_ruler.setText("Ruler");
        menuitem_ruler.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuitem_rulerActionPerformed(evt);
            }
        });
        menu_window.add(menuitem_ruler);

        menuitem_statusbar.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        menuitem_statusbar.setSelected(true);
        menuitem_statusbar.setText("Status Bar");
        menuitem_statusbar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuitem_statusbarActionPerformed(evt);
            }
        });
        menu_window.add(menuitem_statusbar);
        menu_window.add(jSeparator1);

        menuitem_tools.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_T, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        menuitem_tools.setText("Tools");
        menuitem_tools.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuitem_toolsActionPerformed(evt);
            }
        });
        menu_window.add(menuitem_tools);

        menu_bookmarks.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_B, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        menu_bookmarks.setText("Bookmarks");
        menu_bookmarks.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menu_bookmarksActionPerformed(evt);
            }
        });
        menu_window.add(menu_bookmarks);

        menuBar_top.add(menu_window);

        menu_plugins.setText("Plugins");

        menuitem_pluginmanager.setText("Plugin Manager");
        menuitem_pluginmanager.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuitem_pluginmanagerActionPerformed(evt);
            }
        });
        menu_plugins.add(menuitem_pluginmanager);

        menuBar_top.add(menu_plugins);

        menu_help.setText("Help");

        menuitem_usermanual.setText("Manuals");
        menuitem_usermanual.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuitem_usermanualActionPerformed(evt);
            }
        });
        menu_help.add(menuitem_usermanual);

        menuitem_tutorials.setText("Video Tutorials");
        menuitem_tutorials.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuitem_tutorialsActionPerformed(evt);
            }
        });
        menu_help.add(menuitem_tutorials);
        menu_help.add(jSeparator6);

        jMenuItem1.setText("Website");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        menu_help.add(jMenuItem1);

        menuBar_top.add(menu_help);

        setJMenuBar(menuBar_top);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(panel_top, javax.swing.GroupLayout.DEFAULT_SIZE, 808, Short.MAX_VALUE)
            .addComponent(toolbar_bottom, javax.swing.GroupLayout.DEFAULT_SIZE, 808, Short.MAX_VALUE)
            .addComponent(panel_main, javax.swing.GroupLayout.DEFAULT_SIZE, 808, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(panel_top, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(panel_main, javax.swing.GroupLayout.DEFAULT_SIZE, 531, Short.MAX_VALUE)
                .addGap(0, 0, 0)
                .addComponent(toolbar_bottom, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents




    /**
     * Shift the currentViewableRange all the way to the right
     * @param evt The mouse event which triggers the function
     */
    /**
     * Shift the currentViewableRange to the right
     * @param evt The mouse event which triggers the function
     */
    /**
     * Shift the currentViewableRange to the left
     * @param evt The mouse event which triggers the function
     */
    /**
     * Shift the currentViewableRange all the way to the left
     * @param evt The mouse event which triggers the function
     */
    private void menuItem_viewRangeControlsMousePressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItem_viewRangeControlsMousePressed
        this.panel_top.setVisible(!this.panel_top.isVisible());
    }//GEN-LAST:event_menuItem_viewRangeControlsMousePressed

    private void menuItemZoomInActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemZoomInActionPerformed
        RangeController rc = RangeController.getInstance();
        rc.zoomIn();
    }//GEN-LAST:event_menuItemZoomInActionPerformed

    private void menuItemZoomOutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemZoomOutActionPerformed
        RangeController rc = RangeController.getInstance();
        rc.zoomOut();
    }//GEN-LAST:event_menuItemZoomOutActionPerformed

    private void menuItemPanLeftActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemPanLeftActionPerformed
        RangeController rc = RangeController.getInstance();
        rc.shiftRangeLeft();
    }//GEN-LAST:event_menuItemPanLeftActionPerformed

    private void menuItemPanRightActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemPanRightActionPerformed
        RangeController rc = RangeController.getInstance();
        rc.shiftRangeRight();
    }//GEN-LAST:event_menuItemPanRightActionPerformed

    private void menuitem_undoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuitem_undoActionPerformed
        RangeController rc = RangeController.getInstance();
        rc.undoRangeChange();
    }//GEN-LAST:event_menuitem_undoActionPerformed

    private void jMenuItem5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem5ActionPerformed
        RangeController rc = RangeController.getInstance();
        rc.redoRangeChange();
    }//GEN-LAST:event_jMenuItem5ActionPerformed

    private void menuItemAddToFavesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemAddToFavesActionPerformed
        BookmarkController fc = BookmarkController.getInstance();
        fc.addCurrentRangeToBookmarks();
    }//GEN-LAST:event_menuItemAddToFavesActionPerformed

    private void menuItemFormatActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemFormatActionPerformed
        if (!dff.isVisible()) {
            Savant.log("Showing format form...");
            openAfterFormat = false;
            dff.clear();
            dff.setVisible(true);
        }
    }//GEN-LAST:event_menuItemFormatActionPerformed

    private void menuitem_exitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuitem_exitActionPerformed
        System.exit(0);
    }//GEN-LAST:event_menuitem_exitActionPerformed

    private void menuitem_genomeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuitem_genomeActionPerformed
        this.showOpenGenomeDialog();
    }//GEN-LAST:event_menuitem_genomeActionPerformed

    private void menuitem_trackActionPerformed(java.awt.event.ActionEvent evt) {                                               
        this.showOpenTracksDialog();
    }                                              

    private void menuitem_trackURLActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuitem_trackActionPerformed
        this.showOpenURLDialog();
    }//GEN-LAST:event_menuitem_trackActionPerformed

    private void menuitem_screenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuitem_screenActionPerformed
        ScreenShot.takeAndSave();
    }//GEN-LAST:event_menuitem_screenActionPerformed

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        try {
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(BrowserSettings.url));
        } catch (IOException ex) {
            Logger.getLogger(Savant.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void menuitem_preformattedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuitem_preformattedActionPerformed
        try {
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(BrowserSettings.url_preformatteddata));
        } catch (IOException ex) {
            Logger.getLogger(Savant.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_menuitem_preformattedActionPerformed

    private void menuitem_ucscActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuitem_ucscActionPerformed
        try {
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(BrowserSettings.url_ucsctablebrowser));
        } catch (IOException ex) {
            Logger.getLogger(Savant.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_menuitem_ucscActionPerformed

    private void menuitem_thousandgenomesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuitem_thousandgenomesActionPerformed
        try {
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(BrowserSettings.url_thousandgenomes));
        } catch (IOException ex) {
            Logger.getLogger(Savant.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_menuitem_thousandgenomesActionPerformed

    private void menuitem_pluginmanagerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuitem_pluginmanagerActionPerformed
        PluginDialog pd = new PluginDialog();
        pd.setVisible(true);
    }//GEN-LAST:event_menuitem_pluginmanagerActionPerformed

    private void menuitem_view_plumblineActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuitem_view_plumblineActionPerformed
        GraphPaneController gpc = GraphPaneController.getInstance();
        gpc.setPlumbing(this.menuitem_view_plumbline.isSelected());
    }//GEN-LAST:event_menuitem_view_plumblineActionPerformed

    private void menuitem_view_spotlightActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuitem_view_spotlightActionPerformed
        GraphPaneController gpc = GraphPaneController.getInstance();
        gpc.setSpotlight(this.menuitem_view_spotlight.isSelected());
    }//GEN-LAST:event_menuitem_view_spotlightActionPerformed

    private void menu_windowStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_menu_windowStateChanged
        /*
        if(this.getAuxDockingManager().getFrame("Information & Analysis").isVisible() != this.menu_info.getState()){
            this.menu_info.setState(!this.menu_info.getState());
        }
        if(this.getAuxDockingManager().getFrame("Bookmarks").isVisible() != this.menu_bookmarks.getState()){
            this.menu_bookmarks.setState(!this.menu_bookmarks.getState());
        }
         */
    }//GEN-LAST:event_menu_windowStateChanged

    private void menu_bookmarksActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menu_bookmarksActionPerformed

        String frameKey = "Bookmarks";
        DockingManager m = this.getAuxDockingManager();
        boolean isVisible = m.getFrame(frameKey).isHidden();
        setFrameVisibility(frameKey, isVisible, m);
        this.menu_bookmarks.setSelected(isVisible);
    }//GEN-LAST:event_menu_bookmarksActionPerformed

    private void menuitem_tutorialsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuitem_tutorialsActionPerformed
        try {
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(BrowserSettings.url_tutorials));
        } catch (IOException ex) {
            Logger.getLogger(Savant.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_menuitem_tutorialsActionPerformed

    private void menuitem_usermanualActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuitem_usermanualActionPerformed
        try {
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(BrowserSettings.url_manuals));
        } catch (IOException ex) {
            Logger.getLogger(Savant.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void setFrameVisibility(String frameKey, boolean isVisible, DockingManager m) {
        DockableFrame f = m.getFrame(frameKey);
        if (isVisible) { m.showFrame(frameKey); }
        else { m.hideFrame(frameKey); }
    }//GEN-LAST:event_menuitem_usermanualActionPerformed

    private void menuitem_rulerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuitem_rulerActionPerformed
        this.ruler.setVisible(!this.ruler.isVisible());
    }//GEN-LAST:event_menuitem_rulerActionPerformed

    private void menuitem_genomeviewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuitem_genomeviewActionPerformed
        this.rangeSelector.setVisible(!this.rangeSelector.isVisible());
    }//GEN-LAST:event_menuitem_genomeviewActionPerformed

    private void menuitem_statusbarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuitem_statusbarActionPerformed
        this.toolbar_bottom.setVisible(!this.toolbar_bottom.isVisible());
    }//GEN-LAST:event_menuitem_statusbarActionPerformed

    /**
     * Switch the visibility (hidden/shown) of a dockable frame with a given
     * tree.
     * @param frameKey The key for the DockableFrame to change visibility
     * @param m The DockingManager which contains the DockableFrame to change
     * visibility for
     */
    private void switchFrameVisibility(String frameKey, DockingManager m) {
        DockableFrame f = m.getFrame(frameKey);
        if (f.isVisible()) {
            m.hideFrame(frameKey);
        } else {
            m.showFrame(frameKey);
        }
    }

    private void menuitem_exportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuitem_exportActionPerformed
        //ExportImageDialog export = new ExportImageDialog(Savant.getInstance(), true);
        //export.setVisible(true);
        new ExportImage();

    }//GEN-LAST:event_menuitem_exportActionPerformed

    private void menuItemShiftStartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemShiftStartActionPerformed
        rangeController.shiftRangeFarLeft();
    }//GEN-LAST:event_menuItemShiftStartActionPerformed

    private void menuItemShiftEndActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemShiftEndActionPerformed
        rangeController.shiftRangeFarRight();
    }//GEN-LAST:event_menuItemShiftEndActionPerformed

    static boolean arePreferencesIntialized = false;

    private void menuitem_preferencesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuitem_preferencesActionPerformed

        if (!arePreferencesIntialized) {
            SettingsDialog.addSection(new ColourSchemeSettingsSection());
            SettingsDialog.addSection(new TemporaryFilesSettingsSection());
            //SettingsDialog.addSection(new ResolutionSettingsSection());
            arePreferencesIntialized = true;
        }
        
        SettingsDialog.showOptionsDialog();
    }//GEN-LAST:event_menuitem_preferencesActionPerformed

    private void menuitem_toolsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuitem_toolsActionPerformed
        String frameKey = "Tools";
        DockingManager m = this.getAuxDockingManager();
        boolean isVisible = m.getFrame(frameKey).isHidden();
        setFrameVisibility(frameKey, isVisible, m);
        this.menuitem_tools.setSelected(isVisible);
    }//GEN-LAST:event_menuitem_toolsActionPerformed

    /**
     * Starts an instance of the Savant Browser
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                com.jidesoft.utils.Lm.verifyLicense("Marc Fiume", "Savant Genome Browser", "1BimsQGmP.vjmoMbfkPdyh0gs3bl3932");
                LookAndFeelFactory.installJideExtension(LookAndFeelFactory.OFFICE2007_STYLE);
                Savant instance = Savant.getInstance();
                instance.loadPlugins();
                instance.initPlugins();
                instance.displayAuxPanels();
                //instance.setVisible(true);
                //instance.showOpenGenomeDialog();
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem5;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JSeparator jSeparator6;
    private javax.swing.JToolBar.Separator jSeparator7;
    private javax.swing.JLabel label_memory;
    private javax.swing.JLabel label_mouseposition;
    private javax.swing.JLabel label_mouseposition_title;
    private javax.swing.JMenuBar menuBar_top;
    private javax.swing.JMenuItem menuItemAddToFaves;
    private javax.swing.JMenuItem menuItemFormat;
    private javax.swing.JMenuItem menuItemPanLeft;
    private javax.swing.JMenuItem menuItemPanRight;
    private javax.swing.JMenuItem menuItemShiftEnd;
    private javax.swing.JMenuItem menuItemShiftStart;
    private javax.swing.JMenuItem menuItemZoomIn;
    private javax.swing.JMenuItem menuItemZoomOut;
    private javax.swing.JCheckBoxMenuItem menuItem_viewRangeControls;
    private javax.swing.JCheckBoxMenuItem menu_bookmarks;
    private javax.swing.JMenu menu_edit;
    private javax.swing.JMenu menu_file;
    private javax.swing.JMenu menu_help;
    private javax.swing.JMenu menu_load;
    private javax.swing.JMenu menu_plugins;
    private javax.swing.JMenu menu_view;
    private javax.swing.JMenu menu_window;
    private javax.swing.JMenuItem menuitem_exit;
    private javax.swing.JMenuItem menuitem_genome;
    private javax.swing.JCheckBoxMenuItem menuitem_genomeview;
    private javax.swing.JMenuItem menuitem_pluginmanager;
    private javax.swing.JMenuItem menuitem_preferences;
    private javax.swing.JMenuItem menuitem_preformatted;
    private javax.swing.JCheckBoxMenuItem menuitem_ruler;
    private javax.swing.JMenuItem menuitem_screen;
    private javax.swing.JCheckBoxMenuItem menuitem_statusbar;
    private javax.swing.JMenuItem menuitem_thousandgenomes;
    private javax.swing.JCheckBoxMenuItem menuitem_tools;
    private javax.swing.JMenuItem menuitem_track;
    private javax.swing.JMenuItem menuitem_trackURL;
    private javax.swing.JMenuItem menuitem_tutorials;
    private javax.swing.JMenuItem menuitem_ucsc;
    private javax.swing.JMenuItem menuitem_undo;
    private javax.swing.JMenuItem menuitem_usermanual;
    private javax.swing.JCheckBoxMenuItem menuitem_view_plumbline;
    private javax.swing.JCheckBoxMenuItem menuitem_view_spotlight;
    private javax.swing.JPanel panelExtendedMiddle;
    private javax.swing.JPanel panel_main;
    private javax.swing.JPanel panel_top;
    private javax.swing.JMenu submenu_download;
    private javax.swing.JToolBar toolbar_bottom;
    private javax.swing.ButtonGroup view_buttongroup;
    // End of variables declaration//GEN-END:variables


    /**
     * Customize the UI
     */
    void customizeUI() {
        LookAndFeelFactory.UIDefaultsCustomizer uiDefaultsCustomizer = new LookAndFeelFactory.UIDefaultsCustomizer() {
            public void customize(UIDefaults defaults) {
                ThemePainter painter = (ThemePainter) UIDefaultsLookup.get("Theme.painter");
                defaults.put("OptionPaneUI", "com.jidesoft.plaf.basic.BasicJideOptionPaneUI");

                defaults.put("OptionPane.showBanner", Boolean.TRUE); // show banner or not. default is true
                //defaults.put("OptionPane.bannerIcon", JideIconsFactory.getImageIcon(JideIconsFactory.JIDE50));
                defaults.put("OptionPane.bannerFontSize", 13);
                defaults.put("OptionPane.bannerFontStyle", Font.BOLD);
                defaults.put("OptionPane.bannerMaxCharsPerLine", 60);
                defaults.put("OptionPane.bannerForeground", Color.BLACK); //painter != null ? painter.getOptionPaneBannerForeground() : null);  // you should adjust this if banner background is not the default gradient paint
                defaults.put("OptionPane.bannerBorder", null); // use default border

                // set both bannerBackgroundDk and // set both bannerBackgroundLt to null if you don't want gradient
                //defaults.put("OptionPane.bannerBackgroundDk", painter != null ? painter.getOptionPaneBannerDk() : null);
                //defaults.put("OptionPane.bannerBackgroundLt", painter != null ? painter.getOptionPaneBannerLt() : null);
                //defaults.put("OptionPane.bannerBackgroundDirection", Boolean.TRUE); // default is true

                // optionally, you can set a Paint object for BannerPanel. If so, the three UIDefaults related to banner background above will be ignored.
                defaults.put("OptionPane.bannerBackgroundPaint", null);

                defaults.put("OptionPane.buttonAreaBorder", BorderFactory.createEmptyBorder(6, 6, 6, 6));
                defaults.put("OptionPane.buttonOrientation", SwingConstants.RIGHT);

            }
        };
        uiDefaultsCustomizer.customize(UIManager.getDefaults());
    }

    /**
     * Initialize the Browser
     */
    void init() {
        //this.setVisible(false);
        //this.setExtendedState(this.getExtendedState() | JFrame.MAXIMIZED_BOTH);

        rangeController.addRangeChangedListener(this);
        ReferenceController.getInstance().addReferenceChangedListener(this);

        initGUIFrame();
        initPanelsAndDocking();
        initMenu();
        initStatusBar();

        dff = new DataFormatForm(this, false);
        // get async notification when DataFormatForm has finished its business
        dff.addPropertyChangeListener("success", this);

        urlDialog = new OpenURLDialog(Savant.getInstance(), true);
        // comment next line to disable plugin manager
        //this.menu_plugins.setVisible(false);

        //this.setVisible(true);

        disableExperimentalFeatures();
    }

    private void disableExperimentalFeatures() {
        //this.menuitem_preferences.setVisible(false);
        this.menuitem_tools.setVisible(false);
    }

    private void initPanelsAndDocking() {
        initDocking();
        initAuxilliaryPanels();
    }

    private void initAuxilliaryPanels() {
        initAuxPanel1();
        initAuxPanel2();
    }

    private JPanel addDockableFrame(String key, int mode, int side) {
        DockableFrame dockableFrame = new DockableFrame(key);

        dockableFrame.getContext().setInitIndex(1);
        dockableFrame.getContext().setInitMode(mode);
        dockableFrame.getContext().setInitSide(side);

        JPanel p = new JPanel();
        dockableFrame.getContentPane().add(p);

        this.auxDockingManager.addFrame(dockableFrame);

        return p;
    }

    private PluginManager getPluginManager() {
        return pluginManager;
    }

    private void initPlugins() {
        try{
            // init the AuxData plugins

            PluginDescriptor core = pluginManager.getRegistry().getPluginDescriptor("savant.core");
            ExtensionPoint point = pluginManager.getRegistry().getExtensionPoint(core.getId(), "AuxData");

            for (Iterator it = point.getConnectedExtensions().iterator(); it.hasNext();) {

                Extension ext = (Extension) it.next();
                PluginDescriptor descr = ext.getDeclaringPluginDescriptor();
                pluginManager.activatePlugin(descr.getId());
                ClassLoader classLoader = pluginManager.getPluginClassLoader(descr);
                Class pluginCls = classLoader.loadClass(ext.getParameter("class").valueAsString());

                Object plugininstance = pluginCls.newInstance();

                if (plugininstance instanceof GUIPlugin) {

                    GUIPlugin plugin = (GUIPlugin) plugininstance;

                    System.out.println("Loading GUI Plugin : " + plugin.getTitle());

                    final DockableFrame f = DockableFrameFactory.createGUIPluginFrame(plugin.getTitle());
                    JPanel p = (JPanel) f.getContentPane();
                    p.setLayout(new BorderLayout());
                    JPanel canvas = new JPanel();
                    p.add(canvas, BorderLayout.CENTER);
                    canvas.setLayout(new BorderLayout());
                    plugin.init(canvas, new PluginAdapter());
                    this.getAuxDockingManager().addFrame(f);
                    boolean isIntiallyVisible = false;
                    setFrameVisibility(f.getTitle(), isIntiallyVisible, auxDockingManager);
                    final JCheckBoxMenuItem cb = new JCheckBoxMenuItem(plugin.getTitle());
                    cb.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            DockingManager m = auxDockingManager;
                            String frameKey = f.getTitle();
                            boolean isVisible = m.getFrame(frameKey).isHidden();
                            setFrameVisibility(frameKey, isVisible, m);
                            cb.setSelected(isVisible);
                        }
                    });
                    cb.setSelected(!auxDockingManager.getFrame(f.getTitle()).isHidden());
                    //FIXME: this is not ideal...
                    if(plugin.getTitle().equals("Table View")){
                        cb.setSelected(true);
                    }
                    menu_window.add(cb);
                } else if (plugininstance instanceof ToolPlugin) {

                    ToolPlugin p = (ToolPlugin) plugininstance;

                    System.out.println("Loading Tool Plugin : " + p.getToolInformation().getName());

                    p.init(new PluginAdapter());
                    ToolsModule.addTool(p);
                } else {
                    System.out.println("Unknown plugin type");
                }
            }

            // TODO: add provisions for runnable plugins

        }
        catch (Exception e) {
            e.printStackTrace(); // TODO: handle properly
        }
    }

    public DockingManager getAuxDockingManager() {
        return auxDockingManager;
    }

    public DockingManager getTrackDockingManager() {
        return trackDockingManager;
    }

    private void initAuxPanel1() {

        String frameTitle = "Tools";
        DockableFrame df = DockableFrameFactory.createFrame(frameTitle,DockContext.STATE_HIDDEN,DockContext.DOCK_SIDE_SOUTH);
        df.setAvailableButtons(DockableFrame.BUTTON_AUTOHIDE | DockableFrame.BUTTON_FLOATING | DockableFrame.BUTTON_MAXIMIZE);
        this.getAuxDockingManager().addFrame(df);
        setFrameVisibility(frameTitle, false, this.getAuxDockingManager());

        JPanel canvas = new JPanel();
        canvas.setBackground(Color.red);

        savantTools = new ToolsModule(canvas);

        // make sure only one active frame
        //DockingManagerGroup dmg = getDockingManagerGroup(); //new DockingManagerGroup();
        //dmg.add(auxDockingManager);
        //dmg.add(trackDockingManager);
        
        df.getContentPane().setLayout(new BorderLayout());
        df.getContentPane().add(canvas, BorderLayout.CENTER);
    }

    private static DockingManagerGroup dmg;
    public static void addDockingManagerToGroup(DockingManager m) {
        if (dmg == null) { dmg = new DockingManagerGroup(); }
        dmg.add(m);
    }

    private void initAuxPanel2() {

        DockableFrame df = DockableFrameFactory.createFrame("Bookmarks",DockContext.STATE_HIDDEN,DockContext.DOCK_SIDE_EAST);
        df.setAvailableButtons(DockableFrame.BUTTON_AUTOHIDE | DockableFrame.BUTTON_FLOATING | DockableFrame.BUTTON_MAXIMIZE );
        this.getAuxDockingManager().addFrame(df);
        setFrameVisibility("Bookmarks", false, this.getAuxDockingManager());


        df.getContentPane().setLayout(new BorderLayout());

        initBookmarksTab(df.getContentPane());
    }

    /**
     * Provide access to the tabbed pane in the bottom auxilliary panel
     * @return the auxilliary tabbed pane
     */
    public JTabbedPane getAuxTabbedPane() {
         return auxTabbedPane;
     }

    /**
     * Set up frame
     */
    void initGUIFrame() {
        this.setTitle("Savant Genome Browser");
        this.setName("Savant Genome Browser");
        this.setExtendedState(this.getExtendedState() | JFrame.MAXIMIZED_BOTH);
    }

    private void initMenu() {
        menuitem_genome.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_G, osSpecificModifier));
        menuitem_track.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_T, osSpecificModifier));
        menuitem_trackURL.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_U, osSpecificModifier));
        menuItemFormat.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, osSpecificModifier));
        menuitem_screen.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, osSpecificModifier));
        menuitem_exit.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Q, osSpecificModifier));
        menuitem_undo.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, osSpecificModifier));
        jMenuItem5.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Y, osSpecificModifier));
        menuItemAddToFaves.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_B, osSpecificModifier));
        menuItem_viewRangeControls.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.event.InputEvent.SHIFT_MASK | osSpecificModifier));
        menuItemPanLeft.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_LEFT, java.awt.event.InputEvent.SHIFT_MASK));
        menuItemPanRight.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_RIGHT, java.awt.event.InputEvent.SHIFT_MASK));
        menuItemZoomIn.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_UP, java.awt.event.InputEvent.SHIFT_MASK));
        menuItemZoomOut.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DOWN, java.awt.event.InputEvent.SHIFT_MASK));
        menuItemShiftStart.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_HOME, java.awt.event.InputEvent.SHIFT_MASK));
        menuItemShiftEnd.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_END, java.awt.event.InputEvent.SHIFT_MASK));
        menuitem_preferences.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, osSpecificModifier));
        menuitem_view_plumbline.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_K, osSpecificModifier));
        menuitem_view_spotlight.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, osSpecificModifier));
        menu_bookmarks.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_B, osSpecificModifier | java.awt.event.InputEvent.SHIFT_MASK));
        menuitem_genomeview.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, osSpecificModifier | java.awt.event.InputEvent.SHIFT_MASK));
        menuitem_ruler.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, osSpecificModifier | java.awt.event.InputEvent.SHIFT_MASK));
        menuitem_statusbar.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, osSpecificModifier | java.awt.event.InputEvent.SHIFT_MASK));

        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            submenu_download.setEnabled(false);
            this.menuitem_tutorials.setEnabled(false);
            this.menuitem_usermanual.setEnabled(false);
            jMenuItem1.setEnabled(false);
        }
        initBrowseMenu();
    }

    /**
     * == [ RESIZE FORM ] ==
     *  Java does not enforce (proactively) a minimum
     *  form size. Here, we resize the form to a minimum
     *  size if the user has resized it to something
     *  smaller.
     */
    /**
     * Resize the form to the minimum size if the
     * user has resized it to something smaller.
     * @param e The resize event
     */
    public void componentResized(ComponentEvent e) {
        int width = getWidth();
        int height = getHeight();
        //we check if either the width
        //or the height are below minimum
        boolean resize = false;
        if (width < minimumFormWidth) {
            resize = true;
            width = minimumFormWidth;
        }
        if (height < minimumFormHeight) {
            resize = true;
            height = minimumFormHeight;
        }
        if (resize) {
            setSize(width, height);
        }
    }

    public void componentMoved(ComponentEvent e) {
    }

    public void componentShown(ComponentEvent e) {
    }

    public void componentHidden(ComponentEvent e) {
    }

    private void initBrowseMenu() {

        this.menuPanel = new JPanel();
        this.panelExtendedMiddle.setLayout(new BorderLayout());
        this.panelExtendedMiddle.add(menuPanel);
        JPanel p = this.menuPanel;

        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));

        Dimension comboboxDimension = new Dimension(150,23);
        Dimension buttonDimension = new Dimension(45,23);
        Dimension iconDimension = new Dimension(23,23);

        String shortcutMod;
        if(mac) shortcutMod = "Cmd";
        else shortcutMod = "Ctrl";

        /*
        button_genome = addButton(p, "Genome");
        button_genome.setToolTipText("Load a genome");

        // .createRoundedBalloonTip(Component attachedComponent, Alignment alignment, Color borderColor, Color fillColor, int borderWidth, int horizontalOffset, int verticalOffset, int arcWidth, int arcHeight, boolean useCloseButton)

        button_genome.addMouseListener(new MouseListener() {

            public void mouseClicked(MouseEvent e) {
                showOpenGenomeDialog();
            }

            public void mousePressed(MouseEvent e) {
            }

            public void mouseReleased(MouseEvent e) {
                if(button_genome.contains(e.getPoint())){
                    showOpenGenomeDialog();
                }
            }

            public void mouseEntered(MouseEvent e) {
            }

            public void mouseExited(MouseEvent e) {
            }
        });

        trackButton = addButton(p, "  Track  ");
        trackButton.setToolTipText("Load a track");
        trackButton.addMouseListener(new MouseListener() {

            public void mouseClicked(MouseEvent e) {
                showOpenTracksDialog();
            }

            public void mousePressed(MouseEvent e) {
            }

            public void mouseReleased(MouseEvent e) {
                if(trackButton.contains(e.getPoint())){
                    showOpenTracksDialog();
                }
            }

            public void mouseEntered(MouseEvent e) {
            }

            public void mouseExited(MouseEvent e) {
            }
        });

        p.add(Box.createGlue());

        


        p.add(getRigidPadding());
        */

        p.add(this.getRigidPadding());

        JLabel reftext = new JLabel();
        reftext.setText("Reference: ");
        reftext.setToolTipText("Reference sequence");
        p.add(reftext);

        referenceDropdown = new JComboBox();
        referenceDropdown.setPreferredSize(comboboxDimension);
        referenceDropdown.setMinimumSize(comboboxDimension);
        referenceDropdown.setMaximumSize(comboboxDimension);
        referenceDropdown.setToolTipText("Reference sequence");
        referenceDropdown.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                // the first item is a header and not allowed to be selected
                if (referenceDropdown.getItemCount() <= 1) { return; }

                int index = referenceDropdown.getSelectedIndex();
                String ref = (String) referenceDropdown.getItemAt(index);
                if (ref.contains("[")) {
                    int size = referenceDropdown.getItemCount();
                    for (int i = 1; i < size; i++) {
                        int newindex = (index + i) % size;
                        String newref = (String) referenceDropdown.getItemAt( newindex );
                        if (!((String)referenceDropdown.getItemAt(newindex)).contains("[")) {
                            index = newindex;
                            referenceDropdown.setSelectedIndex(index);
                            break;
                        }
                    }
                }
                switchReference(index);
            }

            private void switchReference(int index) {

                String ref = (String) referenceDropdown.getItemAt(index);

                /*
                System.out.println("Trying to change reference to " + ref);
                if (ref.contains("[")) {
                    int newindex = referenceDropdown.getSelectedIndex() + 1;
                    if (newindex < referenceDropdown.getItemCount()) {
                        System.out.println("Instead trying to change reference to " + newindex);
                        switchReference(newindex);
                    } else {
                        System.out.println("Instead trying to change reference to " + 0);
                        switchReference(0);
                    }
                    return;
                }
                 */

                if (!ReferenceController.getInstance().getReferenceNames().contains(ref)) {
                    if (!showNonGenomicReferenceDialog) { return; }
                    //Custom button text
                    Object[] options = {"OK",
                                        "Don't show again"};
                    int n = JOptionPane.showOptionDialog(Savant.getInstance(),
                        "This reference is nongenomic (i.e. it appears in a loaded track but it is not found in the loaded genome)",
                        "Non-Genomic Reference",
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.INFORMATION_MESSAGE,
                        null,
                        options,
                        options[0]);

                    if (n == 1) {
                        showNonGenomicReferenceDialog = false;
                    } else if (n == 0) {
                        return;
                    }
                }

                //System.out.println("Actually changing reference to " + ref);
                ReferenceController.getInstance().setReference(ref);
            }

        });
        p.add(referenceDropdown);

        p.add(getRigidPadding());

        JLabel fromtext = new JLabel();
        fromtext.setText("From: ");
        fromtext.setToolTipText("Start position of range");
        p.add(fromtext);
        //p.add(this.getRigidPadding());

        int tfwidth = 100;
        int labwidth = 100;
        int tfheight = 22;
        textboxFrom = addTextField(p, "");
        textboxFrom.setToolTipText("Start position of range");
        textboxFrom.setHorizontalAlignment(JTextField.CENTER);
        textboxFrom.setPreferredSize(new Dimension(tfwidth, tfheight));
        textboxFrom.setMaximumSize(new Dimension(tfwidth, tfheight));
        textboxFrom.setMinimumSize(new Dimension(tfwidth, tfheight));

        textboxFrom.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyPressed(java.awt.event.KeyEvent evt) {
                RangeTextBoxKeypressed(evt);
            }
        });

        /*
        JLabel sepr = new JLabel();
        sepr.setText(" - ");
        p.add(sepr);
         * 
         */

        p.add(this.getRigidPadding());
        JLabel totext = new JLabel();
        totext.setToolTipText("End position of range");
        totext.setText("To: ");
        p.add(totext);
        //p.add(this.getRigidPadding());

        textboxTo = addTextField(p, "");
        textboxTo.setToolTipText("End position of range");
        textboxTo.setHorizontalAlignment(JTextField.CENTER);
        textboxTo.setPreferredSize(new Dimension(tfwidth, tfheight));
        textboxTo.setMaximumSize(new Dimension(tfwidth, tfheight));
        textboxTo.setMinimumSize(new Dimension(tfwidth, tfheight));

        textboxTo.addKeyListener(new java.awt.event.KeyAdapter() {

            public void keyPressed(java.awt.event.KeyEvent evt) {
                RangeTextBoxKeypressed(evt);
            }
        });

        p.add(this.getRigidPadding());


        goButton = addButton(p, "  Go  ");
        goButton.setToolTipText("Go to specified range (Enter)");
        goButton.addMouseListener(new MouseListener() {

            public void mouseClicked(MouseEvent e) {
            }

            public void mousePressed(MouseEvent e) {
            }

            public void mouseReleased(MouseEvent e) {
                if(goButton.contains(e.getPoint())){
                    setRangeFromTextBoxes();
                }
            }

            public void mouseEntered(MouseEvent e) {
            }

            public void mouseExited(MouseEvent e) {
            }
        });


        p.add(this.getRigidPadding());

        
        JLabel sepl = new JLabel();
        sepl.setText("Length: ");
        sepl.setToolTipText("Length of the current range");
        p.add(sepl);

        label_length = new JLabel();
        label_length.setText("length");
        label_length.setToolTipText("Length of the current range");
        //label_length.setHorizontalAlignment(JLabel.RIGHT);

        label_length.setPreferredSize(new Dimension(labwidth, tfheight));
        label_length.setMaximumSize(new Dimension(labwidth, tfheight));
        label_length.setMinimumSize(new Dimension(labwidth, tfheight));
        p.add(label_length);

        p.add(Box.createGlue());

        JButton button_undo = new JButton("");
        /////////
        button_undo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/savant/images/undo.png")));
        button_undo.setBorder(null);
        button_undo.setBorderPainted(false);
        button_undo.setContentAreaFilled(false);
        button_undo.setFocusPainted(false);
        button_undo.setPreferredSize(iconDimension);
        button_undo.setMinimumSize(iconDimension);
        button_undo.setMaximumSize(iconDimension);
        button_undo.setPressedIcon(new javax.swing.ImageIcon(getClass().getResource("/savant/images/undo_down.png")));
        button_undo.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/savant/images/undo_over.png")));
        /////////
        button_undo.setToolTipText("Undo range change (" + shortcutMod + "+Z)");
        button_undo.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                RangeController.getInstance().undoRangeChange();
            }

        });
        p.add(button_undo);
        p.add(this.getRigidPadding());

        JButton button_redo = new JButton("");
        /////////
        button_redo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/savant/images/redo.png")));
        button_redo.setBorder(null);
        button_redo.setBorderPainted(false);
        button_redo.setContentAreaFilled(false);
        button_redo.setFocusPainted(false);
        button_redo.setPreferredSize(iconDimension);
        button_redo.setMinimumSize(iconDimension);
        button_redo.setMaximumSize(iconDimension);
        button_redo.setPressedIcon(new javax.swing.ImageIcon(getClass().getResource("/savant/images/redo_down.png")));
        button_redo.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/savant/images/redo_over.png")));
        /////////
        button_redo.setToolTipText("Redo range change (" + shortcutMod + "+Y)");
        button_redo.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                RangeController.getInstance().redoRangeChange();
            }

        });
        p.add(button_redo);

        p.add(this.getRigidPadding());
        p.add(this.getRigidPadding());

        JButton zoomIn = addButton(p, "");
        /////////
        zoomIn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/savant/images/in.png")));
        zoomIn.setBorder(null);
        zoomIn.setBorderPainted(false);
        zoomIn.setContentAreaFilled(false);
        zoomIn.setFocusPainted(false);
        zoomIn.setPressedIcon(new javax.swing.ImageIcon(getClass().getResource("/savant/images/in_down.png")));
        zoomIn.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/savant/images/in_over.png")));
        /////////
        zoomIn.setToolTipText("Zoom in (Shift+Up)");
        zoomIn.setPreferredSize(iconDimension);
        zoomIn.setMinimumSize(iconDimension);
        zoomIn.setMaximumSize(iconDimension);
        zoomIn.addMouseListener(new MouseListener() {

            public void mouseClicked(MouseEvent e) {
                rangeController.zoomIn();
            }

            public void mousePressed(MouseEvent e) {
            }

            public void mouseReleased(MouseEvent e) {
            }

            public void mouseEntered(MouseEvent e) {
            }

            public void mouseExited(MouseEvent e) {
            }
        });
        p.add(this.getRigidPadding());

        JButton zoomOut = addButton(p, "");
        /////////
        zoomOut.setIcon(new javax.swing.ImageIcon(getClass().getResource("/savant/images/out.png")));
        zoomOut.setBorder(null);
        zoomOut.setBorderPainted(false);
        zoomOut.setContentAreaFilled(false);
        zoomOut.setFocusPainted(false);
        zoomOut.setPressedIcon(new javax.swing.ImageIcon(getClass().getResource("/savant/images/out_down.png")));
        zoomOut.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/savant/images/out_over.png")));
        /////////
        zoomOut.setToolTipText("Zoom out (Shift+Down)");
        zoomOut.setPreferredSize(iconDimension);
        zoomOut.setMinimumSize(iconDimension);
        zoomOut.setMaximumSize(iconDimension);
        zoomOut.addMouseListener(new MouseListener() {

            public void mouseClicked(MouseEvent e) {
                rangeController.zoomOut();
            }

            public void mousePressed(MouseEvent e) {
            }

            public void mouseReleased(MouseEvent e) {
            }

            public void mouseEntered(MouseEvent e) {
            }

            public void mouseExited(MouseEvent e) {
            }
        });

        p.add(getRigidPadding());
        p.add(this.getRigidPadding());

        JButton shiftFarLeft = addButton(p, "");
        /////////
        shiftFarLeft.setIcon(new javax.swing.ImageIcon(getClass().getResource("/savant/images/leftfull.png")));
        shiftFarLeft.setBorder(null);
        shiftFarLeft.setBorderPainted(false);
        shiftFarLeft.setContentAreaFilled(false);
        shiftFarLeft.setFocusPainted(false);
        shiftFarLeft.setPressedIcon(new javax.swing.ImageIcon(getClass().getResource("/savant/images/leftfull_down.png")));
        shiftFarLeft.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/savant/images/leftfull_over.png")));
        /////////
        shiftFarLeft.setToolTipText("Move to the beginning of the genome (Home)");
        shiftFarLeft.setPreferredSize(iconDimension);
        shiftFarLeft.setMinimumSize(iconDimension);
        shiftFarLeft.setMaximumSize(iconDimension);

        shiftFarLeft.addMouseListener(new MouseListener() {

            public void mouseClicked(MouseEvent e) {
                rangeController.shiftRangeFarLeft();
            }

            public void mousePressed(MouseEvent e) {
            }

            public void mouseReleased(MouseEvent e) {
            }

            public void mouseEntered(MouseEvent e) {
            }

            public void mouseExited(MouseEvent e) {
            }
        });

        p.add(this.getRigidPadding());

        JButton shiftLeft = addButton(p, "");
        /////////
        shiftLeft.setIcon(new javax.swing.ImageIcon(getClass().getResource("/savant/images/left.png")));
        shiftLeft.setBorder(null);
        shiftLeft.setBorderPainted(false);
        shiftLeft.setContentAreaFilled(false);
        shiftLeft.setFocusPainted(false);
        shiftLeft.setPressedIcon(new javax.swing.ImageIcon(getClass().getResource("/savant/images/left_down.png")));
        shiftLeft.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/savant/images/left_over.png")));
        /////////
        shiftLeft.setToolTipText("Move left (Shift+Left)");
        shiftLeft.setPreferredSize(iconDimension);
        shiftLeft.setMinimumSize(iconDimension);
        shiftLeft.setMaximumSize(iconDimension);
        shiftLeft.addMouseListener(new MouseListener() {

            public void mouseClicked(MouseEvent e) {
                rangeController.shiftRangeLeft();
            }

            public void mousePressed(MouseEvent e) {
            }

            public void mouseReleased(MouseEvent e) {
            }

            public void mouseEntered(MouseEvent e) {
            }

            public void mouseExited(MouseEvent e) {
            }
        });

        p.add(this.getRigidPadding());

        JButton shiftRight = addButton(p, "");
        /////////
        shiftRight.setIcon(new javax.swing.ImageIcon(getClass().getResource("/savant/images/right.png")));
        shiftRight.setBorder(null);
        shiftRight.setBorderPainted(false);
        shiftRight.setContentAreaFilled(false);
        shiftRight.setFocusPainted(false);
        shiftRight.setPressedIcon(new javax.swing.ImageIcon(getClass().getResource("/savant/images/right_down.png")));
        shiftRight.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/savant/images/right_over.png")));
        /////////
        shiftRight.setToolTipText("Move right (Shift+Right)");
        shiftRight.setPreferredSize(iconDimension);
        shiftRight.setMinimumSize(iconDimension);
        shiftRight.setMaximumSize(iconDimension);
        shiftRight.addMouseListener(new MouseListener() {

            public void mouseClicked(MouseEvent e) {
                rangeController.shiftRangeRight();
            }

            public void mousePressed(MouseEvent e) {
            }

            public void mouseReleased(MouseEvent e) {
            }

            public void mouseEntered(MouseEvent e) {
            }

            public void mouseExited(MouseEvent e) {
            }
        });

        p.add(this.getRigidPadding());

        JButton shiftFarRight = addButton(p, "");
        /////////
        shiftFarRight.setIcon(new javax.swing.ImageIcon(getClass().getResource("/savant/images/rightfull.png")));
        shiftFarRight.setBorder(null);
        shiftFarRight.setBorderPainted(false);
        shiftFarRight.setContentAreaFilled(false);
        shiftFarRight.setFocusPainted(false);
        shiftFarRight.setPressedIcon(new javax.swing.ImageIcon(getClass().getResource("/savant/images/rightfull_down.png")));
        shiftFarRight.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/savant/images/rightfull_over.png")));
        /////////
        shiftFarRight.setToolTipText("Move to the end of the genome (End)");
        shiftFarRight.setPreferredSize(iconDimension);
        shiftFarRight.setMinimumSize(iconDimension);
        shiftFarRight.setMaximumSize(iconDimension);
        shiftFarRight.addMouseListener(new MouseListener() {

            public void mouseClicked(MouseEvent e) {
                rangeController.shiftRangeFarRight();
            }

            public void mousePressed(MouseEvent e) {
            }

            public void mouseReleased(MouseEvent e) {
            }

            public void mouseEntered(MouseEvent e) {
            }

            public void mouseExited(MouseEvent e) {
            }
        });

        p.add(this.getRigidPadding());

        rangeControls = new ArrayList<JComponent>();
        rangeControls.add(reftext);
        rangeControls.add(referenceDropdown);
        rangeControls.add(fromtext);
        rangeControls.add(totext);
        rangeControls.add(sepl);
        //rangeControls.add(sepr);
        rangeControls.add(label_length);
        rangeControls.add(textboxFrom);
        rangeControls.add(textboxTo);
        rangeControls.add(shiftFarLeft);
        rangeControls.add(shiftFarRight);
        rangeControls.add(shiftLeft);
        rangeControls.add(shiftRight);
        rangeControls.add(zoomIn);
        rangeControls.add(zoomOut);
        rangeControls.add(rangeSelector);
        rangeControls.add(ruler);
        //rangeControls.add(trackButton);
        rangeControls.add(menuitem_track);
        rangeControls.add(menuitem_trackURL);
        rangeControls.add(button_undo);
        rangeControls.add(button_redo);
        rangeControls.add(menuItemPanLeft);
        rangeControls.add(menuItemPanRight);
        rangeControls.add(menuItemZoomOut);
        rangeControls.add(menuItemZoomIn);
        rangeControls.add(menuItemShiftStart);
        rangeControls.add(menuItemShiftEnd);
        rangeControls.add(menuItemAddToFaves);
        rangeControls.add(menuitem_undo);
        rangeControls.add(jMenuItem5);
        rangeControls.add(menuitem_view_plumbline);
        rangeControls.add(menuitem_view_spotlight);
        rangeControls.add(label_mouseposition); rangeControls.add(label_mouseposition_title);
        rangeControls.add(goButton);
        //rangeControls.add(label_status); rangeControls.add(label_status_title);

        hideRangeControls();

        this.panel_top.setVisible(false);
    }


    private JTextField addTextField(JPanel p, String msg) {
        JTextField f = new JTextField(msg);
        p.add(f);
        return f;
    }


    private void RangeTextBoxKeypressed(java.awt.event.KeyEvent evt) {
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            this.setRangeFromTextBoxes();
        }
    }

    private void hideRangeControls() {
        changeVisibility(rangeControls, false);
    }

    private void showRangeControls() {
        changeVisibility(rangeControls, true);
    }

    private static void changeVisibility(List<JComponent> components, boolean isVisible) {
        for (JComponent j : components) {
            j.setEnabled(isVisible);
        }
    }

    private void initBookmarksTab(Container c) {
        //JPanel tablePanel = createTabPanel(jtp, "Bookmarks");
        favoriteSheet = new BookmarkSheet(this, c);
        favoriteController.addFavoritesChangedListener(favoriteSheet);
        favoriteController.addFavoritesChangedListener(this);
    }

    private void initBatchAnalyzeTab(JTabbedPane jtp) {

        JPanel tablePanel = createTabPanel(jtp, "Batch Run");

        JButton addBatchAnalysisButton = new JButton("Add Batch Analysis");
        addBatchAnalysisButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                BatchAnalysisForm baf = new BatchAnalysisForm();
                baf.setVisible(true);
                baf.setAlwaysOnTop(true);
            }

        });

        tablePanel.add(addBatchAnalysisButton);

    }

    private void initLogTab(JTabbedPane jtp) {
        //JPanel pan = createTabPanel(jtp, "Log");
        //this.initLog(pan);
        this.initLog(new JPanel());
    }

    private JPanel createTabPanel(JTabbedPane jtp, String name) {
        JPanel pan = new JPanel();
        pan.setLayout(new BorderLayout());
        pan.setBackground(ColourSettings.colorTabBackground);
        jtp.addTab(name, pan);
        return pan;
    }

    private Component getRigidPadding() {
        return Box.createRigidArea(new Dimension(BrowserSettings.padding, BrowserSettings.padding));
    }

    private JButton addButton(JPanel p, String label) {
        JButton b = new JButton(label);
        p.add(b);
        return b;
    }

    /**
     * [[ DIALOGS ]]
     *  Dialogs are forms which prompt the user
     *  to perform some action (e.g. open / save a file)
     */
    /**
     * Open Genome Dialog
     *  Prompt the user to open a genome file.
     *  Expects a Binary Fasta file (created using the
     *  data formatter included in the distribution)
     */
    void showOpenGenomeDialog() {

        if (ReferenceController.getInstance().isGenomeLoaded()) {
            int n = JOptionPane.showConfirmDialog(this,
            "A genome is already loaded. Replace existing genome?",
            "Replace genome",
            JOptionPane.YES_NO_OPTION);

            if (n != JOptionPane.YES_OPTION) {
                return;
            }
        }

        //Custom button text
        Object[] options = {"From file", "By length", "Cancel"};
        int n = JOptionPane.showOptionDialog(this,
            "How would you like to specify the genome?",
            "Specify a Genome",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]);

        if (n == 1) {
            GenomeLengthForm gf = new GenomeLengthForm();

            if (gf.userCompletedForm) {
                setGenome(gf.loadedGenome.getName(), gf.loadedGenome);
            } else {
                System.out.println("Length not set");
            }

        } else if (n == 0) {
            // create a frame and place the dialog in it
            JFrame jf = new JFrame();
            FileDialog fd = new FileDialog(jf, "Load Genome", FileDialog.LOAD);
//            fd.setFilenameFilter(new SavantFileFilter());
            /*
            fd.setFilenameFilter(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(DataFormatter.defaultExtension);
                }
            });
             */
            fd.setVisible(true);
            jf.setAlwaysOnTop(true);

            // get the path (null if none selected)
            String selectedFileName = fd.getFile();


            // set the genome
            if (selectedFileName != null) {
                selectedFileName = fd.getDirectory() + selectedFileName;
                Savant.log("Loading genome " + fd.getDirectory(), Savant.LOGMODE.NORMAL);
                setGenome(selectedFileName);
                Savant.log("Genome loaded", Savant.LOGMODE.NORMAL);
            } else {
                this.showOpenGenomeDialog();
            }
        }
    }

    /**
     * Open Tracks Dialog
     *  Prompt the user to open track file(s).
     *  Expects a Binary formatted file (created using the
     *  data formatter included in the distribution)
     */
    void showOpenTracksDialog() {

        if (!ReferenceController.getInstance().isGenomeLoaded()) {
            JOptionPane.showMessageDialog(this, "Load a genome first.");
            return;
        }
        
        List<String> filenames = new ArrayList<String>();

        if (mac) {
            // create a frame and place the dialog in it
            JFrame jf = new JFrame();

            FileDialog fd = new FileDialog(jf, "Open Tracks", FileDialog.LOAD);
            //        fd.setFilenameFilter(new SavantFileFilter());
            fd.setVisible(true);
            jf.setAlwaysOnTop(true);

            if (fd.getFile() == null) { return ;}

            String selectedFileName = fd.getDirectory() + fd.getFile();

            filenames.add(selectedFileName);
            
        } else {
            JFileChooser fc = new JFileChooser();
            fc.setMultiSelectionEnabled(true);
            fc.setDialogTitle("Open Tracks");

            int returnVal = fc.showOpenDialog(this);

            File[] selectedFiles = fc.getSelectedFiles();

            for (File f : selectedFiles) {
                filenames.add(f.getAbsolutePath());
            }
        }

        // set the track
        for (String selectedFileName : filenames) {
            try {
                addTrackFromFile(selectedFileName);
            } catch (Exception e) {
                promptUserToFormatFile(selectedFileName, e.getMessage());
            }
        }
    }

    /** [[EVENTS]]**/
    public void rangeSelectionChangeReceived(RangeSelectionChangedEvent event) {
        rangeController.setRange(event.range());
    }



    public void rangeChangeReceived(RangeChangedEvent event) {

        /*
        // adjust descriptions
        setRangeDescription(event.range());

        // adjust range controls
        setRangeSelectorFromRange();

        setRulerFromRange();

        // draw all frames
        FrameController fc = FrameController.getInstance();

        // Get current time
        long start = System.currentTimeMillis();

        updateProgress("Redrawing...");
        spinProgress();

        fc.drawFrames();

        stopSpinningProgress();

        // Get elapsed time in milliseconds
        long elapsedTimeMillis = System.currentTimeMillis()-start;

        // Get elapsed time in seconds
        float elapsedTimeSec = elapsedTimeMillis/1000F;

        updateProgress("Took " + elapsedTimeSec + " s");
         */

    }

    public void updateRange() {
        // adjust descriptions
        setRangeDescription(RangeController.getInstance().getRange());

        // adjust range controls
        setRangeSelectorFromRange();

        setRulerFromRange();

        // draw all frames
        FrameController fc = FrameController.getInstance();

        // Get current time
        //long start = System.currentTimeMillis();

        //updateProgress("Redrawing...");
        //spinProgress();

        fc.drawFrames();

        //stopSpinningProgress();

        // Get elapsed time in milliseconds
        //long elapsedTimeMillis = System.currentTimeMillis()-start;

        // Get elapsed time in seconds
        //float elapsedTimeSec = elapsedTimeMillis/1000F;

        //updateProgress("Took " + elapsedTimeSec + " s");

    }

    /** [[ GETTERS AND SETTERS ]] */


    //public List<TrackDocument> getTrackDocuments() { return this.frames; }
    //public DockPanel getDockPanel() { return this.DOCKPANEL; }
    
    


    public void promptUserToFormatFile(String fileName, String message) {

        String title = "Unrecognized file";
        // display the JOptionPane showConfirmDialog
        int reply = JOptionPane.showConfirmDialog(this, message, title, JOptionPane.YES_NO_OPTION);
        if (reply == JOptionPane.YES_OPTION)
        {
            if (!dff.isVisible()) {
                openAfterFormat = true;
                dff.clear();
                dff.setInFile(fileName);
                dff.setVisible(true);
            }
        }
    }

        /**
     * Genome
     */


    /**
     * Set the genome at specified path.
     * @param filename The name of the genome file containing file to be set
     */
    private void setGenome(String filename) {
        try {
            Genome g = new Genome(filename);
            setGenome(filename, g);
        } catch (FileNotFoundException ex) {
        } catch (Exception ex) {
            promptUserToFormatFile(filename, "This file does not appear to be formatted. Format now?");
        }
    }

    /**
     * Set the genome.
     * @param genome The genome to set
     */
    private void setGenome(String filename, Genome genome) {

        boolean someGenomeSetAlready = ReferenceController.getInstance().isGenomeLoaded();

        if(someGenomeSetAlready && this.genomeFrame != null){
            this.getTrackDockingManager().removeFrame(this.genomeFrame.getTitle());
            this.genomeFrame = null;
        }

        ReferenceController.getInstance().setGenome(genome);

        //rangeController.setMaxRange(new Range(1, genome.getLength()));
        //rangeSelector.setMaximum(genome.getLength());

        if (genome.isSequenceSet()) {
            try {
                DockableFrame df = DockableFrameFactory.createGenomeFrame(MiscUtils.getFilenameFromPath(filename));
                JPanel panel = (JPanel) df.getContentPane();
                List<ViewTrack> tracks = new ArrayList<ViewTrack>();
                tracks.add(new SequenceViewTrack(genome.getName(), genome));

                //////////////////////////////////////////////////

                panel.setLayout(new BorderLayout());
                //JLayeredPane layers = new JLayeredPane();
                //layers.setLayout(new BorderLayout());
                //panel.add(layers);

                //////////////////////////////////////////////////

                Frame frame = new Frame(tracks, df.getName());
                JLayeredPane layers = (JLayeredPane) frame.getFrameLandscape();
                panel.add(layers);
                frame.setDockableFrame(df);
                FrameController.getInstance().addFrame(frame, panel);
                this.getTrackDockingManager().addFrame(df);

                dockFrameToFrameMap.put(df, frame);
                this.genomeFrame = df;
                
            } catch (FileNotFoundException ex) {
                Logger.getLogger(Savant.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        if (!someGenomeSetAlready) {
            rangeController.setRange(1, Math.min(1000,genome.getLength()));
            rangeSelector.setActive(true);
            ruler.setActive(true);

            this.panel_top.setVisible(true);
            this.menuItem_viewRangeControls.setSelected(true);

            this.rangeSelector.setVisible(true);
            this.menuitem_genomeview.setSelected(true);

            this.ruler.setVisible(true);
            this.menuitem_ruler.setSelected(true);

            showRangeControls();
        }

        //button_genome.setEnabled(false);
        //menuitem_genome.setEnabled(false);

        updateReferenceNamesList();
        referenceDropdown.setSelectedIndex(0);
        //referenceDropdown.setSelectedIndex(1); // dont set it to 0 because that item is not allowed to be selected
    }

    /**
     * Set range description.
     *  - Change the from and to textboxes.
     * @param range
     */
    void setRangeDescription(Range range) {
        textboxFrom.setText(MiscUtils.intToString(range.getFrom()));
        textboxTo.setText(MiscUtils.intToString(range.getTo()));
        label_length.setText(MiscUtils.intToString(range.getLength()));
    }

    /**
     * Set the current range from the rangeSelector.
     */
    void setRangeFromRangeSelection() {
        int minRange = rangeSelector.getLowerPosition();
        int maxRange = rangeSelector.getUpperPosition();

        rangeController.setRange(minRange, maxRange);
    }

    /**
     * Set the current range from the Zoom track bar.
     */
    void setRangeFromTextBoxes() {

        String fromtext = textboxFrom.getText().trim();
        String totext = textboxTo.getText().trim();

        int from, to;

        if (fromtext.startsWith("-")) {
            fromtext = MiscUtils.removeChar(fromtext,'-');
            int diff = MiscUtils.stringToInt(MiscUtils.removeChar(fromtext, ','));
            to = MiscUtils.stringToInt(MiscUtils.removeChar(textboxTo.getText(), ','));
            from = to - diff + 1;
        } else if (totext.startsWith("+")) {
            totext = MiscUtils.removeChar(totext,'+');
            int diff = MiscUtils.stringToInt(MiscUtils.removeChar(totext, ','));
            from = MiscUtils.stringToInt(MiscUtils.removeChar(textboxFrom.getText(), ','));
            to = from + diff - 1;
        } else {
            from = MiscUtils.stringToInt(MiscUtils.removeChar(textboxFrom.getText(), ','));
            to = MiscUtils.stringToInt(MiscUtils.removeChar(textboxTo.getText(), ','));
        }

        if (from <= 0) {
            JOptionPane.showMessageDialog(this, "Invalid start value.");
            textboxFrom.requestFocus();
            return;
        }

        if (to <= 0) {
            JOptionPane.showMessageDialog(this, "Invalid end value.");
            textboxTo.requestFocus();
            return;
        }

        if (from > to) {
            //MessageBox.Show("INVALID RANGE");
            JOptionPane.showMessageDialog(this, "Invalid range.");
            textboxTo.requestFocus();
            return;
        }

        rangeController.setRange(from, to);
    }

    /**
     * Set the range selection upper and lower values
     * from the current range.
     */
    void setRangeSelectorFromRange() {
        rangeSelector.setRange(rangeController.getRangeStart(), rangeController.getRangeEnd());
    }

    /**
     * Set the ruler from the current range.
     */
    void setRulerFromRange() {
        ruler.setRulerRange(rangeController.getRange());
    }

    /** == [[ DOCKING ]] ==
     *  Components (such as frames, the Task Pane, etc.)
     *  can be docked to regions of the UI
     */

     /**
      * Start the log
     */
    private void initLog(JPanel pan) {
        if (log != null) {
            JOptionPane.showMessageDialog(this, "Log already started");
            return;
        }

        log = new JTextArea();
        log.setFont(new Font(BrowserSettings.fontName, Font.PLAIN, 18));
        JScrollPane jsp = new JScrollPane(log);

        pan.add(jsp);

        log(log, "LOG STARTED");
    }

    /**
     * Stop the log
     */
    private void killLog() {
        log = null;
    }

    /**
     * Update the log
     */
    private void updateLog() {
        if (log == null) {
            return;
        }
        log(log, "Range change to: " + rangeController.getRange());
        // TODO: add track information
    }

    /**
     * Get a string formatted for the log (with a new line).
     * @param s The message to format
     * @return A string based on the given message to be logged
     */
    private static String logMessage(String s) {
        return "[" + MiscUtils.now() + "]\t" + s + "\n";
    }

    /**
     * Get a string formatted for the log (without a new line).
     * @param s The message to format
     * @return A string based on the given message to be logged
     */
    private static String logMessageN(String s) {
        return "[" + MiscUtils.now() + "]\t" + s;
    }

    /**
     * Update the status bar
     * @param msg
     *
    private void updateProgress(String msg) {
        this.progressbar_status.setString(msg);
    }
    private void spinProgress() {
        this.progressbar_status.setIndeterminate(true);
    }
    private void stopSpinningProgress() {
        this.progressbar_status.setIndeterminate(false);
    }
     */

    private void initStatusBar() {
        toolbar_bottom.add(Box.createGlue(),2);

        MemoryStatusBarItem gc = new MemoryStatusBarItem();
        gc.setMaximumSize(new Dimension(100,30));
        gc.setFillColor(Color.lightGray);
        this.toolbar_bottom.add(gc);
    }
     

    public void bookmarksChangeReceived(BookmarksChangedEvent event) {
        if (!showBookmarksChangedDialog) { return; }
        //Custom button text
        Object[] options = {"OK",
                            "Don't show again"};
        int n = JOptionPane.showOptionDialog(Savant.getInstance(),
            event.message(),
            "Bookmarks changed",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.INFORMATION_MESSAGE,
            null,
            options,
            options[0]);

        if (n == 1) {
            showBookmarksChangedDialog = false;
        }
        //JOptionPane.showMessageDialog(this, , , JOptionPane.INFORMATION_MESSAGE);
    }

    private void updateReferenceNamesList() {

        System.out.println("Updating reference names list");

        String currref = (String) this.referenceDropdown.getSelectedItem();

        List<String> genomicrefnames = MiscUtils.set2List(ReferenceController.getInstance().getReferenceNames());

        referenceDropdown.removeAllItems();

        //this.referenceDropdown.addItem("[ GENOMIC (" + genomicrefnames.size() + ") ]");
        for (String s : genomicrefnames) { this.referenceDropdown.addItem(s); }

        //this.referenceDropdown.addItem("[ NON-GENOMIC (" + nongenomicrefnames.size() + ") ]");
        List<String> nongenomicrefnames = MiscUtils.set2List(ReferenceController.getInstance().getNonGenomicReferenceNames());
        if (nongenomicrefnames.size() > 0) {
            for (String s : nongenomicrefnames) { this.referenceDropdown.addItem(s); }
        }

        if (currref != null) { this.referenceDropdown.setSelectedItem(currref); }
    }

    public void referenceChangeReceived(ReferenceChangedEvent event) {
        this.referenceDropdown.setSelectedItem(event.getReferenceName());
        Genome loadedGenome = ReferenceController.getInstance().getGenome();
        rangeController.setMaxRange(new Range(1, loadedGenome.getLength()));
        rangeSelector.setMaximum(loadedGenome.getLength());
        rangeController.setRange(1, Math.min(1000,loadedGenome.getLength()));
    }

    public void trackListChangeReceived(TrackListChangedEvent evt) {
        updateReferenceNamesList();
    }

    public enum LOGMODE { NORMAL, DEBUG };

    /**
     * log a message on the given text area
     * @param rtb The text area on which to post the message
     * @param msg The message to post
     */
    public static void log(JTextArea rtb, String msg) {
        log(rtb,msg,LOGMODE.DEBUG);
        //rtb.append(logMessage(msg));
        //rtb.setCaretPosition(rtb.getText().length());
    }

    public static void log(JTextArea rtb, String msg, LOGMODE logmode) {
        if (logmode == LOGMODE.DEBUG && isDebugging) {
            rtb.append(logMessage(msg));
            rtb.setCaretPosition(rtb.getText().length());
        } else if (logmode != LOGMODE.DEBUG) {
            rtb.append(logMessage(msg));
            rtb.setCaretPosition(rtb.getText().length());
        }
    }

    /**
     * log a message on the default log text area
     * @param msg The message to post
     */
    public static void log(String msg) {
        log(msg,LOGMODE.DEBUG);
    }
    
    public static void log(String msg, LOGMODE logmode) {
        /*
        if (logmode == LOGMODE.DEBUG && isDebugging) {
            log.append(logMessage(msg));
            log.setCaretPosition(log.getText().length());
        } else if (logmode != LOGMODE.DEBUG) {
            log.append(logMessage(msg));
            log.setCaretPosition(log.getText().length());
        }
         *
         */
    }

    /**
     * Get whether or not a genome has been loaded.
     * @return True iff a genome has been loaded
     *
    public boolean isGenomeLoaded() {
        return getGenome() != null;
    }
     */

    /*
    public JButton createDetailsDialogButton() {

        Action a = new AbstractAction("Show Details Dialog") {
            public void actionPerformed(ActionEvent e) {
                String details = ("java.lang.Exception: Stack trace\n" +
                        "\tat java.awt.Component.processMouseEvent(Component.java:5957)\n" +
                        "\tat javax.swing.JComponent.processMouseEvent(JComponent.java:3284)\n" +
                        "\tat java.awt.Component.processEvent(Component.java:5722)\n" +
                        "\tat java.awt.Container.processEvent(Container.java:1966)\n" +
                        "\tat java.awt.Component.dispatchEventImpl(Component.java:4365)\n" +
                        "\tat java.awt.Container.dispatchEventImpl(Container.java:2024)\n" +
                        "\tat java.awt.Component.dispatchEvent(Component.java:4195)\n" +
                        "\tat java.awt.LightweightDispatcher.retargetMouseEvent(Container.java:4228)\n" +
                        "\tat java.awt.LightweightDispatcher.processMouseEvent(Container.java:3892)\n" +
                        "\tat java.awt.LightweightDispatcher.dispatchEvent(Container.java:3822)\n" +
                        "\tat java.awt.Container.dispatchEventImpl(Container.java:2010)\n" +
                        "\tat java.awt.Window.dispatchEventImpl(Window.java:2299)\n" +
                        "\tat java.awt.Component.dispatchEvent(Component.java:4195)\n" +
                        "\tat java.awt.EventQueue.dispatchEvent(EventQueue.java:599)\n" +
                        "\tat java.awt.EventDispatchThread.pumpOneEventForFilters(EventDispatchThread.java:273)\n" +
                        "\tat java.awt.EventDispatchThread.pumpEventsForFilter(EventDispatchThread.java:183)\n" +
                        "\tat java.awt.EventDispatchThread.pumpEventsForHierarchy(EventDispatchThread.java:173)\n" +
                        "\tat java.awt.EventDispatchThread.pumpEvents(EventDispatchThread.java:168)\n" +
                        "\tat java.awt.EventDispatchThread.pumpEvents(EventDispatchThread.java:160)\n" +
                        "\tat java.awt.EventDispatchThread.run(EventDispatchThread.java:121)");
                JideOptionPane optionPane = new JideOptionPane("Click \"Details\" button to see more information ... ", JOptionPane.ERROR_MESSAGE, JideOptionPane.CLOSE_OPTION);
                optionPane.setTitle("An exception happened during file transfers - if the title is very long, it will wrap automatically.");
                optionPane.setDetails(details);
                JDialog dialog = optionPane.createDialog(Savant.getInstance(), "Warning");
                dialog.setResizable(true);
                dialog.pack();
                dialog.setVisible(true);
            }
        };
        return createButton(a);
    }

    public JButton createButton(Action a) {
        JButton b = new JButton() {
            @Override
            public Dimension getMaximumSize() {
                int width = Short.MAX_VALUE;
                int height = super.getMaximumSize().height;
                return new Dimension(width, height);
            }
        };
        // setting the following client property informs the button to show
        // the action text as it's name. The default is to not show the
        // action text.
        b.putClientProperty("displayActionText", Boolean.TRUE);
        b.setAction(a);
        // b.setAlignmentX(JButton.CENTER_ALIGNMENT);
        return b;
    }
     * 
     */
    
    public void propertyChange(PropertyChangeEvent propertyChangeEvent) {

        if (propertyChangeEvent.getPropertyName().equals("success")) {
            if ((Boolean)propertyChangeEvent.getNewValue() == true) {
                if (openAfterFormat) {
                    String outfilepath = dff.getOutputFilePath();
                    if (dff.getFileType() == FileType.SEQUENCE_FASTA) {
                       this.setGenome(outfilepath);
                    } else {
                        try{
                            addTrackFromFile(outfilepath);
                        } catch (Exception e) {
                            promptUserToFormatFile(outfilepath, e.getMessage());
                        }
                    }
                }
                JOptionPane.showMessageDialog(this, "Format successful", "Format File", JOptionPane.INFORMATION_MESSAGE);
            }
            else {

                String details = (dff.getMessage());
                JideOptionPane optionPane = new JideOptionPane("Click \"Details\" button to see more information ... \n\n"
                        + "Problems formatting files? Please copy these details and email them to savant@cs.toronto.edu \n"
                        + "along with the file you are trying to format (if it is under 10MB in size). The Savant Team will \n"
                        + "be happy to help troubleshoot the issue with you.", JOptionPane.ERROR_MESSAGE, JideOptionPane.CLOSE_OPTION);
                optionPane.setTitle("A problem was encountered while formatting.");
                JDialog dialog = optionPane.createDialog(this,"Format unsuccessful");
                dialog.setResizable(true);
                optionPane.setDetails(details);
                //optionPane.setDetailsVisible(true);
                dialog.pack();
                dialog.setVisible(true);

                /*
                String message = "Format was not successful! ";
                String extraMessage = dff.getMessage();
                String userMessage;
                if (extraMessage != null) {
                    userMessage = message + extraMessage;
                } else {
                    userMessage = message;
                }
                JOptionPane.showMessageDialog(this, userMessage, "Format File", JOptionPane.ERROR_MESSAGE);
                 * 
                 */
            }

        }
    }

    public void updateMousePosition() {
        GraphPaneController gpc = GraphPaneController.getInstance();
        int x = gpc.getMouseXPosition();
        int y = gpc.getMouseYPosition();
        this.label_mouseposition.setText("X: " + MiscUtils.intToString(x) + ((y == -1) ? "" : " Y: " +  MiscUtils.intToString(y)));
    }

    public String[] getSelectedTracks(boolean multiple, String title){
        TrackChooser tc = new TrackChooser(Savant.getInstance(), multiple, title);
        String[] tracks = tc.getSelectedTracks();
        return tracks;
    }

    private void showOpenURLDialog() {
        urlDialog.setVisible(true);

        if (urlDialog.isAccepted()) {
            String urlString = urlDialog.getUrlAsString();
            try {
                URL url = new URL(urlString);
                if (!url.getProtocol().equalsIgnoreCase("http") || !urlString.endsWith(".bam")) {
                    DialogUtils.displayMessage("Only BAM files accessible via HTTP can be opened via URL.");
                    return;
                }
            } catch (MalformedURLException e) {
                // ignore, since it was already caught by the dialog and should never happen here
            }
            try {
                addTrackFromFile(urlDialog.getUrlAsString());
            } catch (IOException e) {
                DialogUtils.displayException("Load Track from URL", "Error opening remote BAM file", e);
            }
        }
    }

    private void displayAuxPanels(){
        setFrameVisibility("Bookmarks", true, this.getAuxDockingManager());
        setFrameVisibility("Table View", true, this.getAuxDockingManager());

        List<String> names = this.getAuxDockingManager().getAllFrameNames();
        for(int i = 0; i < names.size(); i++){
            this.getAuxDockingManager().toggleAutohideState(names.get(i));
        }

        menu_bookmarks.setState(true);
    }
}
