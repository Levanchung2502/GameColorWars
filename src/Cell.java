import javax.swing.*;
import java.awt.*;

public class Cell extends JPanel {
    private int row, col;
    private CellState state = CellState.EMPTY;
    private final Color emptyColor;
    private final Color redTeamColor;
    private final Color blueTeamColor;
    private static final int DOT_RADIUS = 6;

    public Cell(int row, int col, Color emptyColor, Color redTeamColor, Color blueTeamColor) {
        this.row = row;
        this.col = col;
        this.emptyColor = emptyColor;
        this.redTeamColor = redTeamColor;
        this.blueTeamColor = blueTeamColor;
        setOpaque(false);
    }

    public CellState getState() {
        return state;
    }

    public void setState(CellState state) {
        this.state = state;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(emptyColor);
        g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);

        if (state != CellState.EMPTY) {
            if (state.isRed()) {
                g2d.setColor(redTeamColor);
            } else {
                g2d.setColor(blueTeamColor);
            }

            g2d.fillOval(5, 5, getWidth() - 10, getHeight() - 10);
            g2d.setColor(Color.WHITE);

            switch (state) {
                case BLUE_ONE:
                case RED_ONE:
                    g2d.fillOval(getWidth()/2 - DOT_RADIUS, getHeight()/2 - DOT_RADIUS,
                            DOT_RADIUS*2, DOT_RADIUS*2);
                    break;
                case RED_TWO:
                case BLUE_TWO:
                    g2d.fillOval(getWidth()/3 - DOT_RADIUS, getHeight()/2 - DOT_RADIUS,
                            DOT_RADIUS*2, DOT_RADIUS*2);
                    g2d.fillOval(2*getWidth()/3 - DOT_RADIUS, getHeight()/2 - DOT_RADIUS,
                            DOT_RADIUS*2, DOT_RADIUS*2);
                    break;
                case RED_THREE:
                case BLUE_THREE:
                    g2d.fillOval(getWidth()/2 - DOT_RADIUS, getHeight()/3 - DOT_RADIUS,
                            DOT_RADIUS*2, DOT_RADIUS*2);
                    g2d.fillOval(getWidth()/3 - DOT_RADIUS, 2*getHeight()/3 - DOT_RADIUS,
                            DOT_RADIUS*2, DOT_RADIUS*2);
                    g2d.fillOval(2*getWidth()/3 - DOT_RADIUS, 2*getHeight()/3 - DOT_RADIUS,
                            DOT_RADIUS*2, DOT_RADIUS*2);
                    break;
                case RED_FOUR:
                case BLUE_FOUR:
                    g2d.fillOval(getWidth()/3 - DOT_RADIUS, getHeight()/3 - DOT_RADIUS,
                            DOT_RADIUS*2, DOT_RADIUS*2);
                    g2d.fillOval(2*getWidth()/3 - DOT_RADIUS, getHeight()/3 - DOT_RADIUS,
                            DOT_RADIUS*2, DOT_RADIUS*2);
                    g2d.fillOval(getWidth()/3 - DOT_RADIUS, 2*getHeight()/3 - DOT_RADIUS,
                            DOT_RADIUS*2, DOT_RADIUS*2);
                    g2d.fillOval(2*getWidth()/3 - DOT_RADIUS, 2*getHeight()/3 - DOT_RADIUS,
                            DOT_RADIUS*2, DOT_RADIUS*2);
                    break;
            }
        }
    }
}