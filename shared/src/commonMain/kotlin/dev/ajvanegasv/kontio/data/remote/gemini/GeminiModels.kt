package dev.ajvanegasv.kontio.data.remote.gemini

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeminiRequest(
    val contents: List<GeminiContent>,
    @SerialName("system_instruction")
    val systemInstruction: GeminiContent? = null,
    val generationConfig: GeminiGenerationConfig? = null
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
    val inlineData: GeminiInlineData? = null
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

