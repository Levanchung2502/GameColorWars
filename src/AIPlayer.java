import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AIPlayer {
    private final GameLogic gameLogic;
    private final boolean isRed;
    private final int GRID_SIZE;
    private final int SEARCH_DEPTH = 3;
    private Timer timer;
    private final long TIME_LIMIT = 2000;
    private long startTime;

    // Sử dụng mảng nguyên thủy thay vì đối tượng Cell để tối ưu bộ nhớ
    private byte[][] simulationGrid;
    private boolean simulationRedTurn;
    private boolean simulationRedMoved;
    private boolean simulationBlueMoved;

    private static final int MAX_DEPTH = 3;
    private static final int INITIAL_MOVES_LIMIT = 12;
    private int nodesExplored = 0;
    private static final int MAX_NODES = 10000;

    public AIPlayer(GameLogic gameLogic, boolean isRed) {
        this.gameLogic = gameLogic;
        this.isRed = isRed;
        this.GRID_SIZE = gameLogic.GRID_SIZE;
        this.simulationGrid = new byte[GRID_SIZE][GRID_SIZE];
    }

    public void activate() {
        if (timer != null) {
            timer.stop();
        }
        
        timer = new Timer(100, e -> {
            if ((isRed && gameLogic.isRedTurn()) || (!isRed && !gameLogic.isRedTurn())) {
                makeMove();
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
                try {
                    startTime = System.currentTimeMillis();
                    Move bestMove = findBestMove();
                    return bestMove != null ? bestMove : getBestMoveQuick(getAllPossibleMoves());
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void done() {
                try {
                    Move bestMove = get();
                    if (bestMove != null) {
                        SwingUtilities.invokeLater(() -> {
                            try {
                                gameLogic.makeMove(bestMove.row, bestMove.col);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }

    private Move findBestMove() {
        nodesExplored = 0;
        startTime = System.currentTimeMillis();
        
        List<Move> possibleMoves = getAllPossibleMoves();
        if (possibleMoves.isEmpty()) {
            return null;
        }

        Move bestMove = null;
        int bestScore = Integer.MIN_VALUE;
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;

        // Sắp xếp sơ bộ các nước đi để tăng hiệu quả cắt tỉa
        List<MoveScore> scoredMoves = new ArrayList<>();
        for (Move move : possibleMoves) {
            prepareSimulation();
            simulateMove(move);
            int score = evaluateSimulationQuick();
            scoredMoves.add(new MoveScore(move, score));
        }
        scoredMoves.sort((a, b) -> Integer.compare(b.score, a.score));

        // Áp dụng minimax với alpha-beta cho tất cả nước đi
        for (MoveScore moveScore : scoredMoves) {
            Move move = moveScore.move;
            prepareSimulation();
            simulateMove(move);
            int score = minimax(MAX_DEPTH - 1, alpha, beta, false);

            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
            alpha = Math.max(alpha, score);

            if (isTimeUp() || nodesExplored > MAX_NODES) {
                break;
            }
        }

        return bestMove != null ? bestMove : scoredMoves.get(0).move;
    }

    private Move getBestMoveQuick(List<Move> moves) {
        if (moves.isEmpty()) return null;
        
        Move bestMove = null;
        int bestScore = Integer.MIN_VALUE;
        
        for (Move move : moves) {
            prepareSimulation();
            simulateMove(move);
            int score = evaluateSimulationBoard();
            
            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }
        }
        
        return bestMove != null ? bestMove : moves.get(0);
    }

    private boolean isTimeUp() {
        long elapsedTime = System.currentTimeMillis() - startTime;
        return elapsedTime > 5000;
    }

    private void prepareSimulation() {
        Cell[][] originalGrid = gameLogic.getGrid();
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                simulationGrid[row][col] = convertStateToCode(originalGrid[row][col].getState());
            }
        }
        simulationRedTurn = gameLogic.isRedTurn();
        simulationRedMoved = gameLogic.isRedHasMoved();
        simulationBlueMoved = gameLogic.isBlueHasMoved();
    }

    private byte convertStateToCode(CellState state) {
        switch (state) {
            case EMPTY: return 0;
            case RED_ONE: return 1;
            case RED_TWO: return 2;
            case RED_THREE: return 3;
            case RED_FOUR: return 4;
            case BLUE_ONE: return 5;
            case BLUE_TWO: return 6;
            case BLUE_THREE: return 7;
            case BLUE_FOUR: return 8;
            default: return 0;
        }
    }

    private CellState convertCodeToState(byte code) {
        switch (code) {
            case 0: return CellState.EMPTY;
            case 1: return CellState.RED_ONE;
            case 2: return CellState.RED_TWO;
            case 3: return CellState.RED_THREE;
            case 4: return CellState.RED_FOUR;
            case 5: return CellState.BLUE_ONE;
            case 6: return CellState.BLUE_TWO;
            case 7: return CellState.BLUE_THREE;
            case 8: return CellState.BLUE_FOUR;
            default: return CellState.EMPTY;
        }
    }

    private void simulateMove(Move move) {
        byte state = simulationGrid[move.row][move.col];
        CellState cellState = convertCodeToState(state);

        if (cellState == CellState.EMPTY) {
            simulationGrid[move.row][move.col] = convertStateToCode(
                isRed ? CellState.RED_THREE : CellState.BLUE_THREE
            );
            if (isRed) {
                simulationRedMoved = true;
            } else {
                simulationBlueMoved = true;
            }
        } else {
            CellState nextState = cellState.getNextState();
            simulationGrid[move.row][move.col] = convertStateToCode(nextState);
            if (nextState == CellState.RED_FOUR || nextState == CellState.BLUE_FOUR) {
                simulateExplosion(move.row, move.col, nextState);
            }
        }
        simulationRedTurn = !simulationRedTurn;
    }

    private void simulateExplosion(int row, int col, CellState explodingState) {
        int[][] explosionQueue = new int[GRID_SIZE * GRID_SIZE][2];
        int queueStart = 0;
        int queueEnd = 0;
        
        explosionQueue[queueEnd][0] = row;
        explosionQueue[queueEnd][1] = col;
        queueEnd++;

        while (queueStart < queueEnd && queueEnd < GRID_SIZE * GRID_SIZE) {
            int r = explosionQueue[queueStart][0];
            int c = explosionQueue[queueStart][1];
            queueStart++;

            simulationGrid[r][c] = 0;
            boolean isRed = explodingState.isRed();

            int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
            for (int[] dir : directions) {
                int newRow = r + dir[0], newCol = c + dir[1];

                if (isValidPosition(newRow, newCol)) {
                    byte neighborCode = simulationGrid[newRow][newCol];
                    if (neighborCode == 0) {
                        simulationGrid[newRow][newCol] = (byte)(isRed ? 1 : 5);
                        continue;
                    }

                    CellState neighborState = convertCodeToState(neighborCode);
                    if (isOppositeColor(explodingState, neighborState)) {
                        // Tăng số điểm của quân đối phương
                        byte newCode = (byte)(neighborCode + 1);
                        if (newCode > 8) newCode = 8; // Giới hạn tối đa
                        simulationGrid[newRow][newCol] = newCode;
                        
                        // Kiểm tra nổ
                        if (newCode == 4 || newCode == 8) { // RED_FOUR hoặc BLUE_FOUR
                            if (queueEnd < GRID_SIZE * GRID_SIZE) {
                                explosionQueue[queueEnd][0] = newRow;
                                explosionQueue[queueEnd][1] = newCol;
                                queueEnd++;
                            }
                        }
                    } else if ((isRed && neighborState.isRed()) || (!isRed && neighborState.isBlue())) {
                        byte newCode = (byte)(neighborCode + 1);
                        if (newCode > 8) newCode = 8;
                        simulationGrid[newRow][newCol] = newCode;
                        
                        // Kiểm tra nổ
                        if (newCode == 4 || newCode == 8) {
                            if (queueEnd < GRID_SIZE * GRID_SIZE) {
                                explosionQueue[queueEnd][0] = newRow;
                                explosionQueue[queueEnd][1] = newCol;
                                queueEnd++;
                            }
                        }
                    }
                }
            }
        }
    }

    private int minimax(int depth, int alpha, int beta, boolean isMaximizing) {
        nodesExplored++;
        
        if (isTimeUp() || nodesExplored > MAX_NODES || depth == 0 || isSimulationGameOver()) {
            return evaluateSimulationBoard();
        }

        List<Move> possibleMoves = getSimulationPossibleMoves();
        if (possibleMoves.isEmpty()) {
            return evaluateSimulationBoard();
        }

        // Sắp xếp nước đi để tăng hiệu quả cắt tỉa
        List<MoveScore> scoredMoves = new ArrayList<>();
        for (Move move : possibleMoves) {
            prepareSimulation();
            simulateMove(move);
            int score = evaluateSimulationQuick();
            scoredMoves.add(new MoveScore(move, score));
            // Khôi phục trạng thái
            simulateUndoMove(move);
        }
        scoredMoves.sort((a, b) -> isMaximizing ? 
            Integer.compare(b.score, a.score) : 
            Integer.compare(a.score, b.score));

        byte[][] backupGrid = new byte[GRID_SIZE][GRID_SIZE];
        for (int i = 0; i < GRID_SIZE; i++) {
            System.arraycopy(simulationGrid[i], 0, backupGrid[i], 0, GRID_SIZE);
        }
        boolean backupRedTurn = simulationRedTurn;
        boolean backupRedMoved = simulationRedMoved;
        boolean backupBlueMoved = simulationBlueMoved;

        if (isMaximizing) {
            int maxEval = Integer.MIN_VALUE;
            for (MoveScore moveScore : scoredMoves) {
                simulateMove(moveScore.move);
                int eval = minimax(depth - 1, alpha, beta, false);
                
                // Khôi phục trạng thái
                for (int i = 0; i < GRID_SIZE; i++) {
                    System.arraycopy(backupGrid[i], 0, simulationGrid[i], 0, GRID_SIZE);
                }
                simulationRedTurn = backupRedTurn;
                simulationRedMoved = backupRedMoved;
                simulationBlueMoved = backupBlueMoved;

                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);
                if (beta <= alpha) {
                    break; // Cắt tỉa beta
                }
            }
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;
            for (MoveScore moveScore : scoredMoves) {
                simulateMove(moveScore.move);
                int eval = minimax(depth - 1, alpha, beta, true);
                
                // Khôi phục trạng thái
                for (int i = 0; i < GRID_SIZE; i++) {
                    System.arraycopy(backupGrid[i], 0, simulationGrid[i], 0, GRID_SIZE);
                }
                simulationRedTurn = backupRedTurn;
                simulationRedMoved = backupRedMoved;
                simulationBlueMoved = backupBlueMoved;

                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);
                if (beta <= alpha) {
                    break; // Cắt tỉa alpha
                }
            }
            return minEval;
        }
    }

    private void simulateUndoMove(Move move) {
        // Khôi phục trạng thái trước khi thực hiện nước đi
        if (simulationGrid[move.row][move.col] != 0) {
            simulationGrid[move.row][move.col]--;
        }
        simulationRedTurn = !simulationRedTurn;
    }

    private int evaluateSimulationQuick() {
        int myPieces = 0, oppPieces = 0;
        int myDots = 0, oppDots = 0;

        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                byte state = simulationGrid[row][col];
                if (state == 0) continue;

                if ((isRed && state <= 4) || (!isRed && state > 4)) {
                    myPieces++;
                    myDots += (state <= 4 ? state : state - 4);
                } else {
                    oppPieces++;
                    oppDots += (state <= 4 ? state : state - 4);
                }
            }
        }

        return (myPieces - oppPieces) * 200 + (myDots - oppDots) * 150;
    }

    private boolean isSimulationGameOver() {
        if (!simulationRedMoved || !simulationBlueMoved) {
            return false;
        }

        int redCount = 0;
        int blueCount = 0;

        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                CellState state = convertCodeToState(simulationGrid[row][col]);
                if (state.isRed()) {
                    redCount++;
                } else if (state.isBlue()) {
                    blueCount++;
                }
            }
        }

        return (redCount == 0 || blueCount == 0) && (redCount + blueCount > 1);
    }

    private List<Move> getSimulationPossibleMoves() {
        List<Move> moves = new ArrayList<>();
        boolean isRedTurn = simulationRedTurn;

        // Xử lý đặc biệt cho nước đi đầu tiên
        if ((isRedTurn && !simulationRedMoved) || (!isRedTurn && !simulationBlueMoved)) {
            // Tìm vị trí quân của người chơi (nếu có)
            int playerRow = -1, playerCol = -1;
            for (int row = 0; row < GRID_SIZE; row++) {
                for (int col = 0; col < GRID_SIZE; col++) {
                    CellState state = convertCodeToState(simulationGrid[row][col]);
                    if ((isRedTurn && state.isBlue()) || (!isRedTurn && state.isRed())) {
                        playerRow = row;
                        playerCol = col;
                        break;
                    }
                }
            }

            // Nếu người chơi đã đặt quân
            if (playerRow != -1 && playerCol != -1) {
                // Ưu tiên các góc xa người chơi
                int[][] corners = {{0, 0}, {0, GRID_SIZE-1}, {GRID_SIZE-1, 0}, {GRID_SIZE-1, GRID_SIZE-1}};
                for (int[] corner : corners) {
                    if (convertCodeToState(simulationGrid[corner[0]][corner[1]]) == CellState.EMPTY &&
                        Math.abs(corner[0] - playerRow) + Math.abs(corner[1] - playerCol) >= 3) {
                        moves.add(new Move(corner[0], corner[1]));
                    }
                }
                
                // Nếu không có góc phù hợp, tìm các vị trí cách xa người chơi
                if (moves.isEmpty()) {
                    for (int row = 0; row < GRID_SIZE; row++) {
                        for (int col = 0; col < GRID_SIZE; col++) {
                            if (convertCodeToState(simulationGrid[row][col]) == CellState.EMPTY) {
                                int distance = Math.abs(row - playerRow) + Math.abs(col - playerCol);
                                if (distance >= 3) { // Chỉ chọn các vị trí cách xa ít nhất 3 ô
                                    moves.add(new Move(row, col));
                                }
                            }
                        }
                    }
                }
            } else {
                // Nếu là nước đi đầu tiên của cả ván, ưu tiên vị trí trung tâm hoặc góc
                int center = GRID_SIZE / 2;
                if (convertCodeToState(simulationGrid[center][center]) == CellState.EMPTY) {
                    moves.add(new Move(center, center));
                } else {
                    // Nếu trung tâm đã bị chiếm, chọn góc
                    int[][] corners = {{0, 0}, {0, GRID_SIZE-1}, {GRID_SIZE-1, 0}, {GRID_SIZE-1, GRID_SIZE-1}};
                    for (int[] corner : corners) {
                        if (convertCodeToState(simulationGrid[corner[0]][corner[1]]) == CellState.EMPTY) {
                            moves.add(new Move(corner[0], corner[1]));
                        }
                    }
                }
            }

            // Nếu vẫn không tìm được nước đi phù hợp, thêm tất cả các ô trống còn lại
            if (moves.isEmpty()) {
                for (int row = 0; row < GRID_SIZE; row++) {
                    for (int col = 0; col < GRID_SIZE; col++) {
                        if (convertCodeToState(simulationGrid[row][col]) == CellState.EMPTY) {
                            moves.add(new Move(row, col));
                        }
                    }
                }
            }
            return moves;
        }

        // Xử lý các nước đi tiếp theo như bình thường
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                CellState state = convertCodeToState(simulationGrid[row][col]);
                if ((isRedTurn && state.isRed()) || (!isRedTurn && state.isBlue())) {
                    moves.add(new Move(row, col));
                }
            }
        }

        return moves;
    }

    private int evaluateSimulationBoard() {
        int score = 0;

        // Đánh giá số lượng quân và điểm
        int myPieces = 0, oppPieces = 0;
        int myDots = 0, oppDots = 0;
        int myThreeDots = 0, oppThreeDots = 0;
        int chainPotentialScore = 0;

        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                CellState state = convertCodeToState(simulationGrid[row][col]);
                if ((isRed && state.isRed()) || (!isRed && state.isBlue())) {
                    myPieces++;
                    int dots = getDotCount(state);
                    myDots += dots;
                    if (dots == 3) myThreeDots++;
                    if (dots >= 2) {
                        chainPotentialScore += evaluateChainPotential(simulationGrid, row, col, isRed);
                    }
                } else if ((isRed && state.isBlue()) || (!isRed && state.isRed())) {
                    oppPieces++;
                    int dots = getDotCount(state);
                    oppDots += dots;
                    if (dots == 3) oppThreeDots++;
                }
            }
        }

        // Kiểm tra trạng thái kết thúc
        if (myPieces == 0) return -100000; // Tránh nước đi dẫn đến thua
        if (oppPieces == 0) return 100000; // Ưu tiên nước đi dẫn đến thắng

        // Tính điểm tổng hợp - tăng trọng số cho chuỗi nổ
        score = (myPieces - oppPieces) * 200 +                    // Trọng số cho số lượng quân
                (myDots - oppDots) * 150 +                        // Trọng số cho tổng số điểm
                (myThreeDots - oppThreeDots) * 300 +              // Trọng số cho quân 3 điểm
                chainPotentialScore * 250;                        // Tăng trọng số cho khả năng tạo chuỗi nổ

        // Thêm điểm thưởng cho các tình huống đặc biệt
        if (myPieces > oppPieces) {
            score += 400;
        }
        
        if (myThreeDots > oppThreeDots) {
            score += 600;
        }

        return score;
    }

    private int evaluateChainPotential(byte[][] grid, int row, int col, boolean isRed) {
        int chainScore = 0;
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];

            if (isValidPosition(newRow, newCol)) {
                CellState neighborState = convertCodeToState(grid[newRow][newCol]);
                // Tăng điểm cho chuỗi nổ tiềm năng
                if ((isRed && neighborState.isRed() && getDotCount(neighborState) >= 2) ||
                    (!isRed && neighborState.isBlue() && getDotCount(neighborState) >= 2)) {
                    chainScore += 40; // Tăng gấp đôi điểm cho khả năng tạo chuỗi
                }
                // Tăng điểm cho khả năng ảnh hưởng đến quân đối phương
                if ((isRed && neighborState.isBlue()) || (!isRed && neighborState.isRed())) {
                    chainScore += 30; // Tăng gấp đôi điểm cho khả năng tấn công đối thủ
                }
            }
        }
        return chainScore;
    }

    private int countPieces() {
        int count = 0;
        Cell[][] grid = gameLogic.getGrid();
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                CellState state = grid[row][col].getState();
                if (state != CellState.EMPTY) {
                    count++;
                }
            }
        }
        return count;
    }

    private int countInfluenceArea(byte[][] grid, int row, int col) {
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

    private boolean isValidPosition(int row, int col) {
        return row >= 0 && row < GRID_SIZE && col >= 0 && col < GRID_SIZE;
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

    // Phương thức để kiểm tra xem AI có đang hoạt động hay không
    private List<Move> getAllPossibleMoves() {
        List<Move> moves = new ArrayList<>();
        boolean isRedTurn = gameLogic.isRedTurn();
        Cell[][] grid = gameLogic.getGrid();

        // For first moves, consider all positions with slight preference for strategic positions
        if ((isRedTurn && !gameLogic.isRedHasMoved()) || (!isRedTurn && !gameLogic.isBlueHasMoved())) {
            List<Move> strategicMoves = new ArrayList<>();
            List<Move> normalMoves = new ArrayList<>();

            // Consider all empty cells, but prioritize strategic ones
            for (int row = 0; row < GRID_SIZE; row++) {
                for (int col = 0; col < GRID_SIZE; col++) {
                    if (grid[row][col].getState() == CellState.EMPTY) {
                        Move move = new Move(row, col);

                        // Check if this is a strategic position (near center)
                        int center = GRID_SIZE / 2;
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
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
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

    private static class MoveScore {
        Move move;
        int score;

        MoveScore(Move move, int score) {
            this.move = move;
            this.score = score;
        }
    }
}