import java.io.File;
import java.io.IOException;
import javax.sound.sampled.*;

public class SoundManager {
    private static Clip scoreClip;
    private static Clip explosionClip;
    
    static {
        try {

            AudioInputStream scoreStream = AudioSystem.getAudioInputStream(
                new File("sounds/purchase.wav"));
            scoreClip = AudioSystem.getClip();
            scoreClip.open(scoreStream);
            

            AudioInputStream explosionStream = AudioSystem.getAudioInputStream(
                new File("sounds/purchase.wav"));
            explosionClip = AudioSystem.getClip();
            explosionClip.open(explosionStream);
            
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("Không thể tải file âm thanh: " + e.getMessage());
        }
    }
    
    public static void playScore() {
        if (scoreClip != null) {
            scoreClip.setFramePosition(0);
            scoreClip.start();
        }
    }
    
    public static void playExplosion() {
        if (explosionClip != null) {
            explosionClip.setFramePosition(0);
            explosionClip.start();
        }
    }
} 