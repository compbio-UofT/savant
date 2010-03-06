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

package savant.view.swing;

import savant.model.view.DrawingInstructions;
import savant.util.Range;
import savant.view.swing.continuous.ContinuousTrackRenderer;
import savant.view.swing.interval.BAMTrackRenderer;
import savant.view.swing.interval.BEDTrackRenderer;
import savant.view.swing.interval.IntervalTrackRenderer;
import savant.view.swing.point.PointTrackRenderer;
import savant.view.swing.sequence.SequenceTrackRenderer;


import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author mfiume
 */
public class Frame {

    private boolean isHidden = false;
    private GraphPane graphPane;
    private JComponent frameLandscape;
    private List<ViewTrack> tracks;
    private boolean isLocked;
    private Range currentRange;

    public boolean isHidden() { return this.isHidden; }
    public void setHidden(boolean isHidden) { this.isHidden = isHidden; }

    public GraphPane getGraphPane() { return this.graphPane; }
    public JComponent getFrameLandscape() { return this.frameLandscape; }
    public List<ViewTrack> getTracks() { return this.tracks; }

    public boolean isOpen() { return getGraphPane() != null; }

    public Frame(JComponent frameLandscape) { this(frameLandscape, null, null); }

    public Frame(JComponent frameLandscape, List<ViewTrack> tracks) { this(frameLandscape, tracks, new ArrayList<TrackRenderer>()); }

    public Frame(JComponent frameLandscape, List<ViewTrack> tracks, List<TrackRenderer> renderers)
    {
        isLocked = false;
        this.tracks = new ArrayList<ViewTrack>();
        this.frameLandscape = frameLandscape;
        initGraph();
        if (!tracks.isEmpty()) {
            int i=0;
            Iterator<ViewTrack> it = tracks.iterator();
            while ( it.hasNext()) {
                ViewTrack track = it.next();
                TrackRenderer renderer=null;
                if (!renderers.isEmpty()) {
                    renderer = renderers.get(i++);
                }
                addTrack(track, renderer);
            }
        }
        frameLandscape.setLayout(new BorderLayout());
        frameLandscape.add(getGraphPane());
    }

    /**
     * Add a track to the list of tracks in
     * this frame
     * @param track The track to add
     * @param renderer to add for this track; if null, add default renderer for data type
     */
    public void addTrack(ViewTrack track, TrackRenderer renderer) {
        tracks.add(track);

        if (renderer == null) {
            switch(track.getDataType()) {
                case POINT:
                    renderer = new PointTrackRenderer();
                    break;
                case INTERVAL_GENERIC:
                    renderer = new IntervalTrackRenderer();
                    break;
                case CONTINUOUS_GENERIC:
                    renderer = new ContinuousTrackRenderer();
                    break;
                case INTERVAL_BAM:
                    renderer = new BAMTrackRenderer();
                    break;
                case INTERVAL_BED:
                    renderer = new BEDTrackRenderer();
                    break;
                case SEQUENCE_FASTA:
                    renderer = new SequenceTrackRenderer();
                    break;
            }
        }
        renderer.getDrawingInstructions().addInstruction(
                DrawingInstructions.InstructionName.TRACK_DATA_TYPE, track.getDataType());
        track.addTrackRenderer(renderer);
        GraphPane graphPane = getGraphPane();
        graphPane.addTrackRenderer(renderer);
        graphPane.addTrack(track);
    }

    public void addTrack(ViewTrack track) {
        addTrack(track, null);
    }

    public void redrawTracksInRange() throws Exception {
        drawTracksInRange(currentRange);
    }

    /**
     * // TODO: comment
     * @param range
     * @throws Exception
     */
    public void drawTracksInRange(Range range) throws Exception
    {
        if (!isLocked()) { currentRange = range; }
        if (this.graphPane.isLocked()) { return; }

        if (this.tracks.size() > 0) {

            this.graphPane.setXRange(currentRange);

            for (ViewTrack track : tracks) {
                track.prepareForRendering(range);
            }

            this.graphPane.repaint();
        }
    }

    private GraphPane getNewZedGraphControl() {
        GraphPane zgc = new GraphPane();

        // TODO: set properties

        return zgc;
    }

    // TODO: what is locking for?
    public void lockRange(Range r) { setLocked(true, r); }
    public void unlockRange() { setLocked(false, null); }
    public void setLocked(boolean b, Range r) {
        this.isLocked = b;
        this.currentRange = r;
    }

    public boolean isLocked() { return this.isLocked; }

    private void initGraph() {
        graphPane = getNewZedGraphControl();
        graphPane.setBackground(BrowserDefaults.colorFrameBackground);
    }

}
