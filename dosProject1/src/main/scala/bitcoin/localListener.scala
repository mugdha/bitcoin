package bitcoin

/*
 * localistener is a "middle-man" actor that runs on all the machines. 
 * This actor checks number of cores in the machine and creates that many localWorker actors
 * Thus it takes a workload from the "boss" aka remoteListener and distributes it equally on all cores of the machine
 * 
 * It is also responsible for killing the system gracefully
 * 
 * @param ip-address of server
 * @param actor system in which it is acting
 * 
 */

import akka.actor.Actor
import akka.actor.Props
import akka.actor.ActorRef
import akka.actor.PoisonPill
import akka.actor.ActorSystem
import scala.collection.mutable.ListBuffer

case class doThisWork(start: BigInt, offset:BigInt ,noOfZeroReq:Int)
case class AppendResultLL (lstOfVal:ListBuffer[String],noOfZero:Int)
case object connectRemote
case object stopWork

class localListener(var ip :String,actorsys:ActorSystem) extends Actor {
  var semifinalHash = new ListBuffer[String]() //To collect output returned by all workers
  var noCores:Int = 100 //variable to store of cores of CPU
  var boss : akka.actor.ActorSelection = null //reference to store remoteListener actor 
  var count : Int =0 //counter variable
  var noOfZero:Int=0 //variable to store number of zeroes requires in hash
  
  def receive={ 
    
    /*
     * This message comes from main class object. 
     * On receiving this message, this actor will try to connect to the remoteListener 
     * When it finds the remoteListener actor, it will send a Hello message
     */
    case "connectRemote" => 
     	val url = "akka.tcp://RemoteActor@"+ip+":2552/user/Remote"
     	boss =context.actorSelection(url);
     	boss ! "HelloRemote"
     	
    /*
     * This message comes from remoteListener
     * On receiving this message, this actor starts workers in it's actorSystem 
     * and asks the remoteListener for work
     */
  	case "start workers" => 
      startWorkers
      sender ! "more work"    
     
    /*
     * This message comes from the remoteListener. It contains the workload as parameters
     * On receiving this message, this actor distributes work among it's children
     * i.e the localWOrkers running on it's machine
     */
    case doThisWork(start: BigInt, offset:BigInt ,noOfZeroReq:Int) =>
      noOfZero=noOfZeroReq
     	distributeWork(start, offset, noOfZeroReq)      
    
    /*
     * This message comes from each of the localWorkers when they have finished the work assigned to them
     * On receiving this message, this actor appends the localWorkers list to it's own list
     * It uses a count to keep track of how many children have returned
     * When all it's children have returned, it contacts the "boss" aka remoteListener for more work
     */
    case AppendResultLL(list1:ListBuffer[String],noOfZero:Int) =>
       semifinalHash=semifinalHash ++ list1
       count+=1
     	//after all workers return, send values to main server
     	if(count == noCores) {
     	  println("All workers finished their work. localListener asking for more work ...")
     	  boss ! "more work"
     	  count =0
     	}
     
    /*
     * This message comes from the remoteListener at the end of 5 minutes
     * On receiving this message, this actor kills all its children by feeding them a poison pill
     * Then it returns the final result to remoteListener
     */
    case "stopWork" => 
       println("\nWorker Returning Final Value.....\n")
       for(child<-context.children){
    		child ! PoisonPill
       }
       boss ! finalHash(semifinalHash )
         
    /*
     * This message comes from remoteLisener after it has received results from all its workers
     * On receiving this message, this actor shuts down it's actorSyatem
     * This will also shut down the scala program
     */        
    case "kill" => 
       actorsys.shutdown  
        	
      
  }
  
  /*
   * Checks how many cores are available on the machines and creates that many children
   * Children are actors of localWorker class
   */
  def startWorkers = { 
    noCores = Runtime.getRuntime().availableProcessors()
    println(noCores + " workers created by localListener\n")
    for(i<- 0 until (noCores)){
    	var worker = context.actorOf(Props(new localWorker))
    }
  } 
  
  /*
   * Distributes work equally among all its children
   * 
   * @param start of workload
   * @param offset of work to be done
   * @param number of zeroes
   */
  def distributeWork(start : BigInt, offset : BigInt, noOfZeroReq:Int) = {
    var pseudoStart : BigInt = start
    var pseudoOffset : BigInt = offset/noCores
    for(workers<-context.children){
    	workers ! calculate(pseudoStart, pseudoOffset, noOfZeroReq)
    	pseudoStart+=pseudoOffset
    }
  }
}

