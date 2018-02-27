package com.urban.envbot.commands

import com.urban.envbot.users.UserDetailsFetcher
import me.ramswaroop.jbot.core.slack.Bot
import me.ramswaroop.jbot.core.slack.Controller
import me.ramswaroop.jbot.core.slack.EventType
import me.ramswaroop.jbot.core.slack.models.Event
import me.ramswaroop.jbot.core.slack.models.Message
import org.ocpsoft.prettytime.PrettyTime
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketSession
import redis.clients.jedis.Jedis
import java.time.Instant
import java.util.*
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
        val envKeys = jedis.keys("envbot:${event.channelId}:*")
        for (envKey in envKeys.sorted()) {
            logger.info("Listing details for environment $envKey")
            try {
                val environment = jedis.hget(envKey, "name")
                val userId = jedis.hget(envKey, "user")
                val timestamp = jedis.hget(envKey, "timestamp")

                message.append(message(environment, userId, timestamp))
                message.append("\n")
            } catch(e: Exception) {
                logger.info("Could not list details for $envKey: ${e.message}")
            }
        }

        reply(session, event, Message(message.toString()))
    }

    private fun message(environment: String, userId: String?, timestamp: String?): String {
        return if (userId == null) {
            "${environment.toUpperCase()} : Free"
        } else {
            "${environment.toUpperCase()} : Taken ${parseTimestamp(timestamp)} by `${userDetailsFetcher?.username(userId)}`"
        }
    }

    private fun parseTimestamp(timestamp: String?) : String? {
        val date = Instant.ofEpochMilli(timestamp!!.toLong())
        val prettyTime = PrettyTime()
        return prettyTime.format(Date.from(date))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ListEnvironments::class.java)
    }
}
