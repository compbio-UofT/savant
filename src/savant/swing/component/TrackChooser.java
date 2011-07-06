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
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
    private static final Log LOG = LogFactory.getLog(TrackChooser.class);

    private boolean multiple;
    private JList leftList;
    private JList rightList;
    private String[] retVal;
    private JComboBox filterCombo;
    private String[] filteredTracks = null;
    private JCheckBox autoSelectAllCheck;
    private boolean selectBase;
    private JTextField selectBaseField;
    private int baseSelected = -1;

    public TrackChooser(Window parent, boolean multiple, String title){
        this(parent, multiple, title, false, -1);
    }

    public TrackChooser(Window parent, boolean multiple, String title, boolean selectBase){
        this(parent, multiple, title, selectBase, -1);
    }

    public TrackChooser(Window parent, boolean multiple, String title, boolean selectBase, int defaultBase){
        super(parent, ModalityType.APPLICATION_MODAL);

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

        Object selected = filterCombo.getSelectedItem();
        DataFormat ff = selected instanceof DataFormat ? (DataFormat)selected : null;

        //if(filterCombo.getSelectedItem().equals("All")){
        if (ff == null){
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
        //LEFT LABEL
        JLabel leftLabel = new JLabel("All Tracks");
        leftLabel.setFont(new Font(null, Font.BOLD, 12));
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(5, 5, 5, 5);
        add(leftLabel, c);

        // RIGHT LABEL
        JLabel rightLabel = new JLabel("Selected Tracks");
        rightLabel.setFont(new Font(null, Font.BOLD, 12));
        c.gridx = 2;
        c.gridwidth = GridBagConstraints.REMAINDER;
        add(rightLabel, c);

        //LEFT LIST
        leftList = new JList();
        JScrollPane leftScroll = new JScrollPane();
        leftScroll.setViewportView(leftList);
        leftScroll.setMinimumSize(new Dimension(450,300));
        leftScroll.setPreferredSize(new Dimension(450,300));
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.gridheight = 4;
        add(leftScroll, c);

        //RIGHT LIST
        rightList = new JList();
        JScrollPane rightScroll = new JScrollPane();
        rightScroll.setViewportView(rightList);
        rightScroll.setMinimumSize(new Dimension(450,300));
        rightScroll.setPreferredSize(new Dimension(450,300));
        c.gridx = 2;
        c.gridwidth = GridBagConstraints.REMAINDER;
        this.add(rightScroll, c);

        // MOVE RIGHT
        c.weightx = 0.0;
        c.weighty = 0.5;
        c.gridx = 1;
        c.gridy = 1;
        c.gridwidth = 1;
        c.gridheight = 1;
        add(createMoveRight(), c);

        // MOVE LEFT
        c.gridy = 2;
        add(createMoveLeft(), c);

        // ALL RIGHT
        c.gridy = 3;
        add(createAllRight(), c);

        //ALL LEFT
        c.gridy = 4;
        this.add(createAllLeft(), c);

        //FILTER
        c.gridx = 0;
        c.gridy = 5;
        add(createFilterPanel(), c);

        //AUTO SELECT ALL
        c.gridx = 2;
        add(createSelectAllCheck(), c);

        //SEPARATOR
        JSeparator separator1 = new JSeparator(SwingConstants.HORIZONTAL);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = GridBagConstraints.RELATIVE;
        c.weightx = 1.0;
        c.gridwidth = GridBagConstraints.REMAINDER;
        add(separator1, c);

        if (selectBase) {

            //SELECT BASE PANEL
            JPanel selectBasePanel = new JPanel(new BorderLayout());
            c.gridwidth = 2;
            add(selectBasePanel, c);

            //SELECT BASE LABEL
            JLabel selectBaseLabel = new JLabel("(Optional) Select Base: ");
            selectBasePanel.add(selectBaseLabel, BorderLayout.WEST);

            //SELECT BASE FIELD
            selectBaseField = new JTextField();
            selectBasePanel.add(selectBaseField, BorderLayout.CENTER);

            //SEPARATOR
            JSeparator separator2 = new JSeparator(SwingConstants.HORIZONTAL);
            c.gridwidth = GridBagConstraints.REMAINDER;
            add(separator2, c);
        }

        JPanel okCancelPanel = new JPanel(new BorderLayout());
        c.anchor = GridBagConstraints.EAST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.NONE;
        add(okCancelPanel, c);

        //OK
        okCancelPanel.add(createOKButton(), BorderLayout.CENTER);

        //CANCEL
        okCancelPanel.add(createCancelButton(), BorderLayout.EAST);

        pack();
    }

    private JPanel createFilterPanel(){
        JPanel filterPanel = new JPanel();
        filterPanel.setLayout(new BorderLayout());

        JLabel filterLabel = new JLabel("Filter By: ");
        filterPanel.add(filterLabel, BorderLayout.WEST);

        filterCombo = new JComboBox();
        filterPanel.add(filterCombo, BorderLayout.EAST);
        return filterPanel;
    }

    private JButton createOKButton(){
        JButton okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                retVal = ((TrackListModel)rightList.getModel()).getAll();
                if(autoSelectAllCheck.isSelected() != getAutoSelect()){
                    setAutoSelect(autoSelectAllCheck.isSelected());
                    try {
                        PersistentSettings.getInstance().store();
                    } catch (IOException ex) {
                        LOG.error("Unable to store preferences.", ex);
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
        return okButton;
    }

    private JButton createCancelButton(){
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                retVal = null;
                dispose();
            }
        });
        return cancelButton;
    }

    private boolean getAutoSelect(){
        return PersistentSettings.getInstance().getBoolean("TRACK_CHOOSER_AUTO_SELECT", false);
    }

    private void setAutoSelect(boolean value){
        PersistentSettings.getInstance().setBoolean("TRACK_CHOOSER_AUTO_SELECT", value);
    }

    private JButton createMoveRight(){
        JButton moveRight = new JButton(">");
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
        return moveRight;
    }

    private JButton createMoveLeft(){
        JButton moveLeft = new JButton("<");
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
        return moveLeft;
    }

    private JButton  createAllRight(){
        JButton allRight = new JButton(">>");
        allRight.setToolTipText("Add all to selected");
        if(!this.multiple) allRight.setEnabled(false);
        allRight.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectAll();               
            }
        });
        return allRight;
    }

    private JButton createAllLeft(){
        JButton allLeft = new JButton("<<");
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
        return allLeft;
    }

    private JCheckBox createSelectAllCheck(){
        autoSelectAllCheck = new JCheckBox("Always select all");
        autoSelectAllCheck.setSelected(getAutoSelect());
        return autoSelectAllCheck;
    }

    private void init() {
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
            filterCombo.addItem(fileFormats.get(i));
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
