/*
 *    Copyright 2009-2010 University of Toronto
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
package savant.controller.event.thread;

import java.util.EventObject;

/**
 *
 * @author mfiume
 */
public class ThreadActivityChangedEvent extends EventObject {

    public enum Activity { START, COMPLETE, INTERRUPT };
    private Thread t;
    private Activity a;

    public ThreadActivityChangedEvent(Object source, Thread t, Activity a) {
        super(source);
        this.t = t;
        this.a = a;
    }

    public Thread getThread() {
        return t;
    }

    public Activity getActivity() {
        return a;
    }
}