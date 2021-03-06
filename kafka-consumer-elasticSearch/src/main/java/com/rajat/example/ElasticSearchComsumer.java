package com.rajat.example;

import com.google.gson.JsonParser;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.protocol.types.Field;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.*;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

public class ElasticSearchComsumer {

    public static void main(String[] args) throws IOException {

        Logger logger = LoggerFactory.getLogger(ElasticSearchComsumer.class.getName());

        RestHighLevelClient clients = createClient();

       //Create a consumer
        KafkaConsumer<String, String> myConsumer = createConsumer("twitter_tweets");

        //Create a bulk request :
        BulkRequest bulkRequest = new BulkRequest();
        // Read/Poll the data
        while(true){
            ConsumerRecords<String,String> recordsRead =  myConsumer.poll(Duration.ofMillis(100));

            Integer recordsReadCount = recordsRead.count();

            logger.info("Records : " + recordsReadCount + " read");
            for(ConsumerRecord<String, String> records : recordsRead){
                // Insert data into elastic search
                String jsonString = records.value();

                // To make the consumer Idempotent, we have 2 strategies which we re going to define below:

                // Strategy 1: Kafka Generic ID
                //String id1 = records.topic() + "_" + records.partition() + "_" + records.offset();

                //Strategy 2 - Use the incoming ID like in case of twitter we can use a new ID from the payload
                try{
                    String id1 = getIdFromData(records.value());
                    IndexRequest indexRequest = new IndexRequest("twitter","_doc",
                            id1).source(jsonString, XContentType.JSON);
                    bulkRequest.add(indexRequest);
                }catch (Exception e){
                    e.printStackTrace();
                    logger.warn("Skipping Bad Data : " + records.value());
                }
            }

            if (recordsReadCount > 0){
                BulkResponse bulkItemResponses = clients.bulk(bulkRequest, RequestOptions.DEFAULT);
                logger.info("Committing the records...");
                //Performing the commits
                myConsumer.commitSync();

                logger.info("Commited successfully");

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {

                    e.printStackTrace();
                }
            }
        }
        // Close the client
        //clients.close();
    }

    public static KafkaConsumer<String, String> createConsumer(String topic){
        // Create the properties
        Properties properties = new Properties();

        // Create the config
        String bootStrapServer = "127.0.0.1:9092";
        String groupId = "kafkaElasticSearchConsumer";

        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,bootStrapServer);
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG,groupId);
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"earliest");
        properties.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        properties.setProperty(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "200");



        //Create the consumer
        KafkaConsumer<String, String> myConsumer = new KafkaConsumer<String, String>(properties);


        //Subscribe the consumer
        myConsumer.subscribe(Arrays.asList(topic));

        return myConsumer;
    }

    public static RestHighLevelClient createClient(){

        String hostName = "twitter-tweets-4374331603.us-east-1.bonsaisearch.net";
        String userName = "onh3wy3hzn";
        String password = "9q24g6upp0";

        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(userName, password));

        RestClientBuilder builder = RestClient.builder(
                new HttpHost(hostName,443,"https")
        ).setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
            @Override
            public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpAsyncClientBuilder) {
                return httpAsyncClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
            }
        });

        RestHighLevelClient client = new RestHighLevelClient(builder);
        return client;
    }

    private static JsonParser jsonParser = new JsonParser();

    private static String getIdFromData(String tweetJson){
        return  jsonParser.parse(tweetJson).getAsJsonObject().get("id_str").getAsString();
    }
}

