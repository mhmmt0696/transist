package com.transist.data.repository

import android.util.Log
import com.transist.data.remote.api.GeminiApi
import com.transist.data.remote.request.ContentRequest
import com.transist.data.remote.request.GenerateContentRequest
import com.transist.data.remote.request.PartRequest
import com.transist.data.remote.response.MisspelledWord
import com.transist.data.remote.response.MisspelledWords
import com.transist.data.remote.response.RandomSentencesResponse
import com.transist.data.remote.response.Sentence
import com.transist.data.remote.response.SentencesResponse
import com.transist.util.extractJson
import com.transist.util.extractWordsAsJson
import com.transist.util.getLocale
import com.transist.util.handleApiResponse
import com.transist.util.handleApiResponseDefault
import com.transist.util.handleApiResponseNoTranslation
import com.transist.util.handleApiResponseWithBlank
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.transist.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.retryWhen
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ApiRepository {

    val baseUrl = BuildConfig.BACKEND_URL

    val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl) // Cloud Run URL veya localhost:3000/
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val geminiApi: GeminiApi = retrofit.create(GeminiApi::class.java)

    fun sendQueryFindSentences(
        expression: String,
        meaning: String,
        note: String,
        targetLanguage: String,
        nativeLanguage: String
    ): Flow<MutableList<Sentence>> = flow {
        var queryText =
            "Think about the usages of '$expression' in $targetLanguage and then " +
                    "please generate 12 sentences in $targetLanguage using the expression '$expression'."

        if (meaning.isNotEmpty()) queryText += " In the meaning of '$meaning'."
        if (note.isNotEmpty()) queryText += " Consider the note: '$note'."
        queryText += " For each sentence, also provide a $nativeLanguage translation."
        if (note.isNotEmpty()) queryText += " In your translations, translate $expression as $meaning. "
        queryText +="""
            The $nativeLanguage equivalent of \"$expression\" must be of the same type (Verb-verb, noun-noun, adjective-adjective, adverb-adverb, etc.).
            If the expression is in the passive voice, you must use a single passive equivalent in the $nativeLanguage translation.
            Even if the query contains logical errors, in any case, construct the sentences in the most appropriate way and write the necessary explanation in the "explanation" section.
            Return the result in the following JSON format without any additional text: 
            { "explanation": "Explanation in $nativeLanguage language.",
                "sentences": [
                    {"sentence": "First example sentence in $targetLanguage.", "translation": "Corresponding natural translation in $nativeLanguage."},
                    {"sentence": "Second example sentence in $targetLanguage.", "translation": "Corresponding natural translation in $nativeLanguage."},
                    {"sentence": "Third example sentence in $targetLanguage.", "translation": "Corresponding natural translation in $nativeLanguage."},
                    {"sentence": "Fourth example sentence in $targetLanguage.", "translation": "Corresponding natural translation in $nativeLanguage."},
                    {"sentence": "Fifth example sentence in $targetLanguage.", "translation": "Corresponding natural translation in $nativeLanguage."},
                    {"sentence": "Sixth example sentence in $targetLanguage.", "translation": "Corresponding natural translation in $nativeLanguage."},
                    {"sentence": "Seventh example sentence in $targetLanguage.", "translation": "Corresponding natural translation in $nativeLanguage."},
                    {"sentence": "Eighth example sentence in $targetLanguage.", "translation": "Corresponding natural translation in $nativeLanguage."},
                    {"sentence": "Ninth example sentence in $targetLanguage.", "translation": "Corresponding natural translation in $nativeLanguage."},
                    {"sentence": "Tenth example sentence in $targetLanguage.", "translation": "Corresponding natural translation in $nativeLanguage."},
                    {"sentence": "Eleventh example sentence in $targetLanguage.", "translation": "Corresponding natural translation in $nativeLanguage."},
                    {"sentence": "Twelfth example sentence in $targetLanguage.", "translation": "Corresponding natural translation in $nativeLanguage."}
                ]
            }
        """.trimIndent()

        val requestBody = GenerateContentRequest(
            contents = listOf(
                ContentRequest(parts = listOf(PartRequest(text = queryText)))
            )
        )

        try {
            val response = geminiApi.getResponse(requestBody)
            if (response.isSuccessful && response.body() != null) {
                var text = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                Log.d("API_RESPONSE", "Response text: $text")
                if (!text.isNullOrEmpty()) {
                    text = text.replace("json", "").replace("```", "").trim()
                    val gson = Gson()
                    val sentencesResponse = gson.fromJson(text, SentencesResponse::class.java)
                    emit(sentencesResponse.sentences)
                } else {
                    throw Exception("No text found in API response")
                }
            } else {
                throw Exception("API error: ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            throw e
        }
    }.retryWhen { cause, attempt ->
        if (attempt < 3) {
            Log.e("API_RETRY", "Retrying sendQueryFindSentences: attempt $attempt, error: ${cause.message}")
            delay(1000)
            true
        } else {
            false
        }
    }.catch { e ->
        Log.e("API_ERROR", "sendQueryFindSentences failed: ${e.message}")
        emit(emptyList<Sentence>().toMutableList()) // başarısızsa boş liste döndür
    }.flowOn(Dispatchers.IO)

    fun sendQueryCheckMeaning(
        expression: String,
        meaning: String,
        targetLanguage: String,
        nativeLanguage: String
    ): Flow<Boolean> = flow {
        val queryText = """
            Does the $targetLanguage expression "$expression" mean the $nativeLanguage expression "$meaning"? 
            
            Return the result in the following JSON format without any additional text: 
            
            { "isValid": "true or false",
              "feedBack": "your feedback here." }
        """.trimIndent()

        val requestBody = GenerateContentRequest(
            contents = listOf(
                ContentRequest(parts = listOf(PartRequest(text = queryText)))
            )
        )

        try {
            val response = geminiApi.getResponse(requestBody)
            if (response.isSuccessful && response.body() != null) {
                var text = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!text.isNullOrEmpty()) {
                    text = text.replace("json", "").replace("```", "").trim()
                    val isValid = JSONObject(text).getBoolean("isValid")
                    emit(isValid)
                } else {
                    throw Exception("No text found in API response")
                }
            } else {
                throw Exception("API error: ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            throw e
        }
    }.retryWhen { cause, attempt ->
        // max 3 retry
        if (attempt < 3) {
            Log.e("API_RETRY", "Retrying sendQueryCheckMeaning: attempt $attempt, error: ${cause.message}")
            delay(500) // Yarım saniye bekle
            true
        } else {
            false
        }
    }.catch { e ->
        Log.e("API_ERROR", "sendQueryCheckMeaning failed: ${e.message}")
        emit(false) // başarısızsa false döndür
    }.flowOn(Dispatchers.IO)

    fun sendQueryCheckLanguage(
        expression: String,
        targetLanguage: String
    ): Flow<Boolean> = flow {
        val queryText = """
            Please return true if the text in JSON below is in $targetLanguage, even if it has errors, otherwise return false: 
             { "text": "$expression" } 
              Give your response strictly in the following JSON format: 
               { isValid: "true or false", feedBack: "your feedback here."} """.trimIndent()

        val requestBody = GenerateContentRequest(
            contents = listOf(
                ContentRequest(parts = listOf(PartRequest(text = queryText)))
            )
        )

        try {
            val response = geminiApi.getResponse(requestBody)
            if (response.isSuccessful && response.body() != null) {
                var text = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!text.isNullOrEmpty()) {
                    text = text.replace("json", "").replace("```", "").trim()
                    val isValid = JSONObject(text).getBoolean("isValid")
                    emit(isValid)
                } else {
                    throw Exception("No text found in API response")
                }
            } else {
                throw Exception("API error: ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            throw e
        }
    }.retryWhen { cause, attempt ->
        // max 3 retry
        if (attempt < 3) {
            Log.e("API_RETRY", "Retrying sendQueryCheckMeaning: attempt $attempt, error: ${cause.message}")
            delay(500) // Yarım saniye bekle
            true
        } else {
            false
        }
    }.catch { e ->
        Log.e("API_ERROR", "sendQueryCheckMeaning failed: ${e.message}")
        emit(false) // başarısızsa false döndür
    }.flowOn(Dispatchers.IO)

    fun sendQueryCheckSpelling(
        expression: String,
        targetLanguage: String
    ): Flow<Boolean> = flow {
        val queryText = """
            Does the $targetLanguage expression "$expression" have any spelling errors like letter omissions or incorrect character order? 
            Only criteria is letter omissions or character order. 
            Other shits like grammatical issues or sentence structure issues or unclear meaning issues are nothing much to consider at all. 
            Return the result in the following JSON format without any additional text: 
             { isValid: "true or false", 
                feedBack: "your feedback here." } 
            """.trimIndent()

        val requestBody = GenerateContentRequest(
            contents = listOf(
                ContentRequest(parts = listOf(PartRequest(text = queryText)))
            )
        )

        try {
            val response = geminiApi.getResponse(requestBody)
            if (response.isSuccessful && response.body() != null) {
                var text = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!text.isNullOrEmpty()) {
                    text = text.replace("json", "").replace("```", "").trim()
                    val isValid = JSONObject(text).getBoolean("isValid")
                    emit(isValid)
                } else {
                    throw Exception("No text found in API response")
                }
            } else {
                throw Exception("API error: ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            throw e
        }
    }.retryWhen { cause, attempt ->
        // max 3 retry
        if (attempt < 3) {
            Log.e("API_RETRY", "Retrying sendQueryCheckMeaning: attempt $attempt, error: ${cause.message}")
            delay(500) // Yarım saniye bekle
            true
        } else {
            false
        }
    }.catch { e ->
        Log.e("API_ERROR", "sendQueryCheckMeaning failed: ${e.message}")
        emit(false) // başarısızsa false döndür
    }.flowOn(Dispatchers.IO)

    fun sendQueryCheckCommonness(
        expression: String,
        targetLanguage: String
    ): Flow<Boolean> = flow {
        val queryText = """
            Is "$expression" a common expression in $targetLanguage?  
              Give your response strictly in the following JSON format: 
               { isValid: "true or false", feedBack: "your feedback here."} """.trimIndent()

        val requestBody = GenerateContentRequest(
            contents = listOf(
                ContentRequest(parts = listOf(PartRequest(text = queryText)))
            )
        )

        try {
            val response = geminiApi.getResponse(requestBody)
            if (response.isSuccessful && response.body() != null) {
                var text = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!text.isNullOrEmpty()) {
                    text = text.replace("json", "").replace("```", "").trim()
                    val isValid = JSONObject(text).getBoolean("isValid")
                    emit(isValid)
                } else {
                    throw Exception("No text found in API response")
                }
            } else {
                throw Exception("API error: ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            throw e
        }
    }.retryWhen { cause, attempt ->
        // max 3 retry
        if (attempt < 3) {
            Log.e("API_RETRY", "Retrying sendQueryCheckMeaning: attempt $attempt, error: ${cause.message}")
            delay(500) // Yarım saniye bekle
            true
        } else {
            false
        }
    }.catch { e ->
        Log.e("API_ERROR", "sendQueryCheckMeaning failed: ${e.message}")
        emit(false) // başarısızsa false döndür
    }.flowOn(Dispatchers.IO)

    sealed class GetTranslationResult {
        data class Success(val translation: String?, val explanation: String?) : GetTranslationResult()
        data class Error(val message: String) : GetTranslationResult()
    }

    sealed class GetSentenceResult {
        data class Success(val sentence: String) : GetSentenceResult()
        data class Error(val message: String) : GetSentenceResult()
    }

    sealed class GetTranslationResultNoBlank {
        data class Success(val isValidTranslationCheck: Boolean, val feedback: String) : GetTranslationResultNoBlank()
        data class Error(val message: String) : GetTranslationResultNoBlank()
    }

    sealed class GetTranslationResultWithBlank {
        data class Success(val completeSentence: String, val feedback: String) : GetTranslationResultWithBlank()
        data class Error(val message: String) : GetTranslationResultWithBlank()
    }

    sealed class GetLanguageResult {
        data class Success(val isValidLanguageCheck: Boolean) : GetLanguageResult()
        data class Error(val message: String) : GetLanguageResult()
    }

    sealed class GetMisspelledResult {
        data class Success(val misspelledWords:  MutableList<MisspelledWord>) : GetMisspelledResult()
        data class Error(val message: String) : GetMisspelledResult()
    }

    sealed class GetPickedWordsResult {
        data class Success(val pickedWords:  MutableList<String>) : GetPickedWordsResult()
        data class Error(val message: String) : GetPickedWordsResult()
    }

    fun getTranslation(queryText: String
    ): Flow<GetTranslationResult> = flow {
        val requestBody = GenerateContentRequest(
            contents = listOf( ContentRequest(parts = listOf(PartRequest(text = queryText))) ) )

        try {
            val response = geminiApi.getResponse(requestBody) // suspend çağrı
            if (response.isSuccessful && response.body() != null) {
                try {
                    val responseJson = handleApiResponseNoTranslation(response.body())
                    val translation = responseJson.optString("translation", null.toString()).replace("**", "")
                    val explanation = responseJson.optString("explanation", null.toString()).replace("**", "")
                    emit(GetTranslationResult.Success(translation, explanation))
                }
                catch (e: JSONException) { emit(GetTranslationResult.Error("JSON parse error: ${e.message}")) }
                catch (e: IllegalArgumentException) { emit(GetTranslationResult.Error("Invalid data: ${e.message}")) }
            } else { emit(GetTranslationResult.Error("API error: ${response.code()} ${response.message()}")) }
        } catch (e: Exception) { emit(GetTranslationResult.Error("Unexpected error: ${e.message}")) }
    }.retryWhen { cause, attempt ->
        if (attempt < 3) {
            Log.e("API_RETRY", "Retry attempt $attempt: ${cause.message}")
            delay(500)
            true
        } else { false }
    }.catch { e ->
            emit(GetTranslationResult.Error("Flow failed: ${e.message}"))
    }.flowOn(Dispatchers.IO)

    fun getRandomSentence(activeLevel: String, word: String?, language: String
    ): Flow<GetSentenceResult> = flow {

        var queryText = "Please generate 10 example sentences in $activeLevel $language"
        if (word == null){
            queryText += ". "
        } else {
            queryText += " using the word '$word'. "
        }
        queryText += "Avoid creating sentences that are unclear in context. "+
                "Respond strictly in the following JSON format — do not include any explanation or extra text: "
        queryText +="""
                    {
                    "sentences": [
                        {"sentence": "Example sentence."},
                        {"sentence": "Example sentence."},
                        {"sentence": "Example sentence."},
                        {"sentence": "Example sentence."},
                        {"sentence": "Example sentence."},
                        {"sentence": "Example sentence."},
                        {"sentence": "Example sentence."},
                        {"sentence": "Example sentence."},
                        {"sentence": "Example sentence."},
                        {"sentence": "Example sentence."}
                    ]
                    }
                    """.trimIndent()

        Log.d ("RandomSentence", queryText)
        val requestBody = GenerateContentRequest(
            contents = listOf(
                ContentRequest(
                    parts = listOf(
                        PartRequest(text = queryText)
                    )
                )
            )
        )

        try {
            val response = geminiApi.getResponse(requestBody) // suspend çağrı
            if (response.isSuccessful && response.body() != null) {
                var text = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (text != null) {
                    text = extractJson(text)
                    try {
                        val sentencesResponse = Gson().fromJson(text, RandomSentencesResponse::class.java)
                        val randomSentence = sentencesResponse.sentences.random().sentence
                        emit(GetSentenceResult.Success(randomSentence))
                    } catch (e: JsonSyntaxException) {
                        emit(GetSentenceResult.Error("JSON parse error: ${e.message}"))
                    }
                }
            } else { emit(GetSentenceResult.Error("API error: ${response.code()} ${response.message()}")) }
        } catch (e: Exception) { Log.e("GetSentence", "Unexpected error", e)
            e.printStackTrace()
            emit(GetSentenceResult.Error("Unexpected error: ${e}")) }
    }.retryWhen { cause, attempt ->
        if (attempt < 3) {
            Log.e("API_RETRY", "Retry attempt $attempt: ${cause.message}")
            delay(500)
            true
        } else { false }
    }.catch { e ->
        emit(GetSentenceResult.Error("Flow failed: ${e.message}"))
    }.flowOn(Dispatchers.IO)

    fun getWordsInEvaluation(string: String?, targetLanguage: String, targetLanguageCode: String
    ): Flow<GetPickedWordsResult> = flow {

        val evaluation: String
        if (string == null || string.isEmpty()) {
            evaluation = "I want to talk to him."
        } else {
            evaluation = string
        }

        val queryText = """
                       Please extract the $targetLanguage words in the text below.
                       
                       { "text": "$evaluation" }
                        
                        Return the result strictly in the following JSON format:
                        
                        {
                          "words": [
                            { "word": "word"},
                            ...
                            { "word": "word"}
                          ],
                          "feedBack": "feedback"
                        }
                        
                        If there are no English words, return:
                        
                        {
                          "words": [],
                          "feedBack": "feedback"
                        }

                       """.trimIndent()
        val requestBody = GenerateContentRequest(
            contents = listOf( ContentRequest(parts = listOf(PartRequest(text = queryText))) ) )

        try {
            val response = geminiApi.getResponse(requestBody) // suspend çağrı
            if (response.isSuccessful && response.body() != null) {
                try {
                    val responseJson = handleApiResponseDefault(response.body())
                    val jsonArray  = responseJson.getJSONArray("words")
                    val wordsOfEvaluation = mutableListOf<String>()
                    for (i in 0 until jsonArray.length()) {
                        val item = jsonArray.getJSONObject(i)
                        for (word in item.getString("word").lowercase(getLocale(targetLanguageCode)).split(Regex("""[\s,.!?()"]+"""))) {
                            if (word.isNotBlank()) {
                                wordsOfEvaluation.add(word)
                            }
                            emit(GetPickedWordsResult.Success(wordsOfEvaluation))
                        }
                    }
                }
                catch (e: JSONException) { emit(GetPickedWordsResult.Error("JSON parse error: ${e.message}")) }
                catch (e: IllegalArgumentException) { emit(GetPickedWordsResult.Error("Invalid data: ${e.message}")) }
            } else { emit(GetPickedWordsResult.Error("API error: ${response.code()} ${response.message()}")) }
        } catch (e: Exception) { emit(GetPickedWordsResult.Error("Unexpected error: ${e.message}")) }
    }.retryWhen { cause, attempt ->
        if (attempt < 3) {
            Log.e("API_RETRY", "Retry attempt $attempt: ${cause.message}")
            delay(500)
            true
        } else { false }
    }.catch { e ->
        emit(GetPickedWordsResult.Error("Flow failed: ${e.message}"))
    }.flowOn(Dispatchers.IO)

    fun getEvaluationNoBlank(queryText: String
    ): Flow<GetTranslationResultNoBlank> = flow {

        val requestBody = GenerateContentRequest(
            contents = listOf( ContentRequest(parts = listOf(PartRequest(text = queryText))) ) )

        try {
            val response = geminiApi.getResponse(requestBody)
            if (response.isSuccessful && response.body() != null) {
                try {
                    val responseJson = handleApiResponse(response.body())
                    val isValidTranslationCheck = responseJson.getBoolean("isValid")
                    val feedbackTranslationCheck = responseJson.getString("feedBack").replace("**", "")
                    emit(GetTranslationResultNoBlank.Success(isValidTranslationCheck, feedbackTranslationCheck))
                }
                catch (e: JSONException) { emit(GetTranslationResultNoBlank.Error("JSON parse error: ${e.message}")) }
                catch (e: IllegalArgumentException) { emit(GetTranslationResultNoBlank.Error("Invalid data: ${e.message}")) }
            } else { emit(GetTranslationResultNoBlank.Error("API error: ${response.code()} ${response.message()}")) }
        } catch (e: Exception) { emit(GetTranslationResultNoBlank.Error("Unexpected error: ${e.message}")) }
    }.retryWhen { cause, attempt ->
        if (attempt < 3) {
            Log.e("API_RETRY", "Retry attempt $attempt: ${cause.message}")
            delay(500)
            true
        } else { false }
    }.catch { e ->
        emit(GetTranslationResultNoBlank.Error("Flow failed: ${e.message}"))
    }.flowOn(Dispatchers.IO)

    fun getEvaluationWithBlank(queryText: String
    ): Flow<GetTranslationResultWithBlank> = flow {

        val requestBody = GenerateContentRequest(
            contents = listOf( ContentRequest(parts = listOf(PartRequest(text = queryText))) ) )

        try {
            val response = geminiApi.getResponse(requestBody)
            if (response.isSuccessful && response.body() != null) {
                try {
                    val responseJson = handleApiResponseWithBlank(response.body())
                    val completeSentence = responseJson.getString("completeSentence").replace("**", "")
                    val feedbackTranslationCheck = responseJson.getString("feedBack").replace("**", "")
                    emit(GetTranslationResultWithBlank.Success(completeSentence, feedbackTranslationCheck))
                }
                catch (e: JSONException) { emit(GetTranslationResultWithBlank.Error("JSON parse error: ${e.message}")) }
                catch (e: IllegalArgumentException) { emit(GetTranslationResultWithBlank.Error("Invalid data: ${e.message}")) }
            } else { emit(GetTranslationResultWithBlank.Error("API error: ${response.code()} ${response.message()}")) }
        } catch (e: Exception) { emit(GetTranslationResultWithBlank.Error("Unexpected error: ${e.message}")) }
    }.retryWhen { cause, attempt ->
        if (attempt < 3) {
            Log.e("API_RETRY", "Retry attempt $attempt: ${cause.message}")
            delay(500)
            true
        } else { false }
    }.catch { e ->
        emit(GetTranslationResultWithBlank.Error("Flow failed: ${e.message}"))
    }.flowOn(Dispatchers.IO)

    fun checkLanguage(sentence: String, language: String
    ): Flow<GetLanguageResult> = flow {

        val queryText = """
            Please return true if the text in JSON below is in $language, even if it has errors, otherwise return false:
            { "text": "$sentence" }
            Give your response strictly in the following JSON format:
            { isValid: "true or false", feedBack: "your feedback here."}
            """.trimIndent()

        val requestBody = GenerateContentRequest(
            contents = listOf( ContentRequest(parts = listOf(PartRequest(text = queryText))) ) )

        try {
            val response = geminiApi.getResponse(requestBody) // suspend çağrı
            if (response.isSuccessful && response.body() != null) {
                try {
                    val responseJson = handleApiResponseDefault(response.body())
                    val isValidLanguageCheck = responseJson.getBoolean("isValid")
                    emit(GetLanguageResult.Success(isValidLanguageCheck))
                }
                catch (e: JSONException) { emit(GetLanguageResult.Error("JSON parse error: ${e.message}")) }
                catch (e: IllegalArgumentException) { emit(GetLanguageResult.Error("Invalid data: ${e.message}")) }
            } else { emit(GetLanguageResult.Error("API error: ${response.code()} ${response.message()}")) }
        } catch (e: Exception) { emit(GetLanguageResult.Error("Unexpected error: ${e.message}")) }
    }.retryWhen { cause, attempt ->
        if (attempt < 3) {
            Log.e("API_RETRY", "Retry attempt $attempt: ${cause.message}")
            delay(500)
            true
        } else { false }
    }.catch { e ->
        emit(GetLanguageResult.Error("Flow failed: ${e.message}"))
    }.flowOn(Dispatchers.IO)

    fun getWordsInTranslationSentence(translationSentence: String, language: String
    ): Flow<GetMisspelledResult> = flow {

        val queryText = """
                       You will be given a list of words in $language. 
                       Your task is to identify the words that have letter omissions or incorrect character order.

        Return the result strictly in the following JSON format:

        {
          "wordsAndCorrections": [
            { "original": "word1", "correction": "if no correction needed, return original word itself" },
            { "original": "word2", "correction": "if no correction needed, return original word itself" }
          ],
          "feedBack": "feedback"
        }

        ${extractWordsAsJson(translationSentence)}
        
                       """.trimIndent()

        val requestBody = GenerateContentRequest(
            contents = listOf( ContentRequest(parts = listOf(PartRequest(text = queryText))) ) )

        try {
            val response = geminiApi.getResponse(requestBody) // suspend çağrı
            if (response.isSuccessful && response.body() != null) {
                val gson = Gson()
                var text = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

                if (text != null) {
                    text = text.replace("json", "").trim()
                    text = text.replace("```", "").trim()
                    Log.d("api_response_words_in_translation", text.toString())
                    try {
                        val misspelledWords = gson.fromJson(text, MisspelledWords::class.java)
                        val wordsAndCorrections = misspelledWords.wordsAndCorrections
                        emit(GetMisspelledResult.Success(wordsAndCorrections))
                    } catch (e: JSONException) { emit(GetMisspelledResult.Error("JSON parse error: ${e.message}")) }
                    catch (e: IllegalArgumentException) { emit(GetMisspelledResult.Error("Invalid data: ${e.message}")) }
                }
            } else { emit(GetMisspelledResult.Error("API error: ${response.code()} ${response.message()}")) }
        } catch (e: Exception) { emit(GetMisspelledResult.Error("Unexpected error: ${e.message}")) }
    }.retryWhen { cause, attempt ->
        if (attempt < 3) {
            Log.e("API_RETRY", "Retry attempt $attempt: ${cause.message}")
            delay(500)
            true
        } else { false }
    }.catch { e ->
        emit(GetMisspelledResult.Error("Flow failed: ${e.message}"))
    }.flowOn(Dispatchers.IO)

}

