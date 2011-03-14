/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.sql;

import java.awt.Window;
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

    public SQLWorker(Window parent, String progress, final String failure) {
        new SwingWorker() {
            private T result;

            @Override
            public Object doInBackground() {
                try {
                    result = SQLWorker.this.doInBackground();
                } catch (Exception x) {
                    LOG.error(x);
                    DialogUtils.showProgress(null, null, 1.0);
                    DialogUtils.displayException("SQL Error", failure, x);
                }
                return null;
            }

            @Override
            public void done() {
                try {
                    DialogUtils.showProgress(null, null, 1.0);
                    SQLWorker.this.done(result);
                } catch (Exception x) {
                    LOG.error(x);
                    DialogUtils.displayException("SQL Error", failure, x);
                }
            }
        }.execute();
        DialogUtils.showProgress(parent, progress, -1.0);
    }

    public abstract T doInBackground() throws Exception;

    public abstract void done(T value) throws Exception;
}
