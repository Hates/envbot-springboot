package com.urban.envbot

import com.github.seratch.jslack.Slack
import com.github.seratch.jslack.api.methods.request.users.UsersInfoRequest
import me.ramswaroop.jbot.core.common.Controller
import me.ramswaroop.jbot.core.common.EventType
import me.ramswaroop.jbot.core.slack.Bot
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
class AddEnvironment : Bot() {

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

    @Controller(events = arrayOf(EventType.MESSAGE), pattern = "^add environment ([a-zA-Z0-9]+)$")
    fun onReceiveMessage(session: WebSocketSession, event: Event, matcher: Matcher) {
        val environment = matcher.group(1).toLowerCase()
        val channelId = event.channelId
        logger.info("Adding environment {} to {}", environment, channelId)

        val jedis = Jedis(redisUrl)
        jedis.hset("envbot:${event.channelId}:$environment", "name", environment)
        jedis.hset("envbot:${event.channelId}:$environment", "created", java.lang.Long.toString(Instant.now().epochSecond))

        reply(session, event, Message(message(environment, channelId)))
    }

    private fun message(environment: String, channelId: String): String {
        return "Adding ${environment.toUpperCase()}..."
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AddEnvironment::class.java)
    }
}

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

@Component
class ReleaseEnvironment : Bot() {

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

    @Controller(events = arrayOf(EventType.MESSAGE), pattern = "^(?:done|finished).*?([a-zA-Z0-9]+)$")
    fun onReceiveMessage(session: WebSocketSession, event: Event, matcher: Matcher) {
        val environment = matcher.group(1).toLowerCase()
        val userId = event.userId
        logger.info("Releasing environment $environment by $userId")

        val jedis = Jedis(redisUrl)
        jedis.hdel("envbot:${event.channelId}:$environment", "user")

        reply(session, event, Message(message(environment, userId)))
    }

    private fun message(environment: String, userId: String): String {
        return "`${userDetailsFetcher?.username(userId)}` is done with ${environment.toUpperCase()}"
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ReleaseEnvironment::class.java)
    }
}

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

@Component
class UserDetailsFetcher {
    @Value("\${redisUrl}")
    private val redisUrl: String? = null

    @Value("\${slackBotToken}")
    private val slackToken: String? = null

    fun username(userId: String) : String {
        val jedis = Jedis(redisUrl)

        logger.info("Fetching username for $userId")
        var userName = jedis.get("usernames:$userId")

        if(userName.isNullOrBlank()) {
            logger.info("No cached username found for $userId : $userName")
            val slack = Slack.getInstance()
            val response = slack.methods().usersInfo(UsersInfoRequest.builder().token(slackToken).user(userId).build())

            userName = response.user.name
            jedis.set("usernames:$userId", userName)
        }

        return userName
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UserDetailsFetcher::class.java)
    }
}
