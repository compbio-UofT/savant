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
package savant.view.swing.variation;

import java.awt.Point;

import savant.api.data.Record;
import savant.api.data.VariantRecord;

/**
 * Shared functionality of all our variation-related plots which allows us to display popups appropriately.
 * 
 * @author tarkvara
 */
public interface VariationPanel {
    /**
     * Returns either a synthesised ParticipantRecord or the VariantRecord associated with this point.
     */
    public Record pointToRecord(Point hoverPos);

    /**
     * Returns the VariantRecord associated with this point.
     */
    public VariantRecord pointToVariantRecord(Point hoverPos);
}
