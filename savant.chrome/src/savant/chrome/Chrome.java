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
package savant.chrome;

import java.awt.BorderLayout;
import java.io.File;
import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.biojava.bio.chromatogram.Chromatogram;
import org.biojava.bio.chromatogram.ChromatogramFactory;
import org.biojava.bio.chromatogram.UnsupportedChromatogramFormatException;

import savant.api.util.DialogUtils;
import savant.api.util.GenomeUtils;
import savant.controller.event.LocationChangedEvent;
import savant.controller.event.LocationChangeCompletedListener;

import savant.plugin.SavantPanelPlugin;

public class Chrome extends SavantPanelPlugin implements LocationChangeCompletedListener {

    Chromatogram c = null;

    @Override
    public void init(JPanel tablePanel) {

        savant.api.util.NavigationUtils.addLocationChangeListener(this);
        JButton b = new JButton("Hello world");
        tablePanel.add(b);
        try {
            c = ChromatogramFactory.create(new File("/Users/zig/Downloads/91548-HEXBF.ab1"));
        } catch (IOException ex) {
            DialogUtils.displayException("Uh Oh..", "Error reading chromatogram", ex);
        } catch (Throwable ex) {
            DialogUtils.displayException("Uh Oh..", "Error reading chromatogram", ex);
        }
    }

    @Override
    public String getTitle() {
        return "Chrome";
    }

    JPanel canvas = null;

    @Override
    public void locationChangeCompleted(LocationChangedEvent event) {
        if (canvas == null) {
            if (GenomeUtils.isGenomeLoaded() && GenomeUtils.getGenome().isSequenceSet()) {
                canvas = GenomeUtils.getGenome().getSequenceTrack().getLayerCanvas();
                canvas.setLayout(new BorderLayout());
                canvas.add(new ChromePanel(c), BorderLayout.CENTER);
            }
        }

        if (canvas != null) {
            canvas.repaint();
            canvas.revalidate();
        }
    }
}
