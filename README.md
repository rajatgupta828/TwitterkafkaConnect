# TwitterkafkaConnect


This is the sample project which can be used to make sure we are able to connect Kafka and Java code.

This Project will have sample codes to :
1. Connect JAVA application to Twitter API to recieve tweets.
2. Will Push the code to Kafka Topics - twitter_tweets.
3. Will Consume the data from the Kafka topic.
4. Push the data into Bonsai search(Elastic Search).

#Partition Count and Replication factor
'Try to Plan the config in the beginning itself.'

##More partition implies >>
  Better parallelism and better Throughput, Ability to run more consumers.
  1. Small Cluster (< 6 Brokers) == Number of partition = Number of Brokers X 2.
  2. Big Cluster (> 12 Brokers) ==  NUmber of partition = Number of Brokers X 1

# Replication factor
  Minimum 2, Usually 3, Maximum 4
  Never set to 1
  


# Important Details
## These detais will have common problems and solutions
###There are 4 major use cases of Kafka:

1. Data source to Kafka -- For this we can use Kafka Connect APIs
2. Kafka to Kafka -- For this we can use Kafka Streams API
3. Kafka to Sink -- For this we can use Kafka Connect Sink API
4. Kafka to App -- Same as kafka Connect 

# Kafka connect

Kafka Connect architecture is as below :

![image kafka connect](https://github.com/rajatgupta828/TwitterkafkaConnect/blob/master/images/KafkaConnectArc.png)


To show the example of kafka Connect API, we are using https://github.com/jcustenborder/kafka-connect-twitter

He has already written a connector, we will use this to read the data from twitter.
All Connectors are under kafka-connect Directory.
We need to Run the connect profile and will do the job for us :).

#Kafka Streams
Kafka Streams is a java library that provides easy data processing and transformation within Kafka.
![image kafka connect](https://github.com/rajatgupta828/TwitterkafkaConnect/blob/master/images/KafkaStreamArc.png)

Check the module : kafka-stream-filter-tweets for more information on Kafka Streams.

#Schema Registry
Schema registry should be a seperate component from kafka
The producers and Consumers should be able to talk to these schema registries.
The Schema registry must be able to reject bad data.
A common data format must be agreed upon.

## Check confluent Schema registry.
In this case kafka will send the Avro content to Kafka and will send the schema to Schema registry.
Consumer will recieve the schema from Schema Registry and recieve the data.



