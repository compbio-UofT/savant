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

package savant.api.adapter;

import java.net.URI;
import java.util.List;
import savant.data.sources.DataSource;
import savant.file.DataFormat;
import savant.util.Resolution;

/**
 *
 * @author mfiume
 */
public interface ViewTrackAdapter {

    // constructors
    //public static List<ViewTrack> create(URI trackURI) throws IOException {
    //public static Genome createGenome(URI genomeURI) throws IOException, SavantFileNotFormattedException, SavantUnsupportedVersionException {
    //public ViewTrack(String name, FileFormat dataType, DataSource dataSource) {

    // questionable
    /*
    public ColorScheme getColorScheme();
    public void setColor(String name, Color color);
    public DrawingInstructions getDrawingInstructions() {
    public void setDrawingInstructions(DrawingInstructions di) {
    public abstract List<Object> retrieveData(String reference, Range range, Resolution resolution) throws Throwable;
    public void addTrackRenderer(TrackRenderer renderer) {
    public List<TrackRenderer> getTrackRenderers() {
    public void setColorScheme(ColorScheme cs);
    public abstract void resetColorScheme();
    public Frame getFrame();
     */

    // api
    public DataFormat getDataType();
    public String getName();
    public List<Object> getDataInRange();
    public List<ModeAdapter> getDrawModes();
    public ModeAdapter getDrawMode();
    public void setDrawMode(ModeAdapter mode);
    public void setDrawMode(String modename);
    public DataSource getDataSource();
    public Resolution getResolution(RangeAdapter range);
    public ModeAdapter getDefaultDrawMode();
    public URI getURI();

}
