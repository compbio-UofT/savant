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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URI;

import net.sf.samtools.util.SeekableStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import savant.settings.DirectorySettings;

/**
 *
 * @author AndrewBrook
 */
public class CacheableSABS extends SeekableAdjustableBufferedStream {
    //public static final int DEFAULT_BLOCK_SIZE = 65536;
    private static final Log LOG = LogFactory.getLog(CacheableSABS.class);

    private String cacheFile = null;
    private int numBlocks = 0;
    private RandomAccessFile cache = null;
    private int positionInBuff = 0;
    protected InputStream inputStream;
    private URI uri;

    public CacheableSABS(SeekableStream seekable, int bufferSize, URI uri) {
        super(seekable, bufferSize);
        this.uri = uri;
        try {
            this.initCache();
        } catch(IOException e) {
            LOG.error("Unable to initialise cache.", e);
        }
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {

        int posInByteArray = offset;
        int bytesRead = 0;
        while(length > 0){
            //how many we can read from this buffer (ie. how far allow stream is it)
            int canRead = bufferSize - positionInBuff;

            //if we are at the end of the bufferedStream, get a new one
            if(canRead == 0){
                //seek
                this.seek(position);
                //update canRead
                canRead = bufferSize - positionInBuff;
            }

            //Create temporary buffer
            int toRead = Math.min(canRead, length);
            byte[] buff1 = new byte[toRead];

            //read canRead bytes
            int bytesReadThisTime = bufferedStream.read(buff1, 0, toRead);
            System.arraycopy(buff1, 0, buffer, posInByteArray, toRead);

            //prepare for next iteration
            position += bytesReadThisTime;
            posInByteArray += toRead;
            length -= toRead;
            bytesRead += bytesReadThisTime;
            positionInBuff +=bytesReadThisTime;
        }
        return bytesRead;
    }

    private void cachedSeek(int blockOffset, int positionOffset) throws IOException{
        //from offset, calculate actual position in file
        int actualOffset = (numBlocks * 4) + (blockOffset -1) * bufferSize;

        //retrieve from stream
        File file = new File(cacheFile);
        FileInputStream cacheStream = new FileInputStream(file);
        cacheStream.skip(actualOffset);
        bufferedStream = new BufferedInputStream(cacheStream, bufferSize);
        bufferedStream.skip(positionOffset);
        positionInBuff = positionOffset;
    }

    @Override
    public void seek(long position) throws IOException {

        this.position = position;

        //determine which block needs to be accessed
        int block = (int)(position/this.bufferSize);

        //check offset for block
        this.openCache();
        cache.seek(block * 4);
        int offset = cache.readInt();

        if(offset!=0){
            //block is cached
            int positionOffset = (int)(position % bufferSize);
            cachedSeek(offset, positionOffset);
            this.closeCache();
        } else {
            //not cached, seek to start of block
            super.seek(position - (position % bufferSize));

            //cache block
            byte[] b = new byte[bufferSize];
            bufferedStream.read(b, 0, bufferSize); //read buffer into byte[] b
            if(cache == null) openCache(); //sometimes cache is null after above read... TODO: solve this?
            int storeOffset = (int)((cache.length() - (numBlocks * 4))/this.bufferSize)+1; //offset to data in cache
            long actualOffset = cache.length(); //actual pointer to data in cache            
            cache.seek(block * 4); //seek to write offset
            cache.writeInt(storeOffset); //write the offset
            cache.seek(actualOffset); //seek to where data will be written
            cache.write(b); //write data

            //skip to position % buffersize
            positionInBuff = 0;
            this.closeCache();

            //TODO: is this necessary? extra work...
            seek(position);
        }
        
    }

    private void openCache() throws FileNotFoundException{
        File file = new File(cacheFile);
        cache = new RandomAccessFile(file, "rw");
    }

    //TODO: where should this be called?
    private void closeCache() throws IOException{
        cache.close();
        cache = null;
    }

    private void initCache() throws IOException{

        //create index
        String dir = DirectorySettings.getCacheDirectory();
        String sep = System.getProperty("file.separator");
        String indexName = dir + sep + "cacheIndex";
        File index = new File(indexName);
        index.createNewFile();

        //check for entry
        String newETag = NetworkUtils.getHash(uri.toURL());
        boolean entryFound = false;
        BufferedReader bufferedReader = null;
        bufferedReader = new BufferedReader(new FileReader(indexName));
        String line = null;
        while ((line = bufferedReader.readLine()) != null) {
            String[] lineArray = line.split(",");
            String url = lineArray[0];
            if(this.getSource().equals(url)){

                //compare ETags
                String oldETag = lineArray[1];               
                if(!oldETag.equals(newETag)){
                    //file needs to be reloaded
                    //TODO: remove line/delete cache file??
                    continue;
                }

                //compare buffer size
                if(this.bufferSize != Integer.parseInt(lineArray[3])){
                    continue;
                }

                //equivalent entry found
                entryFound = true;
                cacheFile = lineArray[4];
                break;

            }
        }

        //calculate number of blocks in file
        numBlocks = (int)Math.ceil(this.length() / (double)this.bufferSize);

        //add entry
        if(!entryFound){
            BufferedWriter out = new BufferedWriter(new FileWriter(indexName, true));
            cacheFile = dir + sep + this.getSource().replaceAll("[\\:/]", "+");
            out.write(this.getSource() + "," +
                    newETag + "," +   
                    this.length() + "," +
                    this.bufferSize + "," +        //NOTE: bufferSize constant for now
                    cacheFile);                    // replace all instances of \/:*?"<>|
            out.newLine();
            out.close();

            //create the cacheFile
            File file = new File(cacheFile);
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            for(int i = 0; i < numBlocks; i++){
                //write 0x0000
                raf.write(0);
                raf.write(0);
                raf.write(0);
                raf.write(0);
            }
            raf.close();
        }
    }
}
