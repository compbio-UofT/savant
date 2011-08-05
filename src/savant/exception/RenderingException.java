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

package savant.exception;

/**
 * Exception class which indicates that a renderer was unable to render the given
 * track.
 *
 * @author tarkvara
 */
public class RenderingException extends Exception {
    private final int priority;

    /**
     * Construct a new RenderingException.
     *
     * @param message the error message to be rendered
     * @param priority higher-priority messages will override lower-priority ones.
     */
    public RenderingException(String message, int priority) {
        super(message);
        this.priority = priority;
    }

    public int getPriority() {
        return priority;
    }
}
