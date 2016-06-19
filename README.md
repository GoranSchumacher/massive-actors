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
    
    Test notification through Akka Pub/Sub as well as through our own pub/sub interface.
    
    Also test persisting through ElasticSearch with Elastic4s (without Akka Persistence)
    
* ESTest
    
    Test ElasticSerach integration through Elastic4s.
    
* DeadLetterTest
    
    Test deadletter actor.
    
    Purpose if this is to let the deadletter actor notify the lookupactor of messages that's been lost. The lookupactor will then instantiate the entity actor again and resend the message. This way the lookupactor will not have to keep track of the entityactors lifecycle.
    