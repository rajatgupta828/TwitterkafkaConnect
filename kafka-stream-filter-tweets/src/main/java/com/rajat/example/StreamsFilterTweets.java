package com.rajat.example;


import com.google.gson.JsonParser;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.common.metrics.Metrics;
import java.util.Properties;

public class StreamsFilterTweets {

    public static void main(String[] args) {

        // Create Properties
        Properties properties = new Properties();

        properties.setProperty(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG,"localhost:9092");
        properties.setProperty(StreamsConfig.APPLICATION_ID_CONFIG,"demo-kafka-streams");
        properties.setProperty(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.StringSerde.class.getName());
        properties.setProperty(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.StringSerde.class.getName());

        // Create a topology
        StreamsBuilder streamsBuilder = new StreamsBuilder();

        //input Topic
        KStream<String, String> inputTopic = streamsBuilder.stream("twitter_tweets");
        // Build a new Stream having a filter
        KStream<String, String> filteredStream = inputTopic.filter(
                // filter for tweets which has a user of over 10000 followers
                (k, jsonTweet) ->  extractUserFollowersInTweet(jsonTweet) > 10000
        );
        // Where will this stream go ?
        filteredStream.to("follower_tweets"); // Answer

        // Build a topology
        KafkaStreams kafkaStreams = new KafkaStreams(
                streamsBuilder.build(),  // Provide the stream that needs to be build
                properties               // Provide the properties
        );

        // Start our Streams Application
        kafkaStreams.cleanUp();
        kafkaStreams.start();
    }

    private static JsonParser jsonParser = new JsonParser();

    private static Integer extractUserFollowersInTweet(String tweetJson){
        try{
            return  jsonParser.parse(tweetJson)
                    .getAsJsonObject()
                    .get("user")
                    .getAsJsonObject()
                    .get("followers_count")
                    .getAsInt();
        }catch (NullPointerException e){
            return 0;
        }
    }

}
