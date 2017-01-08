package controllers

import javax.inject._

import akka.actor.ActorSystem
import models._
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc._
import repos.news.NewsRepository

import scala.concurrent.{ExecutionContext, Future}


@Singleton
class NewsController @Inject()(actorSystem: ActorSystem, newsRepository: NewsRepository)
                              (implicit exec: ExecutionContext) extends Controller {
  val logger: Logger = Logger(this.getClass)

  def getNews(skip:Int) = Action.async {
    newsRepository.getAll(skip)
      .map((news: List[(News, Option[User], Option[Drink], Option[Achievement])]) => {
        val newsItemList = news.map(item => NewsWithItems(item._1, item._2,item._3, item._4))
        Ok(Json.toJson(NewsResponse(newsItemList)))
      })
      .recoverWith({
        case e => Future {
          logger.error(e.toString)
          InternalServerError
        }
      })
  }


  def createDrinkNews = Action.async(parse.json[CreateDrinkNewsDto]) { request =>
    val drinkId = request.body.drink
    val newsList = request.body.users.map(userNews => {
      News(userNews.cardinality, NewsType.DRINK, userId = Some(userNews.id), drinkId = Some(drinkId))
    })
    newsRepository
      .insertAll(newsList)
      .map(_ => NoContent)
      .recoverWith({
        case e => Future {
          logger.error(e.toString)
          InternalServerError
        }
      })
  }


}





