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
package savant.swing.component;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import savant.controller.FrameController;
import savant.file.DataFormat;
import savant.view.swing.Frame;
import savant.view.swing.Savant;

/**
 *
 * @author AndrewBrook
 */
public class TrackChooser extends javax.swing.JDialog {

    private boolean multiple;
    private JPanel filler1;
    private JPanel filler2;
    private JPanel filler3;
    private JList leftList;
    private JList rightList;
    private JScrollPane leftScroll;
    private JScrollPane rightScroll;
    private JButton moveRight;
    private JButton moveLeft;
    private JButton allRight;
    private JButton allLeft;
    private JButton okButton;
    private JButton cancelButton;
    private String[] retVal;
    private JLabel leftLabel;
    private JLabel rightLabel;
    private JPanel filterPanel;
    private JLabel filterLabel;
    private JComboBox filterCombo;
    private String[] filteredTracks = null;

    @Override
    public void setVisible(boolean isVisible) {
        if (isVisible) {
            initLists();
        }
        super.setVisible(isVisible);
    }

    public TrackChooser(Savant parent, boolean multiple, String title){
        super(parent, true);

        //this.setVisible(true);
        
        this.multiple = multiple;
        this.setTitle(title);

        init();
        initLists();
        
    }

    public String[] getSelectedTracks(){
        return retVal;
    }

    private void filter(){

        if(this.filteredTracks != null){
            for(int i = 0; i < filteredTracks.length; i++){
                ((TrackListModel)leftList.getModel()).add(filteredTracks[i]);
            }
            filteredTracks = null;
        }
        leftList.updateUI();
        leftList.clearSelection();
        if(filterCombo.getSelectedItem().equals("ALL")){
            leftList.updateUI();
            leftList.clearSelection();
            return;
        }

        DataFormat ff = (DataFormat)filterCombo.getSelectedItem();
        String[] leftTracks = ((TrackListModel)leftList.getModel()).getAll();       
        List<Frame> frames = FrameController.getInstance().getFrames();

        String[] removed = new String[leftTracks.length];
        int[] remove = new int[leftTracks.length];
        int count = 0;
        for(int i = 0; i < leftTracks.length; i++){
            String current = leftTracks[i];
            for(int j = 0; j <frames.size(); j++){
                if(frames.get(j).getName().equals(current)){
                    if(!frames.get(j).getTracks().get(0).getDataType().equals(ff)){
                        remove[count] = i;
                        removed[count] = current;
                        count++;
                    }
                    break;
                }
            }
        }
        int[] removeFinal = new int[count];
        String[] removedFinal = new String[count];
        for(int i = 0; i < count; i++){
            removeFinal[i] = remove[i];
            removedFinal[i] = removed[i];
        }

        this.filteredTracks = removedFinal;
        ((TrackListModel)leftList.getModel()).removeIndices(removeFinal);
        leftList.updateUI();
        leftList.clearSelection();
    }

    private void initLayout(){
        
        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        //FILLER
        filler1 = new JPanel();
        //filler1.setSize(10,40);
        filler1.setPreferredSize(new Dimension(10,10));
        //filler1.setBackground(Color.red);
        c.weightx = 1.0;
        c.weighty = 3.0;
        c.gridx = 0;
        c.gridy = 0;
        this.add(filler1, c);

        //LEFT LABEL
        leftLabel = new JLabel("All Tracks");
        leftLabel.setFont(new Font(null, Font.BOLD, 12));
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridx = 1;
        c.gridy = 1;
        c.gridwidth = 1;
        c.anchor = c.CENTER;
        this.add(leftLabel, c);

        //RIGHT LABEL
        rightLabel = new JLabel("Selected Tracks");
        rightLabel.setFont(new Font(null, Font.BOLD, 12));
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridx = 3;
        c.gridy = 1;
        c.gridwidth = 2;
        c.anchor = c.CENTER;
        this.add(rightLabel, c);

        //LEFT LIST
        leftList = new JList();
        leftScroll = new JScrollPane();
        leftScroll.setViewportView(leftList);
        leftScroll.setMinimumSize(new Dimension(200,300));
        leftScroll.setPreferredSize(new Dimension(200,300));
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridx = 1;
        c.gridy = 2;
        c.gridwidth = 1;
        c.gridheight = 4;
        this.add(leftScroll, c);

        //FILLER
        filler3 = new JPanel();
        filler3.setSize(60, 10);
        filler3.setPreferredSize(new Dimension(60,10));
        //filler3.setBackground(Color.red);
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridx = 2;
        c.gridy = 0;
        c.gridheight = 1;
        this.add(filler3, c);

        //MOVE RIGHT
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridx = 2;
        c.gridy = 2;
        c.gridheight = 1;
        this.add(moveRight, c);

        //MOVE LEFT
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridx = 2;
        c.gridy = 3;
        c.gridheight = 1;
        this.add(moveLeft, c);

        //ALL RIGHT
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridx = 2;
        c.gridy = 4;
        c.gridheight = 1;
        this.add(allRight, c);

        //ALL LEFT
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridx = 2;
        c.gridy = 5;
        c.gridheight = 1;
        this.add(allLeft, c);

        //RIGHT LIST
        rightList = new JList();
        rightScroll = new JScrollPane();
        rightScroll.setViewportView(rightList);
        rightScroll.setMinimumSize(new Dimension(200,300));
        rightScroll.setPreferredSize(new Dimension(200,300));
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridx = 3;
        c.gridy = 2;
        c.gridheight = 4;
        c.gridwidth = 2;
        this.add(rightScroll, c);

        //FILLER
        filler2 = new JPanel();
        filler2.setSize(10, 40);
        filler2.setPreferredSize(new Dimension(10,40));
        //filler2.setBackground(Color.red);
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 5;
        c.gridy = 6;
        c.gridheight = 1;
        c.gridwidth = 1;
        this.add(filler2, c);

        //FILTER
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridx = 1;
        c.gridy = 6;
        c.gridheight = 1;
        c.gridwidth = 1;
        this.add(filterPanel, c);

        //OK
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridx = 3;
        c.gridy = 6;
        c.gridheight = 1;
        c.gridwidth = 1;
        this.add(okButton, c);

        //CANCEL
        c.weightx = 1.0;
        c.weighty = 0.5;
        c.gridx = 4;
        c.gridy = 6;
        c.gridheight = 1;
        c.gridwidth = 1;
        this.add(cancelButton, c);

        pack();
    }

    private void createFilter(){
        filterPanel = new JPanel();
        filterPanel.setLayout(new BorderLayout());

        filterLabel = new JLabel("Filter By: ");
        filterLabel.setPreferredSize(new Dimension(50,20));
        filterPanel.add(filterLabel, BorderLayout.WEST);

        filterCombo = new JComboBox();
        filterCombo.setPreferredSize(new Dimension(140,20));       
        filterPanel.add(filterCombo, BorderLayout.EAST);
    }

    private void createOkButton(){
        okButton = new JButton("OK");
        okButton.addMouseListener(new MouseListener() {
            public void mouseClicked(MouseEvent e) {
                retVal = ((TrackListModel)rightList.getModel()).getAll();
                dispose();
            }
            public void mousePressed(MouseEvent e) {}
            public void mouseReleased(MouseEvent e) {}
            public void mouseEntered(MouseEvent e) {}
            public void mouseExited(MouseEvent e) {}
        });    
    }

    private void createCancelButton(){
        cancelButton = new JButton("Cancel");
        cancelButton.addMouseListener(new MouseListener() {
            public void mouseClicked(MouseEvent e) {
                retVal = null;
                dispose();
            }
            public void mousePressed(MouseEvent e) {}
            public void mouseReleased(MouseEvent e) {}
            public void mouseEntered(MouseEvent e) {}
            public void mouseExited(MouseEvent e) {}
        });
    }

    private void createMoveRight(){
        moveRight = new JButton(">");
        moveRight.setToolTipText("Add item(s) to selected");
        moveRight.addMouseListener(new MouseListener() {
            public void mouseClicked(MouseEvent e) {
                Object[] selected = leftList.getSelectedValues();
                if(selected.length > 1 && !multiple) return;
                if(((TrackListModel)rightList.getModel()).getSize() > 0 && !multiple) return;
                for(int i = 0; i < selected.length; i++){
                    ((TrackListModel)rightList.getModel()).add(selected[i].toString());
                }                      
                ((TrackListModel)leftList.getModel()).removeIndices(leftList.getSelectedIndices());
                rightList.updateUI();
                leftList.updateUI();
                leftList.clearSelection();
                rightList.clearSelection();
            }
            public void mousePressed(MouseEvent e) {}
            public void mouseReleased(MouseEvent e) {}
            public void mouseEntered(MouseEvent e) {}
            public void mouseExited(MouseEvent e) {}
        });
    }

    private void createMoveLeft(){
        moveLeft = new JButton("<");
        moveLeft.setToolTipText("Remove item(s) from selected");
        moveLeft.addMouseListener(new MouseListener() {
            public void mouseClicked(MouseEvent e) {
                Object[] selected = rightList.getSelectedValues();
                if(selected.length > 1 && !multiple) return;
                for(int i = 0; i < selected.length; i++){
                    ((TrackListModel)leftList.getModel()).add(selected[i].toString());
                }                
                ((TrackListModel)rightList.getModel()).removeIndices(rightList.getSelectedIndices());
                leftList.updateUI();
                rightList.updateUI();
                leftList.clearSelection();
                rightList.clearSelection();
            }
            public void mousePressed(MouseEvent e) {}
            public void mouseReleased(MouseEvent e) {}
            public void mouseEntered(MouseEvent e) {}
            public void mouseExited(MouseEvent e) {}
        });
    }

    private void createAllRight(){
        allRight = new JButton(">>");
        allRight.setToolTipText("Add all to selected");
        if(!this.multiple) allRight.setEnabled(false);
        allRight.addMouseListener(new MouseListener() {
            public void mouseClicked(MouseEvent e) {
                String[] stringsLeft = ((TrackListModel)leftList.getModel()).getAll();
                for(int i = 0; i < stringsLeft.length; i++){
                    ((TrackListModel)rightList.getModel()).add(stringsLeft[i]);
                }
                ((TrackListModel)leftList.getModel()).removeAll();
                leftList.updateUI();
                rightList.updateUI();
                leftList.clearSelection();
                rightList.clearSelection();
            }
            public void mousePressed(MouseEvent e) {}
            public void mouseReleased(MouseEvent e) {}
            public void mouseEntered(MouseEvent e) {}
            public void mouseExited(MouseEvent e) {}
        });
    }

    private void createAllLeft(){
        allLeft = new JButton("<<");
        allLeft.setToolTipText("Remove all from selected");
        if(!this.multiple) allLeft.setEnabled(false);
        allLeft.addMouseListener(new MouseListener() {
            public void mouseClicked(MouseEvent e) {
                String[] stringsRight = ((TrackListModel)rightList.getModel()).getAll();
                for(int i = 0; i < stringsRight.length; i++){
                    ((TrackListModel)leftList.getModel()).add(stringsRight[i]);
                }
                ((TrackListModel)rightList.getModel()).removeAll();
                leftList.updateUI();
                rightList.updateUI();
                leftList.clearSelection();
                rightList.clearSelection();
            }
            public void mousePressed(MouseEvent e) {}
            public void mouseReleased(MouseEvent e) {}
            public void mouseEntered(MouseEvent e) {}
            public void mouseExited(MouseEvent e) {}
        });
    }

    private void init() {
        createMoveRight();
        createMoveLeft();
        createAllLeft();
        createAllRight();
        createOkButton();
        createCancelButton();
        createFilter();
        initLayout();
    }

    private void initLists() {
        

        leftList.setModel(new TrackListModel());
        rightList.setModel(new TrackListModel());

        List<Frame> frames = FrameController.getInstance().getFrames();
        String[] tracks = new String[frames.size()];
        List<DataFormat> fileFormats = new ArrayList<DataFormat>();
        for(int i = 0; i <frames.size(); i++){
            //tracks[i] = frames.get(i).getTracks().get(0).getName();
            tracks[i] = frames.get(i).getName();
            DataFormat ff = frames.get(i).getTracks().get(0).getDataType();
            if(!fileFormats.contains(ff)) fileFormats.add(ff);
        }
        filterCombo.addItem("ALL");
        for(int i = 0; i < fileFormats.size(); i++){
            filterCombo.addItem(fileFormats.get(i));
        }
        filterCombo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                filter();
            }
        });

        ((TrackListModel)leftList.getModel()).init(tracks);
    }

    private class TrackListModel extends DefaultListModel{
        String[] strings = {};
        public int getSize() { return strings.length; }
        public Object getElementAt(int i) { return strings[i]; }

        public void init(String[] strings1){
            strings = strings1;
        }

        public void add(String s){
            String[] strings1 = new String[strings.length+1];
            for(int i = 0; i < strings.length; i++) strings1[i] = strings[i];
            strings1[strings.length] = s;
            strings = strings1;
        }

        public void removeIndex(int i){
            if(strings.length <= i || i < 0) return;
            String[] strings1 = new String[strings.length-1];
            int k = 0;
            for(int j = 0; j < strings.length; j++){
                if(j!=i){
                    strings1[k] = strings[j];
                    k++;
                }
            }
            strings = strings1;
        }

        public void removeIndices(int[] indices){
            String[] strings1 = new String[strings.length - indices.length];
            int[] strings2 = new int[strings.length];
            for(int i = 0; i < strings.length; i++) strings2[i] = 1;
            for(int i = 0; i < indices.length; i++) strings2[indices[i]] = 0;
            int j = 0;
            for(int i = 0; i < strings.length; i++){
                if(strings2[i] == 1){
                    strings1[j] = strings[i];
                    j++;
                }
            }
            strings = strings1;
        }

        public String[] getAll(){
            return strings;
        }

        public void removeAll(){
            strings = new String[0];
        }

    }

}
