package base;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Environment implements Serializable
{
	int width;
	int height;
	int timeToPerformAction;
	int totalTimeOfWorking;

	HashMap<String,ColorAgentData> agentsData;
	Cell[][] cells;

	public HashMap<String, ColorAgentData> getColorAgentsData() {
		return agentsData;
	}

	public void setColorAgentsData(HashMap<String, ColorAgentData> agentsData) {
		this.agentsData = agentsData;
	}

	public Environment()
	{
		agentsData=new HashMap<>();
	}

	public Environment(int width, int height, int timeToPerformAction, int totalTimeOfWorking){
		this.width = width;
		this.height = height;
		this.timeToPerformAction = timeToPerformAction;
		this.totalTimeOfWorking = totalTimeOfWorking;
		agentsData=new HashMap<>();
		cells = new Cell[height][width];

		for(int i=0;i<height;i++){
			for(int j=0;j<width;j++)
			{
				cells[i][j] = new Cell(i,j);
			}
		}
	}

	public void allocateCells()
	{
		cells = new Cell[height][width];
		for(int i=0;i<height;i++){
			for(int j=0;j<width;j++)
			{
				cells[i][j] = new Cell(i,j);
			}
		}
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public int getTimeToPerformAction() {
		return timeToPerformAction;
	}

	public void setTimeToPerformAction(int timeToPerformAction) {
		this.timeToPerformAction = timeToPerformAction;
	}

	public int getTotalTimeOfWorking() {
		return totalTimeOfWorking;
	}

	public void setTotalTimeOfWorking(int totalTimeOfWorking) {
		this.totalTimeOfWorking = totalTimeOfWorking;
	}

	public Cell getCell(int x, int y){
		return cells[y][x];
	}

	public void deleteCell(int x, int y){
		cells[y][x] = null;
	}

	public void addAgentData(String color,ColorAgentData colorAgentData){
		agentsData.put(color,colorAgentData);
	}

	protected<T> List<CoordinatesHelper> getContentCoordinates(Class<?> className, String color) throws ClassNotFoundException {
		List<CoordinatesHelper> contentCoordinates = new ArrayList<>();
		for(int i=0;i<height;i++){
			for(int j=0;j<width;j++) {
				List<CellContent> cellContents = getCell(j,i).getCellContents();

				for(CellContent cc: cellContents){
					if(className.isInstance(cc)) {
						if(Objects.equals(cc.getColor(), color)){
							if(className == Tile.class) {
								for (int rr = 0; rr < ((Tile) cc).getNumberOfElements(); rr++)
									contentCoordinates.add(new CoordinatesHelper(j,i));
							}
							else if(className == Hole.class) {
								for (int rr = 0; rr < ((Hole) cc).getDepth(); rr++)
									contentCoordinates.add(new CoordinatesHelper(j,i));
							}
							else
								contentCoordinates.add(new CoordinatesHelper(j,i));
						}
						if(Objects.equals("all", color))
							contentCoordinates.add(new CoordinatesHelper(j,i));
					}
				}
			}
		}
		return contentCoordinates;
	}
	List<CoordinatesHelper> getObstacleCoordinates() throws ClassNotFoundException {
		;
		return getContentCoordinates(Obstacle.class, null);
	}

	List<CoordinatesHelper> getTilesCoordinatesByColor(String color) throws ClassNotFoundException {
		return getContentCoordinates(Tile.class, color);
	}

	List<CoordinatesHelper> getAllHoles() throws ClassNotFoundException {
		return getContentCoordinates(Hole.class, "all");
	}
	List<CoordinatesHelper> getHolesCoordinatesByColor(String color) throws ClassNotFoundException {

		return getContentCoordinates(Hole.class, color);
	}

	public void addAgentData(int x, int y, String color){
		agentsData.put(color,new ColorAgentData(x,y,color));
	}

	public ColorAgentData getColorAgentData(String color){
		return agentsData.get(color);
	}

	void setVisualSize()
	{
		AtomicInteger max = new AtomicInteger();
		for(int i=0;i<height;i++){
			for(int j=0;j<width;j++)
			{
				if(max.get() <cells[i][j].computeVisualCellDim()) {
					max.set(cells[i][j].computeVisualCellDim());
					int finalJ = j;
					int finalI = i;
					agentsData.forEach(
							(key, value)
									-> {if(value.getX()== finalJ &&value.getY()== finalI){
										max.set(max.get() + 1);
									}
							});
				}

			}

		}

		for(int i=0;i<height;i++){
			for(int j=0;j<width;j++)
			{
				cells[i][j].setVisualSize(max.get());
			}
		}
	}

	public void clean()
	{
		for(int i=0;i<height;i++) {
			for (int j = 0; j < width; j++) {
				List<CellContent> local = cells[i][j].getCellContents();
				if (local.size() > 1) {
					for (int k = 0; k < local.size() - 1; k++) {
						for (int l = k + 1; l < local.size(); l++) {
							if ((local.get(k) instanceof Hole && local.get(l) instanceof Tile) || (local.get(k) instanceof Tile && local.get(l) instanceof Hole)) {
								if(local.get(k) instanceof Hole&&local.get(l) instanceof Tile)
								{
									int tileNumberOfElem = ((Tile)local.get(l)).getNumberOfElements();
									int holeDepth = ((Hole)local.get(k)).getDepth();
									if(Objects.equals(local.get(k).getColor(), local.get(l).getColor())){
										agentsData.get(local.get(l).getColor()).setScore(agentsData.get(local.get(l).getColor()).getScore()+10);
									}
									if(tileNumberOfElem>=holeDepth){
										tileNumberOfElem = tileNumberOfElem - holeDepth;
										((Hole)cells[i][j].getCellContents().get(k)).setDepth(0);
										((Tile)cells[i][j].getCellContents().get(l)).setNumberOfElements(tileNumberOfElem);
										if(Objects.equals(local.get(k).getColor(), local.get(l).getColor())) {
											agentsData.get(local.get(l).getColor()).setScore(agentsData.get(local.get(l).getColor()).getScore() + 40);
										}
									}
									else{
										holeDepth = holeDepth-tileNumberOfElem;
										((Hole)cells[i][j].getCellContents().get(k)).setDepth(holeDepth);
										((Tile)cells[i][j].getCellContents().get(l)).setNumberOfElements(0);
									}

								}
								else if(local.get(k) instanceof Tile &&local.get(l) instanceof Hole)
								{
									int tileNumberOfElem = ((Tile)local.get(k)).getNumberOfElements();
									int holeDepth = ((Hole)local.get(l)).getDepth();
									if(Objects.equals(local.get(k).getColor(), local.get(l).getColor())){
										agentsData.get(local.get(l).getColor()).setScore(agentsData.get(local.get(l).getColor()).getScore()+10);
									}
									if(tileNumberOfElem>=holeDepth){
										tileNumberOfElem = tileNumberOfElem - holeDepth;
										((Hole)cells[i][j].getCellContents().get(l)).setDepth(0);
										((Tile)cells[i][j].getCellContents().get(k)).setNumberOfElements(tileNumberOfElem);
										if(Objects.equals(local.get(k).getColor(), local.get(l).getColor())) {
											agentsData.get(local.get(l).getColor()).setScore(agentsData.get(local.get(l).getColor()).getScore() + 40);
										}
									}
									else{
										holeDepth = holeDepth-tileNumberOfElem;
										((Hole)cells[i][j].getCellContents().get(l)).setDepth(holeDepth);
										((Tile)cells[i][j].getCellContents().get(k)).setNumberOfElements(0);
									}

								}
							}
							else if (local.get(k) instanceof Tile && local.get(l) instanceof Tile && Objects.equals(local.get(k).getColor(), local.get(l).getColor())){
								int numOfElem = ((Tile) local.get(k)).getNumberOfElements() + ((Tile) local.get(l)).getNumberOfElements();
								((Tile)cells[i][j].getCellContents().get(l)).setNumberOfElements(0);
								((Tile)cells[i][j].getCellContents().get(k)).setNumberOfElements(numOfElem);
							}
						}
					}
				}
			}
		}

		for(int i=0;i<height;i++) {
			for (int j = 0; j < width; j++) {
				List<CellContent> local = List.copyOf(cells[i][j].getCellContents());
				for (CellContent cc:local)
				{
					if(!(cc instanceof Obstacle)) {
						if (cc instanceof Tile){
							if (((Tile) cc).getNumberOfElements() <= 0) {
								cells[i][j].getCellContents().remove(cc);
							}
						}
						if (cc instanceof Hole) {
							if (((Hole) cc).getDepth() <= 0) {
								cells[i][j].getCellContents().remove(cc);
							}
						}
					}
				}
			}
		}
	}

	public List<String> getColorOfAgentsByCoordinates(int x, int y){
		List<String>colors=new ArrayList<>();
		for (Map.Entry<String, ColorAgentData> entry : agentsData.entrySet()) {
			String key = entry.getKey();
			ColorAgentData value = entry.getValue();
			if (value.getX() == x && value.getY() == y)
				colors.add(value.getColor());
		}
		return colors;
	}
	public void print()
	{
		setVisualSize();
		int heightOfPrint=cells[0][0].toPrint().size();
		for(int i=0;i<height;i++){
			List<List<String>> row = new ArrayList<>();
			for(int j=0;j<width;j++)
			{
				List<String> localToPrint=cells[i][j].toPrint();
				List<String> colors=getColorOfAgentsByCoordinates(j,i);
				int k=0;
				int l=0;
				while(k<localToPrint.size()&&l<colors.size())
				{
					if(!colors.isEmpty()&& Objects.equals(localToPrint.get(k), "|    ")) {
						localToPrint.set(k, "|" + colors.get(l).substring(0, 3) + " ");
						l++;
					}
					k++;
				}
				row.add(localToPrint);
			}
			for(int k=0;k<heightOfPrint;k++) {
				for (int j = 0; j < width; j++) {
					System.out.print(row.get(j).get(k));
				}
				System.out.print("|");
				System.out.println("");
			}
		}
		for(int i=0;i<width;i++)
			System.out.print("|----");
		System.out.println("|");
		System.out.println("==========================================");

		agentsData.forEach(
				(key, value)
						-> {
					if(value.getTile().getNumberOfElements()==0)
						System.out.println(value.getColor() + " agent: " + value.getScore()+ " points; carries nothing");
					else
						System.out.println(value.getColor() + " agent: " + value.getScore()+ " points; carries " + value.getTile().getColor());
				});

	}
}
