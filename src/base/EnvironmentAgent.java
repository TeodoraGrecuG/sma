package base;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.core.behaviours.*;
import jade.lang.acl.*;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREResponder;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREResponder;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.LocalTime;
import java.util.*;

import static java.lang.Thread.sleep;

public class EnvironmentAgent extends Agent {
    private static final long serialVersionUID = 5088484951993491458L;
    Environment environment;
    List<AID> colorAgents;
    static final long TICK_PERIOD=500;

    int setEnvironment(String [] args)
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
        return colors.size();
    }

    @Override
    public void setup() {
        Object[] args = getArguments();
        environment = new Environment();
        int numOfColors = setEnvironment((String[]) args);
        colorAgents = new ArrayList<>();

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
        addBehaviour(new DiscoverEnvironmentAndColleaguesBehaviour(this, ParallelBehaviour.WHEN_ALL, numOfColors));
        //environment.print();
    }

    public void addServiceAgent(String serviceType, AID agent, int numOfColors) throws InterruptedException {
        if(serviceType.equals(ServiceType.COLOR_AGENT))
        {
            colorAgents.add(agent);
        }

        if(colorAgents.size()>=numOfColors)
            onDiscoveryCompleted();
    }

    private void onDiscoveryCompleted() throws InterruptedException {
        SingletoneBuffer.getInstance().addLogToPrint(Log.log(this, "color discovery completed" + colorAgents));

        //sincronizarea starii initiale a scorurilor
        MessageTemplate template = MessageTemplate.and(
                MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),
                MessageTemplate.MatchPerformative(ACLMessage.REQUEST) );

//        addBehaviour(new SimpleBehaviour() {
//            List<ACLMessage> tempMsg = new ArrayList<>();
//            @Override
//            public void action() {
//                ACLMessage msg = myAgent.blockingReceive(template);
//                if (msg != null) {
//                    SingletoneBuffer.getInstance().addLogToPrint(Log.log(myAgent, ": got initial sync request from "+msg.getSender().getName()));
//                    tempMsg.add(msg);
//                }
//                else {
//                    block();
//                }
//            }
//
//            @Override
//            public boolean done() {
//                if(tempMsg.size()==environment.getColorAgentsData().size())
//                {
//                    for(ACLMessage msg: tempMsg){
//
//                        ACLMessage reply = msg.createReply();
//                        reply.setPerformative( ACLMessage.INFORM );
//                        reply.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
//
//                        JSONObject resp = new JSONObject();
//                        ColorAgentData temp = environment.getColorAgentData(msg.getContent());
//                        resp.put("score", temp.getScore());
//                        resp.put("x", (Integer)temp.getX());
//                        resp.put("y", (Integer)temp.getY());
//                        reply.setContent(resp.toJSONString());
//
//                        send(reply);
//                    }
//                    return true;
//                }
//                return false;
//            }
//        });
        environment.print();
        System.out.println("\n\n");
        SingletoneBuffer.getInstance().printLogs();
        System.out.println("\nhere begins=================================================================");

        addBehaviour(new TickerBehaviour(this,environment.getTimeToPerformAction()) {
            @Override
            protected void onTick() {
                if(getTickCount() >= environment.getTotalTimeOfWorking()/(float)environment.getTimeToPerformAction()) {
                    stop();
                }

//                addBehaviour(new AchieveREResponder(this.getAgent(), template) {
//                    protected ACLMessage prepareResponse(ACLMessage request) throws NotUnderstoodException, RefuseException {
//                        System.out.println("Agent " + getLocalName() + ": REQUEST received from " +
//                                request.getSender().getName() + ". Action is " + request.getContent());
//
//                        Object obj = JSONValue.parse(request.getContent());
//                        JSONObject jsonObject = (JSONObject) obj;
//
//                        String action = (String) jsonObject.get("action");
//
//                        ACLMessage agree = request.createReply();
//                        switch (action) {
//                            case "pick": {
//                                String color = (String) jsonObject.get("color");
//                                int x = ((Long) jsonObject.get("Current x")).intValue();
//                                int y = ((Long) jsonObject.get("Current y")).intValue();
//                                if (environment.getCell(x, y).getCellContents() != null) {
//                                    for (CellContent cellContent : environment.getCell(x, y).getCellContents()) {
//                                        if (cellContent instanceof Tile) {
//                                            if (cellContent.getColor().equals(color)) {
//                                                environment.getCell(x, y).removeContent(cellContent);
//                                                agree.setPerformative(ACLMessage.AGREE);
//                                            }
//                                        }
//                                    }
//                                }
//                                break;
//                            }
//                            case "drop_tile": {
//                                int x = ((Long) jsonObject.get("Current x")).intValue();
//                                int y = ((Long) jsonObject.get("Current y")).intValue();
//                                if (environment.getCell(x,y).getCellContents() != null) {
//                                    for (CellContent cellContent : environment.getCell(x, y).getCellContents()) {
//                                        if (cellContent instanceof Tile) {
//                                            if (cellContent.getColor().equals((String) jsonObject.get("color"))) {
//                                                ((Tile) cellContent).numberOfElements += 1;
//                                                agree.setPerformative(ACLMessage.AGREE);
//                                                break;
//                                            }
//                                        }
//                                    }
//                                    environment.getCell(x,y).addContent(new Tile((String) jsonObject.get("color"),1));
//                                    agree.setPerformative(ACLMessage.AGREE);
//                                }
//                                break;
//                            }
//                            case "move": {
//                                String direction = (String) jsonObject.get("direction");
//                                int x = environment.getColorAgentData((String) jsonObject.get("color")).getX();
//                                int y = environment.getColorAgentData((String) jsonObject.get("color")).getY();
//                                switch (direction) {
//                                    case "North": {
//                                        if (y - 1 >= 0) {
//                                            for (CellContent cellContent : environment.getCell(x, y - 1).getCellContents()) {
//                                                if (cellContent instanceof Obstacle) {
//                                                    System.out.println("Agent " + getLocalName() + ": Refuse");
//                                                    throw new RefuseException("check-failed");
//                                                } else if (cellContent instanceof Hole) {
//                                                    if (((Hole) cellContent).depth == 0) {
//                                                        agree.setPerformative(ACLMessage.AGREE);
//                                                        break;
//                                                    } else {
//                                                        System.out.println("Agent " + getLocalName() + ": Refuse");
//                                                        throw new RefuseException("check-failed");
//                                                    }
//                                                }
//                                            }
//                                            agree.setPerformative(ACLMessage.AGREE);
//                                            break;
//                                        } else {
//                                            System.out.println("Agent " + getLocalName() + ": Refuse");
//                                            throw new RefuseException("check-failed");
//                                        }
//                                    }
//                                    case "South": {
//                                        if (y + 1 <= environment.height - 1) {
//                                            for (CellContent cellContent : environment.getCell(x, y + 1).getCellContents()) {
//                                                if (cellContent instanceof Obstacle) {
//                                                    System.out.println("Agent " + getLocalName() + ": Refuse");
//                                                    throw new RefuseException("check-failed");
//                                                } else if (cellContent instanceof Hole) {
//                                                    if (((Hole) cellContent).depth == 0) {
//                                                        agree.setPerformative(ACLMessage.AGREE);
//                                                        break;
//                                                    } else {
//                                                        System.out.println("Agent " + getLocalName() + ": Refuse");
//                                                        throw new RefuseException("check-failed");
//                                                    }
//                                                }
//                                            }
//                                            agree.setPerformative(ACLMessage.AGREE);
//                                            break;
//                                        } else {
//                                            System.out.println("Agent " + getLocalName() + ": Refuse");
//                                            throw new RefuseException("check-failed");
//                                        }
//                                    }
//                                    case "East": {
//                                        if (x + 1 <= environment.width - 1) {
//                                            for (CellContent cellContent : environment.getCell(x + 1, y).getCellContents()) {
//                                                if (cellContent instanceof Obstacle) {
//                                                    System.out.println("Agent " + getLocalName() + ": Refuse");
//                                                    throw new RefuseException("check-failed");
//                                                } else if (cellContent instanceof Hole) {
//                                                    if (((Hole) cellContent).depth == 0) {
//                                                        agree.setPerformative(ACLMessage.AGREE);
//                                                        break;
//                                                    } else {
//                                                        System.out.println("Agent " + getLocalName() + ": Refuse");
//                                                        throw new RefuseException("check-failed");
//                                                    }
//                                                }
//                                            }
//                                            agree.setPerformative(ACLMessage.AGREE);
//                                            break;
//                                        } else {
//                                            System.out.println("Agent " + getLocalName() + ": Refuse");
//                                            throw new RefuseException("check-failed");
//                                        }
//                                    }
//                                    case "West": {
//                                        if (x - 1 >= 0) {
//                                            for (CellContent cellContent : environment.getCell(x - 1, y).getCellContents()) {
//                                                if (cellContent instanceof Obstacle) {
//                                                    System.out.println("Agent " + getLocalName() + ": Refuse");
//                                                    throw new RefuseException("check-failed");
//                                                } else if (cellContent instanceof Hole) {
//                                                    if (((Hole) cellContent).depth == 0) {
//                                                        agree.setPerformative(ACLMessage.AGREE);
//                                                        break;
//                                                    } else {
//                                                        System.out.println("Agent " + getLocalName() + ": Refuse");
//                                                        throw new RefuseException("check-failed");
//                                                    }
//                                                }
//                                            }
//                                            agree.setPerformative(ACLMessage.AGREE);
//                                            break;
//                                        } else {
//                                            System.out.println("Agent " + getLocalName() + ": Refuse");
//                                            throw new RefuseException("check-failed");
//                                        }
//                                    }
//                                }
//                                break;
//                            }
//                            case "use_tile": {
//                                String direction = (String) jsonObject.get("direction");
//                                String tile_color = (String) jsonObject.get("color");
//                                int x = ((Long) jsonObject.get("Current x")).intValue();
//                                int y = ((Long) jsonObject.get("Current y")).intValue();
//
//                                switch (direction) {
//                                    case "North": {
//                                        if (y - 1 >= 0) {
//                                            for (CellContent cellContent : environment.getCell(x, y - 1).getCellContents()) {
//                                                if (cellContent instanceof Hole) {
//                                                    if (((Hole) cellContent).depth < 0) {
//                                                        if (((Hole) cellContent).color.equals(tile_color)) {
//                                                            environment.getColorAgentData(((Hole) cellContent).color).
//                                                                    setScore(environment.getColorAgentData(((Hole) cellContent).color).
//                                                                            getScore() + 10);
//                                                        }
//                                                        if (((Hole) cellContent).color.equals(tile_color) &&
//                                                                ((Hole) cellContent).depth == -1) {
//                                                            environment.getColorAgentData(((Hole) cellContent).color).
//                                                                    setScore(environment.getColorAgentData(((Hole) cellContent).color).
//                                                                            getScore() + 40);
//                                                        }
//                                                        agree.setPerformative(ACLMessage.AGREE);
//                                                    }
//                                                }
//                                            }
//                                        } else {
//                                            System.out.println("Agent " + getLocalName() + ": Refuse");
//                                            throw new RefuseException("check-failed");
//                                        }
//                                    }
//                                    case "South": {
//                                        if (y + 1 <= environment.height - 1) {
//                                            for (CellContent cellContent : environment.getCell(x, y + 1).getCellContents()) {
//                                                if (cellContent instanceof Hole) {
//                                                    if (((Hole) cellContent).depth < 0) {
//                                                        if (((Hole) cellContent).color.equals(tile_color)) {
//                                                            environment.getColorAgentData(((Hole) cellContent).color).
//                                                                    setScore(environment.getColorAgentData(((Hole) cellContent).color).
//                                                                            getScore() + 10);
//                                                        }
//                                                        if (((Hole) cellContent).color.equals(tile_color) &&
//                                                                ((Hole) cellContent).depth == -1) {
//                                                            environment.getColorAgentData(((Hole) cellContent).color).
//                                                                    setScore(environment.getColorAgentData(((Hole) cellContent).color).
//                                                                            getScore() + 40);
//                                                        }
//                                                        agree.setPerformative(ACLMessage.AGREE);
//                                                    }
//                                                }
//                                            }
//                                        } else {
//                                            System.out.println("Agent " + getLocalName() + ": Refuse");
//                                            throw new RefuseException("check-failed");
//                                        }
//                                    }
//                                    case "East": {
//                                        if (x + 1 <= environment.width - 1) {
//                                            for (CellContent cellContent : environment.getCell(x + 1, y).getCellContents()) {
//                                                if (cellContent instanceof Hole) {
//                                                    if (((Hole) cellContent).depth < 0) {
//                                                        if (((Hole) cellContent).color.equals(tile_color)) {
//                                                            environment.getColorAgentData(((Hole) cellContent).color).
//                                                                    setScore(environment.getColorAgentData(((Hole) cellContent).color).
//                                                                            getScore() + 10);
//                                                        }
//                                                        if (((Hole) cellContent).color.equals(tile_color) &&
//                                                                ((Hole) cellContent).depth == -1) {
//                                                            environment.getColorAgentData(((Hole) cellContent).color).
//                                                                    setScore(environment.getColorAgentData(((Hole) cellContent).color).
//                                                                            getScore() + 40);
//                                                        }
//                                                        agree.setPerformative(ACLMessage.AGREE);
//                                                    }
//                                                }
//                                            }
//                                        } else {
//                                            System.out.println("Agent " + getLocalName() + ": Refuse");
//                                            throw new RefuseException("check-failed");
//                                        }
//                                    }
//                                    case "West": {
//                                        if (x - 1 >= 0) {
//                                            for (CellContent cellContent : environment.getCell(x - 1, y).getCellContents()) {
//                                                if (cellContent instanceof Hole) {
//                                                    if (((Hole) cellContent).depth < 0) {
//                                                        if (((Hole) cellContent).color.equals(tile_color)) {
//                                                            environment.getColorAgentData(((Hole) cellContent).color).
//                                                                    setScore(environment.getColorAgentData(((Hole) cellContent).color).
//                                                                            getScore() + 10);
//                                                        }
//                                                        if (((Hole) cellContent).color.equals(tile_color) &&
//                                                                ((Hole) cellContent).depth == -1) {
//                                                            environment.getColorAgentData(((Hole) cellContent).color).
//                                                                    setScore(environment.getColorAgentData(((Hole) cellContent).color).
//                                                                            getScore() + 40);
//                                                        }
//                                                        agree.setPerformative(ACLMessage.AGREE);
//                                                    }
//                                                }
//                                            }
//                                        } else {
//                                            System.out.println("Agent " + getLocalName() + ": Refuse");
//                                            throw new RefuseException("check-failed");
//                                        }
//                                    }
//                                }
//
//                                break;
//                            }
//                            case "transfer_points": {
//                                int points = ((Long) jsonObject.get("points")).intValue();
//                                ColorAgentData agent = ((ColorAgentData) jsonObject.get("agent"));
//
//                                agree.setPerformative(ACLMessage.AGREE);
//                                environment.getColorAgentData(agent.getColor()).
//                                        setScore(environment.getColorAgentData(agent.getColor()).getScore() + points);
//                                break;
//                            }
//                            default:
//                                System.out.println("Agent " + getLocalName() + ": Refuse");
//                                throw new RefuseException("check-failed");
//                        }
//
//                        return null;
//                    }
//
//                    protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) throws FailureException {
                       // System.out.println(request.getContent());
                ACLMessage request = myAgent.receive();
                if(request!=null){
                        Object obj = JSONValue.parse(request.getContent());
                        JSONObject jsonObject = (JSONObject) obj;

                        String action = (String) jsonObject.get("action");

                        ACLMessage inform = request.createReply();
                        switch (action) {
                            case "pick": {
                                String color = (String) jsonObject.get("color");
                                int x = environment.getColorAgentData((String) jsonObject.get("agent_color")).getX();
                                int y = environment.getColorAgentData((String) jsonObject.get("agent_color")).getY();
                                inform.setContent("refuse");
                                if (environment.getCell(x, y).getCellContents() != null) {
                                    for (CellContent cellContent : environment.getCell(x, y).getCellContents()) {
                                        if (cellContent instanceof Tile) {
                                            if (cellContent.getColor().equals(color)) {
                                                inform.setPerformative(ACLMessage.INFORM);
                                                inform.setContent("picked");
                                                environment.getColorAgentData((String) jsonObject.get("agent_color")).setTile(new Tile(color,1));
                                                ((Tile) cellContent).setNumberOfElements(((Tile) cellContent).getNumberOfElements()-1);
                                                SingletoneBuffer.getInstance().addLogToPrint(Log.log(myAgent, "accept " + (String) jsonObject.get("agent_color") +" pick up tile " + color));
                                            }
                                        }
                                    }
                                }
                                break;
                            }
                            case "drop_tile": {
                                int x = environment.getColorAgentData((String) jsonObject.get("color")).getX();
                                int y = environment.getColorAgentData((String) jsonObject.get("color")).getY();
                                if (environment.getCell(x,y).getCellContents() != null) {
                                    for (CellContent cellContent : environment.getCell(x, y).getCellContents()) {
                                        if (cellContent instanceof Tile) {
                                            if (cellContent.getColor().equals((String) jsonObject.get("color"))) {
                                                inform.setPerformative(ACLMessage.INFORM);
                                                inform.setContent("dropped on same tiles");
                                                break;
                                            }
                                        }
                                    }
                                    inform.setPerformative(ACLMessage.INFORM);
                                    inform.setContent("formed new tile");
                                }
                                break;
                            }
                            case "move": {
                                String direction = (String) jsonObject.get("direction");
                                int x = environment.getColorAgentData((String) jsonObject.get("color")).getX();
                                int y = environment.getColorAgentData((String) jsonObject.get("color")).getY();
                                switch (direction) {
                                    case "North": {
                                        if (y - 1 >= 0) {
                                            for (CellContent cellContent : environment.getCell(x, y - 1).getCellContents()) {
                                                if (cellContent instanceof Obstacle) {
                                                    inform.setContent("refuse");
                                                    SingletoneBuffer.getInstance().addLogToPrint(Log.log(myAgent, ": refused " +(String) jsonObject.get("color")+" go  North"));
                                                } else if (cellContent instanceof Hole) {
                                                    if (((Hole) cellContent).depth == 0) {
                                                        inform.setPerformative(ACLMessage.INFORM);
                                                        inform.setContent("moved");
                                                        environment.getColorAgentData((String) jsonObject.get("color")).setY(y-1);
                                                        SingletoneBuffer.getInstance().addLogToPrint(Log.log(myAgent, "accept " + (String) jsonObject.get("color") + " go North"));
                                                        break;
                                                    } else {
                                                        inform.setContent("refuse");
                                                        SingletoneBuffer.getInstance().addLogToPrint(Log.log(myAgent, ": refused " +(String) jsonObject.get("color")+" go  North"));
                                                    }
                                                }
                                            }
                                            inform.setPerformative(ACLMessage.INFORM);
                                            inform.setContent("moved");
                                            environment.getColorAgentData((String) jsonObject.get("color")).setY(y-1);
                                            SingletoneBuffer.getInstance().addLogToPrint(Log.log(myAgent, "accept " + (String) jsonObject.get("color") + " go North"));
                                        } else {
                                            inform.setContent("refuse");
                                            SingletoneBuffer.getInstance().addLogToPrint(Log.log(myAgent, ": refused " +(String) jsonObject.get("color")+" go  North"));
                                        }
                                    }
                                    case "South": {
                                        if (y + 1 <= environment.height - 1) {
                                            for (CellContent cellContent : environment.getCell(x, y + 1).getCellContents()) {
                                                if (cellContent instanceof Obstacle) {
                                                    inform.setContent("refuse");
                                                    SingletoneBuffer.getInstance().addLogToPrint(Log.log(myAgent, ": refused " +(String) jsonObject.get("color")+" go  South"));
                                                } else if (cellContent instanceof Hole) {
                                                    if (((Hole) cellContent).depth == 0) {
                                                        inform.setPerformative(ACLMessage.INFORM);
                                                        inform.setContent("moved");
                                                        environment.getColorAgentData((String) jsonObject.get("color")).setY(y+1);
                                                        SingletoneBuffer.getInstance().addLogToPrint(Log.log(myAgent, "accept " + (String) jsonObject.get("color") + " go South"));
                                                        break;
                                                    } else {
                                                        inform.setContent("refuse");
                                                        SingletoneBuffer.getInstance().addLogToPrint(Log.log(myAgent, ": refused " +(String) jsonObject.get("color")+" go  South"));
                                                    }
                                                }
                                            }
                                            inform.setPerformative(ACLMessage.INFORM);
                                            inform.setContent("moved");
                                            environment.getColorAgentData((String) jsonObject.get("color")).setY(y+1);
                                            SingletoneBuffer.getInstance().addLogToPrint(Log.log(myAgent, "accept " + (String) jsonObject.get("color") + " go South"));
                                        } else {
                                            inform.setContent("refuse");
                                            SingletoneBuffer.getInstance().addLogToPrint(Log.log(myAgent, ": refused " +(String) jsonObject.get("color")+" go  South"));
                                        }
                                    }
                                    case "East": {
                                        if (x + 1 <= environment.width - 1) {
                                            for (CellContent cellContent : environment.getCell(x + 1, y).getCellContents()) {
                                                if (cellContent instanceof Obstacle) {
                                                    inform.setContent("refuse");
                                                    SingletoneBuffer.getInstance().addLogToPrint(Log.log(myAgent, ": refused " +(String) jsonObject.get("color")+" go  East"));
                                                } else if (cellContent instanceof Hole) {
                                                    if (((Hole) cellContent).depth == 0) {
                                                        inform.setPerformative(ACLMessage.INFORM);
                                                        inform.setContent("moved");
                                                        environment.getColorAgentData((String) jsonObject.get("color")).setX(x+1);
                                                        SingletoneBuffer.getInstance().addLogToPrint(Log.log(myAgent, "accept " + (String) jsonObject.get("color") + " go East"));
                                                        break;
                                                    } else {
                                                        inform.setContent("refuse");
                                                        SingletoneBuffer.getInstance().addLogToPrint(Log.log(myAgent, ": refused " +(String) jsonObject.get("color")+" go  East"));
                                                    }
                                                }
                                            }
                                            inform.setPerformative(ACLMessage.INFORM);
                                            inform.setContent("moved");
                                            environment.getColorAgentData((String) jsonObject.get("color")).setX(x+1);
                                            SingletoneBuffer.getInstance().addLogToPrint(Log.log(myAgent, "accept " + (String) jsonObject.get("color") + " go East"));
                                        } else {
                                            inform.setContent("refuse");
                                            SingletoneBuffer.getInstance().addLogToPrint(Log.log(myAgent, ": refused " +(String) jsonObject.get("color")+" go  East"));
                                        }
                                    }
                                    case "West": {
                                        if (x - 1 >= 0) {
                                            for (CellContent cellContent : environment.getCell(x - 1, y).getCellContents()) {
                                                if (cellContent instanceof Obstacle) {
                                                    inform.setContent("refuse");
                                                    SingletoneBuffer.getInstance().addLogToPrint(Log.log(myAgent, ": refused " +(String) jsonObject.get("color")+" go  West"));
                                                } else if (cellContent instanceof Hole) {
                                                    if (((Hole) cellContent).depth == 0) {
                                                        inform.setPerformative(ACLMessage.INFORM);
                                                        inform.setContent("moved");
                                                        environment.getColorAgentData((String) jsonObject.get("color")).setX(x-1);
                                                        SingletoneBuffer.getInstance().addLogToPrint(Log.log(myAgent, "accept " + (String) jsonObject.get("color") + " go West"));
                                                        break;
                                                    } else {
                                                        inform.setContent("refuse");
                                                        SingletoneBuffer.getInstance().addLogToPrint(Log.log(myAgent, ": refused " +(String) jsonObject.get("color")+" go  West"));
                                                    }
                                                }
                                            }
                                            inform.setPerformative(ACLMessage.INFORM);
                                            inform.setContent("moved");
                                            environment.getColorAgentData((String) jsonObject.get("color")).setX(x-1);
                                            SingletoneBuffer.getInstance().addLogToPrint(Log.log(myAgent, "accept " + (String) jsonObject.get("color") + " go West"));
                                        } else {
                                            inform.setContent("refuse");
                                            SingletoneBuffer.getInstance().addLogToPrint(Log.log(myAgent, ": refused " +(String) jsonObject.get("color")+" go  West"));
                                        }
                                    }
                                }
                                break;
                            }
                            case "use_tile": {
                                String direction = (String) jsonObject.get("direction");
                                String tile_color = (String) jsonObject.get("color");
                                int x = ((Long) jsonObject.get("Current x")).intValue();
                                int y = ((Long) jsonObject.get("Current y")).intValue();

                                switch (direction) {
                                    case "North": {
                                        if (y - 1 >= 0) {
                                            for (CellContent cellContent : environment.getCell(x, y - 1).getCellContents()) {
                                                if (cellContent instanceof Hole) {
                                                    if (((Hole) cellContent).depth < 0) {
                                                        inform.setPerformative(ACLMessage.INFORM);
                                                        inform.setContent("tile was used");
                                                    }
                                                }
                                            }
                                        } else {
                                            System.out.println("Agent "+getLocalName()+": Action failed");
                                            //throw new FailureException("unexpected-error");
                                        }
                                    }
                                    case "South": {
                                        if (y + 1 <= environment.height - 1) {
                                            for (CellContent cellContent : environment.getCell(x, y + 1).getCellContents()) {
                                                if (cellContent instanceof Hole) {
                                                    if (((Hole) cellContent).depth < 0) {
                                                        inform.setPerformative(ACLMessage.INFORM);
                                                        inform.setContent("tile was used");
                                                    }
                                                }
                                            }
                                        } else {
                                            System.out.println("Agent "+getLocalName()+": Action failed");
                                            //throw new FailureException("unexpected-error");
                                        }
                                    }
                                    case "East": {
                                        if (x + 1 <= environment.width - 1) {
                                            for (CellContent cellContent : environment.getCell(x + 1, y).getCellContents()) {
                                                if (cellContent instanceof Hole) {
                                                    if (((Hole) cellContent).depth < 0) {
                                                        inform.setPerformative(ACLMessage.INFORM);
                                                        inform.setContent("tile was used");
                                                    }
                                                }
                                            }
                                        } else {
                                            System.out.println("Agent "+getLocalName()+": Action failed");
                                            //throw new FailureException("unexpected-error");
                                        }
                                    }
                                    case "West": {
                                        if (x - 1 >= 0) {
                                            for (CellContent cellContent : environment.getCell(x - 1, y).getCellContents()) {
                                                if (cellContent instanceof Hole) {
                                                    if (((Hole) cellContent).depth < 0) {
                                                        inform.setPerformative(ACLMessage.INFORM);
                                                        inform.setContent("tile was used");
                                                    }
                                                }
                                            }
                                        } else {
                                            System.out.println("Agent "+getLocalName()+": Action failed");
                                            //throw new FailureException("unexpected-error");
                                        }
                                    }
                                }

                                break;
                            }
                            case "transfer_points": {
                                int points = ((Long) jsonObject.get("points")).intValue();
                                ColorAgentData agent = ((ColorAgentData) jsonObject.get("agent"));

                                inform.setPerformative(ACLMessage.INFORM);
                                inform.setContent("points were transferred");
                                break;
                            }
                            default:
                                System.out.println("Agent "+getLocalName()+": Action failed");
                                //throw new FailureException("unexpected-error");
                        }

                        //return inform;
//                    try {
//                        Thread.sleep(environment.getTimeToPerformAction());
//                    } catch (InterruptedException e) {
//                        throw new RuntimeException(e);
//                    }
                    myAgent.send(inform);
                    }
                //} );
            }
        });



        addBehaviour(new TickerBehaviour(this,TICK_PERIOD) {
            @Override
            protected void onTick() {
                environment.print();
                System.out.println("\n\n");
                SingletoneBuffer.getInstance().printLogs();
                if(getTickCount() >= environment.getTotalTimeOfWorking()/(float)environment.timeToPerformAction) {
                    stop();
                }
            }
        });


//        addBehaviour(new AchieveREResponder(this, template) {
//            protected ACLMessage prepareResponse(ACLMessage request) throws NotUnderstoodException, RefuseException {
//                System.out.println("Agent " + getLocalName() + ": REQUEST received from " +
//                        request.getSender().getName() + ". Action is " + request.getContent());
//
//                Object obj = JSONValue.parse(request.getContent());
//                JSONObject jsonObject = (JSONObject) obj;
//
//                String action = (String) jsonObject.get("action");
//
//                ACLMessage agree = request.createReply();
//                switch (action) {
//                    case "pick": {
//                        String color = (String) jsonObject.get("color");
//                        int x = ((Long) jsonObject.get("Current x")).intValue();
//                        int y = ((Long) jsonObject.get("Current y")).intValue();
//                        if (environment.getCell(x, y).getCellContents() != null) {
//                            for (CellContent cellContent : environment.getCell(x, y).getCellContents()) {
//                                if (cellContent instanceof Tile) {
//                                    if (cellContent.getColor().equals(color)) {
//                                        environment.getCell(x, y).removeContent(cellContent);
//                                        agree.setPerformative(ACLMessage.AGREE);
//                                    }
//                                }
//                            }
//                        }
//                        break;
//                    }
//                    case "drop_tile": {
//                        int x = ((Long) jsonObject.get("Current x")).intValue();
//                        int y = ((Long) jsonObject.get("Current y")).intValue();
//                        if (environment.getCell(x,y).getCellContents() != null) {
//                            for (CellContent cellContent : environment.getCell(x, y).getCellContents()) {
//                                if (cellContent instanceof Tile) {
//                                    if (cellContent.getColor().equals((String) jsonObject.get("color"))) {
//                                        ((Tile) cellContent).numberOfElements += 1;
//                                        agree.setPerformative(ACLMessage.AGREE);
//                                        break;
//                                    }
//                                }
//                            }
//                            environment.getCell(x,y).addContent(new Tile((String) jsonObject.get("color"),1));
//                            agree.setPerformative(ACLMessage.AGREE);
//                        }
//                        break;
//                    }
//                    case "move": {
//                        String direction = (String) jsonObject.get("direction");
//                        int x = ((Long) jsonObject.get("Current x")).intValue();
//                        int y = ((Long) jsonObject.get("Current y")).intValue();
//                        switch (direction) {
//                            case "North": {
//                                if (y - 1 >= 0) {
//                                    for (CellContent cellContent : environment.getCell(x, y - 1).getCellContents()) {
//                                        if (cellContent instanceof Obstacle) {
//                                            System.out.println("Agent " + getLocalName() + ": Refuse");
//                                            throw new RefuseException("check-failed");
//                                        } else if (cellContent instanceof Hole) {
//                                            if (((Hole) cellContent).depth == 0) {
//                                                agree.setPerformative(ACLMessage.AGREE);
//                                                break;
//                                            } else {
//                                                System.out.println("Agent " + getLocalName() + ": Refuse");
//                                                throw new RefuseException("check-failed");
//                                            }
//                                        }
//                                    }
//                                    agree.setPerformative(ACLMessage.AGREE);
//                                    break;
//                                } else {
//                                    System.out.println("Agent " + getLocalName() + ": Refuse");
//                                    throw new RefuseException("check-failed");
//                                }
//                            }
//                            case "South": {
//                                if (y + 1 <= environment.height - 1) {
//                                    for (CellContent cellContent : environment.getCell(x, y + 1).getCellContents()) {
//                                        if (cellContent instanceof Obstacle) {
//                                            System.out.println("Agent " + getLocalName() + ": Refuse");
//                                            throw new RefuseException("check-failed");
//                                        } else if (cellContent instanceof Hole) {
//                                            if (((Hole) cellContent).depth == 0) {
//                                                agree.setPerformative(ACLMessage.AGREE);
//                                                break;
//                                            } else {
//                                                System.out.println("Agent " + getLocalName() + ": Refuse");
//                                                throw new RefuseException("check-failed");
//                                            }
//                                        }
//                                    }
//                                    agree.setPerformative(ACLMessage.AGREE);
//                                    break;
//                                } else {
//                                    System.out.println("Agent " + getLocalName() + ": Refuse");
//                                    throw new RefuseException("check-failed");
//                                }
//                            }
//                            case "East": {
//                                if (x + 1 <= environment.width - 1) {
//                                    for (CellContent cellContent : environment.getCell(x + 1, y).getCellContents()) {
//                                        if (cellContent instanceof Obstacle) {
//                                            System.out.println("Agent " + getLocalName() + ": Refuse");
//                                            throw new RefuseException("check-failed");
//                                        } else if (cellContent instanceof Hole) {
//                                            if (((Hole) cellContent).depth == 0) {
//                                                agree.setPerformative(ACLMessage.AGREE);
//                                                break;
//                                            } else {
//                                                System.out.println("Agent " + getLocalName() + ": Refuse");
//                                                throw new RefuseException("check-failed");
//                                            }
//                                        }
//                                    }
//                                    agree.setPerformative(ACLMessage.AGREE);
//                                    break;
//                                } else {
//                                    System.out.println("Agent " + getLocalName() + ": Refuse");
//                                    throw new RefuseException("check-failed");
//                                }
//                            }
//                            case "West": {
//                                if (x - 1 >= 0) {
//                                    for (CellContent cellContent : environment.getCell(x - 1, y).getCellContents()) {
//                                        if (cellContent instanceof Obstacle) {
//                                            System.out.println("Agent " + getLocalName() + ": Refuse");
//                                            throw new RefuseException("check-failed");
//                                        } else if (cellContent instanceof Hole) {
//                                            if (((Hole) cellContent).depth == 0) {
//                                                agree.setPerformative(ACLMessage.AGREE);
//                                                break;
//                                            } else {
//                                                System.out.println("Agent " + getLocalName() + ": Refuse");
//                                                throw new RefuseException("check-failed");
//                                            }
//                                        }
//                                    }
//                                    agree.setPerformative(ACLMessage.AGREE);
//                                    break;
//                                } else {
//                                    System.out.println("Agent " + getLocalName() + ": Refuse");
//                                    throw new RefuseException("check-failed");
//                                }
//                            }
//                        }
//                        break;
//                    }
//                    case "use_tile": {
//                        String direction = (String) jsonObject.get("direction");
//                        String tile_color = (String) jsonObject.get("color");
//                        int x = ((Long) jsonObject.get("Current x")).intValue();
//                        int y = ((Long) jsonObject.get("Current y")).intValue();
//
//                        switch (direction) {
//                            case "North": {
//                                if (y - 1 >= 0) {
//                                    for (CellContent cellContent : environment.getCell(x, y - 1).getCellContents()) {
//                                        if (cellContent instanceof Hole) {
//                                            if (((Hole) cellContent).depth < 0) {
//                                                if (((Hole) cellContent).color.equals(tile_color)) {
//                                                    environment.getColorAgentData(((Hole) cellContent).color).
//                                                            setScore(environment.getColorAgentData(((Hole) cellContent).color).
//                                                                    getScore() + 10);
//                                                }
//                                                if (((Hole) cellContent).color.equals(tile_color) &&
//                                                        ((Hole) cellContent).depth == -1) {
//                                                    environment.getColorAgentData(((Hole) cellContent).color).
//                                                            setScore(environment.getColorAgentData(((Hole) cellContent).color).
//                                                                    getScore() + 40);
//                                                }
//                                                agree.setPerformative(ACLMessage.AGREE);
//                                            }
//                                        }
//                                    }
//                                } else {
//                                    System.out.println("Agent " + getLocalName() + ": Refuse");
//                                    throw new RefuseException("check-failed");
//                                }
//                            }
//                            case "South": {
//                                if (y + 1 <= environment.height - 1) {
//                                    for (CellContent cellContent : environment.getCell(x, y + 1).getCellContents()) {
//                                        if (cellContent instanceof Hole) {
//                                            if (((Hole) cellContent).depth < 0) {
//                                                if (((Hole) cellContent).color.equals(tile_color)) {
//                                                    environment.getColorAgentData(((Hole) cellContent).color).
//                                                            setScore(environment.getColorAgentData(((Hole) cellContent).color).
//                                                                    getScore() + 10);
//                                                }
//                                                if (((Hole) cellContent).color.equals(tile_color) &&
//                                                        ((Hole) cellContent).depth == -1) {
//                                                    environment.getColorAgentData(((Hole) cellContent).color).
//                                                            setScore(environment.getColorAgentData(((Hole) cellContent).color).
//                                                                    getScore() + 40);
//                                                }
//                                                agree.setPerformative(ACLMessage.AGREE);
//                                            }
//                                        }
//                                    }
//                                } else {
//                                    System.out.println("Agent " + getLocalName() + ": Refuse");
//                                    throw new RefuseException("check-failed");
//                                }
//                            }
//                            case "East": {
//                                if (x + 1 <= environment.width - 1) {
//                                    for (CellContent cellContent : environment.getCell(x + 1, y).getCellContents()) {
//                                        if (cellContent instanceof Hole) {
//                                            if (((Hole) cellContent).depth < 0) {
//                                                if (((Hole) cellContent).color.equals(tile_color)) {
//                                                    environment.getColorAgentData(((Hole) cellContent).color).
//                                                            setScore(environment.getColorAgentData(((Hole) cellContent).color).
//                                                                    getScore() + 10);
//                                                }
//                                                if (((Hole) cellContent).color.equals(tile_color) &&
//                                                        ((Hole) cellContent).depth == -1) {
//                                                    environment.getColorAgentData(((Hole) cellContent).color).
//                                                            setScore(environment.getColorAgentData(((Hole) cellContent).color).
//                                                                    getScore() + 40);
//                                                }
//                                                agree.setPerformative(ACLMessage.AGREE);
//                                            }
//                                        }
//                                    }
//                                } else {
//                                    System.out.println("Agent " + getLocalName() + ": Refuse");
//                                    throw new RefuseException("check-failed");
//                                }
//                            }
//                            case "West": {
//                                if (x - 1 >= 0) {
//                                    for (CellContent cellContent : environment.getCell(x - 1, y).getCellContents()) {
//                                        if (cellContent instanceof Hole) {
//                                            if (((Hole) cellContent).depth < 0) {
//                                                if (((Hole) cellContent).color.equals(tile_color)) {
//                                                    environment.getColorAgentData(((Hole) cellContent).color).
//                                                            setScore(environment.getColorAgentData(((Hole) cellContent).color).
//                                                                    getScore() + 10);
//                                                }
//                                                if (((Hole) cellContent).color.equals(tile_color) &&
//                                                        ((Hole) cellContent).depth == -1) {
//                                                    environment.getColorAgentData(((Hole) cellContent).color).
//                                                            setScore(environment.getColorAgentData(((Hole) cellContent).color).
//                                                                    getScore() + 40);
//                                                }
//                                                agree.setPerformative(ACLMessage.AGREE);
//                                            }
//                                        }
//                                    }
//                                } else {
//                                    System.out.println("Agent " + getLocalName() + ": Refuse");
//                                    throw new RefuseException("check-failed");
//                                }
//                            }
//                        }
//
//                        break;
//                    }
//                    case "transfer_points": {
//                        int points = ((Long) jsonObject.get("points")).intValue();
//                        ColorAgentData agent = ((ColorAgentData) jsonObject.get("agent"));
//
//                        agree.setPerformative(ACLMessage.AGREE);
//                        environment.getColorAgentData(agent.getColor()).
//                                setScore(environment.getColorAgentData(agent.getColor()).getScore() + points);
//                        break;
//                    }
//                    default:
//                        System.out.println("Agent " + getLocalName() + ": Refuse");
//                        throw new RefuseException("check-failed");
//                }
//
//                return null;
//            }
//
//            protected ACLMessage prepareResultNotification(ACLMessage request, ACLMessage response) throws FailureException {
//                System.out.println(request.getContent());
//                Object obj = JSONValue.parse(request.getContent());
//                JSONObject jsonObject = (JSONObject) obj;
//
//                String action = (String) jsonObject.get("action");
//
//                ACLMessage inform = request.createReply();
//                switch (action) {
//                    case "pick": {
//                        String color = (String) jsonObject.get("color");
//                        int x = ((Long) jsonObject.get("Current x")).intValue();
//                        int y = ((Long) jsonObject.get("Current y")).intValue();
//                        if (environment.getCell(x, y).getCellContents() != null) {
//                            for (CellContent cellContent : environment.getCell(x, y).getCellContents()) {
//                                if (cellContent instanceof Tile) {
//                                    if (cellContent.getColor().equals(color)) {
//                                        inform.setPerformative(ACLMessage.INFORM);
//                                        inform.setContent("removed");
//                                    }
//                                }
//                            }
//                        }
//                        break;
//                    }
//                    case "drop_tile": {
//                        int x = ((Long) jsonObject.get("Current x")).intValue();
//                        int y = ((Long) jsonObject.get("Current y")).intValue();
//                        if (environment.getCell(x,y).getCellContents() != null) {
//                            for (CellContent cellContent : environment.getCell(x, y).getCellContents()) {
//                                if (cellContent instanceof Tile) {
//                                    if (cellContent.getColor().equals((String) jsonObject.get("color"))) {
//                                        inform.setPerformative(ACLMessage.INFORM);
//                                        inform.setContent("dropped on same tiles");
//                                        break;
//                                    }
//                                }
//                            }
//                            inform.setPerformative(ACLMessage.INFORM);
//                            inform.setContent("formed new tile");
//                        }
//                        break;
//                    }
//                    case "move": {
//                        String direction = (String) jsonObject.get("direction");
//                        int x = ((Long) jsonObject.get("Current x")).intValue();
//                        int y = ((Long) jsonObject.get("Current y")).intValue();
//                        switch (direction) {
//                            case "North": {
//                                if (y - 1 >= 0) {
//                                    for (CellContent cellContent : environment.getCell(x, y - 1).getCellContents()) {
//                                        if (cellContent instanceof Obstacle) {
//                                            System.out.println("Agent "+getLocalName()+": Action failed");
//                                            throw new FailureException("unexpected-error");
//                                        } else if (cellContent instanceof Hole) {
//                                            if (((Hole) cellContent).depth == 0) {
//                                                inform.setPerformative(ACLMessage.INFORM);
//                                                inform.setContent("moved");
//                                                break;
//                                            } else {
//                                                System.out.println("Agent "+getLocalName()+": Action failed");
//                                                throw new FailureException("unexpected-error");
//                                            }
//                                        }
//                                    }
//                                    inform.setPerformative(ACLMessage.INFORM);
//                                    inform.setContent("moved");
//                                    environment.getColorAgentData((String) jsonObject.get("color")).setY(y-1);
//                                    SingletoneBuffer.getInstance().addLogToPrint(Log.log(myAgent, "accept " + (String) jsonObject.get("color") + " go North"));
//                                } else {
//                                    System.out.println("Agent "+getLocalName()+": Action failed");
//                                    throw new FailureException("unexpected-error");
//                                }
//                            }
//                            case "South": {
//                                if (y + 1 <= environment.height - 1) {
//                                    for (CellContent cellContent : environment.getCell(x, y + 1).getCellContents()) {
//                                        if (cellContent instanceof Obstacle) {
//                                            System.out.println("Agent "+getLocalName()+": Action failed");
//                                            throw new FailureException("unexpected-error");
//                                        } else if (cellContent instanceof Hole) {
//                                            if (((Hole) cellContent).depth == 0) {
//                                                inform.setPerformative(ACLMessage.INFORM);
//                                                inform.setContent("moved");
//                                                break;
//                                            } else {
//                                                System.out.println("Agent "+getLocalName()+": Action failed");
//                                                throw new FailureException("unexpected-error");
//                                            }
//                                        }
//                                    }
//                                    inform.setPerformative(ACLMessage.INFORM);
//                                    inform.setContent("moved");
//                                    environment.getColorAgentData((String) jsonObject.get("color")).setY(y+1);
//                                    SingletoneBuffer.getInstance().addLogToPrint(Log.log(myAgent, "accept " + (String) jsonObject.get("color") + " go South"));
//                                } else {
//                                    System.out.println("Agent "+getLocalName()+": Action failed");
//                                    throw new FailureException("unexpected-error");
//                                }
//                            }
//                            case "East": {
//                                if (x + 1 <= environment.width - 1) {
//                                    for (CellContent cellContent : environment.getCell(x + 1, y).getCellContents()) {
//                                        if (cellContent instanceof Obstacle) {
//                                            System.out.println("Agent "+getLocalName()+": Action failed");
//                                            throw new FailureException("unexpected-error");
//                                        } else if (cellContent instanceof Hole) {
//                                            if (((Hole) cellContent).depth == 0) {
//                                                inform.setPerformative(ACLMessage.INFORM);
//                                                inform.setContent("moved");
//                                                break;
//                                            } else {
//                                                System.out.println("Agent "+getLocalName()+": Action failed");
//                                                throw new FailureException("unexpected-error");
//                                            }
//                                        }
//                                    }
//                                    inform.setPerformative(ACLMessage.INFORM);
//                                    inform.setContent("moved");
//                                    int localx = environment.getColorAgentData((String) jsonObject.get("color")).getX();
//                                    environment.getColorAgentData((String) jsonObject.get("color")).setX(localx+1);
//                                    SingletoneBuffer.getInstance().addLogToPrint(Log.log(myAgent, "accept " + (String) jsonObject.get("color") + " go East"));
//                                } else {
//                                    System.out.println("Agent "+getLocalName()+": Action failed");
//                                    throw new FailureException("unexpected-error");
//                                }
//                            }
//                            case "West": {
//                                if (x - 1 >= 0) {
//                                    for (CellContent cellContent : environment.getCell(x - 1, y).getCellContents()) {
//                                        if (cellContent instanceof Obstacle) {
//                                            System.out.println("Agent "+getLocalName()+": Action failed");
//                                            throw new FailureException("unexpected-error");
//                                        } else if (cellContent instanceof Hole) {
//                                            if (((Hole) cellContent).depth == 0) {
//                                                inform.setPerformative(ACLMessage.INFORM);
//                                                inform.setContent("moved");
//                                                break;
//                                            } else {
//                                                System.out.println("Agent "+getLocalName()+": Action failed");
//                                                throw new FailureException("unexpected-error");
//                                            }
//                                        }
//                                    }
//                                    inform.setPerformative(ACLMessage.INFORM);
//                                    inform.setContent("moved");
//                                    int localx = environment.getColorAgentData((String) jsonObject.get("color")).getX();
//                                    environment.getColorAgentData((String) jsonObject.get("color")).setX(localx-1);
//                                    SingletoneBuffer.getInstance().addLogToPrint(Log.log(myAgent, "accept " + (String) jsonObject.get("color") + " go West"));
//                                } else {
//                                    System.out.println("Agent "+getLocalName()+": Action failed");
//                                    throw new FailureException("unexpected-error");
//                                }
//                            }
//                        }
//                        break;
//                    }
//                    case "use_tile": {
//                        String direction = (String) jsonObject.get("direction");
//                        String tile_color = (String) jsonObject.get("color");
//                        int x = ((Long) jsonObject.get("Current x")).intValue();
//                        int y = ((Long) jsonObject.get("Current y")).intValue();
//
//                        switch (direction) {
//                            case "North": {
//                                if (y - 1 >= 0) {
//                                    for (CellContent cellContent : environment.getCell(x, y - 1).getCellContents()) {
//                                        if (cellContent instanceof Hole) {
//                                            if (((Hole) cellContent).depth < 0) {
//                                                inform.setPerformative(ACLMessage.INFORM);
//                                                inform.setContent("tile was used");
//                                            }
//                                        }
//                                    }
//                                } else {
//                                    System.out.println("Agent "+getLocalName()+": Action failed");
//                                    throw new FailureException("unexpected-error");
//                                }
//                            }
//                            case "South": {
//                                if (y + 1 <= environment.height - 1) {
//                                    for (CellContent cellContent : environment.getCell(x, y + 1).getCellContents()) {
//                                        if (cellContent instanceof Hole) {
//                                            if (((Hole) cellContent).depth < 0) {
//                                                inform.setPerformative(ACLMessage.INFORM);
//                                                inform.setContent("tile was used");
//                                            }
//                                        }
//                                    }
//                                } else {
//                                    System.out.println("Agent "+getLocalName()+": Action failed");
//                                    throw new FailureException("unexpected-error");
//                                }
//                            }
//                            case "East": {
//                                if (x + 1 <= environment.width - 1) {
//                                    for (CellContent cellContent : environment.getCell(x + 1, y).getCellContents()) {
//                                        if (cellContent instanceof Hole) {
//                                            if (((Hole) cellContent).depth < 0) {
//                                                inform.setPerformative(ACLMessage.INFORM);
//                                                inform.setContent("tile was used");
//                                            }
//                                        }
//                                    }
//                                } else {
//                                    System.out.println("Agent "+getLocalName()+": Action failed");
//                                    throw new FailureException("unexpected-error");
//                                }
//                            }
//                            case "West": {
//                                if (x - 1 >= 0) {
//                                    for (CellContent cellContent : environment.getCell(x - 1, y).getCellContents()) {
//                                        if (cellContent instanceof Hole) {
//                                            if (((Hole) cellContent).depth < 0) {
//                                                inform.setPerformative(ACLMessage.INFORM);
//                                                inform.setContent("tile was used");
//                                            }
//                                        }
//                                    }
//                                } else {
//                                    System.out.println("Agent "+getLocalName()+": Action failed");
//                                    throw new FailureException("unexpected-error");
//                                }
//                            }
//                        }
//
//                        break;
//                    }
//                    case "transfer_points": {
//                        int points = ((Long) jsonObject.get("points")).intValue();
//                        ColorAgentData agent = ((ColorAgentData) jsonObject.get("agent"));
//
//                        inform.setPerformative(ACLMessage.INFORM);
//                        inform.setContent("points were transferred");
//                        break;
//                    }
//                    default:
//                        System.out.println("Agent "+getLocalName()+": Action failed");
//                        throw new FailureException("unexpected-error");
//                }
//
//                return inform;
//            }
//        } );
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
