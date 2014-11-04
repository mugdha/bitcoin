package bitcoin

/* 
 * localWorker is the actor who actually does work of bit coin mining 
 * It collects all the bit coins in a list and passes it to localListener  
 */
import akka.actor.Actor
import java.security.MessageDigest
import akka.dispatch.Foreach
import scala.util.control.Breaks
import java.io._
import scala.collection.mutable.ListBuffer
case class calculate(start : BigInt, offset : BigInt, noOfZeroReq:Int)

class localWorker extends Actor {
  var resultHash = new ListBuffer[String]() //To collect output generated by the worker
  var randomString=""  // alpha numeric value corresponding to whom cryptographic hash function value would be computed
  var hashValue=""   //  container for Hex String generated
     

	def receive = {
	  
	  /*
	   * This message comes from parent of the localWOrker
	   * On receiving this message, this actor collects bitcoins and returns the list of bitcoins to it's parent 
	   *
	   * @param start of workload
	   * @param offset of work to be done
	   * @param number of zeroes
	   * 
	   */
	  case calculate(start : BigInt, offset : BigInt, noOfZeroReq:Int) => 
	    var zeroString=createStringOfZero(noOfZeroReq)
        for ( i <- start to offset+start) { 
           randomString ="mugdha23"+convertdecimalTo94Base(i)
           hashValue =encryptPassword(randomString)
           var noOfZerosInhash:Int=checkForNoOfZeros(hashValue)
            if(noOfZerosInhash>=noOfZeroReq){   		                     
		           var combined:String=randomString+"\t"+hashValue                  
		            resultHash += combined		            	
           }         
        }
	   
	   // send result to parent
	   //println("size of result in worker = "+resultHash.length)
	   sender ! AppendResultLL(resultHash ,noOfZeroReq)
	    
	}
	
   /*
    * This method creates a random string by converting a number to base 94 
    * and converting it from ASCII to char
    * The string starts with gatorlink id of one of the team members - mugdha23
    * 
    * @param BigInt value to be converted to base 94
    * @return random string generated
    */
	def convertdecimalTo94Base(decimavVal:BigInt):String = {
	  	var a=""
  		var b : BigInt =0   // binary representation as a string
  		var c : Char = ' '
  		var deci=decimavVal
  		while (deci!=0) {
            var r:BigInt =deci % 94; // remainder
            
            b = r +33; // concatenate remainder            
            c=b.toChar
            a+=c
            
            deci=deci / 94; // reduce n
        }
        a
  	}

   /*
    * Creates string of "k" zeroes so that we get correct bitcoins
    * 
    * @param number of zeroes required
    * @return String of "k" zeroes
    */
    def createStringOfZero(noOfZero: Int): String = {
	  var s:String=""
			  for(i<- 1 to noOfZero)
				  	s+='0'     
      s
  	}
  
  
   /*
   * Password Hashing Using Message Digest Algo
   * 
   * @param String to be hashed
   */
  def encryptPassword(password: String): String = {
    val algorithm: MessageDigest = MessageDigest.getInstance("SHA-256")
    val defaultBytes: Array[Byte] = password.getBytes("UTF-8")
    algorithm.reset
    algorithm.update(defaultBytes)
    val messageDigest: Array[Byte] = algorithm.digest
    
    getHexString(messageDigest)
  }

  /*
   * Generate HexString For Password & userId Encryption
   * 
   * @param output of SHA256
   * @return output in the form of String
   */
  def getHexString(messageDigest: Array[Byte]): String = {
    val hexString: StringBuffer = new StringBuffer
    
    for(i<-0 to messageDigest.length-1){     
      hexString.append(Integer.toString((messageDigest(i) & 0xff) + 0x100, 16).substring(1));        
    }
     
    hexString.toString
  }
  
  /*
   * Checks for number of zero in the front of hash function value
   * 
   * @param output of hash in form of String
   * @return number of zeroes in String
   */
  def checkForNoOfZeros(a:String):Int = {  
    var noOfZero:Int=0;
    val loop = new Breaks;
    loop.breakable {
        for(j<- 0 to a.length())
		    {
		      if(a(j)== '0'){
              noOfZero+=1
            }
            else
               loop.break;
		    }    
    }	 	     
    noOfZero
  }
}
