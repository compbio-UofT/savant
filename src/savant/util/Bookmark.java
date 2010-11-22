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

    public String getReference() { return this.reference; }
    public RangeAdapter getRange() { return this.range; }
    public String getAnnotation() { return this.annotation; }

    public void setReference(String r) { this.reference = r; }
    public void setRange(RangeAdapter r) { this.range = (Range) r; }
    public void setAnnotation(String ann) { this.annotation = ann; }

}