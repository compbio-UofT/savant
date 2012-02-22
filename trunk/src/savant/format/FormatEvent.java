/*
 *    Copyright 2011-2012 University of Toronto
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
