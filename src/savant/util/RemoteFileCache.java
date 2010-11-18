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

package savant.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import savant.settings.DirectorySettings;

/**
 *
 * @author AndrewBrook
 */
public class RemoteFileCache {

    public static void clearCache(){

        //check if index exists
        String dir = DirectorySettings.getCacheDirectory();
        String sep = System.getProperty("file.separator");
        String indexName = dir + sep + "cacheIndex";
        File index = new File(indexName);
        if(!index.exists())
            return;

        //remove all cached files
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(indexName));
        } catch (FileNotFoundException ex) {
            Logger.getLogger(RemoteFileCache.class.getName()).log(Level.SEVERE, "FileNotFound: " + indexName, ex);
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
        } catch (IOException ex) {
            Logger.getLogger(RemoteFileCache.class.getName()).log(Level.SEVERE, "Error reading: " + indexName, ex);
        }

        //remove index
        boolean deleted = index.delete();
        if(!deleted)
            index.deleteOnExit();
    }


}


