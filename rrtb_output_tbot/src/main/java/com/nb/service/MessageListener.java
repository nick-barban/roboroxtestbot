package com.nb.service;

//@Singleton
//@JMSListener("connectionFactory")
public class MessageListener {
    /*private static final Logger LOG = LoggerFactory.getLogger(MessageListener.class);
    private final TelegramService telegramService;

    public MessageListener(TelegramService telegramService) {
        this.telegramService = telegramService;
    }

    @Queue("${app.queue.output}")
    public void receive(@MessageBody String message) {
        LOG.info("Received message from queue: {}", message);
        telegramService.sendMessage(message);
    }*/
} 