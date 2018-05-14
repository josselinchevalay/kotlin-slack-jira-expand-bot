import com.github.arnaudj.linkify.slackbot.BotFacade
import com.github.arnaudj.linkify.slackbot.eventdriven.events.JiraResolvedEvent
import com.github.arnaudj.linkify.slackbot.eventdriven.mappers.JiraBotReplyFormat
import com.github.arnaudj.linkify.spi.jira.JiraEntity
import com.ullink.slack.simpleslackapi.SlackField
import org.junit.Assert
import org.junit.Test

class JiraEventMapperTest : JiraTestBase() {
    val jiraBotReplyFormatExtended = JiraBotReplyFormat.EXTENDED
    val jiraEntity1 = JiraEntity(key = "JIRA-1234",
            jiraIssueBrowseURL = "http://localhost/browse",
            summary = "A subtask with some summary here",
            fieldsMap = mapOf(
                    "summary" to "Some summary here",
                    "created" to "2017-03-17T15:37:10.000+0100",
                    "updated" to "2017-07-17T10:42:55.000+0200",
                    "status.name" to "Closed",
                    "priority.name" to "Minor",
                    "reporter.name" to "jdoe",
                    "assignee.name" to "noone"
            ))

    @Test
    fun `(extended reply format) Test event mapper`() {
        setupConfigMap(jiraResolveWithAPI = false)
        val event = JiraResolvedEvent("uuid1", jiraEntity1)
        val preparedMessages = BotFacade.createSlackMessageFromEvent(event, configMap, jiraBotReplyFormatExtended)

        Assert.assertEquals(1, preparedMessages.size)
        val pm = preparedMessages[0]
        Assert.assertEquals(1, pm.attachments.size)
        with(pm.attachments[0]) {
            Assert.assertEquals("JIRA-1234: A subtask with some summary here", title)
            Assert.assertEquals("A subtask with some summary here", fallback)
            Assert.assertEquals("", text)
            Assert.assertEquals("", pretext)
            Assert.assertEquals("<!date^1500280975^Updated: {date_num} {time_secs}|2017-07-17T10:42:55.000+0200>", footer)
            Assert.assertEquals("[Priority=Minor, Status=Closed, Reporter=jdoe, Assignee=noone]", expandFields(fields))
        }
    }

    fun expandFields(fields: List<SlackField>) = fields.map { "${it.title}=${it.value}" }.toString()
}
