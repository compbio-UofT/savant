/*
 *    Copyright 2009-2011 University of Toronto
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

import java.awt.*;
import java.awt.event.*;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import javax.jnlp.BasicService;
import javax.jnlp.ServiceManager;
import javax.jnlp.UnavailableServiceException;
import javax.swing.*;

import com.apple.eawt.*;
import com.jidesoft.docking.*;
import com.jidesoft.docking.DockingManager.FrameHandle;
import com.jidesoft.docking.event.DockableFrameAdapter;
import com.jidesoft.docking.event.DockableFrameEvent;
import com.jidesoft.plaf.LookAndFeelFactory;
import com.jidesoft.plaf.UIDefaultsLookup;
import com.jidesoft.plaf.basic.ThemePainter;
import com.jidesoft.status.MemoryStatusBarItem;
import com.jidesoft.swing.JideSplitPane;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.JDOMException;

import savant.api.util.DialogUtils;
import savant.controller.*;
import savant.controller.event.*;
import savant.data.sources.DataSource;
import savant.data.types.Genome;
import savant.data.types.Genome.Auxiliary;
import savant.experimental.XMLTool;
import savant.plugin.builtin.SAFEDataSourcePlugin;
import savant.plugin.builtin.SavantFileRepositoryDataSourcePlugin;
import savant.settings.*;
import savant.swing.component.TrackChooser;
import savant.util.DownloadFile;
import savant.util.MiscUtils;
import savant.util.Range;
import savant.view.dialog.*;
import savant.view.icon.SavantIconFactory;
import savant.view.tools.ToolsModule;
import savant.view.swing.start.StartPanel;
import savant.xml.XMLVersion;
import savant.xml.XMLVersion.Version;


/**
 * Main application Window (Frame).
 *
 * @author mfiume
 */
public class Savant extends JFrame implements BookmarksChangedListener, ReferenceChangedListener {

    private static final Log LOG = LogFactory.getLog(Savant.class);
    public static boolean turnExperimentalFeaturesOff = true;
    private static boolean isDebugging = false;
    private DockingManager auxDockingManager;
    private JPanel masterPlaceholderPanel;
    private JPanel trackBackground;
    private DockingManager trackDockingManager;
    private JPanel trackPanel;
    private NavigationBar navigationBar;
    private ToolsModule savantTools;
    static boolean showNonGenomicReferenceDialog = true;
    private static boolean showBookmarksChangedDialog = false; // turned off, its kind of annoying
    public static final int osSpecificModifier = (MiscUtils.MAC ? java.awt.event.InputEvent.META_MASK : java.awt.event.InputEvent.CTRL_MASK);
    private DataFormatForm dff;
    private MemoryStatusBarItem memorystatusbar;
    private Application macOSXApplication;
    private ProjectHandler projectHandler;
    private boolean browserControlsShown = false;

    //web start
    static BasicService basicService = null;
    static boolean webStart = false;
    private StartPanel startpage;

    public void addToolBar(JToolBar b) {
        this.panel_toolbar.setLayout(new BoxLayout(this.panel_toolbar, BoxLayout.X_AXIS));
        this.panel_toolbar.add(b);
        updateToolBarVisibility();
    }

    public void removeToolBar(JToolBar b) {
        this.panel_toolbar.remove(b);
        updateToolBarVisibility();
    }

    private void updateToolBarVisibility() {
        if (this.panel_toolbar.getComponentCount() == 0) {
            setToolBarVisibility(false);
        } else {
            setToolBarVisibility(true);
        }
    }

    private void setToolBarVisibility(boolean isVisible) {
        this.panel_toolbar.setVisible(isVisible);
        this.menuItem_viewtoolbar.setSelected(isVisible);
    }

    public Frame addTrackFromFile(String selectedFileName) {

        URI uri = null;
        try {
            uri = new URI(selectedFileName);
            if (uri.getScheme() == null) {
                uri = new File(selectedFileName).toURI();
            }
        } catch (URISyntaxException usx) {
            // This can happen if we're passed a file-name containing spaces.
            uri = new File(selectedFileName).toURI();
        }
        return addTrackFromURI(uri);
    }

    public Frame addTrackFromURI(URI uri) {
        LOG.info("Loading track " + uri);
        Frame frame = DockableFrameFactory.createTrackFrame();
        //Force a unique frame key. The title of frame is overwritten by track name later.
        frame.setKey(uri.toString()+System.nanoTime());
        TrackFactory.createTrack(uri, frame);
        addTrackFrame(frame);
        return frame;
    }

    /**
     * Create a frame for a track (or bundled tracks) which already exists.
     *
     * @param name the name for the new frame
     * @param tracks the tracks to be added to the frame
     */
    public Frame createFrameForExistingTrack(Track[] tracks) {
        Frame frame = DockableFrameFactory.createTrackFrame();
        frame.setTracks(tracks);
        LOG.trace("Savant.createFrameForExistingTrack calling trackDockingManager.addFrame");
        addTrackFrame(frame);
        LOG.info("Frame created for track " + frame.getName());
        return frame;
    }

    /** == [[ DOCKING ]] ==
     *  Components (such as frames, the Task Pane, etc.)
     *  can be docked to regions of the UI
     */
    private void initDocking() {

        masterPlaceholderPanel = new JPanel();
        masterPlaceholderPanel.setLayout(new BorderLayout());

        this.panel_main.setLayout(new BorderLayout());
        this.panel_main.add(masterPlaceholderPanel, BorderLayout.CENTER);

        auxDockingManager = new DefaultDockingManager(this, masterPlaceholderPanel);
        masterPlaceholderPanel.setBackground(ColourSettings.getSplitter());
        //auxDockingManager.setSidebarRollover(false);
        auxDockingManager.getWorkspace().setBackground(ColourSettings.getSplitter());
        auxDockingManager.setInitSplitPriority(DockingManager.SPLIT_EAST_SOUTH_WEST_NORTH);
        //auxDockingManager.loadLayoutData();

        trackPanel = new JPanel();
        trackPanel.setLayout(new BorderLayout());

        auxDockingManager.getWorkspace().add(trackPanel, BorderLayout.CENTER);

        trackDockingManager = new DefaultDockingManager(this, trackPanel);
        trackPanel.setBackground(ColourSettings.getSplitter());
        trackDockingManager.getWorkspace().setBackground(ColourSettings.getSplitter());
        //trackDockingManager.setSidebarRollover(false);
        trackDockingManager.getWorkspace().setBackground(Color.red);
        trackDockingManager.setInitNorthSplit(JideSplitPane.VERTICAL_SPLIT);
        //trackDockingManager.loadLayoutData();

        auxDockingManager.setShowInitial(false);
        trackDockingManager.setShowInitial(false);
        auxDockingManager.loadLayoutData();
        trackDockingManager.loadLayoutData();

        rangeSelector = new RangeSelectionPanel();
        rangeSelector.setPreferredSize(new Dimension(10000, 23));
        rangeSelector.setMaximumSize(new Dimension(10000, 23));
        rangeController.addRangeChangedListener(rangeSelector);
        rangeSelector.setVisible(false);

        referenceCombo = new ReferenceCombo();
        referenceCombo.setVisible(false);

        ruler = new Ruler();
        ruler.setPreferredSize(new Dimension(10000, 23));
        ruler.setMaximumSize(new Dimension(10000, 23));
        ruler.setVisible(false);

        Box box1 = Box.createHorizontalBox();
        box1.add(referenceCombo);
        box1.add(rangeSelector);

        Box box2 = Box.createVerticalBox();
        box2.add(box1);
        box2.add(ruler);

        trackPanel.add(box2, BorderLayout.NORTH);

        trackBackground = new JPanel();
        trackBackground.setBackground(Color.darkGray);

        trackBackground.setLayout(new BorderLayout());

        trackDockingManager.getWorkspace().add(trackBackground);
        trackDockingManager.addDockableFrameListener(new DockableFrameAdapter() {
            @Override
            public void dockableFrameRemoved(DockableFrameEvent arg0) {
                FrameController.getInstance().closeFrame((Frame)arg0.getDockableFrame());
            }

            @Override
            public void dockableFrameActivated(DockableFrameEvent arg0) {
                ((Frame)arg0.getDockableFrame()).setActiveFrame();
            }

            @Override
            public void dockableFrameDeactivated(DockableFrameEvent arg0) {
                ((Frame)arg0.getDockableFrame()).setInactiveFrame();
            }
        });
        trackDockingManager.setAllowedDockSides(DockContext.DOCK_SIDE_HORIZONTAL);

        // make sure only one active frame
        addDockingManagerToGroup(auxDockingManager);
        addDockingManagerToGroup(trackDockingManager);
    }
    /** Minimum and maximum dimensions of the browser form */
    static int minimumFormWidth = 500;
    static int minimumFormHeight = 500;
    /** The loaded genome */
    //private Genome loadedGenome;
    /** The log */
    private static JTextArea log;
    private ReferenceCombo referenceCombo;
    /** Click and drag control for range selection */
    private RangeSelectionPanel rangeSelector;
    private Ruler ruler;
    /** Information & Analysis Tabbed Pane (for plugin use) */
    private JTabbedPane auxTabbedPane;
    /**
     * Info
     */
    private static BookmarkSheet favoriteSheet;
    private RangeController rangeController = RangeController.getInstance();
    private BookmarkController favoriteController = BookmarkController.getInstance();
    private SelectionController selectionController = SelectionController.getInstance();
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
            basicService = (BasicService)ServiceManager.lookup("javax.jnlp.BasicService");
            webStart = true;
        } catch (UnavailableServiceException e) {
            //System.err.println("Lookup failed: " + e);
            webStart = false;
        }

        instance = this;

        Splash s = new Splash(instance, false);
        s.setVisible(true);

        loadPreferences();

        //removeTmpFiles();

        addComponentListener(new ComponentAdapter() {

            /**
             * Resize the form to the minimum size if the
             * user has resized it to something smaller.
             * @param e The resize event
             */
            @Override
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
        });

        s.setStatus("Initializing GUI");

        initComponents();
        customizeUI();

        init();

        if (BrowserSettings.getCheckVersionOnStartup()) {
            s.setStatus("Checking version");
            checkVersion();
        }

        if (BrowserSettings.getCollectAnonymousUsage()) {
            logUsageStats();
        }

        s.setStatus("Loading plugins");

        PluginController pc = PluginController.getInstance();
        if (DataSourcePluginController.getInstance().hasOnlySavantRepoDataSource()) {
            loadFromDataSourcePlugin.setText("Load Track from Repository...");
        }

        s.setStatus("Organizing layout");

        displayAuxPanels();

        if (turnExperimentalFeaturesOff) {
            disableExperimentalFeatures();
        }

        s.setVisible(false);

        makeGUIVisible();
    }

    private void initXMLTools() {
        try {
            File dir = DirectorySettings.getXMLToolDescriptionsDirectory();
            for (File f : dir.listFiles()) {
                if (f.getName().toLowerCase().endsWith(".xml")) {
                    ToolsModule.addTool(new XMLTool(f));
                }
            }
        } catch (IOException ex) {
        } catch (JDOMException ex) {
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
        jLabel1 = new javax.swing.JLabel();
        label_status = new javax.swing.JLabel();
        s_e_sep = new javax.swing.JToolBar.Separator();
        label_memory = new javax.swing.JLabel();
        panel_toolbar = new javax.swing.JPanel();
        menuBar_top = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        loadGenomeItem = new javax.swing.JMenuItem();
        loadFromFileItem = new javax.swing.JMenuItem();
        loadFromURLItem = new javax.swing.JMenuItem();
        loadFromDataSourcePlugin = new javax.swing.JMenuItem();
        recentTrackMenu = new javax.swing.JMenu();
        javax.swing.JPopupMenu.Separator jSeparator1 = new javax.swing.JPopupMenu.Separator();
        openProjectItem = new javax.swing.JMenuItem();
        recentProjectMenu = new javax.swing.JMenu();
        saveProjectItem = new javax.swing.JMenuItem();
        saveProjectAsItem = new javax.swing.JMenuItem();
        javax.swing.JPopupMenu.Separator jSeparator2 = new javax.swing.JPopupMenu.Separator();
        formatItem = new javax.swing.JMenuItem();
        javax.swing.JPopupMenu.Separator jSeparator3 = new javax.swing.JPopupMenu.Separator();
        javax.swing.JMenuItem exportItem = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JPopupMenu.Separator();
        exitItem = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        menuitem_undo = new javax.swing.JMenuItem();
        menuitem_redo = new javax.swing.JMenuItem();
        javax.swing.JPopupMenu.Separator jSeparator6 = new javax.swing.JPopupMenu.Separator();
        menuItemAddToFaves = new javax.swing.JMenuItem();
        menuitem_deselectall = new javax.swing.JMenuItem();
        jSeparator7 = new javax.swing.JPopupMenu.Separator();
        menuitem_preferences = new javax.swing.JMenuItem();
        viewMenu = new javax.swing.JMenu();
        menuItemPanLeft = new javax.swing.JMenuItem();
        menuItemPanRight = new javax.swing.JMenuItem();
        menuItemZoomIn = new javax.swing.JMenuItem();
        menuItemZoomOut = new javax.swing.JMenuItem();
        menuItemShiftStart = new javax.swing.JMenuItem();
        menuItemShiftEnd = new javax.swing.JMenuItem();
        javax.swing.JSeparator jSeparator8 = new javax.swing.JSeparator();
        menuitem_aim = new javax.swing.JCheckBoxMenuItem();
        menuitem_view_plumbline = new javax.swing.JCheckBoxMenuItem();
        menuitem_view_spotlight = new javax.swing.JCheckBoxMenuItem();
        windowMenu = new javax.swing.JMenu();
        menuItem_viewRangeControls = new javax.swing.JCheckBoxMenuItem();
        menuitem_genomeview = new javax.swing.JCheckBoxMenuItem();
        menuitem_ruler = new javax.swing.JCheckBoxMenuItem();
        menuItem_viewtoolbar = new javax.swing.JCheckBoxMenuItem();
        menuitem_statusbar = new javax.swing.JCheckBoxMenuItem();
        speedAndEfficiencyItem = new javax.swing.JCheckBoxMenuItem();
        javax.swing.JSeparator jSeparator9 = new javax.swing.JSeparator();
        menuitem_startpage = new javax.swing.JCheckBoxMenuItem();
        menuitem_tools = new javax.swing.JCheckBoxMenuItem();
        menu_bookmarks = new javax.swing.JCheckBoxMenuItem();
        pluginsMenu = new javax.swing.JMenu();
        menuitem_pluginmanager = new javax.swing.JMenuItem();
        jSeparator10 = new javax.swing.JPopupMenu.Separator();
        helpMenu = new javax.swing.JMenu();
        userManualItem = new javax.swing.JMenuItem();
        tutorialsItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem checkForUpdatesItem = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();
        jMenuItem1 = new javax.swing.JMenuItem();
        javax.swing.JSeparator jSeparator11 = new javax.swing.JSeparator();
        websiteItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setBackground(new java.awt.Color(204, 204, 204));

        panel_top.setMaximumSize(new java.awt.Dimension(1000, 30));
        panel_top.setMinimumSize(new java.awt.Dimension(0, 0));
        panel_top.setPreferredSize(new java.awt.Dimension(0, 30));
        panel_top.setLayout(new java.awt.BorderLayout());

        panelExtendedMiddle.setBackground(new java.awt.Color(51, 51, 51));
        panelExtendedMiddle.setMinimumSize(new java.awt.Dimension(0, 0));
        panelExtendedMiddle.setPreferredSize(new java.awt.Dimension(990, 25));

        javax.swing.GroupLayout panelExtendedMiddleLayout = new javax.swing.GroupLayout(panelExtendedMiddle);
        panelExtendedMiddle.setLayout(panelExtendedMiddleLayout);
        panelExtendedMiddleLayout.setHorizontalGroup(
            panelExtendedMiddleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 989, Short.MAX_VALUE)
        );
        panelExtendedMiddleLayout.setVerticalGroup(
            panelExtendedMiddleLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 30, Short.MAX_VALUE)
        );

        panel_top.add(panelExtendedMiddle, java.awt.BorderLayout.CENTER);

        panel_main.setBackground(new java.awt.Color(153, 153, 153));
        panel_main.setMaximumSize(new java.awt.Dimension(99999, 99999));
        panel_main.setMinimumSize(new java.awt.Dimension(1, 1));
        panel_main.setPreferredSize(new java.awt.Dimension(99999, 99999));

        javax.swing.GroupLayout panel_mainLayout = new javax.swing.GroupLayout(panel_main);
        panel_main.setLayout(panel_mainLayout);
        panel_mainLayout.setHorizontalGroup(
            panel_mainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 989, Short.MAX_VALUE)
        );
        panel_mainLayout.setVerticalGroup(
            panel_mainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 526, Short.MAX_VALUE)
        );

        toolbar_bottom.setFloatable(false);
        toolbar_bottom.setAlignmentX(1.0F);

        label_mouseposition_title.setText(" Position: ");
        toolbar_bottom.add(label_mouseposition_title);

        label_mouseposition.setText("mouse over track");
        toolbar_bottom.add(label_mouseposition);

        jLabel1.setText("Time: ");
        toolbar_bottom.add(jLabel1);

        label_status.setMaximumSize(new java.awt.Dimension(300, 14));
        label_status.setMinimumSize(new java.awt.Dimension(100, 14));
        label_status.setPreferredSize(new java.awt.Dimension(100, 14));
        toolbar_bottom.add(label_status);
        toolbar_bottom.add(s_e_sep);

        label_memory.setText(" Memory: ");
        toolbar_bottom.add(label_memory);

        panel_toolbar.setVisible(false);
        panel_toolbar.setPreferredSize(new java.awt.Dimension(856, 24));

        javax.swing.GroupLayout panel_toolbarLayout = new javax.swing.GroupLayout(panel_toolbar);
        panel_toolbar.setLayout(panel_toolbarLayout);
        panel_toolbarLayout.setHorizontalGroup(
            panel_toolbarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 989, Short.MAX_VALUE)
        );
        panel_toolbarLayout.setVerticalGroup(
            panel_toolbarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 24, Short.MAX_VALUE)
        );

        fileMenu.setText("File");

        loadGenomeItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_G, java.awt.event.InputEvent.CTRL_MASK));
        loadGenomeItem.setText("Load Genome...");
        loadGenomeItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadGenomeItemActionPerformed(evt);
            }
        });
        fileMenu.add(loadGenomeItem);

        loadFromFileItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_T, java.awt.event.InputEvent.CTRL_MASK));
        loadFromFileItem.setText("Load Track from File...");
        loadFromFileItem.setEnabled(false);
        loadFromFileItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadFromFileItemActionPerformed(evt);
            }
        });
        fileMenu.add(loadFromFileItem);

        loadFromURLItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_U, java.awt.event.InputEvent.CTRL_MASK));
        loadFromURLItem.setText("Load Track from URL...");
        loadFromURLItem.setEnabled(false);
        loadFromURLItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadFromURLItemActionPerformed(evt);
            }
        });
        fileMenu.add(loadFromURLItem);

        loadFromDataSourcePlugin.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_E, java.awt.event.InputEvent.CTRL_MASK));
        loadFromDataSourcePlugin.setText("Load Track from Other Datasource...");
        loadFromDataSourcePlugin.setEnabled(false);
        loadFromDataSourcePlugin.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadFromDataSourcePluginActionPerformed(evt);
            }
        });
        fileMenu.add(loadFromDataSourcePlugin);

        recentTrackMenu.setText("Load Recent Track");
        fileMenu.add(recentTrackMenu);
        fileMenu.add(jSeparator1);

        openProjectItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        openProjectItem.setText("Open Project...");
        openProjectItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openProjectItemActionPerformed(evt);
            }
        });
        fileMenu.add(openProjectItem);

        recentProjectMenu.setText("Open Recent Project");
        fileMenu.add(recentProjectMenu);

        saveProjectItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        saveProjectItem.setText("Save Project");
        saveProjectItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveProjectItemActionPerformed(evt);
            }
        });
        fileMenu.add(saveProjectItem);

        saveProjectAsItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        saveProjectAsItem.setText("Save Project As...");
        saveProjectAsItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveProjectAsItemActionPerformed(evt);
            }
        });
        fileMenu.add(saveProjectAsItem);
        fileMenu.add(jSeparator2);

        formatItem.setText("Format Text File...");
        formatItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                formatItemActionPerformed(evt);
            }
        });
        fileMenu.add(formatItem);
        fileMenu.add(jSeparator3);

        exportItem.setText("Export Track Images...");
        exportItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuitem_exportActionPerformed(evt);
            }
        });
        fileMenu.add(exportItem);
        fileMenu.add(jSeparator4);

        exitItem.setText("Exit");
        exitItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitItemActionPerformed(evt);
            }
        });
        fileMenu.add(exitItem);

        menuBar_top.add(fileMenu);

        editMenu.setText("Edit");

        menuitem_undo.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, java.awt.event.InputEvent.CTRL_MASK));
        menuitem_undo.setText("Undo Range Change");
        menuitem_undo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuitem_undoActionPerformed(evt);
            }
        });
        editMenu.add(menuitem_undo);

        menuitem_redo.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Y, java.awt.event.InputEvent.CTRL_MASK));
        menuitem_redo.setText("Redo Range Change");
        menuitem_redo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuitem_redoActionPerformed(evt);
            }
        });
        editMenu.add(menuitem_redo);
        editMenu.add(jSeparator6);

        menuItemAddToFaves.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_D, java.awt.event.InputEvent.CTRL_MASK));
        menuItemAddToFaves.setText("Bookmark");
        menuItemAddToFaves.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemAddToFavesActionPerformed(evt);
            }
        });
        editMenu.add(menuItemAddToFaves);

        menuitem_deselectall.setText("Deselect All");
        menuitem_deselectall.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuitem_deselectActionPerformed(evt);
            }
        });
        editMenu.add(menuitem_deselectall);
        editMenu.add(jSeparator7);

        menuitem_preferences.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, java.awt.event.InputEvent.CTRL_MASK));
        menuitem_preferences.setText("Preferences");
        menuitem_preferences.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuitem_preferencesActionPerformed(evt);
            }
        });
        editMenu.add(menuitem_preferences);

        menuBar_top.add(editMenu);

        viewMenu.setText("View");

        menuItemPanLeft.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_LEFT, java.awt.event.InputEvent.SHIFT_MASK));
        menuItemPanLeft.setText("Pan Left");
        menuItemPanLeft.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemPanLeftActionPerformed(evt);
            }
        });
        viewMenu.add(menuItemPanLeft);

        menuItemPanRight.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_RIGHT, java.awt.event.InputEvent.SHIFT_MASK));
        menuItemPanRight.setText("Pan Right");
        menuItemPanRight.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemPanRightActionPerformed(evt);
            }
        });
        viewMenu.add(menuItemPanRight);

        menuItemZoomIn.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_UP, java.awt.event.InputEvent.SHIFT_MASK));
        menuItemZoomIn.setText("Zoom In");
        menuItemZoomIn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemZoomInActionPerformed(evt);
            }
        });
        viewMenu.add(menuItemZoomIn);

        menuItemZoomOut.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DOWN, java.awt.event.InputEvent.SHIFT_MASK));
        menuItemZoomOut.setText("Zoom Out");
        menuItemZoomOut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemZoomOutActionPerformed(evt);
            }
        });
        viewMenu.add(menuItemZoomOut);

        menuItemShiftStart.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_HOME, 0));
        menuItemShiftStart.setText("Shift to Start");
        menuItemShiftStart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemShiftStartActionPerformed(evt);
            }
        });
        viewMenu.add(menuItemShiftStart);

        menuItemShiftEnd.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_END, 0));
        menuItemShiftEnd.setText("Shift to End");
        menuItemShiftEnd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItemShiftEndActionPerformed(evt);
            }
        });
        viewMenu.add(menuItemShiftEnd);
        viewMenu.add(jSeparator8);

        menuitem_aim.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_J, java.awt.event.InputEvent.CTRL_MASK));
        menuitem_aim.setText("Crosshair");
        menuitem_aim.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuitem_aimActionPerformed(evt);
            }
        });
        viewMenu.add(menuitem_aim);

        menuitem_view_plumbline.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_K, java.awt.event.InputEvent.CTRL_MASK));
        menuitem_view_plumbline.setText("Plumbline");
        menuitem_view_plumbline.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuitem_view_plumblineActionPerformed(evt);
            }
        });
        viewMenu.add(menuitem_view_plumbline);

        menuitem_view_spotlight.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, java.awt.event.InputEvent.CTRL_MASK));
        menuitem_view_spotlight.setText("Spotlight");
        menuitem_view_spotlight.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuitem_view_spotlightActionPerformed(evt);
            }
        });
        viewMenu.add(menuitem_view_spotlight);

        menuBar_top.add(viewMenu);

        windowMenu.setText("Window");
        windowMenu.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                windowMenuStateChanged(evt);
            }
        });

        menuItem_viewRangeControls.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        menuItem_viewRangeControls.setText("Navigation");
        menuItem_viewRangeControls.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItem_viewRangeControlsMousePressed(evt);
            }
        });
        windowMenu.add(menuItem_viewRangeControls);

        menuitem_genomeview.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        menuitem_genomeview.setText("Genome");
        menuitem_genomeview.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuitem_genomeviewActionPerformed(evt);
            }
        });
        windowMenu.add(menuitem_genomeview);

        menuitem_ruler.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        menuitem_ruler.setText("Ruler");
        menuitem_ruler.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuitem_rulerActionPerformed(evt);
            }
        });
        windowMenu.add(menuitem_ruler);

        menuItem_viewtoolbar.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_T, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        menuItem_viewtoolbar.setText("Plugin Toolbar");
        menuItem_viewtoolbar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuItem_viewtoolbarActionPerformed(evt);
            }
        });
        windowMenu.add(menuItem_viewtoolbar);

        menuitem_statusbar.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        menuitem_statusbar.setSelected(true);
        menuitem_statusbar.setText("Status Bar");
        menuitem_statusbar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuitem_statusbarActionPerformed(evt);
            }
        });
        windowMenu.add(menuitem_statusbar);

        speedAndEfficiencyItem.setText("Resources");
        speedAndEfficiencyItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                speedAndEfficiencyItemActionPerformed(evt);
            }
        });
        windowMenu.add(speedAndEfficiencyItem);
        windowMenu.add(jSeparator9);

        menuitem_startpage.setSelected(true);
        menuitem_startpage.setText("Start Page");
        menuitem_startpage.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuitem_startpageActionPerformed(evt);
            }
        });
        windowMenu.add(menuitem_startpage);

        menuitem_tools.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        menuitem_tools.setText("Tools");
        menuitem_tools.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuitem_toolsActionPerformed(evt);
            }
        });
        windowMenu.add(menuitem_tools);

        menu_bookmarks.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_B, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        menu_bookmarks.setText("Bookmarks");
        menu_bookmarks.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menu_bookmarksActionPerformed(evt);
            }
        });
        windowMenu.add(menu_bookmarks);

        menuBar_top.add(windowMenu);

        pluginsMenu.setText("Plugins");

        menuitem_pluginmanager.setText("Plugin Manager");
        menuitem_pluginmanager.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuitem_pluginmanagerActionPerformed(evt);
            }
        });
        pluginsMenu.add(menuitem_pluginmanager);
        pluginsMenu.add(jSeparator10);

        menuBar_top.add(pluginsMenu);

        helpMenu.setText("Help");

        userManualItem.setText("Manuals");
        userManualItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                userManualItemActionPerformed(evt);
            }
        });
        helpMenu.add(userManualItem);

        tutorialsItem.setText("Video Tutorials");
        tutorialsItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tutorialsItemActionPerformed(evt);
            }
        });
        helpMenu.add(tutorialsItem);

        checkForUpdatesItem.setText("Check for updates");
        checkForUpdatesItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkForUpdatesItemActionPerformed(evt);
            }
        });
        helpMenu.add(checkForUpdatesItem);

        jMenuItem2.setText("Report an issue");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        helpMenu.add(jMenuItem2);

        jMenuItem1.setText("Request a feature");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        helpMenu.add(jMenuItem1);
        helpMenu.add(jSeparator11);

        websiteItem.setText("Website");
        websiteItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                websiteItemActionPerformed(evt);
            }
        });
        helpMenu.add(websiteItem);

        menuBar_top.add(helpMenu);

        setJMenuBar(menuBar_top);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(panel_top, javax.swing.GroupLayout.DEFAULT_SIZE, 989, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addComponent(toolbar_bottom, javax.swing.GroupLayout.DEFAULT_SIZE, 977, Short.MAX_VALUE)
                .addContainerGap())
            .addComponent(panel_toolbar, javax.swing.GroupLayout.DEFAULT_SIZE, 989, Short.MAX_VALUE)
            .addComponent(panel_main, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 989, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(panel_top, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(panel_toolbar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(panel_main, javax.swing.GroupLayout.DEFAULT_SIZE, 526, Short.MAX_VALUE)
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

    private void menuitem_redoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuitem_redoActionPerformed
        RangeController rc = RangeController.getInstance();
        rc.redoRangeChange();
    }//GEN-LAST:event_menuitem_redoActionPerformed

    private void menuItemAddToFavesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemAddToFavesActionPerformed
        BookmarkController fc = BookmarkController.getInstance();
        fc.addCurrentRangeToBookmarks();
    }//GEN-LAST:event_menuItemAddToFavesActionPerformed

    private void formatItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_formatItemActionPerformed
        if (!dff.isVisible()) {
            LOG.info("Showing format form...");
            dff.clear();
            dff.setLocationRelativeTo(this);
            dff.setVisible(true);
        }
    }//GEN-LAST:event_formatItemActionPerformed

    private void exitItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitItemActionPerformed
        Savant.getInstance().askToDispose();
    }//GEN-LAST:event_exitItemActionPerformed

    private void loadGenomeItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadGenomeItemActionPerformed
        showOpenGenomeDialog();
    }//GEN-LAST:event_loadGenomeItemActionPerformed

    private void loadFromFileItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadFromFileItemActionPerformed
        if (!ReferenceController.getInstance().isGenomeLoaded()) {
            JOptionPane.showMessageDialog(this, "Load a genome first.");
            return;
        }

        File[] selectedFiles = DialogUtils.chooseFilesForOpen("Open Tracks", null, null);
        for (File f : selectedFiles) {
            // This creates the tracks asynchronously, which handles all exceptions internally.
            addTrackFromFile(f.getAbsolutePath());
        }
    }//GEN-LAST:event_loadFromFileItemActionPerformed

    private void loadFromURLItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadFromURLItemActionPerformed
        URL url = OpenURLDialog.getURL(this);
        if (url != null) {
            try {
                addTrackFromURI(url.toURI());
            } catch (URISyntaxException ignored) {
            }
        }
    }//GEN-LAST:event_loadFromURLItemActionPerformed

    private void websiteItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_websiteItemActionPerformed
        try {
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(BrowserSettings.URL));
        } catch (IOException ex) {
            LOG.error("Unable to access Savant website", ex);
        }
    }//GEN-LAST:event_websiteItemActionPerformed

    private void menuitem_pluginmanagerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuitem_pluginmanagerActionPerformed
        PluginManagerDialog pd = PluginManagerDialog.getInstance();
        pd.setLocationRelativeTo(this);
        pd.setVisible(true);
    }//GEN-LAST:event_menuitem_pluginmanagerActionPerformed

    private void menuitem_view_plumblineActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuitem_view_plumblineActionPerformed
        GraphPaneController gpc = GraphPaneController.getInstance();
        gpc.setPlumbing(this.menuitem_view_plumbline.isSelected());
    }//GEN-LAST:event_menuitem_view_plumblineActionPerformed

    public void setPlumbingMenutItemSelected(boolean isSelected) {
        this.menuitem_view_plumbline.setSelected(isSelected);
    }

    public void setSpotlightMenutItemSelected(boolean isSelected) {
        this.menuitem_view_spotlight.setSelected(isSelected);
    }

    public void setCrosshairMenutItemSelected(boolean isSelected) {
        this.menuitem_aim.setSelected(isSelected);
    }

    private void menuitem_view_spotlightActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuitem_view_spotlightActionPerformed
        GraphPaneController gpc = GraphPaneController.getInstance();
        gpc.setSpotlight(this.menuitem_view_spotlight.isSelected());
    }//GEN-LAST:event_menuitem_view_spotlightActionPerformed

    private void windowMenuStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_windowMenuStateChanged
        /*
        if(this.getAuxDockingManager().getFrame("Information & Analysis").isVisible() != this.menu_info.getState()){
        this.menu_info.setState(!this.menu_info.getState());
        }
        if(this.getAuxDockingManager().getFrame("Bookmarks").isVisible() != this.menu_bookmarks.getState()){
        this.menu_bookmarks.setState(!this.menu_bookmarks.getState());
        }
         */
    }//GEN-LAST:event_windowMenuStateChanged

    private void menu_bookmarksActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menu_bookmarksActionPerformed

        String frameKey = "Bookmarks";
        DockingManager m = this.getAuxDockingManager();
        boolean isVisible = m.getFrame(frameKey).isHidden();
        MiscUtils.setFrameVisibility(frameKey, isVisible, m);
        this.menu_bookmarks.setSelected(isVisible);
    }//GEN-LAST:event_menu_bookmarksActionPerformed

    private void tutorialsItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tutorialsItemActionPerformed
        try {
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(BrowserSettings.MEDIA_URL));
        } catch (IOException ex) {
            LOG.error("Unable to access online tutorials.", ex);
        }
    }//GEN-LAST:event_tutorialsItemActionPerformed

    private void userManualItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_userManualItemActionPerformed
        try {
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(BrowserSettings.DOCUMENTATION_URL));
        } catch (IOException ex) {
            LOG.error("Unable to access online user manual.", ex);
        }
    }//GEN-LAST:event_userManualItemActionPerformed

    private void menuitem_rulerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuitem_rulerActionPerformed
        this.ruler.setVisible(!this.ruler.isVisible());
    }//GEN-LAST:event_menuitem_rulerActionPerformed

    private void menuitem_genomeviewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuitem_genomeviewActionPerformed
        boolean flag = !rangeSelector.isVisible();
        rangeSelector.setVisible(flag);
        referenceCombo.setVisible(flag);
    }//GEN-LAST:event_menuitem_genomeviewActionPerformed

    private void menuitem_statusbarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuitem_statusbarActionPerformed
        this.toolbar_bottom.setVisible(!this.toolbar_bottom.isVisible());
    }//GEN-LAST:event_menuitem_statusbarActionPerformed

    private void menuitem_exportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuitem_exportActionPerformed
        //ExportImageDialog export = new ExportImageDialog(Savant.getInstance(), true);
        //export.setVisible(true);
        ExportImage unused = new ExportImage();
    }//GEN-LAST:event_menuitem_exportActionPerformed

    private void menuItemShiftStartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemShiftStartActionPerformed
        rangeController.shiftRangeFarLeft();
    }//GEN-LAST:event_menuItemShiftStartActionPerformed

    private void menuItemShiftEndActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItemShiftEndActionPerformed
        rangeController.shiftRangeFarRight();
    }//GEN-LAST:event_menuItemShiftEndActionPerformed
    static boolean arePreferencesInitialized = false;

    private void menuitem_preferencesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuitem_preferencesActionPerformed

        if (!arePreferencesInitialized) {
            SettingsDialog.addSection(new ColourSchemeSettingsSection());
            SettingsDialog.addSection(new TemporaryFilesSettingsSection());
            SettingsDialog.addSection(new GeneralSettingsSection());
            SettingsDialog.addSection(new RemoteFilesSettingsSection());
            SettingsDialog.addSection(new ResolutionSettingsSection());
            arePreferencesInitialized = true;
        }

        SettingsDialog.showOptionsDialog(this);
    }//GEN-LAST:event_menuitem_preferencesActionPerformed

    private void menuitem_toolsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuitem_toolsActionPerformed
        String frameKey = "Tools";
        DockingManager m = this.getAuxDockingManager();
        boolean isVisible = m.getFrame(frameKey).isHidden();
        MiscUtils.setFrameVisibility(frameKey, isVisible, m);
        this.menuitem_tools.setSelected(isVisible);
    }//GEN-LAST:event_menuitem_toolsActionPerformed

    private void menuitem_deselectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuitem_deselectActionPerformed
        SelectionController.getInstance().removeAll();
    }//GEN-LAST:event_menuitem_deselectActionPerformed

    private void saveProjectAsItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveProjectAsItemActionPerformed
        projectHandler.promptUserToSaveProjectAs();
    }//GEN-LAST:event_saveProjectAsItemActionPerformed

    private void openProjectItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openProjectItemActionPerformed
        projectHandler.promptUserToLoadProject();
    }//GEN-LAST:event_openProjectItemActionPerformed

    private void checkForUpdatesItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkForUpdatesItemActionPerformed
        checkVersion(true);
    }//GEN-LAST:event_checkForUpdatesItemActionPerformed

    private void saveProjectItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveProjectItemActionPerformed
        projectHandler.promptUserToSaveSession();
    }//GEN-LAST:event_saveProjectItemActionPerformed

    private void menuItem_viewtoolbarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuItem_viewtoolbarActionPerformed
        this.setToolBarVisibility(menuItem_viewtoolbar.isSelected());
    }//GEN-LAST:event_menuItem_viewtoolbarActionPerformed

    private void speedAndEfficiencyItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_speedAndEfficiencyItemActionPerformed
        setSpeedAndEfficiencyIndicatorsVisible(speedAndEfficiencyItem.isSelected());
    }//GEN-LAST:event_speedAndEfficiencyItemActionPerformed

    private void menuitem_aimActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuitem_aimActionPerformed
        GraphPaneController gpc = GraphPaneController.getInstance();
        gpc.setAiming(this.menuitem_aim.isSelected());
    }//GEN-LAST:event_menuitem_aimActionPerformed

    private void menuitem_startpageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuitem_startpageActionPerformed
        setStartPageVisible(this.menuitem_startpage.isSelected());
    }//GEN-LAST:event_menuitem_startpageActionPerformed

    private void loadFromDataSourcePluginActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadFromDataSourcePluginActionPerformed
        try {
            DataSource s = DataSourcePluginDialog.getDataSource(this);
            if (s != null) {
                Track t = TrackFactory.createTrack(s);
                createFrameForExistingTrack(new Track[] { t });
            }
        } catch (Exception x) {
            LOG.error("Unable to create track from DataSource plugin", x);
            DialogUtils.displayException("Track Creation Failed", "Unable to create track.", x);
        }
    }//GEN-LAST:event_loadFromDataSourcePluginActionPerformed

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        (new FeatureRequestDialog(this,false)).setVisible(true);
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
        (new BugReportDialog(this,false)).setVisible(true);
    }//GEN-LAST:event_jMenuItem2ActionPerformed

    /**
     * Starts an instance of the Savant Browser
     * @param args the command line arguments
     */
    public static void main(String args[]) {

        boolean loadProject = false;
        boolean loadPlugin = false;
        String loadProjectUrl = null;
        List<String> loadPluginUrls = new ArrayList<String>();
        for(int i = 0; i < args.length; i++){
            String s = args[i];
            if(s.startsWith("--")){ //build
                loadProject = false;
                loadPlugin = false;
                BrowserSettings.build = s.replaceAll("-", "");
                if (s.equals("--debug")) {
                    turnExperimentalFeaturesOff = false;
                }
            } else if (s.startsWith("-")){
                if(s.equals("-project")){
                    loadProject = true;
                    loadPlugin = false;
                } else if (s.equals("-plugins")){
                    loadPlugin = true;
                    loadProject = false;
                }
            } else if (loadProject){
                loadProjectUrl = s;
                loadProject = false;
            } else if (loadPlugin){
                loadPluginUrls.add(s);
            } else {
                //bad argument, skip
            }
        }

        installMissingPlugins(loadPluginUrls);

        //java.awt.EventQueue.invokeLater(new Runnable() {

        //@Override
        //public void run() {

        System.setProperty("apple.laf.useScreenMenuBar", "true");
        com.jidesoft.utils.Lm.verifyLicense("Marc Fiume", "Savant Genome Browser", "1BimsQGmP.vjmoMbfkPdyh0gs3bl3932");
        LookAndFeelFactory.installJideExtension(LookAndFeelFactory.OFFICE2007_STYLE);

        try {

            UIManager.put("JideSplitPaneDivider.border", 5);

            // Set System L&F
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

            LookAndFeelFactory.installJideExtension(LookAndFeelFactory.XERTO_STYLE_WITHOUT_MENU);
            // LookAndFeelFactory.installJideExtension(LookAndFeelFactory.OFFICE2003_STYLE);

        } catch (Exception e) {
            // handle exception
        }

        Savant instance = Savant.getInstance();

        //load project immediately if argument exists
        if (instance.isWebStart() && loadProjectUrl != null){
            instance.projectHandler.loadProjectFromUrl(loadProjectUrl);
        }

    }

    public static void loadPreferences() {
    }

    public static void checkVersion() {
        checkVersion(false);
    }

    private static String getPost(String name, String value) {
        return name + "=" + value;
    }

    private static void logUsageStats() {
        try {
            URL url;
            URLConnection urlConn;
            DataOutputStream printout;
            // URL of CGI-Bin script.
            url = new URL(BrowserSettings.LOG_USAGE_STATS_URL);
            // URL connection channel.
            urlConn = url.openConnection();
            // Let the run-time system (RTS) know that we want input.
            urlConn.setDoInput(true);
            // Let the RTS know that we want to do output.
            urlConn.setDoOutput(true);
            // No caching, we want the real thing.
            urlConn.setUseCaches(false);
            // Specify the content type.
            urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            // Send POST output.
            printout = new DataOutputStream(urlConn.getOutputStream());

            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Date date = new Date();
            Locale locale = Locale.getDefault();

            String content =
                    post("time", dateFormat.format(date))
                    + "&" + post("language", locale.getDisplayLanguage())
                    + "&" + post("user.timezone", System.getProperty("user.timezone"))
                    + "&" + post("savant.version", BrowserSettings.version)
                    + "&" + post("savant.build", BrowserSettings.build)
                    //+ "&" + post("address", InetAddress.getLocalHost().getHostAddress())
                    + "&" + post("java.version", System.getProperty("java.version"))
                    + "&" + post("java.vendor", System.getProperty("java.vendor"))
                    + "&" + post("os.name", System.getProperty("os.name"))
                    + "&" + post("os.arch", System.getProperty("os.arch"))
                    + "&" + post("os.version", System.getProperty("os.version"))
                    + "&" + post("user.region", System.getProperty("user.region")
                    );

            printout.writeBytes(content);
            printout.flush();
            printout.close();
            urlConn.getInputStream();
        } catch (Exception ex) {
           //LOG.error("Error logging usage stats.", ex);
        }
    }

    private static String post(String id, String msg) {
        return id + "=" + id + ":" + ((msg == null) ? "null" : URLEncoder.encode(msg));
    }

    public static void checkVersion(boolean verbose) {
        try {
            File versionFile = DownloadFile.downloadFile(new URL(BrowserSettings.VERSION_URL), DirectorySettings.getSavantDirectory());
            if (versionFile != null) {
                LOG.info("Saved version file to: " + versionFile.getAbsolutePath());
                Version currentversion = (new XMLVersion(versionFile)).getVersion();
                Version thisversion = new Version(BrowserSettings.version);
                if (currentversion.compareTo(thisversion) > 0) {
                    DialogUtils.displayMessage("Savant", "A new version of Savant (" + currentversion.toString() + ") is available.\n"
                            + "To stop this message from appearing, download the newest version at " + BrowserSettings.URL + "\nor disable automatic "
                            + "checking in Preferences.");
                } else {
                    if (verbose) {
                        DialogUtils.displayMessage("Savant", "This version of Savant (" + thisversion.toString() + ") is up to date.");
                    }
                }
            } else {
                if (verbose) {
                    DialogUtils.displayMessage("Savant Warning", "Could not connect to server. Please ensure you have connection to the internet and try again.");
                }
                LOG.error("Error downloading version file");
            }
            versionFile.delete();

        } catch (IOException ex) {
        } catch (JDOMException ex) {
        } catch (NullPointerException ex) {
        }

    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenu editMenu;
    private javax.swing.JMenuItem exitItem;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JMenuItem formatItem;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JPopupMenu.Separator jSeparator10;
    private javax.swing.JPopupMenu.Separator jSeparator4;
    private javax.swing.JPopupMenu.Separator jSeparator7;
    private javax.swing.JLabel label_memory;
    private javax.swing.JLabel label_mouseposition;
    private javax.swing.JLabel label_mouseposition_title;
    private javax.swing.JLabel label_status;
    private javax.swing.JMenuItem loadFromDataSourcePlugin;
    private javax.swing.JMenuItem loadFromFileItem;
    private javax.swing.JMenuItem loadFromURLItem;
    private javax.swing.JMenuItem loadGenomeItem;
    private javax.swing.JMenuBar menuBar_top;
    private javax.swing.JMenuItem menuItemAddToFaves;
    private javax.swing.JMenuItem menuItemPanLeft;
    private javax.swing.JMenuItem menuItemPanRight;
    private javax.swing.JMenuItem menuItemShiftEnd;
    private javax.swing.JMenuItem menuItemShiftStart;
    private javax.swing.JMenuItem menuItemZoomIn;
    private javax.swing.JMenuItem menuItemZoomOut;
    private javax.swing.JCheckBoxMenuItem menuItem_viewRangeControls;
    private javax.swing.JCheckBoxMenuItem menuItem_viewtoolbar;
    private javax.swing.JCheckBoxMenuItem menu_bookmarks;
    private javax.swing.JCheckBoxMenuItem menuitem_aim;
    private javax.swing.JMenuItem menuitem_deselectall;
    private javax.swing.JCheckBoxMenuItem menuitem_genomeview;
    private javax.swing.JMenuItem menuitem_pluginmanager;
    private javax.swing.JMenuItem menuitem_preferences;
    private javax.swing.JMenuItem menuitem_redo;
    private javax.swing.JCheckBoxMenuItem menuitem_ruler;
    private javax.swing.JCheckBoxMenuItem menuitem_startpage;
    private javax.swing.JCheckBoxMenuItem menuitem_statusbar;
    private javax.swing.JCheckBoxMenuItem menuitem_tools;
    private javax.swing.JMenuItem menuitem_undo;
    private javax.swing.JCheckBoxMenuItem menuitem_view_plumbline;
    private javax.swing.JCheckBoxMenuItem menuitem_view_spotlight;
    private javax.swing.JMenuItem openProjectItem;
    private javax.swing.JPanel panelExtendedMiddle;
    private javax.swing.JPanel panel_main;
    private javax.swing.JPanel panel_toolbar;
    private javax.swing.JPanel panel_top;
    private javax.swing.JMenu pluginsMenu;
    private javax.swing.JMenu recentProjectMenu;
    private javax.swing.JMenu recentTrackMenu;
    private javax.swing.JToolBar.Separator s_e_sep;
    private javax.swing.JMenuItem saveProjectAsItem;
    private javax.swing.JMenuItem saveProjectItem;
    private javax.swing.JCheckBoxMenuItem speedAndEfficiencyItem;
    private javax.swing.JToolBar toolbar_bottom;
    private javax.swing.JMenuItem tutorialsItem;
    private javax.swing.JMenuItem userManualItem;
    private javax.swing.JMenu viewMenu;
    private javax.swing.ButtonGroup view_buttongroup;
    private javax.swing.JMenuItem websiteItem;
    private javax.swing.JMenu windowMenu;
    // End of variables declaration//GEN-END:variables

    /**
     * Customize the UI.  This includes doing any platform-specific customization.
     */
    private void customizeUI() {
        if (MiscUtils.MAC) {
            try {
                macOSXApplication = Application.getApplication();
                macOSXApplication.setAboutHandler(new AboutHandler() {

                    @Override
                    public void handleAbout(AppEvent.AboutEvent evt) {
                        final Splash dlg = new Splash(instance, true);
                        dlg.addMouseListener(new MouseAdapter() {

                            @Override
                            public void mouseClicked(MouseEvent e) {
                                dlg.setVisible(false);
                            }
                        });
                        dlg.setVisible(true);
                    }
                });
                macOSXApplication.setPreferencesHandler(new PreferencesHandler() {

                    @Override
                    public void handlePreferences(AppEvent.PreferencesEvent evt) {
                        menuitem_preferencesActionPerformed(null);
                    }
                });
                macOSXApplication.setQuitHandler(new QuitHandler() {

                    @Override
                    public void handleQuitRequestWith(AppEvent.QuitEvent evt, QuitResponse resp) {
                        exitItemActionPerformed(null);
                        // If the user agreed to quit, System.exit would have been
                        // called.  Since we got here, the user has said "No" to quitting.
                        resp.cancelQuit();
                    }
                });
                fileMenu.remove(jSeparator4);
                fileMenu.remove(exitItem);
                editMenu.remove(jSeparator7);
                editMenu.remove(menuitem_preferences);
            } catch (Throwable x) {
                LOG.error("Unable to load Apple eAWT classes.", x);
                DialogUtils.displayError("Warning", "Savant requires Java for Mac OS X 10.6 Update 3 (or later).\nPlease check Software Update for the latest version.");
            }
        }
        LookAndFeelFactory.UIDefaultsCustomizer uiDefaultsCustomizer = new LookAndFeelFactory.UIDefaultsCustomizer() {

            @Override
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
    private void init() {
        ReferenceController.getInstance().addReferenceChangedListener(this);

        initGUIFrame();
        initDocking();
        //initToolsPanel();
        initMenu();
        initStatusBar();
        initGUIHandlers();
        initBookmarksPanel();
        initDataSources();
        initStartPage();

        dff = new DataFormatForm(this, false);

        //urlDialog = new OpenURLDialog(this, true);
    }

    private void disableExperimentalFeatures() {
        menuitem_tools.setVisible(false);
    }

    public DockingManager getAuxDockingManager() {
        return auxDockingManager;
    }

    public DockingManager getTrackDockingManager() {
        return trackDockingManager;
    }

    private void initToolsPanel() {

        String frameTitle = "Tools";
        DockableFrame df = DockableFrameFactory.createFrame(frameTitle, DockContext.STATE_HIDDEN, DockContext.DOCK_SIDE_SOUTH);
        df.setAvailableButtons(DockableFrame.BUTTON_AUTOHIDE | DockableFrame.BUTTON_FLOATING | DockableFrame.BUTTON_MAXIMIZE);
        this.getAuxDockingManager().addFrame(df);
        MiscUtils.setFrameVisibility(frameTitle, false, this.getAuxDockingManager());

        JPanel canvas = new JPanel();

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
        if (dmg == null) {
            dmg = new DockingManagerGroup();
        }
        dmg.add(m);
    }

    private void initBookmarksPanel() {

        DockableFrame df = DockableFrameFactory.createFrame("Bookmarks", DockContext.STATE_HIDDEN, DockContext.DOCK_SIDE_EAST);
        df.setAvailableButtons(DockableFrame.BUTTON_AUTOHIDE | DockableFrame.BUTTON_FLOATING | DockableFrame.BUTTON_MAXIMIZE);
        this.getAuxDockingManager().addFrame(df);
        MiscUtils.setFrameVisibility("Bookmarks", false, this.getAuxDockingManager());

        df.getContentPane().setLayout(new BorderLayout());

        //JPanel tablePanel = createTabPanel(jtp, "Bookmarks");
        favoriteSheet = new BookmarkSheet(this, df.getContentPane());
        favoriteController.addFavoritesChangedListener(favoriteSheet);
        favoriteController.addFavoritesChangedListener(this);
    }

    /**
     * Provide access to the tabbed pane in the bottom auxiliary panel
     * @return the auxiliary tabbed pane
     */
    public JTabbedPane getAuxTabbedPane() {
        return auxTabbedPane;
    }

    private void askToDispose() {

        if (!projectHandler.isProjectSaved()) {

            int answer = DialogUtils.askYesNoCancel("Save project before quitting?");

            if (answer == JOptionPane.CANCEL_OPTION) {
                return;
            }
            if (answer == JOptionPane.YES_OPTION) {
                if (!projectHandler.promptUserToSaveSession()) {
                    return;
                }
            }
        }

        //cleanUpBeforeExit();
        System.exit(0);
    }

    /**
     * Set up frame
     */
    void initGUIFrame() {
        // ask before quitting

        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                askToDispose();

            }
        });

        // other
        this.setIconImage(
                SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.LOGO).getImage());
        this.setTitle("Savant Genome Browser");
        this.setName("Savant Genome Browser");
    }

    private void initMenu() {
        loadGenomeItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_G, osSpecificModifier));
        loadFromFileItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_T, osSpecificModifier));
        loadFromURLItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_U, osSpecificModifier));
        loadFromDataSourcePlugin.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_E, osSpecificModifier));
        openProjectItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, osSpecificModifier));
        saveProjectItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, osSpecificModifier));
        saveProjectAsItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, osSpecificModifier | java.awt.event.InputEvent.SHIFT_MASK));
        formatItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, osSpecificModifier));
        exitItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Q, osSpecificModifier));
        menuitem_undo.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, osSpecificModifier));
        menuitem_redo.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Y, osSpecificModifier));
        menuItemAddToFaves.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_B, osSpecificModifier));
        menuItem_viewRangeControls.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.event.InputEvent.SHIFT_MASK | osSpecificModifier));
        menuItemPanLeft.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_LEFT, java.awt.event.InputEvent.SHIFT_MASK));
        menuItemPanRight.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_RIGHT, java.awt.event.InputEvent.SHIFT_MASK));
        menuItemZoomIn.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_UP, java.awt.event.InputEvent.SHIFT_MASK));
        menuItemZoomOut.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DOWN, java.awt.event.InputEvent.SHIFT_MASK));
        menuItemShiftStart.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_HOME, java.awt.event.InputEvent.SHIFT_MASK));
        menuItemShiftEnd.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_END, java.awt.event.InputEvent.SHIFT_MASK));
        menuitem_preferences.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, osSpecificModifier));
        menuitem_aim.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_J, osSpecificModifier));
        menuitem_view_plumbline.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_K, osSpecificModifier));
        menuitem_view_spotlight.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, osSpecificModifier));
        menu_bookmarks.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_B, osSpecificModifier | java.awt.event.InputEvent.SHIFT_MASK));
        menuitem_genomeview.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, osSpecificModifier | java.awt.event.InputEvent.SHIFT_MASK));
        menuitem_ruler.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, osSpecificModifier | java.awt.event.InputEvent.SHIFT_MASK));
        menuitem_statusbar.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, osSpecificModifier | java.awt.event.InputEvent.SHIFT_MASK));
        menuItem_viewtoolbar.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_T, osSpecificModifier | java.awt.event.InputEvent.SHIFT_MASK));

        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            tutorialsItem.setEnabled(false);
            userManualItem.setEnabled(false);
            websiteItem.setEnabled(false);
        }
        initBrowseMenu();
        try {
            RecentTracksController.getInstance().populateMenu(recentTrackMenu);
            RecentProjectsController.getInstance().populateMenu(recentProjectMenu);
        } catch (IOException ex) {
            LOG.error("Unable to populate Recent Items menu.", ex);
        }

    }

    private void initBrowseMenu() {
        navigationBar = new NavigationBar();
        panelExtendedMiddle.setLayout(new BorderLayout());
        panelExtendedMiddle.add(navigationBar);
        navigationBar.setVisible(false);
        panel_top.setVisible(false);
    }

    /**
     * Prompt the user to open a genome file.
     *
     * Expects a Binary Fasta file (created using the data formatter included in
     * the distribution).
     */
    public void showOpenGenomeDialog() {
        LoadGenomeDialog d = new LoadGenomeDialog(this,true);
        d.setVisible(true);
    }

    /**
     * The user has tried to open an unformatted file.  Prompt them to format it.
     *
     * @param uri the file URI which the user has tried to open.
     */
    public void promptUserToFormatFile(URI uri) {
        String title = "Unrecognized file: " + uri;
        String scheme = uri.getScheme();
        if (scheme == null || scheme.equals("file")) {
            int reply = JOptionPane.showConfirmDialog(this, "This file does not appear to be formatted. Format now?", title, JOptionPane.YES_NO_OPTION);
            if (reply == JOptionPane.YES_OPTION) {
                if (!dff.isVisible()) {
                    dff.clear();
                    dff.setInFile(new File(uri));
                    dff.setLocationRelativeTo(this);
                    dff.setVisible(true);
                }
            }
        } else {
            // TODO: Do something intelligent for network URIs.
            JOptionPane.showMessageDialog(this, "This file does not appear to be formatted. Please try to format it.");
        }
    }

    private void showBrowserControls() {
        if (browserControlsShown) {
            return;
        }

        //TODO: remove start page
        //trackBackground.removeAll();

        this.panel_top.setVisible(true);
        this.menuItem_viewRangeControls.setSelected(true);

        this.rangeSelector.setVisible(true);
        this.referenceCombo.setVisible(true);
        this.menuitem_genomeview.setSelected(true);

        this.ruler.setVisible(true);
        this.menuitem_ruler.setSelected(true);

        this.loadFromFileItem.setEnabled(true);
        this.loadFromURLItem.setEnabled(true);
        this.loadFromDataSourcePlugin.setEnabled(true);

        //setStartPageVisible(false);
        navigationBar.setVisible(true);
        browserControlsShown = true;
    }

    private void setStartPageVisible(boolean b) {
        MiscUtils.setFrameVisibility("Start Page", b, this.getTrackDockingManager());
        this.menuitem_startpage.setSelected(b);
    }

    public void updateStatus(String msg) {
        this.label_status.setText(msg);
        this.label_status.revalidate();
    }

    private void initStatusBar() {
        toolbar_bottom.add(Box.createGlue(), 2);
        memorystatusbar = new MemoryStatusBarItem();
        memorystatusbar.setMaximumSize(new Dimension(100, 30));
        memorystatusbar.setFillColor(Color.lightGray);
        this.toolbar_bottom.add(memorystatusbar);

        setSpeedAndEfficiencyIndicatorsVisible(false);
    }

    @Override
    public void bookmarksChanged(BookmarksChangedEvent event) {
        if (!showBookmarksChangedDialog) {
            return;
        }
        //Custom button text
        Object[] options = {"OK",
            "Don't show again"};
        int n = JOptionPane.showOptionDialog(Savant.getInstance(),
                event.isAdded() ? "Bookmark added at " + event.getBookmark().getReference() + ":" + event.getBookmark().getRange() : "Bookmark removed at " + event.getBookmark().getRange(),
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

    private void addTrackFrame(savant.view.swing.Frame frame) {

        if (startpage != null && startpage.isVisible()) {
            startpage.setVisible(false);
        }

        // remove bogus "#Workspace" frame
        List<FrameHandle> simpleFrames = getCleanedOrderedFrames(trackDockingManager);

        // the number of frames, currently
        int numframes = simpleFrames.size();

        trackDockingManager.addFrame(frame);

        // move the frame to the bottom of the stack
        if (numframes != 0) {
            FrameHandle lastFrame = simpleFrames.get(0);
            trackDockingManager.moveFrame(frame.getKey(), lastFrame.getKey(),DockContext.DOCK_SIDE_SOUTH);
        }
    }

    private List<FrameHandle> getCleanedOrderedFrames(DockingManager dm) {
        List<FrameHandle> cleanFrames = new ArrayList<FrameHandle>();
        for (FrameHandle h : dm.getOrderedFrames()) {
            if (!h.getKey().startsWith("#")) {
                cleanFrames.add(h);
            }
        }
        return cleanFrames;
    }

    @Override
    public void genomeChanged(GenomeChangedEvent event) {
        LOG.info("Genome changed from " + event.getOldGenome() + " to " + event.getNewGenome());
        if (event.getOldGenome() != null) {
            Track[] oldTracks = event.getOldGenome().getTracks();
            for (Track t: oldTracks) {
                // FIXME:  Should never need to deal with DockableFrameController.  TrackController.removeTrack() or FrameController.removeFrame() should be sufficient.
                DockableFrameController.getInstance().closeDockableFrame(t.getFrame(), false);
            }
        }
        loadGenomeItem.setText("Change genome...");
        showBrowserControls();
    }

    @Override
    public void referenceChanged(ReferenceChangedEvent event) {
        Genome loadedGenome = ReferenceController.getInstance().getGenome();
        rangeController.setMaxRange(new Range(1, loadedGenome.getLength()));
        rangeSelector.setMaximum(loadedGenome.getLength());
        rangeController.setRange(1, Math.min(1000, loadedGenome.getLength()));
    }

    private void setSpeedAndEfficiencyIndicatorsVisible(boolean b) {
        this.speedAndEfficiencyItem.setSelected(b);
        this.jLabel1.setVisible(b);
        this.label_memory.setVisible(b);
        this.label_status.setVisible(b);
        this.s_e_sep.setVisible(b);
        this.memorystatusbar.setVisible(b);
    }

    public void addPluginToMenu(JCheckBoxMenuItem cb) {
        pluginsMenu.add(cb);
    }

    /*
    private void cleanUpBeforeExit() {
        removeTmpFiles();
    }
     */
    
    public void removeTmpFiles() {
        for (File f : DirectorySettings.getTmpDirectory().listFiles()) {
            f.delete();
        }
    }
    

    private void initGUIHandlers() {
        this.projectHandler = ProjectHandler.getInstance();
    }

    private void makeGUIVisible() {
        setExtendedState(MAXIMIZED_BOTH);
        setVisible(true);
    }

    private void initDataSources() {
        DataSourcePluginController.getInstance().addDataSourcePlugin(new SavantFileRepositoryDataSourcePlugin());
        
        if (!turnExperimentalFeaturesOff) {
            DataSourcePluginController.getInstance().addDataSourcePlugin(new SAFEDataSourcePlugin());
        }
    }

    public void updateMousePosition() {
        GraphPaneController gpc = GraphPaneController.getInstance();
        int x = gpc.getMouseXPosition();
        int y = gpc.getMouseYPosition();
        if (x == -1 && y == -1) {
            this.label_mouseposition.setText("mouse not over track");
        } else {
            this.label_mouseposition.setText(((x == -1) ? "" : "X: " + MiscUtils.numToString(x)) + ((y == -1) ? "" : " Y: " + MiscUtils.numToString(y)));
        }
    }

    public String[] getSelectedTracks(boolean multiple, String title) {
        TrackChooser tc = new TrackChooser(Savant.getInstance(), multiple, title);
        tc.setVisible(true);
        String[] tracks = tc.getSelectedTracks();
        return tracks;
    }

    private void displayAuxPanels() {

        List<String> names = this.getAuxDockingManager().getAllFrameNames();
        for (int i = 0; i < names.size(); i++) {
            MiscUtils.setFrameVisibility(names.get(i), true, this.getAuxDockingManager());
            this.getAuxDockingManager().toggleAutohideState(names.get(i));
            break;
        }

        //MiscUtils.setFrameVisibility("Bookmarks", true, this.getAuxDockingManager());
        //this.getAuxDockingManager().toggleAutohideState("Bookmarks");
        menu_bookmarks.setState(true);

        this.getAuxDockingManager().setActive(false);
    }

    public SelectionController getSelectionController() {
        return this.selectionController;
    }

    private void initStartPage() {

        if (BrowserSettings.getShowStartPage()) {

/*            DockableFrame df = DockableFrameFactory.createFrame("Start Page", DockContext.STATE_FRAMEDOCKED, DockContext.DOCK_SIDE_NORTH);
            //df.setAvailableButtons(DockableFrame.BUTTON_CLOSE);
            df.setShowTitleBar(false);
            JPanel canvas = (JPanel) df.getContentPane();
            canvas.setLayout(new BorderLayout());
            canvas.add(new StartPanel(), BorderLayout.CENTER);

            trackDockingManager.addFrame(df);
            MiscUtils.setFrameVisibility("Start Page", true,trackDockingManager);
        try {
            df.setMaximized(true);
        } catch (PropertyVetoException ex) {
            Logger.getLogger(Savant.class.getName()).log(Level.SEVERE, null, ex);
        }

            //df.getContentPane().setLayout(new BorderLayout());

            //
            //df.getContentPane().add(start,BorderLayout.CENTER);

            startPageDockableFrame = df;
 *
 */
            startpage = new StartPanel();
            trackBackground.add(startpage, BorderLayout.CENTER);
        }
    }

    public boolean isWebStart(){
        return webStart;
    }

    public static void installMissingPlugins(List<String> pluginUrls) {
        String localFile = null;
        for (String stringUrl: pluginUrls) {

            try{
                URL url  = new URL(stringUrl);
                InputStream is = url.openStream();
                FileOutputStream fos=null;

                StringTokenizer st=new StringTokenizer(url.getFile(), "/");
                while (st.hasMoreTokens()){
                    localFile=st.nextToken();
                }

                String dir;
                if(localFile.endsWith("SavantCore.jar")){
                    File dirFile = DirectorySettings.getLibsDirectory();
                    if(!dirFile.exists()) dirFile.mkdirs();
                    dir = dirFile.getPath();
                } else {
                    dir = DirectorySettings.getPluginsDirectory().getPath();
                }

                localFile = dir + System.getProperty("file.separator") + localFile;

                fos = new FileOutputStream(localFile);

                int oneChar, count=0;
                while ((oneChar=is.read()) != -1)
                {
                    fos.write(oneChar);
                    count++;
                }
                is.close();
                fos.close();

            } catch (Exception e){
                LOG.error(e);
            }
        }
    }
}
