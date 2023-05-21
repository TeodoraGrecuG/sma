package base;

public class Hole extends CellContent{
    String color;

    public int getDepth() {
        return depth;
    }

    public Hole(String color, int depth) {
        this.color = color;
        this.depth = depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    int depth;

    @Override
    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Hole() {
        super();
    }

    @Override
    public String toString() {
        return "Hole{" +
                "color='" + color + '\'' +
                ", depth=" + depth +
                '}';
    }
}
