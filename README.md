# Akka Actor Test Bench

This project is a test bench for several Akka ideas.

Read purpose [here](http://bit.ly/1VGYkqf)

Features
--------
* Logging in Elastic Search

    With the use of a logging actor, log files are stored in Elastic search.
    The purpose is to make log files searchable with Kibana.
    Meta data, as requestID, userID can be added to the logs and made searchable.
    
    
* Work flow using actors.

    Using a mix of stateless and load balanced, configured stateless actors and stateful, event sourced and peristent actors.

Playframework Controller
----
* [ActorController](app/controllers/ActorController.scala)
    
    Start as a Play application: sbt run

    In browser go to http://localhost:9000/urlEmptyForm.

    Follow the instructions and enter a name and a valid ur.
    
    
    The url will be called once every minute and any changes in size will be reported.
    
    
    This demos how to create a web page with a javascript websocket connection.
    
    
    An Actor connected to the form will be created. 
    
    This actor will instantiate and subscribe to events from an actor that will download the url once every minute
    and report back any changes in size.
    

Apps
----

* [StockPersistentActorApp](app/app/StockPersistentActorApp.scala)
    
    Tests the functionality of BaseLookupActor & BaseAutoShutdownActor
    
    Create a LookupActor for your entity by subclassing BaseLookupActor.
    
    Find and instantiate entity actors.
    
    Set how long they should stay in memory when not used.
     
    
* [URLPersistentActorApp](app/app/URLPersistentActorApp.scala)
    
    Test Akka Persistence through MongoDB.
    
    Creates an actor that at given intervals downloads content of an url. If the length of the content has changed the actor notifies listeners through Akka Pub/sub and own pub/sub interface.
    
    Test notification through Akka Pub/Sub as well as through our own pub/sub interface.
    
    Also test persisting through ElasticSearch with Elastic4s (without Akka Persistence)
    
* [ESTest](app/app/ESTest.scala)
    
    Test ElasticSearch integration through Elastic4s.
    
* [DeadLetterTest ===== THIS IS AWESOME!!!](app/app/DeadLetterTest.scala)
    
    Test deadletter actor.
    
    Purpose if this is to let the deadletter actor notify the lookupactor of messages that's been lost. The lookupactor will then instantiate the entity actor again and resend the message.
    This way the lookupactor will not have to keep track of the entityactors lifecycle.
    This is amazing, since now we can have millions of actors in an actor system that can hibernate when they are not used.
    Even when hibernating these actors can receive messages from other actors or timed events from the actor system.
    
    
* [FactorialPersistentActorApp](app/app/FactorialPersistentActorApp.scala)

    Calculate fatorials ( 5! ). Each factor is calculated by one actor instance. The result is cached in the actor. Demoes using MongoDB Akka Persistence, event sourcing, communication between actors and lifetime handling.
    
    
* [HTMLCleanerAndPDFGeneratorApp](app/app/HTMLCleanerAndPDFGeneratorApp.scala)

    Tests stateless actors with [deployment configuration](conf/application.conf). 
    The configuration tells how to scale up and down the number of actor instances.
    Tests actor for cleaning html using HTMLCleaner fw and actor for converting html to pdf.
    Shows how to build work flows consisting of actor chains that pass messages from one actor to the other, using the RouteSlip Enterprise Integration patterns (EIP).
    More EIP's, like filters, routers etc will be implemented in the future.
    Also Try-like error handling and propagation will be implemented.
