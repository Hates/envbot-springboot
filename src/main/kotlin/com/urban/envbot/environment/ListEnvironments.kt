package com.urban.envbot.environment

import com.urban.envbot.users.UserDetailsFetcher
import me.ramswaroop.jbot.core.slack.Bot
import me.ramswaroop.jbot.core.slack.Controller
import me.ramswaroop.jbot.core.slack.EventType
import me.ramswaroop.jbot.core.slack.models.Event
import me.ramswaroop.jbot.core.slack.models.Message
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketSession
import redis.clients.jedis.Jedis
import java.util.regex.Matcher

@Component
class ListEnvironments : Bot() {

    @Autowired
    private val userDetailsFetcher: UserDetailsFetcher? = null

    @Value("\${redisUrl}")
    private val redisUrl: String? = null

    @Value("\${slackBotToken}")
    private val slackToken: String? = null

    override fun getSlackToken(): String? {
        return slackToken
    }

    override fun getSlackBot(): Bot {
        return this
    }

    @Controller(events = arrayOf(EventType.MESSAGE), pattern = "^envs$")
    fun onReceiveMessage(session: WebSocketSession, event: Event, matcher: Matcher) {
        logger.info("Listing environments")

        val message = StringBuilder()
        message.append("*Current Environments*\n")

        val jedis = Jedis(redisUrl)
        val envKeys = jedis.keys("${event.channelId}:*")
        for (envKey in envKeys.sorted()) {
            logger.info("Listing details for environment $envKey")
            try {
                val environment = jedis.hget(envKey, "name")
                val userId = jedis.hget(envKey, "user")

                message.append(message(environment, userId))
                message.append("\n")
            } catch(e: Exception) {
                logger.info("Could not list details for $envKey: ${e.message}")
            }
        }

        reply(session, event, Message(message.toString()))
    }

    private fun message(environment: String, userId: String?): String {
        return if (userId == null) {
            "${environment.toUpperCase()} : Free"
        } else {
            "${environment.toUpperCase()} : Taken by `${userDetailsFetcher?.username(userId)}`"
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ListEnvironments::class.java)
    }
}
