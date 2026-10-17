package com.shubh3011.facesecurity.face;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

/**
 * Loads images into a predictable {@link BufferedImage#TYPE_INT_ARGB} form so the
 * feature extractor never has to worry about the source colour model.
 */
public final class ImageLoader {

    private ImageLoader() {
    }

    public static BufferedImage load(Path path) throws ImageLoadException {
        if (path == null) {
            throw new ImageLoadException("image path must not be null");
        }
        if (!Files.isRegularFile(path)) {
            throw new ImageLoadException("image not found: " + path);
        }
        try (InputStream in = Files.newInputStream(path)) {
            return read(in, path.toString());
        } catch (IOException e) {
            throw new ImageLoadException("could not read image: " + path, e);
        }
    }

    public static BufferedImage load(byte[] bytes) throws ImageLoadException {
        if (bytes == null || bytes.length == 0) {
            throw new ImageLoadException("image payload is empty");
        }
        return read(new ByteArrayInputStream(bytes), "<bytes>");
    }

    private static BufferedImage read(InputStream in, String description) throws ImageLoadException {
        try (ImageInputStream imageStream = ImageIO.createImageInputStream(in)) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageStream);
            if (!readers.hasNext()) {
                throw new ImageLoadException("unsupported image format: " + description);
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(imageStream);
                BufferedImage image = reader.read(0);
                return toIntArgb(image);
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new ImageLoadException("corrupt image: " + description, e);
        }
    }

    private static BufferedImage toIntArgb(BufferedImage source) {
        if (source.getType() == BufferedImage.TYPE_INT_ARGB) {
            return source;
        }
        BufferedImage converted = new BufferedImage(
                source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = converted.createGraphics();
        try {
            g.drawImage(source, 0, 0, null);
        } finally {
            g.dispose();
        }
        return converted;
    }
}
