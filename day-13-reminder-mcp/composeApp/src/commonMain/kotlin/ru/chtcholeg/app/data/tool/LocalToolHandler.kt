package ru.chtcholeg.app.data.tool

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import ru.chtcholeg.app.data.model.FewShotExample
import ru.chtcholeg.app.data.model.GigaChatFunction
import ru.chtcholeg.app.domain.model.McpToolResult
import ru.chtcholeg.app.domain.model.ReminderConfig
import ru.chtcholeg.app.domain.model.ReminderInterval
import ru.chtcholeg.app.presentation.reminder.ReminderIntent
import ru.chtcholeg.app.presentation.reminder.ReminderStore

class LocalToolHandler(
    private val reminderStoreProvider: () -> ReminderStore
) {
    private val reminderStore: ReminderStore get() = reminderStoreProvider()

    companion object {
        val TOOL_NAMES = setOf("setup_reminder", "update_reminder", "stop_reminder")

        val LOCAL_TOOL_DEFINITIONS = listOf(
            GigaChatFunction(
                name = "setup_reminder",
                description = """Настроить периодический мониторинг Telegram-канала с автоматической суммаризацией новых сообщений.
Вызывай этот инструмент когда пользователь просит:
- мониторить канал / следить за каналом
- получать саммари / дайджест из канала
- уведомления о новых сообщениях в канале
- автоматически читать канал

Параметры:
- channel: username канала без знака @ (например "durov", не "@durov")
- interval_seconds: интервал проверки в секундах. Допустимые значения: 10, 30, 60, 300, 600, 1800, 3600
- message_count: сколько последних сообщений учитывать при саммаризации (от 1 до 30, по умолчанию 10)""",
                parameters = buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("channel", buildJsonObject {
                            put("type", "string")
                            put("description", "Username Telegram-канала без знака @ (например durov)")
                        })
                        put("interval_seconds", buildJsonObject {
                            put("type", "integer")
                            put("description", "Интервал проверки в секундах (10, 30, 60, 300, 600, 1800, 3600)")
                        })
                        put("message_count", buildJsonObject {
                            put("type", "integer")
                            put("description", "Количество сообщений для саммари (1-30, по умолчанию 10)")
                        })
                    })
                    put("required", buildJsonArray {
                        add(JsonPrimitive("channel"))
                        add(JsonPrimitive("interval_seconds"))
                    })
                },
                fewShotExamples = listOf(
                    FewShotExample(
                        request = "Мониторь канал @durov каждые 30 секунд",
                        params = buildJsonObject {
                            put("channel", "durov")
                            put("interval_seconds", 30)
                            put("message_count", 10)
                        }
                    ),
                    FewShotExample(
                        request = "Начинай следить за каналом techcrunch каждую минуту, берём 5 сообщений",
                        params = buildJsonObject {
                            put("channel", "techcrunch")
                            put("interval_seconds", 60)
                            put("message_count", 5)
                        }
                    )
                )
            ),

            GigaChatFunction(
                name = "update_reminder",
                description = """Обновить параметр существующего напоминания/мониторинга.
Вызывай когда пользователь просит изменить:
- интервал проверки ("увеличь до 5 минут", "проверяй каждый час")
- количество сообщений ("берёмся за 20 сообщений")
- канал ("переключись на другой канал")""",
                parameters = buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {
                        put("field", buildJsonObject {
                            put("type", "string")
                            put("description", "Поле для изменения: channel, interval_seconds, message_count")
                        })
                        put("value", buildJsonObject {
                            put("type", "string")
                            put("description", "Новое значение (строка, будет парсена автоматически)")
                        })
                    })
                    put("required", buildJsonArray {
                        add(JsonPrimitive("field"))
                        add(JsonPrimitive("value"))
                    })
                },
                fewShotExamples = listOf(
                    FewShotExample(
                        request = "Увеличь интервал до 5 минут",
                        params = buildJsonObject {
                            put("field", "interval_seconds")
                            put("value", "300")
                        }
                    )
                )
            ),

            GigaChatFunction(
                name = "stop_reminder",
                description = """Остановить активный мониторинг / напоминание.
Вызывай когда пользователь просит:
- остановить мониторинг / напоминание
- отменить слежение за каналом
- выключить саммари
- больше не присылай уведомления""",
                parameters = buildJsonObject {
                    put("type", "object")
                    put("properties", buildJsonObject {})
                },
                fewShotExamples = listOf(
                    FewShotExample(
                        request = "Остановь мониторинг",
                        params = buildJsonObject {}
                    )
                )
            )
        )
    }

    suspend fun execute(
        toolName: String,
        arguments: JsonElement,
        currentSessionId: String?
    ): McpToolResult {
        val args = when (arguments) {
            is JsonObject -> arguments
            else -> return McpToolResult(content = "Invalid arguments format", isError = true)
        }

        return when (toolName) {
            "setup_reminder" -> handleSetupReminder(args, currentSessionId)
            "update_reminder" -> handleUpdateReminder(args)
            "stop_reminder" -> handleStopReminder()
            else -> McpToolResult(content = "Unknown local tool: $toolName", isError = true)
        }
    }

    private fun handleSetupReminder(args: JsonObject, sessionId: String?): McpToolResult {
        val channel = (args["channel"] as? JsonPrimitive)?.content
            ?: return McpToolResult(content = "Missing parameter: channel", isError = true)

        val intervalSeconds = (args["interval_seconds"] as? JsonPrimitive)?.content?.toIntOrNull() ?: 30
        val messageCount = (args["message_count"] as? JsonPrimitive)?.content?.toIntOrNull() ?: 10

        val config = ReminderConfig(
            channel = channel.removePrefix("@"),
            interval = ReminderInterval.fromSeconds(intervalSeconds),
            messageCount = messageCount.coerceIn(1, 30),
            enabled = true,
            sessionId = sessionId
        )

        reminderStore.dispatch(ReminderIntent.Start(config))

        return McpToolResult(
            content = """{"status": "started", "channel": "@${config.channel}", "interval": "${config.interval.displayName}", "message_count": ${config.messageCount}}""",
            isError = false
        )
    }

    private fun handleUpdateReminder(args: JsonObject): McpToolResult {
        val field = (args["field"] as? JsonPrimitive)?.content
            ?: return McpToolResult(content = "Missing parameter: field", isError = true)
        val value = (args["value"] as? JsonPrimitive)?.content
            ?: return McpToolResult(content = "Missing parameter: value", isError = true)

        reminderStore.dispatch(ReminderIntent.UpdateConfig(field, value))

        return McpToolResult(
            content = """{"status": "updated", "field": "$field", "value": "$value"}""",
            isError = false
        )
    }

    private fun handleStopReminder(): McpToolResult {
        reminderStore.dispatch(ReminderIntent.Stop)

        return McpToolResult(
            content = """{"status": "stopped"}""",
            isError = false
        )
    }
}
