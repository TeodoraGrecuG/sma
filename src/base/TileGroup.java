package base;

public class TileGroup extends CellContent{
    String color;
    int numberOfElements;

    public TileGroup(String color) {
        this.color = color;
    }
    public TileGroup(int number) {
        this.numberOfElements = number;
    }

    public TileGroup(String color, int numberOfElements) {
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

    public TileGroup() {
        super();
    }
}
