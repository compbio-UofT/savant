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
package savant.pathways;

/**
 *
 * @author AndrewBrook
 */
public class Gene {

    //database
    public static enum geneType{ ENTREZ, ENSEMBL };

    private String chromosome;
    private int start = -1;
    private int end = -1;
    private String name;
    private String description;
    private String id;
    private geneType type;

    public Gene(geneType type, String id){
        this.type = type;
        this.id = id;
    }

    public Gene(String chrom, String start, String end){
        this.chromosome = chrom;
        this.start = Integer.parseInt(start);
        this.end = Integer.parseInt(end);
    }

    public void setChromosome(String chrom){
        this.chromosome = chrom;
    }

    public void setStart(String start){
        this.start = Integer.parseInt(start);
    }

    public void setEnd(String end){
        this.end = Integer.parseInt(end);
    }

    public void setName(String name){
        this.name = name;
    }

    public void setDescription(String desc){
        this.description = desc;
    }

    public void setId(String id){
        this.id = id;
    }

    public void setGeneType(geneType type){
        this.type = type;
    }

    public String getChromosome(){
        return this.chromosome;
    }

    public int getStart(){
        return this.start;
    }

    public int getEnd(){
        return this.end;
    }

    public String getName(){
        return this.name;
    }

    public String getDescription(){
        return this.description;
    }

    public String getId(){
        return this.id;
    }

    public geneType getGeneType(){
        return this.type;
    }

}
