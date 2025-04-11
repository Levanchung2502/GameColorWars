import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class GameOverScreen extends JPanel {
    private final Color backgroundColor = new Color(255, 164, 128);
    private final Color buttonHoverColor = new Color(255, 140, 100);
    private final Color buttonColor = new Color(255, 200, 170);
    private final Color textColor = new Color(80, 40, 0);
    private final Runnable restartGameCallback;

    public GameOverScreen(String winner, Runnable restartGameCallback) {
        this.restartGameCallback = restartGameCallback;
        setLayout(new BorderLayout(0, 30));  // Thêm khoảng cách giữa các thành phần
        setBackground(backgroundColor);
        setBorder(new EmptyBorder(50, 0, 50, 0));  // Thêm padding cho panel

        // Panel chứa tiêu đề
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setBackground(backgroundColor);
        titlePanel.setBorder(new EmptyBorder(20, 0, 0, 0));

        // Hiển thị "GAME OVER"
        JLabel gameOverLabel = new JLabel("GAME OVER", SwingConstants.CENTER);
        gameOverLabel.setFont(new Font("Segoe UI", Font.BOLD, 48));
        gameOverLabel.setForeground(textColor);
        gameOverLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Hiển thị người chiến thắng
        JLabel winnerLabel = new JLabel(winner + " WINS!", SwingConstants.CENTER);
        winnerLabel.setFont(new Font("Segoe UI", Font.BOLD, 36));
        winnerLabel.setForeground(textColor);
        winnerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        titlePanel.add(gameOverLabel);
        titlePanel.add(Box.createVerticalStrut(20));
        titlePanel.add(winnerLabel);
        add(titlePanel, BorderLayout.NORTH);

        // Panel chứa nút
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setBackground(backgroundColor);

        // Tạo nút "PLAY AGAIN"
        JButton playAgainButton = createStyledButton("PLAY AGAIN");
        playAgainButton.addActionListener(e -> restartGameCallback.run());

        // Tạo nút "EXIT"
        JButton exitButton = createStyledButton("EXIT");
        exitButton.addActionListener(e -> System.exit(0));

        // Thêm các nút vào panel
        buttonPanel.add(Box.createVerticalGlue());
        buttonPanel.add(playAgainButton);
        buttonPanel.add(Box.createVerticalStrut(15));
        buttonPanel.add(exitButton);
        buttonPanel.add(Box.createVerticalGlue());

        add(buttonPanel, BorderLayout.CENTER);
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (getModel().isPressed()) {
                    g2.setColor(buttonHoverColor.darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(buttonHoverColor);
                } else {
                    g2.setColor(buttonColor);
                }
                
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();
                
                super.paintComponent(g);
            }
        };

        button.setFont(new Font("Segoe UI", Font.BOLD, 24));
        button.setForeground(textColor);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(200, 50));
        button.setPreferredSize(new Dimension(200, 50));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        return button;
    }
}
