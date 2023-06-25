package org.fuchss.matrix.joinlink

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import net.folivo.trixnity.core.model.UserId
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

/**
 * This is the configuration template of the mensa bot.
 * @param[prefix] the command prefix the bot listens to. By default, "mensa"
 * @param[baseUrl] the base url of the matrix server the bot shall use
 * @param[username] the username of the bot's account
 * @param[password] the password of the bot's account
 * @param[users] the matrix ids of the authorized users or servers. E.g. "@user:invalid.domain" or ":invalid.domain"
 */
data class Config(
    @JsonProperty val prefix: String = "join",
    @JsonProperty val baseUrl: String,
    @JsonProperty val username: String,
    @JsonProperty val password: String,
    @JsonProperty val users: List<String>
) {
    companion object {
        private val log: Logger = LoggerFactory.getLogger(Config::class.java)

        /**
         * Load the config from the file path. You can set "CONFIG_PATH" in the environment to override the default location ("./config.json").
         */
        fun load(): Config {
            val configPath = System.getenv("CONFIG_PATH") ?: "./config.json"
            val configFile = File(configPath)
            if (!configFile.exists()) {
                error("Config ${configFile.absolutePath} does not exist!")
            }

            val config: Config = ObjectMapper().registerKotlinModule().registerModule(JavaTimeModule()).readValue(configFile)
            log.info("Loaded config ${configFile.absolutePath}")
            return config
        }
    }

    /**
     * Determine whether a user id belongs to an authorized user.
     * @param[user] the user id to check
     * @return indicator for authorization
     */
    fun isUser(user: UserId?): Boolean {
        if (user == null) {
            return false
        }
        if (users.isEmpty()) {
            return true
        }
        return users.any { user.full.endsWith(it) }
    }
}
