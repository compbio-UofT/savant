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
package savant.api.adapter;

/**
 * Public interface for Savant bookmark objects.
 *
 * @author mfiume
 */
public interface BookmarkAdapter {

    /**
     * Get the reference associated with the range covered by this bookmark (e.g.&nbsp;"chrX").
     *
     * @return the reference for this bookmark
     */
    public String getReference();

    /**
     * Get the range covered by this bookmark.
     *
     * @return the range covered by this bookmark
     */
    public RangeAdapter getRange();

    /**
     * Get the annotation text associated with this bookmark.
     *
     * @return this bookmark's annotation text
     */
    public String getAnnotation();

    /**
     * Set the reference for this bookmark.
     *
     * @param value the new reference
     */
    public void setReference(String value);

    /**
     * Set the range for this bookmark.
     *
     * @param value the new range
     */
    public void setRange(RangeAdapter value);

    /**
     * Set the annotation text for this bookmark.
     *
     * @param value the new annotation text
     */
    public void setAnnotation(String value);
}
