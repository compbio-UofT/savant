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

package savant.util;

import java.util.concurrent.ExecutionException;
import javax.swing.SwingWorker;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.util.DialogUtils;

/**
 * SwingWorker wrapper which provides hooks do the right thing in response to errors, cancellation, etc.
 *
 * @author tarkvara
 */
public abstract class BackgroundWorker<T> extends SwingWorker<T, Object> {
    private static final Log LOG = LogFactory.getLog(BackgroundWorker.class);

    @Override
    public void done() {
        showProgress(1.0);
        if (!isCancelled()) {
            try {
                showSuccess(get());
            } catch (InterruptedException x) {
                showFailure(x);
            } catch (ExecutionException x) {
                showFailure(x.getCause());
            }
        }
    }
    

    /**
     * Show progress during a lengthy operation.  As a special case, pass 1.0 to remove the progress display.
     * @param fraction the fraction completed (1.0 to indicate full completion; -1.0 as special flag to indicate indeterminate progress-bar).
     */
    protected abstract void showProgress(double fraction);
    
    /**
     * Called when the worker has successfully completed its task.
     * @param result the value returned by <code>doInBackground()</code>.
     */
    protected abstract void showSuccess(T result);
    
    /**
     * Called when the task has thrown an exception.  Default behaviour is to log the exception
     * and put up a dialog box.
     */
    protected void showFailure(Throwable t) {
        if (t instanceof InterruptedException) {
            DialogUtils.displayMessage("Background task interrupted.");
        } else {
            LOG.error("Exception thrown by background task.", t);
            DialogUtils.displayException("Savant", String.format("<html>Exception thrown by background task:<br><br><i>%s</i></html>", MiscUtils.getMessage(t)), t);
        }
    }
}
