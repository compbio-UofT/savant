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
