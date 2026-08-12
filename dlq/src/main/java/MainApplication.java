public class MainApplication {

    private static final String BOOTSTRAP_SERVERS = "localhost:9092";

    public static void main(String[] args) throws InterruptedException {
        // 1. Inicia o Produtor e envia um caso de sucesso e um caso de erro
        OrderProducer producer = new OrderProducer(BOOTSTRAP_SERVERS);
        
        System.out.println("--- Enviando mensagens de teste ---");
        producer.sendOrder(OrderConsumer.MAIN_TOPIC, "ORD-1", "Pedido Sucesso - Item Notebook");
        producer.sendOrder(OrderConsumer.MAIN_TOPIC, "ORD-2", "Pedido Com ERRO - Item Celular");

        // Dá um tempo para garantir a gravação no broker
        Thread.sleep(1000);

        // 2. Inicia o Consumidor
        OrderConsumer consumer = new OrderConsumer(BOOTSTRAP_SERVERS, "grupo-treinamento-java");
        consumer.startConsuming();
    }
}
