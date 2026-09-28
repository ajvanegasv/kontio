package dev.ajvanegasv.kontio.data.remote.gemini

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class GeminiRequest(
    val contents: List<GeminiContent>,
    @SerialName("system_instruction")
    val systemInstruction: GeminiContent? = null,
    val generationConfig: GeminiGenerationConfig? = null,
    val tools: List<GeminiTool>? = null
)

@Serializable
data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart>
)

@Serializable
data class GeminiPart(
    val text: String? = null,
    @SerialName("inline_data")
    val inlineData: GeminiInlineData? = null,
    val functionCall: GeminiFunctionCall? = null,
    val functionResponse: GeminiFunctionResponse? = null
)

@Serializable
data class GeminiFunctionCall(
    val name: String,
    val args: Map<String, JsonElement>? = null
)

@Serializable
data class GeminiFunctionResponse(
    val name: String,
    val response: JsonObject
)

@Serializable
data class GeminiTool(
    @SerialName("function_declarations")
    val functionDeclarations: List<GeminiFunctionDeclaration>? = null
)

@Serializable
data class GeminiFunctionDeclaration(
    val name: String,
    val description: String,
    val parameters: GeminiFunctionParameters? = null
)

@Serializable
data class GeminiFunctionParameters(
    val type: String = "OBJECT",
    val properties: Map<String, GeminiFunctionProperty> = emptyMap(),
    val required: List<String> = emptyList()
)

@Serializable
data class GeminiFunctionProperty(
    val type: String,
    val description: String? = null,
    val enum: List<String>? = null
)

@Serializable
data class GeminiInlineData(
    @SerialName("mime_type")
    val mimeType: String,
    val data: String // Base64
)

@Serializable
data class GeminiGenerationConfig(
    @SerialName("response_mime_type")
    val responseMimeType: String? = null,
    val temperature: Float? = null
)

@Serializable
data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null,
    val error: GeminiError? = null
)

@Serializable
data class GeminiCandidate(
    val content: GeminiContent? = null,
    val finishReason: String? = null
)

@Serializable
data class GeminiError(
    val code: Int? = null,
    val message: String? = null,
    val status: String? = null
)

@Serializable
data class RawStatementAnalysis(
    val detectedBank: String? = null,
    val detectedAccountNumber: String? = null,
    val currency: String? = null,
    val transactions: List<RawStatementTransaction> = emptyList()
)

@Serializable
data class RawStatementTransaction(
    val date: String,
    val rawDescription: String,
    val cleanTitle: String,
    val amount: Double,
    val type: String,
    val suggestedCategoryId: String? = null,
    val confidence: Float = 0.85f,
    val notes: String? = null
)

@Serializable
data class GeminiModelListResponse(
    val models: List<GeminiModelInfo>? = null,
    val error: GeminiError? = null
)

@Serializable
data class GeminiModelInfo(
    val name: String,
    val displayName: String? = null,
    val description: String? = null,
    val supportedGenerationMethods: List<String> = emptyList()
)

