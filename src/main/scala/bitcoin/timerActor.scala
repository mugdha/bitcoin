package bitcoin

/*
 * timerActor.scala blocks for 5 minutes then notifies the remoteListener that time is up
 * timerActor.acala starts at the same time as remoteListener and runs on the server machine
 * 
 * @param actor-reference of remoteListener 
 */

import akka.actor.Actor
import akka.actor.ActorRef

case object start //This is a message object of timerActor

class timerActor(actor: ActorRef) extends Actor{

  def receive={
    //this method blocks for 5 minutes
  	case start => 
      var startTime = System.currentTimeMillis()
      while(System.currentTimeMillis() < startTime + 300000){
        
      } 
      //send message to remoteListener that time is up and you need to stop the code 
      actor ! "stopCode"
  }
}

