package sample;

import static java.lang.Thread.sleep;

import akka.dispatch.verification.DPOR;

import akka.actor.ActorRef;
import akka.actor.Actor;
import akka.actor.ActorCell;
import akka.pattern.AskSupport;
import akka.actor.ActorSystem;
import akka.dispatch.Envelope;
import akka.dispatch.MessageQueue;
import akka.dispatch.MessageDispatcher;

import scala.concurrent.impl.CallbackRunnable;



privileged public aspect WeaveActor {

  DPOR dpor = new DPOR();
    
  pointcut enqueueOperation(MessageQueue me, ActorRef receiver, Envelope handle): 
  execution(public * akka.dispatch.MessageQueue.enqueue(ActorRef, Envelope)) &&
  args(receiver, handle) && this(me);
  
  Object around(MessageQueue me, ActorRef receiver, Envelope handle):
  enqueueOperation(me, receiver, handle) {
  	if (dpor.aroundEnqueue(me, receiver, handle))
   		return proceed(me, receiver, handle);
   	else
   		return null;
  }
   
  before(MessageQueue me, ActorRef receiver, Envelope handle):
  execution(* akka.dispatch.MessageQueue.enqueue(..)) &&
  args(receiver, handle, ..) && this(me) {
  }
  
  
  
  before(ActorCell me, Object msg):
  execution(* akka.actor.ActorCell.receiveMessage(Object)) &&
  args(msg, ..) && this(me) {
	dpor.beginMessageReceive(me);
  }
  
  after(ActorCell me, Object msg):
  execution(* akka.actor.ActorCell.receiveMessage(Object)) &&
  args(msg, ..) && this(me) {
	dpor.afterMessageReceive(me);
  }



  pointcut dispatchOperation(MessageDispatcher me, ActorCell receiver, Envelope handle): 
  execution(* akka.dispatch.MessageDispatcher.dispatch(..)) &&
  args(receiver, handle, ..) && this(me);

  Object around(MessageDispatcher me, ActorCell receiver, Envelope handle):
  dispatchOperation(me, receiver, handle) {
  	if (dpor.aroundDispatch(me, receiver, handle))
   		return proceed(me, receiver, handle);
   	else
   		return null;
  }


}
