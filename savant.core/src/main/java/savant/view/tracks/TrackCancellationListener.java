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
package savant.view.tracks;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.adapter.FrameAdapter;


/**
 * ActionListener which is attached to the cancel button on a ProgressPanel to allow users to cancel a lengthy track-related operation.
 *
 * @author tarkvara
 */
public class TrackCancellationListener implements ActionListener {
    private static final Log LOG = LogFactory.getLog(TrackCancellationListener.class);

    private final FrameAdapter frame;

    public TrackCancellationListener(FrameAdapter f) {
        frame = f;
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        Track[] tracks = frame.getTracks();
        if (tracks.length > 0) {
            LOG.info("Cancelling data requests for " + tracks.length + " tracks.");
            for (Track t: tracks) {
                t.cancelDataRequest();
            }
        } else {
            // User has decided to cancel initial creation of the frame, so close it.
            LOG.info("Closing frame for track.");
            frame.handleEvent(new TrackCreationEvent(new Exception("Cancelled")));
        }
    }
}
