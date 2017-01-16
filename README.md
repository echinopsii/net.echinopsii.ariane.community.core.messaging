ariane.community.messaging
==========================

Ariane Community Core Framework Messaging Module

# Purpose

This module has been created to avoid JMS over complexity and provide efficient but simple message definition and 
messages flow patterns API to fit Ariane Framework needs. 

The first need was the ability to create [a testing environment simulating trade applications workflow] (https://github.com/echinopsii/net.echinopsii.ariane.scenarios)
through RabbitMQ and then [map](https://slack-files.com/T04JMETB8-F1LTAMFGB-4194f337a9) this environment thanks Ariane Framework and the [Ariane RabbitMQ plugin] (https://github.com/echinopsii/net.echinopsii.ariane.community.plugin.rabbitmq).

As we wanted being able to reuse same messaging API through other messaging middleware we wanted to keep this API as simple as possible. 
Today we also use this API with [NATS] (http://nats.io/) and Ariane is using it a lot as described [here](http://nats.io/blog/ariane-mapping-microservice-with-nats/).

Finally we wanted to use actor model provided by [Akka](http://akka.io/) to define the software architecture business logic through this API calls - and so being able to reuse Akka powerful tooling like routers.

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

// when closing all resources used by the client will be also closed 
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

On the server side you first need to create your business logic and then create a service which will listen a request queue and forward messages to 
your business callback. 

```
import net.echinopsii.ariane.community.messaging.common.MomClientFactory
import net.echinopsii.ariane.community.messaging.api.MomClient
import net.echinopsii.ariane.community.messaging.api.MomMsgTranslator;
import net.echinopsii.ariane.community.messaging.api.AppMsgWorker
...

final static String sendedMsgBody = "Hello !";

class MyServerMsgWorker implements AppMsgWorker {

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
MyServerMsgWorker myServerMsgWorker = new MyServerMsgWorker();

// my service listening to "FAF_QUEUE"
client.getServiceFactory().requestService("FAF_QUEUE", myServerMsgWorker);
```

##### Behind the scene:

a message consumer on "FAF_QUEUE" is created as well as a akka router and several akka routees (5 by default but can be overrided 
in the configuration with mom_cli.nb_routees_per_service property field).

The message consumer will forward the message as is to the akka router which will then forward the message to one of its routees (round robin). 

The routee is in charge of decoding the message from its technical definition (NATS or RabbitMQ) to the Messaging API message definition (a map !) and 
finally forward it to the AppMsgWorker through its apply method. 

#### Client side 
 
On the client side you just need to define a message and send your request in fire and forget fashion way.
 
```
import net.echinopsii.ariane.community.messaging.common.MomClientFactory
import net.echinopsii.ariane.community.messaging.api.MomClient
import net.echinopsii.ariane.community.messaging.api.MomMsgTranslator;
...

final static String sendedMsgBody = "Hello !";

// create the message 
Map<String, Object> message = new HashMap<String, Object>();
message.put(MomMsgTranslator.MSG_BODY, sendedMsgBody);

// send the request
client.createRequestExecutor().FAF(message, "FAF_QUEUE");
```
 
### Remote Procedure Call

Through this pattern you want to send a request to a server and get its reply.

#### Server side

On the server side you first need to create your business logic and then create a service which will listen a request queue and forward messages to 
your business callback.

```
import net.echinopsii.ariane.community.messaging.common.MomClientFactory
import net.echinopsii.ariane.community.messaging.api.MomClient
import net.echinopsii.ariane.community.messaging.api.MomMsgTranslator;
import net.echinopsii.ariane.community.messaging.api.AppMsgWorker
...

final static String sendedRequestBody = "Hello !";
final static String sendedReplyBody   = "Hello ! How can I help you ?";

class MyServerMsgWorker implements AppMsgWorker {
    
    @Override
    public Map<String, Object> apply(Map<String, Object> message) {
        String recvMsgBody = new String((byte [])message.get(MomMsgTranslator.MSG_BODY));

        Map<String, Object> reply = new HashMap<String, Object>();
        if (recvMsgBody.equals(sendedRequestBody)) {
            reply.put(MomMsgTranslator.MSG_RC, MomMsgTranslator.MSG_RET_SUCCESS);
            reply.put(MomMsgTranslator.MSG_BODY, sendedReplyBody);
        } else {
            reply.put(MomMsgTranslator.MSG_RC, MomMsgTranslator.MSG_RET_BAD_REQ);
            reply.put(MomMsgTranslator.MSG_ERR, "Uuh ? Are you inviding me from Mars ?");
        }

        return reply;
    }
}

MyServerMsgWorker myServerMsgWorker = new MyServerMsgWorker();
client.getServiceFactory().requestService("RPC_QUEUE", myServerMsgWorker);

```

##### Behind the scene:

There is not so much difference to the fire and forget behavior here. The main difference here is that we return a non null reply in the AppMsgWork.apply 
method to the akka routees which will then encode it (NATS or RabbitMQ fashion) and send it to the pre defined reply queue.  

#### Client side

On the client side you need to create your business logic for reply, define a message request and send your request in RPC fashion way.

```
import net.echinopsii.ariane.community.messaging.common.MomClientFactory
import net.echinopsii.ariane.community.messaging.api.MomClient
import net.echinopsii.ariane.community.messaging.api.MomMsgTranslator;
import net.echinopsii.ariane.community.messaging.api.AppMsgWorker
...

final static String sendedRequestBody = "Hello !";

class MyClientReplyWorker implements AppMsgWorker {
    @Override
    public Map<String, Object> apply(Map<String, Object> message) {
        System.out.println(new String((byte [])message.get(MomMsgTranslator.MSG_BODY)));
        return null;
    }
}

MyServerMsgWorker myServerMsgWorker = new MyServerMsgWorker();
Map<String, Object> request = new HashMap<String, Object>();
request.put(MomMsgTranslator.MSG_BODY, sendedRequestBody);
client.createRequestExecutor().RPC(request, "RPC_QUEUE", myClientReplyWorker);
```

##### Behind the scene:

Some tips to keep in mind : 
+ the reply queue will be defined and added as a message field if not defined already. 
+ a consumer will be created on the reply queue
+ if no reply is coming after timeout (default is 10 seconds but it can be overidded with mom_cli.rpc_timeout field in configuration)
then retry will be executed (2 retry by default but can be overidded with mom_cli.rpc_retry field in configuration)
+ when reply is received it will be forwarded to the AppMsgWorker.apply method else a TimeoutException will be raised.

### Feeder/Subscriber

In contrary to FAF and RPC flow patterns which are widely used in Ariane Framework, this feeder/subscriber pattern has been designed for
our testing environment. Goals here is to regularly feed a bus of messages representing kind of data update. These may change over time and 
depending your feedbacks.
 
#### Feeder

```
import net.echinopsii.ariane.community.messaging.common.MomClientFactory
import net.echinopsii.ariane.community.messaging.api.MomClient
import net.echinopsii.ariane.community.messaging.api.MomMsgTranslator;
import net.echinopsii.ariane.community.messaging.api.AppMsgFeeder
...

class MyStockFeeder implements AppMsgFeeder {

    private int interval = 100;
    private String stockName;

    public MyStockFeeder(String sname) {
        stockName = sname;
    }

    @Override
    public Map<String, Object> apply() {
        Map<String, Object> ret = new HashMap<String, Object>();
        ret.put("NAME", stockName);
        int price = (int)(Math.random() * 10 + Math.random() * 100 + Math.random() * 1000);
        ret.put("PRICE", price );
        return ret;
    }

    @Override
    public int getInterval() {
        return interval;
    }
}

MyStockFeeder feederStockA = new MyStockFeeder("STOCKA");
MyStockFeeder feederStockB = new MyStockFeeder("STOCKB");
MyStockFeeder feederStockC = new MyStockFeeder("STOCKC");

client.getServiceFactory().feederService("PRICE", "STOCKA", feederStockA.getInterval(), feederStockA);
client.getServiceFactory().feederService("PRICE", "STOCKB", feederStockB.getInterval(), feederStockB);
client.getServiceFactory().feederService("PRICE", "STOCKC", feederStockC.getInterval(), feederStockC);
```

#### Subscribers

```
import net.echinopsii.ariane.community.messaging.common.MomClientFactory
import net.echinopsii.ariane.community.messaging.api.MomClient
import net.echinopsii.ariane.community.messaging.api.MomMsgTranslator;
import net.echinopsii.ariane.community.messaging.api.AppMsgWorker
...

class StockPriceSubscriber implements AppMsgWorker {
    @Override
    public Map<String, Object> apply(Map<String, Object> message) {
        System.out.println(new String((byte [])message.get("NAME")) + ": " + new String((byte [])message.get("PRICE")));
        return message;
    }
}

StockPriceSubscriber stockSubs    = new StockPriceSubscriber();

//This subscriber will receive all stock price
client.getServiceFactory().subscriberService("PRICE", null, stockSubs);
//This subscriber will receive STOCKA price only
client.getServiceFactory().subscriberService("PRICE", "STOCKA", stockSubs);
client.getServiceFactory().subscriberService("PRICE", "STOCKB", stockSubs);
client.getServiceFactory().subscriberService("PRICE", "STOCKC", stockSubs);
```

# TODO

+ Manage TLS/SSL connection to brokers
+ Manage connection on cluster
+ Provide Google ProtoBuff serialization on NATS implementation (currently JSON)
+ Documentation for actors configuration
+ Documentation for message grouping and session 
+ Documentation for NATS message splitting

# Contributions

We're always happy to get some help from the free community. [Let's get in touch.](mailto:contact@echinopsii.net)

# Reuse 

We're always happy to provide code which serve other purpose than ours. 
If you want to reuse this code without AGPLv3 license virality, [let's get in touch.](mailto:contact@echinopsii.net) 