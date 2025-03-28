import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Queue;
import java.util.LinkedList;

public class AIPlayer {
    private final GameLogic gameLogic;
    private final boolean isRed;

    private final int SEARCH_DEPTH = 4; // Increased depth for better planning
    private Timer timer;
    private final Random random = new Random();

    public AIPlayer(GameLogic gameLogic, boolean isRed) {
        this.gameLogic = gameLogic;
        this.isRed = isRed;
    }

    public void activate() {
        timer = new Timer(200, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if ((isRed && gameLogic.isRedTurn()) || (!isRed && !gameLogic.isRedTurn())) {
                    makeMove();
                }
            }
        });
        timer.start();
    }

    public void deactivate() {
        if (timer != null) {
            timer.stop();
        }
    }

    private void makeMove() {
        Move bestMove = findBestMove();
        if (bestMove != null) {
            gameLogic.makeMove(bestMove.row, bestMove.col);
        }
    }

    private Move findBestMove() {
        List<Move> possibleMoves = getAllPossibleMoves();
        if (possibleMoves.isEmpty()) {
            return null;
        }

        boolean isEarlyGame = countPieces() < 6;
        if (isEarlyGame && Math.random() < 0.3) {
            return possibleMoves.get(random.nextInt(possibleMoves.size()));
        }

        Move bestMove = null;
        int bestScore = Integer.MIN_VALUE;

        for (Move move : possibleMoves) {
            if (isWinningMove(move)) {
                return move; // Return immediately if we find a winning move
            }
        }

        Cell[][] originalGrid = cloneGrid(gameLogic.getGrid());
        boolean originalTurn = gameLogic.isRedTurn();
        boolean originalRedMoved = gameLogic.isRedHasMoved();
        boolean originalBlueMoved = gameLogic.isBlueHasMoved();

        for (Move move : possibleMoves) {
            // Mô phỏng nước đi
            simulateMove(move);
            int score = minimax(SEARCH_DEPTH - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false);

            // Khôi phục trạng thái
            restoreGameState(originalGrid, originalTurn, originalRedMoved, originalBlueMoved);

            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }
        return bestMove;
    }

    private void simulateMove(Move move) {
        Cell cell = gameLogic.getGrid()[move.row][move.col];
        CellState state = cell.getState();

        if (state == CellState.EMPTY) {
            cell.setState(isRed ? CellState.RED_THREE : CellState.BLUE_THREE);
        } else {
            CellState nextState = state.getNextState();
            cell.setState(nextState);
            if (nextState == CellState.RED_FOUR || nextState == CellState.BLUE_FOUR) {
                simulateExplosion(move.row, move.col, nextState);
            }
        }
    }

    private void simulateExplosion(int row, int col, CellState explodingState) {
        Queue<int[]> queue = new LinkedList<>();
        queue.add(new int[]{row, col});

        while (!queue.isEmpty()) {
            int[] cellPos = queue.poll();
            int r = cellPos[0], c = cellPos[1];

            gameLogic.getGrid()[r][c].setState(CellState.EMPTY);
            boolean isRed = explodingState.isRed();
            int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

            for (int[] dir : directions) {
                int newRow = r + dir[0], newCol = c + dir[1];

                if (isValidPosition(newRow, newCol)) {
                    Cell neighbor = gameLogic.getGrid()[newRow][newCol];
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
    }

    private void restoreGameState(Cell[][] originalGrid, boolean originalTurn, boolean originalRedMoved, boolean originalBlueMoved) {
        gameLogic.setGrid(originalGrid);
        gameLogic.setRedTurn(originalTurn);
        gameLogic.setRedHasMoved(originalRedMoved);
        gameLogic.setBlueHasMoved(originalBlueMoved);
    }

    private int minimax(int depth, int alpha, int beta, boolean isMaximizing) {
        if (gameLogic.isGameOver() || depth == 0) {
            return evaluateBoard();
        }

        List<Move> possibleMoves = getAllPossibleMoves();
        if (possibleMoves.isEmpty()) {
            return evaluateBoard();
        }

        Cell[][] originalGrid = cloneGrid(gameLogic.getGrid());
        boolean originalTurn = gameLogic.isRedTurn();
        boolean originalRedMoved = gameLogic.isRedHasMoved();
        boolean originalBlueMoved = gameLogic.isBlueHasMoved();

        if (isMaximizing) {
            int val = Integer.MIN_VALUE;
            for (Move move : possibleMoves) {
                simulateMove(move);
                int childVal = minimax(depth - 1, alpha, beta, false);
                restoreGameState(originalGrid, originalTurn, originalRedMoved, originalBlueMoved);

                val = Math.max(val, childVal);
                if(val >= beta){
                    return val;
                }
            }
            return val;
        } else {
            int val = Integer.MAX_VALUE;
            for (Move move : possibleMoves) {
                simulateMove(move);
                int childVal = minimax(depth - 1, alpha, beta, true);
                restoreGameState(originalGrid, originalTurn, originalRedMoved, originalBlueMoved);

                val = Math.min(val, childVal);
                beta = Math.min(beta, childVal);
                if (val <= alpha) {
                    return val;
                }
            }
            return val;
        }
    }

    private boolean isWinningMove(Move move) {
        Cell[][] originalGrid = gameLogic.getGrid();
        Cell[][] cloneGrid = cloneGrid(originalGrid);
        boolean originalTurn = gameLogic.isRedTurn();
        boolean originalRedMoved = gameLogic.isRedHasMoved();
        boolean originalBlueMoved = gameLogic.isBlueHasMoved();

        gameLogic.makeMove(move.row, move.col);

        boolean isWinning = false;
        if (isRed && countBluePieces() == 0) {
            isWinning = true;
        } else if (!isRed && countRedPieces() == 0) {
            isWinning = true;
        }

        gameLogic.setGrid(cloneGrid);
        gameLogic.setRedTurn(originalTurn);
        gameLogic.setRedHasMoved(originalRedMoved);
        gameLogic.setBlueHasMoved(originalBlueMoved);

        return isWinning;
    }

    private int countPieces() {
        int count = 0;
        Cell[][] grid = gameLogic.getGrid();
        for (int row = 0; row < gameLogic.GRID_SIZE; row++) {
            for (int col = 0; col < gameLogic.GRID_SIZE; col++) {
                CellState state = grid[row][col].getState();
                if (state != CellState.EMPTY) {
                    count++;
                }
            }
        }
        return count;
    }

    private int countRedPieces() {
        int count = 0;
        Cell[][] grid = gameLogic.getGrid();
        for (int row = 0; row < gameLogic.GRID_SIZE; row++) {
            for (int col = 0; col < gameLogic.GRID_SIZE; col++) {
                CellState state = grid[row][col].getState();
                if (state.isRed()) {
                    count++;
                }
            }
        }
        return count;
    }

    private int countBluePieces() {
        int count = 0;
        Cell[][] grid = gameLogic.getGrid();
        for (int row = 0; row < gameLogic.GRID_SIZE; row++) {
            for (int col = 0; col < gameLogic.GRID_SIZE; col++) {
                CellState state = grid[row][col].getState();
                if (state.isBlue()) {
                    count++;
                }
            }
        }
        return count;
    }


    private int evaluateBoard() {
            int score = 0;
            Cell[][] grid = gameLogic.getGrid();

            // Count pieces and calculate advantage
            int myPieces = 0, oppPieces = 0;
            int myDots = 0, oppDots = 0;
            int myThreeDots = 0, oppThreeDots = 0;

            for (int row = 0; row < gameLogic.GRID_SIZE; row++) {
                for (int col = 0; col < gameLogic.GRID_SIZE; col++) {
                    CellState state = grid[row][col].getState();
                    if ((isRed && state.isRed()) || (!isRed && state.isBlue())) {
                        myPieces++;
                        int dots = getDotCount(state);
                        myDots += dots;
                        if (dots == 3) myThreeDots++;
                    } else if ((isRed && state.isBlue()) || (!isRed && state.isRed())) {
                        oppPieces++;
                        int dots = getDotCount(state);
                        oppDots += dots;
                        if (dots == 3) oppThreeDots++;
                    }
                }
            }

            // Quick terminal state check
            if (myPieces == 0) return -10000;
            if (oppPieces == 0) return 10000;

            // Simplified scoring
            score = (myPieces - oppPieces) * 15 +
                    (myDots - oppDots) * 10 +
                    (myThreeDots - oppThreeDots) * 25;

            return score;
    }

    private int countInfluenceArea(Cell[][] grid, int row, int col) {
        int count = 0;
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];
            if (isValidPosition(newRow, newCol)) {
                count++;
            }
        }

        return count;
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

    private Cell[][] cloneGrid(Cell[][] originalGrid) {
        Cell[][] cloneGrid = new Cell[gameLogic.GRID_SIZE][gameLogic.GRID_SIZE];
        for (int row = 0; row < gameLogic.GRID_SIZE; row++) {
            for (int col = 0; col < gameLogic.GRID_SIZE; col++) {
                cloneGrid[row][col] = originalGrid[row][col].clone();
            }
        }
        return cloneGrid;
    }

    private List<Move> getAllPossibleMoves() {
        List<Move> moves = new ArrayList<>();
        boolean isRedTurn = gameLogic.isRedTurn();
        Cell[][] grid = gameLogic.getGrid();

        // For first moves, consider all positions with slight preference for strategic positions
        if ((isRedTurn && !gameLogic.isRedHasMoved()) || (!isRedTurn && !gameLogic.isBlueHasMoved())) {
            System.out.println("AI đang xét nước đi đầu tiên");

            List<Move> strategicMoves = new ArrayList<>();
            List<Move> normalMoves = new ArrayList<>();

            // Consider all empty cells, but prioritize strategic ones
            for (int row = 0; row < gameLogic.GRID_SIZE; row++) {
                for (int col = 0; col < gameLogic.GRID_SIZE; col++) {
                    if (grid[row][col].getState() == CellState.EMPTY) {
                        Move move = new Move(row, col);

                        // Check if this is a strategic position (near center)
                        int center = gameLogic.GRID_SIZE / 2;
                        int distanceToCenter = Math.abs(row - center) + Math.abs(col - center);

                        if (distanceToCenter <= 2) {
                            strategicMoves.add(move);
                        } else {
                            normalMoves.add(move);
                        }
                    }
                }
            }

            // Combine the lists with strategic moves first
            moves.addAll(strategicMoves);
            moves.addAll(normalMoves);

            return moves;
        }

        // For subsequent moves
        for (int row = 0; row < gameLogic.GRID_SIZE; row++) {
            for (int col = 0; col < gameLogic.GRID_SIZE; col++) {
                CellState state = grid[row][col].getState();

                // For first move of a color, can place on any empty cell
                if (state == CellState.EMPTY && ((isRedTurn && !gameLogic.isRedHasMoved())
                        || (!isRedTurn && !gameLogic.isBlueHasMoved()))) {
                    moves.add(new Move(row, col));
                }
                // Otherwise, can only add to existing pieces of our color
                else if ((isRedTurn && state.isRed()) || (!isRedTurn && state.isBlue())) {
                    // Avoid creating early explosions only in the very early game
                    boolean isVeryEarlyGame = countPieces() < 4;

                    if (isVeryEarlyGame) {
                        if ((isRedTurn && state == CellState.RED_THREE) ||
                                (!isRedTurn && state == CellState.BLUE_THREE)) {
                            // Only avoid early explosions in very early game
                            continue;
                        }
                    }

                    moves.add(new Move(row, col));
                }
            }
        }

        return moves;
    }

    private boolean isValidPosition(int row, int col) {
        return row >= 0 && row < gameLogic.GRID_SIZE && col >= 0 && col < gameLogic.GRID_SIZE;
    }

    private boolean isOppositeColor(CellState s1, CellState s2) {
        return (s1.isRed() && s2.isBlue()) || (s1.isBlue() && s2.isRed());
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

    private int evaluateTerminalState() {
        int redCells = 0, blueCells = 0;
        Cell[][] grid = gameLogic.getGrid();

        for (int row = 0; row < gameLogic.GRID_SIZE; row++) {
            for (int col = 0; col < gameLogic.GRID_SIZE; col++) {
                CellState state = grid[row][col].getState();
                if (state.isRed()) {
                    redCells++;
                } else if (state.isBlue()) {
                    blueCells++;
                }
            }
        }

        if (redCells == 0) {
            return isRed ? -100000 : 100000;
        }

        if (blueCells == 0) {
            return isRed ? 100000 : -100000;
        }

        return 0; // Should not happen in a terminal state
    }
    private int evaluateStrategicPositions(Cell[][] grid, boolean isRed) {
        int score = 0;
        int gridSize = gameLogic.GRID_SIZE;

        // Evaluate board control - corners are less important, center and middle areas more
        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                CellState state = grid[row][col].getState();
                if ((isRed && state.isRed()) || (!isRed && state.isBlue())) {
                    // Center control bonus
                    int centerRow = gridSize / 2;
                    int centerCol = gridSize / 2;
                    int distanceToCenter = Math.abs(row - centerRow) + Math.abs(col - centerCol);

                    // Strategic positioning value (center is most valuable)
                    int positionValue = 12 - distanceToCenter;
                    score += positionValue;

                    // Bonus for controlling cells that can influence multiple areas
                    int influenceScore = countInfluenceArea(grid, row, col);
                    score += influenceScore;
                }
            }
        }

        // Evaluate tactical advantage - looking for patterns like surrounding opponent pieces
        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                CellState state = grid[row][col].getState();

                // If this is an opponent's piece ready to explode, check if it's dangerous to us
                if ((isRed && state == CellState.BLUE_THREE) || (!isRed && state == CellState.RED_THREE)) {
                    int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
                    for (int[] dir : directions) {
                        int newRow = row + dir[0];
                        int newCol = col + dir[1];
                        if (isValidPosition(newRow, newCol)) {
                            CellState neighbor = grid[newRow][newCol].getState();
                            if ((isRed && neighbor.isRed()) || (!isRed && neighbor.isBlue())) {
                                // There's a risk of our piece being affected by opponent's explosion
                                score -= 15;
                            }
                        }
                    }
                }
            }
        }

        return score;
    }

    private int evaluateChainPotential(Cell[][] grid, int row, int col, boolean isRed) {
        int chainScore = 0;
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];

            if (isValidPosition(newRow, newCol)) {
                CellState neighborState = grid[newRow][newCol].getState();
                // Check for potential chain reactions
                if ((isRed && neighborState.isRed() && getDotCount(neighborState) >= 2) ||
                        (!isRed && neighborState.isBlue() && getDotCount(neighborState) >= 2)) {
                    chainScore += 10;
                }
                // Check for opponent pieces that would be affected
                if ((isRed && neighborState.isBlue()) ||
                        (!isRed && neighborState.isRed())) {
                    chainScore += 5;
                }
            }
        }

        return chainScore;
    }
}