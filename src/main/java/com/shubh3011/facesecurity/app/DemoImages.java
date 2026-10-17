package com.shubh3011.facesecurity.app;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import javax.imageio.ImageIO;

/**
 * Deterministic synthetic face generator.
 *
 * <p>The suite must be demonstrable and testable without shipping anyone's
 * biometric data. These images are drawn from a seed: the same seed always yields
 * the same face, which makes the recognition tests reproducible. Each identity gets
 * a distinct head geometry, hue, rotation and a set of random details, while
 * {@link #variant(BufferedImage, int)} produces a second "capture" of the same
 * identity with a different exposure — exactly the mild variation a matcher must
 * tolerate.
 */
public final class DemoImages {

    private DemoImages() {
    }

    public static BufferedImage face(int seed, int size) {
        if (size < 32) {
            throw new IllegalArgumentException("size must be at least 32");
        }
        Random random = new Random(seed);
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Background with a per-identity stripe texture, so identities differ in
            // spatial structure and not only in skin tone.
            int base = 18 + random.nextInt(20);
            g.setColor(new Color(base, base + 5, base + 12));
            g.fillRect(0, 0, size, size);

            double stripeAngle = random.nextDouble() * Math.PI;
            int stripeSpacing = Math.max(6, size / (3 + random.nextInt(4)));
            int tint = 26 + random.nextInt(46);
            g.setColor(new Color(base + tint, base + tint, base + tint + 8));
            g.setStroke(new BasicStroke(1.5f + random.nextFloat() * 3f));
            AffineTransform background = g.getTransform();
            g.rotate(stripeAngle, size / 2.0, size / 2.0);
            for (int x = -size; x < size * 2; x += stripeSpacing) {
                g.drawLine(x, -size, x, size * 2);
            }
            g.setTransform(background);

            // Per-identity rotation of the head itself.
            double rotation = Math.toRadians(random.nextDouble() * 34.0 - 17.0);
            AffineTransform original = g.getTransform();
            g.rotate(rotation, size / 2.0, size / 2.0);

            int headWidth = (int) (size * (0.36 + random.nextDouble() * 0.36));
            int headHeight = (int) (headWidth * (1.05 + random.nextDouble() * 0.25));
            int headX = (size - headWidth) / 2 + random.nextInt(Math.max(1, size / 3)) - size / 6;
            int headY = (size - headHeight) / 2 + random.nextInt(Math.max(1, size / 3)) - size / 6;

            // Hair mass behind the head.
            g.setColor(Color.getHSBColor(random.nextFloat(), 0.45f, 0.20f + random.nextFloat() * 0.25f));
            g.fillOval(headX - 3, headY - headHeight / 6, headWidth + 6, headHeight + headHeight / 4);

            // Skin tone.
            float hue = 0.03f + random.nextFloat() * 0.08f;
            float saturation = 0.28f + random.nextFloat() * 0.22f;
            float brightness = 0.55f + random.nextFloat() * 0.30f;
            g.setColor(Color.getHSBColor(hue, saturation, brightness));
            g.fillOval(headX, headY, headWidth, headHeight);

            // Eyes.
            int eyeSize = Math.max(4, (int) (headWidth * (0.11 + random.nextDouble() * 0.06)));
            int eyeGap = (int) (headWidth * (0.20 + random.nextDouble() * 0.12));
            int eyeY = headY + (int) (headHeight * (0.36 + random.nextDouble() * 0.06));
            int eyeCenterX = headX + headWidth / 2;
            g.setColor(new Color(0x0B, 0x0F, 0x16));
            g.fillOval(eyeCenterX - eyeGap - eyeSize / 2, eyeY - eyeSize / 2, eyeSize, eyeSize);
            g.fillOval(eyeCenterX + eyeGap - eyeSize / 2, eyeY - eyeSize / 2, eyeSize, eyeSize);

            // Nose.
            int noseWidth = Math.max(3, (int) (headWidth * 0.10));
            int noseHeight = Math.max(4, (int) (headHeight * 0.14));
            g.setColor(Color.getHSBColor(hue, saturation + 0.08f, Math.max(0.1f, brightness - 0.12f)));
            g.fillOval(eyeCenterX - noseWidth / 2, eyeY + (int) (headHeight * 0.14), noseWidth, noseHeight);

            // Mouth.
            int mouthWidth = (int) (headWidth * (0.30 + random.nextDouble() * 0.16));
            int mouthHeight = Math.max(4, (int) (headHeight * 0.08));
            g.setColor(new Color(0x8A + random.nextInt(40), 0x2F, 0x38));
            g.fillOval(eyeCenterX - mouthWidth / 2, headY + (int) (headHeight * 0.70), mouthWidth, mouthHeight);

            // Random distinctive details (freckles / accessories) that individualise a seed.
            int details = 3 + random.nextInt(4);
            for (int i = 0; i < details; i++) {
                int detailSize = Math.max(3, (int) (headWidth * (0.06 + random.nextDouble() * 0.10)));
                int dx = headX + random.nextInt(Math.max(1, headWidth));
                int dy = headY + random.nextInt(Math.max(1, headHeight));
                g.setColor(new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256), 200));
                g.fillOval(dx, dy, detailSize, detailSize);
            }

            g.setTransform(original);
        } finally {
            g.dispose();
        }
        return image;
    }

    /**
     * A second capture of an existing identity: brightness shift plus sensor noise,
     * with no change to the underlying structure.
     */
    public static BufferedImage variant(BufferedImage base, int seed) {
        Random random = new Random(seed);
        int width = base.getWidth();
        int height = base.getHeight();
        double brightness = 0.90 + random.nextDouble() * 0.20;
        int noise = 3;
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = base.getRGB(x, y);
                int a = (argb >>> 24) & 0xFF;
                int r = clampChannel((int) (((argb >> 16) & 0xFF) * brightness) + random.nextInt(2 * noise + 1) - noise);
                int g = clampChannel((int) (((argb >> 8) & 0xFF) * brightness) + random.nextInt(2 * noise + 1) - noise);
                int b = clampChannel((int) ((argb & 0xFF) * brightness) + random.nextInt(2 * noise + 1) - noise);
                out.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return out;
    }

    public static Path writePng(Path file, BufferedImage image) throws IOException {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        ImageIO.write(image, "png", file.toFile());
        return file;
    }

    private static int clampChannel(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
