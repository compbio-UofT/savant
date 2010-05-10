/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.util;

/**
 *
 * @author mfiume
 */
public class Bookmark {
    
    private Range range;
    String annotation;

    public Bookmark(Range r) {
        this(r,"");
    }

    public Bookmark(Range r, String ann) {
        this.setRange(r);
        this.setAnnotation(ann);
    }

    public Range getRange() { return this.range; }
    public String getAnnotation() { return this.annotation; }

    public void setRange(Range r) { this.range = r; }
    public void setAnnotation(String ann) { this.annotation = ann; }

}