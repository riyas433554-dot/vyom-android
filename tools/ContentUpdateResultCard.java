import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public final class ContentUpdateResultCard {
    public static void main(String[] args) throws Exception {
        if (args.length != 5) throw new IllegalArgumentException("Usage: version status presence timing output.png");
        int width = 960, height = 540;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(7, 17, 31)); g.fillRect(0, 0, width, height);
        g.setColor(new Color(18, 45, 68)); g.fillRoundRect(54, 42, 852, 456, 30, 30);
        g.setColor(new Color(61, 211, 237)); g.fillOval(90, 86, 58, 58);
        g.setColor(new Color(5, 23, 35)); g.setFont(new Font("SansSerif", Font.BOLD, 30)); g.drawString("V", 109, 126);
        g.setColor(new Color(225, 244, 255)); g.setFont(new Font("SansSerif", Font.BOLD, 34)); g.drawString("CONTENT UPDATE RESULT", 176, 126);
        g.setColor(args[1].equalsIgnoreCase("PASS") ? new Color(87, 220, 150) : new Color(255, 112, 112));
        g.setFont(new Font("SansSerif", Font.BOLD, 28)); g.drawString(args[1].toUpperCase(), 94, 206);
        g.setColor(new Color(176, 199, 216)); g.setFont(new Font("SansSerif", Font.PLAIN, 22)); g.drawString("Version  " + args[0], 94, 258);
        g.drawString("Presence probe  " + args[2], 94, 322);
        g.drawString("Performance  " + args[3], 94, 382);
        g.setColor(new Color(116, 222, 242)); g.setFont(new Font("SansSerif", Font.PLAIN, 17));
        g.drawString("Signed package · staged content · no APK change", 94, 448);
        g.dispose();
        ImageIO.write(image, "png", new File(args[4]));
    }
}
