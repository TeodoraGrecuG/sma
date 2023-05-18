package base;

import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class Templates {
    final static public MessageTemplate templateSync = MessageTemplate.and(
            MessageTemplate.MatchProtocol(Proctocols.SYNC),
            MessageTemplate.MatchPerformative(ACLMessage.INFORM));

    final static public MessageTemplate templateActions = MessageTemplate.and(
            MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),
            MessageTemplate.MatchPerformative(ACLMessage.INFORM));

    final static public MessageTemplate templateEnvStatus = MessageTemplate.and(
            MessageTemplate.MatchProtocol(Proctocols.ENV_STATUS),
            MessageTemplate.MatchPerformative(ACLMessage.INFORM));

    final static public MessageTemplate templateCommunicationBetweenAgents = MessageTemplate.and(
            MessageTemplate.MatchProtocol(Proctocols.AGENT_AGENT),
            MessageTemplate.MatchPerformative(ACLMessage.INFORM));
}
