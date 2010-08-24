/*
 *    Copyright 2010 University of Toronto
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
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Iterator;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import savant.controller.BookmarkController;
import savant.controller.ReferenceController;
import savant.data.types.*;
import savant.file.FileFormat;
import savant.model.view.Mode;
import savant.util.Bookmark;
import savant.util.MiscUtils;
import savant.util.Range;
import savant.view.swing.GraphPane;

/**
 *
 * @author AndrewBrook
 */
public class PopupPanel extends JPanel {

    protected GraphPane gp;
    protected Mode mode;
    protected FileFormat fileFormat;
    protected Record record;

    //info
    protected String name;
    protected String ref;
    protected int start;
    protected int end;

    public static PopupPanel create(GraphPane parent, Mode mode, FileFormat ff, Record rec){

        PopupPanel p = null;
        switch(ff){
            case POINT_GENERIC:
                p = new PointGenericPopup((PointRecord) rec);
                break;
            case INTERVAL_BAM:
                if(!mode.getName().equals("SNP")){
                    p = new IntervalBamPopup((BAMIntervalRecord)rec);
                }
                break;
            case INTERVAL_BED:
                if(!mode.getName().equals("SQUISH")){
                    p = new IntervalBedPopup((BEDIntervalRecord)rec);
                }
                break;
            case INTERVAL_GENERIC:
                p = new IntervalGenericPopup((GenericIntervalRecord)rec);
                break;
            case CONTINUOUS_GENERIC:
                p = new ContinuousPopup((GenericContinuousRecord)rec);
                break;
            default:
                break;
        }

        if(p != null) p.init(parent, mode, ff, rec);
        return p;
    }

    protected void init(GraphPane parent, Mode mode, FileFormat ff, Record rec){

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
        select.addMouseListener(new MouseListener() {
            public void mouseClicked(MouseEvent e) {
                gp.getTrackRenderers().get(0).addToSelected(record);
                gp.repaint();
            }
            public void mousePressed(MouseEvent e) {}
            public void mouseReleased(MouseEvent e) {}
            public void mouseEntered(MouseEvent e) {}
            public void mouseExited(MouseEvent e) {}
        });
        this.add(select);

        //BOOKMARKING
        if(ref == null){
            ref = ReferenceController.getInstance().getReferenceName();
        }
        if(start != -1 && end != -1){
            JLabel bookmark = new JLabel("Add to Bookmarks");
            bookmark.setForeground(Color.BLUE);
            bookmark.setCursor(new Cursor(Cursor.HAND_CURSOR));
            bookmark.addMouseListener(new MouseListener() {
                public void mouseClicked(MouseEvent e) {
                    ref = homogenizeRef(ref);
                    if(name != null){
                        BookmarkController.getInstance().addBookmark(new Bookmark(ref, new Range(start, end), name));
                    } else {
                        BookmarkController.getInstance().addBookmark(new Bookmark(ref, new Range(start, end)));
                    }
                }
                public void mousePressed(MouseEvent e) {}
                public void mouseReleased(MouseEvent e) {}
                public void mouseEntered(MouseEvent e) {}
                public void mouseExited(MouseEvent e) {}
            });
            this.add(bookmark);
        }
    }

    protected void initSpecificButtons(){};

    //default...should be overridden in subclasses
    protected void calculateInfo(){
        name = null;
        ref = null;
        start = -1;
        end = -1;
    };

    protected void initInfo(){};

    protected void hidePanel(){
        this.gp.hidePopup();
    }

    protected String homogenizeRef(String orig){
        if(!ReferenceController.getInstance().getAllReferenceNames().contains(orig)){
            Iterator<String> it = ReferenceController.getInstance().getAllReferenceNames().iterator();
            while(it.hasNext()){
                String current = it.next();
                if(MiscUtils.homogenizeSequence(current).equals(orig)){
                    return current;
                }
            }
        }
        return orig;
    }

}