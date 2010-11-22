/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.api.adapter;

import savant.api.adapter.RangeAdapter;

/**
 *
 * @author mfiume
 */
public interface BookmarkAdapter {

    public String getReference();
    public RangeAdapter getRange();
    public String getAnnotation();
    public void setReference(String r);
    public void setRange(RangeAdapter r);
    public void setAnnotation(String ann);
}
