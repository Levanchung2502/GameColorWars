import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedList;
import java.util.Queue;
import javax.swing.*;
import java.util.List;
import java.util.ArrayList;
public class GameLogic extends JPanel {
    public static final int GRID_SIZE = 5;
    private static final int CELL_SIZE = 80;
    private static final int PADDING = 20;
    private boolean isRedTurn = true;
    private boolean redHasMoved = false;
    private boolean blueHasMoved = false;
    private final Cell[][] grid = new Cell[GRID_SIZE][GRID_SIZE];
    private final Color emptyColor;
    private final Color blueTeamColor;
    private final Color redTeamColor;
    private final JLabel turnLabel;
    private final ViewColorWars parent;
    private int scoreR = 0, scoreB = 0;

    public GameLogic(Color emptyColor, Color blueTeamColor, Color redTeamColor, JLabel turnLabel, ViewColorWars parent) {
        this.emptyColor = emptyColor;
        this.blueTeamColor = blueTeamColor;
        this.redTeamColor = redTeamColor;
        this.turnLabel = turnLabel;
        this.parent = parent;
        setLayout(null);
        setOpaque(false);
        initializeGrid();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        setBackground(new Color(0, 0, 0, 0));
    }

    private void initializeGrid() {
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                grid[row][col] = new Cell(row, col, emptyColor, redTeamColor, blueTeamColor);
                grid[row][col].setBounds(
                        PADDING + col * (CELL_SIZE + 5),
                        PADDING + row * (CELL_SIZE + 5),
                        CELL_SIZE,
                        CELL_SIZE
                );

                final int r = row;
                final int c = col;
                grid[row][col].addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        handleCellClick(r, c);
                    }
                });

                add(grid[row][col]);
            }
        }
    }

    private void handleCellClick(int row, int col) {
        Cell cell = grid[row][col];
        CellState state = cell.getState();
        if (state == CellState.EMPTY) {
            if ((isRedTurn && !redHasMoved) || (!isRedTurn && !blueHasMoved)) {
                cell.setState(isRedTurn ? CellState.RED_THREE : CellState.BLUE_THREE);
                if (isRedTurn) {
                    redHasMoved = true;
                } else {
                    blueHasMoved = true;
                }
                isRedTurn = !isRedTurn;
                updateScoreDisplay();
                updateTurnLabel();
                checkGameOver();
            }
        } else if ((isRedTurn && state.isRed()) || (!isRedTurn && state.isBlue())) {
            CellState nextState = state.getNextState();
            cell.setState(nextState);
            isRedTurn = !isRedTurn;
            if (nextState == CellState.RED_FOUR || nextState == CellState.BLUE_FOUR) {
                explodeCell(row, col, nextState, () -> {
                    // Sau khi explode xong mới cập nhật giao diện và đổi lượt
                    SwingUtilities.invokeLater(() -> {

                        updateScoreDisplay();
                    });
                });
            } else {
                updateTurnLabel();
                checkGameOver();
            }
        }
    }

    private void updateTurnLabel() {

        turnLabel.setText("Lượt: " + (isRedTurn ? "Đỏ" : "Xanh"));
        turnLabel.setForeground(isRedTurn ? redTeamColor : blueTeamColor);


        // Đếm quân hiện tại
        int redCount = 0, blueCount = 0;
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                CellState state = grid[row][col].getState();
                if (state.isRed()) {
                    redCount++;
                } else if (state.isBlue()) {
                    blueCount++;
                }
            }
        }

        // Xử lý kích hoạt AI
        if (!isRedTurn) {
            if (redHasMoved && !isGameOver()) {

                // Sử dụng SwingUtilities.invokeLater để tránh vấn đề với thread
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            parent.activateAI();
                        } catch (Exception e) {

                            e.printStackTrace();
                        }
                    }
                });
            }
        }
    }

    private void explodeCell(int row, int col, CellState explodingState, Runnable onFinish) {
        new Thread(() -> {
            Queue<int[]> queue = new LinkedList<>();
            queue.add(new int[]{row, col});

            while (!queue.isEmpty()) {
                List<int[]> nextExplosions = new ArrayList<>();

                int size = queue.size(); // xử lý theo "từng lớp" của nổ
                for (int i = 0; i < size; i++) {
                    int[] cellPos = queue.poll();
                    int r = cellPos[0], c = cellPos[1];

                    // Xóa ô hiện tại
                    grid[r][c].setState(CellState.EMPTY);

                    boolean isRed = explodingState.isRed();
                    int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

                    List<int[]> toBeUpdated = new ArrayList<>();

                    // ✅ Gom tất cả ô xung quanh trước, sau đó cập nhật đồng thời
                    for (int[] dir : directions) {
                        int newRow = r + dir[0], newCol = c + dir[1];

                        if (isValidPosition(newRow, newCol)) {
                            toBeUpdated.add(new int[]{newRow, newCol});
                        }
                    }

                    // ✅ Cập nhật đồng thời các ô
                    for (int[] pos : toBeUpdated) {
                        int newRow = pos[0], newCol = pos[1];
                        Cell neighbor = grid[newRow][newCol];
                        CellState neighborState = neighbor.getState();
                        int neighborDots = getDotCount(neighborState);

                        if (isOppositeColor(explodingState, neighborState)) {
                            neighborDots += 1;
                            neighbor.setState(isRed ? getStateByDotCount(neighborDots, true) : getStateByDotCount(neighborDots, false));
                        } else if ((isRed && neighborState.isRed()) || (!isRed && neighborState.isBlue())) {
                            neighbor.setState(neighborState.getNextState());
                        } else if (neighborState == CellState.EMPTY) {
                            neighbor.setState(isRed ? CellState.RED_ONE : CellState.BLUE_ONE);
                        }

                        if (neighbor.getState() == CellState.RED_FOUR || neighbor.getState() == CellState.BLUE_FOUR) {
                            nextExplosions.add(new int[]{newRow, newCol});
                        }
                    }
                }

                // ✅ Delay trước khi thực hiện nổ tiếp
                sleep(800);

                // Thêm tất cả ô chuẩn bị nổ tiếp vào queue
                queue.addAll(nextExplosions);
            }

            // ✅ Khi xong hết mới chạy onFinish
            SwingUtilities.invokeLater(() -> {
                updateTurnLabel();
                checkGameOver();
                onFinish.run();
            });
        }).start();
    }


    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    private boolean isValidPosition(int row, int col) {
        return row >= 0 && row < grid.length && col >= 0 && col < grid[0].length;
    }

    private int getDotCount(CellState state) {
        switch (state) {
            case RED_ONE:
            case BLUE_ONE:
                return 1;
            case RED_TWO:
            case BLUE_TWO:
                return 2;
            case RED_THREE:
            case BLUE_THREE:
                return 3;
            case RED_FOUR:
            case BLUE_FOUR:
                return 4;
            default:
                return 0;
        }
    }

    private CellState getStateByDotCount(int dotCount, boolean isRed) {
        switch (dotCount) {
            case 1:
                return isRed ? CellState.RED_ONE : CellState.BLUE_ONE;
            case 2:
                return isRed ? CellState.RED_TWO : CellState.BLUE_TWO;
            case 3:
                return isRed ? CellState.RED_THREE : CellState.BLUE_THREE;
            case 4:
                return isRed ? CellState.RED_FOUR : CellState.BLUE_FOUR;
            default:
                return CellState.EMPTY;
        }
    }

    private boolean isOppositeColor(CellState s1, CellState s2) {
        return (s1.isRed() && s2.isBlue()) || (s1.isBlue() && s2.isRed());
    }

    private void checkGameOver() {
        if (!redHasMoved || !blueHasMoved) {
            return;
        }

        int redCount = 0;
        int blueCount = 0;

        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                CellState state = grid[row][col].getState();
                if (state.isRed()) {
                    redCount++;
                } else if (state.isBlue()) {
                    blueCount++;
                }
            }
        }

        if ((redCount == 0 || blueCount == 0) && (redCount + blueCount > 1)) {
            String winner = (redCount == 0) ? "XANH" : "ĐỎ";
            if (redCount == 0) {
                scoreB++;
            } else {
                scoreR++;
            }
            parent.deactivateAI();
            parent.showGameOver(winner);
            parent.updateScoreDisplay(scoreR, scoreB);
        }
    }

    public void resetGame() {
        redHasMoved = false;
        blueHasMoved = false;
        isRedTurn = true;
        updateTurnLabel();

        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                grid[row][col].setState(CellState.EMPTY);
            }
        }
        parent.reinitializeAI();
    }

    public void makeMove(int row, int col) {
        handleCellClick(row, col);
    }

    public boolean isRedTurn() {
        return isRedTurn;
    }

    public void setRedTurn(boolean redTurn) {
        isRedTurn = redTurn;
    }

    public boolean isRedHasMoved() {
        return redHasMoved;
    }

    public void setRedHasMoved(boolean redHasMoved) {
        this.redHasMoved = redHasMoved;
    }

    public boolean isBlueHasMoved() {
        return blueHasMoved;
    }

    public void setBlueHasMoved(boolean blueHasMoved) {
        this.blueHasMoved = blueHasMoved;
    }

    public Cell[][] getGrid() {
        return grid;
    }
    public void setGrid(Cell[][] newGrid) {
        for (int row = 0 ; row < GRID_SIZE; row++) {
            for(int col = 0; col < GRID_SIZE; col++) {
                grid[row][col].setState(newGrid[row][col].getState());
            }
        }
    }

    public boolean isGameOver() {
        if (!redHasMoved || !blueHasMoved) {
            return false;
        }

        int redCount = 0;
        int blueCount = 0;

        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                CellState state = grid[row][col].getState();
                if (state.isRed()) {
                    redCount++;
                } else if (state.isBlue()) {
                    blueCount++;
                }
            }
        }
        
        return (redCount == 0 || blueCount == 0) && (redCount + blueCount > 1);
    }
    private void updateScoreDisplay() {
        int scoreR = 0, scoreB = 0;

        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                CellState state = grid[row][col].getState();
                if (state.isRed()) {
                    scoreR++;
                } else if (state.isBlue()) {
                    scoreB++;
                }
            }
        }
        parent.updateScoreDisplay(scoreR, scoreB);

    }


}