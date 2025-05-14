package com.transist.util

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Context.INPUT_METHOD_SERVICE
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.os.Build
import android.speech.tts.TextToSpeech
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import com.transist.data.model.Language
import com.transist.R
import com.transist.data.remote.api.StudyFolderApi
import com.transist.data.remote.request.ContentRequest
import com.transist.data.remote.request.GenerateContentRequest
import com.transist.data.remote.request.PartRequest
import com.transist.data.remote.response.ApiResponse
import com.transist.data.remote.response.MisspelledWord
import com.transist.data.remote.response.Sentence
import com.transist.ui.customviews.DrawableSpan
import com.transist.ui.main.addedit.AddFragment
import com.transist.ui.main.list.ListFragment
import com.transist.ui.main.study.folder.StudyFolderFragment
import com.transist.ui.main.study.random.StudyRandomFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.roundToInt

fun ipucuClick (context: Context, editText: EditText){
    // Drawable kaynağını al
    val drawable: Drawable? = ContextCompat.getDrawable(context, R.drawable.line_et)
    drawable?.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)

    // SpannableString oluştur
    val spannableString = SpannableString("____")
    val imageSpan = ImageSpan(drawable!!, ImageSpan.ALIGN_BASELINE)
    spannableString.setSpan(imageSpan, 0, spannableString.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

    // EditText'e ekle
    val cursorPosition = editText.selectionStart
    val editableText = editText.text
    editableText.insert(cursorPosition, spannableString)
}

fun replaceBlanksWithDrawable(context: Context, textView: TextView, text: String) {
    val spannable = SpannableString(text)
    val drawable = ContextCompat.getDrawable(context, R.drawable.line_tv)
    drawable?.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
    if (drawable != null) {
        val pattern = "____"
        var start = text.indexOf(pattern)
        while (start >= 0) {
            val end = start + pattern.length
            val imageSpan = ImageSpan(drawable, ImageSpan.ALIGN_BASELINE)
            spannable.setSpan(imageSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            start = text.indexOf(pattern, end)
        }
    }
    textView.text = spannable
}

fun shrugClick (activity: Activity, editText: EditText){
    editText.setText("")
    // Drawable kaynağını al
    val drawable: Drawable? = ContextCompat.getDrawable(activity, R.drawable.zig_zag)
    drawable?.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)

    // SpannableString oluştur
    val spannableString = SpannableString("----")
    val imageSpan = ImageSpan(drawable!!, ImageSpan.ALIGN_BASELINE)
    spannableString.setSpan(imageSpan, 0, spannableString.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

    // EditText'e ekle
    val cursorPosition = editText.selectionStart
    val editableText = editText.text
    editableText.insert(cursorPosition, spannableString)
}

fun shrugClick(context: Context, editText: EditText) {
    val spannable = SpannableString("----")
    val imageSpan = DrawableSpan(context, R.drawable.zig_zag)
    spannable.setSpan(imageSpan, 0, spannable.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

    val cursorPosition = editText.selectionStart
    editText.text.insert(cursorPosition, spannable)
}

fun readLanguagesFromAssets(context: Context): List<Language> {
    return try {
        val inputStream = context.assets.open("diller.txt")
        inputStream.bufferedReader().useLines { lines ->
            lines.mapNotNull { line ->
                val parts = line.split(":").map { it.trim() }
                if (parts.size == 2) Language(parts[0], parts[1]) else null
            }.toList()
        }
    } catch (e: Exception) {
        listOf(Language("Tanımlanmadı", "unknown"))
    }
}

fun Spinner.setSelectionSilently(position: Int) {
    val listener = onItemSelectedListener   // mevcut listener’ı sakla
    onItemSelectedListener = null           // kapat
    setSelection(position, false)           // seçim yap (tetiklenmez)
    onItemSelectedListener = listener     // geri aç
}


fun getLanguageListInNative(context: Context, languageList: List<Language>): List<String> {

    val languageOptions = languageList.map { it.name }

    val languageListInNative = mutableListOf<String>() // mutable liste oluştur

    languageOptions.forEach {
        val languageStringId = getStringId(it)
        val languageInNative = context.getString(languageStringId)
        languageListInNative.add(languageInNative)
    }

    return languageListInNative
}

fun dpToPx(dp: Int): Float {
    val density = Resources.getSystem().displayMetrics.density
    return (dp * density)
}

fun hideKeyboard(view: View) {
    val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(view.windowToken, 0)
}

fun recreateStudyModes(fragmentManager: FragmentManager){

    val newStudyFolderFragment = StudyFolderFragment()
    fragmentManager.beginTransaction()
        .replace(R.id.fragment_container, newStudyFolderFragment, "studyFolder")
        .commit() // commitNow kullanmana gerek yok

    val newStudyRandomFragment = StudyRandomFragment()
    fragmentManager.beginTransaction()
        .replace(R.id.fragment_container, newStudyRandomFragment, "studyRandom")
        .commit() // commitNow kullanmana gerek yok
}

fun recreateAddFragment(fragmentManager: FragmentManager){
    val newFragment = AddFragment()
    fragmentManager.beginTransaction()
        .replace(R.id.fragment_container, newFragment, "add")
        .commit() // commitNow kullanmana gerek yok
}

fun recreateListeleFragment(fragmentManager: FragmentManager){
    val listFragment = fragmentManager.findFragmentByTag("list")
    val transaction = fragmentManager.beginTransaction()
    if (listFragment != null) {
        transaction.remove(listFragment)
        transaction.commitNow() // Hemen kaldır ki tekrar eklerken çakışma olmasın
    }
    val newListeleFragment = ListFragment()
    fragmentManager.beginTransaction()
        .add(R.id.fragment_container, newListeleFragment, "list")
        .hide(newListeleFragment) // hemen gösterilmeyecekse
        .commit()
}

fun handleApiResponse (apiResponse: ApiResponse?): JSONObject {
    if (apiResponse != null) {
        val candidate = apiResponse.candidates[0]
        val content = candidate.content
        if (content.parts.isNotEmpty()) {
            val part = content.parts[0]

            // JSON verisini içeren metni alalım ve temizleyelim
            val rawJsonString = part.text

            // Başlangıç ve bitişteki ```json ve ``` karakterlerini temizleme
            var cleanedJsonString = rawJsonString
                .replace("```json", "")
                .replace("```", "")
                .trim()

            cleanedJsonString = extractJson(cleanedJsonString).toString()

            val feedbackSubstring = cleanedJsonString.substringAfter("\"feedBack\": \"").substringBeforeLast("\"\n}")

            //cleanedJsonString = cleanedJsonString.replace(Regex("\\n\\s*(?=\"translation)"), "\n")
            //val feedbackSubstring = cleanedJsonString.substringAfter("\"feedBack\": \"").substringBeforeLast("\",\n\"translation")
            val escapedFeedback = feedbackSubstring.replace("\"", "'")
            val cleanedJsonStringUpdated = cleanedJsonString.replace(feedbackSubstring, escapedFeedback)
            return JSONObject(cleanedJsonStringUpdated)
        }
    }
    // Burada bir istisna fırlatılabilir
    throw IllegalArgumentException("Invalid API response")
}

fun handleApiResponseWithBlank (apiResponse: ApiResponse?): JSONObject {
    if (apiResponse != null) {
        val candidate = apiResponse.candidates[0]
        val content = candidate.content
        if (content.parts.isNotEmpty()) {
            val part = content.parts[0]

            // JSON verisini içeren metni alalım ve temizleyelim
            val rawJsonString = part.text

            // Başlangıç ve bitişteki ```json ve ``` karakterlerini temizleme
            var cleanedJsonString = rawJsonString
                .replace("```json", "")
                .replace("```", "")
                .trim()

            cleanedJsonString = extractJson(cleanedJsonString).toString()

            cleanedJsonString = cleanedJsonString.replace(Regex("\\n\\s*(?=\"completeSentence)"), "\n")
            //Log.d("Api_response_result", cleanedJsonString) // JSON yanıtını Logcat'e yazdır
            val feedbackSubstring = cleanedJsonString.substringAfter("\"feedBack\": \"").substringBeforeLast("\",\n\"completeSentence")
            val escapedFeedback = feedbackSubstring.replace("\"", "'")
            val cleanedJsonStringUpdated = cleanedJsonString.replace(feedbackSubstring, escapedFeedback)
            return JSONObject(cleanedJsonStringUpdated)
        }
    }
    // Burada bir istisna fırlatılabilir
    throw IllegalArgumentException("Invalid API response")
}

fun handleApiResponseNoTranslation (apiResponse: ApiResponse?): JSONObject {
    if (apiResponse != null) {
        val candidate = apiResponse.candidates[0]
        val content = candidate.content
        if (content.parts.isNotEmpty()) {
            val part = content.parts[0]

            // JSON verisini içeren metni alalım ve temizleyelim
            val rawJsonString = part.text

            // Başlangıç ve bitişteki ```json ve ``` karakterlerini temizleme
            var cleanedJsonString = rawJsonString
                .replace("```json", "")
                .replace("```", "")
                .trim()

            cleanedJsonString = extractJson(cleanedJsonString).toString()

            cleanedJsonString = cleanedJsonString.replace(Regex("\\n\\s*(?=\"explanation)"), "\n")
            val feedbackSubstring = cleanedJsonString.substringAfter("\"translation\": \"").substringBeforeLast("\",\n\"explanation")
            val escapedFeedback = feedbackSubstring.replace("\"", "'")
            val cleanedJsonStringUpdated = cleanedJsonString.replace(feedbackSubstring, escapedFeedback)
            return JSONObject(cleanedJsonStringUpdated)
        }
    }
    // Burada bir istisna fırlatılabilir
    throw IllegalArgumentException("Invalid API response")
}

fun handleApiResponseDefault (apiResponse: ApiResponse?): JSONObject {
    if (apiResponse != null) {
        val candidate = apiResponse.candidates[0]
        val content = candidate.content
        if (content.parts.isNotEmpty()) {
            val part = content.parts[0]

            // JSON verisini içeren metni alalım ve temizleyelim
            val rawJsonString = part.text

            // Başlangıç ve bitişteki ```json ve ``` karakterlerini temizleme
            var cleanedJsonString = rawJsonString
                .replace("```json", "")
                .replace("```", "")
                .trim()

            cleanedJsonString = extractJson(cleanedJsonString).toString()
            return JSONObject(cleanedJsonString)
        }
    }
    // Burada bir istisna fırlatılabilir
    throw IllegalArgumentException("Invalid API response")
}

fun shouldShowPronunciation(word: String, wordsOfEvaluation: MutableList<String>): Pair<Boolean,String> {
    if (word.isNotEmpty()){
        if (wordsOfEvaluation.contains(word)) {
            return Pair(true,word)
        } else if (hasApostropheFollowedByLetter(word) && wordsOfEvaluation.contains(word.substringBefore("'"))){
            return Pair(true,word.substringBefore("'"))
        } else if (word.isNumericDecimal() || word.substringBefore("'").isNumericDecimal()) {
            return Pair(true,word)
        } else {
            return Pair(false,"")
        }
    } else { return Pair(false,"") }
}

fun sendQuery(geminiApi: StudyFolderApi, queryText: String, callback: (ApiResponse?) -> Unit) {

    val requestBody = GenerateContentRequest(
        contents = listOf(
            ContentRequest(
                parts = listOf(
                    PartRequest(text = queryText)
                )
            )
        )
    )

    geminiApi.getResponse(requestBody).enqueue(object : Callback<ApiResponse> {
        override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
            Log.d("API_RESPONSE", "Response received: ${response.body()}")
            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()
                callback(apiResponse)
            } else {
                Log.e("API_ERROR", "Response Code: ${response.code()}")
                Log.e("API_ERROR", "Response Message: ${response.message()}")
                Log.e("API_ERROR", "Response Error Body: ${response.errorBody()?.string()}")
                Log.d("API_RESPONSE", "No response from API.")
                callback(null)
            }
        }

        override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
            Log.e("API_ERROR", "Failure: ${t.message}", t)
            callback(null)
        }
    })
}

fun dolulukDegistirme(dolulukAnlik: View, toplamDoluluk: Int, anlikDoluluk: Int, toplamDolulukWidth: Float, dp20: Float) {
    // doluluk_anlık view'ının LayoutParams'ını alıyoruz
    val params = dolulukAnlik.layoutParams as ConstraintLayout.LayoutParams

    // Yeni genişlik hesaplaması
    if (toplamDoluluk == 0) {
        params.width = toplamDolulukWidth.toInt()
    } else {
        val yeniGenislik = (toplamDolulukWidth * (anlikDoluluk.toFloat() / (toplamDoluluk+anlikDoluluk).toFloat())).toInt()
        if (yeniGenislik < (dp20/20*24).toInt()) { // params.height vardı
            params.width = (dp20/20*24).toInt()
        } else {
            params.width = yeniGenislik
        }
    }

    // Değişiklikleri yansıtmak için requestLayout() çağrısı yapıyoruz
    dolulukAnlik.requestLayout()
}

fun showKeyboard(activity: Activity){
    // Klavyeyi aç
    val imm = activity.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
    val view = activity.currentFocus
    if (view != null) {
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }
}

fun convertToSentenceList(sentences: List<String>?): MutableList<Sentence> {
    return sentences
        ?.mapNotNull { entry ->
            val sentencePart = Regex("sentence=(.*?),\\s*translation=").find(entry)?.groupValues?.get(1)
            val translationPart = Regex("translation=(.*?)\\)").find(entry)?.groupValues?.get(1)
            if (sentencePart != null && translationPart != null) {
                Sentence(sentencePart, translationPart)
            } else null
        }
        ?.toMutableList()
        ?: mutableListOf()
}

fun extractJson(response: String): String? {
    val jsonStart = response.indexOf("{")
    val jsonEnd = response.lastIndexOf("}")

    return if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
        response.substring(jsonStart, jsonEnd + 1).trim()
    } else {
        null // Geçerli bir JSON bulunamazsa null döndür
    }
}

fun getOffsetForPosition(textView: TextView, x: Float, y: Float): Int {
    val layout = textView.layout ?: return -1
    val line = layout.getLineForVertical(y.toInt())
    return layout.getOffsetForHorizontal(line, x)
}

fun pronunciationClick (metin: String, tts: TextToSpeech){
    tts.speak(metin, TextToSpeech.QUEUE_FLUSH, null, null)
}

fun extractWordAtOffset(text: String, offset: Int): String {
    if (offset < 0 || offset >= text.length || text[offset].isWhitespace()) return ""

    var start = offset
    var end = offset

    while (start > 0 && !text[start - 1].isWhitespace()) start--
    while (end < text.length && !text[end].isWhitespace()) end++

    val rawWord = text.substring(start, end)

    // Eğer "/" ile birleşik iki kelime varsa, tıklanan tarafa göre böl
    val wordPart = if ('/' in rawWord) {
        val slashIndex = rawWord.indexOf('/')
        val localOffset = offset - start
        if (localOffset < slashIndex) {
            rawWord.substring(0, slashIndex)
        } else {
            rawWord.substring(slashIndex + 1)
        }
    } else {
        rawWord
    }

    // Başındaki ve sonundaki noktalama işaretlerini temizle
    return rawWord.trim { it.isWhitespace() || it.isPunctuation() }
}

fun Char.isPunctuation(): Boolean {
    return this in listOf(
        '.', ',', ';', ':', '!', '?', '"', '\'', '“', '”', '‘', '’', '(', ')', '[', ']', '{', '}', '-', '…', '/'
    )
}

// Metin içinde tırnak işareti ve hemen ardından bir harf varsa true döner.
fun hasApostropheFollowedByLetter(input: String): Boolean {
    // Regex: '\'' karakteri ve hemen ardından gelen herhangi bir Unicode harfi (\p{L})
    val regex = Regex("'\\p{L}")
    return regex.containsMatchIn(input)
}

fun getLocale(languageCode: String): Locale {
    return when (languageCode.lowercase()) {
        "ar" -> Locale("ar", "SA")            // Arabic
        "bn" -> Locale("bn", "BD")            // Bengali
        "bg" -> Locale("bg", "BG")            // Bulgarian
        "zh" -> Locale.SIMPLIFIED_CHINESE      // Chinese
        "hr" -> Locale("hr", "HR")            // Croatian
        "cs" -> Locale("cs", "CZ")            // Czech
        "da" -> Locale("da", "DK")            // Danish
        "nl" -> Locale("nl", "NL")            // Dutch
        "en" -> Locale("en", "US")            // English
        "et" -> Locale("et", "EE")            // Estonian
        "fi" -> Locale("fi", "FI")            // Finnish
        "fr" -> Locale("fr", "FR")            // French
        "de" -> Locale("de", "DE")            // German
        "el" -> Locale("el", "GR")            // Greek
        "he" -> Locale("he", "IL")            // Hebrew
        "hi" -> Locale("hi", "IN")            // Hindi
        "hu" -> Locale("hu", "HU")            // Hungarian
        "id" -> Locale("id", "ID")            // Indonesian
        "it" -> Locale("it", "IT")            // Italian
        "ja" -> Locale("ja", "JP")            // Japanese
        "ko" -> Locale("ko", "KR")            // Korean
        "lv" -> Locale("lv", "LV")            // Latvian
        "lt" -> Locale("lt", "LT")            // Lithuanian
        "no" -> Locale("no", "NO")            // Norwegian
        "pl" -> Locale("pl", "PL")            // Polish
        "pt" -> Locale("pt", "PT")            // Portuguese
        "ro" -> Locale("ro", "RO")            // Romanian
        "ru" -> Locale("ru", "RU")            // Russian
        "sr" -> Locale("sr", "RS")            // Serbian
        "sk" -> Locale("sk", "SK")            // Slovak
        "sl" -> Locale("sl", "SI")            // Slovenian
        "es" -> Locale("es", "ES")            // Spanish
        "sw" -> Locale("sw", "TZ")            // Swahili
        "sv" -> Locale("sv", "SE")            // Swedish
        "th" -> Locale("th", "TH")            // Thai
        "tr" -> Locale("tr", "TR")            // Turkish
        "uk" -> Locale("uk", "UA")            // Ukrainian
        "vi" -> Locale("vi", "VN")            // Vietnamese
        "un" -> Locale.getDefault()           // Unknown fallback
        else -> Locale.getDefault()           // Fallback for any other code
    }
}

fun String.isNumericDecimal(): Boolean {
    return matches(Regex("^-?\\d+(\\.\\d+)?\$"))
}

fun String.hasLetter(): Boolean = Regex("\\p{L}").containsMatchIn(this)

fun String.capitalizeFirstLetter(): String {
    if (this.isEmpty()) return this
    return this[0].uppercase() + this.substring(1)
}

fun getLocalizedString(context: Context, locale: Locale, resId: Int): String {
    val config = Configuration(context.resources.configuration)
    config.setLocale(locale)

    val localizedContext = context.createConfigurationContext(config)
    return localizedContext.resources.getString(resId)
}

fun getQueryWithBlank (
    reverse: Boolean,
    localizedContext : Context,
    originalSentence: String,
    translationSentence: String,
    nativeLanguageInNativeLanguage: String,
    translationSentenceLanguage: String) :String {
    if (reverse){
        return localizedContext.getString(R.string.query_with_blank_reverse, originalSentence, translationSentence, nativeLanguageInNativeLanguage)
    } else {
        return localizedContext.getString(R.string.query_with_blank, originalSentence, translationSentenceLanguage, translationSentence, nativeLanguageInNativeLanguage)
    }
}

fun getQueryNoBlank (reverse: Boolean,
                     localizedContext: Context,
                     originalSentence: String,
                     translationSentence: String,
                     nativeLanguageInNativeLanguage: String,
                     translationSentenceLanguage: String):String {
    if (reverse){
        return localizedContext.getString(R.string.query_no_blank_reverse, originalSentence, translationSentence, nativeLanguageInNativeLanguage)
    } else {
        return localizedContext.getString(R.string.query_no_blank, originalSentence, translationSentenceLanguage, translationSentence, nativeLanguageInNativeLanguage)
    }
}

fun startToastAnimation(view: CardView): AnimatorSet {
    // Eğer view daha önce görünmez olarak bırakıldıysa emin ol
    view.alpha = 0f
    view.visibility = View.VISIBLE

    // 1) 500ms fade-in
    val fadeIn = ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f).apply {
        duration = 500
    }
    // 2) 1000ms sabitle (alpha = 1f)
    val hold = ObjectAnimator.ofFloat(view, View.ALPHA, 1f, 1f).apply {
        duration = 3000
    }
    // 3) 500ms fade-out
    val fadeOut = ObjectAnimator.ofFloat(view, View.ALPHA, 1f, 0f).apply {
        duration = 500
    }

    // Hepsini sırayla oynat
    return AnimatorSet().apply {
        playSequentially(fadeIn, hold, fadeOut)
        addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                view.visibility = View.GONE
            }
        })
        start()
    }
}

fun stopToastAnimation(animator: Animator?) {
    animator?.let {
        it.removeAllListeners()
        it.cancel()
    }
}

fun startBlinkingAnimation(vararg views: View): List<ObjectAnimator> {
    val animators = mutableListOf<ObjectAnimator>()
    for (view in views) {
        val animator = ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 0.7f).apply {
            duration = 1000 // 1 saniye içinde geçiş yap
            repeatMode = ValueAnimator.REVERSE // Tekrar ederken tersine çevir
            repeatCount = ValueAnimator.INFINITE // Sürekli tekrar et
            start()
        }
        animators.add(animator)
    }
    return animators
}

fun stopBlinkingAnimation(textView: TextView?, blinkingAnimator: List<ObjectAnimator>?) {

    // Eğer görünüm artık yoksa işlemi iptal et
    //if (textView == null || !textView.isAttachedToWindow) return

    val animators = blinkingAnimator
    // Tüm animasyonları durdur
    if (animators != null) {
        for (animator in animators) {
            animator.cancel()
            (animator.target as? View)?.alpha = 1f // Görünürlüğü tamamen açık yap
        }
    }
}

fun kisaMesaj(cardView: CardView, root: ConstraintLayout){
    val constraintSet1 = ConstraintSet()
    constraintSet1.clone(root) // Mevcut constraintleri kopyala

    // cardView'ün "bottom to top of" bağlantısını kaldır
    constraintSet1.clear(cardView.id, ConstraintSet.BOTTOM)

    constraintSet1.applyTo(root) // Değişikliği uygula

    // Yüksekliği wrap_content yap
    val params = cardView.layoutParams as ConstraintLayout.LayoutParams
    params.height = ViewGroup.LayoutParams.WRAP_CONTENT
    cardView.layoutParams = params
    cardView.requestLayout() // Değişikliği uygula
    cardView.translationX = 0f
}

fun uzunMesaj(cardView: CardView, root: ConstraintLayout, imageButton: ImageButton){
    val constraintSet = ConstraintSet()
    constraintSet.clone(root) // Mevcut constraintleri kopyala
    // Programatik olarak bottom to top bağlantısını yap
    constraintSet.connect(
        cardView.id,
        ConstraintSet.BOTTOM,
        imageButton.id,
        ConstraintSet.TOP
    )

    // Margin bottom'u yeniden ayarla
    constraintSet.setMargin(
        cardView.id,
        ConstraintSet.BOTTOM,
        dpToPx(15).roundToInt()
    )

    constraintSet.applyTo(root) // Constraintleri uygula

    val params = cardView.layoutParams as ConstraintLayout.LayoutParams
    params.height = 0 // 0dp yapar
    cardView.layoutParams = params
    cardView.requestLayout()
    cardView.translationX = 0f
}

suspend fun hasRealInternetAccess(): Boolean = withContext(Dispatchers.IO) {
    return@withContext try {
        val connection = URL("https://clients3.google.com/generate_204")
            .openConnection() as HttpURLConnection
        connection.setRequestProperty("User-Agent", "Android")
        connection.setRequestProperty("Connection", "close")
        connection.connectTimeout = 1500
        connection.connect()
        connection.responseCode == 204
    } catch (e: Exception) {
        false
    }
}

fun getStringId(stringName: String): Int {
    val stringNameLowerCase = stringName.lowercase()
    val stringId = when (stringNameLowerCase) {
        "arabic" -> R.string.arabic
        "bengali" -> R.string.bengali
        "bulgarian" -> R.string.bulgarian
        "chinese" -> R.string.chinese
        "croatian" -> R.string.croatian
        "czech" -> R.string.czech
        "danish" -> R.string.danish
        "dutch" -> R.string.dutch
        "english" -> R.string.english
        "estonian" -> R.string.estonian
        "finnish" -> R.string.finnish
        "french" -> R.string.french
        "german" -> R.string.german
        "greek" -> R.string.greek
        "hebrew" -> R.string.hebrew
        "hindi" -> R.string.hindi
        "hungarian" -> R.string.hungarian
        "indonesian" -> R.string.indonesian
        "italian" -> R.string.italian
        "japanese" -> R.string.japanese
        "korean" -> R.string.korean
        "latvian" -> R.string.latvian
        "lithuanian" -> R.string.lithuanian
        "norwegian" -> R.string.norwegian
        "polish" -> R.string.polish
        "portuguese" -> R.string.portuguese
        "romanian" -> R.string.romanian
        "russian" -> R.string.russian
        "serbian" -> R.string.serbian
        "slovak" -> R.string.slovak
        "slovenian" -> R.string.slovenian
        "spanish" -> R.string.spanish
        "swahili" -> R.string.swahili
        "swedish" -> R.string.swedish
        "thai" -> R.string.thai
        "turkish" -> R.string.turkish
        "ukrainian" -> R.string.ukrainian
        "vietnamese" -> R.string.vietnamese
        "unknown" -> R.string.unknown
        else -> 0
    }
    return stringId
}

fun pronunciationAnimation(viewToShow: CardView) {
    viewToShow.visibility = View.VISIBLE
    ObjectAnimator.ofFloat(viewToShow, View.ALPHA, 0f, 1f).apply {
        duration = 500 // 1 saniye içinde geçiş yap
        start()
    }
}

fun highlightTypos(textView: TextView, wordsAndCorrections: List<MisspelledWord>) {
    val originalText = textView.text

    // Eğer text zaten Spannable ise onu kullan, değilse yeni bir tane oluştur
    val spannable = if (originalText is Spannable) {
        SpannableStringBuilder(originalText)
    } else {
        SpannableStringBuilder(originalText.toString())
    }

    // Yanlış yazılan kelimeleri filtrele
    val typoList = wordsAndCorrections.filter { it.original != it.correction }

    typoList.forEach { typo ->
        val pattern = Regex("\\b${Regex.escape(typo.original)}\\b", RegexOption.IGNORE_CASE)
        pattern.findAll(spannable.toString()).forEach { match ->
            spannable.setSpan(
                ForegroundColorSpan(
                    ContextCompat.getColor(
                        textView.context,
                        R.color.wrong_evaluation_red
                    )
                ),
                match.range.first,
                match.range.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    // Burada artık drawable'lar korunur çünkü mevcut Spannable üzerinde işlem yaptık
    textView.text = spannable
}


fun extractWordsAsJson(input: String): String {
    val words = input.lowercase()
        .replace("____", "")
        .replace(Regex("[.,!?;:]+$"), "")  // Noktalama işaretlerini temizle
        .split(Regex("\\s+"))              // Boşluklardan ayır
        .filter { it.isNotBlank() }

    val jsonArray = JSONArray()
    for (word in words) {
        val wordObject = JSONObject()
        wordObject.put("word", word)
        jsonArray.put(wordObject)
    }

    val result = JSONObject()
    result.put("words", jsonArray)

    return result.toString(4) // Güzel biçimde yazdırmak için 4 boşlukla girinti
}

fun showDialog(view: Int, activity: Activity, b: Boolean): Pair<View, Dialog>{
    val dialogView = LayoutInflater.from(activity).inflate(view, null)
    val dialog = Dialog(activity)
    dialog.setContentView(dialogView)
    dialog.window?.setBackgroundDrawableResource(android.R.color.transparent) // Şeffaf arka plan
    dialog.show()

    // 2. EKRAN BOYUTLARINI AL
    val displayMetrics = DisplayMetrics()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        // Android 11 ve üstü için modern API
        val windowMetrics = activity.windowManager.currentWindowMetrics
        val insets = windowMetrics.windowInsets.getInsets(android.view.WindowInsets.Type.systemBars())
        val screenWidth = windowMetrics.bounds.width() - insets.left - insets.right
        val screenHeight = windowMetrics.bounds.height() - insets.top - insets.bottom
        displayMetrics.widthPixels = screenWidth
        displayMetrics.heightPixels = screenHeight
    } else {
        // Eski sürümler için
        @Suppress("DEPRECATION")
        activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
    }

    val screenWidth = displayMetrics.widthPixels
    val screenHeight = displayMetrics.heightPixels

    // 3. HEDEF BOYUTLARI HESAPLA
    // Genişlik: Ekran genişliğinin %90'ı (kenarlarda %5'er boşluk)
    val targetWidth = (screenWidth * 0.75).toInt()

    // Yükseklik: Ekran yüksekliğinin yarısı
    val targetHeight: Int
    if (b){
        targetHeight = (screenHeight * 0.50).toInt()
    } else {
        targetHeight = WindowManager.LayoutParams.WRAP_CONTENT
    }
    // 4. DIALOG PENCERESİNE BOYUTLARI ATA
    dialog.window?.setLayout(targetWidth, targetHeight)

    return Pair(dialogView, dialog)
}

fun initTtsWithTargetLanguageSupport(activity: Activity, targetLanguageCode: String, onReady: (TextToSpeech?) -> Unit) {
    val localeTR = getLocale(targetLanguageCode)

    // 1. Tüm yüklü motorları listele
    val ttsTemp = TextToSpeech(activity) {}
    val installedEngines = ttsTemp.engines.map { it.name }
    ttsTemp.shutdown()

    // Öncelik sırası: Google → Samsung → Diğer motorlar
    val sortedEngines = mutableListOf<String>()
    if (installedEngines.contains("com.google.android.tts")) sortedEngines.add("com.google.android.tts")
    //if (installedEngines.contains("com.samsung.SMT")) sortedEngines.add("com.samsung.SMT")
    sortedEngines.addAll(installedEngines.filterNot { it in sortedEngines })

    fun tryNextEngine(index: Int) {
        if (index >= sortedEngines.size) {
            // Hiçbir motor desteklemiyor → ayarlara yönlendir

            val intent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
            val canHandle = intent.resolveActivity(activity.packageManager) != null
            if (canHandle) {
                activity.startActivity(intent)
            } else {
                //makeToast(activity, activity.getString(R.string.no_voice_engine))
                //showDialogGoToPlayStore()
                // Telefonunuzda, seslendirme için gerekli özellikler mevcut değil.
                // Google play store mağazasından gerekli uygulamayı indirmeniz gerekiyor.
                // Mağazaya git. // Daha sonra
            }
            onReady(null)
            return
        }

        val engine = sortedEngines[index]
        var tts: TextToSpeech? = null  // <-- burada nullable olarak tanımla

        val initListener = TextToSpeech.OnInitListener { status ->
            if (status == TextToSpeech.SUCCESS) {
                val availability = tts?.isLanguageAvailable(localeTR)
                if (availability == TextToSpeech.LANG_AVAILABLE
                    || availability == TextToSpeech.LANG_COUNTRY_AVAILABLE
                    || availability == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE) {

                    tts!!.language = localeTR
                    tts!!.setSpeechRate(0.7f)
                    onReady(tts)

                } else {
                    tts!!.shutdown()
                    tryNextEngine(index + 1)
                }
            } else {
                tts?.shutdown()
                tryNextEngine(index + 1)
            }
        }

        // önce listener’ı hazırladık, sonra tts’i atadık
        tts = TextToSpeech(activity, initListener, engine)
    }

    // İlk motordan başla
    tryNextEngine(0)
}

fun showDialogNoVoiceEngine(context: Context){
    val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_no_voice_engine, null)
    val dialog = Dialog(context)
    dialog.setContentView(dialogView)

    dialog.window?.setBackgroundDrawableResource(android.R.color.transparent) // Şeffaf arka plan
    dialog.show()
    val tvCancel = dialogView.findViewById<TextView>(R.id.tv_cancel)
    val tvOk = dialogView.findViewById<TextView>(R.id.tv_ok)

    tvOk.setOnClickListener {
        val intent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
        context.startActivity(intent)
        dialog.cancel()
    }

    tvCancel.setOnClickListener {
        dialog.cancel()
    }

}

fun getLocalizedContext(context: Context, languageCode: String): Context {
    val locale = Locale(languageCode)
    Locale.setDefault(locale)

    val config = context.resources.configuration
    config.setLocale(locale)

    return context.createConfigurationContext(config)
}

fun animateTextChange(textView: TextView, newText: String) {
    textView.animate()
        .alpha(0f)
        .setDuration(250)
        .withEndAction {
            textView.text = newText
            textView.animate()
                .alpha(1f)
                .setDuration(250)
                .start()
        }
        .start()
}

fun fadeIn(view: View) {
    view.apply {
        alpha = 0f
        visibility = View.VISIBLE
        animate()
            .alpha(1f)
            .setDuration(250)
            .start()
    }
}

fun scaleIn(view: View) {
    view.apply {
        scaleX = 0f
        scaleY = 0f
        alpha = 0f
        visibility = View.VISIBLE
        animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(250)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                visibility = View.VISIBLE
            }
            .start()
    }
}

fun scaleOut(view: View) {
    view.apply {
        scaleX = 1f
        scaleY = 1f
        alpha = 1f
        animate()
            .scaleX(0f)
            .scaleY(0f)
            .alpha(0f)
            .setDuration(250)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                visibility = View.GONE
            }
            .start()
    }
}

fun flipViews(frontView: View, backView: View) {
    val originalFrontElevation = frontView.elevation
    val originalBackElevation = backView.elevation

    val outAnim = ObjectAnimator.ofFloat(frontView, "rotationY", 0f, 90f).apply {
        duration = 150
        interpolator = AccelerateInterpolator()
    }
    val inAnim = ObjectAnimator.ofFloat(backView, "rotationY", -90f, 0f).apply {
        duration = 150
        interpolator = DecelerateInterpolator()
    }

    outAnim.addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationStart(animation: Animator) {
            // Gölge efektini geçici olarak kapat
            frontView.elevation = 0f
            backView.elevation = 0f
        }

        override fun onAnimationEnd(animation: Animator) {
            frontView.visibility = View.GONE
            backView.visibility = View.VISIBLE
            inAnim.start()
        }
    })

    inAnim.addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
            // Gölgeyi geri getir
            frontView.elevation = originalFrontElevation
            backView.elevation = originalBackElevation
        }
    })

    outAnim.start()
}










