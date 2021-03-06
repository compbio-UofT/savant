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
package savant.sql;

import java.io.IOException;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.adapter.DataSourceAdapter;
import savant.api.adapter.RangeAdapter;
import savant.api.adapter.RecordFilterAdapter;
import savant.api.data.ContinuousRecord;
import savant.api.data.Interval;
import savant.api.data.IntervalRecord;
import savant.api.data.Record;
import savant.api.util.RangeUtils;
import savant.api.util.Resolution;


/**
 * Each combination of reference and resolution requires a separate cache.
 *
 * @author tarkvara
 */
public class RecordCache<E extends Record> {

    private static final Log LOG = LogFactory.getLog(RecordCache.class);
    private DataSourceAdapter<E> source;
    private String reference;
    private Resolution resolution;
    List<RangeAdapter> covered = new ArrayList<RangeAdapter>();
    RecordStash<E> stash;

    RecordCache(DataSourceAdapter<E> ds, String ref, Resolution res) {
        source = ds;
        reference = ref;
        resolution = res;
    }

    List<E> getRecords(RangeAdapter range, RecordFilterAdapter filt) throws IOException, InterruptedException {

        List<RangeAdapter> missing = getMissingRanges(range);
        for (RangeAdapter r : missing) {
            List<E> subFetch = source.getRecords(reference, r, resolution, filt);
            LOG.debug("Fetched " + subFetch.size() + " records from DataSource for " + r.getFrom() + "-" + r.getTo());
            addToCovered(r);
            if (subFetch.size() > 0) {
                if (stash == null) {
                    if (subFetch.get(0) instanceof IntervalRecord) {
                        stash = new IntervalStash();
                    } else {
                        stash = new PlainStash();
                    }
                }
                stash.store(subFetch);
            }
        }
        if (LOG.isDebugEnabled()) {
            String s = "Covered: " + covered.get(0);
            for (int i = 1; i < covered.size(); i++) {
                s += ", " + covered.get(i);
            }
            LOG.debug(s);
        }

        if (stash != null) {
            LOG.debug("Assembling records for " + range.getFrom() + "-" + range.getTo());
            return stash.retrieve(range);
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
        for (RangeAdapter r2 : covered) {
            List<RangeAdapter> newMissing = new ArrayList<RangeAdapter>();
            for (RangeAdapter r3 : missing) {
                if (RangeUtils.intersects(r2, r3)) {
                    newMissing.addAll(Arrays.asList(RangeUtils.subtract(r3, r2)));
                } else {
                    newMissing.add(r3);
                }
            }
            missing = newMissing;
        }
        return missing;
    }

    private void addToCovered(RangeAdapter r) {
        for (RangeAdapter r2 : covered) {
            if (r2.getFrom() == r.getTo() + 1) {
                covered.remove(r2);
                covered.add(RangeUtils.createRange(r.getFrom(), r2.getTo()));
                return;
            }
            if (r.getFrom() == r2.getTo() + 1) {
                covered.remove(r2);
                covered.add(RangeUtils.createRange(r2.getFrom(), r.getTo()));
                return;
            }

        }
        covered.add(r);
    }

    static abstract class RecordStash<E extends Record> {

        /**
         * We have some new records from SQL.  Store them in our stash.  Some of
         * them may be duplicates of existing records.
         *
         * @param recs the new records
         */
        abstract void store(List<E> recs);

        abstract List<E> retrieve(RangeAdapter r);
    }

    static class PlainStash<E extends Record> extends RecordStash<E> {

        SortedMap<Integer, List<E>> map = new TreeMap<Integer, List<E>>();

        @Override
        void store(List<E> fetched) {
            Map<Integer, List<E>> tempMap = new HashMap<Integer, List<E>>();
            for (E rec : fetched) {
                int key = ((ContinuousRecord)rec).getPosition();
                List<E> existing = tempMap.get(key);
                if (existing == null) {
                    existing = new ArrayList<E>();
                    tempMap.put(key, existing);
                }
                existing.add(rec);
            }
            map.putAll(tempMap);
        }

        @Override
        List<E> retrieve(RangeAdapter r) {
            SortedMap<Integer, List<E>> subMap = map.subMap(r.getFrom(), r.getTo() + 1);
            List<E> result = new ArrayList<E>();
            for (List<E> value : subMap.values()) {
                result.addAll(value);
            }
            return result;
        }
    }

    static class IntervalStash<E extends IntervalRecord> extends RecordStash<E> {
        SortedMap<Interval, List<E>> map = new TreeMap<Interval, List<E>>(new Comparator<Interval>() {
            @Override
            public int compare(Interval i1, Interval i2) {
                long a = i1.getStart();
                long b = i2.getStart();
                if (a < b) {
                    return -1;
                } else if (a > b) {
                    return 1;
                }
                return (int) (i1.getEnd() - i2.getEnd());
            }
        });

        @Override
        void store(List<E> fetched) {
            Map<Interval, List<E>> tempMap = new HashMap<Interval, List<E>>();
            for (E rec : fetched) {
                Interval key = ((IntervalRecord) rec).getInterval();
                List<E> existing = tempMap.get(key);
                if (existing == null) {
                    existing = new ArrayList<E>();
                    tempMap.put(key, existing);
                }
                existing.add(rec);
            }
            map.putAll(tempMap);
        }

        @Override
        List<E> retrieve(RangeAdapter r) {
            // TODO: For efficiency's sake, should calculate a sub-map based on the requested range.
            SortedMap<Interval, List<E>> subMap = map;
            List<E> result = new ArrayList<E>();
            for (List<E> value : subMap.values()) {
                if (value.get(0).getInterval().intersectsRange(r)) {
                    result.addAll(value);
                }
            }
            return result;
        }
    }
}
