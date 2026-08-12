import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class OrderConsumer {

    public static final String MAIN_TOPIC = "order-topic";
    public static final String RETRY_TOPIC = "order-topic-retry";
    public static final String DLQ_TOPIC = "order-topic-dlq";
    
    private static final int MAX_RETRIES = 3;

    private final KafkaConsumer<String, String> consumer;
    private final KafkaProducer<String, String> producer;

    public OrderConsumer(String bootstrapServers, String groupId) {
        // Configuração do Consumidor
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // Desativamos o commit automático para garantir controle manual no fluxo de erro
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        this.consumer = new KafkaConsumer<>(consumerProps);

        // Configuração do Produtor (usado para mover a mensagem de fila se falhar)
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        this.producer = new KafkaProducer<>(producerProps);
    }

    public void startConsuming() {
        // Inscreve o consumidor no Tópico Principal e no de Retry
        this.consumer.subscribe(Collections.singletonList(MAIN_TOPIC));

        System.out.println(" [Consumer] Ouvindo tópico: " + MAIN_TOPIC);

        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));

                for (ConsumerRecord<String, String> record : records) {
                    processRecord(record);
                }

                // Sincroniza o offset manualmente após processar ou reencaminhar o lote
                consumer.commitSync();
            }
        } finally {
            consumer.close();
            producer.close();
        }
    }

    private void processRecord(ConsumerRecord<String, String> record) {
        int retryCount = getRetryCountFromHeader(record);

        System.out.println("\n [Consumer] Mensagem Recebida -> Key: " + record.key() 
                + " | Valor: " + record.value() 
                + " | Tentativa Atual: " + retryCount);

        try {
            // Lógica de negócio que pode gerar falha
            executeBusinessLogic(record.value());
            System.out.println(" [Success] Processado com sucesso!");

        } catch (Exception e) {
            System.err.println(" [Error] Falha no processamento: " + e.getMessage());

            if (retryCount < MAX_RETRIES) {
                System.out.println(" [Retry] Enviando para o tópico de RETRY...");
                forwardToTopic(record, RETRY_TOPIC, retryCount + 1);
            } else {
                System.err.println(" [DLQ] Limite máximo de tentativas atingido! Enviando para DLQ...");
                forwardToTopic(record, DLQ_TOPIC, retryCount);
            }
        }
    }

    private void executeBusinessLogic(String payload) throws Exception {
        // Simulação de erro condicional para fins didáticos
        if (payload.contains("ERRO")) {
            throw new RuntimeException("Falha na integração com banco de dados/serviço externo.");
        }
    }

    private void forwardToTopic(ConsumerRecord<String, String> originalRecord, String targetTopic, int nextRetryCount) {
        ProducerRecord<String, String> newRecord = new ProducerRecord<>(
                targetTopic, 
                originalRecord.key(), 
                originalRecord.value()
        );

        // Preserva ou atualiza o cabeçalho 'x-retry-count'
        newRecord.headers().add(new RecordHeader("x-retry-count", String.valueOf(nextRetryCount).getBytes(StandardCharsets.UTF_8)));

        producer.send(newRecord, (metadata, exception) -> {
            if (exception == null) {
                System.out.println(" [Redirect] Mensagem redirecionada com sucesso para " + targetTopic);
            } else {
                System.err.println(" [Redirect Error] Falha ao mover mensagem para " + targetTopic + ": " + exception.getMessage());
            }
        });
    }

    private int getRetryCountFromHeader(ConsumerRecord<String, String> record) {
        Header header = record.headers().lastHeader("x-retry-count");
        if (header != null) {
            return Integer.parseInt(new String(header.value(), StandardCharsets.UTF_8));
        }
        return 1; // Primeira tentativa por padrão
    }
}
