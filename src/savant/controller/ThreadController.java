/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import savant.controller.event.thread.ThreadActivityChangedEvent;
import savant.controller.event.thread.ThreadActivityChangedListener;
import savant.view.tools.ToolRunInformation;

/**
 *
 * @author Marc Fiume
 */
public class ThreadController {

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
        instance = this;
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
        for (ThreadActivityChangedListener l : threadActivityListeners) {
            l.threadActivityChangedReceived(evt);
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
