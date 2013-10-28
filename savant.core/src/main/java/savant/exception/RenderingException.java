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
package savant.exception;

/**
 * Exception class which indicates that a renderer was unable to render the given
 * track.
 *
 * @author tarkvara
 */
public class RenderingException extends Exception {
    public static final int LOWEST_PRIORITY = 0;
    public static final int INFO_PRIORITY = 1;
    public static final int WARNING_PRIORITY = 2;
    public static final int ERROR_PRIORITY = 3;

    private final int priority;

    /**
     * Construct a new RenderingException.
     *
     * @param message the error message to be rendered
     * @param priority higher-priority messages will override lower-priority ones.
     */
    public RenderingException(String message, int priority) {
        super(message);
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }
}
