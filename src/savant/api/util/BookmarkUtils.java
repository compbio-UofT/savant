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

package savant.api.util;

import savant.api.adapter.BookmarkAdapter;
import savant.api.adapter.RangeAdapter;
import savant.controller.BookmarkController;
import savant.controller.event.BookmarksChangedListener;
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
    public static void addBookmarksChangedListener(BookmarksChangedListener l) {
        controller.addFavoritesChangedListener(l);
    }

    /**
     * Unsubscribe a listener from being notified when Savant's bookmark list changes.
     *
     * @param l the listener to unsubscribe
     */
    public static void removeBookmarksChangedListener(BookmarksChangedListener l) {
        controller.removeFavoritesChangedListener(l);
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
}
