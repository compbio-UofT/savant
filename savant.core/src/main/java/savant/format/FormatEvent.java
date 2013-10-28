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
package savant.format;

import java.io.File;
import savant.util.DownloadEvent;

/**
 * Functionally identical to a DownloadEvent, but we give it a different name for clarity's sake.
 *
 * @author tarkvara
 */
public class FormatEvent extends DownloadEvent {
    private String subTask;

    /**
     * A format event indicating that the process has started.
     */
    public FormatEvent(Type type) {
        super(type);
    }

    /**
     * A format event represent progress towards our goal.
     *
     * @param progress a value from 0.0 to 1.0 indicating the amount of progress completed
     * @param task the current subtask being executed
     */
    public FormatEvent(double progress, String task) {
        super(progress);
        subTask = task;
    }

    /**
     * A format event representing successful completion of the download.
     *
     * @param file the destination file
     */
    public FormatEvent(File file) {
        super(file);
    }

    /**
     * A format event indicating that the formatting process has failed.
     *
     * @param error the exception which caused the download to fail
     */
    public FormatEvent(Throwable error) {
        super(error);
    }
    
    public String getSubTask() {
        return subTask;
    }
}
