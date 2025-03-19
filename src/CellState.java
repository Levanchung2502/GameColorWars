public enum CellState {
    EMPTY,
    RED_ONE,
    RED_TWO,
    RED_THREE,
    BLUE_ONE,
    BLUE_TWO,
    BLUE_THREE,
    RED_FOUR,
    BLUE_FOUR;
    public boolean isRed() {
        return this == RED_ONE || this == RED_TWO || this == RED_THREE || this == RED_FOUR;
    }

    // Kiểm tra xem ô hiện tại có phải là của người chơi XANH không
    public boolean isBlue() {
        return this == BLUE_ONE || this == BLUE_TWO || this == BLUE_THREE || this == BLUE_FOUR;
    }

    // Lấy trạng thái kế tiếp sau khi click
    public CellState getNextState() {
        switch (this) {
            case RED_ONE: return RED_TWO;
            case RED_TWO: return RED_THREE;
            case RED_THREE: return RED_FOUR;
            case BLUE_ONE: return BLUE_TWO;
            case BLUE_TWO: return BLUE_THREE;
            case BLUE_THREE: return BLUE_FOUR;
            default: return this; // Nếu đã là RED_FOUR hoặc BLUE_FOUR thì không thay đổi
        }
    }
}

