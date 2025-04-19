import java.awt.*;
import java.awt.geom.Rectangle2D;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class GameOverScreen extends JPanel {
    private final Color defaultBackgroundColor = new Color(255, 164, 128);
    private final Color redBackgroundColor = new Color(255, 182, 182);
    private final Color blueBackgroundColor = new Color(185, 232, 255);
    private final Color buttonColor = new Color(255, 200, 170);
    private final Color textColor = new Color(80, 40, 0);
    private final Color redTeamColor = new Color(255, 82, 82);
    private final Color blueTeamColor = new Color(8, 168, 247);
    private final Runnable restartGameCallback;
    private final ViewMenuGame parentFrame;
    private final String winner;

    public GameOverScreen(String winner, Runnable restartGameCallback, ViewMenuGame parent) {
        this.restartGameCallback = restartGameCallback;
        this.parentFrame = parent;
        this.winner = winner;

        final Color bgColor;
        if (winner.equals("ĐỎ")) {
            bgColor = redBackgroundColor;
        } else if (winner.equals("XANH")) {
            bgColor = blueBackgroundColor;
        } else {
            bgColor = defaultBackgroundColor;
        }
        
        setLayout(new BorderLayout(0, 30));
        setBackground(bgColor);
        setBorder(new EmptyBorder(50, 0, 50, 0));

        // Panel chứa tiêu đề
        JPanel titlePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                setBackground(bgColor);
            }
        };
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setBorder(new EmptyBorder(20, 0, 0, 0));

        // Custom game over label with enhanced visuals
        JLabel gameOverLabel = new EnhancedLabel("KẾT THÚC", new Font("Arial", Font.BOLD, 52), textColor, true, true);
        gameOverLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Custom winner label with enhanced visuals
        Color winnerColor = textColor;
        if (winner.equals("ĐỎ")) {
            winnerColor = redTeamColor;
        } else if (winner.equals("XANH")) {
            winnerColor = blueTeamColor;
        }
        JLabel winnerLabel = new EnhancedLabel(winner + " THẮNG !", new Font("Arial", Font.BOLD, 40), winnerColor, true, true);
        winnerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        titlePanel.add(gameOverLabel);
        titlePanel.add(Box.createVerticalStrut(20));
        titlePanel.add(winnerLabel);
        add(titlePanel, BorderLayout.NORTH);

        // Panel chứa nút
        JPanel buttonPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                setBackground(bgColor);
            }
        };
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));

        // Tạo nút "PLAY AGAIN"
        JButton playAgainButton = createStyledButton("CHƠI LẠI");
        playAgainButton.addActionListener(e -> restartGameCallback.run());

        // Tạo nút "EXIT"
        JButton exitButton = createStyledButton("THOÁT");
        exitButton.addActionListener(e -> parentFrame.showMenu());

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
                
                // Create gradient based on winner color
                Color buttonBaseColor = buttonColor;
                if (winner.equals("ĐỎ")) {
                    buttonBaseColor = new Color(255, 210, 200);
                } else if (winner.equals("XANH")) {
                    buttonBaseColor = new Color(210, 230, 255);
                }
                
                Color buttonTopColor = new Color(
                    Math.min(255, buttonBaseColor.getRed() + 20),
                    Math.min(255, buttonBaseColor.getGreen() + 20),
                    Math.min(255, buttonBaseColor.getBlue() + 20)
                );
                
                if (getModel().isPressed()) {
                    buttonTopColor = buttonBaseColor;
                    buttonBaseColor = buttonBaseColor.darker();
                } else if (getModel().isRollover()) {
                    buttonTopColor = new Color(
                        Math.min(255, buttonTopColor.getRed() + 10),
                        Math.min(255, buttonTopColor.getGreen() + 10),
                        Math.min(255, buttonTopColor.getBlue() + 10)
                    );
                }
                
                GradientPaint gp = new GradientPaint(
                    0, 0, buttonTopColor, 
                    0, getHeight(), buttonBaseColor
                );
                
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                
                // Add a subtle border
                g2.setColor(new Color(0, 0, 0, 40));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 20, 20);
                
                g2.dispose();
                
                super.paintComponent(g);
            }
        };

        button.setFont(new Font("Arial", Font.BOLD, 26));
        button.setForeground(textColor);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setMaximumSize(new Dimension(200, 60));
        button.setPreferredSize(new Dimension(200, 60));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        return button;
    }
    
    // Custom label class with text effects
    private class EnhancedLabel extends JLabel {
        private boolean hasDropShadow;
        private boolean hasShadowOutline;
        
        public EnhancedLabel(String text, int alignment) {
            super(text, alignment);
            this.hasDropShadow = true;
            this.hasShadowOutline = false;
        }
        
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
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            
            // Get the text and layout information
            FontMetrics fm = g2d.getFontMetrics(getFont());
            Rectangle2D textBounds = fm.getStringBounds(getText(), g2d);
            int textX = (getWidth() - (int) textBounds.getWidth()) / 2;
            int textY = (getHeight() - (int) textBounds.getHeight()) / 2 + fm.getAscent();
            
            // Draw text shadow
            if (hasDropShadow) {
                g2d.setColor(new Color(0, 0, 0, 60));
                g2d.drawString(getText(), textX + 3, textY + 3);
            }
            
            // Shadow outline effect
            if (hasShadowOutline) {
                g2d.setColor(new Color(0, 0, 0, 100));
                g2d.drawString(getText(), textX + 1, textY + 1);
                g2d.drawString(getText(), textX - 1, textY - 1);
                g2d.drawString(getText(), textX + 1, textY - 1);
                g2d.drawString(getText(), textX - 1, textY + 1);
            }
            
            // Draw the actual text
            g2d.setColor(getForeground());
            g2d.drawString(getText(), textX, textY);
            
            g2d.dispose();
        }
    }
}
