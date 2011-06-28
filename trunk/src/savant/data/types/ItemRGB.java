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

package savant.data.types;

import java.awt.Color;

/**
 * Immutable lightweight class to represent an RGB colour.
 *
 * @author mfiume
 */
public final class ItemRGB {

    private final int red;
    private final int blue;
    private final int green;

    /**
     * Constructor. Applications should use the static factory method valueOf() instead.
     *
     * @param r red component, between 0 and 255
     * @param g green component, between 0 and 255
     * @param b blue component, between 0 and 255
     */
    public ItemRGB(int r, int g, int b) {
        this.red = r;
        this.green = g;
        this.blue = b;
    }

    /**
     * Static factory method for constructing an object from 3 colour components
     * @param r red component, between 0 and 255
     * @param g green component, between 0 and 255
     * @param b blue component, between 0 and 255
     * @return a new ItemRGB
     */
    public static ItemRGB valueOf(int r, int g, int b) {
        return new ItemRGB(r, g, b);
    }

    /**
     * Parse a token which contains an ItemRGB value.  
     * @param token
     * @return
     */
    public static ItemRGB parseItemRGB(String token) {
        String[] values = token.split(",");
        if (values.length==3){
            try {
                return valueOf(
                        Integer.parseInt(values[0].trim()),
                        Integer.parseInt(values[1].trim()),
                        Integer.parseInt(values[2].trim()));
            } catch (NumberFormatException e){}
        } else if (token.equals("0")) {
            // A lot of bed files just store a zero in this column.
            return valueOf(0, 0, 0);
        }
        //if null or bad info, denote with negative values
        return valueOf(-1, -1, -1);
    }


    public int getRed() {
        return this.red;
    }

    public int getGreen() {
        return green;
    }

    public int getBlue() {
        return blue;
    }

    public boolean isNull(){
        return red < 0 || green < 0 || blue < 0;
    }

    public Color createColor(){
        return new Color(red, green, blue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ItemRGB itemRGB = (ItemRGB) o;

        if (blue != itemRGB.blue) return false;
        if (green != itemRGB.green) return false;
        if (red != itemRGB.red) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = red;
        result = 31 * result + blue;
        result = 31 * result + green;
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("red=").append(red);
        sb.append(", blue=").append(blue);
        sb.append(", green=").append(green);
        return sb.toString();
    }
}
