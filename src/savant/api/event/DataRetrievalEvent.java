/*
 *    Copyright 2010-2011 University of Toronto
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

package savant.api.event;

import java.util.List;

import savant.api.adapter.TrackAdapter;
import savant.api.data.Record;


/**
 * Event class which allows asynchronous retrieval of data.
 *
 * @author tarkvara
 */
public class DataRetrievalEvent {
    public enum Type {
        STARTED,
        COMPLETED,
        FAILED
    };
    Type type;
    TrackAdapter track;
    List<Record> data;
    Throwable error;

    /**
     * Constructor for retrieval starting.
     */
    public DataRetrievalEvent(TrackAdapter t) {
        this.track = t;
        this.type = Type.STARTED;
    }

    /**
     * Constructor when data is successfully retrieved.
     * @param data the records retrieved
     */
    public DataRetrievalEvent(TrackAdapter t, List<Record> data) {
        this.track = t;
        this.type = Type.COMPLETED;
        this.data = data;
    }

    /**
     * Constructor when retrieval has failed.
     *
     * @param error
     */
    public DataRetrievalEvent(TrackAdapter t, Throwable error) {
        this.track = t;
        this.type = Type.FAILED;
        this.error = error;
    }

    public Type getType() {
        return type;
    }

    public TrackAdapter getTrack() {
        return track;
    }

    public List<Record> getData() {
        return data;
    }

    public Throwable getError() {
        return error;
    }
}
