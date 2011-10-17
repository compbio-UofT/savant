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
package savant.data.event;

/**
 * Classes can implement this interface if they are interested in monitoring data
 * coming from a DataSource.
 *
 * @author tarkvara
 */
public interface DataRetrievalListener {

    /**
     * A DataSource has started retrieving data.
     *
     * @param evt describes the data source which triggered the event
     */
    public void dataRetrievalStarted(DataRetrievalEvent evt);


    /**
     * A DataSource has successfully completed retrieving data.
     *
     * @param evt describes the data source which triggered the event
     */
    public void dataRetrievalCompleted(DataRetrievalEvent evt);


    /**
     * Data retrieval has failed for some reason.
     *
     * @param evt describes the data source which triggered the event
     */
    public void dataRetrievalFailed(DataRetrievalEvent evt);
}
