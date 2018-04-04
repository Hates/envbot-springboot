package com.urban.envbot.commands

import me.ramswaroop.jbot.core.common.Controller
import me.ramswaroop.jbot.core.common.EventType
import me.ramswaroop.jbot.core.slack.Bot
import me.ramswaroop.jbot.core.slack.models.Event
import me.ramswaroop.jbot.core.slack.models.Message
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketSession
import redis.clients.jedis.Jedis
import java.util.regex.Matcher

@Component
class RemoveEnvironment : Bot() {

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

    @Controller(events = arrayOf(EventType.MESSAGE), pattern = "^remove environment ([a-zA-Z0-9]+)$")
    fun onReceiveMessage(session: WebSocketSession, event: Event, matcher: Matcher) {
        val environment = matcher.group(1).toLowerCase()
        val channelId = event.channelId
        logger.info("Removing environment {} to {}", environment, channelId)

        val jedis = Jedis(redisUrl)
        jedis.del("envbot:${event.channelId}:$environment")

        reply(session, event, Message(message(environment, channelId)))
    }

    private fun message(environment: String, channelId: String): String {
        return "Removing ${environment.toUpperCase()}..."
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RemoveEnvironment::class.java)
    }
}
