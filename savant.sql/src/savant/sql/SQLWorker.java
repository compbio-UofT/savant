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
public abstract class SQLWorker {
    private static final Log LOG = LogFactory.getLog(SQLWorker.class);

    public SQLWorker(Window parent, String progress, final String failure) {
        new SwingWorker() {
            @Override
            public Object doInBackground() {
                try {
                    SQLWorker.this.doInBackground();
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
                    SQLWorker.this.done();
                } catch (Exception x) {
                    LOG.error(x);
                    DialogUtils.displayException("SQL Error", failure, x);
                }
            }
        }.execute();
        DialogUtils.showProgress(parent, progress, -1.0);
    }

    public abstract void doInBackground() throws Exception;

    public abstract void done() throws Exception;
}
