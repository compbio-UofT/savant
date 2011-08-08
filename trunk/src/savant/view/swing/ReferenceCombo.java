/*
 *    Copyright 2011 University of Toronto
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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Set;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import savant.controller.GenomeController;
import savant.controller.Listener;

import savant.controller.LocationController;
import savant.controller.event.GenomeChangedEvent;
import savant.controller.event.LocationChangedEvent;
import savant.controller.event.LocationChangedListener;
import savant.util.MiscUtils;


/**
 *
 * @author tarkvara
 */
public class ReferenceCombo extends JComboBox {
    private LocationController locationController;

    /**
     * Constructor just initialises combo's contents from ReferenceController.
     */
    public ReferenceCombo() {
        locationController = LocationController.getInstance();
        setToolTipText("Reference sequence");
        addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                // the first item is a header and not allowed to be selected
                if (getItemCount() <= 1) {
                    return;
                }

                int index = getSelectedIndex();
                String ref = (String)getItemAt(index);
                if (ref.contains("[")) {
                    int size = getItemCount();
                    for (int i = 1; i < size; i++) {
                        int newindex = (index + i) % size;
                        String newref = (String)getItemAt(newindex);
                        if (!((String) getItemAt(newindex)).contains("[")) {
                            index = newindex;
                            setSelectedIndex(index);
                            break;
                        }
                    }
                }
                switchReference(index);
            }

            private void switchReference(int index) {

                String ref = (String)getItemAt(index);

                if (!locationController.getReferenceNames().contains(ref)) {
                    if (!Savant.showNonGenomicReferenceDialog) {
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
                        Savant.showNonGenomicReferenceDialog = false;
                    } else if (n == 0) {
                        return;
                    }
                }

                locationController.setLocation(ref);
            }
        });

        locationController.addLocationChangedListener(new LocationChangedListener() {
            @Override
            public void locationChanged(LocationChangedEvent event) {
                if(event.isNewReference())
                    setSelectedItem(event.getReference());
            }

        });

        GenomeController.getInstance().addListener(new Listener<GenomeChangedEvent>() {
            @Override
            public void handleEvent(GenomeChangedEvent event) {
                String curRef = (String)getSelectedItem();
                removeAllItems();

                int maxWidth = 0;
                Set<String> refNames = event.getNewGenome().getReferenceNames();
                for (String s : refNames) {
                    maxWidth = Math.max(maxWidth, s.length());
                    addItem(s);
                }
                maxWidth = Math.max(200, maxWidth * 8 + 20);
                Dimension dim = new Dimension(maxWidth, 23);
                setPreferredSize(dim);
                setMinimumSize(dim);
                setMaximumSize(dim);
                invalidate();

                List<String> nonGenomicRefs = MiscUtils.set2List(locationController.getNonGenomicReferenceNames());
                if (nonGenomicRefs.size() > 0) {
                    for (String s : nonGenomicRefs) {
                        addItem(s);
                    }
                }

                if (curRef != null) {
                    setSelectedItem(curRef);
                }
            }
        });
    }
}
