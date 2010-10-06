/*
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
 * FormatProgressListener.java
 * Created on Apr 22, 2010
 */

package savant.format;

/**
 * Interface to allow notification of formatting progress.
 */
public interface FormatProgressListener {

    /**
     * Called when a progress update is available.
     *
     * @param value - percentage value between 0 and 100.
     */
    public void taskProgressUpdate(Integer progress, String status);

    //public void overallProgressUpdate(int at, int total);
    //public void overallProgressUpdate(int at);
    public void incrementOverallProgress();
}
