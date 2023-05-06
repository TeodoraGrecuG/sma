package base;

import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

public class EnvironmentAgent extends Agent {
    private static final long serialVersionUID = 5088484951993491458L;
    Environment environment;

    void setEnvironment(String [] args)
    {
        String filename = "tests/system__default.txt";

        String tempColor;
        int tempX=0;
        int tempY=0;
        List<String> colors=new ArrayList<>();

        if(args.length<1){
            System.out.println("You haven't passed any argument to program! I'll use the default");
        }
        else
            filename = (String) args[0];

        File file = new File(filename);
        Scanner input = null;
        try {
            input = new Scanner(file);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        int numberOfAgents = Integer.valueOf(input.next());
        int timeToPerformAction = Integer.valueOf(input.next());
        int totalTimeOfAction = Integer.valueOf(input.next());
        int width = Integer.valueOf(input.next());
        int height = Integer.valueOf(input.next());

        //Environment environment = new Environment(width, height, timeToPerformAction, totalTimeOfAction);
        environment.setHeight(height);
        environment.setWidth(width);
        environment.setTimeToPerformAction(timeToPerformAction);
        environment.setTotalTimeOfWorking(totalTimeOfAction);
        environment.allocateCells();

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
    }

    @Override
    public void setup() {
        Object[] args = getArguments();
        environment = new Environment();
        setEnvironment((String[]) args);

        // Register the ambient-agent service in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());

        ServiceDescription sd = new ServiceDescription();
        sd.setType(ServiceType.ENV_AGENT);
        sd.setName("environment-agent");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        environment.print();
    }

    @Override
    protected void takeDown() {
        // Unregister from the yellow pages
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }
}
