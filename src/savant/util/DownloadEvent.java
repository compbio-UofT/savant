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

package savant.util;

import java.io.File;

/**
 * Event which is sent by asynchronous downloads.
 *
 * @author tarkvara
 */
public class DownloadEvent {
    public enum Type {
        STARTED,
        COMPLETED,
        FAILED,
        PROGRESS
    }

    final Type type;
    final double progress;
    final File file;
    final Throwable error;

    private DownloadEvent(Type type, double progress, File file, Throwable error) {
        this.type = type;
        this.progress = progress;
        this.file = file;
        this.error = error;
    }

    /**
     * A download event indicating that the process has started.
     */
    public DownloadEvent(Type type) {
        this(type, Double.NaN, null, null);
    }

    /**
     * A download event represent progress towards our goal.
     *
     * @param progress a value from 0.0 to 1.0 indicating the amount of progress completed
     */
    public DownloadEvent(double progress) {
        this(Type.PROGRESS, progress, null, null);
    }

    /**
     * A download event representing successful completion of the download.
     *
     * @param file the destination file
     */
    public DownloadEvent(File file) {
        this(Type.COMPLETED, Double.NaN, file, null);
    }

    /**
     * A download event indicating that the download has failed.
     *
     * @param error the exception which caused the download to fail
     */
    public DownloadEvent(Throwable error) {
        this(Type.FAILED, Double.NaN, null, error);
    }

    public Type getType() {
        return type;
    }

    public double getProgress() {
        return progress;
    }

    public File getFile() {
        return file;
    }

    public Throwable getError() {
        return error;
    }
}
