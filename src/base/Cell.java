package base;

import java.util.ArrayList;
import java.util.List;

public class Cell {
    int visualSize;
    List<CellContent> cellContents;

    int x;
    int y;

    public List<CellContent> getCellContents() {
        return cellContents;
    }

    public void setCellContents(List<CellContent> cellContents) {
        this.cellContents = cellContents;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getVisualSize() {
        return visualSize;
    }

    public void setVisualSize(int visualSize) {
        this.visualSize = visualSize;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public Cell(){
        cellContents=new ArrayList<>();
    }
    public Cell(int x, int y) {
        this.x = x;
        this.y = y;
        cellContents=new ArrayList<>();
    }

    public void addContent(CellContent cellContent){
        this.cellContents.add(cellContent);
    }

    public void removeContent(CellContent cellContent) {
        this.cellContents.remove(cellContent);
    }


    public int computeVisualCellDim()
    {
        if(cellContents.isEmpty()|| cellContents.size()==1){
            return 2;
        }
        else{
           return cellContents.size()+1;
        }
    }

    public List<String> toPrint()
    {
        int j=0;
        List<String> toPrint = new ArrayList<>();
        toPrint.add("|----");
        for(CellContent cc: cellContents)
        {
            if(cc instanceof Hole){
                toPrint.add("|@"+String.format("%2d",((Hole)cc).getDepth()) + ((Hole)cc).getColor().charAt(0));
                j++;
            }
            if(cc instanceof Tile){
                toPrint.add("|E"+String.format("%2d",((Tile) cc).getNumberOfElements())+((Tile) cc).getColor().charAt(0));
                j++;
            }
            if(cc instanceof Obstacle){
                for(int i=0;i<visualSize;i++){
                    toPrint.add("|XXXX");
                    j++;
                }
            }
        }

        while(j<visualSize)
        {
            toPrint.add("|    ");
            j++;
        }

        return toPrint;
    }

}
