import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.sound.sampled.*;

public class SoundManager {
    private static final String SCORE_SOUND = "sounds/purchase.wav";
    private static final String EXPLOSION_SOUND = "sounds/purchase.wav";
    
    private static Map<String, AudioInputStream> soundStreams = new HashMap<>();
    
    static {
        try {
            // Load and cache sound files
            File scoreFile = new File(SCORE_SOUND);
            File explosionFile = new File(EXPLOSION_SOUND);
            
            if (scoreFile.exists()) {
                AudioInputStream scoreStream = AudioSystem.getAudioInputStream(scoreFile);
                soundStreams.put("score", scoreStream);
            } else {
                System.err.println("Sound file not found: " + SCORE_SOUND);
            }
            
            if (explosionFile.exists()) {
                AudioInputStream explosionStream = AudioSystem.getAudioInputStream(explosionFile);
                soundStreams.put("explosion", explosionStream);
            } else {
                System.err.println("Sound file not found: " + EXPLOSION_SOUND);
            }
            
        } catch (UnsupportedAudioFileException | IOException e) {
            System.err.println("Error loading sound files: " + e.getMessage());
        }
    }
    
    public static void playScore() {
        playSound("score");
    }
    
    public static void playExplosion() {
        playSound("explosion");
    }
    
    private static void playSound(String soundName) {
        try {
            if (soundStreams.containsKey(soundName)) {
                // Create a fresh clip for each playback
                Clip clip = AudioSystem.getClip();
                
                // Need to get a fresh stream each time
                File soundFile = new File(soundName.equals("score") ? SCORE_SOUND : EXPLOSION_SOUND);
                AudioInputStream inputStream = AudioSystem.getAudioInputStream(soundFile);
                
                clip.open(inputStream);
                clip.start();
                
                // Auto-close the clip when it's done playing
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        clip.close();
                    }
                });
            }
        } catch (LineUnavailableException | IOException | UnsupportedAudioFileException e) {
            System.err.println("Error playing sound: " + e.getMessage());
        }
    }
} 