ariane.community.messaging
==========================

Ariane Community Core Framework Messaging Module

# Purpose

This module has been created to avoid JMS over complexity and provide efficient but simple message definition and 
messages flow patterns API to fit Ariane Framework needs. 

The first need was the ability to create [a testing environment simulating trade applications workflow] (https://github.com/echinopsii/net.echinopsii.ariane.scenarios)
through RabbitMQ first and then map this environment thanks Ariane Framework and the [Ariane RabbitMQ plugin] (https://github.com/echinopsii/net.echinopsii.ariane.community.plugin.rabbitmq).

![alt text](https://slack-files.com/T04JMETB8-F1LTAMFGB-4194f337a9?nojsmode=1)

We also wanted being able to reuse same messaging API through other messaging middleware and so keep the API as simple as possible. 
Today we can also can use this API with [NATS] (http://nats.io/).
  
Finally we wanted to use Actor Model provided by [Akka](http://akka.io/) to define business logic call back on top of this API calls.

# Some technical inputs

## Simple messages

## Simple flow patterns

### RPC pattern

### Feeder/Subscriber pattern

# TODO

+ Manage TLS/SSL connection to brokers
+ Manage connection on cluster
+ Documentation for message grouping and session 
+ Documentation for NATS message splitting