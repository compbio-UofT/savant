/*
 * Copyright (c) 2007-2010 by The Broad Institute, Inc. and the Massachusetts Institute of Technology.
 * All Rights Reserved.
 *
 * This software is licensed under the terms of the GNU Lesser General Public License (LGPL), Version 2.1 which
 * is available at http://www.opensource.org/licenses/lgpl-2.1.php.
 *
 * THE SOFTWARE IS PROVIDED "AS IS." THE BROAD AND MIT MAKE NO REPRESENTATIONS OR WARRANTIES OF
 * ANY KIND CONCERNING THE SOFTWARE, EXPRESS OR IMPLIED, INCLUDING, WITHOUT LIMITATION, WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NONINFRINGEMENT, OR THE ABSENCE OF LATENT
 * OR OTHER DEFECTS, WHETHER OR NOT DISCOVERABLE.  IN NO EVENT SHALL THE BROAD OR MIT, OR THEIR
 * RESPECTIVE TRUSTEES, DIRECTORS, OFFICERS, EMPLOYEES, AND AFFILIATES BE LIABLE FOR ANY DAMAGES OF
 * ANY KIND, INCLUDING, WITHOUT LIMITATION, INCIDENTAL OR CONSEQUENTIAL DAMAGES, ECONOMIC
 * DAMAGES OR INJURY TO PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER THE BROAD OR MIT SHALL
 * BE ADVISED, SHALL HAVE OTHER REASON TO KNOW, OR IN FACT SHALL KNOW OF THE POSSIBILITY OF THE
 * FOREGOING.
 */

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.broad.igv.tdf;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.broad.igv.feature.Genome;
import org.broad.igv.feature.LocusScore;
import org.broad.igv.track.TrackType;
import org.broad.igv.track.WindowFunction;

import java.util.List;

/**
 * @author jrobinso
 */
public class TDFDataSource {

    private static Log log = LogFactory.getLog(TDFDataSource.class);
    // TODO -- read from file
    int maxZoom = 6;
    TDFReader reader;
    private int trackNumber = 0;
    String trackName;
    Genome genome;
    Interval currentInterval;
    WindowFunction windowFunction = WindowFunction.mean;
//    List<WindowFunction> availableFunctions;

    private boolean aggregateLikeBins = true;

    float normalizationFactor = 1.0f;


    public TDFDataSource(TDFReader reader, int trackNumber, String trackName) {

        boolean normalizeCounts = false;

        // TODO -- a single reader will be shared across data sources
        this.trackNumber = trackNumber;
        this.trackName = trackName;
        this.reader = reader;

        TDFGroup rootGroup = reader.getGroup("/");
        try {
            maxZoom = Integer.parseInt(rootGroup.getAttribute("maxZoom"));
        } catch (Exception e) {
            log.error("Error reading attribute 'maxZoom'", e);
        }
        try {
            if (normalizeCounts) {
                String totalCountString = rootGroup.getAttribute("totalCount");
                if (totalCountString != null) {
                    int totalCount = Integer.parseInt(totalCountString);
                    normalizationFactor = 1.0e6f / totalCount;
                }
            }
        } catch (Exception e) {
            log.error("Error reading attribute 'maxZoom'", e);
        }

    }

    public String getPath() {
        return reader == null ? null : reader.getPath();
    }

    public String getTrackName() {
        return trackName;
    }

    public double getDataMax() {
        return reader.getUpperLimit() * normalizationFactor;
    }

    public double getDataMin() {
        return reader.getLowerLimit() * normalizationFactor;
    }

    public void setAggregateLikeBins(boolean aggregateLikeBins) {
        this.aggregateLikeBins = aggregateLikeBins;
    }

    class Interval {

        String chr;
        private int start;
        private int end;
        private int zoom;
        private List<LocusScore> scores;

        public Interval(String chr, int start, int end, int zoom, List<LocusScore> scores) {
            this.chr = chr;
            this.start = start;
            this.end = end;
            this.zoom = zoom;
            this.scores = scores;
        }

        public boolean contains(String chr, int s, int e, int zoom) {
            return chr.equals(this.chr) && zoom == this.zoom && s >= getStart() && e <= getEnd();
        }

        /**
         * @return the start
         */
        public int getStart() {
            return start;
        }

        /**
         * @return the end
         */
        public int getEnd() {
            return end;
        }

        /**
         * @return the scores
         */
        public List<LocusScore> getScores() {
            return scores;
        }
    }

    public TrackType getTrackType() {
        return reader.getTrackType();
    }

    public void setWindowFunction(WindowFunction wf) {
        this.windowFunction = wf;
    }

    public boolean isLogNormalized() {
        //return false;
        return getDataMin() < 0;
    }

    public void refreshData(long timestamp) {
        // ignored
    }

    public WindowFunction getWindowFunction() {
        return windowFunction;
    }
}

