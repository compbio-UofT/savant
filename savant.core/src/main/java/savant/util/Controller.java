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
package savant.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.util.Listener;


/**
 * Generic controller class which provides functionality which can be used by other
 * controllers.
 *
 * @author tarkvara
 */
public abstract class Controller<E> {
    private static final Log LOG = LogFactory.getLog(Controller.class);

    protected List<Listener<E>> listeners = new ArrayList<Listener<E>>();
    private Stack<List<Listener<E>>> listenersToAdd = new Stack<List<Listener<E>>>();
    private Stack<List<Listener<E>>> listenersToRemove = new Stack<List<Listener<E>>>();

    /**
     * Fire the specified event to all our listeners.
     */
    public synchronized void fireEvent(E event) {
        listenersToAdd.push(new ArrayList<Listener<E>>());
        listenersToRemove.push(new ArrayList<Listener<E>>());
        for (final Listener l: listeners) {
            try {
                l.handleEvent(event);
            } catch (Throwable x) {
                LOG.warn(l + " threw exception while handling event.", x);
            }
        }
        for (Listener<E> l: listenersToAdd.pop()) {
            listeners.add(l);
        }
        for (Listener<E> l: listenersToRemove.pop()) {
            listeners.remove(l);
        }
    }

    public void addListener(Listener<E> l) {
        if (listenersToAdd.isEmpty()) {
            // Not in a loop, so add the listener immediately.
            listeners.add(l);
        } else {
            // Currently enumerating, so delay the add until the loop is done.
            listenersToAdd.peek().add(l);
        }
    }

    public void removeListener(Listener<E> l) {
        if (listenersToRemove.isEmpty()) {
            // Not in a loop, so remove the listener immediately.
            listeners.remove(l);
        } else {
            // Currently enumerating, so delay the removal until the loop is done.
            listenersToRemove.peek().add(l);
        }
    }
}
