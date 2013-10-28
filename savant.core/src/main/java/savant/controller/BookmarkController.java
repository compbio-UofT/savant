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

import savant.api.event.BookmarksChangedEvent;
import savant.util.Bookmark;
import savant.util.Controller;
import savant.util.Range;


/**
 * Controller object to manage changes to bookmarks.
 *
 * @author mfiume
 */
public class BookmarkController extends Controller<BookmarksChangedEvent> {
    private static final Log LOG = LogFactory.getLog(LocationController.class);

    private static BookmarkController instance;

    private List<Bookmark> bookmarks;

    public static synchronized BookmarkController getInstance() {
        if (instance == null) {
            instance = new BookmarkController();
        }
        return instance;
    }

    private BookmarkController() {
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
        if(fireEvent) {
            fireEvent(new BookmarksChangedEvent(f,true));
        }
    }

    public void addBookmarks(List<Bookmark> bkmks){
        for (Bookmark b : bkmks) {
            addBookmark(b, false);
        }
        fireEvent(new BookmarksChangedEvent(bkmks.get(bkmks.size()-1), true));
    }
    
     private static Bookmark parseBookmark(String line, boolean addMargin) {

        StringTokenizer st = new StringTokenizer(line,"\t");

        String ref = st.nextToken();
        int from = Integer.parseInt(st.nextToken());
        int to = Integer.parseInt(st.nextToken());
        String annotation = "";

        if (st.hasMoreElements()) {
            annotation = st.nextToken();
            annotation.trim();
        }

        return new Bookmark(ref, new Range(from,to), annotation, addMargin);
    }

     public void addBookmarksFromFile(File f, boolean addMargin) throws FileNotFoundException, IOException {

        BufferedReader br = new BufferedReader(new FileReader(f));

        String line = "";

        List<Bookmark> newBookmarks = new ArrayList<Bookmark>();

        while ((line = br.readLine()) != null) {
            newBookmarks.add(parseBookmark(line, addMargin));
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
            fireEvent(new BookmarksChangedEvent(b, false));
        } catch(Exception e) {}
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
