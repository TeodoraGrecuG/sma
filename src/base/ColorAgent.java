package base;;
import FIPA.FipaMessage;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.proto.AchieveREInitiator;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.*;
import base.Templates.*;

import java.util.*;
import java.util.function.Function;

import static base.Templates.*;

public class ColorAgent extends Agent{
    private static final long serialVersionUID = 5088484951993491459L;

    ColorAgentData colorAgentData;
    AID environmentAgent;
    List<AID> colleagues;

    Environment envStatus = new Environment();

    Vector<MyFunction> functionPlan = new Vector<>();
    Vector<HashMap<String, Object>> parametersPlan=new Vector<>();

    int totalTimeOfAction;
    int timeToPerformAction;

    @Override
    public void setup() {
        Object[] args = getArguments();
        colorAgentData = new ColorAgentData(
                Integer.valueOf((String) args[0]),
                Integer.valueOf((String) args[1]),
                (String)args[2]
        );
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());

        ServiceDescription sd = new ServiceDescription();
        sd.setType(ServiceType.COLOR_AGENT);
        sd.setName((String)args[2]);
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        colleagues = new ArrayList<>();
        addBehaviour(new DiscoverEnvironmentAndColleaguesBehaviour(this, ParallelBehaviour.WHEN_ALL, Integer.valueOf((String)args[3])-1));
    }

    protected void handleContentObjectResponse(Agent myAgent, ACLMessage msg) throws UnreadableException {
        SingletoneBuffer.getInstance().addLogToPrint(Log.log(myAgent, "environment provided its status"));
        envStatus = (Environment) msg.getContentObject();

        try {
            makePlan();
        }
        catch(ClassNotFoundException e){
            System.out.println(e.toString());
        }
    }

    protected void responseInterpret(Agent agent, ACLMessage msg) throws UnreadableException {
        Object obj = JSONValue.parse(msg.getContent());
        JSONObject jsonObject = (JSONObject) obj;
        String response = (String) jsonObject.get("response");
        String action = (String) jsonObject.get("action");
        if(Objects.equals(response, "accept")) {
            switch (action) {
                case "pick up": {
                    String tileColor = (String) jsonObject.get("additional_info");
                    colorAgentData.setTile(tileColor, 1);
                    break;
                }
                case "move": {
                    String direction = (String) jsonObject.get("additional_info");
                    if (direction.equals("North")) {
                        colorAgentData.setY(colorAgentData.getY() - 1);
                    } else if (direction.equals("South")) {
                        colorAgentData.setY(colorAgentData.getY() + 1);
                    } else if (direction.equals("East")) {
                        colorAgentData.setX(colorAgentData.getX() + 1);
                    } else if (direction.equals("West")) {
                        colorAgentData.setX(colorAgentData.getX() - 1);
                    }
                    break;
                }
                case "drop":{
                    colorAgentData.setTile("", 0);
                    break;
                }
                case "use":{
                    colorAgentData.setTile("", 0);
                    colorAgentData.setScore(((Long) jsonObject.get("score")).intValue());
                    break;
                }
                case "sync":{
                    int score = ((Long) jsonObject.get("score")).intValue();
                    int xLocal = ((Long) jsonObject.get("x")).intValue();
                    int yLocal = ((Long) jsonObject.get("y")).intValue();
                    colorAgentData.setScore(score);
                    colorAgentData.setX(xLocal);
                    colorAgentData.setY(yLocal);
                    totalTimeOfAction = ((Long) jsonObject.get("total_time_of_action")).intValue();
                    timeToPerformAction = ((Long) jsonObject.get("time_to_perform_action")).intValue();
                    break;
                }
            }
        }
        else{
            if(!Objects.equals(action, "env-status")){
                parametersPlan.clear();
                functionPlan.clear();
                this.getEnvStatus(templateEnvStatus,ACLMessage.REQUEST, Proctocols.ENV_STATUS);
            }
        }
        SingletoneBuffer.getInstance().addLogToPrint(Log.log(agent, "environment " + response+" " + action + " "+(String) jsonObject.get("additional_info")));
    }

    protected void listeningOtherAgents(){
        addBehaviour(new SimpleBehaviour() {

            @Override
            public void action() {
                ACLMessage msg = myAgent.receive(templateCommunicationBetweenAgents);
                if (msg != null) {
                    //SingletoneBuffer.getInstance().addLogToPrint(Log.log(myAgent, msg.getSender().getLocalName() + " said: " + msg.getContent()));
                }
                else {
                    block();
                }
            }
            @Override
            public boolean done() {
                return false;
            }
        });
    }

    protected void sendActionToColleagues(String content)
    {
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.setProtocol(Proctocols.AGENT_AGENT);
        msg.setConversationId("inform-" + this.getLocalName()+new Date());
        msg.setContent(content);
        for(AID aid: colleagues){
            msg.addReceiver(aid);
            this.send(msg);
        }
    }

    protected void performCommunication(JSONObject obj, MessageTemplate template, int messageType, String protocol, boolean blocking)
    {
        addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = new ACLMessage(messageType);
                msg.setProtocol(protocol);
                msg.setConversationId("inform-" + myAgent.getName()+new Date());
                msg.addReceiver(environmentAgent);
                msg.setContent(obj.toJSONString());
                myAgent.send(msg);
                String desire = (String)obj.get("action") + " " + (String) obj.get("additional_info");
                sendActionToColleagues(desire);
                SingletoneBuffer.getInstance().addLogToPrint(Log.log(myAgent, "request to " + desire));
            }
        });

        addBehaviour(new SimpleBehaviour() {
            int responses=0;
            @Override
            public void action() {
                ACLMessage msg;
                //boolean lblocking=true;
                if (blocking)
                    msg=myAgent.blockingReceive(template);
                else
                    msg= myAgent.receive(template);
                if (msg != null) {
                    responses++;
                    try {
                        if(Objects.equals(msg.getProtocol(), Proctocols.ENV_STATUS))
                            handleContentObjectResponse(myAgent, msg);
                        else
                            responseInterpret(myAgent, msg);
                    } catch (UnreadableException e) {
                        ;//aici era o problema ca desi seteaza corect envStatus, arunca o exceptie de unreadableexc
                    }
                }
                else {
                    block();
                }
            }
            @Override
            public boolean done() {
                return responses > 0;
            }
        });
    }
    protected MyFunction Pick = (additionalInfo, template, messageType, protocol) -> {
        String tile_color = additionalInfo;

        JSONObject obj = new JSONObject();
        obj.put("action", "pick");
        obj.put("additional_info", tile_color);
        obj.put("agent_color", colorAgentData.getColor());

        performCommunication(obj,template, messageType, protocol, false);
    };

    protected MyFunction DropTile = (additionalInfo, template, messageType, protocol) -> {

        JSONObject obj = new JSONObject();
        obj.put("action", "drop_tile");
        obj.put("additional_info", colorAgentData.getTile().getColor());
        obj.put("agent_color", colorAgentData.getColor());

        performCommunication(obj,template, messageType, protocol, false);
    };

    protected MyFunction Move = (additionalInfo, template, messageType, protocol) -> {
        String direction = additionalInfo;

        JSONObject obj = new JSONObject();
        obj.put("action", "move");
        obj.put("additional_info", direction);
        obj.put("agent_color", colorAgentData.getColor());

        performCommunication(obj,template, messageType, protocol, false);
    };

    protected MyFunction UseTile = (additionalInfo, template, messageType, protocol) -> {
        String direction = additionalInfo;

        JSONObject obj = new JSONObject();
        obj.put("action", "use_tile");
        obj.put("additional_info", direction);
        obj.put("tile_color", colorAgentData.getTile().getColor());
        obj.put("agent_color", colorAgentData.getColor());

        performCommunication(obj,template, messageType, protocol, false);
    };

    protected void sync(MessageTemplate template,int messageType, String protocol)
    {
        JSONObject obj = new JSONObject();
        obj.put("action", "sync");
        obj.put("additional_info", "");
        obj.put("agent_color", colorAgentData.getColor());

        performCommunication(obj, template, messageType, protocol, true);
    }

    protected void getEnvStatus(MessageTemplate template,int messageType, String protocol)
    {
        JSONObject obj = new JSONObject();
        obj.put("action", "env-status");
        obj.put("additional_info", "");
        obj.put("agent_color", colorAgentData.getColor());

        performCommunication(obj, template, messageType, protocol, true);
    }

    private List<CoordinatesHelper> removeEmptyHolesCoordinates(List<CoordinatesHelper> holesCoordinates){
        List<CoordinatesHelper> holesCoordinatesToReturn = new ArrayList<>();

        for (CoordinatesHelper ch: holesCoordinates){
            if(((Hole)envStatus.getCell(ch.getX(), ch.getY()).getCellContents().get(0)).getDepth()>0)
                holesCoordinatesToReturn.add(ch);
        }

        return holesCoordinates;
    }

    /**
     * sets unreachable cells from environment to null
     * @param reasonCoordinates - coordinates of elements that make cells unreachable
     */
    private void removeUnreachableCells(List<CoordinatesHelper> reasonCoordinates){
        for(CoordinatesHelper ch: reasonCoordinates){
            envStatus.deleteCell(ch.getX(), ch.getY());
        }
    }

    private List<Integer> computeDistanceToAllNeededElements(CoordinatesHelper current, List<CoordinatesHelper> neededElements){
        List<Integer> distances = new ArrayList<>();
        int distance;

        for(CoordinatesHelper ch: neededElements){
            distance = Math.abs(current.getX()-ch.getX())+Math.abs(current.getY()-ch.getY());
            distances.add(distance);
        }

        return distances;
    }

    private CoordinatesHelper getClosestElement(List<Integer> distances, List<CoordinatesHelper> neededElements){
        if(distances.size()==0)
            return null;

        int minValue=distances.get(0);
        int position = 0;
        for(int i=0;i<distances.size();i++){
            if(distances.get(i)<minValue) {
                minValue = distances.get(i);
                position = i;
            }
        }

        return neededElements.remove(position);
    }

    CoordinatesHelper goToPos(CoordinatesHelper currentPos, CoordinatesHelper wantedPos){
        String possibleDirection="";
        boolean isHole=false;

        if(envStatus.getCell(wantedPos.getX(), wantedPos.getY())==null) {
            isHole = true;
        }

        // de verificat daca next e groapa ca sa ma opresc ;
        PathFinder pf = new PathFinder(
                envStatus.getHeight(),
                envStatus.getWidth(),
                currentPos,
                wantedPos
        );
        List<String> solution = new ArrayList<>();
        pf.solveMaze(envStatus.cells, solution);
        SingletoneBuffer.addLogToPrint(Log.log(this, solution));

//        if(wantedPos==null)
//            return currentPos;

        if(solution.size()>0) {
            int i = 0;
            for (i = 0; i < solution.size() - 1; i++) {
                functionPlan.add(Move);
                int finalI = i;
                parametersPlan.add(new HashMap<>() {{
                    put("additional_info", solution.get(finalI)); // aici sa pun in additional info directia
                    put("template", templateActions);
                    put("messageType", ACLMessage.REQUEST);
                    put("protocol", FIPANames.InteractionProtocol.FIPA_REQUEST);
                }});
            }
            int finalI1 = i;
            if (isHole) {
                // usetile
                functionPlan.add(UseTile);
                if(Objects.equals(solution.get(finalI1), "West"))
                    wantedPos.setX(wantedPos.getX()+1);
                if(Objects.equals(solution.get(finalI1), "East"))
                    wantedPos.setX(wantedPos.getX()-1);
                if(Objects.equals(solution.get(finalI1), "North"))
                    wantedPos.setY(wantedPos.getY()+1);
                if(Objects.equals(solution.get(finalI1), "South"))
                    wantedPos.setY(wantedPos.getY()-1);
            } else {
                functionPlan.add(Move);
            }

            parametersPlan.add(new HashMap<>() {{
                put("additional_info", solution.get(finalI1)); // aici sa pun in additional info directia
                put("template", templateActions);
                put("messageType", ACLMessage.REQUEST);
                put("protocol", FIPANames.InteractionProtocol.FIPA_REQUEST);
            }});
            return wantedPos;
        }
        else
            return currentPos;
    }

    void makePlan() throws ClassNotFoundException {
        CoordinatesHelper currentPos = new CoordinatesHelper(colorAgentData.getX(), colorAgentData.getY());

        // obtin toate elementele de care am nevoie
        List<CoordinatesHelper> myColorTiles = envStatus.getTilesCoordinatesByColor(colorAgentData.getColor());
        List<CoordinatesHelper> myColorHoles = envStatus.getHolesCoordinatesByColor(colorAgentData.getColor());
        List<CoordinatesHelper> obstacles = envStatus.getObstacleCoordinates();
        List<CoordinatesHelper> allHoles = envStatus.getAllHoles();

        // elimin din liste gropile cu adancimi nule
        myColorHoles = removeEmptyHolesCoordinates(myColorHoles);
        allHoles = removeEmptyHolesCoordinates(allHoles);

        // elimin celulele prin care nu pot merge - le setez null
        removeUnreachableCells(allHoles);
        removeUnreachableCells(obstacles);

        while(myColorTiles.size()>0) {
            // calculez distantele de la agent la toate tile-urile care il intereseaza
            List<Integer> distances = computeDistanceToAllNeededElements(currentPos, myColorTiles);

            // o alege pe cea mai mica si merge la acel tile
            CoordinatesHelper whereToGo = getClosestElement(distances, myColorTiles);
            currentPos = goToPos(currentPos, whereToGo);

            // pick
            functionPlan.add(Pick);
            parametersPlan.add(new HashMap<>() {{
                put("additional_info", colorAgentData.getColor());
                put("template", templateActions);
                put("messageType", ACLMessage.REQUEST);
                put("protocol", FIPANames.InteractionProtocol.FIPA_REQUEST);
            }});

            // calculez distanta de la tile la groapa
            distances = computeDistanceToAllNeededElements(currentPos, myColorHoles);

            // o aleg pe cea mai mica si merg acolo si folosesc dala
            whereToGo = getClosestElement(distances, myColorHoles);
            currentPos = goToPos(currentPos, whereToGo);

        }

    }
    protected void onDiscoveryCompleted() throws InterruptedException {
        SingletoneBuffer.getInstance().addLogToPrint(Log.log(this, "color discovery completed" + colleagues));

        this.sync(templateSync, ACLMessage.REQUEST, Proctocols.SYNC);

        this.getEnvStatus(templateEnvStatus,ACLMessage.REQUEST, Proctocols.ENV_STATUS);

        this.listeningOtherAgents();

        addBehaviour(new TickerBehaviour(this, Times.timeToPerformAction) {
            @Override
            protected void onTick() {
                if(getTickCount() >= Times.totalTimeOfWorking/(float)Times.timeToPerformAction) {
                    stop();
                    takeDown();
                }

                if(functionPlan.size()>0) {
                    MyFunction f = functionPlan.remove(0);
                    HashMap<String, Object> param = parametersPlan.remove(0);
                    f.call(
                            (String) param.get("additional_info"),
                            (MessageTemplate) param.get("template"),
                            (Integer) param.get("messageType"),
                            (String) param.get("protocol")
                    );
                }
            }
        });



//        this.Pick(colorAgentData.getColor());


        //this.Move("North");
//        this.Move("West",templateActions, ACLMessage.REQUEST, FIPANames.InteractionProtocol.FIPA_REQUEST);
//        this.Pick("green",templateActions, ACLMessage.REQUEST, FIPANames.InteractionProtocol.FIPA_REQUEST);
//        //Thread.sleep(300);
//       // this.Move("South");
//        this.Move("West",templateActions, ACLMessage.REQUEST, FIPANames.InteractionProtocol.FIPA_REQUEST);
//        this.UseTile("West",templateActions, ACLMessage.REQUEST, FIPANames.InteractionProtocol.FIPA_REQUEST);
        //this.Pick("green");
        //this.DropTile();
        //Thread.sleep(300);
    }
    public void addServiceAgent(String serviceType, AID agent, int numOfColors) throws InterruptedException {
        if(serviceType.equals(ServiceType.ENV_AGENT))
            environmentAgent = agent;

        if(serviceType.equals(ServiceType.COLOR_AGENT))
        {
            colleagues.add(agent);
        }

        if(environmentAgent != null && colleagues.size()>=numOfColors)
            onDiscoveryCompleted();
    }

    @Override
    protected void takeDown()
    {
        // De-register from the yellow pages
        try
        {
            DFService.deregister(this);
        } catch(FIPAException fe)
        {
            fe.printStackTrace();
        }
    }
}
