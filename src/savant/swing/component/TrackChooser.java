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
package savant.swing.component;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;

import savant.controller.FrameController;
import savant.file.DataFormat;
import savant.settings.PersistentSettings;
import savant.util.MiscUtils;
import savant.view.swing.Frame;


/**
 *
 * @author AndrewBrook
 */
public class TrackChooser extends JDialog {

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
    private JCheckBox autoSelectAllCheck;
    private boolean selectBase;
    private JTextField selectBaseField;
    private int baseSelected = -1;

    public TrackChooser(JFrame parent, boolean multiple, String title){
        this(parent, multiple, title, false, -1);
    }

    public TrackChooser(JFrame parent, boolean multiple, String title, boolean selectBase){
        this(parent, multiple, title, selectBase, -1);
    }

    public TrackChooser(JFrame parent, boolean multiple, String title, boolean selectBase, int defaultBase){
        super(parent, true);

        this.multiple = multiple;
        this.selectBase = selectBase;
        this.setTitle(title);

        init();
        initLists();
        if(selectBase && defaultBase != -1){
            selectBaseField.setText(MiscUtils.numToString(defaultBase));
        }
        if(this.getAutoSelect()) selectAll();
        this.setLocationRelativeTo(null);
    }

    public String[] getSelectedTracks(){
        return retVal;
    }

    public int getBaseSelected(){
        return baseSelected;
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

        DataFormat ff = MiscUtils.dataFormatFromString((String)filterCombo.getSelectedItem());

        //if(filterCombo.getSelectedItem().equals("All")){
        if(ff == null){
            leftList.updateUI();
            leftList.clearSelection();
            return;
        }
    
        String[] leftTracks = ((TrackListModel)leftList.getModel()).getAll();       
        List<Frame> frames = FrameController.getInstance().getFrames();

        String[] removed = new String[leftTracks.length];
        int[] remove = new int[leftTracks.length];
        int count = 0;
        for(int i = 0; i < leftTracks.length; i++){
            String current = leftTracks[i];
            for(int j = 0; j <frames.size(); j++){
                if(frames.get(j).getName().equals(current)){
                    if(!frames.get(j).getTracks()[0].getDataSource().getDataFormat().equals(ff)){
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
        filler1.setPreferredSize(new Dimension(10,10));
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
        c.anchor = GridBagConstraints.CENTER;
        this.add(leftLabel, c);

        //RIGHT LABEL
        rightLabel = new JLabel("Selected Tracks");
        rightLabel.setFont(new Font(null, Font.BOLD, 12));
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridx = 3;
        c.gridy = 1;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.CENTER;
        this.add(rightLabel, c);

        //LEFT LIST
        leftList = new JList();
        leftScroll = new JScrollPane();
        leftScroll.setViewportView(leftList);
        leftScroll.setMinimumSize(new Dimension(450,300));
        leftScroll.setPreferredSize(new Dimension(450,300));
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
        rightScroll.setMinimumSize(new Dimension(450,300));
        rightScroll.setPreferredSize(new Dimension(450,300));
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
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 5;
        c.gridy = 7;
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

        //AUTO SELECT ALL
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridx = 3;
        c.gridy = 6;
        c.gridheight = 1;
        c.gridwidth = 2;
        this.add(autoSelectAllCheck, c);

        //SEPARATOR
        JPanel sepPanel1 = new JPanel();
        sepPanel1.setMinimumSize(new Dimension(5, 10));
        sepPanel1.setBackground(Color.red);
        sepPanel1.setLayout(new BorderLayout());
        sepPanel1.add(new JSeparator(SwingConstants.HORIZONTAL), BorderLayout.CENTER);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridx = 1;
        c.gridy = 7;
        c.gridheight = 1;
        c.gridwidth = 5;
        this.add(sepPanel1, c);

        int y = 8;
        if(selectBase){

            //SELECT BASE PANEL
            JPanel selectBasePanel = new JPanel();
            selectBasePanel.setMinimumSize(new Dimension(40, 10));
            selectBasePanel.setLayout(new BorderLayout());
            c.weightx = 1.0;
            c.weighty = 1.0;
            c.gridx = 1;
            c.gridy = y;
            c.gridheight = 1;
            c.gridwidth = 2;
            this.add(selectBasePanel, c);

            //SELECT BASE LABEL
            JLabel selectBaseLabel = new JLabel("(Optional) Select Base: ");
            selectBasePanel.add(selectBaseLabel, BorderLayout.WEST);

            //SELECT BASE FIELD
            selectBaseField = new JTextField();
            selectBasePanel.add(selectBaseField, BorderLayout.CENTER);

            //FILLER
            JPanel selectBaseFiller = new JPanel();
            selectBaseFiller.setSize(new Dimension(10,10));
            selectBaseFiller.setPreferredSize(new Dimension(10,10));
            selectBasePanel.add(selectBaseFiller, BorderLayout.SOUTH);
            y++;

            //SEPARATOR
            JPanel sepPanel2 = new JPanel();
            sepPanel2.setMinimumSize(new Dimension(10, 10));
            sepPanel2.setBackground(Color.red);
            sepPanel2.setLayout(new BorderLayout());
            sepPanel2.add(new JSeparator(SwingConstants.HORIZONTAL), BorderLayout.CENTER);
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1.0;
            c.weighty = 1.0;
            c.gridx = 1;
            c.gridy = y;
            c.gridheight = 1;
            c.gridwidth = 5;
            this.add(sepPanel2, c);
            y++;

            //FILLER
            JPanel filler5 = new JPanel();
            filler5.setSize(10, 10);
            filler5.setPreferredSize(new Dimension(10,10));
            c.weightx = 0;
            c.weighty = 0;
            c.gridx = 0;
            c.gridy = y;
            c.gridheight = 1;
            c.gridwidth = 1;
            this.add(filler5, c);
            y++;
        }

        //OK
        c.fill = GridBagConstraints.NONE;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridx = 3;
        c.gridy = y;
        c.gridheight = 1;
        c.gridwidth = 1;
        this.add(okButton, c);

        //CANCEL
        c.weightx = 1.0;
        c.weighty = 0.5;
        c.gridx = 4;
        c.gridy = y;
        c.gridheight = 1;
        c.gridwidth = 1;
        this.add(cancelButton, c);
        y++;

        //FILLER
        JPanel filler4 = new JPanel();
        filler4.setSize(10, 10);
        filler4.setPreferredSize(new Dimension(10,10));
        c.weightx = 0;
        c.weighty = 0;
        c.gridx = 0;
        c.gridy = y;
        c.gridheight = 1;
        c.gridwidth = 1;
        this.add(filler4, c);

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
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                retVal = ((TrackListModel)rightList.getModel()).getAll();
                if(autoSelectAllCheck.isSelected() != getAutoSelect()){
                    setAutoSelect(autoSelectAllCheck.isSelected());
                    try {
                        PersistentSettings.getInstance().store();
                    } catch (IOException ex) {
                        Logger.getLogger(TrackChooser.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                if(selectBase){
                    try {
                        baseSelected = Integer.parseInt(selectBaseField.getText().replaceAll(",", ""));
                    } catch (NumberFormatException ex){
                        baseSelected = -1;
                    }
                }
                dispose();
            }
        });    
    }

    private void createCancelButton(){
        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                retVal = null;
                dispose();
            }
        });
    }

    private boolean getAutoSelect(){
        return PersistentSettings.getInstance().getBoolean("TRACK_CHOOSER_AUTO_SELECT", false);
    }

    private void setAutoSelect(boolean value){
        PersistentSettings.getInstance().setBoolean("TRACK_CHOOSER_AUTO_SELECT", value);
    }

    private void createMoveRight(){
        moveRight = new JButton(">");
        moveRight.setToolTipText("Add item(s) to selected");
        moveRight.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
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
        });
    }

    private void createMoveLeft(){
        moveLeft = new JButton("<");
        moveLeft.setToolTipText("Remove item(s) from selected");
        moveLeft.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
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
        });
    }

    private void createAllRight(){
        allRight = new JButton(">>");
        allRight.setToolTipText("Add all to selected");
        if(!this.multiple) allRight.setEnabled(false);
        allRight.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectAll();               
            }
        });
    }

    private void createAllLeft(){
        allLeft = new JButton("<<");
        allLeft.setToolTipText("Remove all from selected");
        if(!this.multiple) allLeft.setEnabled(false);
        allLeft.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
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
        });
    }

    private void createSelectAllCheck(){
        autoSelectAllCheck = new JCheckBox("Always select all");
        autoSelectAllCheck.setSelected(getAutoSelect());
    }

    private void createSelectBase(){
        
    }

    private void init() {
        createMoveRight();
        createMoveLeft();
        createAllLeft();
        createAllRight();
        createOkButton();
        createCancelButton();
        createFilter();
        createSelectAllCheck();
        if(selectBase) createSelectBase();
        initLayout();
    }

    private void initLists() {
        

        leftList.setModel(new TrackListModel());
        rightList.setModel(new TrackListModel());

        List<Frame> frames = FrameController.getInstance().getFrames();
        String[] trackNames = new String[frames.size()];
        List<DataFormat> fileFormats = new ArrayList<DataFormat>();
        for(int i = 0; i <frames.size(); i++){
            //tracks[i] = frames.get(i).getTracks().get(0).getName();
            trackNames[i] = frames.get(i).getName();
            DataFormat ff = frames.get(i).getTracks()[0].getDataSource().getDataFormat();
            if(!fileFormats.contains(ff)) fileFormats.add(ff);
        }
        filterCombo.addItem("All");
        for(int i = 0; i < fileFormats.size(); i++){
            filterCombo.addItem(MiscUtils.dataFormatToString(fileFormats.get(i)));
        }
        filterCombo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                filter();
            }
        });

        ((TrackListModel)leftList.getModel()).init(trackNames);
    }

    private void selectAll(){
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

    private class TrackListModel extends DefaultListModel {
        String[] strings = {};

        @Override
        public int getSize() {
            return strings.length;
        }

        @Override
        public Object getElementAt(int i) {
            return strings[i];
        }

        public void init(String[] strings1){
            strings = strings1;
        }

        public void add(String s){
            String[] strings1 = new String[strings.length+1];
            System.arraycopy(strings, 0, strings1, 0, strings.length);
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
