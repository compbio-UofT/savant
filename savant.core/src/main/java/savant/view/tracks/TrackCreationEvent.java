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
package savant.view.tracks;


/**
 * Event class which allows asynchronous creation of tracks.
 *
 * @author tarkvara
 */
public class TrackCreationEvent {
    public enum Type {
        STARTED,
        COMPLETED,
        FAILED,
        PROGRESS
    };
    Type type;
    Track[] tracks;
    String name;
    Throwable error;
    String progressMessage;
    double progressFraction;

    /**
     * Constructor for event which is fired as track-creation begins.
     */
    public TrackCreationEvent() {
        this.type = Type.STARTED;
    }

    /**
     * Constructor when tracks have been successfully created.
     *
     * @param tracks the tracks created
     * @param name the display name for this collection of tracks
     */
    public TrackCreationEvent(Track[] tracks, String name) {
        this.type = Type.COMPLETED;
        this.tracks = tracks;
        this.name = name;
    }

    /**
     * Constructor when track creation has failed.
     *
     * @param error
     */
    public TrackCreationEvent(Throwable error) {
        this.type = Type.FAILED;
        this.error = error;
    }

    public TrackCreationEvent(String msg, double amount) {
        this.type = Type.PROGRESS;
        progressMessage = msg;
        progressFraction = amount;
    }

    public Type getType() {
        return type;
    }

    public Track[] getTracks() {
        return tracks;
    }

    public String getName() {
        return name;
    }

    public Throwable getError() {
        return error;
    }
    
    public String getProgressMessage() {
        return progressMessage;
    }
    
    public double getProgressFraction() {
        return progressFraction;
    }
}
