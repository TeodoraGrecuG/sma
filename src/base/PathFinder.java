package base;

import base.Cell;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PathFinder {
    int height;
    int width;
    CoordinatesHelper start;
    CoordinatesHelper dest;

    int shortestPath=-1;

    List<String> bestSolution= new ArrayList<>();
    public PathFinder(int height, int width, CoordinatesHelper start, CoordinatesHelper dest) {
        this.height = height;
        this.width = width;
        this.start = start;
        this.dest = dest;
    }

    void printSolution(int sol[][]) {
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++)
                System.out.print(" " + sol[i][j] + " ");
            System.out.println();
        }
    }

    boolean isSafe(Cell cells[][], int checked[][], int x, int y) {
        return (x >= 0 && x < width && y >= 0 && y < height && cells[y][x] != null&&checked[y][x]==0);
    }

    boolean solveMaze(Cell cells[][],List<String> solution) {

        int[][] checked = new int[height][width];
        for(int i=0;i<height;i++)
            Arrays.fill(checked[i], 0);
        List<String> tempSol= new ArrayList<>();
        if (!solveMazeUtil(cells, start.getX(), start.getY(), dest.getX(), dest.getY(), checked, tempSol,0)) {
            System.out.println("Solution doesn't exist");
            return false;
        }
        solution.addAll(bestSolution);
        return true;
    }

    boolean solveMazeUtil(Cell cells[][], int x, int y, int wantedX,int wantedY,int checked[][],List<String> solution,int nrSteps) {
        if (x == wantedX && y == wantedY) {
            if(shortestPath==-1||(shortestPath!=-1&&nrSteps<shortestPath)) {
                bestSolution.clear();
                bestSolution.addAll(solution);
                shortestPath=nrSteps;
            }
            return true;
        }

        boolean n=false,s=false,e=false,w=false;
        if(isSafe(cells,checked,x+1,y)||(x+1==wantedX&&y==wantedY)){
            solution.add("East");
            checked[y][x+1]=1;
            e=solveMazeUtil(cells,x+1,y,wantedX,wantedY,checked,solution,nrSteps+1);
            checked[y][x+1]=0;
            solution.remove(solution.size()-1);
        }
        if(isSafe(cells,checked,x,y+1)||(x==wantedX&&y+1==wantedY)){
            solution.add("South");
            checked[y+1][x]=1;
            s=solveMazeUtil(cells,x,y+1,wantedX,wantedY,checked,solution,nrSteps+1);
            checked[y+1][x]=0;
            solution.remove(solution.size()-1);
        }
        if(isSafe(cells,checked,x-1,y)||(x-1==wantedX&&y==wantedY)){
            solution.add("West");
            checked[y][x-1]=1;
            w=solveMazeUtil(cells,x-1,y,wantedX,wantedY,checked,solution,nrSteps+1);
            checked[y][x-1]=0;
            solution.remove(solution.size()-1);
        }
        if(isSafe(cells,checked,x,y-1)||(x==wantedX&&y-1==wantedY)){
            solution.add("North");
            checked[y-1][x]=1;
            n=solveMazeUtil(cells,x,y-1,wantedX,wantedY,checked,solution,nrSteps+1);
            checked[y-1][x]=0;
            solution.remove(solution.size()-1);
        }

        return s||n||e||w;
    }
}
