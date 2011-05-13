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

package savant.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.controller.event.ThreadActivityChangedEvent;
import savant.controller.event.ThreadActivityChangedListener;
import savant.view.tools.ToolRunInformation;

/**
 *
 * @author mfiume
 */
public class ThreadController {
    private static final Log LOG = LogFactory.getLog(ThreadController.class);

    static ThreadController instance;

    Map<String,String> thread2StartTimeMap = new HashMap<String,String>();
    Map<String,ToolRunInformation> threadExtrasMap = new HashMap<String,ToolRunInformation>();

    public static synchronized ThreadController getInstance() {
        if (instance == null) {
            instance = new ThreadController();
        }
        return instance;
    }

    /** Creates new ToolsThreadManager */
    private ThreadController() {
        threads = new ArrayList<InformativeThread>();
    }

    List<InformativeThread> threads;

    static int threadCount = 0;

    public InformativeThread runInNewThread(Runnable r, String name) {

        InformativeThread t = new InformativeThread(r, new ToolRunInformation());
        threadCount++;
        t.setName(threadCount + ". " + name);
        t.start();
        threads.add(t);
        return t;
    }

    public void killThread(String name) {
        Thread t = getThreadWithName(name);
        killThread(t);
    }

    public InformativeThread getThreadWithName(String name) {
        for (InformativeThread t : threads) {
            if (t.getName().equals(name)) {
                return t;
            }
        }
        return null;
    }

    public void killThread(Thread t) {
        if (t != null) {
            t.interrupt();
            fireThreadActivityChangedEvent(new ThreadActivityChangedEvent(this, t, ThreadActivityChangedEvent.Activity.INTERRUPT));
        }
    }

    public List<InformativeThread> getThreads() { return threads; }

    public List<InformativeThread> getCompletedThreads() {
        return getThreadsWithCompletionStatus(true);
    }

    public List<InformativeThread> getRunningThreads() {
        return getThreadsWithCompletionStatus(false);
    }

    private List<InformativeThread> getThreadsWithCompletionStatus(boolean b) {
        List<InformativeThread> result = new ArrayList<InformativeThread>();
        for (InformativeThread t : threads) {
            if (t.isAlive() == b) {
                result.add(t);
            }
        }
        return result;
    }

    List<ThreadActivityChangedListener> threadActivityListeners = new ArrayList<ThreadActivityChangedListener>();

    public void addThreadActivityListener(ThreadActivityChangedListener l) {
        threadActivityListeners.add(l);
    }

    public void removeThreadActivityListener(ThreadActivityChangedListener l) {
        threadActivityListeners.remove(l);
    }

    public void fireThreadActivityChangedEvent(ThreadActivityChangedEvent evt) {
        LOG.info("Firing notification of thread activity: " + evt.getActivity());
        for (ThreadActivityChangedListener l : threadActivityListeners) {
            l.threadActivityChanged(evt);
        }
    }

    //static Map<String,ToolRunInformation> thread2ExtrasMap = new HashMap<String,ToolRunInformation>();

    /*
    public static void registerExtras(Thread thread, ToolRunInformation extras) {
        thread2ExtrasMap.put(thread.getName(), extras);
    }
     *
     */

    public ToolRunInformation getRunInformationForThread(Thread t) {
        InformativeThread st = this.getThreadWithName(t.getName());
        if (st != null) { return st.getRunInformation(); }
        else { return null; }
    }
}
