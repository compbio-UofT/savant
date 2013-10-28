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
package savant.controller.event;


/**
 * Something has changed in the state of our GraphPaneController.  Tell our clients about it.
 *
 * @author mfiume, tarkvara
 */
public class GraphPaneEvent {
    private final Type type;
    private final int mouseX;
    private final double mouseY;
    private final boolean yIntegral;
    private final String status;

    /**
     * Create an event representing a change in highlighting.
     */
    public GraphPaneEvent() {
        this.type = Type.HIGHLIGHTING;
        this.mouseX = -1;
        this.mouseY = Double.NaN;
        this.yIntegral = false;
        this.status = null;
    }

    public GraphPaneEvent(int mouseX, double mouseY, boolean yIntegral) {
        this.type = Type.MOUSE;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.yIntegral = yIntegral;
        this.status = null;
    }

    public GraphPaneEvent(String status) {
        this.type = Type.STATUS;
        this.mouseX = -1;
        this.mouseY = Double.NaN;
        this.yIntegral = false;
        this.status = status;
    }

    public Type getType() {
        return type;
    }

    public int getMouseX() {
        return mouseX;
    }

    public double getMouseY() {
        return mouseY;
    }

    public boolean isYIntegral() {
        return yIntegral;
    }

    public String getStatus() {
        return status;
    }

    public enum Type {
        HIGHLIGHTING,
        MOUSE,
        STATUS
    }
}