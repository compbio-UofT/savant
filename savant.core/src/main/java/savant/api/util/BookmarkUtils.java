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
package savant.api.util;

import savant.api.adapter.BookmarkAdapter;
import savant.api.adapter.RangeAdapter;
import savant.api.event.BookmarksChangedEvent;
import savant.controller.BookmarkController;
import savant.util.Bookmark;
import savant.util.Range;


/**
 * Utilities for managing Savant bookmarks.
 *
 * @author mfiume
 */
public class BookmarkUtils {

    private static BookmarkController controller = BookmarkController.getInstance();

    /**
     * Get an array of the current bookmarks.
     *
     * @return all the current bookmarks
     */
    public static BookmarkAdapter[] getBookmarks() {
        return controller.getBookmarks().toArray(new BookmarkAdapter[0]);
    }

    /**
     * Add a bookmark.
     *
     * @param value the bookmark to add
     */
    public static void addBookmark(BookmarkAdapter value) {
        controller.addBookmark((Bookmark)value);
    }

    /**
     * Remove the last bookmark.
     */
    public static void removeLastBookmark() {
        controller.removeBookmark();
    }

    /**
     * Remove the given bookmark from the list.
     *
     * @param index index of the bookmark to remove
     */
    public static void removeBookmark(int index) {
        controller.removeBookmark(index);
    }

    /**
     * Subscribe a listener to be notified when Savant's bookmark list changes.
     *
     * @param l the listener to subscribe
     */
    public static void addBookmarksChangedListener(Listener<BookmarksChangedEvent> l) {
        controller.addListener(l);
    }

    /**
     * Unsubscribe a listener from being notified when Savant's bookmark list changes.
     *
     * @param l the listener to unsubscribe
     */
    public static void removeBookmarksChangedListener(Listener<BookmarksChangedEvent> l) {
        controller.removeListener(l);
    }

    /**
     * Add the current range as a bookmark.
     */
    public static void addCurrentRangeToBookmarks() {
        controller.addCurrentRangeToBookmarks();
    }

    /**
     * Get a bookmark from the bookmark list.
     *
     * @param index the index of the bookmark to retrieve
     * @return the bookmark at the specified index
     */
    public static BookmarkAdapter getBookmark(int index) {
        return (BookmarkAdapter)controller.getBookmark(index);
    }

    /**
     * Clear Savant's bookmark list.
     */
    public static void clearBookmarks() {
        controller.clearBookmarks();
    }

    /**
     * Factory method for creating a new BookmarkAdapter object.
     *
     * @param ref the reference for the new bookmark (e.g. "chrX")
     * @param range the range for the new bookmark
     * @return a newly-created <code>BookmarkAdapter</code> corresponding to <code>ref</code> and <code>range</code>.
     */
    public static BookmarkAdapter createBookmark(String ref, RangeAdapter range) {
        return new Bookmark(ref, (Range)range);
    }

    /**
     * Factory method for creating a new BookmarkAdapter object with text annotation.
     *
     * @param ref the reference for the new bookmark (e.g. "chrX")
     * @param range the range for the new bookmark
     * @param ann the annotation text for the new bookmark
     * @return a newly-created <code>BookmarkAdapter</code> corresponding to <code>ref</code> and <code>range</code>.
     */
    public static BookmarkAdapter createBookmark(String ref, RangeAdapter range, String ann) {
        return new Bookmark(ref, (Range)range, ann);
    }
    
    /**
     * Factory method for creating a new BookmarkAdapter object with text annotation.
     *
     * @param ref the reference for the new bookmark (e.g. "chrX")
     * @param range the range for the new bookmark
     * @param ann the annotation text for the new bookmark
     * @param addMargin if true add padding to range
     * @return a newly-created <code>BookmarkAdapter</code> corresponding to <code>ref</code> and <code>range</code>.
     */
    public static BookmarkAdapter createBookmark(String ref, RangeAdapter range, String ann, boolean addMargin) {
        return new Bookmark(ref, (Range)range, ann, addMargin);
    }
}
