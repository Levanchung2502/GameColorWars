import java.awt.*;
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

    public ViewColorWars() {
        setTitle("Color Wars");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

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
        setVisible(true);

        // Khởi tạo AI nhưng chưa kích hoạt ngay
        aiPlayer = new AIPlayer(gameLogic, false);
    }

    public void deactivateAI() {
        if (aiPlayer != null) {
            aiPlayer.deactivate();
            aiPlayer = null;
        }
    }

    public void reinitializeAI() {
        deactivateAI(); // Đảm bảo AI cũ được hủy
        aiPlayer = new AIPlayer(gameLogic, false);
        // Kích hoạt AI ngay nếu đến lượt của nó
        if (!gameLogic.isRedTurn()) {
            aiPlayer.activate();
        }
    }

    public void activateAI() {
        if (aiPlayer != null && !gameLogic.isRedTurn()) {
            try {
                aiPlayer.activate();
            } catch (Exception e) {
                System.err.println("Lỗi khi kích hoạt AI: " + e.getMessage());
                e.printStackTrace();
                // Tạo AI mới nếu xảy ra lỗi
                reinitializeAI();
            }
        }
    }
    public void updateScoreDisplay(int scoreR, int scoreB) {
        scoreRed.setText(String.valueOf(scoreR));
        scoreBlue.setText(String.valueOf(scoreB));

    }

    public GameLogic getGameLogic() {
        return gameLogic;
    }
}
