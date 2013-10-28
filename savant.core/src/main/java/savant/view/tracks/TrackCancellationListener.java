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
