package broadcast;

import akka.actor.{ Actor, ActorRef, DeadLetter }
import akka.actor.ActorSystem
import akka.actor.Props
import akka.dispatch.verification.NodesUnreachable

case class Stop()
case class Init(msg: Set[String])
case class DataMessage(seq: Int, data: String)
case class RB_Broadcast(msg: DataMessage)
case class BEB_Broadcast(msg: DataMessage)
case class BEB_Deliver(msg: DataMessage)


class NodeTest extends Actor {
  type NodeSetT = Set[ActorRef]
  type DeliveredT = Set[DataMessage]
  
  val me = self.path.name
  var started = false
  
  var allActors: NodeSetT = Set()
  var delivered: DeliveredT = Set()
  

  def receive = {
    case d: DeadLetter =>
      println("DeadLetter")
      allActors = allActors - d.recipient
    case msg @ DataMessage(_, _) => println(me + " DataMessage: " + msg)
    case NodesUnreachable(nodes) => println(me + " NodesUnreachable: " + nodes)
    case other => println(me + " received an unknown message " + other)
  }
}

/**
 *
 * Local sending will just pass a reference to the
 * message inside the same JVM, without any restrictions
 * on the underlying object which is sent, whereas a
 * remote transport will place a limit on the message size.
 *
 * Messages which cannot be delivered (and for which this
 * can be ascertained) will be delivered to a synthetic actor
 * called /deadLetters. This delivery happens on a best-effort
 * basis; it may fail even within the local JVM (e.g. during
 * actor termination). Messages sent via unreliable network
 * transports will be lost without turning up as dead letters.
 *
 * An actor can subscribe to class akka.actor.DeadLetter on
 * the event stream, see Event Stream (Java) or Event Stream
 * (Scala) for how to do that. The subscribed actor will then
 * receive all dead letters published in the (local) system
 * from that point onwards. Dead letters are not propagated
 * over the network, if you want to collect them in one place
 * you will have to subscribe one actor per network node and
 * forward them manually. Also consider that dead letters are
 * generated at that node which can determine that a send
 * operation is failed, which for a remote send can be the
 * local system (if no network connection can be established)
 * or the remote one (if the actor you are sending to does
 * not exist at that point in time).
 *
 */
class Node extends Actor {
  type NodeSetT = Set[ActorRef]
  type DeliveredT = Set[DataMessage]
  
  val me = self.path.name
  var started = false
  
  var allActors: NodeSetT = Set()
  var delivered: DeliveredT = Set()

  def rb_bradcast(msg: DataMessage) {
    println(me + " is doing a reliable broadcast.")
    if (!started) {
      println("Not started!")
    }
    
    beb_broadcast(msg)
  }

  def beb_broadcast(msg: DataMessage) {
    delivered = delivered + msg
    println(me + " is doing a best effort broadcast.")
    if (!started) {
      println("not started")
    }
    
    allActors.map(node => node ! BEB_Deliver(msg))
  }

  def rb_deliver(msg: DataMessage) {
    if (!started) {
      println(me + " is not Started!")
    }
    println(me + " reliably delivered a broadcast mesage " + msg)
  }

  def beb_deliver(msg: DataMessage) {

    if (!started) {
      println(me + " is not Started!")
    }
    
    if (delivered contains msg) {
      return
    }

    rb_deliver(msg)
    beb_broadcast(msg)
  }
  

  def init(names: Set[String]) {
    started = true
    println(me + " is being intialized.")
    allActors = names.map(i => context.actorFor("../" + i))
  }
  

  def receive = {
    case d: DeadLetter => allActors = allActors - d.recipient
    case Init(nodes) => init(nodes)
    case Stop => context.stop(self)
    case RB_Broadcast(msg) => rb_bradcast(msg)
    case BEB_Deliver(msg) => beb_deliver(msg)
    case _ => println(me + " received an unknown message")
  }
}
