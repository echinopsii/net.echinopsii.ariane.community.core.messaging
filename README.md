ariane.community.messaging
==========================

Ariane Community Core Framework Messaging Module

# Purpose

This module has been created to avoid JMS over complexity and provide efficient but simple message definition and 
messages flow patterns API to fit Ariane Framework needs. 

The first need was the ability to create [a testing environment simulating trade applications workflow] (https://github.com/echinopsii/net.echinopsii.ariane.scenarios)
through RabbitMQ first and then [map](https://slack-files.com/T04JMETB8-F1LTAMFGB-4194f337a9) this environment thanks Ariane Framework and the [Ariane RabbitMQ plugin] (https://github.com/echinopsii/net.echinopsii.ariane.community.plugin.rabbitmq).

As we wanted being able to reuse same messaging API through other messaging middleware we wanted to keep this API as simple as possible. 
Today we also use this API with [NATS] (http://nats.io/) and Ariane is using it a lot as described [here](http://nats.io/blog/ariane-mapping-microservice-with-nats/).

Finally we wanted to use actor model provided by [Akka](http://akka.io/) to define business logic call back through this API calls - and so being able to reuse Akka powerful tooling like routers.

# Some technical inputs

## Configuration and connection to the messaging broker

The first step to use this module is to configure it depending the broker you need. Bellow are java properties files which are used to initialize and then connect our testing unit to the brokers:

+ NATS
```
mom_cli.nats.connection_name=TestConnection
mom_cli.impl=net.echinopsii.ariane.community.messaging.nats.Client
mom_host.fqdn=localhost
mom_host.port=4222
mom_host.user=ariane
mom_host.password=password
```

+ RabbitMQ 
```
mom_cli.impl=net.echinopsii.ariane.community.messaging.rabbitmq.Client
mom_host.fqdn=localhost
mom_host.port=5672
mom_host.user=ariane
mom_host.password=password
mom_host.rbq_vhost=/ariane
```

Then you're ready to create the API client which will connect to the message broker like this : 

```
import net.echinopsii.ariane.community.messaging.common.MomClientFactory
import net.echinopsii.ariane.community.messaging.api.MomClient
...

Properties props = new Properties();
props.load(ClientTest.class.getResourceAsStream("/my_broker_config.properties"));
client = MomClientFactory.make(props.getProperty(MomClient.MOM_CLI));
try {
    client.init(props);
} catch (Exception e) {
    e.printStackTrace();
    System.err.println("Connection failture ! ");
    client = null;
}

...

client.close();
```

## Simple messages

This API defines messages as simple Java Map where you can define several fields. 
Depending on the broker you are using, you can define technical JMS like fields or specific broker fields to define behavior of your message.
As well as in JMS or RabbitMQ messages definition you can also define a body field. And for sure you can also define any kind of field you need for you app. 
The API implementation will then do the plumbing for you.

Bellow are some pre defined fields you will find in the API :

| Field name         | Field description                                | NATS support | RabbitMQ support |
| ------------------ | ------------------------------------------------ | ------------ | ---------------- |
| MSG_APPLICATION_ID | Define message application id source             | OK           | OK               |
| MSG_MESSAGE_ID     | Define message ID                                | OK           | OK               |
| MSG_CORRELATION_ID | Define message correlation ID                    | OK           | OK               |
| MSG_DELIVERY_MODE  | Define message delivery mode (persistent or not) | IGNORED      | OK               |
| MSG_EXPIRATION     | Define message expiration                        | IGNORED      | OK               |
| MSG_PRIORITY       | Define message priority                          | IGNORED      | OK               |
| MSG_REPLY_TO       | Define message reply destination                 | OK           | OK               |
| MSG_TIMESTAMP      | Define message timestamp                         | OK           | OK               |
| MSG_TYPE           | Define message type                              | IGNORED      | OK               |
| MSG_RC             | Define return code on the reply                  | OK           | OK               |
| MSG_ERR            | Define message error if any                      | OK           | OK               |
| MSG_BODY           | Define the message body (IE: you app data)       | OK           | OK               |

Depending on your needs you can also reuse some broker specific message fields 

## Simple flow patterns

### Fire and forget

Through this pattern you just want to send a request to a server but in this case you don't need the reply.

#### Server side

On the server side you first need to create your business logic and then create a service which will listen a request queue and forward messages to your business callback. 

```
import net.echinopsii.ariane.community.messaging.common.MomClientFactory
import net.echinopsii.ariane.community.messaging.api.MomClient
import net.echinopsii.ariane.community.messaging.api.AppMsgWorker
...

final static String sendedMsgBody = "Hello !";

class TestMsgWorker implements AppMsgWorker {

    boolean OK = false;

    @Override
    public Map<String, Object> apply(Map<String, Object> message) {
        String recvMsgBody = new String((byte [])message.get(MomMsgTranslator.MSG_BODY));
        if (recvMsgBody.equals(sendedMsgBody))
            OK = true;
        // no reply needed : return null
        return null;
    }

    public boolean isOK() {
        return OK;
    }
}

// my application business logic 
TestMsgWorker test = new TestMsgWorker();

// my service listening to "FAF_QUEUE"
client.getServiceFactory().requestService("FAF_QUEUE", test);
```

*Behind the scene:*
a message consumer on "FAF_QUEUE" is created as well as a akka router and several akka routees (5 by default but can be overrided 
in the configuration with mom_cli.nb_routees_per_service property field).
The message consumer will forward the message as is to the akka router which will then forward the message to one of its routees (round robin). 
The routee is in charge of decoding the message from its technical definition (NATS or RabbitMQ) to the Messaging API message definition (a map !) and 
finally forward it to the AppMsgWorker through its apply method. 

#### Client side 
 
On the client side you just need to define a message and send your request in fire and forget fashion.
 
```
import net.echinopsii.ariane.community.messaging.common.MomClientFactory
import net.echinopsii.ariane.community.messaging.api.MomClient
...

// create the message 
Map<String, Object> message = new HashMap<String, Object>();
message.put(MomMsgTranslator.MSG_BODY, sendedMsgBody);

// send the request
client.createRequestExecutor().FAF(message, "FAF_QUEUE");
```
 
### RPC

### Feeder/Subscriber

# TODO

+ Manage TLS/SSL connection to brokers
+ Manage connection on cluster
+ Provide Google ProtoBuff serialization
+ Documentation for actors configuration
+ Documentation for message grouping and session 
+ Documentation for NATS message splitting