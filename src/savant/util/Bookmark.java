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
import savant.controller.RangeController;
import savant.controller.ReferenceController;


/**
 *
 * @author mfiume
 */
public class Bookmark implements BookmarkAdapter, Serializable {

    /** For parsing numbers which may include commas. */
    private static final NumberFormat NUMBER_PARSER = NumberFormat.getIntegerInstance();

    private String reference;
    private Range range;
    private String annotation;

    public Bookmark(String reference, Range r) {
        this(reference,r,"");
    }

    public Bookmark(String reference, Range r, String ann) {
        this.setReference(reference);
        this.setRange(r);
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
        RangeController rangeController = RangeController.getInstance();
        int from = rangeController.getRangeStart();
        int to = rangeController.getRangeEnd();

        // Extract a chromosome name (if any).
        int colonPos = text.indexOf(':');
        if (colonPos >= 0) {
            reference = text.substring(0, colonPos);
            text = text.substring(colonPos + 1);
        } else {
            reference = ReferenceController.getInstance().getReferenceName();
        }

        if (text.length() > 0) {
            int minusPos = text.indexOf('-');
            if (minusPos == 0) {
                // Leading minus sign.  Shift to the left.
                int delta = NUMBER_PARSER.parse(text.substring(1)).intValue();
                from -= delta;
                to -= delta;
            } else if (minusPos > 0) {
                // Fully-specified range.
                from = NUMBER_PARSER.parse(text.substring(0, minusPos)).intValue();
                to = NUMBER_PARSER.parse(text.substring(minusPos + 1)).intValue();
            } else {
                // No minus sign.  Maybe there's a plus?
                int plusPos = text.indexOf('+');
                if (plusPos == 0) {
                    // Leading plus sign.  Shift to the right.
                    int delta = NUMBER_PARSER.parse(text.substring(1)).intValue();
                    from += delta;
                    to += delta;
                } else if (plusPos > 0) {
                    // Range specified as start+length.
                    from = NUMBER_PARSER.parse(text.substring(0, plusPos)).intValue();
                    to = from + NUMBER_PARSER.parse(text.substring(plusPos + 1)).intValue() - 1;
                } else {
                    // No plusses or minusses.  User is specifying a new start position, but the length remains unchanged.
                    int newFrom = NUMBER_PARSER.parse(text).intValue();
                    to += newFrom - from;
                    from = newFrom;
                }
            }
        }
        range = new Range(from, to);
    }

    @Override
    public String getReference() { return this.reference; }

    @Override
    public RangeAdapter getRange() { return this.range; }

    @Override
    public String getAnnotation() { return this.annotation; }

    @Override
    public final void setReference(String r) { this.reference = r; }

    @Override
    public final void setRange(RangeAdapter r) { this.range = (Range) r; }

    @Override
    public final void setAnnotation(String ann) { this.annotation = ann; }

}