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

import savant.controller.ReferenceController;
import savant.controller.event.GenomeChangedEvent;
import savant.controller.event.ReferenceChangedEvent;
import savant.controller.event.ReferenceChangedListener;
import savant.util.MiscUtils;


/**
 *
 * @author tarkvara
 */
public class ReferenceCombo extends JComboBox {
    private ReferenceController referenceController;

    /**
     * Constructor just initialises combo's contents from ReferenceController.
     */
    public ReferenceCombo() {
        referenceController = ReferenceController.getInstance();
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

                if (!referenceController.getReferenceNames().contains(ref)) {
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

                referenceController.setReference(ref);
            }
        });

        referenceController.addReferenceChangedListener(new ReferenceChangedListener() {
            @Override
            public void referenceChanged(ReferenceChangedEvent event) {
                setSelectedItem(event.getReference());
            }

            @Override
            public void genomeChanged(GenomeChangedEvent event) {
                String curRef = (String)getSelectedItem();
                removeAllItems();

                int maxWidth = 0;
                Set<String> refNames = referenceController.getGenome().getReferenceNames();
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

                //this.referenceDropdown.addItem("[ NON-GENOMIC (" + nongenomicrefnames.size() + ") ]");
                List<String> nonGenomicRefs = MiscUtils.set2List(referenceController.getNonGenomicReferenceNames());
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
