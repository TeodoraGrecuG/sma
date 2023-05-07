package base;

public class ColorAgentData {
    int x;
    int y;
    String color;
    int score;
    Tile tile;

    public ColorAgentData(){
        ;
    }
    public ColorAgentData(int x, int y, String color) {
        this.x = x;
        this.y = y;
        this.color = color;
        score=0;

        tile = new Tile();
        tile.color = "";
        tile.numberOfElements = 0;
    }

    public ColorAgentData(String color) {
        this.color = color;
        score=0;

        tile = new Tile();
        tile.color = "";
        tile.numberOfElements = 0;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Tile getTile() {
        return tile;
    }

    public void setTile(Tile tile) {
        this.tile = tile;
    }
}
