package com.github.arnaudj.linkify.slackbot

import com.github.arnaudj.eventdriven.events.Event
import com.github.arnaudj.eventdriven.events.EventSourceData
import com.github.arnaudj.linkify.jiraengine.AppEventHandler
import com.github.arnaudj.linkify.jiraengine.JiraEngineThrottlingStrategy
import com.github.arnaudj.linkify.jiraengine.JiraResolutionEngine
import com.github.arnaudj.linkify.jiraengine.dtos.replies.JiraBotReplyFormat
import com.github.arnaudj.linkify.jiraengine.eventdriven.events.JiraResolvedEvent
import com.github.arnaudj.linkify.jiraengine.eventdriven.events.JiraSeenEvent
import com.github.arnaudj.linkify.jiraengine.eventdriven.mappers.JiraResolvedEventMapperExtendedReply
import com.github.arnaudj.linkify.jiraengine.eventdriven.mappers.JiraResolvedEventMapperShortReply
import com.github.arnaudj.linkify.spi.jira.JiraKeyType
import com.github.salomonbrys.kodein.Kodein
import com.ullink.slack.simpleslackapi.SlackPreparedMessage
import java.util.concurrent.TimeUnit

class BotFacade(kodein: Kodein, workerPoolSize: Int, val appEventHandler: AppEventHandler) {
    private val forwardingHandler = object : AppEventHandler {
        override fun onJiraResolvedEvent(event: JiraResolvedEvent, kodein: Kodein) {
            appEventHandler.onJiraResolvedEvent(event, kodein)
        }
    }
    private val jiraPerChannelTimeThrottlingStrategy = object : JiraEngineThrottlingStrategy {
        override fun shouldThrottle(event: JiraSeenEvent): Boolean {
            val now = System.currentTimeMillis()
            val key = "${event.source.sourceId}-${event.entity.key}"
            val ret = (now - jiraKeyToLastSeen.getOrDefault(key, 0)) < TimeUnit.MINUTES.toMillis(throttlingDelayMinutes)
            jiraKeyToLastSeen[key] = now
            return ret
        }
    }
    private val engine = JiraResolutionEngine(kodein, workerPoolSize, forwardingHandler, jiraPerChannelTimeThrottlingStrategy)
    private val throttlingDelayMinutes = 2L
    private val jiraKeyToLastSeen: MutableMap<JiraKeyType, Long> = mutableMapOf()

    fun handleChatMessage(message: String, source: EventSourceData) = engine.handleMessage(message, source)

    companion object {
        @JvmStatic
        fun createSlackMessageFromEvent(event: Event, configMap: Map<String, Any>, jiraBotReplyFormat: JiraBotReplyFormat): List<SlackPreparedMessage> {
            return when (event) {
                is JiraResolvedEvent ->
                    when (jiraBotReplyFormat) {
                        JiraBotReplyFormat.SHORT -> JiraResolvedEventMapperShortReply().map(event, configMap)
                        JiraBotReplyFormat.EXTENDED -> JiraResolvedEventMapperExtendedReply().map(event, configMap)
                    }
                else -> error("Unsupported event type")
            }
        }

        @JvmStatic
        fun createConfigMap(config: Map<String, Any>): Map<String, Any> = config
    }
}