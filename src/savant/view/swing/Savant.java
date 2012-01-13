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

import savant.view.swing.variation.VariationSheet;
import java.awt.*;
import java.awt.event.*;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
import com.jidesoft.plaf.LookAndFeelFactory;
import com.jidesoft.plaf.UIDefaultsLookup;
import com.jidesoft.plaf.basic.ThemePainter;
import com.jidesoft.status.MemoryStatusBarItem;
import com.jidesoft.swing.JideSplitPane;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.adapter.DataSourceAdapter;
import savant.api.event.BookmarksChangedEvent;
import savant.api.event.GenomeChangedEvent;
import savant.api.event.PluginEvent;
import savant.api.util.DialogUtils;
import savant.api.util.Listener;
import savant.controller.*;
import savant.controller.event.*;
import savant.plugin.PluginController;
import savant.plugin.SavantDataSourcePlugin;
import savant.plugin.SavantPanelPlugin;
import savant.plugin.SavantPlugin;
import savant.plugin.builtin.SAFEDataSourcePlugin;
import savant.plugin.builtin.SavantFileRepositoryDataSourcePlugin;
import savant.selection.SelectionController;
import savant.settings.*;
import savant.util.ColourKey;
import savant.util.MiscUtils;
import savant.util.Version;
import savant.util.swing.TrackChooser;
import savant.view.dialog.*;
import savant.view.icon.SavantIconFactory;
import savant.view.swing.start.StartPanel;
import savant.view.tracks.Track;
import savant.view.tracks.TrackFactory;


/**
 * Main application Window (Frame).
 *
 * @author mfiume
 */
public class Savant extends JFrame {
    private static final Log LOG = LogFactory.getLog(Savant.class);
    public static boolean turnExperimentalFeaturesOff = true;
    private static boolean isDebugging = false;
    private DockingManager auxDockingManager;
    private JPanel trackBackground;
    private DockingManager trackDockingManager;
    private NavigationBar navigationBar;
    static boolean showNonGenomicReferenceDialog = true;
    private static boolean showBookmarksChangedDialog = false; // turned off, its kind of annoying
    private MemoryStatusBarItem memorystatusbar;
    private Application macOSXApplication;
    private boolean browserControlsShown = false;

    //web start
    static BasicService basicService = null;
    static boolean webStart = false;
    private StartPanel startpage;

    /** == [[ DOCKING ]] ==
     *  Components (such as frames, the Task Pane, etc.)
     *  can be docked to regions of the UI
     */
    private void initDocking() {

        JPanel masterPlaceholderPanel = new JPanel();
        masterPlaceholderPanel.setLayout(new BorderLayout());

        panel_main.setLayout(new BorderLayout());
        panel_main.add(masterPlaceholderPanel, BorderLayout.CENTER);

        auxDockingManager = new DefaultDockingManager(this, masterPlaceholderPanel);
        masterPlaceholderPanel.setBackground(ColourSettings.getColor(ColourKey.SPLITTER));
        auxDockingManager.setSidebarRollover(false);
        auxDockingManager.getWorkspace().setBackground(ColourSettings.getColor(ColourKey.SPLITTER));
        auxDockingManager.setInitSplitPriority(DockingManager.SPLIT_EAST_SOUTH_WEST_NORTH);
        //auxDockingManager.loadLayoutData();
        //auxDockingManager.setAutohidable(false);

        JPanel trackPanel = new JPanel();
        trackPanel.setLayout(new BorderLayout());

        auxDockingManager.getWorkspace().add(trackPanel, BorderLayout.CENTER);

        trackDockingManager = new DefaultDockingManager(this, trackPanel);
        trackPanel.setBackground(ColourSettings.getColor(ColourKey.SPLITTER));
        trackDockingManager.getWorkspace().setBackground(ColourSettings.getColor(ColourKey.SPLITTER));
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
        locationController.addListener(rangeSelector);
        rangeSelector.setVisible(false);

        ruler = new Ruler();
        ruler.setPreferredSize(new Dimension(10000, 23));
        ruler.setMaximumSize(new Dimension(10000, 23));
        ruler.setVisible(false);

        Box box2 = Box.createVerticalBox();
        box2.add(rangeSelector);
        box2.add(ruler);

        trackPanel.add(box2, BorderLayout.NORTH);

        trackBackground = new JPanel();
        trackBackground.setBackground(Color.darkGray);

        trackBackground.setLayout(new BorderLayout());

        trackDockingManager.getWorkspace().add(trackBackground);
        trackDockingManager.setAllowedDockSides(DockContext.DOCK_SIDE_HORIZONTAL);
    }
    /** Minimum and maximum dimensions of the browser form */
    static int minimumFormWidth = 500;
    static int minimumFormHeight = 500;
    /** The loaded genome */
    //private Genome loadedGenome;
    /** The log */
    private static JTextArea log;
    /** Click and drag control for range selection */
    private RangeSelectionPanel rangeSelector;
    private Ruler ruler;

    /**
     * Info
     */
    private static BookmarkSheet favoriteSheet;
    private LocationController locationController = LocationController.getInstance();
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

        GraphPaneController.getInstance().addListener(new Listener<GraphPaneEvent>() {
            @Override
            public void handleEvent(GraphPaneEvent event) {
                GraphPaneController controller = GraphPaneController.getInstance();
                switch (event.getType()) {
                    case HIGHLIGHTING:
                        plumblineItem.setSelected(controller.isPlumbing());
                        spotlightItem.setSelected(controller.isSpotlight());
                        crosshairItem.setSelected(controller.isAiming());
                        break;
                    case MOUSE:
                        updateMousePosition(event.getMouseX(), event.getMouseY(), event.isYIntegral());
                        break;
                    case STATUS:
                        updateStatus(event.getStatus());
                        break;
                }
            }
        });

        ProjectController.getInstance().addListener(new Listener<ProjectEvent>() {
            @Override
            public void handleEvent(ProjectEvent event) {
                String activity;
                switch (event.getType()) {
                    case LOADING:
                        activity = "Loading " + event.getPath() + "...";
                        setTitle("Savant Genome Browser - " + activity);
                        break;
                    case LOADED:
                    case SAVED:
                        MiscUtils.setUnsavedTitle(Savant.this, "Savant Genome Browser - " + event.getPath(), false);
                        break;
                    case SAVING:
                        activity = "Saving " + event.getPath() + "...";
                        setTitle("Savant Genome Browser - " + activity);
                        break;
                    case UNSAVED:
                        MiscUtils.setUnsavedTitle(Savant.this, "Savant Genome Browser - " + event.getPath(), true);
                        break;
                }
            }
        });

        GenomeController.getInstance().addListener(new Listener<GenomeChangedEvent>() {
            @Override
            public void handleEvent(GenomeChangedEvent event) {
                LOG.info("Genome changed from " + event.getOldGenome() + " to " + event.getNewGenome());
                loadGenomeItem.setText("Change genome...");
                showBrowserControls();
            }
        });

        s.setStatus("Initializing GUI");

        initComponents();       
        customizeUI();
        init();
        initHiddenShortcuts();

        if (BrowserSettings.getCheckVersionOnStartup()) {
            s.setStatus("Checking version");
            checkVersion(false);
        }

        if (BrowserSettings.getCollectAnonymousUsage()) {
            logUsageStats();
        }

        s.setStatus("Loading plugins");
        PluginController pluginController = PluginController.getInstance();
        pluginController.addListener(new Listener<PluginEvent>() {
            @Override
            public void handleEvent(PluginEvent event) {
                SavantPlugin plugin = event.getPlugin();
                if (event.getType() == PluginEvent.Type.LOADED) {
                    if (plugin instanceof SavantPanelPlugin) {
                        DockableFrame f = DockableFrameFactory.createGUIPluginFrame(plugin.getTitle());
                        JPanel p = (JPanel)f.getContentPane();
                        p.setLayout(new BorderLayout());
                        p.add(event.getCanvas(), BorderLayout.CENTER);
                        auxDockingManager.addFrame(f);
                        addPluginToMenu(new PluginMenuItem((SavantPanelPlugin)plugin));
                    } else if (event.getPlugin() instanceof SavantDataSourcePlugin) {
                        loadFromDataSourcePluginItem.setText("Load Track from Other Datasource...");
                    }
                }
            }
        });
        pluginController.loadPlugins(DirectorySettings.getPluginsDirectory());

        s.setStatus("Organizing layout");

        displayBookmarksPanel();

        if (turnExperimentalFeaturesOff) {
            disableExperimentalFeatures();
        }

        s.setVisible(false);

        makeGUIVisible();
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
        mousePositionLabel = new javax.swing.JLabel();
        timeCaption = new javax.swing.JLabel();
        label_status = new javax.swing.JLabel();
        s_e_sep = new javax.swing.JToolBar.Separator();
        label_memory = new javax.swing.JLabel();
        pluginToolbar = new javax.swing.JPanel();
        menuBar_top = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        loadGenomeItem = new javax.swing.JMenuItem();
        loadFromFileItem = new javax.swing.JMenuItem();
        loadFromURLItem = new javax.swing.JMenuItem();
        loadFromDataSourcePluginItem = new javax.swing.JMenuItem();
        recentTrackMenu = new javax.swing.JMenu();
        javax.swing.JPopupMenu.Separator jSeparator1 = new javax.swing.JPopupMenu.Separator();
        openProjectItem = new javax.swing.JMenuItem();
        recentProjectMenu = new javax.swing.JMenu();
        saveProjectItem = new javax.swing.JMenuItem();
        saveProjectAsItem = new javax.swing.JMenuItem();
        javax.swing.JPopupMenu.Separator jSeparator2 = new javax.swing.JPopupMenu.Separator();
        formatItem = new javax.swing.JMenuItem();
        javax.swing.JPopupMenu.Separator jSeparator3 = new javax.swing.JPopupMenu.Separator();
        exportItem = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JPopupMenu.Separator();
        exitItem = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        undoItem = new javax.swing.JMenuItem();
        redoItem = new javax.swing.JMenuItem();
        javax.swing.JPopupMenu.Separator jSeparator6 = new javax.swing.JPopupMenu.Separator();
        bookmarkItem = new javax.swing.JMenuItem();
        deselectAllItem = new javax.swing.JMenuItem();
        jSeparator7 = new javax.swing.JPopupMenu.Separator();
        preferencesItem = new javax.swing.JMenuItem();
        viewMenu = new javax.swing.JMenu();
        panLeftItem = new javax.swing.JMenuItem();
        panRightItem = new javax.swing.JMenuItem();
        zoomInItem = new javax.swing.JMenuItem();
        zoomOutItem = new javax.swing.JMenuItem();
        toStartItem = new javax.swing.JMenuItem();
        toEndItem = new javax.swing.JMenuItem();
        javax.swing.JSeparator jSeparator8 = new javax.swing.JSeparator();
        crosshairItem = new javax.swing.JCheckBoxMenuItem();
        plumblineItem = new javax.swing.JCheckBoxMenuItem();
        spotlightItem = new javax.swing.JCheckBoxMenuItem();
        windowMenu = new javax.swing.JMenu();
        navigationItem = new javax.swing.JCheckBoxMenuItem();
        genomeItem = new javax.swing.JCheckBoxMenuItem();
        rulerItem = new javax.swing.JCheckBoxMenuItem();
        pluginToolbarItem = new javax.swing.JCheckBoxMenuItem();
        statusBarItem = new javax.swing.JCheckBoxMenuItem();
        speedAndEfficiencyItem = new javax.swing.JCheckBoxMenuItem();
        javax.swing.JSeparator jSeparator9 = new javax.swing.JSeparator();
        startPageItem = new javax.swing.JCheckBoxMenuItem();
        bookmarksItem = new javax.swing.JCheckBoxMenuItem();
        pluginsMenu = new javax.swing.JMenu();
        menuitem_pluginmanager = new javax.swing.JMenuItem();
        jSeparator10 = new javax.swing.JPopupMenu.Separator();
        helpMenu = new javax.swing.JMenu();
        userManualItem = new javax.swing.JMenuItem();
        tutorialsItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem checkForUpdatesItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem bugReportItem = new javax.swing.JMenuItem();
        javax.swing.JMenuItem featureRequestItem = new javax.swing.JMenuItem();
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
            .addGap(0, 1027, Short.MAX_VALUE)
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
            .addGap(0, 1027, Short.MAX_VALUE)
        );
        panel_mainLayout.setVerticalGroup(
            panel_mainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 526, Short.MAX_VALUE)
        );

        toolbar_bottom.setFloatable(false);
        toolbar_bottom.setAlignmentX(1.0F);

        label_mouseposition_title.setText(" Position: ");
        toolbar_bottom.add(label_mouseposition_title);
        toolbar_bottom.add(mousePositionLabel);

        timeCaption.setText("Time: ");
        toolbar_bottom.add(timeCaption);

        label_status.setMaximumSize(new java.awt.Dimension(300, 14));
        label_status.setMinimumSize(new java.awt.Dimension(100, 14));
        label_status.setPreferredSize(new java.awt.Dimension(100, 14));
        toolbar_bottom.add(label_status);
        toolbar_bottom.add(s_e_sep);

        label_memory.setText(" Memory: ");
        toolbar_bottom.add(label_memory);

        pluginToolbar.setVisible(false);
        pluginToolbar.setPreferredSize(new java.awt.Dimension(856, 24));
        pluginToolbar.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEADING));

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

        loadFromDataSourcePluginItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_E, java.awt.event.InputEvent.CTRL_MASK));
        loadFromDataSourcePluginItem.setText("Load Track from Repository...");
        loadFromDataSourcePluginItem.setEnabled(false);
        loadFromDataSourcePluginItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadFromDataSourcePluginItemActionPerformed(evt);
            }
        });
        fileMenu.add(loadFromDataSourcePluginItem);

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

        formatItem.setText("Format File...");
        formatItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                formatItemActionPerformed(evt);
            }
        });
        fileMenu.add(formatItem);
        fileMenu.add(jSeparator3);

        exportItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_I, java.awt.event.InputEvent.CTRL_MASK));
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

        undoItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, java.awt.event.InputEvent.CTRL_MASK));
        undoItem.setText("Undo Range Change");
        undoItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                undoItemActionPerformed(evt);
            }
        });
        editMenu.add(undoItem);

        redoItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Y, java.awt.event.InputEvent.CTRL_MASK));
        redoItem.setText("Redo Range Change");
        redoItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                redoItemActionPerformed(evt);
            }
        });
        editMenu.add(redoItem);
        editMenu.add(jSeparator6);

        bookmarkItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_D, java.awt.event.InputEvent.CTRL_MASK));
        bookmarkItem.setText("Bookmark");
        bookmarkItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bookmarkItemActionPerformed(evt);
            }
        });
        editMenu.add(bookmarkItem);

        deselectAllItem.setText("Deselect All");
        deselectAllItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuitem_deselectActionPerformed(evt);
            }
        });
        editMenu.add(deselectAllItem);
        editMenu.add(jSeparator7);

        preferencesItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, java.awt.event.InputEvent.CTRL_MASK));
        preferencesItem.setText("Preferences");
        preferencesItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                preferencesItemActionPerformed(evt);
            }
        });
        editMenu.add(preferencesItem);

        menuBar_top.add(editMenu);

        viewMenu.setText("View");

        panLeftItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_LEFT, java.awt.event.InputEvent.SHIFT_MASK));
        panLeftItem.setText("Pan Left");
        panLeftItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                panLeftItemActionPerformed(evt);
            }
        });
        viewMenu.add(panLeftItem);

        panRightItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_RIGHT, java.awt.event.InputEvent.SHIFT_MASK));
        panRightItem.setText("Pan Right");
        panRightItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                panRightItemActionPerformed(evt);
            }
        });
        viewMenu.add(panRightItem);

        zoomInItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_UP, java.awt.event.InputEvent.SHIFT_MASK));
        zoomInItem.setText("Zoom In");
        zoomInItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zoomInItemActionPerformed(evt);
            }
        });
        viewMenu.add(zoomInItem);

        zoomOutItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DOWN, java.awt.event.InputEvent.SHIFT_MASK));
        zoomOutItem.setText("Zoom Out");
        zoomOutItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                zoomOutItemActionPerformed(evt);
            }
        });
        viewMenu.add(zoomOutItem);

        toStartItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_HOME, 0));
        toStartItem.setText("Shift to Start");
        toStartItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                toStartItemActionPerformed(evt);
            }
        });
        viewMenu.add(toStartItem);

        toEndItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_END, 0));
        toEndItem.setText("Shift to End");
        toEndItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                toEndItemActionPerformed(evt);
            }
        });
        viewMenu.add(toEndItem);
        viewMenu.add(jSeparator8);

        crosshairItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_J, java.awt.event.InputEvent.CTRL_MASK));
        crosshairItem.setText("Crosshair");
        crosshairItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                crosshairItemActionPerformed(evt);
            }
        });
        viewMenu.add(crosshairItem);

        plumblineItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_K, java.awt.event.InputEvent.CTRL_MASK));
        plumblineItem.setText("Plumbline");
        plumblineItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                plumblineItemActionPerformed(evt);
            }
        });
        viewMenu.add(plumblineItem);

        spotlightItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, java.awt.event.InputEvent.CTRL_MASK));
        spotlightItem.setText("Spotlight");
        spotlightItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                spotlightItemActionPerformed(evt);
            }
        });
        viewMenu.add(spotlightItem);

        menuBar_top.add(viewMenu);

        windowMenu.setText("Window");
        windowMenu.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                windowMenuStateChanged(evt);
            }
        });

        navigationItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        navigationItem.setText("Navigation");
        navigationItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                navigationItemMousePressed(evt);
            }
        });
        windowMenu.add(navigationItem);

        genomeItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        genomeItem.setText("Genome");
        genomeItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                genomeItemActionPerformed(evt);
            }
        });
        windowMenu.add(genomeItem);

        rulerItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        rulerItem.setText("Ruler");
        rulerItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rulerItemActionPerformed(evt);
            }
        });
        windowMenu.add(rulerItem);

        pluginToolbarItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_T, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        pluginToolbarItem.setText("Plugin Toolbar");
        pluginToolbarItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pluginToolbarItemActionPerformed(evt);
            }
        });
        windowMenu.add(pluginToolbarItem);

        statusBarItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        statusBarItem.setSelected(true);
        statusBarItem.setText("Status Bar");
        statusBarItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                statusBarItemActionPerformed(evt);
            }
        });
        windowMenu.add(statusBarItem);

        speedAndEfficiencyItem.setText("Resources");
        speedAndEfficiencyItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                speedAndEfficiencyItemActionPerformed(evt);
            }
        });
        windowMenu.add(speedAndEfficiencyItem);
        windowMenu.add(jSeparator9);

        startPageItem.setSelected(true);
        startPageItem.setText("Start Page");
        startPageItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startPageItemActionPerformed(evt);
            }
        });
        windowMenu.add(startPageItem);

        bookmarksItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_B, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
        bookmarksItem.setText("Bookmarks");
        bookmarksItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bookmarksItemActionPerformed(evt);
            }
        });
        windowMenu.add(bookmarksItem);

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

        bugReportItem.setText("Report an issue");
        bugReportItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                bugReportItemActionPerformed(evt);
            }
        });
        helpMenu.add(bugReportItem);

        featureRequestItem.setText("Request a feature");
        featureRequestItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                featureRequestItemActionPerformed(evt);
            }
        });
        helpMenu.add(featureRequestItem);
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
            .addComponent(panel_top, javax.swing.GroupLayout.DEFAULT_SIZE, 1027, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addComponent(toolbar_bottom, javax.swing.GroupLayout.DEFAULT_SIZE, 1007, Short.MAX_VALUE)
                .addContainerGap())
            .addComponent(pluginToolbar, javax.swing.GroupLayout.DEFAULT_SIZE, 1027, Short.MAX_VALUE)
            .addComponent(panel_main, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 1027, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(panel_top, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(pluginToolbar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
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
    private void navigationItemMousePressed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_navigationItemMousePressed
        this.panel_top.setVisible(!this.panel_top.isVisible());
    }//GEN-LAST:event_navigationItemMousePressed

    private void zoomInItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_zoomInItemActionPerformed
        LocationController rc = LocationController.getInstance();
        rc.zoomIn();
    }//GEN-LAST:event_zoomInItemActionPerformed

    private void zoomOutItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_zoomOutItemActionPerformed
        LocationController rc = LocationController.getInstance();
        rc.zoomOut();
    }//GEN-LAST:event_zoomOutItemActionPerformed

    private void panLeftItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_panLeftItemActionPerformed
        LocationController rc = LocationController.getInstance();
        rc.shiftRangeLeft();
    }//GEN-LAST:event_panLeftItemActionPerformed

    private void panRightItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_panRightItemActionPerformed
        LocationController rc = LocationController.getInstance();
        rc.shiftRangeRight();
    }//GEN-LAST:event_panRightItemActionPerformed

    private void undoItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_undoItemActionPerformed
        LocationController rc = LocationController.getInstance();
        rc.undoLocationChange();
    }//GEN-LAST:event_undoItemActionPerformed

    private void redoItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_redoItemActionPerformed
        LocationController rc = LocationController.getInstance();
        rc.redoLocationChange();
    }//GEN-LAST:event_redoItemActionPerformed

    private void bookmarkItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bookmarkItemActionPerformed
        BookmarkController fc = BookmarkController.getInstance();
        fc.addCurrentRangeToBookmarks();
    }//GEN-LAST:event_bookmarkItemActionPerformed

    private void formatItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_formatItemActionPerformed
        LOG.info("Showing format form...");
        new DataFormatForm(this, null, false).setVisible(true);
    }//GEN-LAST:event_formatItemActionPerformed

    private void exitItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitItemActionPerformed
        Savant.getInstance().askToDispose();
    }//GEN-LAST:event_exitItemActionPerformed

    private void loadGenomeItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadGenomeItemActionPerformed
        showOpenGenomeDialog();
    }//GEN-LAST:event_loadGenomeItemActionPerformed

    private File lastTrackDirectory = null;

    private void loadFromFileItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadFromFileItemActionPerformed
        if (!GenomeController.getInstance().isGenomeLoaded()) {
            JOptionPane.showMessageDialog(this, "Load a genome first.");
            return;
        }

        File[] selectedFiles = DialogUtils.chooseFilesForOpen("Open Tracks", null, lastTrackDirectory);
        for (File f : selectedFiles) {
            // This creates the tracks asynchronously, which handles all exceptions internally.
            FrameController.getInstance().addTrackFromPath(f.getAbsolutePath(), null);
        }
        if (selectedFiles.length > 0) {
            this.setLastTrackDirectory(selectedFiles[0].getParentFile());
        }
    }//GEN-LAST:event_loadFromFileItemActionPerformed

    private void loadFromURLItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadFromURLItemActionPerformed
        URL url = OpenURLDialog.getURL(this);
        if (url != null) {
            try {
                FrameController.getInstance().addTrackFromURI(url.toURI(), null);
            } catch (URISyntaxException ignored) {
            }
        }
    }//GEN-LAST:event_loadFromURLItemActionPerformed

    private void websiteItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_websiteItemActionPerformed
        try {
            java.awt.Desktop.getDesktop().browse(BrowserSettings.URL.toURI());
        } catch (Exception ex) {
            LOG.error("Unable to access Savant website.", ex);
        }
    }//GEN-LAST:event_websiteItemActionPerformed

    private void menuitem_pluginmanagerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuitem_pluginmanagerActionPerformed
        PluginManagerDialog.getInstance().setVisible(true);
    }//GEN-LAST:event_menuitem_pluginmanagerActionPerformed

    private void plumblineItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_plumblineItemActionPerformed
        GraphPaneController gpc = GraphPaneController.getInstance();
        gpc.setPlumbing(plumblineItem.isSelected());
    }//GEN-LAST:event_plumblineItemActionPerformed

    private void spotlightItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_spotlightItemActionPerformed
        GraphPaneController gpc = GraphPaneController.getInstance();
        gpc.setSpotlight(spotlightItem.isSelected());
    }//GEN-LAST:event_spotlightItemActionPerformed

    private void windowMenuStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_windowMenuStateChanged
        /*
        if(this.getAuxDockingManager().getFrame("Information & Analysis").isVisible() != this.menu_info.getState()) {
        this.menu_info.setState(!this.menu_info.getState());
        }
        if(this.getAuxDockingManager().getFrame("Bookmarks").isVisible() != this.menu_bookmarks.getState()) {
        this.menu_bookmarks.setState(!this.menu_bookmarks.getState());
        }
         */
    }//GEN-LAST:event_windowMenuStateChanged

    private void bookmarksItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bookmarksItemActionPerformed

        String frameKey = "Bookmarks";
        boolean isVisible = auxDockingManager.getFrame(frameKey).isHidden();
        MiscUtils.setFrameVisibility(frameKey, isVisible, auxDockingManager);
        bookmarksItem.setSelected(isVisible);
    }//GEN-LAST:event_bookmarksItemActionPerformed

    private void tutorialsItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tutorialsItemActionPerformed
        try {
            Desktop.getDesktop().browse(BrowserSettings.MEDIA_URL.toURI());
        } catch (Exception ex) {
            LOG.error("Unable to access online tutorials.", ex);
        }
    }//GEN-LAST:event_tutorialsItemActionPerformed

    private void userManualItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_userManualItemActionPerformed
        try {
            Desktop.getDesktop().browse(BrowserSettings.DOCUMENTATION_URL.toURI());
        } catch (Exception ex) {
            LOG.error("Unable to access online user manual.", ex);
        }
    }//GEN-LAST:event_userManualItemActionPerformed

    private void rulerItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rulerItemActionPerformed
        this.ruler.setVisible(!this.ruler.isVisible());
    }//GEN-LAST:event_rulerItemActionPerformed

    private void genomeItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_genomeItemActionPerformed
        rangeSelector.setVisible(!rangeSelector.isVisible());
    }//GEN-LAST:event_genomeItemActionPerformed

    private void statusBarItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_statusBarItemActionPerformed
        this.toolbar_bottom.setVisible(!this.toolbar_bottom.isVisible());
    }//GEN-LAST:event_statusBarItemActionPerformed

    private void menuitem_exportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuitem_exportActionPerformed
        //ExportImageDialog export = new ExportImageDialog(Savant.getInstance(), true);
        //export.setVisible(true);
        ExportImage unused = new ExportImage();
    }//GEN-LAST:event_menuitem_exportActionPerformed

    private void toStartItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_toStartItemActionPerformed
        locationController.shiftRangeFarLeft();
    }//GEN-LAST:event_toStartItemActionPerformed

    private void toEndItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_toEndItemActionPerformed
        locationController.shiftRangeFarRight();
    }//GEN-LAST:event_toEndItemActionPerformed

    private void preferencesItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_preferencesItemActionPerformed
        SettingsDialog dlg = new SettingsDialog(this);
        dlg.setVisible(true);
    }//GEN-LAST:event_preferencesItemActionPerformed

    private void menuitem_deselectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuitem_deselectActionPerformed
        SelectionController.getInstance().removeAll();
    }//GEN-LAST:event_menuitem_deselectActionPerformed

    private void saveProjectAsItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveProjectAsItemActionPerformed
        try {
            ProjectController.getInstance().promptToSaveProjectAs();
        } catch (Exception x) {
            DialogUtils.displayException("Savant Error", "Unable to save project.", x);
        }
    }//GEN-LAST:event_saveProjectAsItemActionPerformed

    private void openProjectItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openProjectItemActionPerformed
        try {
            ProjectController.getInstance().promptToLoadProject();
        } catch (Exception x) {
            DialogUtils.displayException("Savant Error", "Unable to open project.", x);
        }
    }//GEN-LAST:event_openProjectItemActionPerformed

    private void checkForUpdatesItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkForUpdatesItemActionPerformed
        checkVersion(true);
    }//GEN-LAST:event_checkForUpdatesItemActionPerformed

    private void saveProjectItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveProjectItemActionPerformed
        try {
            ProjectController.getInstance().promptToSaveProject();
        } catch (Exception x) {
            DialogUtils.displayException("Savant Error", "Unable to save project.", x);
        }
    }//GEN-LAST:event_saveProjectItemActionPerformed

    private void pluginToolbarItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pluginToolbarItemActionPerformed
        pluginToolbar.setVisible(pluginToolbarItem.isSelected());
    }//GEN-LAST:event_pluginToolbarItemActionPerformed

    private void speedAndEfficiencyItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_speedAndEfficiencyItemActionPerformed
        setSpeedAndEfficiencyIndicatorsVisible(speedAndEfficiencyItem.isSelected());
    }//GEN-LAST:event_speedAndEfficiencyItemActionPerformed

    private void crosshairItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_crosshairItemActionPerformed
        GraphPaneController gpc = GraphPaneController.getInstance();
        gpc.setAiming(crosshairItem.isSelected());
    }//GEN-LAST:event_crosshairItemActionPerformed

    private void startPageItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startPageItemActionPerformed
        setStartPageVisible(startPageItem.isSelected());
    }//GEN-LAST:event_startPageItemActionPerformed

    private void loadFromDataSourcePluginItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadFromDataSourcePluginItemActionPerformed
        try {
            DataSourceAdapter s = DataSourcePluginDialog.getDataSource(this);
            if (s != null) {
                FrameController.getInstance().createFrame(new Track[] { TrackFactory.createTrack(s) });
            }
        } catch (Throwable x) {
            DialogUtils.displayException("Track Creation Failed", "Unable to create track.", x);
        }
    }//GEN-LAST:event_loadFromDataSourcePluginItemActionPerformed

    private void featureRequestItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_featureRequestItemActionPerformed
        (new FeatureRequestDialog(this,false)).setVisible(true);
    }//GEN-LAST:event_featureRequestItemActionPerformed

    private void bugReportItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_bugReportItemActionPerformed
        (new BugReportDialog("Bug Report", null)).setVisible(true);
    }//GEN-LAST:event_bugReportItemActionPerformed

    /**
     * Starts an instance of the Savant Browser
     * @param args the command line arguments
     */
    public static void main(String args[]) {

        try {
            boolean loadProject = false;
            boolean loadPlugin = false;
            String loadProjectUrl = null;
            List<String> loadPluginUrls = new ArrayList<String>();
            for(int i = 0; i < args.length; i++) {
                String s = args[i];
                if(s.startsWith("--")) { //build
                    loadProject = false;
                    loadPlugin = false;
                    BrowserSettings.BUILD = s.replaceAll("-", "");
                    if (s.equals("--debug")) {
                        turnExperimentalFeaturesOff = false;
                    }
                } else if (s.startsWith("-")) {
                    if(s.equals("-project")) {
                        loadProject = true;
                        loadPlugin = false;
                    } else if (s.equals("-plugins")) {
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

            UIManager.put("JideSplitPaneDivider.border", 5);

            // Set System L&F
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

            LookAndFeelFactory.installJideExtension(LookAndFeelFactory.XERTO_STYLE_WITHOUT_MENU);

            // Load project immediately if argument exists.
            if (Savant.getInstance().isWebStart() && loadProjectUrl != null){
                ProjectController.getInstance().loadProjectFromURL(loadProjectUrl);
            }
        } catch (Exception x) {
            LOG.error("Error in main()", x);
        }
    }

    private static void logUsageStats() {
        try {
            URLConnection urlConn;
            DataOutputStream printout;
            // URL of CGI-Bin script.
            // URL connection channel.
            urlConn = BrowserSettings.LOG_USAGE_STATS_URL.openConnection();
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
                    + "&" + post("savant.version", BrowserSettings.VERSION)
                    + "&" + post("savant.build", BrowserSettings.BUILD)
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
        try {
            return id + "=" + id + ":" + ((msg == null) ? "null" : URLEncoder.encode(msg, "UTF-8"));
        } catch (UnsupportedEncodingException ignored) {
            return null;    // Never happens
        }
    }

    public static void checkVersion(boolean verbose) {
        try {
            Version latestVersion = Version.fromURL(BrowserSettings.VERSION_URL);
            Version ourVersion = new Version(BrowserSettings.VERSION);
            int diff = latestVersion.compareTo(ourVersion);
            if (diff > 0) {
                DialogUtils.displayMessage("Savant", "A new version of Savant (" + latestVersion + ") is available.\n"
                        + "To stop this message from appearing, download the newest version at " + BrowserSettings.URL + "\nor disable automatic "
                        + "checking in Preferences.");
            } else if (diff == 0 && BrowserSettings.isBeta()) {
                DialogUtils.displayMessage("Savant", "The release version of Savant (" + latestVersion + ") is available.\n"
                        + "Please upgrade this beta version to the official release.");
            } else {
                if (verbose) {
                    DialogUtils.displayMessage("Savant", "This version of Savant (" + ourVersion + ") is up to date.");
                }
            }
        } catch (IOException x) {
            if (verbose) {
                DialogUtils.displayMessage("Savant Warning", "Could not connect to server. Please ensure you have connection to the internet and try again.");
            }
            LOG.error("Error downloading version file", x);
        }
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem bookmarkItem;
    private javax.swing.JCheckBoxMenuItem bookmarksItem;
    private javax.swing.JCheckBoxMenuItem crosshairItem;
    private javax.swing.JMenuItem deselectAllItem;
    private javax.swing.JMenu editMenu;
    private javax.swing.JMenuItem exitItem;
    private javax.swing.JMenuItem exportItem;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JMenuItem formatItem;
    private javax.swing.JCheckBoxMenuItem genomeItem;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JPopupMenu.Separator jSeparator10;
    private javax.swing.JPopupMenu.Separator jSeparator4;
    private javax.swing.JPopupMenu.Separator jSeparator7;
    private javax.swing.JLabel label_memory;
    private javax.swing.JLabel label_mouseposition_title;
    private javax.swing.JLabel label_status;
    private javax.swing.JMenuItem loadFromDataSourcePluginItem;
    private javax.swing.JMenuItem loadFromFileItem;
    private javax.swing.JMenuItem loadFromURLItem;
    private javax.swing.JMenuItem loadGenomeItem;
    private javax.swing.JMenuBar menuBar_top;
    private javax.swing.JMenuItem menuitem_pluginmanager;
    private javax.swing.JLabel mousePositionLabel;
    private javax.swing.JCheckBoxMenuItem navigationItem;
    private javax.swing.JMenuItem openProjectItem;
    private javax.swing.JMenuItem panLeftItem;
    private javax.swing.JMenuItem panRightItem;
    private javax.swing.JPanel panelExtendedMiddle;
    private javax.swing.JPanel panel_main;
    private javax.swing.JPanel panel_top;
    private javax.swing.JPanel pluginToolbar;
    private javax.swing.JCheckBoxMenuItem pluginToolbarItem;
    private javax.swing.JMenu pluginsMenu;
    private javax.swing.JCheckBoxMenuItem plumblineItem;
    private javax.swing.JMenuItem preferencesItem;
    private javax.swing.JMenu recentProjectMenu;
    private javax.swing.JMenu recentTrackMenu;
    private javax.swing.JMenuItem redoItem;
    private javax.swing.JCheckBoxMenuItem rulerItem;
    private javax.swing.JToolBar.Separator s_e_sep;
    private javax.swing.JMenuItem saveProjectAsItem;
    private javax.swing.JMenuItem saveProjectItem;
    private javax.swing.JCheckBoxMenuItem speedAndEfficiencyItem;
    private javax.swing.JCheckBoxMenuItem spotlightItem;
    private javax.swing.JCheckBoxMenuItem startPageItem;
    private javax.swing.JCheckBoxMenuItem statusBarItem;
    private javax.swing.JLabel timeCaption;
    private javax.swing.JMenuItem toEndItem;
    private javax.swing.JMenuItem toStartItem;
    private javax.swing.JToolBar toolbar_bottom;
    private javax.swing.JMenuItem tutorialsItem;
    private javax.swing.JMenuItem undoItem;
    private javax.swing.JMenuItem userManualItem;
    private javax.swing.JMenu viewMenu;
    private javax.swing.ButtonGroup view_buttongroup;
    private javax.swing.JMenuItem websiteItem;
    private javax.swing.JMenu windowMenu;
    private javax.swing.JMenuItem zoomInItem;
    private javax.swing.JMenuItem zoomOutItem;
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
                        preferencesItemActionPerformed(null);
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
                editMenu.remove(preferencesItem);
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
        initGUIFrame();
        initDocking();
        initMenu();
        initStatusBar();
        initBookmarksPanel();
        initVariationPanel();
        initDataSources();
        initStartPage();
    }

    private void disableExperimentalFeatures() {
    }

    public DockingManager getAuxDockingManager() {
        return auxDockingManager;
    }

    public DockingManager getTrackDockingManager() {
        return trackDockingManager;
    }

    private void initBookmarksPanel() {

        DockableFrame df = DockableFrameFactory.createFrame("Bookmarks", DockContext.STATE_HIDDEN, DockContext.DOCK_SIDE_EAST);
        df.setAvailableButtons(DockableFrame.BUTTON_AUTOHIDE | DockableFrame.BUTTON_FLOATING | DockableFrame.BUTTON_MAXIMIZE);
        auxDockingManager.addFrame(df);
        MiscUtils.setFrameVisibility("Bookmarks", false, auxDockingManager);

        df.getContentPane().setLayout(new BorderLayout());

        //JPanel tablePanel = createTabPanel(jtp, "Bookmarks");
        favoriteSheet = new BookmarkSheet(df.getContentPane());
        BookmarkController bc = BookmarkController.getInstance();
        bc.addListener(favoriteSheet);
        bc.addListener(new Listener<BookmarksChangedEvent>() {
            @Override
            public void handleEvent(BookmarksChangedEvent event) {
                if (showBookmarksChangedDialog) {
                    //Custom button text
                    Object[] options = {"OK", "Don't show again"};
                    int n = JOptionPane.showOptionDialog(Savant.this,
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
                }
            }
        });
    }

    private void initVariationPanel() {

        DockableFrame df = DockableFrameFactory.createFrame("Variation", DockContext.STATE_HIDDEN, DockContext.DOCK_SIDE_EAST);
        df.setAvailableButtons(DockableFrame.BUTTON_AUTOHIDE | DockableFrame.BUTTON_FLOATING | DockableFrame.BUTTON_MAXIMIZE);
        auxDockingManager.addFrame(df);
        MiscUtils.setFrameVisibility("Variation", false, auxDockingManager);
        df.getContentPane().add(new VariationSheet());
    }

    private void askToDispose() {
        try {
            if (ProjectController.getInstance().promptToSaveChanges(true)) {
                System.exit(0);
            }
        } catch (Exception x) {
            DialogUtils.displayException("Error", "Unable to save project file.", x);
        }
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
        this.setIconImage(SavantIconFactory.getInstance().getIcon(SavantIconFactory.StandardIcon.LOGO).getImage());
        this.setTitle("Savant Genome Browser");
        this.setName("Savant Genome Browser");
    }

    private void initMenu() {
        loadGenomeItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_G, MiscUtils.MENU_MASK));
        loadFromFileItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_T, MiscUtils.MENU_MASK));
        loadFromURLItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_U, MiscUtils.MENU_MASK));
        loadFromDataSourcePluginItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_E, MiscUtils.MENU_MASK));
        openProjectItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, MiscUtils.MENU_MASK));
        saveProjectItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, MiscUtils.MENU_MASK));
        saveProjectAsItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, MiscUtils.MENU_MASK | java.awt.event.InputEvent.SHIFT_MASK));
        formatItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, MiscUtils.MENU_MASK));
        exitItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Q, MiscUtils.MENU_MASK));
        undoItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, MiscUtils.MENU_MASK));
        redoItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Y, MiscUtils.MENU_MASK));
        bookmarkItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_B, MiscUtils.MENU_MASK));
        navigationItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.event.InputEvent.SHIFT_MASK | MiscUtils.MENU_MASK));
        panLeftItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_LEFT, java.awt.event.InputEvent.SHIFT_MASK));
        panRightItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_RIGHT, java.awt.event.InputEvent.SHIFT_MASK));
        zoomInItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_UP, java.awt.event.InputEvent.SHIFT_MASK));
        zoomOutItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DOWN, java.awt.event.InputEvent.SHIFT_MASK));
        toStartItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_HOME, java.awt.event.InputEvent.SHIFT_MASK));
        toEndItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_END, java.awt.event.InputEvent.SHIFT_MASK));
        preferencesItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, MiscUtils.MENU_MASK));
        crosshairItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_J, MiscUtils.MENU_MASK));
        plumblineItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_K, MiscUtils.MENU_MASK));
        spotlightItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, MiscUtils.MENU_MASK));
        bookmarksItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_B, MiscUtils.MENU_MASK | java.awt.event.InputEvent.SHIFT_MASK));
        genomeItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, MiscUtils.MENU_MASK | java.awt.event.InputEvent.SHIFT_MASK));
        rulerItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, MiscUtils.MENU_MASK | java.awt.event.InputEvent.SHIFT_MASK));
        statusBarItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, MiscUtils.MENU_MASK | java.awt.event.InputEvent.SHIFT_MASK));
        pluginToolbarItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_T, MiscUtils.MENU_MASK | java.awt.event.InputEvent.SHIFT_MASK));
        exportItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_I, MiscUtils.MENU_MASK));

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
        d.setFromFileDirectory(lastTrackDirectory);
        d.setVisible(true);
    }

    /**
     * Move our start page out of the way and open up our rulers and navigation widgets.
     * Called by FrameController when the first Frame is opened.
     */
    public void showBrowserControls() {
        if (browserControlsShown) {
            return;
        }

        if (startpage != null) {
            startpage.setVisible(false);
        }

        panel_top.setVisible(true);
        navigationItem.setSelected(true);

        rangeSelector.setVisible(true);
        genomeItem.setSelected(true);

        ruler.setVisible(true);
        rulerItem.setSelected(true);

        loadFromFileItem.setEnabled(true);
        loadFromURLItem.setEnabled(true);
        loadFromDataSourcePluginItem.setEnabled(true);

        navigationBar.setVisible(true);
        browserControlsShown = true;
    }

    private void setStartPageVisible(boolean b) {
        MiscUtils.setFrameVisibility("Start Page", b, this.getTrackDockingManager());
        startPageItem.setSelected(b);
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

    private void setSpeedAndEfficiencyIndicatorsVisible(boolean b) {
        speedAndEfficiencyItem.setSelected(b);
        timeCaption.setVisible(b);
        label_memory.setVisible(b);
        label_status.setVisible(b);
        s_e_sep.setVisible(b);
        memorystatusbar.setVisible(b);
    }

    public void addPluginToMenu(JCheckBoxMenuItem cb) {
        pluginsMenu.add(cb);
    }

    public JPanel getPluginToolbar() {
        return pluginToolbar;
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

    private void updateMousePosition(int x, double y, boolean yIntegral) {
        if (x == -1 && Double.isNaN(y)) {
            mousePositionLabel.setText("");
        } else {
            String s = x == -1 ? "" : "X: " + MiscUtils.numToString(x);
            if (!Double.isNaN(y)) {
                // If the value is an exact integer (e.g. for interval tracks) display it with no decimal places.
                s += yIntegral ? String.format(" Y: %d", (int)y) : String.format(" Y: %.3f", y);
            }
            mousePositionLabel.setText(s);
        }
    }

    public String[] getSelectedTracks(boolean multiple, String title) {
        TrackChooser tc = new TrackChooser(Savant.getInstance(), multiple, title);
        tc.setVisible(true);
        String[] tracks = tc.getSelectedTracks();
        return tracks;
    }

    public final void displayBookmarksPanel() {
        MiscUtils.setFrameVisibility("Bookmarks", true, auxDockingManager);
        auxDockingManager.toggleAutohideState("Bookmarks");
        bookmarksItem.setState(true);
        auxDockingManager.setActive(false);

        MiscUtils.setFrameVisibility("Variation", true, auxDockingManager);
        auxDockingManager.toggleAutohideState("Variation");
        auxDockingManager.setActive(false);
    }

    private void initStartPage() {
        if (BrowserSettings.getShowStartPage()) {
            startpage = new StartPanel();
            trackBackground.add(startpage, BorderLayout.CENTER);
        }
    }

    public boolean isWebStart() {
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
                while (st.hasMoreTokens()) {
                    localFile=st.nextToken();
                }

                localFile = new File(DirectorySettings.getPluginsDirectory(), localFile).getAbsolutePath();

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

    public void setLastTrackDirectory(File dir) {
        File full = new File(dir.getAbsolutePath() + System.getProperty("file.separator") + " ");
        LOG.info("Setting directory to: " + full);
        lastTrackDirectory = full;
    }

    private void initHiddenShortcuts() {
        JMenuBar hiddenBar = new JMenuBar();
        hiddenBar.setSize(new Dimension(0,0));
        hiddenBar.setMaximumSize(new Dimension(0,0));
        hiddenBar.setPreferredSize(new Dimension(0,0));

        JMenuItem hiddenBookmarkPrev = new JMenuItem("");
        hiddenBookmarkPrev.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_9, MiscUtils.MENU_MASK));
        hiddenBookmarkPrev.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if(favoriteSheet != null){
                    favoriteSheet.goToPreviousBookmark();
                }
            }
        });

        JMenuItem hiddenBookmarkNext = new JMenuItem("");
        hiddenBookmarkNext.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_0, MiscUtils.MENU_MASK));
        hiddenBookmarkNext.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if(favoriteSheet != null){
                    favoriteSheet.goToNextBookmark();
                }
            }
        });

        hiddenBar.add(hiddenBookmarkPrev);
        hiddenBar.add(hiddenBookmarkNext);
        this.add(hiddenBar);
    }
}
