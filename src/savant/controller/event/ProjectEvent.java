/*
 *    Copyright 2011 University of Toronto
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

import java.io.File;


/**
 * Event class which is fired by the ProjectController when the current project changes.
 *
 * @author tarkvara
 */
public class ProjectEvent {
    private final Type type;
    private final String path;

    public ProjectEvent(Type type, File f) {
        this.type = type;
        this.path = f.getAbsolutePath();
    }

    public Type getType() {
        return type;
    }

    public String getPath() {
        return path;
    }

    public enum Type {
        LOADING,
        LOADED,
        SAVING,
        SAVED,
        UNSAVED
    }
}
