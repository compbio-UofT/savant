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

package savant.controller;

import java.util.ArrayList;
import java.util.List;

import savant.util.MiscUtils;


/**
 * Generic controller class which provides functionality which can be used by other
 * controllers.
 *
 * @author tarkvara
 */
public abstract class Controller<E> {
    protected List<Listener<E>> listeners = new ArrayList<Listener<E>>();

    /**
     * Fire the specified event to all our listeners.
     */
    protected void fireEvent(final E event) {
        for (final Listener l: listeners) {
            l.handleEvent(event);
        }
    }

    public void addListener(Listener<E> l) {
        listeners.add(l);
    }


    public void removeListener(Listener<E> l) {
        listeners.remove(l);
    }
}
