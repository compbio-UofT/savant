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

package savant.sql;

import javax.swing.SwingWorker;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.util.DialogUtils;

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
    private boolean workerDone = false;
    private final Object waiter = new Object();

    public SQLWorker(final String progress, final String failure) {
        new SwingWorker() {
            private T result;

            @Override
            public Object doInBackground() {
                try {
                    result = SQLWorker.this.doInBackground();
                } catch (Exception x) {
                    DialogUtils.showProgress(null, 1.0);
                    DialogUtils.displayException("SQL Error", failure, x);
                }
                return null;
            }

            @Override
            public void done() {
                try {
                    synchronized(waiter) {
                        workerDone = true;
                        waiter.notifyAll();
                    }
                    DialogUtils.showProgress(null, 1.0);
                    SQLWorker.this.done(result);
                } catch (Exception x) {
                    DialogUtils.displayException("SQL Error", failure, x);
                }
            }
        }.execute();

        synchronized (waiter) {
            try {
                waiter.wait(PROGRESS_DELAY);
                if (!workerDone) {
                    DialogUtils.showProgress(progress, -1.0);
                }
            } catch (InterruptedException ignored) {
            }
        }
    }

    public abstract T doInBackground() throws Exception;

    public abstract void done(T value) throws Exception;
}
