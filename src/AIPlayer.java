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
    private Timer timer;
    private long startTime;

    private byte[][] simulationGrid;
    private boolean simulationRedTurn;
    private boolean simulationRedMoved;
    private boolean simulationBlueMoved;

    private static final int MAX_DEPTH = 3;
    private int nodesExplored = 0;
    private static final int MAX_NODES = 15000;

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

        boolean isAITurn = (isRed && gameLogic.isRedTurn()) || (!isRed && !gameLogic.isRedTurn());
        
        if (!isAITurn) {
            return;
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


    //Tao nuoc di
    private void makeMove() {
        SwingWorker<Move, Void> worker = new SwingWorker<Move, Void>() {
            @Override
            protected Move doInBackground() {
                try {
                    startTime = System.currentTimeMillis();
                    Move bestMove = findBestMove();
                    return bestMove;
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

    // Tìm nước đi tốt nhất bằng thuật toán minimax với alpha-beta
    private Move findBestMove() {
        nodesExplored = 0;
        startTime = System.currentTimeMillis();
        
        List<Move> possibleMoves = getAllPossibleMoves();
        if (possibleMoves.isEmpty()) {
            return null;
        }
        
        // First check if we have any moves that create explosions and prioritize them
        for (Move move : possibleMoves) {
            Cell cell = gameLogic.getGrid()[move.row][move.col];
            CellState state = cell.getState();
            
            // If this is a 3-dot piece, prioritize it immediately for explosion
            if ((isRed && state == CellState.RED_THREE) || (!isRed && state == CellState.BLUE_THREE)) {
                return move;
            }
        }

        Move bestMove = null;
        int bestScore = Integer.MIN_VALUE;
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;

        // Áp dụng minimax với alpha-beta cho tất cả nước đi
        for (Move move : possibleMoves) {
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

        return bestMove != null ? bestMove : possibleMoves.get(0);
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
        
        if (nodesExplored > MAX_NODES || depth == 0 || isSimulationGameOver()) {
            return evaluateSimulationBoard();
        }

        List<Move> possibleMoves = getSimulationPossibleMoves();
        if (possibleMoves.isEmpty()) {
            return evaluateSimulationBoard();
        }
        
        // Lưu trạng thái hiện tại
        byte[][] backupGrid = new byte[GRID_SIZE][GRID_SIZE];
        for (int i = 0; i < GRID_SIZE; i++) {
            System.arraycopy(simulationGrid[i], 0, backupGrid[i], 0, GRID_SIZE);
        }
        boolean backupRedTurn = simulationRedTurn;
        boolean backupRedMoved = simulationRedMoved;
        boolean backupBlueMoved = simulationBlueMoved;

        if (isMaximizing) {
            int maxEval = Integer.MIN_VALUE;
            for (Move move : possibleMoves) {
                simulateMove(move);
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
            for (Move move : possibleMoves) {
                simulateMove(move);
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
        int opponentChainThreat = 0;
        int opponentThreeDotThreat = 0;

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
                    if (dots == 3) {
                        oppThreeDots++;
                        opponentThreeDotThreat += evaluateThreeDotThreat(simulationGrid, row, col, isRed);
                    }
                    if (dots >= 2) {
                        opponentChainThreat += evaluateOpponentChainThreat(simulationGrid, row, col, isRed);
                    }
                }
            }
        }

        // Kiểm tra trạng thái kết thúc
        if (myPieces == 0) return -100000;
        if (oppPieces == 0) return 100000;

        // Tính điểm tổng hợp với trọng số mới
        score = (myPieces - oppPieces) * 200 +                    // Trọng số cho số lượng quân
                (myDots - oppDots) * 150 +                        // Trọng số cho tổng số điểm
                (myThreeDots) * 800 +                             // INCREASED weight for 3-dot pieces
                chainPotentialScore * 600 -                       // INCREASED weight for chain potential
                opponentChainThreat * 350 -                       // Trừ điểm cho mối đe dọa từ đối phương
                opponentThreeDotThreat * 500;                     // Trừ điểm mạnh cho mối đe dọa từ quân 3 điểm đối phương

        // Thêm điểm thưởng cho các tình huống đặc biệt
        if (myPieces > oppPieces) {
            score += 500;
        }
        
        if (myThreeDots > 0) {
            score += 700; // INCREASED bonus for having 3-dot pieces
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

    private int evaluateOpponentChainThreat(byte[][] grid, int row, int col, boolean isRed) {
        int threatScore = 0;
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];

            if (isValidPosition(newRow, newCol)) {
                CellState neighborState = convertCodeToState(grid[newRow][newCol]);
                // Tăng điểm cho mối đe dọa từ đối phương
                if ((isRed && neighborState.isBlue() && getDotCount(neighborState) >= 2) ||
                    (!isRed && neighborState.isRed() && getDotCount(neighborState) >= 2)) {
                    threatScore += 50;
                }
            }
        }
        return threatScore;
    }

    private int evaluateThreeDotThreat(byte[][] grid, int row, int col, boolean isRed) {
        int threatScore = 0;
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];

            if (isValidPosition(newRow, newCol)) {
                CellState neighborState = convertCodeToState(grid[newRow][newCol]);
                // Tăng điểm cho mối đe dọa từ quân 3 điểm đối phương
                if ((isRed && neighborState.isBlue() && getDotCount(neighborState) == 3) ||
                    (!isRed && neighborState.isRed() && getDotCount(neighborState) == 3)) {
                    threatScore += 100; // Tăng điểm mạnh cho mối đe dọa từ quân 3 điểm
                }
            }
        }
        return threatScore;
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

    // Phương thức để kiểm tra xem AI có đang hoạt động hay không
    private List<Move> getAllPossibleMoves() {
        List<Move> moves = new ArrayList<>();
        List<Move> explosionMoves = new ArrayList<>(); // Special list for moves that cause explosions
        boolean isRedTurn = gameLogic.isRedTurn();
        Cell[][] grid = gameLogic.getGrid();

        // First, check for any 3-dot pieces that can be clicked to create explosions (4-dot)
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                CellState state = grid[row][col].getState();
                
                // Find 3-dot pieces of our color
                if ((isRedTurn && state == CellState.RED_THREE) || 
                    (!isRedTurn && state == CellState.BLUE_THREE)) {
                    Move explosionMove = new Move(row, col);
                    explosionMoves.add(explosionMove);
                }
            }
        }
        
        // If we found explosion moves, prioritize them highly
        if (!explosionMoves.isEmpty()) {
            return explosionMoves;
        }

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

        // For subsequent moves, prioritize pieces with higher dot counts
        List<Move> highDotMoves = new ArrayList<>(); // For 2-dot pieces
        List<Move> lowDotMoves = new ArrayList<>();  // For 1-dot pieces
        
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
                    // Check the dot count
                    int dots = getDotCount(state);
                    Move move = new Move(row, col);
                    
                    if (dots == 2) {
                        highDotMoves.add(move);
                    } else if (dots == 1) {
                        lowDotMoves.add(move);
                    } else if (dots == 3) {
                        // Already handled in explosionMoves
                    }
                }
            }
        }
        
        // Add moves in priority order
        moves.addAll(highDotMoves);
        moves.addAll(lowDotMoves);
        
        // If no valid moves found, try any move
        if (moves.isEmpty()) {
            for (int row = 0; row < GRID_SIZE; row++) {
                for (int col = 0; col < GRID_SIZE; col++) {
                    CellState state = grid[row][col].getState();
                    if ((isRedTurn && state.isRed()) || (!isRedTurn && state.isBlue())) {
                        moves.add(new Move(row, col));
                    }
                }
            }
        }

        return moves;
    }

}