import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.*;

public class Cell extends JPanel {
    private int row, col;
    private CellState state = CellState.EMPTY;
    private final Color emptyColor;
    private final Color redTeamColor;
    private final Color blueTeamColor;
    private static final int DOT_RADIUS = 6;
    private boolean highlighted = false;

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
    
    public void setHighlighted(boolean highlighted) {
        this.highlighted = highlighted;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw regular cell (square background)
        g2d.setColor(emptyColor);
        g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);

        if (state != CellState.EMPTY) {
            int padding = 5;

            Shape outerSquare = new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 15, 15);
            Shape innerCircle = new Ellipse2D.Float(padding, padding, getWidth() - padding*2, getHeight() - padding*2);
            Area areaSquare = new Area(outerSquare);
            Area areaCircle = new Area(innerCircle);
            areaSquare.subtract(areaCircle);

            Color areaColor;
            if (highlighted) {
                if (state.isRed()) {
                    areaColor = new Color(redTeamColor.getRed(), redTeamColor.getGreen(), redTeamColor.getBlue(), 70);
                } else {
                    areaColor = new Color(blueTeamColor.getRed(), blueTeamColor.getGreen(), blueTeamColor.getBlue(), 70);
                }
            } else{
                areaColor = emptyColor;
            }
            
            // Fill the area between circle and square
            g2d.setColor(areaColor);
            g2d.fill(areaSquare);
            
            // Draw the colored circle
            if (state.isRed()) {
                g2d.setColor(redTeamColor);
            } else {
                g2d.setColor(blueTeamColor);
            }
            g2d.fillOval(padding, padding, getWidth() - padding*2, getHeight() - padding*2);
            
            // Add a subtle glow if highlighted
            if (highlighted) {
                Color glowColor;
                if (state.isRed()) {
                    glowColor = new Color(redTeamColor.getRed(), redTeamColor.getGreen(), redTeamColor.getBlue(), 40);
                } else {
                    glowColor = new Color(blueTeamColor.getRed(), blueTeamColor.getGreen(), blueTeamColor.getBlue(), 40);
                }
                
                g2d.setColor(glowColor);
                for (int i = 2; i >= 0; i--) {
                    float alpha = 0.15f - (i * 0.04f);
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                    g2d.fillOval(
                        padding - i, 
                        padding - i, 
                        getWidth() - padding*2 + i*2, 
                        getHeight() - padding*2 + i*2
                    );
                }
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            }
            
            // Draw the white dots
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

    public Cell clone(){
        Cell cloneCell = new Cell(row, col, emptyColor, redTeamColor, blueTeamColor);
        cloneCell.setState(state);
        return cloneCell;
    }
}