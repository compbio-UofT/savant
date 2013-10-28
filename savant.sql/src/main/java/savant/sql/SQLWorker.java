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
package savant.sql;

import javax.swing.SwingWorker;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.util.DialogUtils;
import savant.util.MiscUtils;

/**
 * Like a SwingWorker, but with built-in error handling and progress dialog.
 *
 * @author tarkvara
 */
public abstract class SQLWorker<T> {
    private static final Log LOG = LogFactory.getLog(SQLWorker.class);

    /**
     * Delay in milliseconds before we put up a progress dialog.
     */
    private static final int PROGRESS_DELAY = 1000;

    private SwingWorker innerWorker;
    protected String progressMessage;
    protected String failureMessage;
    private boolean workerDone = false;
    private final Object waiter = new Object();

    public SQLWorker(String prog, String fail) {
        this.progressMessage = prog;
        this.failureMessage = fail;
        innerWorker = new SwingWorker() {
            private T result;

            @Override
            public Object doInBackground() {
                try {
                    result = SQLWorker.this.doInBackground();
                } catch (final Exception x) {
                    MiscUtils.invokeLaterIfNecessary(new Runnable() {
                        @Override
                        public void run() {
                            showProgress(1.0);
                            DialogUtils.displayException("SQL Error", failureMessage, x);
                        }
                    });
                }
                return null;
            }

            @Override
            public void done() {
                try {
                    synchronized (waiter) {
                        workerDone = true;
                        waiter.notifyAll();
                    }
                    showProgress(1.0);
                    SQLWorker.this.done(result);
                } catch (Exception x) {
                    DialogUtils.displayException("SQL Error", failureMessage, x);
                }
            }
        };
    }
    
    public void execute() {
        innerWorker.execute();
        synchronized (waiter) {
            try {
                waiter.wait(PROGRESS_DELAY);
            } catch (InterruptedException ignored) {
            }
        }
        if (!workerDone) {
            showProgress(-1.0);
        }
    }

    public abstract T doInBackground() throws Exception;

    /**
     * Perform whatever processing is appropriate upon successful completion.
     * Will be invoked on the AWT thread.
     *
     * @param value the progress value between 0.0 and 1.0 (-1.0 to indicate indefinite progress)
     */
    public abstract void done(T value) throws Exception;
    
    /**
     * Default behaviour is to use DialogUtils to indicate progress.  Other classes
     * can override this to show progress in a less obtrusive manner.  Will be invoked
     * on the AWT thread.
     *
     * @param value the progress value between 0.0 and 1.0 (-1.0 to indicate indefinite progress)
     */
    public void showProgress(double value) {
        DialogUtils.showProgress(progressMessage, value);
    }
}
