import base.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.io.*;

public class Main {
    public static void main(String[] args) throws FileNotFoundException {
        String filename = "tests/system__default.txt";

        String tempColor;
        int tempX=0;
        int tempY=0;
        List<String> colors=new ArrayList<>();

        if(args.length<1){
            System.out.println("You haven't passed any argument to program! I'll use the default");
        }
        else
            filename = args[0];

        File file = new File(filename);
        Scanner input = new Scanner(file);

        int numberOfAgents = Integer.valueOf(input.next());
        int timeToPerformAction = Integer.valueOf(input.next());
        int totalTimeOfAction = Integer.valueOf(input.next());
        int width = Integer.valueOf(input.next());
        int height = Integer.valueOf(input.next());

        Environment environment = new Environment(width, height, timeToPerformAction, totalTimeOfAction);

        for(int i=0;i<numberOfAgents;i++) {
            tempColor = input.next();
            environment.addAgentData(tempColor,new ColorAgentData(tempColor));
            colors.add(tempColor);
        }

        for(int i=0;i<numberOfAgents;i++){
            tempX = Integer.valueOf(input.next());
            tempY = Integer.valueOf(input.next());

            environment.getColorAgentData(colors.get(i)).setX(tempX);
            environment.getColorAgentData(colors.get(i)).setY(tempY);
        }

        String status = input.next();
        if(Objects.equals(status, "OBSTACLES")){
            while(!status.equals("TILES")){
                status = input.next();
                if(!status.equals("TILES")){
                    tempX = Integer.valueOf(status);
                    tempY = Integer.valueOf(input.next());
                    environment.getCell(tempX, tempY).addContent(new Obstacle());
                }
            }
        }

        if(Objects.equals(status, "TILES"))
        {
            while(!status.equals("HOLES")) {
                status = input.next();
                if (!Objects.equals(status, "HOLES")) {
                    int numberInGroup = Integer.valueOf(status);
                    tempColor = input.next();
                    tempX = Integer.valueOf(input.next());
                    tempY = Integer.valueOf(input.next());
                    environment.getCell(tempX, tempY).addContent(new Tile(tempColor, numberInGroup));
                }
            }
        }

        if(Objects.equals(status, "HOLES"))
        {
            while(input.hasNext()){
                int d = Integer.valueOf(input.next());
                tempColor = input.next();
                tempX = Integer.valueOf(input.next());
                tempY = Integer.valueOf(input.next());
                environment.getCell(tempX, tempY).addContent(new Hole(tempColor, d));
            }
        }

        environment.clean();
        environment.print();
    }
}