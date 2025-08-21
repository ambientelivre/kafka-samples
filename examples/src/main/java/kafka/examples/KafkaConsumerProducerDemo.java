package kafka.examples;

public class KafkaConsumerProducerDemo {
    public static void main(String[] args) {
        boolean isAsync = args.length == 0 || !args[0].trim().equalsIgnoreCase("sync");

        // Inicia o Producer em thread separada
        Producer producer = new Producer(KafkaProperties.TOPIC, isAsync);
        Thread producerThread = new Thread(producer);
        producerThread.start();

        // Inicia o Consumer em thread separada
        Consumer consumer = new Consumer(KafkaProperties.TOPIC);
        Thread consumerThread = new Thread(consumer);
        consumerThread.start();

        // Adiciona shutdown hook para encerrar os dois
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Encerrando producer e consumer...");
            producer.shutdown();
            consumer.shutdown();
        }));
    }
}
