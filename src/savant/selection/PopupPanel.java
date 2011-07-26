/*
 *    Copyright 2010-2011 University of Toronto
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

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Iterator;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import savant.api.adapter.DataSourceAdapter;
import savant.controller.BookmarkController;
import savant.controller.LocationController;
import savant.data.types.*;
import savant.file.DataFormat;
import savant.util.Bookmark;
import savant.util.DrawingMode;
import savant.util.MiscUtils;
import savant.util.Range;
import savant.view.swing.GraphPane;
import savant.view.swing.Track;

/**
 * Panel to display information about the current selection.
 *
 * @author AndrewBrook
 */
public abstract class PopupPanel extends JPanel {

    protected GraphPane gp;
    protected DrawingMode mode;
    protected DataFormat fileFormat;
    protected Record record;

    //info
    protected String name;
    protected String ref;
    protected int start;
    protected int end;

    public static PopupPanel create(GraphPane parent, DrawingMode mode, DataSourceAdapter dataSource, Record rec){

        PopupPanel p = null;
        switch(dataSource.getDataFormat()) {
            case POINT_GENERIC:
                p = new PointGenericPopup((GenericPointRecord) rec);
                break;
            case INTERVAL_BAM:
                if (mode != DrawingMode.SNP) {
                    p = new IntervalBamPopup((BAMIntervalRecord)rec);
                }
                break;
            case INTERVAL_RICH:
                if (mode != DrawingMode.SQUISH) {
                    if (rec instanceof TabixIntervalRecord) {
                        p = new TabixPopup((TabixIntervalRecord)rec, dataSource);
                    } else {
                        p = new IntervalBedPopup((BEDIntervalRecord)rec);
                    }
                }
                break;
            case INTERVAL_GENERIC:
                if (rec instanceof TabixIntervalRecord) {
                    p = new TabixPopup((TabixIntervalRecord)rec, dataSource);
                } else {
                    p = new IntervalGenericPopup((GenericIntervalRecord)rec);
                }
                break;
            case CONTINUOUS_GENERIC:
                p = new ContinuousPopup((GenericContinuousRecord)rec);
                break;
            default:
                break;
        }

        if(p != null) p.init(parent, mode, dataSource.getDataFormat(), rec);
        return p;
    }

    protected void init(GraphPane parent, DrawingMode mode, DataFormat ff, Record rec){

        this.fileFormat = ff;
        this.mode = mode;
        this.gp = parent;
        this.record = rec;

        this.setBackground(Color.WHITE);
        this.setLayout(new GridLayout(0,1));

        calculateInfo();
        initInfo();
        initStandardButtons();
        initSpecificButtons();

    }

    protected void initStandardButtons(){

        //START OF BUTTONS
        JPanel filler = new JPanel();
        filler.setPreferredSize(new Dimension(5,5));
        filler.setSize(new Dimension(5,5));
        filler.setBackground(Color.WHITE);
        this.add(filler);
        this.add(new JSeparator());

        //SELECT
        final JLabel select = new JLabel("Select/Deselect");
        select.setForeground(Color.BLUE);
        select.setCursor(new Cursor(Cursor.HAND_CURSOR));
        select.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                for (Track t: gp.getTracks()) {
                    if (t.getDataFormat() == fileFormat){
                        t.getRenderer().addToSelected(record);
                        break;
                    }
                }
                gp.repaint();
            }
        });
        this.add(select);

        //BOOKMARKING
        if(ref == null){
            ref = LocationController.getInstance().getReferenceName();
        }
        if(start != -1 && end != -1){
            JLabel bookmark = new JLabel("Add to Bookmarks");
            bookmark.setForeground(Color.BLUE);
            bookmark.setCursor(new Cursor(Cursor.HAND_CURSOR));
            bookmark.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    ref = homogenizeRef(ref);
                    if (name != null){
                        BookmarkController.getInstance().addBookmark(new Bookmark(ref, new Range(start, end), name));
                    } else {
                        BookmarkController.getInstance().addBookmark(new Bookmark(ref, new Range(start, end)));
                    }
                    hidePopup();
                }
            });
            this.add(bookmark);
        }
    }

    protected void initSpecificButtons(){};

    protected abstract void calculateInfo();

    protected void initInfo(){};

    protected String homogenizeRef(String orig){
        if(!LocationController.getInstance().getAllReferenceNames().contains(orig)){
            Iterator<String> it = LocationController.getInstance().getAllReferenceNames().iterator();
            while(it.hasNext()){
                String current = it.next();
                if(MiscUtils.homogenizeSequence(current).equals(orig)){
                    return current;
                }
            }
        }
        return orig;
    }

    public Record getRecord() {
        return record;
    }

    public void hidePopup() {
        gp.hidePopup();
    }

    protected void initIntervalJumps(final IntervalRecord rec){
        //jump to start of read
        if(LocationController.getInstance().getRangeStart() > rec.getInterval().getStart()){
            JLabel endJump = new JLabel("Jump to Start of Interval");
            endJump.setForeground(Color.BLUE);
            endJump.setCursor(new Cursor(Cursor.HAND_CURSOR));
            endJump.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    LocationController lc = LocationController.getInstance();
                    int len = lc.getRangeEnd() - lc.getRangeStart();
                    lc.setLocation(rec.getInterval().getStart()-(len/2), rec.getInterval().getStart()+(len/2));
                    hidePopup();
                }
            });
            this.add(endJump);
        }

        //jump to end of read
        if(LocationController.getInstance().getRangeEnd() < rec.getInterval().getEnd()){
            JLabel endJump = new JLabel("Jump to End of Interval");
            endJump.setForeground(Color.BLUE);
            endJump.setCursor(new Cursor(Cursor.HAND_CURSOR));
            endJump.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    LocationController lc = LocationController.getInstance();
                    int len = lc.getRangeEnd() - lc.getRangeStart();
                    lc.setLocation(rec.getInterval().getEnd()-(len/2), rec.getInterval().getEnd()+(len/2));
                    hidePopup();
                }
            });
            this.add(endJump);
        }
    }

}