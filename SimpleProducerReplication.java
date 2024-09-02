package br.com.ambientelivre;

import java.util.Collections;
//import util.properties packages
import java.util.Properties;

//import simple producer packages
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.Callback;
//import KafkaProducer packages
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;

//import ProducerRecord packages
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

//Create java class named “SimpleProducer”
public class SimpleProducerReplication {

	public static void main(String[] args) throws Exception {

		// Check arguments length value
		// if(args.length == 0){
		// System.out.println("Enter topic name");
		// return;
		// }

		// Assign topicName to string variable
		// String topicName = "topic-kafka-simple-3all3"; //args[0].toString();

		Properties config = new Properties();
		config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9091,localhost:9092,localhost:9093");

		try (AdminClient adminClient = AdminClient.create(config)) {
			// Defina o nome do tópico, o número de partições e o fator de replicação
			String topicName = "meu-topico";
			int numPartitions = 3;
			short replicationFactor = 3;

			// Criação do tópico com as configurações acima
			NewTopic newTopic = new NewTopic(topicName, numPartitions, replicationFactor);
			adminClient.createTopics(Collections.singletonList(newTopic)).all().get();

			System.out.println("Tópico criado com sucesso com fator de replicação " + replicationFactor);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// create instance for properties to access producer configs
		Properties props = new Properties();

		// Assign localhost id
		props.put("bootstrap.servers", "localhost:9091,localhost:9092,localhost:9093");

		// Set acknowledgements for producer requests.
		props.put("acks", "all");

		//// Número de replicas.
		// props.put("topic.replication-factor", 3);

		// If the request fails, the producer can automatically retry,
		props.put("retries", 0);

		// Specify buffer size in config
		props.put("batch.size", 16384);

		// Reduce the no of requests less than 0
		props.put("linger.ms", 1);

		// The buffer.memory controls the total amount of memory available to the
		// producer for buffering.
		props.put("buffer.memory", 33554432);

		props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");

		props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

		// Criação do produtor Kafka
		KafkaProducer<String, String> producer = new KafkaProducer<>(props);

		// Mensagem simples a ser enviada
		String topic = "meu-topico";
		String key = "minha-chave";
		String value = "meu-valor";

		// Criação de um registro (ProducerRecord) e envio da mensagem
		ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);

		for (int i = 0; i < 1000; i++) {

			// Envia a mensagem de forma assíncrona
			producer.send(record, new Callback() {
				@Override
				public void onCompletion(RecordMetadata metadata, Exception exception) {
					if (exception == null) {
						System.out.printf("Mensagem enviada com sucesso para o tópico %s - Partição: %d - Offset: %d%n",
								metadata.topic(), metadata.partition(), metadata.offset());
					} else {
						exception.printStackTrace();
					}
				}
			});

			// Fecha o produtor para liberar os recursos

			System.out.println("Message sent successfully");
		}

		producer.close();
	}
}
