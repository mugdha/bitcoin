package bitcoin

/*
 * remoteListener is the main "boss" actor that runs on server machine and assigns work to all the worker machines including itself
 * It uses two bigint variables - start and offset - to keep track of how much work has been assigned
 * It saves the final output in the form of a List and prints it
 * 
 * @param number of zeroes 
 * 
 */

import akka.actor.Actor
import scala.collection.mutable.ListBuffer
import akka.actor.ActorRef

case class finalHash (lst:ListBuffer[String]) 

class remoteListener(noOfZeros : Int) extends Actor {
  
  var hashTemp = ListBuffer[String]() //to save final output
  var hashFinal = List.empty[String]
  val offset:BigInt=1000000 //size of workload
  var start:BigInt=0 //start of workload
  var masterList:List[ActorRef]=List() //list to save all localListener actors that contact this server
  var count : Int = 0 //counter variable
  var masterCount : Int = 0
  
  def receive = { 
    /*
     * This method will come from a localListener trying to start communication 
     * On receiving this message, this actor will add the sender actor to its list
     * Then it will send a message to sender saying start workers on your machine 
     */
    case "HelloRemote" => 
      masterCount+=1
      println("localListener added to remoteListener's list. Now we have " + masterCount +" localListeners")
      if(!masterList.contains(sender)) {
    		masterList =  masterList:+sender
      }
      sender ! "start workers"
      
    /*
     * This message will come from a localListener when it is ready to take more work
     * On receiving this message, this actor will assign more work to the sender actor and adjust the value of start variable
     */
    case "more work" =>
      sender ! doThisWork(start, offset,noOfZeros)
      start += offset
      
     /*
      * This message will come from timerActor at the end of specified time
      * On receiving this message, this actor will direct all localListeners in it's List to stop working and return
      */
     case "stopCode" => 
        //println("Time is up...")
     	for(master <- masterList){    
		println(master.path.name + " worker has stopped")
		master ! "stopWork"
     	}
        
     /*
      * This message will arrive from localListener after receiving the stop message
      * On receiving this message, this actor will receive the output from all localListeners and save it in its hashmap
      * It will print this list and then it will give signal to the masters to kill themselves 
      */
     case finalHash(bitCoinList: ListBuffer[String]) =>
           
       hashTemp = hashTemp ++ bitCoinList
      // hashFinal = hashFinal ++ hashTemp
       count+=1
       println("localListener returned final list to remoteListener. Now " + count +" localListeners have returned results")
       
       if(count == masterList.length) {
    	  Thread.sleep(1000) // To ensure output on console is readable :)
		  if(!hashTemp.isEmpty){
		    println("-------------------------------------------------")
		    println("Final Output")
		    println("-------------------------------------------------")
		    println(hashTemp.mkString("\n"))
		    println("-------------------------------------------------")
		    println("Total bit Coins found::"+hashTemp.length)
		    println("-------------------------------------------------")
		    }
		    else {
		      println("-------------------------------------------------")
		      println("Final Output")
		      println("-------------------------------------------------")
		      println("Could not get bit coins!")
		    }    
    	   killMasters
       }         
  }  
  
  /*
   * Sends a message to all localListeners in mastersList to kill their actorSystems
   */
  def killMasters = {
    for(master <- masterList){
    	master ! "kill"
	}
  }
  
}

