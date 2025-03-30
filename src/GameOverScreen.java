import javax.swing.*;
import java.awt.*;

public class GameOverScreen extends JFrame {
    private final Color backgroundColor = new Color(255, 164, 128);

    public GameOverScreen(String winner, Runnable restartGameCallback) {
        setTitle("Game Over");
        setSize(300, 200);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(backgroundColor);

        // Tạo panel chứa tiêu đề
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setBackground(backgroundColor);

        // Thêm khoảng cách 20px từ trên xuống
        titlePanel.add(Box.createVerticalStrut(20));

        // Hiển thị người chiến thắng
        JLabel winnerLabel = new JLabel(winner + " WINS", SwingConstants.CENTER);
        winnerLabel.setFont(new Font("Arial", Font.BOLD, 18));
        winnerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        titlePanel.add(winnerLabel);
        add(titlePanel, BorderLayout.NORTH);

        // Tạo panel chứa nút, xếp theo chiều dọc
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        buttonPanel.setBackground(backgroundColor);

        // Tạo nút "PLAY AGAIN"
        JButton playAgainButton = new JButton("PLAY AGAIN");
        playAgainButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        playAgainButton.setMaximumSize(new Dimension(120, 30));
        playAgainButton.addActionListener(e -> {
            restartGameCallback.run();
            dispose();
        });

        // Tạo nút "EXIT"
        JButton exitButton = new JButton("EXIT");
        exitButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        exitButton.setMaximumSize(new Dimension(120, 30));
        exitButton.addActionListener(e -> System.exit(0));

        // Thêm khoảng cách giữa các nút
        buttonPanel.add(Box.createVerticalStrut(10));
        buttonPanel.add(playAgainButton);
        buttonPanel.add(Box.createVerticalStrut(10));
        buttonPanel.add(exitButton);

        // Thêm panel vào cửa sổ
        add(buttonPanel, BorderLayout.CENTER);

        setVisible(true);
    }


}
