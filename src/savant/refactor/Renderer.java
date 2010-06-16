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
 * Renderer.java
 * Created on Jun 16, 2010
 */

package savant.refactor;

import savant.model.view.DrawingInstructions;

/**
 * Class to render some data from a data source
 *
 * @author vwilliams
 */
public abstract class Renderer {

    private DataSource dataSource;
    private Track track;
    private DrawingInstructions drawingInstructions;

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Track getTrack() {
        return track;
    }

    public void setTrack(Track track) {
        this.track = track;
    }

    public DrawingInstructions getDrawingInstructions() {
        return drawingInstructions;
    }

    public void setDrawingInstructions(DrawingInstructions drawingInstructions) {
        this.drawingInstructions = drawingInstructions;
    }
}
