
import broadcast.Node,
       broadcast.NodeTest,
       broadcast.Init,
       broadcast.RB_Broadcast,
       broadcast.DataMessage

import akka.actor.Actor,
       akka.actor.ActorRef,
       akka.actor.DeadLetter,
       akka.actor.ActorSystem,
       akka.actor.Props
       
import akka.dispatch.verification.Instrumenter,
       akka.dispatch.verification.ExternalEvent,
       akka.dispatch.verification.Start,
       akka.dispatch.verification.Send,
       akka.dispatch.verification.DPOR

import scala.collection.immutable.Vector

import akka.dispatch.verification.NetworkPartition

object Main2 {

  val scheduler = new DPOR
  Instrumenter().scheduler = scheduler
  Instrumenter().tellEnqueue = new akka.dispatch.verification.TellEnqueueBusyWait
  
  val names = List.range(0, 3).map(i => "A-" + i.toString())
  
  val spawns = names.map(i => Start(Props[Node], name = i))
  val inits = names.map(name => Send(name, Init(names.toSet)))
  val rb = Send(names(0), RB_Broadcast(DataMessage(1, "Message")))
  
  val a = Set(names(0))
  val b = Set(names(1), names(2))
  val par = NetworkPartition(a, b)
  
  val externalEvents : Vector[ExternalEvent] = Vector() ++
    spawns ++ inits :+ rb :+ par
  
  scheduler.run(externalEvents)
}


object Main extends App {

  val scheduler = new DPOR
  Instrumenter().scheduler = scheduler
  Instrumenter().tellEnqueue = new akka.dispatch.verification.TellEnqueueBusyWait
  
  val names = List.range(0, 3).map(i => "A-" + i.toString())
  
  val spawns = names.map(i => Start(Props[NodeTest], name = i))
  val rb0 = Send(names(0), DataMessage(1, "Message"))
  val rb1 = Send(names(1), DataMessage(2, "Message"))
  val rb2 = Send(names(2), DataMessage(3, "Message"))
  val rb3 = Send(names(0), DataMessage(4, "Message"))
  
  val a = Set(names(0))
  val b = Set(names(1), names(2))
  val par = NetworkPartition(a, b)
  
  val externalEvents : Vector[ExternalEvent] = Vector() ++
    spawns :+ rb0 :+ rb1 :+ rb2 :+ rb3 :+ par
  
  scheduler.run(externalEvents)
}
