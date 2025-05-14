package com.transist.ui.main.study.folder

import android.animation.Animator
import android.app.Activity
import android.content.Context

import android.content.res.Resources
import android.graphics.Rect
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.transist.data.local.DatabaseHelper
import com.transist.data.model.ExpressionData
import com.transist.R
import com.transist.data.remote.api.StudyFolderApi
import com.transist.data.remote.response.MisspelledWord
import com.transist.data.remote.response.MisspelledWords
import com.transist.data.repository.PreferencesRepository
import com.transist.databinding.FragmentStudyFolderBinding
import com.transist.util.capitalizeFirstLetter
import com.transist.util.dolulukDegistirme
import com.transist.util.dpToPx
import com.transist.util.extractWordAtOffset
import com.transist.util.extractWordsAsJson
import com.transist.util.getLocale
import com.transist.util.getOffsetForPosition
import com.transist.util.getStringId
import com.transist.util.handleApiResponse
import com.transist.util.handleApiResponseDefault
import com.transist.util.handleApiResponseWithBlank
import com.transist.util.hasLetter
import com.transist.util.highlightTypos
import com.transist.util.initTtsWithTargetLanguageSupport
import com.transist.util.ipucuClick
import com.transist.util.pronunciationAnimation
import com.transist.util.readLanguagesFromAssets
import com.transist.util.sendQuery
import com.transist.util.showDialog
import com.transist.util.showDialogNoVoiceEngine
import com.transist.util.showKeyboard
import com.transist.util.shrugClick
import com.transist.util.startToastAnimation
import com.transist.util.stopToastAnimation
import com.transist.util.pronunciationClick
import com.transist.ui.main.ActivityMain
import com.transist.ui.main.MainViewModel
import com.transist.util.getLocalizedContext
import com.transist.util.hasRealInternetAccess
import com.transist.util.kisaMesaj
import com.transist.util.shouldShowPronunciation
import com.transist.util.uzunMesaj
import com.google.gson.Gson
import kotlinx.coroutines.launch
import org.json.JSONException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.math.roundToInt

class StudyFolderFragment : Fragment() {

    private lateinit var activity: Activity
    private lateinit var dbHelper: DatabaseHelper
    private var item: ExpressionData? = null
    private lateinit var expression: ExpressionData

    private var _binding: FragmentStudyFolderBinding? = null
    private val binding get() = _binding!!  // Null güvenliği için

    val displayMetrics = Resources.getSystem().displayMetrics
    val toplamDolulukWith = displayMetrics.widthPixels - 2 * dpToPx(20)
    private var directionAnimator: Animator? = null

    var targetSentence = ""
    var nativeLanguageCode = "un"
    var targetLanguageCode = "un"
    var targetLanguage = "Unknown"
    var nativeLanguage = "Unknown"
    private lateinit var localizedContext: Context
    var targetLanguageStringId = 0
    var targetLanguageInNativeLanguage = "Unknown"
    var nativeLanguageStringId = 0
    var nativeLanguageInNativeLanguage = "Unknown"
    var degerlendirmeAsamasi = 0
    var wordsOfEvaluation = mutableListOf<String>()
    var wordsAndCorrections = mutableListOf<MisspelledWord>()
    private var isValidLanguageCheck: Boolean? = null
    private var feedbackTranslationCheck: String? = null
    var isUzun = false
    private var resendCount = 0
    private lateinit var tts: TextToSpeech
    private var isTTsExist = false
    private lateinit var prefsRepository: PreferencesRepository
    private var translationQuota = 0
    private val mainViewModel: MainViewModel by activityViewModels()

    val retrofit = Retrofit.Builder()
        .baseUrl("https://my-backend-image-347075314413.europe-west1.run.app/") // Cloud Run URL veya localhost:3000/
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val studyFolderApi: StudyFolderApi = retrofit.create(StudyFolderApi::class.java)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStudyFolderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // TTS kaynaklarını serbest bırak
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        _binding = null  // Hafıza sızıntısını önlemek için
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity = requireActivity() as ActivityMain
        dbHelper = DatabaseHelper(activity)
        prefsRepository = PreferencesRepository(activity)

        mainViewModel.checkSubscription()
        val diller = dbHelper.readLanguages(dbHelper.writableDatabase)
        if (diller != null) {
            nativeLanguageCode = diller.first
            targetLanguageCode = diller.second
        }

        item = dbHelper.getRandomExpressionByFolder()

        if (item == null) {
            showBreak()
            dolulukGoster()
        }
        else {
            expression = item as ExpressionData
            val languageList = readLanguagesFromAssets(activity)
            nativeLanguage = languageList.find { it.code == nativeLanguageCode }?.name ?: nativeLanguageCode
            targetLanguage = languageList.find { it.code == targetLanguageCode }?.name ?: targetLanguageCode

            localizedContext = getLocalizedContext(requireContext(), nativeLanguageCode)
            targetLanguageStringId = getStringId(targetLanguage)
            targetLanguageInNativeLanguage = localizedContext.getString(targetLanguageStringId)
            nativeLanguageStringId = getStringId(nativeLanguage)
            nativeLanguageInNativeLanguage = localizedContext.getString(nativeLanguageStringId)

            Log.d("diller", "native: $nativeLanguageCode, target: $targetLanguageCode")
            Log.d("diller", "native: $nativeLanguage, target: $targetLanguage")
            Log.d("diller", "targetLanguageInNativeLanguage: $targetLanguageInNativeLanguage, nativeLanguageInNativeLanguage: $nativeLanguageInNativeLanguage")

            binding.cvToast.visibility = View.GONE
            setUIforTranslation()
            showSentenceFromList()
            dolulukGoster()
        }


        mainViewModel.translation_count.observe(viewLifecycleOwner) { count ->
            if (count != translationQuota){
                if (count == 0){
                    showDialog(R.layout.dialog_no_quota, requireActivity(), false)
                }
                translationQuota = count ?: 0
            }
        }

        // Önce tüm yüklü ses motorlarını bulup, sonra herhangi birinde dil dosyası yüklü mü diye bakıyoruz.
        initTtsWithTargetLanguageSupport(activity, targetLanguageCode) { tts1 ->
            if (tts1 != null) {
                isTTsExist = true
                tts = tts1
            } else {
                // ❌ Hiçbir motor Türkçe desteklemiyor, kullanıcı ayarlara yönlendirilecek
                isTTsExist = false
            }
        }

        binding.buttonStartFromScratch.setOnClickListener {
            dbHelper.resetExpressionsStatusByFolder(dbHelper.getActiveStudyFolder().first)
            updateDeckAndViews()
        }

        binding.ibOriginalSentenceRefresh.setOnClickListener {
            if (binding.cvEvaluation.visibility == View.GONE){
                expression.nextSentenceIndex = dbHelper.updateSentenceIndex(expression)
            }
            val nextCumleCifti = dbHelper.getSentenceById(expression)
            setUIforTranslation()
            binding.tvOriginalSentence.setText(nextCumleCifti.second)
            targetSentence = nextCumleCifti.first
            binding.tvTarget.setText(targetSentence)
            binding.etTranslation.isEnabled = true
            binding.etTranslation.requestFocus()
            showKeyboard(activity)
        }

        binding.tvHint.setOnClickListener {
            ipucuClick(activity, binding.etTranslation)
        }

        binding.ibEdit.setOnClickListener{
            if (binding.etTranslation.text.toString() == "----") {
                binding.etTranslation.text?.clear()
            }

            binding.tvHint.visibility = View.VISIBLE
            binding.ibEditDone.visibility = View.VISIBLE
            binding.tvTranslation.visibility = View.GONE
            binding.etTranslation.visibility = View.VISIBLE
            binding.etTranslation.isEnabled = true
            binding.etTranslation.requestFocus()
            showKeyboard(activity)

            binding.ibEdit.visibility = View.GONE
        }

        binding.ibEditDone.setOnClickListener{
            binding.etTranslation.isEnabled = false
            binding.etTranslation.visibility = View.GONE
            binding.tvTranslation.visibility = View.VISIBLE
            binding.tvTranslation.text = binding.etTranslation.text

            binding.tvHint.visibility = View.GONE
            binding.ibEdit.visibility = View.VISIBLE
            binding.ibEditDone.visibility = View.GONE
            checkTranslationSender()
        }

        binding.ibSubmit.setOnClickListener {
            val inputText = binding.etTranslation.text.toString().trim()
            if (inputText.hasLetter() || inputText == "----") {
                checkTranslationSender()
            } else {
                makeToast(activity.getString(R.string.no_given_translation))
            }
        }

        binding.ibShrug.setOnClickListener {
            shrugClick(activity, binding.etTranslation)
            binding.etTranslation.isEnabled = false
            binding.ibShrug.visibility = View.GONE
            binding.tvHint.visibility = View.GONE
            binding.ibEdit.visibility = View.VISIBLE
            checkTranslationSender()
        }

        binding.ibShrugClose.setOnClickListener {
            binding.ibShrugClose.visibility = View.GONE
            binding.tvHint.visibility = View.VISIBLE
            binding.tvTranslation.visibility = View.GONE
            binding.etTranslation.visibility = View.VISIBLE
            binding.etTranslation.isEnabled = true
            binding.etTranslation.text?.clear()
            binding.etTranslation.requestFocus()
            showKeyboard(activity)
        }

        binding.etTranslation.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.toString() == "----" || s.toString().hasLetter()){
                    binding.ibShrug.visibility = View.GONE
                    if (binding.ibSubmit.visibility == View.VISIBLE) {
                        binding.ibEditDone.visibility = View.GONE
                    } else {
                        binding.ibEditDone.visibility = View.VISIBLE
                    }
                } else {
                    binding.ibShrug.visibility = View.VISIBLE
                    binding.ibEditDone.visibility = View.GONE
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.ibNext.setOnClickListener {
            // Güncel itemin nextSentenceIndex'ini ve status'unu güncelle
            dbHelper.updateSentenceIndex(expression)
            dbHelper.updateExpressionStatus(expression)

            item = dbHelper.getRandomExpressionByFolder()

            if (item == null ){
                showBreak()
            } else {
                expression = item as ExpressionData
                val nextCumleCifti = dbHelper.getSentenceById(expression)
                setUIforTranslation()
                binding.tvOriginalSentence.setText(nextCumleCifti.second)
                targetSentence = nextCumleCifti.first
                binding.tvTarget.setText(targetSentence)
                binding.etTranslation.isEnabled = true
                binding.etTranslation.requestFocus()
                showKeyboard(activity)
            }
            dolulukGoster()
        }

        binding.ibSecondCheck.setOnClickListener {
            checkTranslationSender()
        }

        binding.tvEvaluation.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val offset = getOffsetForPosition(binding.tvEvaluation, event.x, event.y)
                val word = extractWordAtOffset(binding.tvEvaluation.text.toString(), offset).lowercase(
                    getLocale(targetLanguageCode)
                )
                hidePronunciation()
                val displayWord = shouldShowPronunciation(word, wordsOfEvaluation)
                if (displayWord.first) {
                    val match = wordsAndCorrections.firstOrNull { it.original == displayWord.second }
                    if (match != null && match.original.hasLetter()){
                        if (match.original != match.correction) {
                            showTypoPronunciation(match.original, match.correction)
                        } else {
                            showPronunciation(displayWord.second)
                        }
                    } else {
                        showPronunciation(displayWord.second)
                    }
                }
                v.performClick()
            }
            true
        }

        binding.tvTranslation.setOnTouchListener (translationClickListener())

        binding.tvTarget.setOnTouchListener (sentenceTouchlistener())
        binding.tvCompleteSentence.setOnTouchListener (sentenceTouchlistener())

        binding.ibPronunciation1.setOnClickListener {
            if (isTTsExist) {
                pronunciationClick(binding.tvPronunciation1.text.toString(), tts)
            } else {
                showDialogNoVoiceEngine(activity)
            }
        }

        binding.ibPronunciation2.setOnClickListener {
            if (isTTsExist) {
                pronunciationClick(binding.tvPronunciation2.text.toString(), tts)
            } else {
                showDialogNoVoiceEngine(activity)
            }
        }

        binding.ibPronunciationTypo1.setOnClickListener {
            if (isTTsExist) {
                pronunciationClick(binding.tvCorrection1.text.toString(), tts)
            } else {
                showDialogNoVoiceEngine(activity)
            }
        }

        binding.ibPronunciationTypo2.setOnClickListener {
            if (isTTsExist) {
                pronunciationClick(binding.tvCorrection2.text.toString(), tts)
            } else {
                showDialogNoVoiceEngine(activity)
            }
        }

        binding.topBar.root.setOnClickListener {
            val dialog = showDialog(R.layout.dialog_current_folder, activity, false)

            val tvFolderName = dialog.first.findViewById<TextView>(R.id.tv_folder_name)
            val btnStartFromScratch = dialog.first.findViewById<Button>(R.id.button_start_from_scratch)
            tvFolderName.setText(dbHelper.getActiveStudyFolder().second.capitalizeFirstLetter())
            btnStartFromScratch.setOnClickListener {
                dbHelper.resetExpressionsStatusByFolder(dbHelper.getActiveStudyFolder().first)
                updateDeckAndViews()
                dialog.second.dismiss()
            }
        }

    }

    fun sentenceTouchlistener () = View.OnTouchListener { v, event ->
        if (event.action == MotionEvent.ACTION_DOWN) {
            val tv = v as TextView
            val offset = getOffsetForPosition(tv, event.x, event.y)
            val word = extractWordAtOffset(tv.text.toString(), offset).lowercase(getLocale(targetLanguageCode))
            Log.d("word", word)
            hidePronunciation()
            if (word.hasLetter() ){
                Log.d("word", "kelime direk alındı")
                showPronunciation(word)
            } else {
                Log.d("word", "boş")
            }
            v.performClick() // Bu yeterli
        }
        true
    }

    fun translationClickListener() = View.OnTouchListener { v, event ->
        if (event.action == MotionEvent.ACTION_DOWN) {
            val offset = getOffsetForPosition(binding.tvTranslation, event.x, event.y)
            val word = extractWordAtOffset(binding.tvTranslation.text.toString(), offset).lowercase(
                getLocale(targetLanguageCode)
            )
            hidePronunciation()
            if (word.isNotEmpty()){
                val match = wordsAndCorrections.firstOrNull { it.original == word }

                if (match != null && match.original == match.correction) {
                    showPronunciation(word)
                } else if (match != null) {
                    showTypoPronunciation(match.original, match.correction)
                }
            }
            v.performClick()
        }
        true
    }

    fun updateDeckAndViews () {
        item = dbHelper.getRandomExpressionByFolder()
        expression = item as ExpressionData
        val nextCumleCifti = dbHelper.getSentenceById(expression)
        setUIforTranslation()
        binding.tvOriginalSentence.setText(nextCumleCifti.second)
        targetSentence = nextCumleCifti.first
        binding.tvTarget.setText(targetSentence)
        binding.etTranslation.isEnabled = true
        binding.etTranslation.requestFocus()
        showKeyboard(activity)
        dolulukGoster()
    }

    fun showSentenceFromList (){
        val cumleCifti = dbHelper.getSentenceById(expression)
        binding.tvOriginalSentence.setText(cumleCifti.second)
        targetSentence = cumleCifti.first
        binding.tvTarget.setText(targetSentence)
    }

    fun dolulukGoster() {
        val counttoStudy = dbHelper.getCountToStudyInFolder()
        val anlikDolulukSayisi = dbHelper.getStudiedCountInFolder()
        binding.topBar.tvCountToStudy.setText(counttoStudy.toString())
        binding.topBar.tvStudiedCount.setText(anlikDolulukSayisi.toString())
        binding.topBar.txtFolderName.setText(dbHelper.getActiveStudyFolder().second.capitalizeFirstLetter())
        dolulukDegistirme(binding.topBar.viewCurrentOccupancy, counttoStudy, anlikDolulukSayisi, toplamDolulukWith, dpToPx(20))
    }

    fun checkTranslationWithBlank (sentenceInNativeLanguage:String, translationInTargetLanguage: String){
        feedbackTranslationCheck = null
        val queryWithBlank = getString(R.string.query_with_blank, sentenceInNativeLanguage, targetLanguage, translationInTargetLanguage, nativeLanguageInNativeLanguage)
        sendQuery(studyFolderApi, queryWithBlank){ apiResponse ->
            try {
                val responseJson = handleApiResponseWithBlank(apiResponse)
                val completedSentence = responseJson.getString("completeSentence").replace("**", "")
                feedbackTranslationCheck = responseJson.getString("feedBack").replace("**", "")
                binding.tvEvaluation.setText(feedbackTranslationCheck)
                getWordsInEvaluationByApi(feedbackTranslationCheck)

                if (completedSentence.equals(targetSentence)){
                    showHint(false)
                } else {
                    showHint(true)
                    binding.tvCompleteSentence.setText(completedSentence)
                }

                if (targetSentence.equals(translationInTargetLanguage)){
                    binding.ibTarget.visibility = View.GONE
                    binding.ib100.visibility = View.VISIBLE
                } else {
                    binding.ibTarget.visibility = View.VISIBLE
                    binding.ib100.visibility = View.GONE
                }

                binding.ibBlankEvaluation.visibility = View.VISIBLE
                binding.ibBlankEvaluation.bringToFront()

                degerlendirmeAsamasi = degerlendirmeAsamasi + 1

                if (isValidLanguageCheck != null){
                    if (isValidLanguageCheck == false) {
                        showHint(false)
                        binding.tvEvaluation.setText(activity.getString(R.string.wrong_translation_direction).replace("xxx", targetLanguageInNativeLanguage))
                        binding.ibWrongEvaluation.visibility = View.VISIBLE
                        binding.ibWrongEvaluation.bringToFront()
                    }
                    if (degerlendirmeAsamasi == 1){
                        showEvaluationByLength()
                    }
                }

            } catch (e: IllegalArgumentException) {
                Log.e("Api_response", e.message ?: "Unknown error")
                reSendToApi(::checkTranslationWithBlank, sentenceInNativeLanguage, translationInTargetLanguage)
            } catch (e: JSONException) {
                Log.e("Api_response", "Error parsing JSON: ${e.message}")
                reSendToApi(::checkTranslationWithBlank, sentenceInNativeLanguage, translationInTargetLanguage)
            }
            if (apiResponse == null){
                reSendToApi(::checkTranslationWithBlank, sentenceInNativeLanguage, translationInTargetLanguage)
            }
        }
    }

    fun checkTranslationNoBlank (sentenceInNativeLanguage: String, translationInTargetLanguage: String){
        feedbackTranslationCheck = null
        val queryNoBlank = getString(R.string.query_no_blank, sentenceInNativeLanguage, targetLanguage, translationInTargetLanguage, nativeLanguageInNativeLanguage)
        Log.d("query_no_blank", queryNoBlank)
        sendQuery(studyFolderApi, queryNoBlank){ apiResponse ->
            try {
                val responseJson = handleApiResponse(apiResponse)
                Log.d("api_response_no_blank", responseJson.toString())
                val isValidTranslationCheck = responseJson.getBoolean("isValid")
                feedbackTranslationCheck = responseJson.getString("feedBack").replace("**", "")
                binding.tvEvaluation.setText(feedbackTranslationCheck)
                degerlendirmeAsamasi = degerlendirmeAsamasi + 1
                showHint(false)
                getWordsInEvaluationByApi(feedbackTranslationCheck)

                if (isValidTranslationCheck) {
                    binding.ibCorrectEvaluation.visibility = View.VISIBLE
                    binding.ibCorrectEvaluation.bringToFront()

                } else {
                    binding.ibWrongEvaluation.visibility = View.VISIBLE
                    binding.ibWrongEvaluation.bringToFront()
                }

                if (targetSentence.equals(translationInTargetLanguage)){
                    binding.ibTarget.visibility = View.GONE
                    binding.ib100.visibility = View.VISIBLE
                } else {
                    binding.ibTarget.visibility = View.VISIBLE
                    binding.ib100.visibility = View.GONE
                }

                if (isValidLanguageCheck != null){
                    if (isValidLanguageCheck == false) {
                        showHint(false)
                        binding.tvEvaluation.setText(activity.getString(R.string.wrong_translation_direction).replace("xxx", targetLanguageInNativeLanguage))
                        binding.ibWrongEvaluation.visibility = View.VISIBLE
                        binding.ibWrongEvaluation.bringToFront()
                    }
                    if (degerlendirmeAsamasi == 1){
                        showEvaluationByLength()
                    }
                }

            } catch (e: IllegalArgumentException) {
                Log.e("Api_response", e.message ?: "Unknown error")
                reSendToApi(::checkTranslationNoBlank, sentenceInNativeLanguage, translationInTargetLanguage)
            } catch (e: JSONException) {
                Log.e("Api_response", "Error parsing JSON: ${e.message}")
                reSendToApi(::checkTranslationNoBlank, sentenceInNativeLanguage, translationInTargetLanguage)
            }
            if (apiResponse == null){
                reSendToApi(::checkTranslationNoBlank, sentenceInNativeLanguage, translationInTargetLanguage)
            }
        }
    }

    fun checkLanguageByApi(translationSentence: String) {
        isValidLanguageCheck = null

        val queryForLanguageCheck = """
            Please return true if the text in JSON below is in $targetLanguage, even if it has errors, otherwise return false:
            { "text": "$translationSentence" }
            Give your response strictly in the following JSON format:
            { isValid: "true or false", feedBack: "your feedback here."}
            """.trimIndent()
        Log.d("query_language_check", queryForLanguageCheck)
        sendQuery(studyFolderApi, queryForLanguageCheck) { apiResponse ->

            try {
                val responseJson = handleApiResponseDefault(apiResponse)
                Log.d("api_response_language_check", responseJson.toString())
                isValidLanguageCheck = responseJson.getBoolean("isValid")

                if (isValidLanguageCheck == false) {
                    showHint(false)
                    binding.tvEvaluation.setText(activity.getString(R.string.wrong_translation_direction).replace("xxx", targetLanguageInNativeLanguage))
                    binding.ibWrongEvaluation.visibility = View.VISIBLE
                    binding.ibWrongEvaluation.bringToFront()
                    showEvaluationByLength()
                } else {
                    getWordsInTranslationSentenceByApi(translationSentence)
                    if (degerlendirmeAsamasi == 1){
                        showEvaluationByLength()
                    }
                }

            } catch (e: IllegalArgumentException) {
                Log.e("Api_response", e.message ?: "Unknown error")
                reSendToApi(::checkLanguageByApi, translationSentence)
            } catch (e: JSONException) {
                Log.e("Api_response", "Error parsing JSON: ${e.message}")
                reSendToApi(::checkLanguageByApi, translationSentence)
            }
            if (apiResponse == null){
                reSendToApi(::checkLanguageByApi, translationSentence)
            }
        }

    }

    fun setViewsForCheckingTranslation (){
        binding.ibOriginalSentenceRefresh.isEnabled = false
        binding.ibEdit.isEnabled = false
        binding.topBar.txtFolderName.isEnabled = false
        binding.etTranslation.isEnabled = false
        binding.etTranslation.visibility = View.GONE
        binding.tvTranslation.text = binding.etTranslation.text
        binding.tvTranslation.visibility = View.VISIBLE
        binding.tvHint.visibility = View.GONE
        binding.ibEdit.visibility = View.VISIBLE
        binding.ibEditDone.visibility = View.GONE
        binding.ibShrugClose.visibility = View.GONE
        binding.ibShrug.visibility = View.GONE
        binding.progressBar.visibility = View.VISIBLE
        binding.ibSubmit.visibility = View.GONE
        binding.ibNext.visibility = View.GONE
        binding.cvEvaluation.visibility = View.GONE
        hidePronunciation()
    }

    fun checkTranslationSender(){
        if (mainViewModel.isSubscribed.value.first || (!mainViewModel.isSubscribed.value.first && translationQuota > 0)){
            resendCount=0
            kisaMesaj(binding.cvEvaluation, binding.root) // Kısa mesaj-uzun mesaj mantığının çalışması için ilk önce kısa mesaj ayarlı olması gerekiyor.
            degerlendirmeAsamasi = 0
            setViewsForCheckingTranslation()
            val sentenceInNativeLanguage = binding.tvOriginalSentence.text.toString().trim()
            val translationInTargetLanguage = binding.etTranslation.text.toString().trim()
            lifecycleScope.launch {
                if (hasRealInternetAccess()) {
                    if (translationInTargetLanguage == "----") {
                        giveTranslation(sentenceInNativeLanguage)
                    } else {
                        checkLanguageByApi(translationInTargetLanguage)
                        if (translationInTargetLanguage.contains("____")){
                            checkTranslationWithBlank(sentenceInNativeLanguage, translationInTargetLanguage)
                        } else {
                            checkTranslationNoBlank(sentenceInNativeLanguage, translationInTargetLanguage)
                        }
                    }
                } else {
                    makeToast(getString(R.string.no_internet))
                    setViewsHata()
                }
            }
        } else {
            showDialog(R.layout.dialog_no_quota, requireActivity(), false)
            setUIforTranslation()
        }
    }

    fun giveTranslation(originalSentence: String){

        val queryForNoTranslation = getString(R.string.query_for_no_translation, nativeLanguageInNativeLanguage, originalSentence, targetLanguageInNativeLanguage)

        sendQuery(studyFolderApi, queryForNoTranslation){ apiResponse ->
            try {
                val responseJson = handleApiResponseDefault(apiResponse)
                val explanation = responseJson.getString("explanation").replace("**", "")
                binding.tvEvaluation.setText(explanation)
                binding.tvTarget.setText(targetSentence)

                showHint(false)
                binding.ibTarget.visibility = View.VISIBLE
                binding.ib100.visibility = View.GONE
                binding.ibCorrectEvaluation.visibility = View.GONE
                binding.ibWrongEvaluation.visibility = View.GONE
                binding.ibBlankEvaluation.visibility = View.GONE
                binding.ibDetailedTranslation.visibility = View.VISIBLE

                getWordsInEvaluationByApi(explanation)
                showEvaluationByLength()


            } catch (e: IllegalArgumentException) {
                Log.e("Api_response_1", e.message ?: "Unknown error")
                reSendToApi(::giveTranslation, originalSentence)
            } catch (e: JSONException) {
                Log.e("Api_response_2", "Error parsing JSON: ${e.message}")
                reSendToApi(::giveTranslation, originalSentence)
            }
            if (apiResponse == null){
                Log.e("Api_response_3", "aaaaaaaaaaaaaa")
                reSendToApi(::giveTranslation, originalSentence)
            }
        }
    }

    fun getWordsInEvaluationByApi(string: String?)  {

        val evaluation: String
        if (string == null) {
            evaluation = "I want to talk to him."
        } else {
            evaluation = string
        }

        val query = """
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
        Log.d("query_words_evaluation", query)
        sendQuery(studyFolderApi, query){ apiResponse ->
            try {
                val responseJson = handleApiResponseDefault(apiResponse)
                val jsonArray  = responseJson.getJSONArray("words")
                Log.d("api_response_words_evaluation", responseJson.toString())
                wordsOfEvaluation.clear()
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    for (word in item.getString("word").lowercase(getLocale(targetLanguageCode)).split(Regex("""[\s,.!?()"]+"""))) {
                        if (word.isNotBlank()) {
                            wordsOfEvaluation.add(word)
                        }
                    }
                }
            } catch (e: IllegalArgumentException) {
                Log.e("Api_response", e.message ?: "Unknown error")
                reSendToApi(::getWordsInEvaluationByApi, evaluation)
            } catch (e: JSONException) {
                Log.e("Api_response", "Error parsing JSON: ${e.message}")
                reSendToApi(::getWordsInEvaluationByApi, evaluation)
            }
            if (apiResponse == null){
                reSendToApi(::getWordsInEvaluationByApi, evaluation)
            }
        }

    }

    fun getWordsInTranslationSentenceByApi(translationSentence: String) {
        wordsAndCorrections.clear()
        val queryForTypoesInTranslation = """
                       You will be given a list of words. 
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
        Log.d("query_words_in_translation", queryForTypoesInTranslation)
        sendQuery(studyFolderApi, queryForTypoesInTranslation){ apiResponse ->

            // Gelen JSON verisini işleme
            val gson = Gson()
            var text = apiResponse?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text

            if (text != null) {
                text = text.replace("json", "").trim()
                text = text.replace("```", "").trim()
                Log.d("api_response_words_in_translation", text.toString())
                try {
                    val misspelledWords = gson.fromJson(text, MisspelledWords::class.java)
                    wordsAndCorrections = misspelledWords.wordsAndCorrections

                    highlightTypos(
                        textView = binding.tvTranslation,
                        wordsAndCorrections = wordsAndCorrections
                    )

                } catch (e: IllegalArgumentException) {
                    Log.e("Api_response", e.message ?: "Unknown error")
                    reSendToApi(::getWordsInTranslationSentenceByApi, translationSentence)
                } catch (e: JSONException) {
                    Log.e("Api_response", "Error parsing JSON: ${e.message}")
                    reSendToApi(::getWordsInTranslationSentenceByApi, translationSentence)
                }
            }
            if (apiResponse == null){
                reSendToApi(::getWordsInTranslationSentenceByApi, translationSentence)
            }
        }

    }

    fun showEvaluationByLength (){
        if (!mainViewModel.isSubscribed.value.first) {
            mainViewModel.decreaseTranslationCount()
        }
        translationQuota = prefsRepository.getTranslationQuota()
        if (translationQuota == 0){
            showDialog(R.layout.dialog_no_quota, requireActivity(), false)
        }
        binding.scrollViewEvaluation.scrollTo(0, 0)
        binding.progressBar.visibility = View.GONE
        binding.ibNext.visibility = View.VISIBLE
        binding.ibSubmit.visibility = View.GONE
        binding.ibOriginalSentenceRefresh.isEnabled = true
        binding.ibEdit.isEnabled = true
        binding.topBar.txtFolderName.isEnabled = true

        // cvDeğerlendirmeyi ekran dışına al
        binding.cvEvaluation.visibility = View.VISIBLE
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        binding.cvEvaluation.translationX = screenWidth.toFloat()

        binding.tvEvaluation.post {
            val cardViewPosition = IntArray(2)
            val buttonPosition = IntArray(2)
            binding.cvEvaluation.getLocationOnScreen(cardViewPosition)
            binding.ibNext.getLocationOnScreen(buttonPosition)
            val cardViewBottom = cardViewPosition[1] + binding.cvEvaluation.height
            val buttonTop = buttonPosition[1]
            if (cardViewBottom > buttonTop - dpToPx(15) ) {
                Log.d ("mesaj:", "uzun mesaj")
                isUzun = true
                uzunMesaj(binding.cvEvaluation, binding.root, binding.ibNext)
            } else {
                isUzun = false
                kisaMesaj(binding.cvEvaluation, binding.root)
                Log.d ("mesaj:", "kısa mesaj")
            }
        }
    }

    fun showRightArrow(){
        binding.cvUnknown.root.visibility = View.VISIBLE
        binding.cvBreak.visibility = View.GONE
        binding.cvOriginalSentence.visibility = View.GONE
        binding.cvTranslation.visibility = View.GONE
        binding.cvEvaluation.visibility = View.GONE
        binding.ibSubmit.visibility = View.GONE
        binding.topBar.tvCountToStudy.visibility = View.GONE
        binding.topBar.tvStudiedCount.visibility = View.GONE
        binding.topBar.viewCurrentOccupancy.visibility = View.GONE
        binding.topBar.viewFullOccupancy.visibility = View.GONE
        binding.ibNext.visibility = View.GONE
        binding.progressBar.visibility = View.GONE
        binding.cvToast.visibility = View.GONE
        hidePronunciation()
    }

    fun showBreak () {
        binding.cvBreak.visibility = View.VISIBLE
        binding.cvTranslation.visibility = View.GONE
        binding.cvOriginalSentence.visibility = View.GONE
        binding.ibSubmit.visibility = View.GONE
        binding.ibNext.visibility = View.GONE
        binding.cvEvaluation.visibility = View.GONE
        binding.cvUnknown.root.visibility = View.GONE
        binding.topBar.tvCountToStudy.visibility = View.VISIBLE
        binding.topBar.tvStudiedCount.visibility = View.VISIBLE
        binding.topBar.viewCurrentOccupancy.visibility = View.VISIBLE
        binding.topBar.viewFullOccupancy.visibility = View.VISIBLE
        binding.ibNext.visibility = View.GONE
        binding.progressBar.visibility = View.GONE
        binding.cvToast.visibility = View.GONE
        hidePronunciation()
    }

    fun setUIforTranslation(){
        binding.etTranslation.isEnabled = true
        binding.etTranslation.visibility = View.VISIBLE
        binding.tvTranslation.visibility = View.GONE
        binding.cvOriginalSentence.visibility = View.VISIBLE
        binding.ibSubmit.visibility = View.VISIBLE
        binding.cvTranslation.visibility = View.VISIBLE
        binding.etTranslation.text?.clear()
        binding.ibNext.visibility = View.GONE
        binding.ibEdit.visibility = View.GONE
        binding.ibEditDone.visibility = View.GONE
        binding.tvHint.visibility = View.VISIBLE
        binding.cvEvaluation.visibility = View.GONE
        binding.cvUnknown.root.visibility = View.GONE
        binding.cvBreak.visibility = View.GONE
        binding.progressBar.visibility = View.GONE
        binding.ibShrugClose.visibility = View.GONE
        binding.ibShrug.visibility = View.VISIBLE
        hidePronunciation()
        //binding.ibMenu.visibility = View.VISIBLE
        binding.topBar.tvCountToStudy.visibility = View.VISIBLE
        binding.topBar.tvStudiedCount.visibility = View.VISIBLE
        binding.topBar.viewCurrentOccupancy.visibility = View.VISIBLE
        binding.topBar.viewFullOccupancy.visibility = View.VISIBLE
    }

    fun showHint(visibility: Boolean){
        if (visibility){
            binding.viewHint.visibility = View.VISIBLE
            binding.tvCompleteSentence.visibility = View.VISIBLE
            binding.ibBlankEvaluation.visibility = View.VISIBLE
            binding.ibWrongEvaluation.visibility = View.GONE
            binding.ibCorrectEvaluation.visibility = View.GONE
        } else {
            binding.viewHint.visibility = View.GONE
            binding.tvCompleteSentence.visibility = View.GONE
            binding.ibBlankEvaluation.visibility = View.GONE
        }
    }

    private val keyboardListener = ViewTreeObserver.OnGlobalLayoutListener {
        val rect = Rect()
        binding.root.getWindowVisibleDisplayFrame(rect)
        val screenHeight = binding.root.height
        val keyboardHeight = screenHeight - rect.height()
        val isKeyboardVisible = keyboardHeight > screenHeight * 0.15
        Log.d("keyboardListener-list", "list")
        val layoutParams = binding.cvEvaluation.layoutParams as ViewGroup.MarginLayoutParams

        if (isKeyboardVisible && layoutParams.bottomMargin != keyboardHeight - dpToPx(111).roundToInt()) {
            Log.d("keyboardListener-list", "klavye açıldı: $keyboardHeight")

            layoutParams.bottomMargin = keyboardHeight - dpToPx(111).roundToInt()
            binding.cvEvaluation.layoutParams = layoutParams
        }
        if (!isKeyboardVisible && layoutParams.bottomMargin != dpToPx(15).roundToInt()){
            Log.d("keyboardListener-list", "klavye kapandı: $keyboardHeight")

            layoutParams.bottomMargin = dpToPx(15).roundToInt()
            binding.cvEvaluation.layoutParams = layoutParams
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        _binding?.let { binding ->
            if (hidden) {
                // Fragment gizlendiğinde yapılacak işlemler
                binding.root.viewTreeObserver.removeOnGlobalLayoutListener(keyboardListener)
                Log.d ("StudyFolder", "onhidden fragment gizlendi")
            } else {
                // Fragment görünür olduğunda yapılacak işlemler
                binding.root.viewTreeObserver.addOnGlobalLayoutListener(keyboardListener)
                mainViewModel.checkSubscription()
                mainViewModel.checkAndResetDailyQuota()
                Log.d ("StudyFolder", "onhidden fragment görünür hale geldi")
            }
        }
    }

    private fun makeToast(s: String){
        binding.tvToast.setText(s)
        stopToastAnimation(directionAnimator)
        directionAnimator = startToastAnimation(binding.cvToast)
    }

    fun setViewsHata (){
        // Görünümler sıfırlanıyor ve yazılan çeviri korunuyor.
        val ceviri = binding.etTranslation.text.toString()
        setUIforTranslation()
        if (ceviri == "----"){
            binding.etTranslation.text?.clear()
        } else {
            binding.etTranslation.setText(ceviri)
        }
    }

    fun reSendToApi (action: (String) -> Unit, param1: String): Boolean {
        resendCount += 1
        if (resendCount < 5){
            Log.d("reSendToApi", "reSendToApi çalıştı")
            action(param1)
            return false
        } else {
            val text = activity.getString(R.string.api_busy) + " " + activity.getString(R.string.try_again_later)
            makeToast(text)
            setViewsHata()
            return true
        }
    }

    fun reSendToApi (action: (String, String) -> Unit, param1: String, param2: String): Boolean {
        resendCount += 1
        if (resendCount < 5){
            Log.d("reSendToApi", "reSendToApi çalıştı")
            action(param1, param2)
            return false
        } else {
            val text = activity.getString(R.string.api_busy) + " " + activity.getString(R.string.try_again_later)
            makeToast(text)
            setViewsHata()
            return true
        }
    }

    fun hidePronunciation(){
        binding.cvPronunciationShort.visibility = View.GONE
        binding.cvPronunciationLong.visibility = View.GONE
        binding.cvTypoPronunciationShort.visibility = View.GONE
        binding.cvTypoPronunciationLong.visibility = View.GONE
    }

    fun showPronunciation(word: String){
        binding.hsvPronunciation.scrollTo(0, 0)

        // görünümleri en aşağı veya yukarı taşı
        binding.cvPronunciationShort.visibility = View.VISIBLE
        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        binding.cvPronunciationShort.translationY = screenHeight.toFloat()

        // yazıları ayarla
        binding.tvPronunciation1.setText(word)
        binding.tvPronunciation2.setText(word)

        binding.tvPronunciation2.post {
            val cardViewPosition = IntArray(2)
            val buttonNextPosition = IntArray(2)
            val buttonMenuPosition = IntArray(2)
            binding.cvPronunciationShort.getLocationOnScreen(cardViewPosition)
            binding.ibNextGuide.getLocationOnScreen(buttonNextPosition)
            binding.ibBottomLeftGuide.getLocationOnScreen(buttonMenuPosition)
            val cardViewEnd = cardViewPosition[0] + binding.cvPronunciationShort.width
            val buttonNextStart = buttonNextPosition[0]
            if (cardViewEnd > buttonNextStart - dpToPx(10)) {
                pronunciationAnimation(binding.cvPronunciationLong)
                binding.cvPronunciationLong.post{
                    binding.hsvPronunciation.fullScroll(View.FOCUS_RIGHT)
                }
            } else {
                binding.cvPronunciationLong.visibility = View.GONE
                binding.cvPronunciationShort.translationY = 0f
                pronunciationAnimation(binding.cvPronunciationShort)
            }
        }
    }

    fun showTypoPronunciation(original: String, correction: String){
        binding.hsvTypoPronunciation.scrollTo(0, 0)

        // görünümleri en aşağı veya yukarı taşı
        binding.cvTypoPronunciationShort.visibility = View.VISIBLE
        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        binding.cvTypoPronunciationShort.translationY = screenHeight.toFloat()

        // yazıları ayarla
        binding.tvMisspelled1.setText(original)
        binding.tvMisspelled2.setText(original)
        binding.tvCorrection1.setText(correction)
        binding.tvCorrection2.setText(correction)

        binding.tvCorrection2.post {
            val cardViewPosition = IntArray(2)
            val buttonNextPosition = IntArray(2)
            val buttonMenuPosition = IntArray(2)
            binding.cvTypoPronunciationShort.getLocationOnScreen(cardViewPosition)
            binding.ibNextGuide.getLocationOnScreen(buttonNextPosition)
            binding.ibBottomLeftGuide.getLocationOnScreen(buttonMenuPosition)
            val cardViewEnd = cardViewPosition[0] + binding.cvTypoPronunciationShort.width
            val buttonNextStart = buttonNextPosition[0]
            if (cardViewEnd > buttonNextStart - dpToPx(10)) {
                pronunciationAnimation(binding.cvTypoPronunciationLong)
                binding.cvTypoPronunciationLong.post{
                    binding.hsvTypoPronunciation.fullScroll(View.FOCUS_RIGHT)
                }
            } else {
                binding.cvTypoPronunciationLong.visibility = View.GONE
                binding.cvTypoPronunciationShort.translationY = 0f
                pronunciationAnimation(binding.cvTypoPronunciationShort)
            }
        }
    }


}







