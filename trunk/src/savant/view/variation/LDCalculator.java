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

package savant.view.variation;

import java.util.List;
import javax.swing.SwingWorker;
import savant.api.data.VariantRecord;
import savant.api.data.VariantType;

/**
 * Worker which calculates linkage disequilibrium measures in the background.
 *
 * @author tarkvara
 */
public class LDCalculator extends SwingWorker {
    private final VariationController controller;
    protected final float[][] dPrimes;
    protected final float[][] rSquareds;

    public LDCalculator(VariationController vc, boolean phased) throws OutOfMemoryError {
        controller = vc;
        int n = vc.getData().size();
        dPrimes = phased ? new float[n][n] : null;
        rSquareds = new float[n][n];                
    }

    @Override
    protected Object doInBackground() throws Exception {
        if (dPrimes != null) {
            calculatePhased();
        } else {
            calculateUnphased();
        }
        return null;
    }
    
    public void calculatePhased() {
        List<VariantRecord> data = controller.getData();
        int participantCount = controller.getParticipantCount();
        for (int i = 0; i < data.size(); i++) {
            VariantRecord recI = (VariantRecord)data.get(i);
            VariantType varI = recI.getVariantType();
            int n1 = 0;
            double count = participantCount * 2.0;
            for (int k = 0; k < participantCount; k++) {
                n1 += countMatches(recI.getVariantsForParticipant(k), varI);
            }
            double p1 = n1 / count;
            double p2 = 1.0 - p1;

            for (int j = i + 1; j < data.size(); j++) {
                if (p1 > 0.0 && p1 < 1.0) {
                    VariantRecord recJ = (VariantRecord)data.get(j);
                    VariantType varJ = recJ.getVariantType();

                    n1 = 0;
                    int n11 = 0;
                    for (int k = 0; k < participantCount; k++) {
                        VariantType[] varJK = recJ.getVariantsForParticipant(k);
                        VariantType[] varIK = recI.getVariantsForParticipant(k);
                        if (varJK != null && varIK != null) {
                            if (varJK.length == 1) {
                                if (varJK[0] == varJ) {
                                    n1 += 2;
                                    n11 += countMatches(varIK, varI);
                                }
                            } else {
                                if (varJK[0] == varJ) {
                                    n1++;
                                    if (varIK[0] == varI) {  // Comparison works for either hetero- and homo-zygous.
                                        n11++;
                                    }
                                } else if (varJK[1] == varJ) {
                                    n1++;
                                    if ((varIK.length == 1 && varIK[0] == varI) || (varIK.length == 2 && varIK[1] == varI)) {
                                        n11++;
                                    }
                                }
                            }
                        }
                    }
                    double q1 = n1 / count;
                    double q2 = 1.0 - q1;
                    if (q1 > 0.0 && q1 < 1.0) {
                        double x11 = n11 / count;
                        double d = x11 - p1 * q1;
                        //TODO: something is wrong when x11 is 0, a possible solution ->    double d = x11 == 0 ? 0 : x11 - p1 * q1;

                        // D'
                        double dMax = d < 0.0 ? -Math.min(p1 * q1, p2 * q2) : Math.min(p1 * q2, p2 * q1);
                        dPrimes[i][j] = (float)(d / dMax);
                        rSquareds[i][j] = (float)(d * d / (p1 * p2 * q1 * q2));
                    } else {
                        dPrimes[i][j] = Float.NaN;
                        rSquareds[i][j] = Float.NaN;
                    }
                } else {
                    dPrimes[i][j] = Float.NaN;
                    rSquareds[i][j] = Float.NaN;
                }
            }
            showProgress((double)i / data.size());
        }
    }

    private int countMatches(VariantType[] participantTypes, VariantType target) {
        int result = 0;
        if (participantTypes != null) {
            if (participantTypes.length == 1) {
                if (participantTypes[0] == target) {
                    result = 2;
                }
            } else {
                if (participantTypes[0] == target) {
                    result = 1;
                }
                if (participantTypes[1] == target) {
                    result++;
                }
            }
        }
        return result;
    }

    public void calculateUnphased() {
        List<VariantRecord> data = controller.getData();
        int participantCount = controller.getParticipantCount();
        for (int i = 0; i < data.size(); i++) {
            VariantRecord recI = (VariantRecord)data.get(i);

            for (int j = i + 1; j < data.size(); j++) {
                VariantRecord recJ = (VariantRecord)data.get(j);

                double sumI = 0.0;
                double sumJ = 0.0;
                double squaresI = 0.0;
                double squaresJ = 0.0;
                double prodIJ = 0.0;
                for (int k = 0; k < participantCount; k++) {
                    VariantType[] varIK = recI.getVariantsForParticipant(k);
                    VariantType[] varJK = recJ.getVariantsForParticipant(k);
                    int countI = 0;
                    int countJ = 0;

                    if (varIK.length == 1) {
                        if (varIK[0] == VariantType.NONE) {
                            countI = 2;
                        }
                    } else {
                        countI = 1;
                    }
                    
                    if (varJK.length == 1) {
                        if (varJK[0] == VariantType.NONE) {
                            countJ = 2;
                        }
                    } else {
                        countJ = 1;
                    }

                    sumI += countI;
                    sumJ += countJ;
                    prodIJ += countI * countJ;
                    squaresI += countI * countI;
                    squaresJ += countJ * countJ;
                }
                sumI /= participantCount;
                squaresI /= participantCount;
                sumJ /= participantCount;
                squaresJ /= participantCount;
                prodIJ  /= participantCount;

                double varI = squaresI - sumI * sumI;
                double varJ = squaresJ - sumJ * sumJ;
                double covIJ = prodIJ - sumI * sumJ;

                rSquareds[i][j] = (float)(covIJ * covIJ / (varI * varJ));
            }
            showProgress((double)i / data.size());
        }
    }

    protected void showProgress(double fract) {
    }
}
