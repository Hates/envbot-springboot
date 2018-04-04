package com.urban.envbot.commands

import com.urban.envbot.users.UserDetailsFetcher
import me.ramswaroop.jbot.core.common.Controller
import me.ramswaroop.jbot.core.common.EventType
import me.ramswaroop.jbot.core.slack.Bot
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
class ReserveEnvironment : Bot() {

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

    @Controller(events = arrayOf(EventType.MESSAGE), pattern = "^(?:take|taking|using|grabbing) ([a-zA-Z0-9]+)$")
    fun onReceiveMessage(session: WebSocketSession, event: Event, matcher: Matcher) {
        val environment = matcher.group(1).toLowerCase()
        val userId = event.userId
        logger.info("Reserving environment $environment by $userId")

        val jedis = Jedis(redisUrl)
        jedis.hset("envbot:${event.channelId}:$environment", "user", userId)
        jedis.hset("envbot:${event.channelId}:$environment", "timestamp", System.currentTimeMillis().toString())

        reply(session, event, Message(message(environment, userId)))
    }

    private fun message(environment: String, userId: String): String {
        return "${environment.toUpperCase()} taken by `${userDetailsFetcher?.username(userId)}`"
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ReserveEnvironment::class.java)
    }
}
