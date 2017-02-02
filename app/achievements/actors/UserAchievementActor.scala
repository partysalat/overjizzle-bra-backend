package achievements.actors

import achievements.AchievementDefinitions
import achievements.models.Achievement
import achievements.repos.AchievementsRepository
import akka.actor.{Actor, Props, Stash}
import drinks.models.DrinkType
import drinks.repos.DrinksRepository
import news.models.{News, NewsStats, NewsType}
import news.repos.NewsRepository
import play.api.Logger
import websocket.WebsocketService

import scala.concurrent.{ExecutionContext, Future}

object UserAchievementActor {
  def props(userId: Int, newsStats: NewsStats, newsRepository: NewsRepository, drinksRepository: DrinksRepository, achievementsRepository: AchievementsRepository, websocketService: WebsocketService)(implicit executionContext: ExecutionContext) = {
    Props(new UserAchievementActor(userId, newsStats, newsRepository, drinksRepository, achievementsRepository, websocketService))
  }

  case class ProcessDrinkNews(news: News)

}


class UserAchievementActor(userId: Int, newsStats: NewsStats, newsRepository: NewsRepository, drinksRepository: DrinksRepository, achievementsRepository: AchievementsRepository, websocketService: WebsocketService)
                          (implicit ec: ExecutionContext) extends Actor with Stash {

  import UserAchievementActor._
  import scala.collection.mutable

  var achievementMetrics: AchievementMetrics = AchievementMetrics()

  override def preStart = {
    Logger.debug(s"Started Actor with userId $userId")
    initializeAchievementMetrics.onSuccess({
      case _ => self ! InitializationDone
    })
  }

  def receive = initialReceive

  def initialReceive: Receive = {
    case InitializationDone => {
      context.become(normalReceive)
      unstashAll()
    }
    case _ => stash()
  }

  def normalReceive: Receive = {
    case ProcessDrinkNews(news) if news.userId.contains(userId) && news.`type` == NewsType.DRINK =>
      Logger.debug(s"actor $userId received news $news")
        Logger.info(s"User $userId has drink with id ${news.drinkId}")
        increaseCounters(news)
          .map(_ => achievementMetrics.checkAchievements)
          .map(addAchievementsToUser)
    case _ => ()
  }

  def addAchievementsToUser(unlockedAchievements: List[AchievementConstraints]) = {
    Logger.info(s"User $userId has unlocked achievements ${unlockedAchievements.toString()}")

    achievementsRepository
      .getByNames(unlockedAchievements.map(_.achievement.name))
      .flatMap {
        achievements: List[Achievement] => {
          val achievementNewsList = achievements.map(achievement => News(1, NewsType.ACHIEVEMENT, userId = Some(userId), achievementId = achievement.id))
          newsRepository.insertAll(achievementNewsList)
        }
      }.map(websocketService.notify)

  }

  def increaseCounters(news: News): Future[Unit] = {
    import Property._
    achievementMetrics.addValue(anyDrinkProperties.keySet.toList, news.cardinality)
    drinksRepository.getById(news.drinkId.get).map(_.`type`).map {
      case DrinkType.BEER => achievementMetrics.addValue(beerProperties.keySet.toList, news.cardinality)
      case DrinkType.COCKTAIL => achievementMetrics.addValue(cocktailProperties.keySet.toList, news.cardinality)
      case DrinkType.SHOT => achievementMetrics.addValue(shotProperties.keySet.toList, news.cardinality)
      case DrinkType.SOFTDRINK => achievementMetrics.addValue(softdrinkProperties.keySet.toList, news.cardinality)
      case _ => ()
    }
  }

  def initializeAchievementMetrics: Future[Unit] = {
    import Property._

    AchievementDefinitions.achievements
      .map(_.copy())
      .foreach(achievementMetrics.defineAchievement)

    initCounter(anyDrinkProperties, newsStats.drinkCount.getOrElse(0))
    initCounter(beerProperties, newsStats.beerCount.getOrElse(0))
    initCounter(cocktailProperties, newsStats.cocktailCount.getOrElse(0))
    initCounter(shotProperties, newsStats.shotCount.getOrElse(0))
    initCounter(softdrinkProperties, newsStats.softdrinkCount.getOrElse(0))

    val previousAchievements = achievementMetrics.checkAchievements
    Logger.debug(s"Initial unlocked achievements: ${previousAchievements.toString()}")
    Future.successful((): Unit)
  }

  private def initCounter(properties: mutable.Map[String, Property], value: Int) = {
    properties.foreach {
      case (_, property) => achievementMetrics.defineProperty(property.copy(initialValue = value, value = value))
    }

  }

  case class InitializationDone()

}