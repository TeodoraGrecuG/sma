package base;

public class Tile extends CellContent{
    String color;
    int numberOfElements;

    public Tile(String color) {
        this.color = color;
    }
    public Tile(int number) {
        this.numberOfElements = number;
    }

    public Tile(String color, int numberOfElements) {
        this.color = color;
        this.numberOfElements = numberOfElements;
    }

    public int getNumberOfElements() {
        return numberOfElements;
    }

    public void setNumberOfElements(int numberOfElements) {
        this.numberOfElements = numberOfElements;
    }

    @Override
    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Tile() {
        super();
    }
}
