package savant.plugin;/*
 *    Copyright 2010 University of Toronto
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

/*
 * savant.plugin.AuxData.java
 * Created on Feb 23, 2010
 */


import java.io.PrintStream;
import javax.swing.*;
import savant.controller.event.thread.ThreadActivityChangedEvent;
import savant.view.tools.ToolRunInformation;
import savant.controller.ThreadController;

public abstract class Tool implements Runnable {

    private JTextArea outputTextArea;

    public abstract ToolInformation getToolInformation();

    public abstract JComponent getCanvas();

    private ToolRunInformation runInformation;

    @Override
    public void run() {
        runInformation = ThreadController.getInstance().getRunInformationForThread(Thread.currentThread());

        JProgressBar tb = runInformation.getProgressBar();
        runInformation.setStartTimeAsNow();
        runInformation.setTerminationStatus(ToolRunInformation.TerminationStatus.INCOMPLETE);
        try {
            runTool();
            runInformation.setEndTimeAsNow();
            tb.setValue(100);
            tb.setString("complete");
            runInformation.setTerminationStatus(ToolRunInformation.TerminationStatus.COMPLETE);
        } catch (InterruptedException e) {
            tb.setString("cancelled");
            runInformation.setTerminationStatus(ToolRunInformation.TerminationStatus.INTERRUPT);
        } finally {
            runInformation.setEndTimeAsNow();
            if (tb.isIndeterminate()) { tb.setIndeterminate(false); }
            switch(runInformation.getTerminationStatus()) {
                case COMPLETE:
                    ThreadController.getInstance().fireThreadActivityChangedEvent(new ThreadActivityChangedEvent(this, Thread.currentThread(), ThreadActivityChangedEvent.Activity.COMPLETE));
                    break;
                case INTERRUPT:
                    ThreadController.getInstance().fireThreadActivityChangedEvent(new ThreadActivityChangedEvent(this, Thread.currentThread(), ThreadActivityChangedEvent.Activity.INTERRUPT));
                    break;
                default:
                    break;
            }
        }
    }

    public ToolRunInformation getRunInformation() { return runInformation; }

    public PrintStream getOutputStream() {
        if (runInformation == null) { return System.out; }
        else { return runInformation.getOutputStream(); }
    }

    public void terminateIfInterruped() throws InterruptedException {
        if (Thread.currentThread().interrupted()) {
            ThreadController.getInstance().killThread(Thread.currentThread());
            throw new InterruptedException();
        }

    }

    public abstract void runTool() throws InterruptedException;

}
