import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

public class OrderProducer {

    private final KafkaProducer<String, String> producer;

    public OrderProducer(String bootstrapServers) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        this.producer = new KafkaProducer<>(props);
    }

    public void sendOrder(String topic, String key, String value) {
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);
        
        producer.send(record, (metadata, exception) -> {
            if (exception == null) {
                System.out.println(" [Producer] Mensagem enviada -> Tópico: " + metadata.topic() 
                        + " | Partição: " + metadata.partition() 
                        + " | Offset: " + metadata.offset());
            } else {
                System.err.println(" [Producer] Erro ao enviar mensagem: " + exception.getMessage());
            }
        });
    }

    public void close() {
        this.producer.close();
    }
}
