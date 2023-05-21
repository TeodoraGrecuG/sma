package base;

public class CoordinatesHelper {
    int x;
    int y;

    public CoordinatesHelper() {
    }

    public CoordinatesHelper(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    @Override
    public String toString() {
        return "CoordinatesHelper{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }
}
