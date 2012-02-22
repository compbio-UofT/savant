/*
 *    Copyright 2010-2012 University of Toronto
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

import savant.api.adapter.RangeAdapter;
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
    private final Type type;
    private final TrackAdapter track;
    private final List<Record> data;
    private final RangeAdapter range;
    private final Throwable error;

    /**
     * Constructor for retrieval starting.
     */
    public DataRetrievalEvent(TrackAdapter t, RangeAdapter r) {
        type = Type.STARTED;
        track = t;
        data = null;
        range = r;
        error = null;
    }

    /**
     * Constructor when data is successfully retrieved.
     * @param d the records retrieved
     */
    public DataRetrievalEvent(TrackAdapter t, List<Record> d, RangeAdapter r) {
        type = Type.COMPLETED;
        track = t;
        data = d;
        range = r;
        error = null;
    }

    /**
     * Constructor when retrieval has failed.
     *
     * @param t track for which the retrieval failed
     * @param x error which caused the failure
     * @param r range which was requested at the time of failure
     */
    public DataRetrievalEvent(TrackAdapter t, Throwable x, RangeAdapter r) {
        type = Type.FAILED;
        track = t;
        data = null;
        range = r;
        error = x;
        
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

    public RangeAdapter getRange() {
        return range;
    }

    public Throwable getError() {
        return error;
    }
}
