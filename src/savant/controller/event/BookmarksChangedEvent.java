/*
 *    Copyright 2009-2010 University of Toronto
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
package savant.controller.event;

import savant.util.Bookmark;

import java.util.EventObject;
import java.util.List;

/**
 *
 * @author mfiume
 */
public class BookmarksChangedEvent extends EventObject {

    //private List<Bookmark> bookmarks;
    //private String message;
    private Bookmark changedbkmk;
    private boolean isAdded;
    //private boolean isSilent;

    /**
     *
     * @param source
     * @param changedbkmk
     * @param isAdded true iff bookmark was added, false if removed
     * @param isSilent
     */
    public BookmarksChangedEvent(Object source, Bookmark changedbkmk, boolean isAdded) {
        super(source);
        this.changedbkmk = changedbkmk;
        this.isAdded = isAdded;
       // this.isSilent = isSilent;
    }

    public boolean isAdded() {
        return this.isAdded;
    }

    public Bookmark getBookmark() {
        return this.changedbkmk;
    }

    /*
    public boolean isSilent() {
        return this.isSilent();
    }
     * 
     */
}