package base;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

public class ColorAgent {//extends Agent{
//    private static final long serialVersionUID = 5088484951993491457L;
//
//    @Override
//    public void setup() {
//        DFAgentDescription dfd = new DFAgentDescription();
//        dfd.setName(getAID());
//
//        ServiceDescription sd = new ServiceDescription();
//        sd.setType(ServiceType.COLOR_AGENT);
//        sd.setName("ambient-wake-up-call");
//        dfd.addServices(sd);
//        try {
//            DFService.register(this, dfd);
//        } catch (FIPAException fe) {
//            fe.printStackTrace();
//        }
//    }
//
//        @Override
//    protected void takeDown()
//    {
//        // De-register from the yellow pages
//        try
//        {
//            DFService.deregister(this);
//        } catch(FIPAException fe)
//        {
//            fe.printStackTrace();
//        }
//    }
}
