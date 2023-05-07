package base;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.ParallelBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.proto.AchieveREInitiator;
import jade.proto.ContractNetInitiator;
import org.json.simple.JSONObject;

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
    protected void onDiscoveryCompleted() {
        Log.log(this, "color discovery completed" + colleagues);

        this.Pick(colorAgentData.getColor());
        this.DropTile();
        this.Move("North");
        this.Move("East");
        this.Move("South");
        this.Move("West");
        this.UseTile("North");

    }
    public void addServiceAgent(String serviceType, AID agent, int numOfColors)
    {
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

    protected void Pick(String tile_color) {
        // Fill the REQUEST message
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(environmentAgent);
        msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);

        // We want to receive a reply in 10 secs
        msg.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
        JSONObject obj = new JSONObject();
        obj.put("action", "pick");
        obj.put("color", tile_color);
        obj.put("Current x", colorAgentData.getX());
        obj.put("Current y", colorAgentData.getY());
        msg.setContent(obj.toJSONString());

        addBehaviour(new AchieveREInitiator(this, msg) {
            protected void handleInform(ACLMessage inform) {
                System.out.println("Agent " + inform.getSender().getName() + " successfully performed the requested action, got " + inform.getContent());
                colorAgentData.setTile(new Tile(colorAgentData.getColor(), 1));
            }
            protected void handleRefuse(ACLMessage refuse) {
                System.out.println("Agent " + refuse.getSender().getName() + " refused to perform the requested action");
            }
            protected void handleFailure(ACLMessage failure) {
                if (failure.getSender().equals(myAgent.getAMS())) {
                    // FAILURE notification from the JADE runtime: the receiver
                    // does not exist
                    System.out.println("Responder does not exist");
                }
                else {
                    System.out.println("Agent " + failure.getSender().getName() + " failed to perform the requested action");
                }
            }
            protected void handleAllResultNotifications(Vector notifications) {
                System.out.println("Timeout expired: missing responses");
            }
        } );

    }

    protected void DropTile() {
        // Fill the REQUEST message
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(environmentAgent);
        msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);

        // We want to receive a reply in 10 secs
//        msg.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
        JSONObject obj = new JSONObject();
        obj.put("action", "drop_tile");
        obj.put("color", colorAgentData.getTile().getColor());
        obj.put("Current x", colorAgentData.getX());
        obj.put("Current y", colorAgentData.getY());
        msg.setContent(obj.toJSONString());

        addBehaviour(new AchieveREInitiator(this, msg) {
            protected void handleInform(ACLMessage inform) {
                System.out.println("Agent " + inform.getSender().getName() + " successfully performed the requested action, got " + inform.getContent());

                //Droped the tile at the current location
                Tile newTile = new Tile("", 0);
                colorAgentData.setTile(newTile);
            }
            protected void handleRefuse(ACLMessage refuse) {
                System.out.println("Agent " + refuse.getSender().getName() + " refused to perform the requested action");
            }
            protected void handleFailure(ACLMessage failure) {
                if (failure.getSender().equals(myAgent.getAMS())) {
                    // FAILURE notification from the JADE runtime: the receiver
                    // does not exist
                    System.out.println("Responder does not exist");
                }
                else {
                    System.out.println("Agent " + failure.getSender().getName() + " failed to perform the requested action");
                }
            }
            protected void handleAllResultNotifications(Vector notifications) {
                System.out.println("Timeout expired: missing responses");
            }
        } );

    }

    protected void Move(String direction) {
        // Fill the REQUEST message
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(environmentAgent);
        msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);

        // We want to receive a reply in 10 secs
//        msg.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
        JSONObject obj = new JSONObject();
        obj.put("action", "move");
        obj.put("direction", direction);
        obj.put("Current x", colorAgentData.getX());
        obj.put("Current y", colorAgentData.getY());
        msg.setContent(obj.toJSONString());

        addBehaviour(new AchieveREInitiator(this, msg) {
            protected void handleInform(ACLMessage inform) {
                System.out.println("Agent " + inform.getSender().getName() + " successfully performed the requested action, got " + inform.getContent());
                if (direction.equals("North")) {
                    colorAgentData.setY(colorAgentData.getY() - 1);
                } else if (direction.equals("South")) {
                    colorAgentData.setY(colorAgentData.getY() + 1);
                } else if (direction.equals("East")) {
                    colorAgentData.setX(colorAgentData.getX() + 1);
                } else if (direction.equals("West")) {
                    colorAgentData.setX(colorAgentData.getX() - 1);
                }
            }
            protected void handleRefuse(ACLMessage refuse) {
                System.out.println("Agent " + refuse.getSender().getName() + " refused to perform the requested action");
            }
            protected void handleFailure(ACLMessage failure) {
                if (failure.getSender().equals(myAgent.getAMS())) {
                    // FAILURE notification from the JADE runtime: the receiver
                    // does not exist
                    System.out.println("Responder does not exist");
                }
                else {
                    System.out.println("Agent " + failure.getSender().getName() + " failed to perform the requested action");
                }
            }
            protected void handleAllResultNotifications(Vector notifications) {
                System.out.println("Timeout expired: missing responses");
            }
        } );

    }

    protected void UseTile(String direction) {
        // Fill the REQUEST message
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(environmentAgent);
        msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);

        // We want to receive a reply in 10 secs
//        msg.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
        JSONObject obj = new JSONObject();
        obj.put("action", "use_tile");
        obj.put("color", colorAgentData.getTile().getColor());
        obj.put("direction", direction);
        obj.put("Current x", colorAgentData.getX());
        obj.put("Current y", colorAgentData.getY());
        msg.setContent(obj.toJSONString());

        addBehaviour(new AchieveREInitiator(this, msg) {
            protected void handleInform(ACLMessage inform) {
                System.out.println("Agent " + inform.getSender().getName() + " successfully performed the requested action, got " + inform.getContent());

                //Used the tile
                Tile newTile = new Tile("", 0);
                colorAgentData.setTile(newTile);
            }
            protected void handleRefuse(ACLMessage refuse) {
                System.out.println("Agent " + refuse.getSender().getName() + " refused to perform the requested action");
            }
            protected void handleFailure(ACLMessage failure) {
                if (failure.getSender().equals(myAgent.getAMS())) {
                    // FAILURE notification from the JADE runtime: the receiver
                    // does not exist
                    System.out.println("Responder does not exist");
                }
                else {
                    System.out.println("Agent " + failure.getSender().getName() + " failed to perform the requested action");
                }
            }
            protected void handleAllResultNotifications(Vector notifications) {
                System.out.println("Timeout expired: missing responses");
            }
        } );

    }

    protected void TransferPoints(ColorAgent agent, int points) {
//    protected void TransferPoints(int points) {
        // Fill the REQUEST message
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(environmentAgent);
        msg.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);

        // We want to receive a reply in 10 secs
//        msg.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
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
                }
                else {
                    System.out.println("Agent " + failure.getSender().getName() + " failed to perform the requested action");
                }
            }
            protected void handleAllResultNotifications(Vector notifications) {
                System.out.println("Timeout expired: missing responses");
            }
        } );

    }

}
