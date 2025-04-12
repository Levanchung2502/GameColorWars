import javax.swing.*;
import java.awt.*;

public class ViewMenuGame extends JFrame {
    private final CardLayout cardLayout;
    private final JPanel mainPanel;

    // Đồng bộ với GameOverScreen
    private final Color backgroundColor = new Color(255, 164, 128);
    private final Color buttonHoverColor = new Color(255, 140, 100);
    private final Color buttonColor = new Color(255, 200, 170);
    private final Color textColor = new Color(80, 40, 0);

    public ViewMenuGame() {
        setTitle("Color Wars");
        setSize(515, 585);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // Thêm 2 view vào mainPanel
        mainPanel.add(createMenuPanel(), "menu");
        mainPanel.add(new ViewGuide(this), "guide"); // ViewGuide là lớp bạn đã định nghĩa

        add(mainPanel);
        setVisible(true);
    }

    // Giao diện menu chính
    private JPanel createMenuPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(backgroundColor); // đồng bộ màu nền

        JLabel title = new JLabel("COLOR WARS");
        title.setFont(new Font("Segoe UI", Font.BOLD, 48));
        title.setForeground(textColor);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton friendBtn = createStyledButton("Chơi với Bạn");
        JButton botBtn = createStyledButton("Chơi với Máy");
        JButton guideBtn = createStyledButton("Hướng dẫn");

        // Căn giữa các button
        friendBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        botBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        guideBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Sự kiện chuyển sang giao diện hướng dẫn
        guideBtn.addActionListener(e -> cardLayout.show(mainPanel, "guide"));

        panel.add(Box.createVerticalStrut(50));
        panel.add(title);
        panel.add(Box.createVerticalStrut(40));
        panel.add(friendBtn);
        panel.add(Box.createVerticalStrut(20));
        panel.add(botBtn);
        panel.add(Box.createVerticalStrut(20));
        panel.add(guideBtn);

        return panel;
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
        button.setMaximumSize(new Dimension(220, 70));
        button.setPreferredSize(new Dimension(200, 50));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        return button;
    }

    // Chuyển về menu
    public void showMenu() {
        cardLayout.show(mainPanel, "menu");
    }

    // Main
    public static void main(String[] args) {
        new ViewMenuGame();
    }
}
