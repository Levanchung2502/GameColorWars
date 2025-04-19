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

    private static final int MAX_DEPTH = 4;
    private int nodesExplored = 0;
    private static final int MAX_NODES = 50000;

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
        
        List<Move> possibleMoves = getPossibleMoves(false);
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

    //Tạo bảng mô phỏng
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

    //Chuyển trạng thái ô thành mã số
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

    //Chuyển mã số thành trạng thái ô
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

    //Mô phỏng nổ
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

        List<Move> possibleMoves = getPossibleMoves(true);
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

    //Kiểm tra xem trò chơi đã kết thúc hay chưa
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

    //Đánh giá các nước đi
    private int evaluateSimulationBoard() {
        int score = 0;

        // Đánh giá số lượng quân và điểm
        int myPieces = 0, oppPieces = 0;
        int myDots = 0, oppDots = 0;
        int myThreeDots = 0, oppThreeDots = 0;
        int chainPotentialScore = 0;
        int opponentChainThreat = 0;
        int positionScore = 0;  // Thêm điểm vị trí

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
                    
                    // Đánh giá vị trí chiến lược
                    positionScore += evaluatePosition(row, col, dots, isRed);
                } else if ((isRed && state.isBlue()) || (!isRed && state.isRed())) {
                    oppPieces++;
                    int dots = getDotCount(state);
                    oppDots += dots;
                    if (dots == 3) {
                        oppThreeDots++;
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
                (myDots - oppDots) * 180 +                        // Tăng trọng số cho tổng số điểm
                (myThreeDots) * 1000 +                            // Tăng mạnh trọng số cho quân 3 điểm
                chainPotentialScore * 750 -                       // Tăng trọng số khả năng tạo chuỗi nổ
                opponentChainThreat * 500 +                       // Tăng trọng số cho mối đe dọa từ đối phương
                positionScore * 300;                              // Thêm điểm vị trí chiến lược

        // Tỷ lệ quân tương đối
        if (oppPieces > 0) {
            float pieceRatio = (float) myPieces / oppPieces;
            if (pieceRatio > 1.5) {
                score += 800;  
            }
        }
        
        // Điểm phạt khi bị đối thủ bao vây
        int surroundedPenalty = evaluateSurroundedPieces(simulationGrid, isRed);
        score -= surroundedPenalty * 250;
        
        // Thêm điểm thưởng cho các tình huống đặc biệt
        if (myPieces > oppPieces) {
            score += 500;
        }
        
        if (myThreeDots > 0) {
            score += 900; // Tăng điểm thưởng cho việc có quân 3 điểm
        }

        return score;
    }

    // Đánh giá vị trí chiến lược
    private int evaluatePosition(int row, int col, int dots, boolean isRed) {
        int posScore = 0;
        
        // Vị trí góc có giá trị cao vì khó bị bao vây
        if ((row == 0 || row == GRID_SIZE-1) && (col == 0 || col == GRID_SIZE-1)) {
            posScore += 50;
        }
        
        // Vị trí cạnh có giá trị trung bình
        else if (row == 0 || row == GRID_SIZE-1 || col == 0 || col == GRID_SIZE-1) {
            posScore += 25;
        }
        
        // Vị trí trung tâm có giá trị cho việc kiểm soát bàn cờ
        int center = GRID_SIZE / 2;
        int distanceToCenter = Math.abs(row - center) + Math.abs(col - center);
        if (distanceToCenter <= 1) {
            posScore += 40;
        }
        
        // Quân 3 điểm ở vị trí tốt có giá trị rất cao
        if (dots == 3) {
            posScore += 30;
            
            // Đặc biệt cao nếu ở vị trí có thể gây chuỗi nổ
            int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
            for (int[] dir : directions) {
                int newRow = row + dir[0], newCol = col + dir[1];
                if (isValidPosition(newRow, newCol)) {
                    CellState neighborState = convertCodeToState(simulationGrid[newRow][newCol]);
                    // Tăng giá trị nếu kế bên là quân cùng màu
                    if ((isRed && neighborState.isRed()) || (!isRed && neighborState.isBlue())) {
                        posScore += 40;
                    }
                }
            }
        }
        
        return posScore;
    }

    //Đánh giá mức độ bị bao vây
    private int evaluateSurroundedPieces(byte[][] grid, boolean isRed) {
        int surroundedCount = 0;
        
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                CellState state = convertCodeToState(grid[row][col]);
                
                // Chỉ xét quân của chúng ta
                if ((isRed && state.isRed()) || (!isRed && state.isBlue())) {
                    int opponentNeighbors = 0;
                    int totalNeighbors = 0;
                    
                    int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
                    for (int[] dir : directions) {
                        int newRow = row + dir[0], newCol = col + dir[1];
                        if (isValidPosition(newRow, newCol)) {
                            totalNeighbors++;
                            CellState neighborState = convertCodeToState(grid[newRow][newCol]);
                            if ((isRed && neighborState.isBlue()) || (!isRed && neighborState.isRed())) {
                                opponentNeighbors++;
                            }
                        }
                    }
                    
                    // Nếu đa số hàng xóm là quân đối phương, quân này bị bao vây
                    if (opponentNeighbors > totalNeighbors / 2) {
                        surroundedCount++;
                    }
                }
            }
        }
        
        return surroundedCount;
    }


    //Đánh giá khả năng tạo chuỗi nổ
    private int evaluateChainPotential(byte[][] grid, int row, int col, boolean isRed) {
        int chainScore = 0;
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        
        // Đánh giá quân hiện tại
        CellState currentState = convertCodeToState(grid[row][col]);
        int currentDots = getDotCount(currentState);
        
        // Quân càng nhiều điểm càng có khả năng tạo chuỗi nổ
        chainScore += currentDots * 20;

        // Đánh giá các quân xung quanh
        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];

            if (isValidPosition(newRow, newCol)) {
                CellState neighborState = convertCodeToState(grid[newRow][newCol]);
                
                // Tăng điểm cho chuỗi nổ tiềm năng với quân cùng màu
                if ((isRed && neighborState.isRed()) || (!isRed && neighborState.isBlue())) {
                    int neighborDots = getDotCount(neighborState);

                    chainScore += 40 + neighborDots * 15;
                    
                    // Đặc biệt ưu tiên chuỗi có quân 3 điểm
                    if (neighborDots == 3 || currentDots == 3) {
                        chainScore += 100;
                    }

                    if ((currentDots == 2 && neighborDots == 3) || 
                        (currentDots == 3 && neighborDots == 2) ||
                        (currentDots == 2 && neighborDots == 2) ||
                        (currentDots == 3 && neighborDots == 3)) {
                        chainScore += 120;
                    }
                }
                
                // Tăng điểm cho khả năng ảnh hưởng đến quân đối phương
                if ((isRed && neighborState.isBlue()) || (!isRed && neighborState.isRed())) {
                    chainScore += 60;
                    
                    // Nếu quân đối phương có nhiều điểm, gây ảnh hưởng có giá trị cao
                    int oppDots = getDotCount(neighborState);
                    if (oppDots >= 2) {
                        chainScore += oppDots * 20;
                    }
                }
            }
        }
        
        // Đánh giá chuỗi mở rộng hơn (kiểm tra các quân cách 2 ô)
        if (currentDots >= 2) {
            for (int[] dir1 : directions) {
                for (int[] dir2 : directions) {
                    int extendedRow = row + dir1[0] + dir2[0];
                    int extendedCol = col + dir1[1] + dir2[1];
                    
                    if (isValidPosition(extendedRow, extendedCol)) {
                        CellState extendedState = convertCodeToState(grid[extendedRow][extendedCol]);
                        if ((isRed && extendedState.isRed() && getDotCount(extendedState) >= 2) ||
                            (!isRed && extendedState.isBlue() && getDotCount(extendedState) >= 2)) {
                            chainScore += 30;
                        }
                    }
                }
            }
        }
        
        return chainScore;
    }

    // Cải thiện đánh giá mối đe dọa từ đối phương
    private int evaluateOpponentChainThreat(byte[][] grid, int row, int col, boolean isRed) {
        int threatScore = 0;
        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        
        CellState currentState = convertCodeToState(grid[row][col]);
        int currentDots = getDotCount(currentState);
        
        // Quân đối phương càng nhiều điểm càng nguy hiểm
        threatScore += currentDots * 30;

        for (int[] dir : directions) {
            int newRow = row + dir[0];
            int newCol = col + dir[1];

            if (isValidPosition(newRow, newCol)) {
                CellState neighborState = convertCodeToState(grid[newRow][newCol]);
                
                // Đánh giá mối đe dọa từ quân khác của đối phương
                if ((isRed && neighborState.isBlue()) || (!isRed && neighborState.isRed())) {
                    int neighborDots = getDotCount(neighborState);
                    threatScore += 50 + neighborDots * 20;
                    
                    // Mối đe dọa cao với chuỗi 2-2, 2-3, 3-2, 3-3
                    if ((currentDots == 2 && neighborDots == 2) ||
                        (currentDots == 2 && neighborDots == 3) ||
                        (currentDots == 3 && neighborDots == 2) ||
                        (currentDots == 3 && neighborDots == 3)) {
                        threatScore += 150;
                    }
                    
                    // Đánh giá mối đe dọa từ quân 3 điểm đối phương (từ hàm evaluateThreeDotThreat)
                    if (neighborDots == 3) {
                        threatScore += 100;
                    }
                }
                
                // Đánh giá mối đe dọa đến quân của chúng ta
                if ((isRed && neighborState.isRed()) || (!isRed && neighborState.isBlue())) {
                    threatScore += 40; // Quân đối phương gần quân của chúng ta
                }
            }
        }
        
        // Kiểm tra mẫu hình đe dọa đặc biệt: quân 3 điểm có thể gây chuỗi nổ
        if (currentDots == 3) {
            int threatCount = 0;
            for (int[] dir : directions) {
                int newRow = row + dir[0];
                int newCol = col + dir[1];
                
                if (isValidPosition(newRow, newCol)) {
                    CellState neighborState = convertCodeToState(grid[newRow][newCol]);
                    // Nếu hàng xóm cùng màu với đối phương
                    if ((isRed && neighborState.isBlue()) || (!isRed && neighborState.isRed())) {
                        threatCount++;
                    }
                }
            }
            
            // Nếu quân 3 điểm có ít nhất 2 quân liền kề cùng màu, tăng mạnh điểm đe dọa
            if (threatCount >= 2) {
                threatScore += 250;
            }
        }
        
        return threatScore;
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


     //Tìm tất cả các nước đi có thể cho AI trong bất kỳ trạng thái nào.
     // isSimulation true nếu đang trong mô phỏng (sử dụng simulationGrid), false nếu đang tìm nước đi thực tế

    private List<Move> getPossibleMoves(boolean isSimulation) {
        List<Move> moves = new ArrayList<>();
        
        // Xác định trạng thái và dữ liệu hiện tại dựa trên mode
        boolean isCurrentRedTurn, isRedHasMoved, isBlueHasMoved;
        
        if (isSimulation) {
            isCurrentRedTurn = simulationRedTurn;
            isRedHasMoved = simulationRedMoved;
            isBlueHasMoved = simulationBlueMoved;
        } else {
            isCurrentRedTurn = gameLogic.isRedTurn();
            isRedHasMoved = gameLogic.isRedHasMoved();
            isBlueHasMoved = gameLogic.isBlueHasMoved();
        }
        
        // 1. Ưu tiên nước đi nổ từ quân 3 điểm (quân có 3 điểm)
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                CellState state;
                
                if (isSimulation) {
                    state = convertCodeToState(simulationGrid[row][col]);
                } else {
                    state = gameLogic.getGrid()[row][col].getState();
                }
                
                if ((isCurrentRedTurn && state == CellState.RED_THREE) || 
                    (!isCurrentRedTurn && state == CellState.BLUE_THREE)) {
                    return Collections.singletonList(new Move(row, col)); // Ưu tiên cao nhất, trả về ngay
                }
            }
        }
        
        // 2. Xử lý nước đi đầu tiên của mỗi màu
        if ((isCurrentRedTurn && !isRedHasMoved) || (!isCurrentRedTurn && !isBlueHasMoved)) {
            int center = GRID_SIZE / 2;
            List<Move> strategicMoves = new ArrayList<>();
            
            // Nếu trong mô phỏng và đối thủ đã di chuyển, ưu tiên các vị trí xa đối thủ
            if (isSimulation && ((!isCurrentRedTurn && isRedHasMoved) || (isCurrentRedTurn && isBlueHasMoved))) {
                // Tìm vị trí quân của đối thủ
                int opponentRow = -1, opponentCol = -1;
                
                for (int row = 0; row < GRID_SIZE; row++) {
                    for (int col = 0; col < GRID_SIZE; col++) {
                        CellState state = convertCodeToState(simulationGrid[row][col]);
                        if ((isCurrentRedTurn && state.isBlue()) || (!isCurrentRedTurn && state.isRed())) {
                            opponentRow = row;
                            opponentCol = col;
                            break;
                        }
                    }
                    if (opponentRow != -1) break;
                }
                
                // Nếu tìm thấy quân đối thủ
                if (opponentRow != -1) {
                    // Ưu tiên các góc xa quân đối thủ
                    int[][] corners = {{0, 0}, {0, GRID_SIZE-1}, {GRID_SIZE-1, 0}, {GRID_SIZE-1, GRID_SIZE-1}};
                    for (int[] corner : corners) {
                        CellState cornerState = isSimulation ? 
                            convertCodeToState(simulationGrid[corner[0]][corner[1]]) : 
                            gameLogic.getGrid()[corner[0]][corner[1]].getState();
                            
                        if (cornerState == CellState.EMPTY &&
                            Math.abs(corner[0] - opponentRow) + Math.abs(corner[1] - opponentCol) >= 3) {
                            strategicMoves.add(new Move(corner[0], corner[1]));
                        }
                    }
                    
                    // Nếu không có góc phù hợp, tìm các vị trí cách xa đối thủ
                    if (strategicMoves.isEmpty()) {
                        for (int row = 0; row < GRID_SIZE; row++) {
                            for (int col = 0; col < GRID_SIZE; col++) {
                                CellState cellState = isSimulation ? 
                                    convertCodeToState(simulationGrid[row][col]) : 
                                    gameLogic.getGrid()[row][col].getState();
                                    
                                if (cellState == CellState.EMPTY) {
                                    int distance = Math.abs(row - opponentRow) + Math.abs(col - opponentCol);
                                    if (distance >= 3) { // Vị trí cách xa đối thủ
                                        strategicMoves.add(new Move(row, col));
                                    }
                                }
                            }
                        }
                    }
                    
                    if (!strategicMoves.isEmpty()) {
                        return strategicMoves;
                    }
                }
            }
            
            // Nếu không có thông tin về đối thủ hoặc vẫn chưa tìm được vị trí chiến lược
            // Ưu tiên vị trí trung tâm và các góc
            CellState centerState = isSimulation ? 
                convertCodeToState(simulationGrid[center][center]) : 
                gameLogic.getGrid()[center][center].getState();
                
            if (centerState == CellState.EMPTY) {
                strategicMoves.add(new Move(center, center));
            }
            
            int[][] corners = {{0, 0}, {0, GRID_SIZE-1}, {GRID_SIZE-1, 0}, {GRID_SIZE-1, GRID_SIZE-1}};
            for (int[] corner : corners) {
                CellState cornerState = isSimulation ? 
                    convertCodeToState(simulationGrid[corner[0]][corner[1]]) : 
                    gameLogic.getGrid()[corner[0]][corner[1]].getState();
                    
                if (cornerState == CellState.EMPTY) {
                    strategicMoves.add(new Move(corner[0], corner[1]));
                }
            }
            
            if (!strategicMoves.isEmpty()) {
                return strategicMoves;
            }
            
            // Nếu không có vị trí chiến lược, thêm tất cả các ô trống
            for (int row = 0; row < GRID_SIZE; row++) {
                for (int col = 0; col < GRID_SIZE; col++) {
                    CellState cellState = isSimulation ? 
                        convertCodeToState(simulationGrid[row][col]) : 
                        gameLogic.getGrid()[row][col].getState();
                        
                    if (cellState == CellState.EMPTY) {
                        moves.add(new Move(row, col));
                    }
                }
            }
            
            return moves;
        }
        
        // 3. Xử lý các nước đi tiếp theo - cho cả quân 1 điểm và 2 điểm
        List<Move> highDotMoves = new ArrayList<>();   // Ưu tiên quân 2 điểm
        List<Move> lowDotMoves = new ArrayList<>();    // Sau đó đến quân 1 điểm
        
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                CellState state;
                
                if (isSimulation) {
                    state = convertCodeToState(simulationGrid[row][col]);
                } else {
                    state = gameLogic.getGrid()[row][col].getState();
                }
                
                if ((isCurrentRedTurn && state.isRed()) || (!isCurrentRedTurn && state.isBlue())) {
                    int dots = getDotCount(state);
                    if (dots == 2) {
                        highDotMoves.add(new Move(row, col));
                    } else if (dots == 1) {
                        lowDotMoves.add(new Move(row, col));
                    }
                    // Quân 3 điểm đã được xử lý ở trên
                }
            }
        }
        
        // Thêm theo thứ tự ưu tiên
        moves.addAll(highDotMoves);
        moves.addAll(lowDotMoves);
        
        return moves.isEmpty() ? Collections.emptyList() : moves;
    }
}