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

    private final int SEARCH_DEPTH = 3;
    private Timer timer;
    private final Random random = new Random();
    private final long TIME_LIMIT = 2000; // Giới hạn thời gian tìm kiếm (1 giây)
    private long startTime;

    private Cell[][] simulationGrid;
    private boolean simulationRedTurn;
    private boolean simulationRedMoved;
    private boolean simulationBlueMoved;

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
        timer.setRepeats(false);
        timer.start();
    }

    public void deactivate() {
        if (timer != null) {
            timer.stop();
        }
    }

    private void makeMove() {
        SwingWorker<Move, Void> worker = new SwingWorker<Move, Void>() {
            @Override
            protected Move doInBackground() {
                return findBestMove();
            }

            @Override
            protected void done() {
                try {
                    Move bestMove = get();
                    if (bestMove != null) {
                        gameLogic.makeMove(bestMove.row, bestMove.col);
                    } else {
                        randomMove();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    randomMove();
                }
            }
        };
        worker.execute();
    }

    private void randomMove() {
        Cell[][] grid = gameLogic.getGrid();
        List<Move> availableMoves = new ArrayList<>();

        for (int row = 0; row < gameLogic.GRID_SIZE; row++) {
            for (int col = 0; col < gameLogic.GRID_SIZE; col++) {
                CellState state = grid[row][col].getState();
                if (state == CellState.EMPTY ||
                        (isRed && state.isRed()) ||
                        (!isRed && state.isBlue())) {
                    availableMoves.add(new Move(row, col));
                }
            }
        }

        if (!availableMoves.isEmpty()) {
            Move randomMove = availableMoves.get(random.nextInt(availableMoves.size()));
            gameLogic.makeMove(randomMove.row, randomMove.col);
        }
    }

    private Move findBestMove() {
        startTime = System.currentTimeMillis();
        List<Move> possibleMoves = getAllPossibleMoves();
        if (possibleMoves.isEmpty()) {
            return null;
        }

        boolean isEarlyGame = countPieces() < 6;
        if (isEarlyGame && Math.random() < 0.3) {
            return possibleMoves.get(random.nextInt(possibleMoves.size()));
        }

        // Kiểm tra nước thắng ngay lập tức
        for (Move move : possibleMoves) {
            prepareSimulation();
            if (isWinningMove(move)) {
                return move;
            }

            if (isTimeUp()) {
                return possibleMoves.get(random.nextInt(possibleMoves.size()));
            }
        }

        Move bestMove = null;
        int bestScore = Integer.MIN_VALUE;

        for (Move move : possibleMoves) {
            if (isTimeUp()) {
                // Nếu hết thời gian mà chưa tìm được nước đi tốt nhất, trả về nước đi tốt nhất hiện tại
                // hoặc một nước đi ngẫu nhiên nếu chưa tìm được nước đi nào
                return (bestMove != null) ? bestMove : possibleMoves.get(random.nextInt(possibleMoves.size()));
            }

            prepareSimulation();
            simulateMove(move);
            int score = minimax(SEARCH_DEPTH - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false);

            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }
        return (bestMove != null) ? bestMove : possibleMoves.get(random.nextInt(possibleMoves.size()));
    }

    private boolean isTimeUp() {
        return System.currentTimeMillis() - startTime > TIME_LIMIT;
    }

    private void prepareSimulation() {
        // Sao chép trạng thái hiện tại của bàn cờ
        simulationGrid = cloneGrid(gameLogic.getGrid());
        simulationRedTurn = gameLogic.isRedTurn();
        simulationRedMoved = gameLogic.isRedHasMoved();
        simulationBlueMoved = gameLogic.isBlueHasMoved();
    }

    private void simulateMove(Move move) {
        Cell cell = simulationGrid[move.row][move.col];
        CellState state = cell.getState();

        if (state == CellState.EMPTY) {
            cell.setState(isRed ? CellState.RED_THREE : CellState.BLUE_THREE);
            if (isRed) {
                simulationRedMoved = true;
            } else {
                simulationBlueMoved = true;
            }
        } else {
            CellState nextState = state.getNextState();
            cell.setState(nextState);
            if (nextState == CellState.RED_FOUR || nextState == CellState.BLUE_FOUR) {
                simulateExplosion(move.row, move.col, nextState);
            }
        }
        simulationRedTurn = !simulationRedTurn;
    }

    private void simulateExplosion(int row, int col, CellState explodingState) {
        Queue<int[]> queue = new LinkedList<>();
        queue.add(new int[]{row, col});

        while (!queue.isEmpty()) {
            int[] cellPos = queue.poll();
            int r = cellPos[0], c = cellPos[1];

            simulationGrid[r][c].setState(CellState.EMPTY);
            boolean isRed = explodingState.isRed();
            int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

            for (int[] dir : directions) {
                int newRow = r + dir[0], newCol = c + dir[1];

                if (isValidPosition(newRow, newCol)) {
                    Cell neighbor = simulationGrid[newRow][newCol];
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

    private int minimax(int depth, int alpha, int beta, boolean isMaximizing) {
        if (isSimulationGameOver() || depth == 0 || isTimeUp()) {
            return evaluateSimulationBoard();
        }

        List<Move> possibleMoves = getSimulationPossibleMoves();
        if (possibleMoves.isEmpty()) {
            return evaluateSimulationBoard();
        }

        // Lưu trạng thái hiện tại
        Cell[][] backupGrid = cloneGrid(simulationGrid);
        boolean backupRedTurn = simulationRedTurn;
        boolean backupRedMoved = simulationRedMoved;
        boolean backupBlueMoved = simulationBlueMoved;

        if (isMaximizing) {
            int val = Integer.MIN_VALUE;
            for (Move move : possibleMoves) {
                simulateMove(move);
                int childVal = minimax(depth - 1, alpha, beta, false);

                // Khôi phục lại trạng thái
                simulationGrid = cloneGrid(backupGrid);
                simulationRedTurn = backupRedTurn;
                simulationRedMoved = backupRedMoved;
                simulationBlueMoved = backupBlueMoved;

                val = Math.max(val, childVal);
                alpha = Math.max(alpha, val);
                if (val >= beta) {
                    return val;
                }
            }
            return val;
        } else {
            int val = Integer.MAX_VALUE;
            for (Move move : possibleMoves) {
                simulateMove(move);
                int childVal = minimax(depth - 1, alpha, beta, true);

                // Khôi phục lại trạng thái
                simulationGrid = cloneGrid(backupGrid);
                simulationRedTurn = backupRedTurn;
                simulationRedMoved = backupRedMoved;
                simulationBlueMoved = backupBlueMoved;

                val = Math.min(val, childVal);
                beta = Math.min(beta, val);
                if (val <= alpha) {
                    return val;
                }
            }
            return val;
        }
    }

    private boolean isWinningMove(Move move) {
        // Lưu trạng thái
        Cell[][] backupGrid = cloneGrid(simulationGrid);
        boolean backupRedTurn = simulationRedTurn;
        boolean backupRedMoved = simulationRedMoved;
        boolean backupBlueMoved = simulationBlueMoved;

        // Thực hiện nước đi
        simulateMove(move);

        // Đánh giá
        boolean isWinning = false;
        if (isRed && countSimulationBluePieces() == 0) {
            isWinning = true;
        } else if (!isRed && countSimulationRedPieces() == 0) {
            isWinning = true;
        }

        // Khôi phục
        simulationGrid = backupGrid;
        simulationRedTurn = backupRedTurn;
        simulationRedMoved = backupRedMoved;
        simulationBlueMoved = backupBlueMoved;

        return isWinning;
    }

    private boolean isSimulationGameOver() {
        if (!simulationRedMoved || !simulationBlueMoved) {
            return false;
        }

        int redCount = 0;
        int blueCount = 0;

        for (int row = 0; row < gameLogic.GRID_SIZE; row++) {
            for (int col = 0; col < gameLogic.GRID_SIZE; col++) {
                CellState state = simulationGrid[row][col].getState();
                if (state.isRed()) {
                    redCount++;
                } else if (state.isBlue()) {
                    blueCount++;
                }
            }
        }

        return (redCount == 0 || blueCount == 0) && (redCount + blueCount > 1);
    }

    private int countSimulationRedPieces() {
        int count = 0;
        for (int row = 0; row < gameLogic.GRID_SIZE; row++) {
            for (int col = 0; col < gameLogic.GRID_SIZE; col++) {
                CellState state = simulationGrid[row][col].getState();
                if (state.isRed()) {
                    count++;
                }
            }
        }
        return count;
    }

    private int countSimulationBluePieces() {
        int count = 0;
        for (int row = 0; row < gameLogic.GRID_SIZE; row++) {
            for (int col = 0; col < gameLogic.GRID_SIZE; col++) {
                CellState state = simulationGrid[row][col].getState();
                if (state.isBlue()) {
                    count++;
                }
            }
        }
        return count;
    }

    private List<Move> getSimulationPossibleMoves() {
        List<Move> moves = new ArrayList<>();
        boolean isRedTurn = simulationRedTurn;

        // For first moves, consider all positions with slight preference for strategic positions
        if ((isRedTurn && !simulationRedMoved) || (!isRedTurn && !simulationBlueMoved)) {
            List<Move> strategicMoves = new ArrayList<>();
            List<Move> normalMoves = new ArrayList<>();

            // Consider all empty cells, but prioritize strategic ones
            for (int row = 0; row < gameLogic.GRID_SIZE; row++) {
                for (int col = 0; col < gameLogic.GRID_SIZE; col++) {
                    if (simulationGrid[row][col].getState() == CellState.EMPTY) {
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
                CellState state = simulationGrid[row][col].getState();

                // For first move of a color, can place on any empty cell
                if (state == CellState.EMPTY && ((isRedTurn && !simulationRedMoved)
                        || (!isRedTurn && !simulationBlueMoved))) {
                    moves.add(new Move(row, col));
                }
                // Otherwise, can only add to existing pieces of our color
                else if ((isRedTurn && state.isRed()) || (!isRedTurn && state.isBlue())) {
                    // Avoid creating early explosions only in the very early game
                    boolean isVeryEarlyGame = countSimulationPieces() < 4;

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

    private int countSimulationPieces() {
        int count = 0;
        for (int row = 0; row < gameLogic.GRID_SIZE; row++) {
            for (int col = 0; col < gameLogic.GRID_SIZE; col++) {
                CellState state = simulationGrid[row][col].getState();
                if (state != CellState.EMPTY) {
                    count++;
                }
            }
        }
        return count;
    }

    private int evaluateSimulationBoard() {
        int score = 0;

        // Danh gia so luong quan co, so diem quan, so luong quan
        int myPieces = 0, oppPieces = 0;
        int myDots = 0, oppDots = 0;
        int myThreeDots = 0, oppThreeDots = 0;

        for (int row = 0; row < gameLogic.GRID_SIZE; row++) {
            for (int col = 0; col < gameLogic.GRID_SIZE; col++) {
                CellState state = simulationGrid[row][col].getState();
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


    //Danh gia vi tri o tren ban co
    private int evaluateStrategicPositions(Cell[][] grid, boolean isRed) {
        int score = 0;
        int gridSize = gameLogic.GRID_SIZE;

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

    //Danh gia kha nang xay ra chuoi phat no
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