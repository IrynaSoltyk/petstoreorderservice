package com.chtrembl.petstore.order.service;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ServiceBusMessageSenderImpl implements MessageSender {

    @Autowired
    private ServiceBusSenderClient serviceBusSenderClient;

    @Override
    public void send(final String message) {
        serviceBusSenderClient.sendMessage(new ServiceBusMessage(message));
    }
}
