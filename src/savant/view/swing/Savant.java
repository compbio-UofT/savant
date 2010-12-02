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

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;

import com.apple.eawt.*;
import com.jidesoft.docking.*;
import com.jidesoft.docking.event.DockableFrameEvent;
import com.jidesoft.docking.event.DockableFrameListener;
import com.jidesoft.plaf.LookAndFeelFactory;
import com.jidesoft.plaf.UIDefaultsLookup;
import com.jidesoft.plaf.basic.ThemePainter;
import com.jidesoft.status.MemoryStatusBarItem;
import com.jidesoft.swing.JideSplitPane;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.JDOMException;
import savant.api.adapter.ViewTrackAdapter;

import savant.controller.*;
import savant.controller.event.BookmarksChangedEvent;
import savant.controller.event.BookmarksChangedListener;
import savant.controller.event.RangeSelectionChangedEvent;
import savant.controller.event.RangeSelectionChangedListener;
import savant.controller.event.ReferenceChangedEvent;
import savant.controller.event.ReferenceChangedListener;
import savant.controller.event.TrackListChangedEvent;
import savant.controller.event.TrackListChangedListener;
import savant.data.sources.DataSource;
import savant.data.types.Genome;
import savant.exception.SavantTrackCreationCancelledException;
import savant.file.SavantFileNotFormattedException;
import savant.file.SavantUnsupportedVersionException;
import savant.net.RemoteTrackTreeList;
import savant.experimental.XMLTool;
import savant.file.DataFormat;
import savant.plugin.SavantDataSourcePlugin;
import savant.plugin.builtin.SavantFileRepositoryDataSource;
import savant.settings.*;
import savant.startpage.StartPage;
import savant.swing.component.TrackChooser;
import savant.util.DownloadFile;
import savant.util.MiscUtils;
import savant.util.Range;
import savant.view.dialog.DataFormatForm;
import savant.view.dialog.DataSourcePluginDialog;
import savant.view.dialog.LoadGenomeDialog;
import savant.view.dialog.OpenURLDialog;
import savant.view.dialog.PluginManagerDialog;
import savant.view.icon.SavantIconFactory;
import savant.view.swing.util.DialogUtils;
import savant.view.tools.ToolsModule;
import savant.xml.XMLVersion;
import savant.xml.XMLVersion.Version;

/**
 * Main application Window (Frame).
 *
 * @author mfiume
 */
public class Savant extends javax.swing.JFrame implements RangeSelectionChangedListener,
        /*RangeChangedListener, */ /*PropertyChangeListener,*/ BookmarksChangedListener,
        ReferenceChangedListener, TrackListChangedListener {

    private static final Log LOG = LogFactory.getLog(Savant.class);
    public static boolean turnExperimentalFeaturesOff = true;
    private static boolean isDebugging = false;
    private DockingManager auxDockingManager;
    private JPanel masterPlaceholderPanel;
    private DockingManager trackDockingManager;
    private JPanel trackPanel;
    private JPanel menuPanel;
    private JButton goButton;
    private ToolsModule savantTools;
    private static boolean showNonGenomicReferenceDialog = true;
    private static boolean showBookmarksChangedDialog = true;
    public static final String os = System.getProperty("os.name").toLowerCase();
    public static final boolean mac = os.contains("mac");
    public static final int osSpecificModifier = (mac ? java.awt.event.InputEvent.META_MASK : java.awt.event.InputEvent.CTRL_MASK);
    private static Map<DockableFrame, Frame> dockFrameToFrameMap = new HashMap<DockableFrame, Frame>();
    private DockableFrame genomeFrame = null;
    private DataFormatForm dff;
    private OpenURLDialog urlDialog;
    private MemoryStatusBarItem memorystatusbar;
    private DockableFrame startPageDockableFrame;
    private Application macOSXApplication;
    private ProjectHandler projectHandler;

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

    public void addTrackFromFile(String selectedFileName) throws IOException, SavantFileNotFormattedException, SavantUnsupportedVersionException {

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

        Savant.log("Loading track " + selectedFileName, Savant.LOGMODE.NORMAL);

        try {
            List<ViewTrack> tracks = TrackFactory.createTrack(uri);
            createFrameForTracks(tracks);
        } catch (SavantTrackCreationCancelledException ex) {
        }
    }

    public void createFrameForTracks(List<ViewTrack> tracks) {
        if (tracks != null && tracks.size() > 0) {
            createFrameForTrack(tracks.get(0).getName(), tracks);
        }
    }

    public void createFrameForTrack(String name, List<ViewTrack> tracks) {

        if (!ReferenceController.getInstance().isGenomeLoaded()) {
            if (tracks.get(0).getDataSource().getDataFormat() == DataFormat.SEQUENCE_FASTA) {
                setGenomeFromTrack(tracks.get(0));
            } else {
                DialogUtils.displayError("Sorry", "This does not appear to be a genome track. Please load a genome first.");
            }
            return;
        }

        /*
         if (!ReferenceController.getInstance().isGenomeLoaded()) {
            if (askIfTrackShouldBeLoadedAsGenome()) {
                setGenomeFromTrack(tracks.get(0));
            }
            return;
        }
         *
         */

        /*
        if (!ReferenceController.getInstance().isGenomeLoaded()) {
            int result = JOptionPane.showConfirmDialog(this, "No genome is loaded yet. Load file as genome?", "No genome loaded", JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
            }
            return null;
        }
         * 
         */

        Frame frame = null;
        DockableFrame df = DockableFrameFactory.createTrackFrame(name);
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
        trackDockingManager.addDockableFrameListener(new DockableFrameListener() {

            @Override
            public void dockableFrameAdded(DockableFrameEvent arg0) {
            }

            @Override
            public void dockableFrameRemoved(DockableFrameEvent arg0) {
                FrameController.getInstance().closeFrame(dockFrameToFrameMap.get(arg0.getDockableFrame()));
            }

            @Override
            public void dockableFrameShown(DockableFrameEvent arg0) {
            }

            @Override
            public void dockableFrameHidden(DockableFrameEvent arg0) {
            }

            @Override
            public void dockableFrameDocked(DockableFrameEvent arg0) {
            }

            @Override
            public void dockableFrameFloating(DockableFrameEvent arg0) {
            }

            @Override
            public void dockableFrameAutohidden(DockableFrameEvent arg0) {
            }

            @Override
            public void dockableFrameAutohideShowing(DockableFrameEvent arg0) {
            }

            @Override
            public void dockableFrameActivated(DockableFrameEvent arg0) {
                dockFrameToFrameMap.get(arg0.getDockableFrame()).setActiveFrame();
            }

            @Override
            public void dockableFrameDeactivated(DockableFrameEvent arg0) {
                dockFrameToFrameMap.get(arg0.getDockableFrame()).setInactiveFrame();
            }

            @Override
            public void dockableFrameTabShown(DockableFrameEvent arg0) {
            }

            @Override
            public void dockableFrameTabHidden(DockableFrameEvent arg0) {
            }

            @Override
            public void dockableFrameMaximized(DockableFrameEvent arg0) {
            }

            @Override
            public void dockableFrameRestored(DockableFrameEvent arg0) {
            }

            @Override
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
            String dir = DirectorySettings.getXMLToolDescriptionsDirectory();
            for (String fn : (new File(dir)).list()) {
                if (fn.toLowerCase().endsWith(".xml")) {
                    ToolsModule.addTool(new XMLTool(dir + System.getProperty("file.separator") + fn));
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
            .addGap(0, 894, Short.MAX_VALUE)
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
            .addGap(0, 894, Short.MAX_VALUE)
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

        jLabel1.setText("Status: ");
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
            .addGap(0, 894, Short.MAX_VALUE)
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
        loadFromFileItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadFromFileItemActionPerformed(evt);
            }
        });
        fileMenu.add(loadFromFileItem);

        loadFromURLItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_U, java.awt.event.InputEvent.CTRL_MASK));
        loadFromURLItem.setText("Load Track from URL...");
        loadFromURLItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadFromURLItemActionPerformed(evt);
            }
        });
        fileMenu.add(loadFromURLItem);

        loadFromDataSourcePlugin.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_E, java.awt.event.InputEvent.CTRL_MASK));
        loadFromDataSourcePlugin.setText("Load Track from Other Datasource...");
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
            .addComponent(panel_top, javax.swing.GroupLayout.DEFAULT_SIZE, 894, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addComponent(toolbar_bottom, javax.swing.GroupLayout.DEFAULT_SIZE, 882, Short.MAX_VALUE)
                .addContainerGap())
            .addComponent(panel_toolbar, javax.swing.GroupLayout.DEFAULT_SIZE, 894, Short.MAX_VALUE)
            .addComponent(panel_main, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 894, Short.MAX_VALUE)
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
            Savant.log("Showing format form...");
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
        this.showOpenTracksDialog(false);
    }//GEN-LAST:event_loadFromFileItemActionPerformed

    private void loadFromURLItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadFromURLItemActionPerformed
        this.showOpenURLDialog(false);
    }//GEN-LAST:event_loadFromURLItemActionPerformed

    private void websiteItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_websiteItemActionPerformed
        try {
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(BrowserSettings.url));
        } catch (IOException ex) {
            LOG.error("Unable to access Savant website", ex);
        }
    }//GEN-LAST:event_websiteItemActionPerformed

    private void menuitem_pluginmanagerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuitem_pluginmanagerActionPerformed
        PluginManagerDialog pd = PluginManagerDialog.getInstance();
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
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(BrowserSettings.url_tutorials));
        } catch (IOException ex) {
            LOG.error("Unable to access online tutorials.", ex);
        }
    }//GEN-LAST:event_tutorialsItemActionPerformed

    private void userManualItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_userManualItemActionPerformed
        try {
            java.awt.Desktop.getDesktop().browse(java.net.URI.create(BrowserSettings.url_manuals));
        } catch (IOException ex) {
            LOG.error("Unable to access online user manual.", ex);
        }
    }//GEN-LAST:event_userManualItemActionPerformed

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
            //SettingsDialog.addSection(new ResolutionSettingsSection());
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
        if (DataSourcePluginController.getInstance().hasOnlySavantRepoDataSource()) {
            DataSource s = DataSourcePluginController.getInstance().getDataSourcePlugins().get(0).getDataSource();
            ViewTrack t;
            try {
                if (s == null) { return; }
                t = TrackFactory.createTrack(s);
                    List<ViewTrack> tracks = new ArrayList<ViewTrack>(); // TODO: should not need to do this!!
                    tracks.add(t);
                    createFrameForTrack(t.getName(), tracks);
            } catch (SavantTrackCreationCancelledException ex) {
            }
        } else {
            showLoadFromOtherDataSourceDialog(false);
        }
    }//GEN-LAST:event_loadFromDataSourcePluginActionPerformed

    /**
     * Starts an instance of the Savant Browser
     * @param args the command line arguments
     */
    public static void main(String args[]) {

        if (args.length > 0) {
            if (args[0].equals("--debug")) {
                turnExperimentalFeaturesOff = false;
            }
        }

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
        //}
        //});
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
            url = new URL(BrowserSettings.url_logusagestats);
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
                    + "&" + post("java.version", System.getProperty("java.version"))
                    + "&" + post("java.vendor", System.getProperty("java.vendor"))
                    + "&" + post("os.name", System.getProperty("os.name"))
                    + "&" + post("os.arch", System.getProperty("os.arch"))
                    + "&" + post("os.version", System.getProperty("os.version"))
                    + "&" + post("user.region", System.getProperty("user.region"))
                    + "&" + post("user.timezone", System.getProperty("user.timezone"))
                    + "&" + post("savant.version", BrowserSettings.version);

            printout.writeBytes(content);
            printout.flush();
            printout.close();
            urlConn.getInputStream();
        } catch (IOException ex) {
            Logger.getLogger(Savant.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static String post(String id, String msg) {
        return id + "=" + id + ":" + ((msg == null) ? "null" : URLEncoder.encode(msg));
    }

    public static void checkVersion(boolean verbose) {
        try {
            File versionFile = DownloadFile.downloadFile(new URL(BrowserSettings.url_version), DirectorySettings.getSavantDirectory());
            if (versionFile != null) {
                LOG.info("Saved version file to: " + versionFile.getAbsolutePath());
                Version currentversion = (new XMLVersion(versionFile)).getVersion();
                Version thisversion = new Version(BrowserSettings.version);
                if (currentversion.compareTo(thisversion) > 0) {
                    DialogUtils.displayMessage("Savant", "A new version of Savant (" + currentversion.toString() + ") is available.\n"
                            + "To stop this message from appearing, download the newest version at " + BrowserSettings.url + "\nor disable automatic "
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
        if (mac) {
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
            } catch (Exception e) {
                LOG.error("Unable to load Apple eAWT classes.");
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
        initToolsPanel();
        initMenu();
        initStatusBar();
        initGUIHandlers();
        initBookmarksPanel();
        initDataSources();


        //initStartPage();

        dff = new DataFormatForm(this, false);

        urlDialog = new OpenURLDialog(this, true);
    }

    private void disableExperimentalFeatures() {
        menuitem_tools.setVisible(false);
        MiscUtils.setFrameVisibility("Start Page", false, trackDockingManager);
        menuitem_startpage.setVisible(false);

        // Start page may be null if there was a problem loading the page.
        if (startPageDockableFrame != null) {
            startPageDockableFrame.setVisible(false);
        }
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

            int answer = JOptionPane.showConfirmDialog(
                    Savant.getInstance(),
                    "Save project before quitting?");

            if (answer == JOptionPane.CANCEL_OPTION) {
                return;
            }
            if (answer == JOptionPane.YES_OPTION) {
                projectHandler.promptUserToSaveSession();
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
        menuitem_view_plumbline.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_K, osSpecificModifier));
        menuitem_view_spotlight.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, osSpecificModifier));
        menu_bookmarks.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_B, osSpecificModifier | java.awt.event.InputEvent.SHIFT_MASK));
        menuitem_genomeview.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, osSpecificModifier | java.awt.event.InputEvent.SHIFT_MASK));
        menuitem_ruler.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_L, osSpecificModifier | java.awt.event.InputEvent.SHIFT_MASK));
        menuitem_statusbar.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, osSpecificModifier | java.awt.event.InputEvent.SHIFT_MASK));

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

        this.menuPanel = new JPanel();
        this.panelExtendedMiddle.setLayout(new BorderLayout());
        this.panelExtendedMiddle.add(menuPanel);
        JPanel p = this.menuPanel;

        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));

        Dimension comboboxDimension = new Dimension(150, 23);
        Dimension iconDimension = new Dimension(23, 23);

        String shortcutMod;
        if (mac) {
            shortcutMod = "Cmd";
        } else {
            shortcutMod = "Ctrl";
        }

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

            @Override
            public void actionPerformed(ActionEvent e) {

                // the first item is a header and not allowed to be selected
                if (referenceDropdown.getItemCount() <= 1) {
                    return;
                }

                int index = referenceDropdown.getSelectedIndex();
                String ref = (String) referenceDropdown.getItemAt(index);
                if (ref.contains("[")) {
                    int size = referenceDropdown.getItemCount();
                    for (int i = 1; i < size; i++) {
                        int newindex = (index + i) % size;
                        String newref = (String) referenceDropdown.getItemAt(newindex);
                        if (!((String) referenceDropdown.getItemAt(newindex)).contains("[")) {
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
                    if (!showNonGenomicReferenceDialog) {
                        return;
                    }
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

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Actually changing reference to " + ref);
                }
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

        textboxFrom.addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent evt) {
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

        textboxTo.addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(java.awt.event.KeyEvent evt) {
                RangeTextBoxKeypressed(evt);
            }
        });

        p.add(this.getRigidPadding());


        goButton = addButton(p, "  Go  ");
        goButton.setToolTipText("Go to specified range (Enter)");
        goButton.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseReleased(MouseEvent e) {
                if (goButton.contains(e.getPoint())) {
                    setRangeFromTextBoxes();
                }
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

            @Override
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

            @Override
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
        zoomIn.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                rangeController.zoomIn();
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
        zoomOut.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                rangeController.zoomOut();
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

        shiftFarLeft.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                rangeController.shiftRangeFarLeft();
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
        shiftLeft.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                rangeController.shiftRangeLeft();
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
        shiftRight.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                rangeController.shiftRangeRight();
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
        shiftFarRight.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                rangeController.shiftRangeFarRight();
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
        rangeControls.add(menuitem_deselectall);
        rangeControls.add(shiftFarLeft);
        rangeControls.add(shiftFarRight);
        rangeControls.add(shiftLeft);
        rangeControls.add(shiftRight);
        rangeControls.add(zoomIn);
        rangeControls.add(zoomOut);
        rangeControls.add(rangeSelector);
        rangeControls.add(ruler);
        //rangeControls.add(trackButton);
        rangeControls.add(loadFromFileItem);
        rangeControls.add(loadFromDataSourcePlugin);
        rangeControls.add(loadFromURLItem);
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
        rangeControls.add(menuitem_redo);
        rangeControls.add(menuitem_view_plumbline);
        rangeControls.add(menuitem_view_spotlight);
        rangeControls.add(menuitem_aim);
        rangeControls.add(label_mouseposition);
        rangeControls.add(label_mouseposition_title);
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

    private JPanel createTabPanel(JTabbedPane jtp, String name) {
        JPanel pan = new JPanel();
        pan.setLayout(new BorderLayout());
        pan.setBackground(ColourSettings.getTabBackground());
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
     * Prompt the user to open a genome file.
     *
     * Expects a Binary Fasta file (created using the data formatter included in
     * the distribution).
     */
    public void showOpenGenomeDialog() {

        LoadGenomeDialog d = new LoadGenomeDialog(this,true);
        d.setVisible(true);

        /*

        //Custom button text
        Object[] options = {"From file", /*"From URL", "By length", "Cancel"};
        int n = JOptionPane.showOptionDialog(this,
                "How would you like to specify the genome?",
                "Specify a Genome",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);

        if (n == 1) {

            
            //GenomeLengthForm gf = new GenomeLengthForm(this, true);

            //if (gf.userCompletedForm) {
            //    setGenome(gf.loadedGenome.getName(), gf.loadedGenome);
            //}
        } else if (n == 0) {
            File selectedFile = DialogUtils.chooseFileForOpen(this, "Load Genome", null);
            // set the genome
            if (selectedFile != null) {
                Savant.log("Loading genome " + selectedFile, Savant.LOGMODE.NORMAL);
                try {
                    List<ViewTrack> trks = TrackFactory.createTrack(selectedFile.toURI());
                    if (!trks.isEmpty()) {
                        setGenomeFromTrack(trks.get(0));
                        Savant.log("Genome loaded", Savant.LOGMODE.NORMAL);
                    }
                } catch (Exception x) {
                    LOG.error("Unable to load genome.", x);
                    DialogUtils.displayException("Error Loading Genome", String.format("Unable to load genome from %s.", selectedFile.getName()), x);
                }
            } else {
                showOpenGenomeDialog();
            }
        }
         *
         */
    }

    /**
     * Open Tracks Dialog
     *  Prompt the user to open track file(s).
     *  Expects a Binary formatted file (created using the
     *  data formatter included in the distribution)
     */
    public void showOpenTracksDialog(boolean loadAsGenome) {

        if (loadAsGenome) {
            File selectedFile = DialogUtils.chooseFileForOpen(Savant.getInstance(), "Load Genome", null);
            // set the genome
            if (selectedFile != null) {
                try {
                    List<ViewTrack> trks = TrackFactory.createTrack(selectedFile.toURI());
                    if (!trks.isEmpty()) {
                        Savant.getInstance().setGenomeFromTrack(trks.get(0));
                    }
                } catch (Exception x) {
                    DialogUtils.displayException("Error Loading Genome", String.format("Unable to load genome from %s.", selectedFile.getName()), x);
                }
            } else {
                Savant.getInstance().showOpenGenomeDialog();
            }
        } else {

            if (!ReferenceController.getInstance().isGenomeLoaded()) {
                JOptionPane.showMessageDialog(this, "Load a genome first.");
                return;
            }

            File[] selectedFiles = DialogUtils.chooseFilesForOpen(this, "Open Tracks", null);
            for (File f : selectedFiles) {
                try {
                    addTrackFromFile(f.getAbsolutePath());
                } catch (SavantFileNotFormattedException sfnfx) {
                    promptUserToFormatFile(f.toURI());
                } catch (Exception x) {
                    DialogUtils.displayException("Error", String.format("%s opening %s.", x.getClass(), f), x);
                }
            }
        }
    }

    /** [[EVENTS]]**/
    @Override
    public void rangeSelectionChangeReceived(RangeSelectionChangedEvent event) {
        rangeController.setRange(event.range());
    }

    /*
    @Override
    public void rangeChangeReceived(RangeChangedEvent event) {
    }
     * 
     */
    public void updateRange() {
        // adjust descriptions
        setRangeDescription(RangeController.getInstance().getRange());

        // adjust range controls
        setRangeSelectorFromRange();

        setRulerFromRange();

        // draw all frames
        FrameController fc = FrameController.getInstance();

        fc.drawFrames();

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
                    dff.setInFile(uri.getPath());
                    dff.setLocationRelativeTo(this);
                    dff.setVisible(true);
                }
            }
        } else {
            // TODO: Do something intelligent for network URIs.
            JOptionPane.showMessageDialog(this, "This file does not appear to be formatted. Please try to format it.");
        }
    }

    /**
     * Genome
     */
    /**
     * Set the genome to the specified file.
     *
     * @param f path to the genome.
     */
    public void setGenomeFromTrack(ViewTrack track) {
        Genome g = ViewTrack.createGenome(track);
        if (g != null) {
            setGenome(track.getName(), g);
        }
    }

    /**
     * Set the genome.
     * @param name name of the genome (will be a full path if the sequence is set)
     * @param genome the genome to set
     */
    public void setGenome(String genomeName, Genome genome) {

        boolean someGenomeSetAlready = ReferenceController.getInstance().isGenomeLoaded();

        if (someGenomeSetAlready && this.genomeFrame != null) {
            this.getTrackDockingManager().removeFrame(this.genomeFrame.getTitle());
            this.genomeFrame = null;
        }

        ReferenceController.getInstance().setGenome(genome);
        loadGenomeItem.setText("Change genome...");

        if (genome.isSequenceSet()) {
            DockableFrame df = DockableFrameFactory.createGenomeFrame(genomeName);
            //MiscUtils.getFilenameFromPath(filename));
            JPanel panel = (JPanel) df.getContentPane();
            List<ViewTrack> tracks = new ArrayList<ViewTrack>();
            tracks.add(genome.getViewTrack());
            // layered pane
            panel.setLayout(new BorderLayout());
            Frame frame = new Frame(tracks, df.getName());
            JLayeredPane layers = (JLayeredPane) frame.getFrameLandscape();
            panel.add(layers);
            frame.setDockableFrame(df);
            FrameController.getInstance().addFrame(frame, panel);
            this.getTrackDockingManager().addFrame(df);

            dockFrameToFrameMap.put(df, frame);
            this.genomeFrame = df;
        }

        showBrowserControls();

        //button_genome.setEnabled(false);
        //menuitem_genome.setEnabled(false);

        updateReferenceNamesList();
        referenceDropdown.setSelectedIndex(0);
        //referenceDropdown.setSelectedIndex(1); // dont set it to 0 because that item is not allowed to be selected
    }
    private boolean browserControlsShown = false;

    private void showBrowserControls() {
        if (browserControlsShown) {
            return;
        }

        // This line of code hangs Savant after formatting a genome
        //rangeController.setRange(50, 550);
        rangeSelector.setActive(true);
        ruler.setActive(true);

        this.panel_top.setVisible(true);
        this.menuItem_viewRangeControls.setSelected(true);

        this.rangeSelector.setVisible(true);
        this.menuitem_genomeview.setSelected(true);

        this.ruler.setVisible(true);
        this.menuitem_ruler.setSelected(true);

        //setStartPageVisible(false);
        showRangeControls();
        browserControlsShown = true;
    }

    private void setStartPageVisible(boolean b) {
        MiscUtils.setFrameVisibility("Start Page", b, this.getTrackDockingManager());
        this.menuitem_startpage.setSelected(b);
    }

    /**
     * Set range description.
     *  - Change the from and to textboxes.
     * @param range
     */
    void setRangeDescription(Range range) {
        textboxFrom.setText(MiscUtils.numToString(range.getFrom()));
        textboxTo.setText(MiscUtils.numToString(range.getTo()));
        label_length.setText(MiscUtils.numToString(range.getLength()));
    }

    /**
     * Set the current range from the rangeSelector.
     */
    void setRangeFromRangeSelection() {
        long minRange = rangeSelector.getLowerPosition();
        long maxRange = rangeSelector.getUpperPosition();

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
            fromtext = MiscUtils.removeChar(fromtext, '-');
            int diff = MiscUtils.stringToInt(MiscUtils.removeChar(fromtext, ','));
            to = MiscUtils.stringToInt(MiscUtils.removeChar(textboxTo.getText(), ','));
            from = to - diff + 1;
        } else if (totext.startsWith("+")) {
            totext = MiscUtils.removeChar(totext, '+');
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
    public void bookmarksChangeReceived(BookmarksChangedEvent event) {
        if (!showBookmarksChangedDialog) {
            return;
        }
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

        LOG.debug("Updating reference names list");

        Object curRef = referenceDropdown.getSelectedItem();

        List<String> genomicrefnames = MiscUtils.set2List(ReferenceController.getInstance().getReferenceNames());

        referenceDropdown.removeAllItems();

        //this.referenceDropdown.addItem("[ GENOMIC (" + genomicrefnames.size() + ") ]");
        for (String s : genomicrefnames) {
            this.referenceDropdown.addItem(s);
        }

        //this.referenceDropdown.addItem("[ NON-GENOMIC (" + nongenomicrefnames.size() + ") ]");
        List<String> nongenomicrefnames = MiscUtils.set2List(ReferenceController.getInstance().getNonGenomicReferenceNames());
        if (nongenomicrefnames.size() > 0) {
            for (String s : nongenomicrefnames) {
                this.referenceDropdown.addItem(s);
            }
        }

        if (curRef != null) {
            this.referenceDropdown.setSelectedItem(curRef);
        }
    }

    public void referenceChangeReceived(ReferenceChangedEvent event) {
        this.referenceDropdown.setSelectedItem(event.getReferenceName());
        Genome loadedGenome = ReferenceController.getInstance().getGenome();
        rangeController.setMaxRange(new Range(1, loadedGenome.getLength()));
        rangeSelector.setMaximum(loadedGenome.getLength());
        rangeController.setRange(1, Math.min(1000, loadedGenome.getLength()));
        LOG.debug("referenceChangeReceived has set the range to 1-1000 (or so)");
    }

    @Override
    public void trackListChangeReceived(TrackListChangedEvent evt) {
        updateReferenceNamesList();
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

    private void removeTmpFiles() {
        for (File f : ((new File(DirectorySettings.getTmpDirectory())).listFiles())) {
            f.delete();
        }
    }
     * 
     */

    private void initGUIHandlers() {
        this.projectHandler = ProjectHandler.getInstance();
    }

    private void makeGUIVisible() {
        auxDockingManager.showInitial();
        trackDockingManager.showInitial();

        //this.setExtendedState(this.getExtendedState() | this.MAXIMIZED_BOTH);
    }

    private boolean askIfTrackShouldBeLoadedAsGenome() {
        int result = JOptionPane.showConfirmDialog(this, "No genome is loaded yet. Load file as genome?", "No genome loaded", JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION) {
            return true;
        }
        return false;
    }

    private void initDataSources() {
        DataSourcePluginController.getInstance().addDataSourcePlugin(new SavantFileRepositoryDataSource());
    }

    public int showLoadFromOtherDataSourceDialog(boolean loadAsGenome) {
        List<SavantDataSourcePlugin> ps = DataSourcePluginController.getInstance().getDataSourcePlugins();
        DataSourcePluginDialog d = new DataSourcePluginDialog(this, true, DataSourcePluginController.getInstance().getDataSourcePlugins());
        d.setVisible(true);
        SavantDataSourcePlugin p = d.getSelectedPlugin();
        d.dispose();

        // 0 = loaded fine
        // 1 = cancelled
        // 2 = error

        if (p != null) {
            System.out.println("Plugin selected: " + p.getTitle());
            DataSource s = p.getDataSource();
            ViewTrack t;
            try {
                if (s == null) { return 2; }
                t = TrackFactory.createTrack(s);
                if (loadAsGenome) {
                    setGenomeFromTrack(t);
                } else {
                    List<ViewTrack> tracks = new ArrayList<ViewTrack>(); // TODO: should not need to do this!!
                    tracks.add(t);
                    createFrameForTrack(t.getName(), tracks);
                }
                return 0;
            } catch (SavantTrackCreationCancelledException ex) {
                return 1;
            }
        } else {
            return 2;
        }
    }

    public enum LOGMODE {

        NORMAL, DEBUG
    };

    /**
     * log a message on the given text area
     * @param rtb The text area on which to post the message
     * @param msg The message to post
     *
    public static void log(JTextArea rtb, String msg) {
    log(rtb, msg, LOGMODE.DEBUG);
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
     */
    /**
     * log a message on the default log text area
     * @param msg The message to post
     */
    public static void log(String msg) {
        log(msg, LOGMODE.DEBUG);
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

    public void updateMousePosition() {
        GraphPaneController gpc = GraphPaneController.getInstance();
        long x = gpc.getMouseXPosition();
        long y = gpc.getMouseYPosition();
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

    public void showOpenURLDialog(boolean loadAsGenome) {
        urlDialog.setLocationRelativeTo(this);
        urlDialog.setVisible(true);

        if (urlDialog.isAccepted()) {
            String urlString = urlDialog.getUrlAsString();
            try {
                URL url = new URL(urlString);
                String proto = url.getProtocol().toLowerCase();
                if (!proto.equals("http") && !proto.equals("ftp")) {
                    DialogUtils.displayMessage("Sorry", "Only files accessible via HTTP or FTP can be opened via URL.");
                    return;
                }
            } catch (MalformedURLException e) {
                // ignore, since it was already caught by the dialog and should never happen here
            }
            try {

                List<ViewTrack> tracks = TrackFactory.createTrack(new URI(urlDialog.getUrlAsString()));

                if (loadAsGenome) {
                    setGenomeFromTrack(tracks.get(0));
                } else {
                    createFrameForTrack(tracks.get(0).getName(), tracks);
                }

            } catch (Exception ex) {
                DialogUtils.displayException("Load Track from URL", "Error opening remote file", ex);
            }
        } else {
            if (loadAsGenome) {
                Savant.getInstance().showOpenGenomeDialog();
            }
        }
    }

    private void displayAuxPanels() {

        List<String> names = this.getAuxDockingManager().getAllFrameNames();
        for (int i = 0; i < names.size(); i++) {
            MiscUtils.setFrameVisibility(names.get(i), false, this.getAuxDockingManager());
        }

        MiscUtils.setFrameVisibility("Bookmarks", true, this.getAuxDockingManager());
        this.getAuxDockingManager().toggleAutohideState("Bookmarks");
        menu_bookmarks.setState(true);
    }

    public SelectionController getSelectionController() {
        return this.selectionController;
    }

    private void initStartPage() {

        try {
            StartPage sp = new StartPage();
            sp.setMaximumSize(new java.awt.Dimension(99999, 99999));
            sp.setMinimumSize(new java.awt.Dimension(500, 500));
            sp.setPreferredSize(new java.awt.Dimension(99999, 99999));

            startPageDockableFrame = DockableFrameFactory.createFrame("Start Page", DockContext.STATE_AUTOHIDE_SHOWING, DockContext.DOCK_SIDE_NORTH);
            startPageDockableFrame.setAvailableButtons(DockableFrame.BUTTON_CLOSE);
            this.getTrackDockingManager().addFrame(startPageDockableFrame);
            /*try {
            df.setMaximized(true);
            } catch (PropertyVetoException ex) {
            }*/
            startPageDockableFrame.getContentPane().setLayout(new BorderLayout());
            startPageDockableFrame.getContentPane().add(sp, BorderLayout.NORTH);
            MiscUtils.setFrameVisibility("Start Page", true, getTrackDockingManager());

        } catch (IOException ex) {
            LOG.error("Unable to load start page.", ex);
        }
    }
}
