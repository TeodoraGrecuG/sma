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

import java.io.*;
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

    private JSONObject performMovement(int wantedX, int wantedY, String agentColor){
        JSONObject resp = new JSONObject();
        if(environment.getCell(wantedX, wantedY).getCellContents().size()==0) {
            resp.put("response", "accept");
            environment.getColorAgentData(agentColor).setY(wantedY);
            environment.getColorAgentData(agentColor).setX(wantedX);
        }
        else {
            for (CellContent cellContent : environment.getCell(wantedX, wantedY).getCellContents()) {
                if (cellContent instanceof Obstacle) {
                    //inform.setContent("refuse");
                    resp.put("response", "refuse");
                    break;
                } else if (cellContent instanceof Hole) {
                    if (((Hole) cellContent).depth > 0) {
                        resp.put("response", "refuse");
                        break;
                    } else {
                        environment.getColorAgentData(agentColor).setY(wantedY);
                        environment.getColorAgentData(agentColor).setX(wantedX);
                        resp.put("response", "accept");
                        break;
                    }
                }
                else{
                    resp.put("response", "accept");
                    environment.getColorAgentData(agentColor).setY(wantedY);
                    environment.getColorAgentData(agentColor).setX(wantedX);
                    break;
                }
            }
        }
        return resp;
            //inform.setContent("moved");
    }

    private JSONObject understandMove(int x, int y, JSONObject jsonObject) {
        String direction = (String) jsonObject.get("additional_info");
        JSONObject respObj = new JSONObject();
        respObj.put("action", "move");
        respObj.put("additional_info", direction);
        switch (direction) {
            case "North": {
                if (y - 1 >= 0) {
                    respObj.putAll(performMovement(x,y-1,(String)jsonObject.get("agent_color")));
                } else {
                    respObj.put("response", "refuse");
                }
                break;
            }
            case "South": {
                if (y + 1 <= environment.height - 1) {
                    respObj.putAll(performMovement(x,y+1,(String)jsonObject.get("agent_color")));
                } else {
                    //inform.setContent("refuse");
                    respObj.put("response", "refuse");
                }
                break;
            }
            case "East": {
                if (x + 1 <= environment.width - 1) {
                    respObj.putAll(performMovement(x+1,y,(String)jsonObject.get("agent_color")));
                } else {
                    respObj.put("response", "refuse");
                }
                break;
            }
            case "West": {
                if (x - 1 >= 0) {
                    respObj.putAll(performMovement(x-1,y,(String)jsonObject.get("agent_color")));
                } else {
                    respObj.put("response", "refuse");
                }
                break;
            }
            default:{
                respObj.put("response", "refuse");
            }
        }
        return respObj;
    }

    private JSONObject performUse(int wantedX, int wantedY, JSONObject jsonObject) {
        JSONObject resp =  new JSONObject();
        String agentColor = (String) jsonObject.get("agent_color");
        String tile_color = environment.getColorAgentData(agentColor).getTile().getColor();
        resp.put("response", "refuse");
        if(environment.getColorAgentData(agentColor).getTile().getNumberOfElements()==0)
            return resp;

        for (CellContent cellContent : environment.getCell(wantedX, wantedY).getCellContents()) {
            if (cellContent instanceof Hole) {
                if (((Hole) cellContent).depth > 0) {
                    if(Objects.equals(tile_color, cellContent.getColor()))
                    {
                        //setez punctajul pentru agentul de culoarea dalei
                        environment.getColorAgentData(tile_color).setScore(environment.getColorAgentData(tile_color).getScore()+10);

                        if(((Hole) cellContent).getDepth() == 1)
                        {
                            environment.getColorAgentData(tile_color).setScore(environment.getColorAgentData(tile_color).getScore()+40);
                        }
                    }
                    environment.getColorAgentData(agentColor).setTile("",0);
                    ((Hole) cellContent).setDepth(((Hole) cellContent).getDepth()-1);
                    resp.put("score", environment.getColorAgentData(agentColor).getScore());
                    resp.put("response", "accept");
                }
            }
        }
        return resp;
    }

    private JSONObject understandUsage(int x, int y, JSONObject jsonObject) {
        String direction = (String) jsonObject.get("additional_info");

        JSONObject response = new JSONObject();
        response.put("additional_info", direction);
        response.put("action", "drop");

        switch (direction) {
            case "North": {
                if (y - 1 >= 0) {
                   response.putAll(performUse(x,y-1,jsonObject));
                } else {
                    response.put("response", "refuse");
                }
                break;
            }
            case "South": {
                if (y + 1 <= environment.height - 1) {
                    response.putAll(performUse(x, y+1, jsonObject));
                } else {
                    response.put("response", "refuse");
                }
                break;
            }
            case "East": {
                if (x + 1 <= environment.width - 1) {
                   response.putAll(performUse(x+1, y, jsonObject));
                } else {
                    response.put("response", "refuse");
                }
                break;
            }
            case "West": {
                if (x - 1 >= 0) {
                    response.putAll(performUse(x-1, y, jsonObject));
                } else {
                    response.put("response", "refuse");
                }
                break;
            }
        }
        return response;
    }

    private JSONObject performPickUp(int x, int y, JSONObject jsonObject) {
        JSONObject respObj = new JSONObject();
        String color = (String) jsonObject.get("additional_info");
        respObj.put("action", "pick up");
        respObj.put("additional_info", color);
        respObj.put("response", "refuse");

        // verificare daca agentul cara deja o dala
        if(environment.getColorAgentData((String) jsonObject.get("agent_color")).getTile().getNumberOfElements()>0)
        {
            return respObj;
        }

        if (environment.getCell(x, y).getCellContents() != null) {
            for (CellContent cellContent : environment.getCell(x, y).getCellContents()) {
                if (cellContent instanceof Tile) {
                    if (cellContent.getColor().equals(color)) {
                        respObj.put("response", "accept");
                        environment.getColorAgentData((String) jsonObject.get("agent_color")).setTile(color,1);
                        ((Tile) cellContent).setNumberOfElements(((Tile) cellContent).getNumberOfElements()-1);

                        if(((Tile) cellContent).getNumberOfElements()==0)
                        {
                            environment.getCell(x, y).getCellContents().remove(cellContent);
                        }
                        return respObj;
                        //SingletoneBuffer.getInstance().addLogToPrint(Log.log(myAgent, "accept " + (String) jsonObject.get("agent_color") +" pick up tile " + color));
                    }
                }
            }
        }
        return respObj;
    }

    private JSONObject performDrop(int x, int y, JSONObject jsonObject){
        JSONObject respObj = new JSONObject();
        String tileColor = environment.getColorAgentData((String) jsonObject.get("agent_color")).getTile().getColor();//(String) jsonObject.get("additional_info");
        respObj.put("action", "drop");
        respObj.put("additional_info", tileColor);
        respObj.put("response", "refuse");

        // verificare daca agentul are ce sa puna jos
        if(environment.getColorAgentData((String) jsonObject.get("agent_color")).getTile().getNumberOfElements()==0)
        {
            return respObj;
        }

        // daca exista deja un grup de aceasta culoare, sa  se adauge la grup
        for (CellContent cellContent : environment.getCell(x, y).getCellContents()) {
            if (cellContent instanceof Tile) {
                if (cellContent.getColor().equals(tileColor)) {
                    respObj.put("response", "accept");
                    environment.getColorAgentData((String) jsonObject.get("agent_color")).setTile("",0);
                    ((Tile) cellContent).setNumberOfElements(((Tile) cellContent).getNumberOfElements() + 1);
                    return respObj;
                }
            }
        }
        environment.getColorAgentData((String) jsonObject.get("agent_color")).setTile("",0);
        // adauga un nou cell content
        environment.getCell(x,y).addContent(new Tile(tileColor, 1));
        respObj.put("response", "accept");

        return respObj;
    }

    private void performSync(){
        //sincronizarea starii initiale a scorurilor
        addBehaviour(new SimpleBehaviour() {
            List<ACLMessage> tempMsg = new ArrayList<>();
            @Override
            public void action() {
                ACLMessage msg = myAgent.receive(TemplatesForEnvironment.templateSync);
                if (msg != null) {
                    SingletoneBuffer.getInstance().addLogToPrint(Log.log(myAgent, ": got initial sync request from "+msg.getSender().getLocalName()));
                    tempMsg.add(msg);
                }
                else {
                    block();
                }
            }

            @Override
            public boolean done() {
                if(tempMsg.size()==environment.getColorAgentsData().size())
                {
                    for(ACLMessage msg: tempMsg){
                        Object obj = JSONValue.parse(msg.getContent());
                        JSONObject jsonObject = (JSONObject) obj;

                        ACLMessage reply = msg.createReply();
                        reply.setPerformative( ACLMessage.INFORM );
                        reply.setProtocol(Proctocols.SYNC);

                        JSONObject resp = new JSONObject();
                        ColorAgentData temp = environment.getColorAgentData((String) jsonObject.get("agent_color"));
                        resp.put("action", "sync");
                        resp.put("response", "accept");
                        resp.put("score", temp.getScore());
                        resp.put("additional_info", "");
                        resp.put("time_to_perform_action", environment.getTimeToPerformAction());
                        resp.put("total_time_of_action", environment.getTotalTimeOfWorking());
                        resp.put("x", temp.getX());
                        resp.put("y", temp.getY());
                        reply.setContent(resp.toJSONString());

                        send(reply);
                    }
                    return true;
                }
                return false;
            }
        });
    }

    private void onDiscoveryCompleted() throws InterruptedException {
        SingletoneBuffer.getInstance().addLogToPrint(Log.log(this, "color discovery completed" + colorAgents));

        //sincronizarea initiala a scorurilor agentilor
        performSync();

        environment.print();
        System.out.println("\n\n");
        SingletoneBuffer.getInstance().printLogs();
        System.out.println("\nhere begins=================================================================");

        // actiuni
        addBehaviour(new TickerBehaviour(this,environment.getTimeToPerformAction()) {
            @Override
            protected void onTick() {
                if(getTickCount() >= environment.getTotalTimeOfWorking()/(float)environment.getTimeToPerformAction()) {
                    stop();
                    takeDown();
//                    for(AID aid: colorAgents){
//                    }
                }

                ACLMessage req;
                List<ACLMessage> requests = new ArrayList<>();
                do{
                    req = myAgent.receive(TemplatesForEnvironment.templateActions);
                    if(req!=null)
                        requests.add(req);
                }while(req!=null);

                for(ACLMessage request: requests){
                        Object obj = JSONValue.parse(request.getContent());
                        JSONObject jsonObject = (JSONObject) obj;
                        int x = environment.getColorAgentData((String) jsonObject.get("agent_color")).getX();
                        int y = environment.getColorAgentData((String) jsonObject.get("agent_color")).getY();
                        JSONObject respObj = new JSONObject();
                        String action = (String) jsonObject.get("action");

                        ACLMessage inform = request.createReply();
                        inform.setPerformative(ACLMessage.INFORM);
                        switch (action) {
                            case "pick": {
                                respObj.putAll(performPickUp(x, y, jsonObject));
                                break;
                            }
                            case "drop_tile": {
                                respObj.putAll(performDrop(x, y, jsonObject));
                                break;
                            }
                            case "move": {
                                respObj.putAll(understandMove(x,y,jsonObject));
                                break;
                            }
                            case "use_tile": {
                                respObj.putAll(understandUsage(x, y, jsonObject));
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
                    SingletoneBuffer.getInstance().addLogToPrint(Log.log(myAgent, " " + respObj.get("response")+" " + (String) jsonObject.get("agent_color") + " " + respObj.get("action") + " " + respObj.get("additional_info")));
                    inform.setPerformative(ACLMessage.INFORM);
                    inform.setContent(respObj.toJSONString());
                    myAgent.send(inform);
                }
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

        // here it receives the environment status requests
        addBehaviour(new SimpleBehaviour() {
            @Override
            public void action() {
                ACLMessage request = myAgent.receive(TemplatesForEnvironment.templateEnvStatus);
                if(request!=null) {
                    Object obj = JSONValue.parse(request.getContent());
                    JSONObject jsonObject = (JSONObject) obj;

                    if (Objects.equals((String) jsonObject.get("action"), "env-status")) {
                        ACLMessage response = request.createReply();
                        response.setPerformative(ACLMessage.INFORM);
                        JSONObject resp = new JSONObject();
                        resp.put("response", "refuse");
                        resp.put("action", "env-status");
                        resp.put("additional_info", " ");
                        try {
                            response.setContentObject(environment);
                            resp.put("response", "accept");
                            SingletoneBuffer.getInstance().addLogToPrint(Log.log(myAgent, " " + resp.get("response")+" " + (String) jsonObject.get("agent_color") + " " + resp.get("action")));
                        } catch (IOException e) {
                            response.setContent(resp.toJSONString());
                        }
                        myAgent.send(response);
                    }
                }
                else
                    block();
            }

            @Override
            public boolean done() {
                return false;
            }
        });
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
