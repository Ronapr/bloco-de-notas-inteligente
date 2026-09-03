package com.securenotes.app.ai

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.securenotes.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class TokiaAiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun chat(
        messages: List<ChatMessage>,
        model: String = DEFAULT_MODEL,
        temperature: Double = 0.7,
        maxTokens: Int = 1024
    ): AiResult = withContext(Dispatchers.IO) {
        try {
            val apiKey = BuildConfig.TOOKIA_API_KEY
            if (apiKey.isBlank()) {
                return@withContext AiResult.Error(
                    "Chave de API não configurada. " +
                        "Adicione TOOKIA_API_KEY no arquivo local.properties."
                )
            }

            val requestBody = ChatRequest(
                model = model,
                messages = messages,
                temperature = temperature,
                maxTokens = maxTokens
            )

            val jsonBody = gson.toJson(requestBody)
                .toRequestBody(jsonMediaType)

            val request = Request.Builder()
                .url("${BuildConfig.TOOKIA_BASE_URL}/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(jsonBody)
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Sem detalhes do erro"
                return@withContext AiResult.Error(
                    "Erro HTTP ${response.code}: $errorBody"
                )
            }

            val responseBody = response.body?.string()
                ?: return@withContext AiResult.Error("Resposta vazia da API")

            val chatResponse = gson.fromJson(responseBody, ChatResponse::class.java)
            val content = chatResponse.choices.firstOrNull()?.message?.content

            if (content.isNullOrBlank()) {
                return@withContext AiResult.Error("Resposta da IA vazia")
            }

            AiResult.Success(content)
        } catch (e: java.net.UnknownHostException) {
            AiResult.Error("Sem conexão com a internet")
        } catch (e: java.net.SocketTimeoutException) {
            AiResult.Error("Tempo limite de conexão excedido")
        } catch (e: Exception) {
            AiResult.Error("Erro ao comunicar com a IA: ${e.message}")
        }
    }

    suspend fun completeText(
        text: String,
        instruction: String = "Melhore o texto mantendo o sentido original.",
        model: String = DEFAULT_MODEL
    ): AiResult {
        val messages = listOf(
            ChatMessage(role = "system", content = instruction),
            ChatMessage(role = "user", content = text)
        )
        return chat(messages, model = model)
    }

    suspend fun summarize(
        text: String,
        model: String = DEFAULT_MODEL
    ): AiResult {
        val messages = listOf(
            ChatMessage(
                role = "system",
                content = "Você é um assistente que resume textos de forma clara e concisa em português brasileiro."
            ),
            ChatMessage(
                role = "user",
                content = "Resuma o seguinte texto:\n\n$text"
            )
        )
        return chat(messages, model = model)
    }

    suspend fun correctSpelling(
        text: String,
        model: String = DEFAULT_MODEL
    ): AiResult {
        val messages = listOf(
            ChatMessage(
                role = "system",
                content = "Você é um corretor ortográfico em português brasileiro. " +
                    "Retorne apenas o texto corrigido, sem explicações adicionais."
            ),
            ChatMessage(
                role = "user",
                content = "Corrija ortografia e gramática:\n\n$text"
            )
        )
        return chat(messages, model = model)
    }

    suspend fun generateTitle(
        content: String,
        model: String = DEFAULT_MODEL
    ): AiResult {
        val messages = listOf(
            ChatMessage(
                role = "system",
                content = "Gere um título curto e descritivo em português brasileiro para a nota. " +
                    "Retorne APENAS o título, sem aspas ou formatação."
            ),
            ChatMessage(
                role = "user",
                content = content
            )
        )
        return chat(messages, model = model, maxTokens = 100)
    }

    suspend fun testConnection(): AiResult {
        val messages = listOf(
            ChatMessage(role = "user", content = "Responda apenas: OK")
        )
        return chat(messages, maxTokens = 10)
    }

    companion object {
        const val DEFAULT_MODEL = "gpt-3.5-turbo"
    }
}

data class ChatMessage(
    val role: String,
    val content: String
)

private data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.7,
    @com.google.gson.annotations.SerializedName("max_tokens")
    val maxTokens: Int = 1024
)

private data class ChatResponse(
    val choices: List<Choice>
)

private data class Choice(
    val message: ChatMessage
)

sealed class AiResult {
    data class Success(val content: String) : AiResult()
    data class Error(val message: String) : AiResult()
}
