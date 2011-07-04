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
