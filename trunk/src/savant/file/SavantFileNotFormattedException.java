/*
 *    Copyright 2010 University of Toronto
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

/*
 * SavantFileNotFormattedException.java
 * Created on Sep 8, 2010
 */

package savant.file;

public class SavantFileNotFormattedException extends Exception {

    public SavantFileNotFormattedException() {
        super();
    }

    public SavantFileNotFormattedException(String msg) {
        super(msg);
    }

    public SavantFileNotFormattedException(String msg, Throwable t) {
        super(msg, t);
    }

    public SavantFileNotFormattedException(Throwable t) {
        super(t);
    }
}
