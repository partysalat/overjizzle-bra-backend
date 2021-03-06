package websocket

import akka.actor.ActorSystem
import com.google.inject.{Inject, Singleton}
import news.repos.NewsRepository
import websocket.WebsocketActor.{NotifyNews, NotifyNewsRemove, NotifyReloadImage}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class WebsocketService @Inject()(     newsRepository: NewsRepository,
                                  manager: WebsocketManager
                                )
                                (implicit ec: ExecutionContext, system: ActorSystem) {
  def notify(newsIds:Seq[Int]): Future[Unit] = {
    newsRepository.getNewsByIds(newsIds)
      .map(newsWithItems=>
        manager.broadcast(NotifyNews(newsWithItems))
      )
  }

  def notifyNewsRemove(newsId:Int): Unit ={
    manager.broadcast(NotifyNewsRemove(newsId))
  }

  def notifyReloadPhotoStream(base64Image:String): Unit ={
    manager.broadcast(NotifyReloadImage(base64Image))
  }
}
