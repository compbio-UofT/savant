/*
 *    Copyright 2010-2012 University of Toronto
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

package savant.selection;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Iterator;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import savant.api.adapter.DataSourceAdapter;
import savant.api.adapter.PopupHostingAdapter;
import savant.api.data.DataFormat;
import savant.api.data.Interval;
import savant.api.data.IntervalRecord;
import savant.api.data.Record;
import savant.controller.BookmarkController;
import savant.controller.LocationController;
import savant.data.types.TabixIntervalRecord;
import savant.util.Bookmark;
import savant.util.DrawingMode;
import savant.util.MiscUtils;
import savant.util.Range;
import savant.view.tracks.Track;

/**
 * Panel to display information about the current selection.
 *
 * @author AndrewBrook
 */
public abstract class PopupPanel extends JPanel {

    private static JPopupMenu activePopup;
    private static PopupHostingAdapter activeHost;

    protected PopupHostingAdapter host;
    protected DrawingMode mode;
    protected DataFormat fileFormat;
    protected Record record;

    // Common fields used for bookmarking.
    protected String name;
    protected String ref;
    protected int start;
    protected int end;

    private static PopupPanel create(PopupHostingAdapter parent, DrawingMode mode, DataSourceAdapter dataSource, Record rec) {

        PopupPanel p = null;
        switch (dataSource.getDataFormat()) {
            case POINT:
                p = new PointGenericPopup();
                break;
            case ALIGNMENT:
                if (mode != DrawingMode.SNP && mode != DrawingMode.STRAND_SNP) {
                    p = new IntervalBamPopup();
                } else {
                    p = new PileupPopup();
                }
                break;
            case RICH_INTERVAL:
                if (mode != DrawingMode.SQUISH) {
                    if (rec instanceof TabixIntervalRecord) {
                        p = new TabixPopup(dataSource);
                    } else {
                        p = new IntervalBedPopup();
                    }
                }
                break;
            case GENERIC_INTERVAL:
                if (rec instanceof TabixIntervalRecord) {
                    p = new TabixPopup(dataSource);
                } else {
                    p = new IntervalGenericPopup();
                }
                break;
            case CONTINUOUS:
                p = new ContinuousPopup();
                break;
            case VARIANT:
                p = new VariantPopup();
                break;
            default:
                break;
        }

        if (p != null) {
            p.init(parent, mode, dataSource.getDataFormat(), rec);
        }
        return p;
    }

    public static void showPopup(PopupHostingAdapter parent, Point globalPt, Track t, Record overRecord) {
        JPopupMenu jp = new JPopupMenu();
        PopupPanel pp = create(parent, t.getDrawingMode(), t.getDataSource(), overRecord);
        parent.firePopupEvent(pp);
        if (pp != null) {
            jp.setLayout(new BorderLayout());
            jp.add(pp, BorderLayout.CENTER);
            jp.setLocation(globalPt.x - 2, globalPt.y - 2);
            activePopup = jp;
            activeHost = parent;
            jp.setVisible(true);
        }
    }

    
    public static void hidePopup() {
        if (activePopup != null) {
            activePopup.setVisible(false);
            activePopup = null;
        }
        if (activeHost != null) {
            activeHost.popupHidden();
            activeHost = null;
        }
    }


    protected void init(PopupHostingAdapter parent, DrawingMode m, DataFormat ff, Record rec) {

        fileFormat = ff;
        mode = m;
        host = parent;
        record = rec;

        setBackground(Color.WHITE);
        setLayout(new GridLayout(0, 1));
        setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        initInfo();
        initStandardButtons();
        initSpecificButtons();

    }

    protected void initStandardButtons() {

        //START OF BUTTONS
        add(new JSeparator());

        Buttonoid select = new Buttonoid("Select/Deselect");
        select.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                for (Track t: host.getTracks()) {
                    if (t.getDataFormat() == fileFormat) {
                        t.getRenderer().addToSelected(record);
                        break;
                    }
                }
                host.repaint();
            }
        });
        add(select);

        //BOOKMARKING
        if (ref == null) {
            ref = LocationController.getInstance().getReferenceName();
        }
        if (start != -1 && end != -1) {
            Buttonoid bookmark = new Buttonoid("Add to Bookmarks");
            bookmark.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    ref = homogenizeRef(ref);
                    if (name != null) {
                        BookmarkController.getInstance().addBookmark(new Bookmark(ref, new Range(start, end), name, true));
                    } else {
                        BookmarkController.getInstance().addBookmark(new Bookmark(ref, new Range(start, end), true));
                    }
                    hidePopup();
                }
            });
            add(bookmark);
        }
    }


    protected void initSpecificButtons() {};

    protected abstract void initInfo();

    protected String homogenizeRef(String orig) {
        if (!LocationController.getInstance().getAllReferenceNames().contains(orig)) {
            Iterator<String> it = LocationController.getInstance().getAllReferenceNames().iterator();
            while(it.hasNext()) {
                String current = it.next();
                if (MiscUtils.homogenizeSequence(current).equals(orig)) {
                    return current;
                }
            }
        }
        return orig;
    }

    public Record getRecord() {
        return record;
    }

    protected void initIntervalJumps() {
        //jump to start of read
        if (LocationController.getInstance().getRangeStart() > ((IntervalRecord)record).getInterval().getStart()) {
            Buttonoid startJump = new Buttonoid("Jump to Start of Interval");
            startJump.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    LocationController lc = LocationController.getInstance();
                    int len = lc.getRangeEnd() - lc.getRangeStart();
                    Interval inter = ((IntervalRecord)record).getInterval();
                    lc.setLocation(inter.getStart() - len / 2, inter.getStart() + len / 2);
                    hidePopup();
                }
            });
            add(startJump);
        }

        //jump to end of read
        if (LocationController.getInstance().getRangeEnd() < ((IntervalRecord)record).getInterval().getEnd()) {
            Buttonoid endJump = new Buttonoid("Jump to End of Interval");
            endJump.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    LocationController lc = LocationController.getInstance();
                    int len = lc.getRangeEnd() - lc.getRangeStart();
                    Interval inter = ((IntervalRecord)record).getInterval();
                    lc.setLocation(inter.getEnd() - len / 2, inter.getEnd() + len / 2);
                    hidePopup();
                }
            });
            add(endJump);
        }
    }
    
    /**
     * A clickable buttonish thing on the popup panel.
     */
    class Buttonoid extends JLabel {
        Buttonoid(String text) {
            super(text);
            setForeground(Color.BLUE);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setOpaque(true);
            
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent me) {
                    setBackground(Color.BLUE);
                    setForeground(Color.WHITE);
                }

                @Override
                public void mouseExited(MouseEvent me) {
                    setBackground(Color.WHITE);
                    setForeground(Color.BLUE);
                }
            });
        }
    }
}