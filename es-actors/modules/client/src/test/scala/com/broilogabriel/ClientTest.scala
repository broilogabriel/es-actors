package com.broilogabriel

import java.util.UUID

import akka.actor.{ ActorRef, ActorSystem, PoisonPill }
import akka.testkit.TestActor.AutoPilot
import akka.testkit.{ ImplicitSender, TestActor, TestActorRef, TestKit }
import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{ BeforeAndAfter, BeforeAndAfterAll, WordSpecLike }

class ClientTest extends TestKit(ActorSystem("MySpec")) with LazyLogging with ImplicitSender
    with WordSpecLike with InMemoryES with BeforeAndAfterAll with BeforeAndAfter {

  override def afterAll(): Unit = {
    node.close()
    TestKit.shutdownActorSystem(system)
  }

  after {
    deleteAllData()
  }

  "Client" should {
    "send cluster config to remote and close" in {
      createIndex("index-1")
      setAutoPilot(new TestActor.AutoPilot {
        override def run(sender: ActorRef, msg: Any): AutoPilot = {
          logger.info(msg.toString)
          msg match {
            case cc: ClusterConfig => sender ! UUID.randomUUID()
            case other: Any => logger.info(s"Received unexpected message $other")
          }
          TestActor.KeepRunning
        }
      })
      val config = Config(index = "index-1", sourceCluster = "elasticsearch_embedded")
      val actorRef = TestActorRef(new Client(config, testActor.path))
      expectMsgClass(classOf[ClusterConfig])
    }

    "send cluster config to remote and send some data" in {
      val numItems = 500
      putSomeData("index-1", "type", numItems)
      setAutoPilot(new TestActor.AutoPilot {
        override def run(sender: ActorRef, msg: Any): AutoPilot = {
          msg match {
            case cc: ClusterConfig => sender ! UUID.randomUUID()
            case to: TransferObject => sender ! to.hitId
            case other: Any => logger.info(s"Received unexpected message $other")
          }
          TestActor.KeepRunning
        }
      })
      val config = Config(index = "index-1", sourceCluster = "elasticsearch_embedded")
      val actorRef = TestActorRef(new Client(config, testActor.path))
      expectMsgClass(classOf[ClusterConfig])
      val messages = receiveWhile() {
        case to: TransferObject => to
      }
      assert(messages.nonEmpty)
      assert(messages.size == numItems)
    }

    "send cluster config to remote and send some data with scrolling - remote init shutdown" in {
      val numItems = 1500
      putSomeData("index-1", "type", numItems)
      setAutoPilot(new TestActor.AutoPilot {
        override def run(sender: ActorRef, msg: Any): AutoPilot = {
          msg match {
            case cc: ClusterConfig => sender ! UUID.randomUUID()
            case to: TransferObject => sender ! to.hitId
            case other: Any => logger.info(s"Received unexpected message $other")
          }
          TestActor.KeepRunning
        }
      })
      val config = Config(index = "index-1", sourceCluster = "elasticsearch_embedded")
      val actorRef = TestActorRef(new Client(config, testActor.path))

      expectMsgClass(classOf[ClusterConfig])

      val firstBatch = receiveWhile() {
        case to: TransferObject => to
      }
      assert(firstBatch.nonEmpty)
      assert(firstBatch.size == ClusterConfig.scrollSize)
      actorRef ! MORE

      val secondBatch = receiveWhile() {
        case to: TransferObject => to
      }
      assert(secondBatch.nonEmpty)
      assert(secondBatch.size == (numItems - ClusterConfig.scrollSize))

      actorRef ! PoisonPill
    }

    "send cluster config to remote and send some data with scrolling - self shutdown" in {
      val numItems = 1500
      putSomeData("index-1", "type", numItems)
      setAutoPilot(new TestActor.AutoPilot {
        override def run(sender: ActorRef, msg: Any): AutoPilot = {
          msg match {
            case cc: ClusterConfig => sender ! UUID.randomUUID()
            case to: TransferObject => sender ! to.hitId
            case other: Any => logger.info(s"Received unexpected message $other")
          }
          TestActor.KeepRunning
        }
      })
      val config = Config(index = "index-1", sourceCluster = "elasticsearch_embedded")
      val actorRef = TestActorRef(new Client(config, testActor.path))

      expectMsgClass(classOf[ClusterConfig])

      val firstBatch = receiveWhile() {
        case to: TransferObject => to
      }
      assert(firstBatch.nonEmpty)
      assert(firstBatch.size == ClusterConfig.scrollSize)
      actorRef ! MORE

      val secondBatch = receiveWhile() {
        case to: TransferObject => to
      }
      assert(secondBatch.nonEmpty)
      assert(secondBatch.size == (numItems - ClusterConfig.scrollSize))
    }
  }
}
