ariane.community.messaging
==========================

Ariane Community Core Framework Messaging Module

# Purpose

This module has been created to avoid JMS over complexity and provide efficient but simple message definition and 
messages flow patterns API to fit Ariane Framework needs. 

The first need was the ability to create [a testing environment simulating trade applications workflow] (https://github.com/echinopsii/net.echinopsii.ariane.scenarios)
through RabbitMQ first and then [map](https://slack-files.com/T04JMETB8-F1LTAMFGB-4194f337a9) this environment thanks Ariane Framework and the [Ariane RabbitMQ plugin] (https://github.com/echinopsii/net.echinopsii.ariane.community.plugin.rabbitmq).

As we wanted being able to reuse same messaging API through other messaging middleware we want to keep this API as simple as possible. 
Today we can also use this API with [NATS] (http://nats.io/).

Finally we wanted to use Actor Model provided by [Akka](http://akka.io/) to define business logic call back on top of this API calls - and so being able to reuse Akka powerful tooling like routers.

# Some technical inputs

## Simple messages

This API defines messages as simple Java Map where you can define several fields. 
Depending on the broker you are using, you can define technical JMS like fields or specific broker fields to define behavior of your message.
As well as in JMS or RabbitMQ messages definition you can also define a body field. And for sure you can also define any kind of field you need for you app. 
The API implementation will then do the plumbing for you.

Bellow are some pre defined fields you will find in the API :

| Field name                          | Field description                                | RabbitMQ impl support | NATS support |
| ----------------------------------- | ------------------------------------------------ | --------------------- | ------------ |
| MomMsgTranslator.MSG_APPLICATION_ID | Define message application id source             | OK                    | OK           |
| MomMsgTranslator.MSG_MESSAGE_ID     | Define message ID                                | OK                    | OK           |
| MomMsgTranslator.MSG_CORRELATION_ID | Define message correlation ID                    | OK                    | OK           |
| MomMsgTranslator.MSG_DELIVERY_MODE  | Define message delivery mode (persistent or not) | OK                    | IGNORED      |
| MomMsgTranslator.MSG_EXPIRATION     | Define message expiration                        | OK                    | IGNORED      |
| MomMsgTranslator.MSG_PRIORITY       | Define message priority                          | OK                    | IGNORED      |
| MomMsgTranslator.MSG_REPLY_TO       | Define message reply destination                 | OK                    | OK           |
| MomMsgTranslator.MSG_TIMESTAMP      | Define message timestamp                         | OK                    | OK           |
| MomMsgTranslator.MSG_TYPE           | Define message type                              | OK                    | IGNORED      |
| MomMsgTranslator.MSG_RC             | Define return code on the reply                  | OK                    | OK           |
| MomMsgTranslator.MSG_ERR            | Define message error if any                      | OK                    | OK           |

## Simple flow patterns

### RPC pattern

### Feeder/Subscriber pattern

# TODO

+ Manage TLS/SSL connection to brokers
+ Manage connection on cluster
+ Provide Google ProtoBuff serialization
+ Documentation for message grouping and session 
+ Documentation for NATS message splitting