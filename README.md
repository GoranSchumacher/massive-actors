# Akka Actor Test Bench

This project is a test bench for several Akka ideas.


URLs
====
http://localhost:9000/url/apple.com/1
localhost:9000/urlget/apple.com

Read purpose [here](http://bit.ly/1VGYkqf)


Apps
----

* StockPersistentActorApp
    
    Tests the functionality of BaseLookupActor & BaseAutoShutdownActor
    
    Create a LookupActor for your entity by subclassing BaseLookupActor.
    
    Find and instantiate entity actors.
    
    Set how long they should stay in memory when not used.
     
    
* URLPersistentActorApp
    
    Test Akka Persistence through MongoDB.
    
    Creates an actor that at given intervals downloads content of an url. If the length of the content has changed the actor notifies listeners through Akka Pub/sub and own pub/sub interface.
    
    Test notification through Akka Pub/Sub as well as through our own pub/sub interface.
    
    Also test persisting through ElasticSearch with Elastic4s (without Akka Persistence)
    
* ESTest
    
    Test ElasticSearch integration through Elastic4s.
    
* DeadLetterTest
    
    Test deadletter actor.
    
    Purpose if this is to let the deadletter actor notify the lookupactor of messages that's been lost. The lookupactor will then instantiate the entity actor again and resend the message. This way the lookupactor will not have to keep track of the entityactors lifecycle.
    
    
* FactorialPersistentActorApp

    Calculate fatorials ( 5! ). Each factor is calculated by one actor instance. The result is cached in the actor. Demoes using MongoDB Akka Persistence, event sourcing, communication between actors and lifetime handling.
    