/*
 *    Copyright 2011 University of Toronto
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

package savant.sql;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.adapter.RangeAdapter;
import savant.api.util.RangeUtils;
import savant.data.sources.DataSource;
import savant.data.types.Interval;
import savant.data.types.IntervalRecord;
import savant.data.types.Record;
import savant.util.Resolution;


/**
 * Each combination of reference and resolution requires a separate cache.
 *
 * @author tarkvara
 */
public class RecordCache<E extends Record> {
    private static final Log LOG = LogFactory.getLog(RecordCache.class);

    private DataSource<E> source;
    private String reference;
    private Resolution resolution;
    CacheNode<E> rootNode;

    RecordCache(DataSource<E> ds, String ref, Resolution res) {
        source = ds;
        reference = ref;
        resolution = res;
    }

    List<E> getRecords(RangeAdapter range) throws IOException {

        List<RangeAdapter> missing = getMissingRanges(range);
        for (RangeAdapter r: missing) {
            List<E> subFetch = source.getRecords(reference, r, resolution);
            LOG.debug("Fetched " + subFetch.size() + " records from DataSource for " + r);

            if (subFetch.size() > 0) {
                if (rootNode == null) {
                    RangeAdapter totalRange = RangeUtils.createRange(1, Long.MAX_VALUE);
                    List<E> emptyData = new ArrayList<E>();
                    if (subFetch.get(0) instanceof IntervalRecord) {
                        rootNode = new IntervalCacheNode<E>(totalRange, emptyData);
                    } else {
                        rootNode = new CacheNode<E>(totalRange, emptyData);
                    }
                }
                CacheNode<E> parent = rootNode.findContainingNode(r);
                parent.addChild(r, subFetch);
                rootNode.dump("");
            }
        }

        if (rootNode != null) {
            LOG.debug("Assembling records for " + range);
            List<E> result = rootNode.getRecords(range);
            rootNode.postProcess(result, range);
            return result;
        }
        return new ArrayList<E>();
    }

    /**
     * Given a requested range, figure out which ranges are not in the cache and
     * for which we need to do a fetch.
     *
     * @param r range of data requested
     */
    private List<RangeAdapter> getMissingRanges(RangeAdapter r) {
        List<RangeAdapter> missing = new ArrayList<RangeAdapter>();
        missing.add(r);
        if (rootNode != null) {
            List<CacheNode<E>> intersectingNodes = rootNode.findIntersectingLeaves(r);
            for (CacheNode<E> c: intersectingNodes) {
                List<RangeAdapter> newMissing = new ArrayList<RangeAdapter>();
                for (RangeAdapter r1: missing) {
                    newMissing.addAll(RangeUtils.subtract(r1, c.range));
                }
                missing = newMissing;
            }
        }
        return missing;
    }

    static class CacheNode<E extends Record> {
        final RangeAdapter range;
        final List<E> data;
        List<CacheNode<E>> children;

        CacheNode(RangeAdapter r, List<E> d) {
            range = r;
            data = d;
        }

        void addChild(RangeAdapter r, List<E> d) {
            if (children == null) {
                children = new ArrayList<CacheNode<E>>();
            }
            children.add(new CacheNode<E>(r, d));
        }

        /**
         * Find all the leaf nodes which contain the requested range.
         *
         * @param r
         * @return
         */
        List<CacheNode<E>> findIntersectingLeaves(RangeAdapter r) {
            if (RangeUtils.intersects(r, range)) {
                if (children != null) {
                    List<CacheNode<E>> result = new ArrayList<CacheNode<E>>();
                    for (CacheNode<E> c: children) {
                        List<CacheNode<E>> childResult = c.findIntersectingLeaves(r);
                        if (childResult != null) {
                            result.addAll(childResult);
                        }
                    }
                    return result;
                } else {
                    return Arrays.asList(this);
                }
            }
            // Range doesn't intersect this node.
            return null;
        }

        /**
         * When we're adding a new node to the cache, find out which node should
         * be its parent.
         *
         * @param r
         * @return
         */
        CacheNode<E> findContainingNode(RangeAdapter r) {
            if (RangeUtils.contains(range, r)) {
                if (children != null) {
                    for (CacheNode<E> c: children) {
                        CacheNode<E> result = c.findContainingNode(r);
                        if (result != null) {
                            return result;
                        }
                    }
                }
                return this;
            }
            return null;
        }

        /**
         * Build up a list of records for the given range.
         *
         * @param r
         * @return
         */
        List<E> getRecords(RangeAdapter r) {
            if (RangeUtils.intersects(r, range)) {
                List<E> result = new ArrayList<E>();
                if (children != null) {
                    for (CacheNode<E> c: children) {
                        List<E> childRecs = c.getRecords(r);
                        if (childRecs != null) {
                            result.addAll(childRecs);
                        }
                    }
                }
                result.addAll(data);
                LOG.debug("   assembled " + data.size() + " records from " + range);
                return result;
            }
            return null;
        }

        /**
         * Give derived classes an opportunity to filter and sort the results.
         */
        void postProcess(List<E> result, RangeAdapter r) {
        }

        /**
         * For debug purposes, dump the cache out so we can see what's going on.
         */
        void dump(String header) {
            String recordDump = data.size() + " records";
            for (E rec: data) {
                if (rec instanceof IntervalRecord) {
                    recordDump += " (" + ((IntervalRecord)rec).getInterval() + ")";
                }
            }
            LOG.debug(header + range + " had " + recordDump);
            if (children != null) {
                for (CacheNode<E> child: children) {
                    child.dump(header + "   ");
                }
            }
        }
    }

    /**
     * CacheNode class for IntervalRecords has extra logic to move IntervalRecords
     * which span a given range up to a higher level.
     */
    static class IntervalCacheNode<E extends Record> extends CacheNode<E> {
        /** IntervalRecords will often overflow the notional range of the node.*/
        private RangeAdapter span;

        IntervalCacheNode(RangeAdapter r, List<E> d) {
            super(r, d);
        }

        @Override
        void addChild(RangeAdapter r, List<E> d) {
            IntervalCacheNode<E> newChild = new IntervalCacheNode<E>(r, d);
            if (children == null) {
                children = new ArrayList<CacheNode<E>>();
                children.add(newChild);
            } else {
                // There may be some IntervalRecords in the existing children which are shared
                // with the new child.
                List<CacheNode<E>> spanningChildren = new ArrayList<CacheNode<E>>();
                long spanningFrom = r.getFrom();
                long spanningTo = r.getTo();
                List<E> accumulatedSpanningRecords = new ArrayList<E>();
                for (CacheNode<E> child: children) {
                    List<E> spanningRecords = ((IntervalCacheNode<E>)child).getSpanningRecords(r);
                    if (spanningRecords.size() > 0) {
                        // Remove duplicates from the existing child and our new one.
                        child.data.removeAll(spanningRecords);
                        ((IntervalCacheNode<E>)child).span = null;
                        spanningRecords = newChild.getSpanningRecords(child.range);
                        newChild.data.removeAll(spanningRecords);

                        // Build up our potential uber-parent.
                        spanningFrom = Math.min(spanningFrom, child.range.getFrom());
                        spanningTo = Math.max(spanningTo, child.range.getTo());
                        accumulatedSpanningRecords.addAll(spanningRecords);
                        spanningChildren.add(child);
                        LOG.debug("Found " + spanningRecords.size() + " spanning records, pushing them from " + child.range + " up to " + spanningFrom + "-" + spanningTo);
                    }
                }
                if (spanningChildren.size() > 0) {
                    // We've accumulated some spanning nodes.  Lets see if they warrant
                    // the insertion of a new parent.
                    if (spanningFrom > range.getFrom() || spanningTo < range.getTo()) {
                        // New range differs from our current range.  Insert a new parent.
                        LOG.debug("Inserting new parent with range " + spanningFrom + "-" + spanningTo + ", " + spanningChildren.size() + " children, and " + accumulatedSpanningRecords.size() + " records.");
                        children.removeAll(spanningChildren);
                        IntervalCacheNode<E> newParent = new IntervalCacheNode<E>(RangeUtils.createRange(spanningFrom, spanningTo), accumulatedSpanningRecords);
                        children.add(newParent);
                        newParent.children = spanningChildren;
                        newParent.children.add(newChild);
                    } else {
                        // Let's just push these records into the current parent.
                        LOG.debug("No new parent, just adding 1 child and " + accumulatedSpanningRecords.size() + " records to " + range);
                        data.addAll(accumulatedSpanningRecords);
                        children.add(newChild);
                    }
                }
            }
            span = null;
        }

        /**
         * Build up a list of records for the given range.
         *
         * @param r
         * @return
         */
        @Override
        void postProcess(List<E> result, RangeAdapter r) {
            if (result != null) {
                List<E> outOfRange = new ArrayList<E>();
                for (E rec: result) {
                    if (!((IntervalRecord)rec).getInterval().intersectsRange(r)) {
                        outOfRange.add(rec);
                    }
                }
                result.removeAll(outOfRange);
                LOG.debug("Reduced to " + result.size() + " records after pruning range.");
                Collections.sort(result, new Comparator() {
                    @Override
                    public int compare(Object o1, Object o2) {
                        Interval i1 = ((IntervalRecord)o1).getInterval();
                        Interval i2 = ((IntervalRecord)o2).getInterval();

                        if (i1.getStart() < i2.getStart()) {
                            return -1;
                        } else if (i1.getStart() > i2.getStart()) {
                            return 1;
                        } else {
                            return 0;
                        }
                    }
                });
            }
        }

        private RangeAdapter getSpan() {
            if (span == null) {
                long min = range.getFrom();
                long max = range.getTo();
                for (E rec: data) {
                    Interval x = ((IntervalRecord)rec).getInterval();
                    min = Math.min(min, x.getStart());
                    max = Math.max(max, x.getEnd());
                }
                RangeAdapter foo = RangeUtils.createRange(min, max);
                if (children != null) {
                    for (CacheNode<E> child: children) {
                        min = Math.min(min, ((IntervalCacheNode<E>)child).getSpan().getFrom());
                        max = Math.max(max, ((IntervalCacheNode<E>)child).getSpan().getTo());
                    }
                }
                span = RangeUtils.createRange(min, max);
                LOG.debug("For " + range + " data-span was " + foo + ", child-span was " + span);
            }
            return span;
        }

        /**
         * Find all IntervalRecords which intersect with the given range.  These are ones
         * which overflow this node's range into another sibling node.  They will
         * be pushed up to the parent to avoid duplication.
         *
         * @param r the range of some other node
         * @return list of all IntervalRecords which overflow into the other node's range
         */
        private List<E> getSpanningRecords(RangeAdapter r) {
            List<E> result = new ArrayList<E>();
            if (RangeUtils.intersects(r, getSpan())) {
                for (E rec: data) {
                    Interval x = ((IntervalRecord)rec).getInterval();
                    if (x.intersectsRange(r)) {
                        result.add(rec);
                    }
                }
                if (children != null) {
                    for (CacheNode<E> child: children) {
                        result.addAll(((IntervalCacheNode<E>)child).getSpanningRecords(r));
                    }
                }
            }
            return result;
        }
    }
}
