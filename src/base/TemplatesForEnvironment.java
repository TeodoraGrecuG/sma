package base;

import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class TemplatesForEnvironment {
    final static MessageTemplate templateSync = MessageTemplate.and(
            MessageTemplate.MatchProtocol(Proctocols.SYNC),
            MessageTemplate.MatchPerformative(ACLMessage.REQUEST) );

    final static  MessageTemplate templateActions = MessageTemplate.and(
            MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST),
            MessageTemplate.MatchPerformative(ACLMessage.REQUEST));

    final static MessageTemplate templateEnvStatus = MessageTemplate.and(
            MessageTemplate.MatchProtocol(Proctocols.ENV_STATUS),
            MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
}
