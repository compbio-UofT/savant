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

package savant.debug;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Rectangle;
import javax.swing.JComponent;
import javax.swing.JProgressBar;
import javax.swing.RepaintManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Utility class to help debug cases where a component is receiving unexpected repaints.
 * Use RepaintManager.setCurrentManager() to install this.  It generates <b>a lot</b> of output.
 *
 * @author tarkvara
 */
public class DebugRepaintManager extends RepaintManager {
    private static final Log LOG = LogFactory.getLog(DebugRepaintManager.class);

    RepaintManager realManager;

    public DebugRepaintManager() {
        realManager = RepaintManager.currentManager(null);
    }

    @Override
    public void addDirtyRegion(JComponent c, int x, int y, int w, int h) {
        if (w > 0 && h > 0 && !(c instanceof JProgressBar)) {
            LOG.info("RepaintManager.addDirtyRegion(" + c + ", " + new Rectangle(x, y, w, h) + ")");
        }
        realManager.addDirtyRegion(c, x, y, w, h);
    }

    @Override
    public void addInvalidComponent(JComponent invalidComponent) {
        LOG.info("RepaintManager.addInvalidComponent(" + invalidComponent + ")");
        realManager.addInvalidComponent(invalidComponent);
    }

    @Override
    public Rectangle getDirtyRegion(JComponent aComponent) {
        return realManager.getDirtyRegion(aComponent);
    }

    @Override
    public Dimension getDoubleBufferMaximumSize() {
        return realManager.getDoubleBufferMaximumSize();
    }

    @Override
    public Image getOffscreenBuffer(Component c, int proposedWidth, int proposedHeight) {
        return realManager.getOffscreenBuffer(c, proposedWidth, proposedHeight);
    }

    @Override
    public Image getVolatileOffscreenBuffer(Component c, int proposedWidth, int proposedHeight) {
        return realManager.getVolatileOffscreenBuffer(c, proposedWidth, proposedHeight);
    }

    @Override
    public boolean isCompletelyDirty(JComponent aComponent) {
        return realManager.isCompletelyDirty(aComponent);
    }

    @Override
    public boolean isDoubleBufferingEnabled() {
        return realManager.isDoubleBufferingEnabled();
    }

    @Override
    public void markCompletelyClean(JComponent aComponent) {
        realManager.markCompletelyClean(aComponent);
    }

    @Override
    public void markCompletelyDirty(JComponent aComponent) {
        realManager.markCompletelyDirty(aComponent);
    }

    @Override
    public void paintDirtyRegions() {
        realManager.paintDirtyRegions();
    }

    @Override
    public void removeInvalidComponent(JComponent component) {
        realManager.removeInvalidComponent(component);
    }

    @Override
    public void setDoubleBufferingEnabled(boolean aFlag) {
        realManager.setDoubleBufferingEnabled(aFlag);
    }

    @Override
    public void setDoubleBufferMaximumSize(Dimension d) {
        realManager.setDoubleBufferMaximumSize(d);
    }

    @Override
    public void validateInvalidComponents() {
        realManager.validateInvalidComponents();
    }
}
