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
