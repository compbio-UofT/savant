/*
 *    Copyright 2009-2011 University of Toronto
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
     *
     * @param source
     * @param bm
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