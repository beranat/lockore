/*
 * Copyright (C) 2017 Anatoly madRat L. Berenblit <beranat@users.noreply.github.com>
 *
 * This file is part of madRat's J2ME helpers (madrat.sys).
 *
 * This package is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This package distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with package.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package madrat.sys;

import javax.microedition.lcdui.Image;
import java.util.Hashtable;

/**
 * Image rescaler (nearest, bilinear) and pre-caching class
 */
public final class ImageCache {
    private static final int PREFER_32 = 20;
    private static final String IMAGE_PREFIXES[] = { "-16.png", "-32.png", ".png" };

    public static final int NEAREST_MODE = 0;
    public static final int BILINEAR_MODE = 1;

    private static final Hashtable IMAGE_CACHE = new Hashtable();

    private ImageCache() {
    }

    private static Image rescaleImage(Image source, int width, int height, int mode) {
        if (null == source || (0 >= width && 0 >= height)) {
            return source;
        }

        final int srcWidth = source.getWidth();
        final int srcHeight = source.getHeight();

        if (0 >= width) {
            width = height * srcWidth / srcHeight;
        }

        if (0 >= height) {
            height = width * srcHeight / srcWidth;
        }

        if (height == srcHeight && width == srcWidth) {
            return source;
        }

        int[] src = new int[srcWidth * srcHeight];
        int[] dst = new int[width * height];

        source.getRGB(src, 0, srcWidth, 0, 0, srcWidth, srcHeight);

        switch (mode) {
            case BILINEAR_MODE:
                resizeBilinear(src, srcWidth, srcHeight, dst, width, height);
                break;
            default:
            case NEAREST_MODE:
                resizeNearest(src, srcWidth, srcHeight, dst, width, height);
        }

        return Image.createRGBImage(dst, width, height, true);
    }

    private static void resizeNearest(int[] src, int srcWidth, int srcHeight, int[] dst, int dstWidth, int dstHeight) {

        for (int y = 0, dstIndex = 0; y < dstHeight; ++y) {
            int sy = (y * srcHeight / dstHeight) * srcWidth;
            int sx = 0;
            for (int x = 0; x < dstWidth; ++x, ++dstIndex, sx += srcWidth) {
                dst[dstIndex] = src[sy + sx / dstWidth];
            }
        }
    }

    private static void resizeBilinear(int[] src, int srcWidth, int srcHeight, int[] dst, int dstWidth, int dstHeight) {

        for (int y = 0, dstIndex = 0; y < dstHeight; ++y) {
            final int numY = (srcHeight - 1) * y;
            final int sy = numY / dstHeight;
            final int srcIndex = sy * srcWidth;
            final int diffY8 = (numY << 8) / dstHeight - (sy << 8);

            for (int x = 0; x < dstWidth; ++dstIndex, ++x) {
                final int numX = (srcWidth - 1) * x;
                final int srcX = numX / dstWidth;
                final int diffX8 = (numX << 8) / dstWidth - (srcX << 8);

                final int index = srcIndex + srcX;
                final int a = src[index];
                final int b = src[index + 1];
                final int c = src[index + srcWidth];
                final int d = src[index + srcWidth + 1];

                final int _1w1h = (255 - diffX8) * (255 - diffY8);
                final int _w1h = diffX8 * (255 - diffY8);
                final int _1wh = (255 - diffX8) * diffY8;
                final int _wh = diffX8 * diffY8;

                final int blu = (((a & 0xff) * _1w1h) >> 16) + (((b & 0xff) * _w1h) >> 16)
                        + (((c & 0xff) * _1wh) >> 16) + (((d & 0xff) * _wh) >> 16);
                final int grn = ((((a >> 8) & 0xff) * _1w1h) >> 16) + ((((b >> 8) & 0xff) * _w1h) >> 16)
                        + ((((c >> 8) & 0xff) * _1wh) >> 16) + ((((d >> 8) & 0xff) * _wh) >> 16);
                final int red = ((((a >> 16) & 0xff) * _1w1h) >> 16) + ((((b >> 16) & 0xff) * _w1h) >> 16)
                        + ((((c >> 16) & 0xff) * _1wh) >> 16) + ((((d >> 16) & 0xff) * _wh) >> 16);
                final int alf = ((((a >> 24) & 0xff) * _1w1h) >> 16) + ((((b >> 24) & 0xff) * _w1h) >> 16)
                        + ((((c >> 24) & 0xff) * _1wh) >> 16) + ((((d >> 24) & 0xff) * _wh) >> 16);

                dst[dstIndex] = ((alf & 0xFF) << 24)
                        | ((red & 0xFF) << 16)
                        | ((grn & 0xFF) << 8)
                        | (blu & 0xFF);
            }
        }
    }

    private static Image loadImage(String name, int w, int h, int mode) {
        final int sz = (w >= h) ? w : h;
        final int preferIndex = (sz >= PREFER_32)?1:0;

        for (int i = 0; i < IMAGE_PREFIXES.length; ++i) {
            // 0 -> preferIndex
            // preferIndex -> 0
            final int index = (0 != i)
                                ?((preferIndex == i)?0:i)
                                :preferIndex;
            String filename = "/" + name + IMAGE_PREFIXES[index];
            try {
                return Image.createImage(rescaleImage(Image.createImage(filename), w, h, mode));
            } catch (java.io.IOException e) {
            }
        }
        return null;
    }

    public static final Image getImage(String name, int width) {
        return getImage(name, width, -1, ImageCache.BILINEAR_MODE);
    }

    public static final Image getImage(String name, int width, int height, int mode) {

        if (null == name || 0 == name.length())
            return null;

        if (width <= 0)
            return null;

        final Long value = new Long((((long)width)&0xFFFFFFFFL) | (((long)height)<<32));

        Hashtable cache = (Hashtable) IMAGE_CACHE.get(name);
        if (null == cache) {
            cache = new Hashtable();
            IMAGE_CACHE.put(name, cache);
        }

        Image image = (Image) cache.get(value);
        if (null == image) {
            image = loadImage(name, width, height, mode);
            if (null != image) {
                cache.put(value, image);
            }
        }
        return image;
    }
}
