/*
 *    Copyright 2010-2011 University of Toronto
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
package savant.util;

import java.io.Serializable;
import java.text.NumberFormat;
import java.text.ParseException;

import savant.api.adapter.BookmarkAdapter;
import savant.api.adapter.RangeAdapter;
import savant.api.util.RangeUtils;
import savant.controller.LocationController;


/**
 *
 * @author mfiume
 */
public class Bookmark implements BookmarkAdapter, Serializable {
    static final long serialVersionUID = 8942835825767587873L;

    private String reference;
    private int from, to;
    private String annotation;

    public Bookmark(String reference, Range r) {
        this(reference, r, "", false);
    }
    
    public Bookmark(String reference, Range r, boolean addMargin){
        this(reference, r, "", addMargin);
    }

    public Bookmark(String reference, Range r, String ann) {
        this(reference, r, "", false);
    }
    
    public Bookmark(String reference, Range r, String ann, boolean addMargin){
        this.setReference(reference);
        this.setRange(addMargin ? RangeUtils.addMargin(r) : r);
        this.setAnnotation(ann);
    }

    /**
     * Construct a bookmark from a string containing a range specification.
     *
     * Note that the bookmark produced by the constructor may be relative to the reference and range
     * which were current at the time of its construction.
     *
     * <dl>
     * <dt>chr2:1000-2000</dt><dd>chr2, range 1000-2000</dd>
     * <dt>chr2:</dt><dd>the current range, but in chr2</dd>
     * <dt>1000-2000</dt><dd>in current chromosome, range 1000-2000</dd>
     * <dt>1000+2000</dt><dd>in current chromosome, range 1000-3000</dd>
     * <dt>1000</dt><dd>in current chromosome, start position at 1000, keeping current range-length</dd>
     * <dt>+1000</dt><dd>1000 bases to the right of the current start, keeping same range-length</dd>
     * <dt>-1000</dt><dd>1000 bases to the left of the current range, keeping same range-length</dd>
     * </dl>
     */
    public Bookmark(String text) throws ParseException {
        LocationController locationController = LocationController.getInstance();
        Range r = locationController.getRange();
        from = -1;
        to = -1;
        if (r != null) {
            from = r.getFrom();
            to = r.getTo();
        }

        text = text.replace(" ", "");

        // Extract a chromosome name (if any).
        int colonPos = text.indexOf(':');
        if (colonPos >= 0) {
            reference = text.substring(0, colonPos).intern();
            text = text.substring(colonPos + 1);
        } else {
            // No reference before a colon, so le
            reference = locationController.getReferenceName();
        }

        if (text.length() > 0) {
            NumberFormat numberParser = NumberFormat.getIntegerInstance();
            int minusPos = text.indexOf('-');
            if (minusPos == 0) {
                // Leading minus sign.  Shift to the left.
                int delta = numberParser.parse(text.substring(1)).intValue();
                from -= delta;
                to -= delta;
            } else if (minusPos > 0) {
                // Fully-specified range.
                from = numberParser.parse(text.substring(0, minusPos)).intValue();
                to = numberParser.parse(text.substring(minusPos + 1)).intValue();
            } else {
                // No minus sign.  Maybe there's a plus?
                int plusPos = text.indexOf('+');
                if (plusPos == 0) {
                    // Leading plus sign.  Shift to the right.
                    int delta = numberParser.parse(text.substring(1)).intValue();
                    from += delta;
                    to += delta;
                } else if (plusPos > 0) {
                    // Range specified as start+length.
                    from = numberParser.parse(text.substring(0, plusPos)).intValue();
                    to = from + numberParser.parse(text.substring(plusPos + 1)).intValue() - 1;
                } else {
                    // No plusses or minusses.
                    if (locationController.getReferenceNames().contains(text)) {
                        // User has just specified a bare chromosome name.
                        reference = text;
                        from = 1;
                        to = 1000;
                    } else {
                        // User is specifying a new start position, but the length remains unchanged.
                        int newFrom = numberParser.parse(text).intValue();
                        to += newFrom - from;
                        from = newFrom;
                    }
                }
            }
        }
    }

    /**
     * Constructor used when loading bookmarks from a dictionary.
     *
     * @param text a location expression of a form like "chr2:1000-2000"
     * @param ann the annotation for this bookmark
     * @throws ParseException if <code>text</code> is not a valid location expression
     */
    public Bookmark(String text, String ann) throws ParseException {
        this(text);
        annotation = ann;

        // Dictionary bookmarks are all given a margin.
        setRange(RangeUtils.addMargin(getRange()));
    }

    /**
     * Intended for use when we want to display bookmarks in a list or combo-box.
     * @return the bookmark's annotation
     */
    @Override
    public String toString() {
        return annotation;
    }

    @Override
    public String getReference() {
        return reference;
    }

    @Override
    public final RangeAdapter getRange() {
        return new Range(from, to);
    }

    @Override
    public String getAnnotation() {
        return annotation;
    }

    @Override
    public final void setReference(String r) { this.reference = r; }

    @Override
    public final void setRange(RangeAdapter r) {
        from = r.getFrom();
        to = r.getTo();
    }

    @Override
    public final void setAnnotation(String ann) { this.annotation = ann; }

    public final int getFrom() {
        return from;
    }

    public final int getTo() {
        return to;
    }

    /**
     * Get the bookmark's location expressed in its canonical form.
     */
    public final String getLocationText() {
        return String.format("%s:%d-%d", reference, from, to);
    }
}