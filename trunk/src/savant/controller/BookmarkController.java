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

package savant.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.controller.event.BookmarksChangedEvent;
import savant.controller.event.BookmarksChangedListener;
import savant.util.Bookmark;
import savant.util.Range;


/**
 * Controller object to manage changes to bookmarks.
 *
 * @author mfiume
 */
public class BookmarkController {
    private static final Log LOG = LogFactory.getLog(LocationController.class);


    private static BookmarkController instance;

    private List<Bookmark> bookmarks;

    private List<BookmarksChangedListener> bookmarksChangedListeners;

    public static synchronized BookmarkController getInstance() {
        if (instance == null) {
            instance = new BookmarkController();
        }
        return instance;
    }

    private BookmarkController() {
        bookmarksChangedListeners = new ArrayList<BookmarksChangedListener>();
        bookmarks = new ArrayList<Bookmark>();
    }

    public List<Bookmark> getBookmarks() {
        return bookmarks;
    }

    public void addBookmark(Bookmark f) {
        addBookmark(f, true);
    }

    public void addBookmark(Bookmark f, boolean fireEvent) {
        if (bookmarks == null || bookmarks.isEmpty()) { bookmarks = new ArrayList<Bookmark>(); }
        bookmarks.add(f);
        if(fireEvent) fireBookmarksChangedEvent(f,true);
    }

    public void addBookmarks(List<Bookmark> bkmks){
        for (Bookmark b : bkmks) {
            addBookmark(b, false);
        }
        this.fireBookmarksChangedEvent(bkmks.get(bkmks.size()-1), true);
    }
    
     private static Bookmark parseBookmark(String line) {

        StringTokenizer st = new StringTokenizer(line,"\t");

        String ref = st.nextToken();
        int from = Integer.parseInt(st.nextToken());
        int to = Integer.parseInt(st.nextToken());
        String annotation = "";

        while (st.hasMoreElements()) {
            annotation += st.nextToken() + " ";
        }
        annotation.trim();

        return new Bookmark(ref, new Range(from,to), annotation);
    }

     public void addBookmarksFromFile(File f) throws FileNotFoundException, IOException {

        BufferedReader br = new BufferedReader(new FileReader(f));

        String line = "";

        List<Bookmark> newBookmarks = new ArrayList<Bookmark>();

        while ((line = br.readLine()) != null) {
            newBookmarks.add(parseBookmark(line));
        }

        //bookmarks.addAll(newBookmarks);
        addBookmarks(newBookmarks);

        br.close();
    }

    public void removeBookmark() {
        this.removeBookmark(bookmarks.size()-1);
    }

    public void removeBookmark(int index) {
        try {
            LOG.info("Bookmark removed.");
            Bookmark b = bookmarks.get(index);
            bookmarks.remove(index);
            fireBookmarksChangedEvent(b,false);
        } catch(Exception e) {}
    }

    /**
     * Fire the RangeChangedEvent
     */
    private synchronized void fireBookmarksChangedEvent(Bookmark bkmk, boolean isAdded) {
        BookmarksChangedEvent evt = new BookmarksChangedEvent(this, bkmk, isAdded);
        for (BookmarksChangedListener listener : bookmarksChangedListeners) {
            listener.bookmarksChanged(evt);
        }
    }

    public synchronized void addBookmarksChangedListener(BookmarksChangedListener l) {
        bookmarksChangedListeners.add(l);
    }

    public synchronized void removeBookmarksChangedListener(BookmarksChangedListener l) {
        bookmarksChangedListeners.remove(l);
    }

    public void addCurrentRangeToBookmarks() {
        LocationController lc = LocationController.getInstance();
        if (lc.getRange() != null) {
            addBookmark(new Bookmark(lc.getReferenceName(), lc.getRange()));
        }
    }

    public Bookmark getBookmark(int index) {
        return bookmarks.get(index);
    }

    public void clearBookmarks() {
        bookmarks.clear();
    }
}
