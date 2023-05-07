package base;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.util.ExtendedProperties;
import jade.util.leap.Properties;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Launches a slave container and associated agents.
 */
public class SlaveContainerLauncher
{
	
	/**
	 * A reference to the launched container.
	 */
	AgentContainer secondaryContainer;
	
	/**
	 * Configures and launches a slave container.
	 */
	public void setupPlatform()
	{
		Properties secondaryProps = new ExtendedProperties();
		secondaryProps.setProperty(Profile.CONTAINER_NAME, "AmI-Slave"); // change if multiple slaves.
		
		// TODO: replace with actual IP of the current machine
		secondaryProps.setProperty(Profile.LOCAL_HOST, "localhost");
		secondaryProps.setProperty(Profile.LOCAL_PORT, "1100");
		secondaryProps.setProperty(Profile.PLATFORM_ID, "ami-agents");
		
		// TODO: replace with actual IP of the machine running the main container.
		secondaryProps.setProperty(Profile.MAIN_HOST, "localhost");
		secondaryProps.setProperty(Profile.MAIN_PORT, "1099");
		
		ProfileImpl secondaryProfile = new ProfileImpl(secondaryProps);
		secondaryContainer = Runtime.instance().createAgentContainer(secondaryProfile);
	}
	
	/**
	 * Starts the agents assigned to this container.
	 */
	public void startAgents(String[] args)
	{
		try
		{
			String filename = "tests/system__default.txt";

			String tempColor;
			String tempX;
			String tempY;
			String[] argsToPass=new String[4];
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

			for(int i=0;i<numberOfAgents;i++) {
				tempColor = input.next();
				colors.add(tempColor);
			}

			for(int i=0;i<numberOfAgents;i++){
				tempX = input.next();
				tempY = input.next();
				argsToPass[0]=tempX;
				argsToPass[1]=tempY;
				argsToPass[2]=colors.get(i);
				argsToPass[3]=String.valueOf(colors.size());

				AgentController colorAgent = secondaryContainer.createNewAgent(colors.get(i),
						ColorAgent.class.getName(), argsToPass);
				colorAgent.start();
			}
		} catch(StaleProxyException e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Launches a slave container.
	 * 
	 * @param args
	 *            - not used.
	 */
	public static void main(String[] args)
	{
		SlaveContainerLauncher launcher = new SlaveContainerLauncher();
		
		launcher.setupPlatform();
		launcher.startAgents(args);
	}
	
}
