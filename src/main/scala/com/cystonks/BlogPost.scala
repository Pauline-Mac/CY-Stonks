import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.state.scaladsl.{DurableStateBehavior, Effect}

object BlogPost {
  // Commands
  sealed trait Command
  final case class CreateDraft(title: String, content: String, replyTo: ActorRef[Response]) extends Command
  final case class Publish(replyTo: ActorRef[Response]) extends Command
  final case class GetPost(replyTo: ActorRef[Response]) extends Command
  
  // Responses
  sealed trait Response
  final case class PostResponse(post: State) extends Response
  final case object Accepted extends Response
  final case object Rejected extends Response
  
  // State
  sealed trait State
  case object BlankState extends State
  final case class DraftState(content: Content) extends State
  final case class PublishedState(content: Content) extends State
  
  final case class Content(title: String, body: String)
  
  // Entity behavior
  def apply(entityId: String): Behavior[Command] = {
    Behaviors.setup { context =>
      DurableStateBehavior[Command, State](
        persistenceId = PersistenceId("BlogPost", entityId),
        emptyState = BlankState,
        commandHandler = commandHandler
      )
    }
  }
  
  private val commandHandler: (State, Command) => Effect[State] = { (state, command) =>
    (state, command) match {
      case (BlankState, CreateDraft(title, content, replyTo)) =>
        Effect
          .persist(DraftState(Content(title, content)))
          .thenReply(replyTo)(state => Accepted)
          
      case (DraftState(content), Publish(replyTo)) =>
        Effect
          .persist(PublishedState(content))
          .thenReply(replyTo)(state => Accepted)
          
      case (state: State, GetPost(replyTo)) =>
        Effect.reply(replyTo)(PostResponse(state))
        
      // Fix for the structural typing error
      case _ =>
        command match {
          case cmd: CreateDraft => Effect.reply(cmd.replyTo)(Rejected)
          case cmd: Publish => Effect.reply(cmd.replyTo)(Rejected)
          case cmd: GetPost => Effect.reply(cmd.replyTo)(Rejected)
        }
    }
  }
}

import akka.persistence.r2dbc.state.scaladsl.AdditionalColumn
import io.r2dbc.postgresql.codec.Json

// Title column for simple string queries
class BlogPostTitleColumn extends AdditionalColumn[BlogPost.State, String] {
  override val columnName: String = "title"
  
  override def bind(upsert: AdditionalColumn.Upsert[BlogPost.State]): AdditionalColumn.Binding[String] =
    upsert.value match {
      case BlogPost.BlankState =>
        AdditionalColumn.BindNull
      case s: BlogPost.DraftState =>
        AdditionalColumn.BindValue(s.content.title)
      case s: BlogPost.PublishedState =>
        AdditionalColumn.BindValue(s.content.title)
    }
}

// JSON column for more complex queries
class BlogPostJsonColumn extends AdditionalColumn[BlogPost.State, Json] {
  override val columnName: String = "query_json"
  
  override def bind(upsert: AdditionalColumn.Upsert[BlogPost.State]): AdditionalColumn.Binding[Json] =
    upsert.value match {
      case BlogPost.BlankState =>
        AdditionalColumn.BindNull
      case s: BlogPost.DraftState =>
        val jsonString = s"""{"title": "${s.content.title}", "body": "${s.content.body}", "published": false}"""
        val json = Json.of(jsonString)
        AdditionalColumn.BindValue(json)
      case s: BlogPost.PublishedState =>
        val jsonString = s"""{"title": "${s.content.title}", "body": "${s.content.body}", "published": true}"""
        val json = Json.of(jsonString)
        AdditionalColumn.BindValue(json)
    }
}

import scala.concurrent.{ExecutionContext, Future}
import akka.actor.typed.ActorSystem
import akka.persistence.r2dbc.session.scaladsl.R2dbcSession
import akka.serialization.SerializationExtension

class BlogPostQuery(system: ActorSystem[_]) {
  private implicit val ec: ExecutionContext = system.executionContext
  
  // Query by title
  private val findByTitleSql =
    "SELECT state_ser_id, state_ser_manifest, state_payload " +
    "FROM public.durable_state_blog_post " +
    "WHERE title = $1"
    
  def findByTitle(title: String): Future[IndexedSeq[BlogPost.State]] = {
    R2dbcSession.withSession(system) { session =>
      session.select(session.createStatement(findByTitleSql).bind(0, title)) { row =>
        val serializerId = row.get("state_ser_id", classOf[java.lang.Integer])
        val serializerManifest = row.get("state_ser_manifest", classOf[String])
        val payload = row.get("state_payload", classOf[Array[Byte]])
        val state = SerializationExtension(system)
          .deserialize(payload, serializerId, serializerManifest)
          .get
          .asInstanceOf[BlogPost.State]
        state
      }
    }
  }
  
  // Query using JSON capabilities
  private val findPublishedPostsSql =
    "SELECT state_ser_id, state_ser_manifest, state_payload " +
    "FROM public.durable_state_blog_post " +
    "WHERE query_json->>'published' = 'true'"
    
  def findPublishedPosts(): Future[IndexedSeq[BlogPost.State]] = {
    R2dbcSession.withSession(system) { session =>
      session.select(session.createStatement(findPublishedPostsSql)) { row =>
        val serializerId = row.get("state_ser_id", classOf[java.lang.Integer])
        val serializerManifest = row.get("state_ser_manifest", classOf[String])
        val payload = row.get("state_payload", classOf[Array[Byte]])
        val state = SerializationExtension(system)
          .deserialize(payload, serializerId, serializerManifest)
          .get
          .asInstanceOf[BlogPost.State]
        state
      }
    }
  }
}

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.typed.Cluster
import akka.persistence.r2dbc.query.scaladsl.R2dbcReadJournal
import akka.persistence.typed.PersistenceId
import akka.util.Timeout

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

object BlogPostApp extends App {
  // Create the ActorSystem
  val system = ActorSystem[Nothing](Behaviors.empty, "BlogPostSystem")
  implicit val ec: ExecutionContext = system.executionContext
  implicit val timeout: Timeout = 20.seconds
  implicit val scheduler = system.scheduler
  // In your BlogPostApp
  DatabaseConnectionTest.testConnection(system)
  DatabaseConnectionTest.checkTables(system)
  try {
    // Create a blog post entity
    val blogPostId = "post-1"
    val blogPost = system.systemActorOf(
      BlogPost(blogPostId), 
      s"blogPost-$blogPostId"
    )

    // Create a draft post
    val createDraftFuture = Future {
      import akka.actor.typed.scaladsl.AskPattern._
      blogPost.ask[BlogPost.Response](ref => 
        BlogPost.CreateDraft("My First Post", "This is the content of my first post", ref)
      )
    }.flatten

    createDraftFuture.onComplete {
      case Success(BlogPost.Accepted) =>
        println(s"Draft created for post $blogPostId")
        
        // Publish the post
        val publishFuture = Future {
          import akka.actor.typed.scaladsl.AskPattern._
          blogPost.ask[BlogPost.Response](ref => BlogPost.Publish(ref))
        }.flatten
        
        publishFuture.onComplete {
          case Success(BlogPost.Accepted) =>
            println(s"Post $blogPostId published")
            
            // Get the post
            val getPostFuture = Future {
              import akka.actor.typed.scaladsl.AskPattern._
              blogPost.ask[BlogPost.Response](ref => BlogPost.GetPost(ref))
            }.flatten
            
            getPostFuture.onComplete {
              case Success(BlogPost.PostResponse(state)) =>
                state match {
                  case BlogPost.PublishedState(content) =>
                    println(s"Published post: ${content.title} - ${content.body}")
                  case BlogPost.DraftState(content) =>
                    println(s"Draft post: ${content.title} - ${content.body}")
                  case BlogPost.BlankState =>
                    println("Blank post")
                }
                
                // Terminate the system after all operations
                println("Test completed successfully. Terminating...")
                system.terminate()
                
              case Failure(ex) =>
                println(s"Error getting post: ${ex.getMessage}")
                system.terminate()
              case _ =>
                println("Get post returned unexpected response")
                system.terminate()
            }
            
          case Failure(ex) =>
            println(s"Error publishing post: ${ex.getMessage}")
            system.terminate()
          case _ =>
            println("Publish rejected")
            system.terminate()
        }
        
      case Failure(ex) =>
        println(s"Error creating draft: ${ex.getMessage}")
        system.terminate()
      case _ =>
        println("Create draft rejected")
        system.terminate()
    }
    
  } catch {
    case ex: Exception =>
      println(s"Error in main: ${ex.getMessage}")
      ex.printStackTrace()
      system.terminate()
  }
}

import akka.actor.typed.ActorSystem
import akka.persistence.r2dbc.session.scaladsl.R2dbcSession
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Failure}

object DatabaseConnectionTest {
  def testConnection(system: ActorSystem[_]): Unit = {
    implicit val ec: ExecutionContext = system.executionContext
    
    val testQuery = "SELECT 1 as test"
    
    val result = R2dbcSession.withSession(system) { session =>
      session.select(session.createStatement(testQuery)) { row =>
        row.get("test", classOf[Integer])
      }
    }
    
    result.onComplete {
      case Success(results) => 
        println(s"✅ Database connection successful! Result: ${results.mkString}")
      case Failure(ex) => 
        println(s"❌ Database connection failed: ${ex.getMessage}")
        ex.printStackTrace()
    }
  }

  def checkTables(system: ActorSystem[_]): Future[Unit] = {
  implicit val ec: ExecutionContext = system.executionContext
  
  val query = 
    """
    SELECT table_name 
    FROM information_schema.tables 
    WHERE table_schema = 'public' 
    AND table_name IN ('durable_state', 'durable_state_blog_post')
    """
  
  R2dbcSession.withSession(system) { session =>
    session.select(session.createStatement(query)) { row =>
      row.get("table_name", classOf[String])
    }
  }.map { tables =>
    println(s"Found tables: ${tables.mkString(", ")}")
    if (!tables.contains("durable_state_blog_post")) {
      println("WARNING: durable_state_blog_post table does not exist!")
    }
  }
  }
}