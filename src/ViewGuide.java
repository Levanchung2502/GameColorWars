import javax.swing.*;
import java.awt.*;

public class ViewGuide extends JPanel {
    private final Color buttonHoverColor = new Color(255, 140, 100);
    private final Color buttonColor = new Color(255, 200, 170);
    private final Color textColor = new Color(80, 40, 0);
    public ViewGuide(ViewMenuGame parent) {
        // Màu sắc đồng bộ với GameOverScreen & ViewMenuGame
        Color backgroundColor = new Color(255, 164, 128);

        setBackground(backgroundColor);
        setLayout(new BorderLayout(20, 20));

        // Tiêu đề
        JLabel title = new JLabel("HƯỚNG DẪN CHƠI", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 32));
        title.setForeground(textColor);
        title.setBorder(BorderFactory.createEmptyBorder(20, 10, 0, 10));
        add(title, BorderLayout.NORTH);


        String instructions = """
                 Mục tiêu:
                Chiếm lĩnh ô bằng màu sắc để chiếm nhiều vùng nhất.

                 Cách chơi:
                - Lượt đầu tiên của mỗi người chơi khi chọn vào một ô sẽ được 3 chấm.
                - Khi một ô được 4 chấm nó sẽ tỏa ra các ô xung quanh trên, dưới, trái, phải tương đương với ô đó sẽ là 1 chấm, nếu những ô có sẵn chấm thì sẽ cộng thêm 1 chấm.
                - Trò chơi kết thúc khi đối phương không còn chấm nào trên bàn cờ.
                """;

        JTextArea textArea = new JTextArea(instructions);
        textArea.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        textArea.setForeground(textColor);
        textArea.setBackground(backgroundColor);
        textArea.setEditable(false);
        textArea.setFocusable(false); //
        textArea.setWrapStyleWord(true);
        textArea.setLineWrap(true);
        textArea.setMargin(new Insets(20, 20, 20, 20));
        textArea.setBorder(null); //


        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(backgroundColor);
        add(scrollPane, BorderLayout.CENTER);

        // Nút BACK
        JButton backButton = createStyledButton("QUAY LẠI");
        backButton.addActionListener(e -> parent.showMenu());

        // Panel chứa nút BACK ở dưới, căn giữa
        JPanel bottomPanel = new JPanel();
        bottomPanel.setBackground(backgroundColor);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        bottomPanel.add(backButton);

        add(bottomPanel, BorderLayout.SOUTH);
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
}
