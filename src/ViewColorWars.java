import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class ViewColorWars extends JFrame {
    private static final int GRID_SIZE = 5;
    private static final int CELL_SIZE = 80;
    private static final int PADDING = 20;
    private static final int DOT_RADIUS = 6;

    private final Cell[][] grid = new Cell[GRID_SIZE][GRID_SIZE];
    private JLabel turnLabel;

    private final Color backgroundColor = new Color(255, 164, 128);
    private final Color emptyColor = new Color(255, 243, 224);
    private final Color blueTeamColor = new Color(0, 188, 212);
    private final Color redTeamColor = new Color(255, 82, 82);

    public ViewColorWars() {
        setTitle("Color Wars");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        //Tạo khung mainPanel
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(backgroundColor);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        mainPanel.setLayout(null);

        // Tạo và đặt bảng chơi
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                grid[row][col] = new Cell(row, col);
                grid[row][col].setBounds(
                        PADDING + col * (CELL_SIZE + 5),
                        PADDING + row * (CELL_SIZE + 5),
                        CELL_SIZE, CELL_SIZE
                );

//                final int r = row;
//                final int c = col;
//                grid[row][col].addMouseListener(new MouseAdapter() {
//                    @Override
//                    public void mousePressed(MouseEvent e) {
//                        // Placeholder for your game logic
//                        System.out.println("Cell clicked: " + r + ", " + c);
//                    }
//                });

                mainPanel.add(grid[row][col]);
            }
        }

        // Khung hiện thị điểm và lượt chơi cùng với nút thoát trò chơi
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


        //Khung bên trái hiện thị điểm
        JPanel scorePanel = new JPanel();
        scorePanel.setOpaque(false);
        scorePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 3, 3));

        JLabel scoreRed = new JLabel("4");
        scoreRed.setFont(new Font("Arial", Font.BOLD, 24));
        scoreRed.setForeground(redTeamColor);

        JLabel scoreLabel = new JLabel(" • ");
        scoreLabel.setFont(new Font("Arial", Font.BOLD, 24));

        JLabel scoreBlue = new JLabel("10");
        scoreBlue.setFont(new Font("Arial", Font.BOLD, 24));
        scoreBlue.setForeground(blueTeamColor);

        scorePanel.add(scoreRed);
        scorePanel.add(scoreLabel);
        scorePanel.add(scoreBlue);

        // Khung chính giữa hiện thị lượt chơi
        turnLabel = new JLabel("Turn: Red", JLabel.CENTER);
        turnLabel.setFont(new Font("Arial", Font.BOLD, 20));
        turnLabel.setForeground(redTeamColor);

        //Nút bên phải để thoát
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
                PADDING + GRID_SIZE * (CELL_SIZE + 5) + 10,
                GRID_SIZE * (CELL_SIZE + 5) - 10,
                50
        );
        mainPanel.add(infoPanel);
        setContentPane(mainPanel);
        setSize(PADDING * 2 + GRID_SIZE * (CELL_SIZE + 5), PADDING * 2 + GRID_SIZE * (CELL_SIZE + 5) + 70);
        setLocationRelativeTo(null);

        //Khởi tạo bảng với bố cục mẫu
        setupSampleBoard();
    }

    // Khởi tạo với một mẫu bố trí bảng để tham khảo trực quan
    private void setupSampleBoard() {
        grid[2][2].setState(CellState.RED_ONE);
        grid[2][1].setState(CellState.RED_TWO);
        grid[2][3].setState(CellState.RED_TWO);
        grid[1][2].setState(CellState.BLUE_TWO);
        grid[1][3].setState(CellState.BLUE_THREE);
        grid[2][4].setState(CellState.BLUE_THREE);
        grid[4][4].setState(CellState.BLUE_FOUR);
        grid[3][4].setState(CellState.RED_FOUR);

    }

    // Phương thức xóa tất cả các chấm ở bảng
//    public void clearBoard() {
//        for (int row = 0; row < GRID_SIZE; row++) {
//            for (int col = 0; col < GRID_SIZE; col++) {
//                grid[row][col].setState(CellState.EMPTY);
//            }
//        }
//    }

    // Phương thức thiết lập chấm của ô
//    public void setCellState(int row, int col, CellState state) {
//        if (row >= 0 && row < GRID_SIZE && col >= 0 && col < GRID_SIZE) {
//            grid[row][col].setState(state);
//        }
//    }

    //Phương thức cập nhật đến lượt chơi của ai
//    public void setTurn(boolean isRedTurn) {
//        if (isRedTurn) {
//            turnLabel.setText("Turn: Red");
//            turnLabel.setForeground(redTeamColor);
//        } else {
//            turnLabel.setText("Turn: Blue");
//            turnLabel.setForeground(blueTeamColor);
//        }
//    }

    // Lớp biểu diễn từng vị trí của bảng
    class Cell extends JPanel {
        private int row, col;
        private CellState state = CellState.EMPTY;

        public Cell(int row, int col) {
            this.row = row;
            this.col = col;
            setOpaque(false);
        }

        public CellState getState() {
            return state;
        }

        public void setState(CellState state) {
            this.state = state;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(emptyColor);
            g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);

            if (state != CellState.EMPTY) {
                if (state.toString().startsWith("RED")) {
                    g2d.setColor(redTeamColor);
                } else {
                    g2d.setColor(blueTeamColor);
                }

                g2d.fillOval(5, 5, getWidth() - 10, getHeight() - 10);
                g2d.setColor(Color.WHITE);

                switch (state) {
                    case BLUE_ONE:
                    case RED_ONE:
                        g2d.fillOval(getWidth()/2 - DOT_RADIUS, getHeight()/2 - DOT_RADIUS,
                                DOT_RADIUS*2, DOT_RADIUS*2);
                        break;
                    case RED_TWO:
                    case BLUE_TWO:
                        g2d.fillOval(getWidth()/3 - DOT_RADIUS, getHeight()/2 - DOT_RADIUS,
                                DOT_RADIUS*2, DOT_RADIUS*2);
                        g2d.fillOval(2*getWidth()/3 - DOT_RADIUS, getHeight()/2 - DOT_RADIUS,
                                DOT_RADIUS*2, DOT_RADIUS*2);
                        break;
                    case RED_THREE:
                    case BLUE_THREE:
                        g2d.fillOval(getWidth()/2 - DOT_RADIUS, getHeight()/3 - DOT_RADIUS,
                                DOT_RADIUS*2, DOT_RADIUS*2);
                        g2d.fillOval(getWidth()/3 - DOT_RADIUS, 2*getHeight()/3 - DOT_RADIUS,
                                DOT_RADIUS*2, DOT_RADIUS*2);
                        g2d.fillOval(2*getWidth()/3 - DOT_RADIUS, 2*getHeight()/3 - DOT_RADIUS,
                                DOT_RADIUS*2, DOT_RADIUS*2);
                        break;
                    case RED_FOUR:
                    case BLUE_FOUR:
                        g2d.fillOval(getWidth() / 3 - DOT_RADIUS, getHeight() / 3 - DOT_RADIUS,
                                DOT_RADIUS * 2, DOT_RADIUS * 2); // Chấm góc trên trái

                        g2d.fillOval(2 * getWidth() / 3 - DOT_RADIUS, getHeight() / 3 - DOT_RADIUS,
                                DOT_RADIUS * 2, DOT_RADIUS * 2); // Chấm góc trên phải

                        g2d.fillOval(getWidth() / 3 - DOT_RADIUS, 2 * getHeight() / 3 - DOT_RADIUS,
                                DOT_RADIUS * 2, DOT_RADIUS * 2); // Chấm góc dưới trái

                        g2d.fillOval(2 * getWidth() / 3 - DOT_RADIUS, 2 * getHeight() / 3 - DOT_RADIUS,
                                DOT_RADIUS * 2, DOT_RADIUS * 2); // Chấm góc dưới phải
                        break;
                }
            }
        }
    }

}
