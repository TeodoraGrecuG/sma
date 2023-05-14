package base;;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.SimpleBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.proto.AchieveREInitiator;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.util.ArrayList;
import java.util.List;
import java.util.*;

import java.util.*;

public class ColorAgent extends Agent{
    private static final long serialVersionUID = 5088484951993491459L;

    ColorAgentData colorAgentData;
    AID environmentAgent;
    List<AID> colleagues;

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

    protected void responseInterpret(Agent agent, ACLMessage msg)
    {
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
                    break;
                }
            }
        }
        SingletoneBuffer.getInstance().addLogToPrint(Log.log(agent, ": environment " + response+" " + action + " "+(String) jsonObject.get("additional_info")));
    }

    protected void performCommunication(JSONObject obj)
    {
        addBehaviour(new OneShotBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
                msg.setConversationId("inform-" + myAgent.getName()+new Date());
                msg.addReceiver(environmentAgent);
                msg.setContent(obj.toJSONString());
                myAgent.send(msg);
                SingletoneBuffer.getInstance().addLogToPrint(Log.log(myAgent, ": request to " + (String)obj.get("action") + " " + (String) obj.get("additional_info")));
            }
        });

        addBehaviour(new SimpleBehaviour() {
            int responses=0;
            @Override
            public void action() {
                ACLMessage msg = myAgent.blockingReceive();
                if (msg != null) {
                    responses++;
                    responseInterpret(myAgent, msg);
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
    protected void Pick(String tile_color) {

        JSONObject obj = new JSONObject();
        obj.put("action", "pick");
        obj.put("additional_info", tile_color);
        obj.put("agent_color", colorAgentData.getColor());

        performCommunication(obj);
    }

    protected void DropTile() {

        JSONObject obj = new JSONObject();
        obj.put("action", "drop_tile");
        obj.put("additional_info", colorAgentData.getTile().getColor());
        obj.put("agent_color", colorAgentData.getColor());

        performCommunication(obj);
    }

    protected void Move(String direction) {
        JSONObject obj = new JSONObject();
        obj.put("action", "move");
        obj.put("additional_info", direction);
        obj.put("agent_color", colorAgentData.getColor());

        performCommunication(obj);
    }

    protected void UseTile(String direction) {
        JSONObject obj = new JSONObject();
        obj.put("action", "use_tile");
        obj.put("additional_info", direction);
        obj.put("tile_color", colorAgentData.getTile().getColor());
        obj.put("agent_color", colorAgentData.getColor());

        performCommunication(obj);
    }

    protected void TransferPoints(ColorAgent agent, int points) {
//    protected void TransferPoints(int points) {
        // Fill the REQUEST message
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(environmentAgent);
        msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);

        // We want to receive a reply in 10 secs
        msg.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
        JSONObject obj = new JSONObject();
        obj.put("action", "transfer_points");
        obj.put("agent", agent.colorAgentData);
        obj.put("points", points);
        obj.put("Current x", colorAgentData.getX());
        obj.put("Current y", colorAgentData.getY());
        msg.setContent(obj.toJSONString());

        addBehaviour(new AchieveREInitiator(this, msg) {
            protected void handleInform(ACLMessage inform) {
                System.out.println("Agent " + inform.getSender().getName() + " successfully performed the requested action, got " + inform.getContent());
            }

            protected void handleRefuse(ACLMessage refuse) {
                System.out.println("Agent " + refuse.getSender().getName() + " refused to perform the requested action");
            }

            protected void handleFailure(ACLMessage failure) {
                if (failure.getSender().equals(myAgent.getAMS())) {
                    // FAILURE notification from the JADE runtime: the receiver
                    // does not exist
                    System.out.println("Responder does not exist");
                } else {
                    System.out.println("Agent " + failure.getSender().getName() + " failed to perform the requested action");
                }
            }

            protected void handleAllResultNotifications(Vector notifications) {
                //System.out.println("Timeout expired: missing responses");
            }
        });

    }


    protected void sync()
    {
        JSONObject obj = new JSONObject();
        obj.put("action", "sync");
        obj.put("additional_info", "");
        obj.put("agent_color", colorAgentData.getColor());

        performCommunication(obj);
    }
    protected void onDiscoveryCompleted() throws InterruptedException {
        SingletoneBuffer.getInstance().addLogToPrint(Log.log(this, "color discovery completed" + colleagues));

       this.sync();


//        this.Pick(colorAgentData.getColor());

        //this.Move("North");
        this.Move("West");
        this.Pick("green");
        //Thread.sleep(300);
       // this.Move("South");
        this.Move("West");
        this.UseTile("West");
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
