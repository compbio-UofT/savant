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
