import java.awt.*;
import java.awt.event.ActionListener;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.*;

// Thêm import cho AIPlayer


public class ViewColorWars extends JPanel {
    private static final int GRID_SIZE = 5;
    private static final int CELL_SIZE = 80;
    private static final int PADDING = 20;
    private JLabel turnLabel;
    private JPanel scorePanel;
    private JLabel scoreRed;
    private JLabel scoreBlue;
    private JLabel scoreLabel;
    private final Color backgroundColor = new Color(255, 164, 128);
    private final Color emptyColor = new Color(255, 243, 224);
    private final Color blueTeamColor = new Color(0, 188, 212);
    private final Color redTeamColor = new Color(255, 82, 82);
    private GameLogic gameLogic;
    private AIPlayer aiPlayer;
    private Timer activationTimer;
    private Timer watchdogTimer;
    private ExecutorService aiExecutor;
    private boolean isAIThinking = false;
    private GameOverScreen gameOverScreen;
    private JLayeredPane layeredPane;
    private ViewMenuGame parentFrame;
    private JPanel mainPanel;
    private Color redBgColor = new Color(252, 112, 112);
    private Color blueBgColor = new Color(94, 224, 255);

    static {
        // Tăng kích thước heap cho JVM
        System.setProperty("java.awt.headless", "false");
        System.setProperty("sun.java2d.noddraw", "true");
        System.setProperty("sun.java2d.opengl", "true");
    }

    public ViewColorWars(ViewMenuGame parent, boolean isPlayWithBot) {
        this.parentFrame = parent;
        setLayout(new BorderLayout());

        // Khởi tạo thread pool riêng cho AI với cấu hình tối ưu
        aiExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r);
            thread.setPriority(Thread.MIN_PRIORITY);
            thread.setDaemon(true);
            return thread;
        });

        // Tạo layeredPane để quản lý các lớp
        layeredPane = new JLayeredPane();
        add(layeredPane, BorderLayout.CENTER);

        mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                boolean isRedTurn = gameLogic != null ? gameLogic.isRedTurn() : true;
                g.setColor(isRedTurn ? redBgColor : blueBgColor);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        mainPanel.setLayout(null);

        // Khung hiện thị điểm và lượt chơi
        JPanel infoPanel = new JPanel(new GridLayout(1, 3)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(emptyColor);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
            }
        };
        infoPanel.setOpaque(false);
        infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Khung điểm số
        scorePanel = new JPanel();
        scorePanel.setOpaque(false);
        scorePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 3, 3));

        scoreRed = new JLabel("0");
        scoreRed.setFont(new Font("Arial", Font.BOLD, 24));
        scoreRed.setForeground(redTeamColor);

        scoreLabel = new JLabel(" • ");
        scoreLabel.setFont(new Font("Arial", Font.BOLD, 24));

        scoreBlue = new JLabel("0");
        scoreBlue.setFont(new Font("Arial", Font.BOLD, 24));
        scoreBlue.setForeground(blueTeamColor);

        scorePanel.add(scoreRed);
        scorePanel.add(scoreLabel);
        scorePanel.add(scoreBlue);

        // Nhãn lượt chơi
        turnLabel = new JLabel("Lượt: ĐỎ", JLabel.CENTER);
        turnLabel.setFont(new Font("Arial", Font.BOLD, 20));
        turnLabel.setForeground(redTeamColor);

        // Nút thoát
        JButton exitButton = new JButton("THOÁT");
        exitButton.setFont(new Font("Arial", Font.BOLD, 20));
        exitButton.setBackground(emptyColor);
        exitButton.setFocusPainted(false);
        exitButton.setBorderPainted(false);
        exitButton.setContentAreaFilled(false);
        exitButton.addActionListener(e -> {
            cleanup();
            parentFrame.showMenu();
        });

        infoPanel.add(scorePanel);
        infoPanel.add(turnLabel);
        infoPanel.add(exitButton);

        infoPanel.setBounds(
                PADDING,
                PADDING + GRID_SIZE * (CELL_SIZE + 15) + 10,
                GRID_SIZE * (CELL_SIZE + 15) - 10,
                50
        );
        mainPanel.add(infoPanel);

        // Tạo game logic
        gameLogic = new GameLogic(emptyColor, blueTeamColor, redTeamColor, turnLabel, this);
        gameLogic.setBounds(
                PADDING,
                PADDING,
                GRID_SIZE * (CELL_SIZE + 15),
                GRID_SIZE * (CELL_SIZE + 15)
        );
        mainPanel.add(gameLogic);

        // Thêm mainPanel vào layeredPane ở lớp mặc định
        mainPanel.setBounds(0, 0, PADDING * 2 + GRID_SIZE * (CELL_SIZE + 15), 
                           PADDING * 2 + GRID_SIZE * (CELL_SIZE + 15) + 70);
        layeredPane.add(mainPanel, JLayeredPane.DEFAULT_LAYER);

        // Khởi tạo gameOverScreen
        gameOverScreen = new GameOverScreen("", () -> resetGame(), parentFrame);
        gameOverScreen.setBounds(0, 0, PADDING * 2 + GRID_SIZE * (CELL_SIZE + 15),
                                PADDING * 2 + GRID_SIZE * (CELL_SIZE + 15) + 70);
        gameOverScreen.setVisible(false);
        layeredPane.add(gameOverScreen, JLayeredPane.POPUP_LAYER);

        setPreferredSize(new Dimension(PADDING * 2 + GRID_SIZE * (CELL_SIZE + 15), 
                PADDING * 2 + GRID_SIZE * (CELL_SIZE + 15) + 70));
        
        // Khởi tạo AI nếu chơi với máy
        if (isPlayWithBot) {
            aiPlayer = new AIPlayer(gameLogic, false);
        }
    }

    private void cleanup() {
        stopExistingTimers();
        if (aiExecutor != null) {
            try {
                aiExecutor.shutdownNow();
                aiExecutor.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (aiPlayer != null) {
            aiPlayer.deactivate();
        }
    }

    public void deactivateAI() {
        if (aiPlayer != null) {
            isAIThinking = false;
            aiPlayer.deactivate();
        }
    }

    public void reinitializeAI() {
        if (aiPlayer != null) {
            isAIThinking = false;
            aiPlayer.deactivate();
            if (!gameLogic.isRedTurn()) {
                activateAIWithDelay();
            }
        }
    }

    private void activateAIWithDelay() {
        if (!isAIThinking) {
            SwingUtilities.invokeLater(() -> {
                try {
                    // Tăng độ trễ lên 1 giây
                    Thread.sleep(1000);
                    aiPlayer.activate();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public void activateAI() {
        if (aiPlayer != null && !gameLogic.isRedTurn() && !isAIThinking) {
            stopExistingTimers();
            isAIThinking = true;
            
            aiExecutor.submit(() -> {
                try {
                    if (!gameLogic.isRedTurn() && !gameLogic.isGameOver()) {
                        // Thêm độ trễ 1 giây trước khi AI đánh
                        Thread.sleep(1000);
                        aiPlayer.activate();
                    }
                } catch (OutOfMemoryError e) {
                    System.gc();
                    reinitializeAI();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    isAIThinking = false;
                }
            });

            watchdogTimer = new Timer(5000, e -> {
                if (!gameLogic.isRedTurn() && !gameLogic.isGameOver() && !isAIThinking) {
                    activateAIWithDelay();
                }
            });
            watchdogTimer.setRepeats(false);
            watchdogTimer.start();
        }
    }

    private void stopExistingTimers() {
        if (activationTimer != null) {
            activationTimer.stop();
            for (ActionListener listener : activationTimer.getActionListeners()) {
                activationTimer.removeActionListener(listener);
            }
        }
        
        if (watchdogTimer != null) {
            watchdogTimer.stop();
            for (ActionListener listener : watchdogTimer.getActionListeners()) {
                watchdogTimer.removeActionListener(listener);
            }
        }
    }
    
    public void updateScoreDisplay(int scoreR, int scoreB) {
        SwingUtilities.invokeLater(() -> {
            scoreRed.setText(String.valueOf(scoreR));
            scoreBlue.setText(String.valueOf(scoreB));
            updateBackgroundColor();
        });
    }

    public GameLogic getGameLogic() {
        return gameLogic;
    }

    public void showGameOver(String winner) {
        // Xóa gameOverScreen cũ khỏi layeredPane nếu có
        if (gameOverScreen != null) {
            gameOverScreen.setVisible(false);
            layeredPane.remove(gameOverScreen);
        }
        
        // Tạo và thêm gameOverScreen mới
        gameOverScreen = new GameOverScreen(winner, () -> resetGame(), parentFrame);
        gameOverScreen.setBounds(0, 0, PADDING * 2 + GRID_SIZE * (CELL_SIZE + 15),
                                PADDING * 2 + GRID_SIZE * (CELL_SIZE + 15) + 70);
        layeredPane.add(gameOverScreen, JLayeredPane.POPUP_LAYER);
        
        // Hiển thị gameOverScreen và cập nhật giao diện
        gameOverScreen.setVisible(true);
        layeredPane.revalidate();
        layeredPane.repaint();
    }

    public void hideGameOver() {
        if (gameOverScreen != null) {
            gameOverScreen.setVisible(false);
            layeredPane.remove(gameOverScreen);
            layeredPane.revalidate();
            layeredPane.repaint();
        }
    }

    private void resetGame() {
        hideGameOver();
        gameLogic.resetGame();
        repaint();
    }

    // Update the background color when the game state changes
    public void updateBackgroundColor() {
        if (mainPanel != null) {
            mainPanel.repaint();
        }
    }
}
