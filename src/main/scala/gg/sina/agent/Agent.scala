package gg.sina.agent

import org.apache.pekko.actor.typed._
import org.apache.pekko.actor.typed.scaladsl.AskPattern._
import org.apache.pekko.actor.typed.scaladsl.Behaviors
import org.apache.pekko.util.Timeout

import java.util.UUID
import scala.concurrent.Future

case class Agent[A](private val initialValue: Future[A])(implicit val system: ActorSystem[?], timeout: Timeout) {
  private val name: String = UUID.randomUUID().toString
  private val agent: ActorRef[Agent.AgentMessage[A]] = system.systemActorOf(
    Agent.agent(initialValue),
    name,
    Props.empty
  )

  def map(value: A => A): Future[A] = agent
    .ask[Future[A]](ref => Agent.SetFn(value, ref))
    .flatten

  def map(value: A): Future[A] = agent
    .ask[Future[A]](ref => Agent.Set(value, ref))
    .flatten

  def flatMap(value: Future[A]): Future[A] = agent
    .ask[Future[A]](ref => Agent.SetFuture(value, ref))
    .flatten

  def flatMap(value: A => Future[A]): Future[A] = agent
    .ask[Future[A]](ref => Agent.SetFutureFn(value, ref))
    .flatten

  def clone[B](value: Future[B]): Agent[B] = Agent(
    agent
      .ask[Future[B]](ref => Agent.Clone[A, B](value, ref))
      .flatten
  )

  def clone[B](value: A => Future[B]): Agent[B] = Agent(
    agent
      .ask[Future[B]](ref => Agent.CloneFn[A, B](value, ref))
      .flatten
  )

  def apply(): Future[A] = get

  def get: Future[A] = agent
    .ask[Future[A]](ref => Agent.Get(ref))
    .flatten

}

object Agent {
  def apply[A](value: A)(implicit system: ActorSystem[SpawnProtocol.Command], timeout: Timeout): Agent[A] = Agent(
    Future.successful(value)
  )

  private def process[A](result: Future[A], replyTo: ActorRef[Future[A]]): Behavior[AgentMessage[A]] = {
    val newAgent = agent(result)
    replyTo ! result
    newAgent
  }

  private def agent[A](value: Future[A]): Behavior[AgentMessage[A]] =
    Behaviors.setup { context =>
      Behaviors.receiveMessage {
        case Set(value, replyTo) =>
          val result = Future.successful(value)
          process(result, replyTo)
        case SetFn(fn, replyTo) =>
          val result = value.map(fn)(context.executionContext)
          process(result, replyTo)
        case SetFuture(future, replyTo) =>
          val result = future
          process(result, replyTo)
        case SetFutureFn(fn, replyTo) =>
          val result = value.flatMap(fn)(context.executionContext)
          process(result, replyTo)
        case Clone(future, replyTo) =>
          replyTo ! future
          Behaviors.same
        case CloneFn(fn, replyTo) =>
          replyTo ! value.flatMap(fn)(context.executionContext)
          Behaviors.same
        case Get(replyTo) =>
          replyTo ! value
          Behaviors.same
      }
    }

  private sealed trait AgentMessage[A]

  private final case class Set[A](value: A, replyTo: ActorRef[Future[A]]) extends AgentMessage[A]

  private final case class SetFn[A](fn: A => A, replyTo: ActorRef[Future[A]]) extends AgentMessage[A]

  private final case class SetFuture[A](value: Future[A], replyTo: ActorRef[Future[A]]) extends AgentMessage[A]

  private final case class SetFutureFn[A](fn: A => Future[A], replyTo: ActorRef[Future[A]]) extends AgentMessage[A]

  private final case class Clone[A, B](fn: Future[B], replyTo: ActorRef[Future[B]]) extends AgentMessage[A]

  private final case class CloneFn[A, B](fn: A => Future[B], replyTo: ActorRef[Future[B]]) extends AgentMessage[A]

  private final case class Get[A](replyTo: ActorRef[Future[A]]) extends AgentMessage[A]
}