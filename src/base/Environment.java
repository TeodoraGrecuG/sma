package base;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Environment
{
	int width;
	int height;
	int timeToPerformAction;
	int totalTimeOfWorking;

	HashMap<String,ColorAgentData> agentsData;
	Cell[][] cells;



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


	public Cell getCell(int x, int y){
		return cells[y][x];
	}

	public void addAgentData(String color,ColorAgentData colorAgentData){
		agentsData.put(color,colorAgentData);
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
							if ((local.get(k) instanceof Hole && local.get(l) instanceof TileGroup) || (local.get(k) instanceof TileGroup && local.get(l) instanceof Hole)) {
								if(local.get(k) instanceof Hole&&local.get(l) instanceof TileGroup)
								{
									int tileNumberOfElem = ((TileGroup)local.get(l)).getNumberOfElements();
									int holeDepth = ((Hole)local.get(k)).getDepth();
									if(Objects.equals(local.get(k).getColor(), local.get(l).getColor())){
										agentsData.get(local.get(l).getColor()).setScore(agentsData.get(local.get(l).getColor()).getScore()+10);
									}
									if(tileNumberOfElem>=holeDepth){
										tileNumberOfElem = tileNumberOfElem - holeDepth;
										((Hole)cells[i][j].getCellContents().get(k)).setDepth(0);
										((TileGroup)cells[i][j].getCellContents().get(l)).setNumberOfElements(tileNumberOfElem);
										if(Objects.equals(local.get(k).getColor(), local.get(l).getColor())) {
											agentsData.get(local.get(l).getColor()).setScore(agentsData.get(local.get(l).getColor()).getScore() + 40);
										}
									}
									else{
										holeDepth = holeDepth-tileNumberOfElem;
										((Hole)cells[i][j].getCellContents().get(k)).setDepth(holeDepth);
										((TileGroup)cells[i][j].getCellContents().get(l)).setNumberOfElements(0);
									}

								}
								else if(local.get(k) instanceof TileGroup&&local.get(l) instanceof Hole)
								{
									int tileNumberOfElem = ((TileGroup)local.get(k)).getNumberOfElements();
									int holeDepth = ((Hole)local.get(l)).getDepth();
									if(Objects.equals(local.get(k).getColor(), local.get(l).getColor())){
										agentsData.get(local.get(l).getColor()).setScore(agentsData.get(local.get(l).getColor()).getScore()+10);
									}
									if(tileNumberOfElem>=holeDepth){
										tileNumberOfElem = tileNumberOfElem - holeDepth;
										((Hole)cells[i][j].getCellContents().get(l)).setDepth(0);
										((TileGroup)cells[i][j].getCellContents().get(k)).setNumberOfElements(tileNumberOfElem);
										if(Objects.equals(local.get(k).getColor(), local.get(l).getColor())) {
											agentsData.get(local.get(l).getColor()).setScore(agentsData.get(local.get(l).getColor()).getScore() + 40);
										}
									}
									else{
										holeDepth = holeDepth-tileNumberOfElem;
										((Hole)cells[i][j].getCellContents().get(l)).setDepth(holeDepth);
										((TileGroup)cells[i][j].getCellContents().get(k)).setNumberOfElements(0);
									}

								}
							}
							else if (local.get(k) instanceof TileGroup && local.get(l) instanceof TileGroup && Objects.equals(local.get(k).getColor(), local.get(l).getColor())){
								int numOfElem = ((TileGroup) local.get(k)).getNumberOfElements() + ((TileGroup) local.get(l)).getNumberOfElements();
								((TileGroup)cells[i][j].getCellContents().get(l)).setNumberOfElements(0);
								((TileGroup)cells[i][j].getCellContents().get(k)).setNumberOfElements(numOfElem);
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
						if (cc instanceof TileGroup){
							if (((TileGroup) cc).getNumberOfElements() <= 0) {
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
						-> System.out.println(value.getColor() + " agent: " + value.getScore()+ " points"));

	}
}
