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

package savant.exception;

import java.net.MalformedURLException;
import java.net.URI;


/**
 * Thrown if the URI does not have a known scheme.
 *
 * @author tarkvara
 */
public class UnknownSchemeException extends MalformedURLException {
    public UnknownSchemeException(URI uri) {
        super(String.format("%s not recognised as a known URI scheme.", uri));
    }
}
