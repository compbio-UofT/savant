/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.util;

import java.io.Serializable;
import savant.api.adapter.BookmarkAdapter;
import savant.api.adapter.RangeAdapter;

/**
 *
 * @author mfiume
 */
public class Bookmark implements BookmarkAdapter, Serializable {

    private String reference;
    private Range range;
    String annotation;

    public Bookmark(String reference, Range r) {
        this(reference,r,"");
    }

    public Bookmark(String reference, Range r, String ann) {
        this.setReference(reference);
        this.setRange(r);
        this.setAnnotation(ann);
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