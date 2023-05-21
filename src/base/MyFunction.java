package base;

import jade.lang.acl.MessageTemplate;

public interface MyFunction {
    void call(String additionalInfo, MessageTemplate template, int messageType, String protocol);
}
