/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package savant.util;

/**
 *
 * @author mfiume
 */
public class ItemRGB {
        int red;
        int blue;
        int green;

        public ItemRGB(int r, int g, int b) {
            this.red = r;
            this.green = g;
            this.blue = b;
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
}
