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

        if (!solveMazeUtil(cells, start.getX(), start.getY(), dest.getX(), dest.getY(), checked, solution)) {
            System.out.println("Solution doesn't exist");
            return false;
        }
        Collections.reverse(solution);
        return true;
    }

    boolean solveMazeUtil(Cell cells[][], int x, int y, int wantedX,int wantedY,int checked[][],List<String> solution) {
        if (x == wantedX && y == wantedY) {
            return true;
        }

        if (isSafe(cells,checked, x, y) == true) {
            checked[y][x]=1;
            if (solveMazeUtil(cells, x + 1, y, wantedX,wantedY,checked,solution)) {
                solution.add("East");
                return true;
            }

            if (solveMazeUtil(cells, x, y + 1,wantedX,wantedY,checked,solution)){
                solution.add("South");
                return true;
            }

            if (solveMazeUtil(cells, x - 1, y ,wantedX,wantedY,checked,solution)){
                solution.add("West");
                return true;
            }

            if (solveMazeUtil(cells, x, y - 1,wantedX,wantedY,checked,solution)){
                solution.add("North");
                return true;
            }

            return false;
        }

        return false;
    }
}
