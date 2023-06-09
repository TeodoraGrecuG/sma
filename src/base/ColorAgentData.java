package base;

import java.io.Serializable;

public class ColorAgentData implements Serializable {
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
        tile.setColor("");
        tile.setNumberOfElements(0);
    }

    public ColorAgentData(String color) {
        this.color = color;
        score=0;
        tile = new Tile();
        tile.setColor("");
        tile.setNumberOfElements(0);
    }

    public Tile getTile() {
        return tile;
    }

    public void setTile(Tile tile) {
        this.tile = tile;
    }

    public void setTile(String color, int numberOfElements){
        this.tile.setColor(color);
        this.tile.setNumberOfElements(numberOfElements);
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
}
