/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.format;

import savant.util.Range;

/**
 *
 * @author mfiume
 */
public class LinePlusRange {
    public Range range;
    public int lineNum;

    public LinePlusRange(Range r, int ln) {
        range = r;
        lineNum = ln;
    }

}
