import base.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.io.*;

public class Main {
    public static void main(String[] args){
        MainContainerLauncher main = new MainContainerLauncher();
        SlaveContainerLauncher slave = new SlaveContainerLauncher();

        main.setupPlatform();
        main.startAgents(args);

        slave.setupPlatform();
        slave.startAgents(args);
    }
}