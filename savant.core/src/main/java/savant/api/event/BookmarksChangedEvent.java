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
package savant.api.event;

import savant.api.adapter.BookmarkAdapter;
import savant.util.Bookmark;

/**
 *
 * @author mfiume
 */
public class BookmarksChangedEvent {
    public enum Type {
        ADDED,
        REMOVED
    }

    private Type type;
    private Bookmark bookmark;

    /**
     * Event which describes a change in a bookmark.
     *
     * @param bm the bookmark which has change
     * @param isAdded true iff bookmark was added, false if removed
     */
    public BookmarksChangedEvent(Bookmark bm, boolean isAdded) {
        this.bookmark = bm;
        type = isAdded ? Type.ADDED : Type.REMOVED;
    }

    public boolean isAdded() {
        return type == Type.ADDED;
    }

    public BookmarkAdapter getBookmark() {
        return bookmark;
    }

}