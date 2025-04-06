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

    private final Random random = new Random();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

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
        System.out.println("AI được kích hoạt");
        
        if (timer != null) {
            timer.stop();
        }
        
        timer = new Timer(100, e -> {
            if ((isRed && gameLogic.isRedTurn()) || (!isRed && !gameLogic.isRedTurn())) {
                System.out.println("AI đang thực hiện nước đi");
                makeMove();
            } else {
                System.out.println("Không phải lượt của AI");
            }
        });
        timer.setRepeats(false);
        System.out.println("Bắt đầu timer để AI đánh trong 100ms");
        timer.start();

        // Timer bảo vệ để kiểm tra nếu AI bị treo
        Timer watchdog = new Timer(3000, e -> {
            System.out.println("Kiểm tra xem AI có bị treo không...");
            if (!isAIWorking()) {
                System.out.println("CẢNH BÁO: AI có vẻ đã bị treo! Đang thử tìm nước đi thay thế...");
                try {
                    List<Move> moves = getAllPossibleMoves();
                    if (!moves.isEmpty()) {
                        Move move = getBestMoveQuick(moves);
                        if (move != null) {
                            System.out.println("Thực hiện nước đi thay thế tại: " + move.row + "," + move.col);
                            SwingUtilities.invokeLater(() -> {
                                try {
                                    gameLogic.makeMove(move.row, move.col);
                                } catch (Exception ex) {
                                    System.err.println("Lỗi khi thực hiện nước đi thay thế: " + ex.getMessage());
                                }
                            });
                        }
                    }
                } catch (Exception ex) {
                    System.err.println("Không thể thực hiện nước đi thay thế: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        });
        watchdog.setRepeats(false);
        watchdog.start();
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
                    System.out.println("AI bắt đầu tìm nước đi...");
                    startTime = System.currentTimeMillis();
                    Move bestMove = findBestMove();
                    
                    if (bestMove != null) {
                        return bestMove;
                    } else {
                        // Nếu không tìm được nước đi tốt nhất, tìm nước đi an toàn nhất
                        List<Move> possibleMoves = getAllPossibleMoves();
                        if (!possibleMoves.isEmpty()) {
                            return getBestMoveQuick(possibleMoves);
                        }
                    }
                    return null;
                } catch (Exception e) {
                    System.err.println("Lỗi khi tìm nước đi: " + e.getMessage());
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void done() {
                try {
                    Move bestMove = get();
                    if (bestMove != null) {
                        System.out.println("AI thực hiện nước đi: " + bestMove.row + "," + bestMove.col);
                        SwingUtilities.invokeLater(() -> {
                            try {
                                gameLogic.makeMove(bestMove.row, bestMove.col);
                            } catch (Exception e) {
                                System.err.println("Lỗi khi thực hiện nước đi: " + e.getMessage());
                                e.printStackTrace();
                            }
                        });
                    } else {
                        System.err.println("Không tìm thấy nước đi phù hợp!");
                    }
                } catch (Exception e) {
                    System.err.println("Lỗi khi thực hiện nước đi: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        };
        worker.execute();
    }

    private Move findBestMove() {
        nodesExplored = 0; // Reset số nút đã khám phá
        startTime = System.currentTimeMillis();
        
        List<Move> possibleMoves = getAllPossibleMoves();
        if (possibleMoves.isEmpty()) {
            return null;
        }

        Move bestMove = null;
        int bestScore = Integer.MIN_VALUE;

        // Đánh giá từng nước đi với minimax
        for (Move move : possibleMoves) {
            prepareSimulation();
            simulateMove(move);
            int score = minimax(MAX_DEPTH - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false);

            if (score > bestScore) {
                bestScore = score;
                bestMove = move;
            }

            if (isTimeUp() || nodesExplored > MAX_NODES) {
                System.out.println("Dừng tìm kiếm do " + 
                    (isTimeUp() ? "hết thời gian" : "đạt giới hạn nút"));
                break;
            }
        }

        return bestMove != null ? bestMove : possibleMoves.get(0);
    }

    private Move findFirstMove() {
        Cell[][] grid = gameLogic.getGrid();
        int center = gameLogic.GRID_SIZE / 2;
        
        // Kiểm tra xem người chơi đã đặt ở giữa chưa
        if (grid[center][center].getState() == CellState.RED_THREE) {
            // Nếu người chơi đã đặt ở giữa, chọn một góc
            int[][] corners = {
                {0, 0}, {0, gameLogic.GRID_SIZE-1},
                {gameLogic.GRID_SIZE-1, 0}, {gameLogic.GRID_SIZE-1, gameLogic.GRID_SIZE-1}
            };
            
            // Đánh giá từng góc
            Move bestCorner = null;
            int bestScore = Integer.MIN_VALUE;
            
            for (int[] corner : corners) {
                if (grid[corner[0]][corner[1]].getState() == CellState.EMPTY) {
                    Move move = new Move(corner[0], corner[1]);
                    prepareSimulation();
                    simulateMove(move);
                    int score = evaluateFirstMovePosition(corner[0], corner[1]);
                    
                    if (score > bestScore) {
                        bestScore = score;
                        bestCorner = move;
                    }
                }
            }
            
            if (bestCorner != null) {
                return bestCorner;
            }
        } else {
            // Nếu người chơi không đặt ở giữa, ưu tiên đặt ở giữa
            if (grid[center][center].getState() == CellState.EMPTY) {
                return new Move(center, center);
            }
            
            // Nếu không thể đặt ở giữa, tìm vị trí tốt nhất gần trung tâm
            int[][] nearCenter = {
                {center-1, center}, {center+1, center},
                {center, center-1}, {center, center+1}
            };
            
            Move bestMove = null;
            int bestScore = Integer.MIN_VALUE;
            
            for (int[] pos : nearCenter) {
                if (isValidPosition(pos[0], pos[1]) && 
                    grid[pos[0]][pos[1]].getState() == CellState.EMPTY) {
                    Move move = new Move(pos[0], pos[1]);
                    prepareSimulation();
                    simulateMove(move);
                    int score = evaluateFirstMovePosition(pos[0], pos[1]);
                    
                    if (score > bestScore) {
                        bestScore = score;
                        bestMove = move;
                    }
                }
            }
            
            if (bestMove != null) {
                return bestMove;
            }
        }
        
        return null;
    }

    private int evaluateFirstMovePosition(int row, int col) {
        int score = 0;
        int center = gameLogic.GRID_SIZE / 2;
        
        // Đánh giá khoảng cách đến trung tâm
        int distanceToCenter = Math.abs(row - center) + Math.abs(col - center);
        
        // Nếu người chơi đã đặt ở giữa
        if (gameLogic.getGrid()[center][center].getState() == CellState.RED_THREE) {
            // Ưu tiên các góc xa quân của đối thủ
            if ((row == 0 || row == gameLogic.GRID_SIZE-1) && 
                (col == 0 || col == gameLogic.GRID_SIZE-1)) {
                score += 500;
            }
        } else {
            // Ưu tiên vị trí gần trung tâm
            score += (4 - distanceToCenter) * 100;
        }
        
        // Đánh giá khả năng phát triển
        score += countInfluenceArea(simulationGrid, row, col) * 50;
        
        return score;
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
        // Tăng thời gian tìm kiếm lên 5000ms (5 giây)
        boolean timeUp = elapsedTime > 5000;
        if (timeUp) {
            System.out.println("Đã vượt quá thời gian: " + elapsedTime + "ms");
        }
        return timeUp;
    }

    private void prepareSimulation() {
        // Sao chép trạng thái hiện tại của bàn cờ vào mảng byte
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
        // Chuyển đổi CellState thành byte để tiết kiệm bộ nhớ
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
        // Chuyển đổi byte thành CellState
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
        // Sử dụng mảng cố định thay vì Queue để tránh tạo nhiều đối tượng
        int[][] explosionQueue = new int[GRID_SIZE * GRID_SIZE][2];
        int queueStart = 0;
        int queueEnd = 0;
        
        // Thêm vị trí đầu tiên vào queue
        explosionQueue[queueEnd][0] = row;
        explosionQueue[queueEnd][1] = col;
        queueEnd++;

        while (queueStart < queueEnd && queueEnd < GRID_SIZE * GRID_SIZE) {
            int r = explosionQueue[queueStart][0];
            int c = explosionQueue[queueStart][1];
            queueStart++;

            simulationGrid[r][c] = 0; // EMPTY
            boolean isRed = explodingState.isRed();

            // Sử dụng mảng directions cố định
            int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
            for (int[] dir : directions) {
                int newRow = r + dir[0], newCol = c + dir[1];

                if (isValidPosition(newRow, newCol)) {
                    byte neighborCode = simulationGrid[newRow][newCol];
                    if (neighborCode == 0) { // EMPTY
                        simulationGrid[newRow][newCol] = (byte)(isRed ? 1 : 5); // RED_ONE : BLUE_ONE
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
                        // Tăng số điểm của quân cùng màu
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
        
        // Kiểm tra điều kiện dừng sớm
        if (isTimeUp() || depth == 0 || nodesExplored > MAX_NODES) {
            return evaluateSimulationBoard();
        }
        
        if (isSimulationGameOver()) {
            return evaluateSimulationBoard();
        }

        List<Move> possibleMoves = getSimulationPossibleMoves();
        if (possibleMoves.isEmpty()) {
            return evaluateSimulationBoard();
        }

        // Chỉ giới hạn số nước đi ở độ sâu đầu tiên
        if (depth == MAX_DEPTH && possibleMoves.size() > INITIAL_MOVES_LIMIT) {
            // Sắp xếp và lọc các nước đi theo đánh giá sơ bộ
            possibleMoves.sort((a, b) -> {
                int scoreA = evaluateMove(a);
                int scoreB = evaluateMove(b);
                return Integer.compare(scoreB, scoreA);
            });
            possibleMoves = possibleMoves.subList(0, INITIAL_MOVES_LIMIT);
        }

        // Lưu trạng thái hiện tại vào mảng tạm
        byte[][] backupGrid = new byte[GRID_SIZE][GRID_SIZE];
        for (int i = 0; i < GRID_SIZE; i++) {
            System.arraycopy(simulationGrid[i], 0, backupGrid[i], 0, GRID_SIZE);
        }
        boolean backupRedTurn = simulationRedTurn;
        boolean backupRedMoved = simulationRedMoved;
        boolean backupBlueMoved = simulationBlueMoved;

        try {
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
                    if (beta <= alpha) break; // Cắt tỉa alpha-beta
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
                    if (beta <= alpha) break; // Cắt tỉa alpha-beta
                }
                return minEval;
            }
        } catch (Exception e) {
            System.err.println("Lỗi trong minimax: " + e.getMessage());
            return evaluateSimulationBoard();
        }
    }

    private int evaluateMove(Move move) {
        // Đánh giá nhanh một nước đi mà không đi sâu vào minimax
        simulateMove(move);
        int score = evaluateSimulationQuick();
        
        // Khôi phục trạng thái
        if (simulationGrid[move.row][move.col] != 0) { // Không phải ô trống
            simulationGrid[move.row][move.col]--;
        }
        
        return score;
    }

    private int evaluateSimulationQuick() {
        int score = 0;
        int myPieces = 0, oppPieces = 0;

        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                byte state = simulationGrid[row][col];
                if (state == 0) continue; // Bỏ qua ô trống

                if ((isRed && state <= 4) || (!isRed && state > 4)) {
                    myPieces++;
                    score += (state <= 4 ? state : state - 4) * 10;
                } else {
                    oppPieces++;
                    score -= (state <= 4 ? state : state - 4) * 10;
                }
            }
        }

        return score + (myPieces - oppPieces) * 50;
    }

    private boolean isWinningMove(Move move) {
        // Lưu trạng thái
        byte[][] backupGrid = new byte[GRID_SIZE][GRID_SIZE];
        for (int i = 0; i < GRID_SIZE; i++) {
            System.arraycopy(simulationGrid[i], 0, backupGrid[i], 0, GRID_SIZE);
        }
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
        for (int i = 0; i < GRID_SIZE; i++) {
            System.arraycopy(backupGrid[i], 0, simulationGrid[i], 0, GRID_SIZE);
        }
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

    private int countSimulationRedPieces() {
        int count = 0;
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                CellState state = convertCodeToState(simulationGrid[row][col]);
                if (state.isRed()) {
                    count++;
                }
            }
        }
        return count;
    }

    private int countSimulationBluePieces() {
        int count = 0;
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                CellState state = convertCodeToState(simulationGrid[row][col]);
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
            for (int row = 0; row < GRID_SIZE; row++) {
                for (int col = 0; col < GRID_SIZE; col++) {
                    if (convertCodeToState(simulationGrid[row][col]) == CellState.EMPTY) {
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
                CellState state = convertCodeToState(simulationGrid[row][col]);

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
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                CellState state = convertCodeToState(simulationGrid[row][col]);
                if (state != CellState.EMPTY) {
                    count++;
                }
            }
        }
        return count;
    }

    private int evaluateSimulationBoard() {
        int score = 0;

        // Đánh giá số lượng quân và điểm
        int myPieces = 0, oppPieces = 0;
        int myDots = 0, oppDots = 0;
        int myThreeDots = 0, oppThreeDots = 0;
        int myStrategicScore = 0, oppStrategicScore = 0;

        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                CellState state = convertCodeToState(simulationGrid[row][col]);
                if ((isRed && state.isRed()) || (!isRed && state.isBlue())) {
                    myPieces++;
                    int dots = getDotCount(state);
                    myDots += dots;
                    if (dots == 3) myThreeDots++;
                    
                    // Đánh giá vị trí chiến lược
                    myStrategicScore += evaluatePosition(row, col);
                    
                    // Đánh giá khả năng tạo chuỗi nổ
                    if (dots >= 2) {
                        myStrategicScore += evaluateChainPotential(simulationGrid, row, col, isRed);
                    }
                } else if ((isRed && state.isBlue()) || (!isRed && state.isRed())) {
                    oppPieces++;
                    int dots = getDotCount(state);
                    oppDots += dots;
                    if (dots == 3) oppThreeDots++;
                    
                    // Đánh giá vị trí chiến lược của đối thủ
                    oppStrategicScore += evaluatePosition(row, col);
                }
            }
        }

        // Kiểm tra trạng thái kết thúc
        if (myPieces == 0) return -100000; // Tránh nước đi dẫn đến thua
        if (oppPieces == 0) return 100000; // Ưu tiên nước đi dẫn đến thắng

        // Tính điểm tổng hợp
        score = (myPieces - oppPieces) * 150 +                    // Trọng số cao cho số lượng quân
                (myDots - oppDots) * 100 +                        // Trọng số cho tổng số điểm
                (myThreeDots - oppThreeDots) * 250 +              // Trọng số rất cao cho quân 3 điểm
                (myStrategicScore - oppStrategicScore) * 80;      // Trọng số cho vị trí chiến lược

        // Thêm điểm thưởng cho các tình huống đặc biệt
        if (myPieces > oppPieces) {
            score += 300; // Thưởng khi có nhiều quân hơn
        }
        
        if (myThreeDots > oppThreeDots) {
            score += 500; // Thưởng lớn khi có nhiều quân 3 điểm hơn
        }

        return score;
    }

    private int evaluatePosition(int row, int col) {
        int score = 0;
        int center = GRID_SIZE / 2;
        
        // Đánh giá khoảng cách đến trung tâm
        int distanceToCenter = Math.abs(row - center) + Math.abs(col - center);
        score += (4 - distanceToCenter) * 30; // Càng gần trung tâm càng tốt
        
        // Đánh giá các góc và cạnh
        if ((row == 0 || row == GRID_SIZE - 1) && 
            (col == 0 || col == GRID_SIZE - 1)) {
            score -= 20; // Trừ điểm cho vị trí góc
        }
        
        // Đánh giá khả năng kiểm soát
        score += countInfluenceArea(simulationGrid, row, col) * 15;
        
        return score;
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

    //Danh gia kha nang xay ra chuoi phat no
    private int evaluateChainPotential(byte[][] grid, int row, int col, boolean isRed) {
        int chainScore = 0;
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];

            if (isValidPosition(newRow, newCol)) {
                CellState neighborState = convertCodeToState(grid[newRow][newCol]);
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

    // Phương thức để kiểm tra xem AI có đang hoạt động hay không
    private boolean isAIWorking() {
        // Kiểm tra tính hợp lệ của bàn cờ
        Cell[][] grid = gameLogic.getGrid();
        int redCount = 0, blueCount = 0;
        
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                CellState state = grid[row][col].getState();
                if (state != null) {
                    if (state.isRed()) redCount++;
                    if (state.isBlue()) blueCount++;
                }
            }
        }
        
        System.out.println("Kiểm tra bàn cờ: Quân đỏ=" + redCount + ", Quân xanh=" + blueCount);
        
        // Nếu game kết thúc, AI không cần làm gì
        if (gameLogic.isGameOver()) {
            System.out.println("Game đã kết thúc, AI không cần đánh tiếp");
            return true;
        }
        
        // Kiểm tra xem có lượt nào đang diễn ra không
        if ((isRed && gameLogic.isRedTurn()) || (!isRed && !gameLogic.isRedTurn())) {
            // Vẫn đến lượt AI nhưng chưa đánh, có thể AI bị treo
            return false;
        }
        
        return true;
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
}