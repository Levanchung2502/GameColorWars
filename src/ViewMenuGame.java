import java.awt.*;
import java.awt.geom.Rectangle2D;
import javax.swing.*;

public class ViewMenuGame extends JFrame {
    private final CardLayout cardLayout;
    private final JPanel mainPanel;
    private ViewColorWars gamePanel;
    private static final int WINDOW_WIDTH = 2 * 20 + 5 * (80 + 15);  // 2*PADDING + GRID_SIZE*(CELL_SIZE + 15)
    private static final int WINDOW_HEIGHT = 2 * 20 + 5 * (80 + 15) + 70;  // 2*PADDING + GRID_SIZE*(CELL_SIZE + 15) + 70
    private final Color backgroundColor = new Color(255, 164, 128);
    private final Color buttonColor = new Color(255, 200, 170);
    private final Color textColor = new Color(80, 40, 0);


    public ViewMenuGame() {
        setTitle("Color Wars");
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // Thêm các view vào mainPanel
        mainPanel.add(createMenuPanel(), "menu");
        mainPanel.add(new ViewGuide(this), "guide");

        add(mainPanel);
        setVisible(true);
    }

    private JPanel createMenuPanel() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                
                // Create a gradient background
                GradientPaint gp = new GradientPaint(
                    0, 0, new Color(255, 190, 150), 
                    getWidth(), getHeight(), backgroundColor
                );
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        EnhancedLabel title = new EnhancedLabel("COLOR WARS", new Font("Arial", Font.BOLD, 54), textColor, true, true);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton friendBtn = createStyledButton("Chơi với Bạn");
        JButton botBtn = createStyledButton("Chơi với Máy");
        JButton guideBtn = createStyledButton("Hướng dẫn");

        friendBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        botBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        guideBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        friendBtn.addActionListener(e -> startGame(false));
        botBtn.addActionListener(e -> startGame(true));
        guideBtn.addActionListener(e -> cardLayout.show(mainPanel, "guide"));

        panel.add(Box.createVerticalStrut(70));
        panel.add(title);
        panel.add(Box.createVerticalStrut(50));
        panel.add(friendBtn);
        panel.add(Box.createVerticalStrut(25));
        panel.add(botBtn);
        panel.add(Box.createVerticalStrut(25));
        panel.add(guideBtn);

        return panel;
    }

    private void startGame(boolean isPlayWithBot) {
        if (gamePanel != null) {
            mainPanel.remove(gamePanel);
        }
        gamePanel = new ViewColorWars(this, isPlayWithBot);
        mainPanel.add(gamePanel, "game");
        cardLayout.show(mainPanel, "game");
        pack();
        setLocationRelativeTo(null);
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                Color buttonTopColor = new Color(
                    Math.min(255, buttonColor.getRed() + 20),
                    Math.min(255, buttonColor.getGreen() + 20),
                    Math.min(255, buttonColor.getBlue() + 20)
                );
                
                Color buttonBottomColor = buttonColor;
                
                if (getModel().isPressed()) {
                    buttonTopColor = buttonColor;
                    buttonBottomColor = buttonColor.darker();
                } else if (getModel().isRollover()) {
                    buttonTopColor = new Color(
                        Math.min(255, buttonTopColor.getRed() + 15),
                        Math.min(255, buttonTopColor.getGreen() + 15),
                        Math.min(255, buttonTopColor.getBlue() + 15)
                    );
                }
                
                GradientPaint gp = new GradientPaint(
                    0, 0, buttonTopColor, 
                    0, getHeight(), buttonBottomColor
                );
                
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                
                // Add a subtle border
                g2.setColor(new Color(0, 0, 0, 40));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 20, 20);
                
                // Draw text with shadow
                FontMetrics fm = g2.getFontMetrics(getFont());
                Rectangle2D r = fm.getStringBounds(text, g2);
                int x = (getWidth() - (int) r.getWidth()) / 2;
                int y = (getHeight() - (int) r.getHeight()) / 2 + fm.getAscent();
                
                g2.setColor(new Color(0, 0, 0, 60));
                g2.drawString(text, x+2, y+2);
                g2.setColor(textColor);
                g2.drawString(text, x, y);
                
                g2.dispose();
            }
            
            // Don't use default painting
            @Override
            protected void paintBorder(Graphics g) {}
        };

        button.setFont(new Font("Arial", Font.BOLD, 26));
        button.setForeground(textColor);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(250, 70));
        button.setPreferredSize(new Dimension(220, 60));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        return button;
    }

    // Chuyển về menu
    public void showMenu() {
        cardLayout.show(mainPanel, "menu");
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setLocationRelativeTo(null);
    }
    
    // Custom label class with text effects
    private class EnhancedLabel extends JLabel {
        private boolean hasDropShadow;
        private boolean hasShadowOutline;
        
        public EnhancedLabel(String text, Font font, Color color, boolean hasDropShadow, boolean hasShadowOutline) {
            super(text);
            this.hasDropShadow = hasDropShadow;
            this.hasShadowOutline = hasShadowOutline;
            setFont(font);
            setForeground(color);
            setHorizontalAlignment(JLabel.CENTER);
            setAlignmentX(Component.CENTER_ALIGNMENT);
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            
            FontMetrics fm = g2.getFontMetrics(getFont());
            String text = getText();
            Rectangle2D r = fm.getStringBounds(text, g2);
            int x = (getWidth() - (int) r.getWidth()) / 2;
            int y = (getHeight() - (int) r.getHeight()) / 2 + fm.getAscent();
            
            // Drop shadow effect
            if (hasDropShadow) {
                g2.setColor(new Color(0, 0, 0, 80));
                g2.drawString(text, x + 3, y + 3);
            }
            
            // Shadow outline effect
            if (hasShadowOutline) {
                g2.setColor(new Color(0, 0, 0, 100));
                g2.drawString(text, x + 1, y + 1);
                g2.drawString(text, x - 1, y - 1);
                g2.drawString(text, x + 1, y - 1);
                g2.drawString(text, x - 1, y + 1);
            }
            
            // Draw the actual text
            g2.setColor(getForeground());
            g2.drawString(text, x, y);
            
            g2.dispose();
        }
    }
}
