package base;

public class Tile extends CellContent{
    String color;
    /**
     * numberOfElements == 0 means that the tile is empty <br>
     * numberOfElements > 0 means that the tile is full
     */
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
