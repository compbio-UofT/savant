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
