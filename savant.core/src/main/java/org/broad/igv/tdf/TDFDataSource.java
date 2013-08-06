/*
 * Copyright (c) 2007-2011 by The Broad Institute of MIT and Harvard.  All Rights Reserved.
 *
 * This software is licensed under the terms of the GNU Lesser General Public License (LGPL),
 * Version 2.1 which is available at http://www.opensource.org/licenses/lgpl-2.1.php.
 *
 * THE SOFTWARE IS PROVIDED "AS IS." THE BROAD AND MIT MAKE NO REPRESENTATIONS OR
 * WARRANTES OF ANY KIND CONCERNING THE SOFTWARE, EXPRESS OR IMPLIED, INCLUDING,
 * WITHOUT LIMITATION, WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, NONINFRINGEMENT, OR THE ABSENCE OF LATENT OR OTHER DEFECTS, WHETHER
 * OR NOT DISCOVERABLE.  IN NO EVENT SHALL THE BROAD OR MIT, OR THEIR RESPECTIVE
 * TRUSTEES, DIRECTORS, OFFICERS, EMPLOYEES, AND AFFILIATES BE LIABLE FOR ANY DAMAGES
 * OF ANY KIND, INCLUDING, WITHOUT LIMITATION, INCIDENTAL OR CONSEQUENTIAL DAMAGES,
 * ECONOMIC DAMAGES OR INJURY TO PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER
 * THE BROAD OR MIT SHALL BE ADVISED, SHALL HAVE OTHER REASON TO KNOW, OR IN FACT
 * SHALL KNOW OF THE POSSIBILITY OF THE FOREGOING.
 */

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.broad.igv.tdf;

import org.apache.log4j.Logger;
import org.broad.igv.feature.genome.Genome;
import org.broad.igv.feature.LocusScore;
import org.broad.igv.track.TrackType;
import org.broad.igv.track.WindowFunction;
import org.broad.igv.util.LRUCache;

import java.util.*;

/**
 * @author jrobinso
 */
public class TDFDataSource {

    private static Logger log = Logger.getLogger(TDFDataSource.class);

    int maxPrecomputedZoom = 6;
    TDFReader reader;
    private int trackNumber = 0;
    String trackName;
    LRUCache<String, List<LocusScore>> summaryScoreCache = new LRUCache(this, 20);
    Genome genome;
    Interval currentInterval;
    WindowFunction windowFunction = WindowFunction.mean;
    List<WindowFunction> availableFunctions;

    private boolean aggregateLikeBins = true;

    boolean normalizeCounts = false;
    int totalCount = 0;
    float normalizationFactor = 1.0f;
    private Map<String, String> chrNameMap = new HashMap();


    public TDFDataSource(TDFReader reader, int trackNumber, String trackName, Genome genome) {

        this.genome = genome;

        // TODO -- a single reader will be shared across data sources
        this.trackNumber = trackNumber;
        this.trackName = trackName;
        this.reader = reader;
        this.availableFunctions = reader.getWindowFunctions();

        TDFGroup rootGroup = reader.getGroup("/");
        try {
            maxPrecomputedZoom = Integer.parseInt(rootGroup.getAttribute("maxZoom"));
        } catch (Exception e) {
            log.error("Error reading attribute 'maxZoom'", e);
        }
        try {
            String dataGenome = rootGroup.getAttribute("genome");
            // TODO -- throw exception if data genome != current genome 
        } catch (Exception e) {
            log.error("Unknown genome " + rootGroup.getAttribute("genome"));
            throw new RuntimeException("Unknown genome " + rootGroup.getAttribute("genome"));
        }

        try {
            String totalCountString = rootGroup.getAttribute("totalCount");
            if (totalCountString != null) {
                totalCount = Integer.parseInt(totalCountString);
            }
        } catch (Exception e) {
            log.error("Error reading attribute 'totalCount'", e);
        }

        // If we have a genome, build a reverse-lookup table for queries
        if (genome != null) {
            Set<String> chrNames = reader.getChromosomeNames();
            for (String chr : chrNames) {
                String igvChr = genome.getChromosomeAlias(chr);
                if (igvChr != null && !igvChr.equals(chr)) {
                    chrNameMap.put(igvChr, chr);
                }
            }
        }
    }

    public void setNormalize(boolean normalizeCounts) {
        setNormalizeCounts(normalizeCounts, 1.0e6f);
    }

    public void setNormalizeCounts(boolean normalizeCounts, float scalingFactor) {
        this.normalizeCounts = normalizeCounts;
        if (normalizeCounts && totalCount > 0) {
            normalizationFactor = scalingFactor / totalCount;
        } else {
            normalizationFactor = 1;
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

    public Collection<WindowFunction> getAvailableWindowFunctions() {
        return availableFunctions;
    }

}

