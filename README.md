# TwitterkafkaConnect
This is the sample project which can be used to make sure we are able to connect Kafka and Java code.

This Project will have sample codes to :
1. Connect JAVA application to Twitter API to recieve tweets.
2. Will Push the code to Kafka Topics - twitter_tweets.
3. Will Consume the data from the Kafka topic.
4. Push the data into Bonsai search(Elastic Search).



# Important Details
## These detais will have common problems and solutions
###There are 4 major use cases of Kafka:

1. Data source to Kafka -- For this we can use Kafka Connect APIs
2. Kafka to Kafka -- For this we can use Kafka Streams API
3. Kafka to Sink -- For this we can use Kafka Connect Sink API
4. Kafka to App -- Same as kafka Connect 

# Kafka connect

Kafka Connect architecture is as below :

![image kafka connect](/images/KafkaConnectArc.PNG)

 
