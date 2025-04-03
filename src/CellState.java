public enum CellState {
    EMPTY,
    RED_ONE,
    RED_TWO,
    RED_THREE,
    RED_FOUR,
    BLUE_ONE,
    BLUE_TWO,
    BLUE_THREE,
    BLUE_FOUR;

    public boolean isRed() {
        return this == RED_ONE || this == RED_TWO || this == RED_THREE || this == RED_FOUR;
    }

    public boolean isBlue() {
        return this == BLUE_ONE || this == BLUE_TWO || this == BLUE_THREE || this == BLUE_FOUR;
    }

    public CellState getNextState() {
        switch (this) {
            case RED_ONE:
                return RED_TWO;
            case RED_TWO:
                return RED_THREE;
            case RED_THREE:
                return RED_FOUR;
            case BLUE_ONE:
                return BLUE_TWO;
            case BLUE_TWO:
                return BLUE_THREE;
            case BLUE_THREE:
                return BLUE_FOUR;
            default:
                return this;
        }
    }
}

