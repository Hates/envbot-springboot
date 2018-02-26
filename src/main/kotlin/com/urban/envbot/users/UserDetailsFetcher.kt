package com.urban.envbot.users

import com.github.seratch.jslack.Slack
import com.github.seratch.jslack.api.methods.request.users.UsersInfoRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import redis.clients.jedis.Jedis
import javax.annotation.PostConstruct

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
