package base;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;

public class SingletoneBuffer {
    private static SingletoneBuffer instance;
    private static List<String> toPrintLogs;

    private static long beginTime;

    private SingletoneBuffer()
    {
        toPrintLogs = new ArrayList<>();
        Date date = new Date();
        beginTime = date.getTime();
    }

    //synchronized method to control simultaneous access
    synchronized public static SingletoneBuffer getInstance()
    {
        if (instance == null)
        {
            // if instance is null, initialize
            instance = new SingletoneBuffer();
        }
        return instance;
    }

    synchronized public static void addLogToPrint(String msg)
    {
        Date date = new Date();
        long local = date.getTime();
        toPrintLogs.add("["+(local-beginTime)/(float)1000+"]"+msg);
    }

    synchronized public static void printLogs()
    {
        for(String msg:toPrintLogs){
            System.out.println(msg);
        }

        toPrintLogs.clear();
    }

}
