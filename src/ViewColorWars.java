import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.*;

// Thêm import cho AIPlayer


public class ViewColorWars extends JFrame {
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

    static {
        // Tăng kích thước heap cho JVM
        System.setProperty("java.awt.headless", "false");
        System.setProperty("sun.java2d.noddraw", "true");
        System.setProperty("sun.java2d.opengl", "true");
    }

    public ViewColorWars() {
        setTitle("Color Wars");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        // Khởi tạo thread pool riêng cho AI với cấu hình tối ưu
        aiExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r);
            thread.setPriority(Thread.MIN_PRIORITY); // Giảm độ ưu tiên xuống mức thấp nhất
            thread.setDaemon(true); // Đặt là daemon thread
            return thread;
        });

        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(backgroundColor);
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
        turnLabel = new JLabel("Turn: Red", JLabel.CENTER);
        turnLabel.setFont(new Font("Arial", Font.BOLD, 20));
        turnLabel.setForeground(redTeamColor);

        // Nút thoát
        JButton exitButton = new JButton("EXIT");
        exitButton.setFont(new Font("Arial", Font.BOLD, 20));
        exitButton.setBackground(emptyColor);
        exitButton.setFocusPainted(false);
        exitButton.setBorderPainted(false);
        exitButton.setContentAreaFilled(false);
        exitButton.addActionListener(e -> System.exit(0));

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

        setContentPane(mainPanel);
        setSize(PADDING * 2 + GRID_SIZE * (CELL_SIZE + 15), PADDING * 2 + GRID_SIZE * (CELL_SIZE + 15) + 70);
        setLocationRelativeTo(null);
        
        // Khởi tạo AI một lần duy nhất khi tạo game
        aiPlayer = new AIPlayer(gameLogic, false);
        
        setVisible(true);

        // Thêm xử lý khi đóng cửa sổ
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cleanup();
            }
        });
    }

    private void cleanup() {
        System.out.println("Dọn dẹp tài nguyên...");
        stopExistingTimers();
        if (aiExecutor != null) {
            try {
                aiExecutor.shutdownNow();
                aiExecutor.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                System.gc(); // Dọn dẹp bộ nhớ sau khi đóng
            }
        }
        if (aiPlayer != null) {
            aiPlayer.deactivate();
        }
    }

    public void deactivateAI() {
        if (aiPlayer != null) {
            System.out.println("Tạm dừng AI");
            isAIThinking = false;
            aiPlayer.deactivate();
        }
    }

    public void reinitializeAI() {
        System.out.println("Reset AI về trạng thái ban đầu");
        if (aiPlayer != null) {
            isAIThinking = false;
            aiPlayer.deactivate();
            // Không tạo mới AI, chỉ kích hoạt lại nếu cần
            if (!gameLogic.isRedTurn()) {
                System.out.println("Kích hoạt lại AI");
                activateAIWithDelay();
            }
        }
    }

    private void activateAIWithDelay() {
        if (!isAIThinking) {
            SwingUtilities.invokeLater(() -> {
                try {
                    Thread.sleep(100); // Thêm độ trễ nhỏ để tránh quá tải
                    aiPlayer.activate();
                } catch (Exception e) {
                    System.err.println("Lỗi khi kích hoạt AI: " + e.getMessage());
                }
            });
        }
    }

    public void activateAI() {
        if (aiPlayer != null && !gameLogic.isRedTurn() && !isAIThinking) {
            System.out.println("Kích hoạt AI (từ ViewColorWars)");
            try {
                stopExistingTimers();
                isAIThinking = true;

                // Thêm xử lý garbage collection trước khi AI chạy
                System.gc();
                
                aiExecutor.submit(() -> {
                    try {
                        if (!gameLogic.isRedTurn() && !gameLogic.isGameOver()) {
                            System.out.println("AI bắt đầu đánh...");
                            aiPlayer.activate();
                        }
                    } catch (OutOfMemoryError e) {
                        System.err.println("Lỗi hết bộ nhớ trong AI: " + e.getMessage());
                        System.gc(); // Cố gắng giải phóng bộ nhớ
                        reinitializeAI(); // Khởi động lại AI
                    } catch (Exception e) {
                        System.err.println("Lỗi trong quá trình AI suy nghĩ: " + e.getMessage());
                    } finally {
                        isAIThinking = false;
                    }
                });

                // Timer giám sát với thời gian dài hơn
                watchdogTimer = new Timer(5000, e -> {
                    if (!gameLogic.isRedTurn() && !gameLogic.isGameOver() && !isAIThinking) {
                        System.out.println("Watchdog: AI không phản hồi, thử kích hoạt lại...");
                        System.gc(); // Dọn dẹp bộ nhớ trước khi thử lại
                        activateAIWithDelay();
                    }
                });
                watchdogTimer.setRepeats(false);
                watchdogTimer.start();

            } catch (Exception e) {
                System.err.println("Lỗi trong activateAI: " + e.getMessage());
                isAIThinking = false;
            }
        } else {
            String reason = aiPlayer == null ? "AI chưa được khởi tạo" : 
                          isAIThinking ? "AI đang suy nghĩ" : "Không phải lượt của AI";
            System.out.println("Không thể kích hoạt AI: " + reason);
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
        });
    }

    public GameLogic getGameLogic() {
        return gameLogic;
    }
}
