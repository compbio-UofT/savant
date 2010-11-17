/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.plugin.util;

import java.util.List;
import savant.controller.BookmarkController;
import savant.controller.event.bookmark.BookmarksChangedListener;
import savant.util.Bookmark;

/**
 *
 * @author mfiume
 */
public class BookmarkUtils {

    /**
     * Get a list of the current bookmarks
     * @return A list of bookmarks
     */
    public static List<Bookmark> getBookmarks() {
        return BookmarkController.getInstance().getBookmarks();
    }

    /**
     * Add a bookmark
     * @param f The bookmark to add
     */
    public static void addBookmark(Bookmark f) {
        BookmarkController.getInstance().addBookmark(f);
    }

    /**
     * Remove the last bookmark
     */
    public static void removeLastBookmark() {
        BookmarkController.getInstance().removeBookmark();
    }

    /**
     * Remove some bookmark
     * @param index Index of the bookmark to remove
     */
    public static void removeBookmark(int index) {
        BookmarkController.getInstance().removeBookmark(index);
    }

    /**
     * Subscribe a listener to be notified when the Bookmarks list changes
     * @param l The listener to subscribe
     */
    public static void addBookmarksChangedListener(BookmarksChangedListener l) {
        BookmarkController.getInstance().addFavoritesChangedListener(l);
    }

    /**
     * Unsubscribe a listener from being notified when the Bookmarks list changes
     * @param l The listener to unsubscribe
     */
    public static void removeFavoritesChangedListener(BookmarksChangedListener l) {
        BookmarkController.getInstance().removeFavoritesChangedListener(l);
    }

    /**
     * Add the current range as a bookmark
     */
    public static void addCurrentRangeToBookmarks() {
        BookmarkController.getInstance().addCurrentRangeToBookmarks();
    }

    /**
     * Get a bookmark from the bookmark list
     * @param index The index of the bookmark to get
     * @return The bookmark at the specified index
     */
    public static Bookmark getBookmark(int index) {
        return BookmarkController.getInstance().getBookmark(index);
    }

    /**
     * Clear the bookmarks list
     */
    public static void clearBookmarks() {
        BookmarkController.getInstance().clearBookmarks();
    }
}
