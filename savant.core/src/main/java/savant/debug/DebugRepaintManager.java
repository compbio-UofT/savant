/**
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
