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

import java.util.List;
import javax.swing.JPanel;
import savant.api.data.VariantRecord;
import savant.api.data.VariantType;

/**
 * A Linkage Disequilibrium plot.
 *
 * @author tarkvara
 */
public class LDPlot extends JPanel {
    double[][] ldData;

    VariationPanel owner;

    LDPlot(VariationPanel owner) {
        this.owner = owner;
    }

    public double[][] getLDData() {
        if (ldData == null) {
            List<VariantRecord> data = owner.getData();
            int participantCount = owner.getParticipantCount();

            ldData = new double[data.size()][data.size()];
            for (int i = 0; i < data.size(); i++) {
                VariantRecord recI = (VariantRecord)data.get(i);
                VariantType var = recI.getVariantType();
                int n1 = 0;
                for (int k = 0; k < participantCount; k++) {
                    if (recI.getVariantForParticipant(k) == var) {
                        n1++;
                    }
                }
                double p1 = n1 / (double)participantCount;
                double p2 = 1.0 - p1;

                for (int j = i + 1; j < data.size(); j++) {
                    VariantRecord recJ = (VariantRecord)data.get(j);

                    n1 = 0;
                    int n11 = 0;
                    for (int k = 0; k < participantCount; k++) {
                        if (recJ.getVariantForParticipant(k) == var) {
                            n1++;
                            if (recI.getVariantForParticipant(k) == var) {
                                n11++;
                            }
                        }
                    }
                    double q1 = n1 / (double)participantCount;
                    double q2 = 1.0 - q1;
                    double x11 = n11 / (double)participantCount;
                    double d = x11 - p1 * q1;
                    double dMax = d < 0.0 ? Math.min(p1 * q1, p2 * q2) : Math.min(p1 * q2, p2 * q1);
                    double dPrime = d / dMax;
                    ldData[i][j] = ldData[j][i] = dPrime;
                }
            }
        }
        return ldData;
    }
}
