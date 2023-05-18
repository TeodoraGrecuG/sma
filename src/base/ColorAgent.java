package base;;
import FIPA.FipaMessage;
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
                    break;
                }
            }
        }
        else{
            if(!Objects.equals(action, "env-status")){
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
                    SingletoneBuffer.getInstance().addLogToPrint(Log.log(myAgent, msg.getSender().getLocalName() + " said: " + msg.getContent()));
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
    protected void Pick(String tile_color, MessageTemplate template, int messageType, String protocol) {

        JSONObject obj = new JSONObject();
        obj.put("action", "pick");
        obj.put("additional_info", tile_color);
        obj.put("agent_color", colorAgentData.getColor());

        performCommunication(obj,template, messageType, protocol, false);
    }

    protected void DropTile(MessageTemplate template, int messageType, String protocol) {

        JSONObject obj = new JSONObject();
        obj.put("action", "drop_tile");
        obj.put("additional_info", colorAgentData.getTile().getColor());
        obj.put("agent_color", colorAgentData.getColor());

        performCommunication(obj,template, messageType, protocol, false);
    }

    protected void Move(String direction, MessageTemplate template, int messageType, String protocol) {
        JSONObject obj = new JSONObject();
        obj.put("action", "move");
        obj.put("additional_info", direction);
        obj.put("agent_color", colorAgentData.getColor());

        performCommunication(obj,template, messageType, protocol, false);
    }

    protected void UseTile(String direction, MessageTemplate template, int messageType, String protocol) {
        JSONObject obj = new JSONObject();
        obj.put("action", "use_tile");
        obj.put("additional_info", direction);
        obj.put("tile_color", colorAgentData.getTile().getColor());
        obj.put("agent_color", colorAgentData.getColor());

        performCommunication(obj,template, messageType, protocol, false);
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
    protected void onDiscoveryCompleted() throws InterruptedException {
        SingletoneBuffer.getInstance().addLogToPrint(Log.log(this, "color discovery completed" + colleagues));

        this.sync(templateSync, ACLMessage.REQUEST, Proctocols.SYNC);

        this.getEnvStatus(templateEnvStatus,ACLMessage.REQUEST, Proctocols.ENV_STATUS);
        this.listeningOtherAgents();

//        this.Pick(colorAgentData.getColor());


        //this.Move("North");
        this.Move("West",templateActions, ACLMessage.REQUEST, FIPANames.InteractionProtocol.FIPA_REQUEST);
        this.Pick("green",templateActions, ACLMessage.REQUEST, FIPANames.InteractionProtocol.FIPA_REQUEST);
        //Thread.sleep(300);
       // this.Move("South");
        this.Move("West",templateActions, ACLMessage.REQUEST, FIPANames.InteractionProtocol.FIPA_REQUEST);
        this.UseTile("West",templateActions, ACLMessage.REQUEST, FIPANames.InteractionProtocol.FIPA_REQUEST);
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
