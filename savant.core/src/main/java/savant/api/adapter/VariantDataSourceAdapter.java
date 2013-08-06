/*
 *    Copyright 2012 University of Toronto
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

package savant.api.adapter;

/**
 * Additional functionality which Savant expects of a Variant DataSource which is not presented
 * by the basic DataSourceAdapter adapter.
 *
 * @author tarkvara
 * @since 2.0.1
 */
public interface VariantDataSourceAdapter {
    /**
     * Get the names or IDs of all the participants in this DataSource.
     * @return array of particiapant identifiers
     */
    public String[] getParticipants();
}
