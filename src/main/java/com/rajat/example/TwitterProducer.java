package com.rajat.example;

import com.google.common.collect.Lists;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.Hosts;
import com.twitter.hbc.core.HttpHosts;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.protocol.types.Field;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class TwitterProducer {

    Logger logger = LoggerFactory.getLogger(TwitterProducer.class.getName());

    // Constructor
    public TwitterProducer(){
    }

    public static void main(String[] args) {
        new TwitterProducer().run();
    }

    public void run(){

        logger.info("Setting up...");
        //Set up your blocking queues: Be sure to size these properly based on expected TPS of your stream
        BlockingQueue<String> msgQueue = new LinkedBlockingQueue<String>(1000);

        // Create a twitter Client
        Client client = createTwitterClient(msgQueue);
        client.connect();

        logger.info("Creating the producer now....");
        // Create a Kafka Producer
        KafkaProducer<String, String> twitterProducer = CreateKafkaProducer();

        // Create a shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() ->{
            logger.info("Caught Shutdown Hook...");
            twitterProducer.close();
            client.stop();
            logger.info("Closed the producer to no in-memory messages are left...");
            logger.info("Switching off the client that reads from Kafka twitter...");
        }));

        // Loop to send tweets to Kafka
        // on a different thread, or multiple different threads....
        while (!client.isDone()) {
            String msg = null;
            try {
                msg = msgQueue.poll(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
                client.stop();
            }
            if(msg != null){
                logger.info(msg);
                //Create a producer record
                ProducerRecord<String, String> record = new ProducerRecord<String, String>
                        ("twitter_tweets", null, msg);
                twitterProducer.send(record, new Callback() {
                    @Override
                    public void onCompletion(RecordMetadata recordMetadata, Exception e) {
                        if(e != null){
                            logger.error("error found" ,  e);
                        }
                    }
                });
            }

        }
        logger.info("End of application");
    }
    // Method to create the twitter Client
    public Client createTwitterClient(BlockingQueue msgQueue){

        String consumerKey = "XZCkg3XgwQnEUeDuoDpJmOqON";
        String consumerSecret = "KM4KSJdla4m9Kr7lp6lxrEOVlxnt0341KwNwM7fkrsg6s3I8kL";
        String token = "4184816793-cm8jmGJlQ7Op9Sjwum06Z8pIgqb1x6mWeMHik6m";
        String tokenSecret = "VDunybLgWTVVvlxirMoSWh5FZ9G7aonrNilENpWDIILGT";

        logger.info("Creating the client with provided information....");

        /* Declare the host you want to connect to, the endpoint, and authentication (basic auth or oauth) */
        Hosts hosebirdHosts = new HttpHosts(Constants.STREAM_HOST);
        StatusesFilterEndpoint hosebirdEndpoint = new StatusesFilterEndpoint();

        // Optional: set up some followings and track terms
        List<String> terms = Lists.newArrayList("ssr");
        hosebirdEndpoint.trackTerms(terms);

        // These secrets should be read from a config file
        Authentication hosebirdAuth = new OAuth1(consumerKey, consumerSecret,token, tokenSecret);

        ClientBuilder builder = new ClientBuilder()
                .name("Hosebird-Client-01")                              // optional: mainly for the logs
                .hosts(hosebirdHosts)
                .authentication(hosebirdAuth)
                .endpoint(hosebirdEndpoint)
                .processor(new StringDelimitedProcessor(msgQueue));

        Client hosebirdClient = builder.build();
        return hosebirdClient;
    }

    public KafkaProducer<String, String> CreateKafkaProducer(){
        // Create Producer Properties
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,"127.0.0.1:9092");
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // Create Safe producer settings
        properties.setProperty(ProducerConfig.ACKS_CONFIG,"all");
        properties.setProperty(ProducerConfig.RETRIES_CONFIG,Integer.toString(Integer.MAX_VALUE));
        properties.setProperty(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION,"5");// Since Kafka is used greater than 1.1 , if less use 1

        // Create the producer
        KafkaProducer<String, String> producer = new KafkaProducer<String, String>(properties);

        return producer;
    }

}
