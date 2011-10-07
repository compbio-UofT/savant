/*
 *    Copyright 2010-2011 University of Toronto
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

package savant.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.api.util.DialogUtils;
import savant.controller.TrackController;
import savant.settings.DirectorySettings;
import savant.view.tracks.Track;

/**
 *
 * @author AndrewBrook
 */
public class RemoteFileCache {
    private static final Log LOG = LogFactory.getLog(RemoteFileCache.class);

    public static void clearCache(){

        //Make sure no remote files are currently open. If so, display warning. 
        List<Track> tracks = TrackController.getInstance().getTracks();
        for(int i = 0; i < tracks.size(); i++){
            URI uri = tracks.get(i).getDataSource().getURI();
            String scheme = uri.getScheme().toLowerCase();
            if (!scheme.equals("file")) {
                DialogUtils.displayMessage("Cannot clear cache", "You have one or more remote files currently open. Close them and try again.");
                return;
            }
        }

        //check if index exists
        File cacheDir = DirectorySettings.getCacheDirectory();
        File index = new File(cacheDir, "cacheIndex");
        if(!index.exists())
            return;

        //remove all cached files
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(index));
        } catch (FileNotFoundException ex) {
            LOG.error("File not found: " + index, ex);
        }
        String line = null;
        try {
            while ((line = bufferedReader.readLine()) != null) {
                String[] lineArray = line.split(",");
                String currFileName = lineArray[4];
                File currFile = new File(currFileName);
                boolean deleted = currFile.delete();
                //if we can't delete the file, try again when the program terminates
                if (!deleted) {
                    currFile.deleteOnExit();
                }
            }
            bufferedReader.close();
        } catch (IOException ex) {
            LOG.error("Error reading: " + index, ex);
        }

        //remove index
        boolean deleted = index.delete();
        if (!deleted) {
            index.deleteOnExit();
        }
    }
}


