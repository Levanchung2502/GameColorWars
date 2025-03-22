import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedList;
import java.util.Queue;
import javax.swing.*;

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
                updateTurnLabel();
                if (isRedTurn) {
                    redHasMoved = true;
                } else {
                    blueHasMoved = true;
                }
                isRedTurn = !isRedTurn;
            }
        } else if ((isRedTurn && state.isRed()) || (!isRedTurn && state.isBlue())) {
            CellState nextState = state.getNextState();
            cell.setState(nextState);
            updateTurnLabel();
            if (nextState == CellState.RED_FOUR || nextState == CellState.BLUE_FOUR) {
                explodeCell(row, col, nextState);
            }
            isRedTurn = !isRedTurn;
        }
        checkGameOver();
    }

    private void updateTurnLabel() {
        turnLabel.setText("Turn: " + (isRedTurn ? "Blue" : "Red"));
        turnLabel.setForeground(isRedTurn ? blueTeamColor : redTeamColor);
    }

    private void explodeCell(int row, int col, CellState explodingState) {
        Queue<int[]> queue = new LinkedList<>();
        queue.add(new int[]{row, col});

        while (!queue.isEmpty()) {
            int[] cellPos = queue.poll();
            int r = cellPos[0], c = cellPos[1];

            grid[r][c].setState(CellState.EMPTY);
            boolean isRed = explodingState.isRed();
            int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

            for (int[] dir : directions) {
                int newRow = r + dir[0], newCol = c + dir[1];

                if (isValidPosition(newRow, newCol)) {
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
                        queue.add(new int[]{newRow, newCol});
                    }
                }
            }
        }
        updateTurnLabel();
        checkGameOver();
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

        boolean onlyRed = true;
        boolean onlyBlue = true;

        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                CellState state = grid[row][col].getState();
                if (state.isRed()) {
                    onlyBlue = false;
                } else if (state.isBlue()) {
                    onlyRed = false;
                }
            }
        }

        if (onlyRed || onlyBlue) {
            String winner = onlyRed ? "Đỏ" : "Xanh";
            JOptionPane.showMessageDialog(parent, "Người chơi " + winner + " thắng!", "Trò chơi kết thúc", JOptionPane.INFORMATION_MESSAGE);
            if (onlyRed) {
                scoreR++;
            } else {
                scoreB++;
            }
            resetGame();
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
    }

}