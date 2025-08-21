package kafka.examples;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class Consumer implements Runnable {
    private final KafkaConsumer<Integer, String> consumer;
    private final String topic;
    private volatile boolean running = true;

    public Consumer(String topic) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaProperties.KAFKA_SERVER_URL + ":" + KafkaProperties.KAFKA_SERVER_PORT);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "DemoConsumer");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "30000");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.IntegerDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");

        consumer = new KafkaConsumer<>(props);
        this.topic = topic;
    }

    @Override
    public void run() {
        try {
            consumer.subscribe(Collections.singletonList(this.topic));
            while (running) {
                ConsumerRecords<Integer, String> records = consumer.poll(Duration.ofMillis(1000));
                for (ConsumerRecord<Integer, String> record : records) {
                    System.out.println("Received message: (" + record.key() + ", " + record.value() + ") at offset " + record.offset());
                }
            }
        } catch (Exception e) {
            System.out.println("Consumer encerrado: " + e.getMessage());
        } finally {
            consumer.close();
        }
    }

    public void shutdown() {
        running = false;
        consumer.wakeup(); // interrompe o poll
    }

    public static void main(String[] args) {
        Consumer consumerThread = new Consumer("test");
        Thread t = new Thread(consumerThread);
        t.start();

        Runtime.getRuntime().addShutdownHook(new Thread(consumerThread::shutdown));
    }
}
