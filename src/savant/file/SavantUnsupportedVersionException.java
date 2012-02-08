/*
 *    Copyright 2010-2012 University of Toronto
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
package savant.file;

import java.io.IOException;


/**
 * Exception thrown when old 1.x files have a different version than expected.
 * 
 * @deprecated No longer relevant as of Savant 2.
 */
public final class SavantUnsupportedVersionException extends IOException {

    private final int givenVersion;
    private final String supportedVersions;

    public SavantUnsupportedVersionException(int givenVersion, String supportedVersions) {
        super();
        this.givenVersion = givenVersion;
        this.supportedVersions = supportedVersions;
    }

    public SavantUnsupportedVersionException(int givenVersion, String supportedVersions, String msg) {
        super(msg);
        this.givenVersion = givenVersion;
        this.supportedVersions = supportedVersions;
    }

    public SavantUnsupportedVersionException(int givenVersion, String supportedVersions, String msg, Throwable t) {
        super(msg, t);
        this.givenVersion = givenVersion;
        this.supportedVersions = supportedVersions;
    }

    public SavantUnsupportedVersionException(int givenVersion, String supportedVersions, Throwable t) {
        super(t);
        this.givenVersion = givenVersion;
        this.supportedVersions = supportedVersions;
    }

    public int getGivenVersion() {
        return givenVersion;
    }

    public String getSupportedVersions() {
        return supportedVersions;
    }
}
