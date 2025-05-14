package com.transist.ui.main.study.random

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.transist.R
import com.transist.data.remote.response.MisspelledWord
import com.transist.data.repository.ApiRepository
import com.transist.data.repository.StudyRepository
import com.transist.databinding.FragmentStudyRandomBinding
import com.transist.ui.main.MainViewModel
import com.transist.util.dpToPx
import com.transist.util.extractWordAtOffset
import com.transist.util.getLocale
import com.transist.util.getLocalizedContext
import com.transist.util.getOffsetForPosition
import com.transist.util.getQueryNoBlank
import com.transist.util.getQueryWithBlank
import com.transist.util.getStringId
import com.transist.util.hasLetter
import com.transist.util.highlightTypos
import com.transist.util.initTtsWithTargetLanguageSupport
import com.transist.util.ipucuClick
import com.transist.util.pronunciationAnimation
import com.transist.util.showDialogNoVoiceEngine
import com.transist.util.showKeyboard
import com.transist.util.shrugClick
import com.transist.util.startToastAnimation
import com.transist.util.stopToastAnimation
import com.transist.util.pronunciationClick
import com.transist.util.kisaMesaj
import com.transist.util.replaceBlanksWithDrawable
import com.transist.util.shouldShowPronunciation
import com.transist.util.showDialog
import com.transist.util.startBlinkingAnimation
import com.transist.util.stopBlinkingAnimation
import com.transist.util.uzunMesaj
import kotlinx.coroutines.launch

class StudyRandomFragment : Fragment() {

    private var _binding: FragmentStudyRandomBinding? = null
    private val binding get() = _binding!!  // Null güvenliği için

    var translationCount = 0
    var nativeLanguageCode = "--"
    var targetLanguageCode = "--"
    var targetLanguage = "Unknown"
    var nativeLanguage = "Unknown"
    var targetLanguageStringId = 0
    var targetLanguageInNativeLanguage = "Unknown"
    var nativeLanguageStringId = 0
    var nativeLanguageInNativeLanguage = "Unknown"
    private lateinit var localizedContext: Context
    var orientation = 0

    var lastProcessedWords: List<MisspelledWord>? = null
    private lateinit var lastSelectedLevel: String

    var blinkingAnimator: List<ObjectAnimator>? = null
    private var directionAnimator: Animator? = null
    private var toastAnimator: Animator? = null

    private lateinit var tts: TextToSpeech
    private var isTTsExist = false
    private lateinit var viewModel: StudyViewModel
    private val mainViewModel: MainViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStudyRandomBinding.inflate(inflater, container, false)
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

        val factory = StudyViewModelFactory(
            StudyRepository(requireContext()), ApiRepository()
        )
        viewModel = ViewModelProvider(this, factory).get(StudyViewModel::class.java)

        orientation = resources.configuration.orientation

        observerViewModel()
        binding.cvToast.visibility = View.GONE
    }

    private fun setupUI(){
        // Önce tüm yüklü ses motorlarını bulup, sonra herhangi birinde dil dosyası yüklü mü diye bakıyoruz.
        initTtsWithTargetLanguageSupport(requireActivity(), targetLanguageCode) { tts1 ->
            if (tts1 != null) {
                isTTsExist = true
                tts = tts1
            } else {
                // ❌ Hiçbir motor Türkçe desteklemiyor, kullanıcı ayarlara yönlendirilecek
                isTTsExist = false
            }
        }

        binding.topBar.a1.setOnClickListener {
            viewModel.setActiveLevelState("A1")
        }

        binding.topBar.a2.setOnClickListener {
            viewModel.setActiveLevelState("A2")
        }

        binding.topBar.b1.setOnClickListener {
            viewModel.setActiveLevelState("B1")
        }

        binding.topBar.b2.setOnClickListener {
            viewModel.setActiveLevelState("B2")
        }

        binding.topBar.c1.setOnClickListener {
            viewModel.setActiveLevelState("C1")
        }

        binding.ibPronunciation1.setOnClickListener {
            if (isTTsExist) {
                pronunciationClick(binding.tvPronunciation1.text.toString(), tts)
            } else {
                showDialogNoVoiceEngine(requireContext())
            }
        }

        binding.ibPronunciation2.setOnClickListener {
            if (isTTsExist) {
                pronunciationClick(binding.tvPronunciation2.text.toString(), tts)
            } else {
                showDialogNoVoiceEngine(requireContext())
            }
        }

        binding.ibPronunciationTypo2.setOnClickListener {
            if (isTTsExist) {
                pronunciationClick(binding.tvCorrection2.text.toString(), tts)
            } else {
                showDialogNoVoiceEngine(requireContext())
            }
        }

        binding.ibPronunciationTypo1.setOnClickListener {
            if (isTTsExist) {
                pronunciationClick(binding.tvCorrection1.text.toString(), tts)
            } else {
                showDialogNoVoiceEngine(requireContext())
            }
        }

        binding.ibOriginalSentenceRefresh.setOnClickListener {
            viewModel.sentenceGetter()
            setUiForTranslation()
        }

        binding.tvHint.setOnClickListener {
            ipucuClick(requireContext(), binding.etTranslation)
        }

        binding.ibEdit.setOnClickListener {

            if (binding.etTranslation.text.toString() == "----") {
                binding.etTranslation.text?.clear()
            }

            binding.tvHint.visibility = View.VISIBLE
            binding.ibEditDone.visibility = View.VISIBLE
            binding.tvTranslation.visibility = View.GONE
            binding.etTranslation.visibility = View.VISIBLE
            binding.etTranslation.isEnabled = true
            binding.etTranslation.requestFocus()
            showKeyboard(requireActivity())

            binding.ibEdit.visibility = View.GONE
        }

        binding.ibEditDone.setOnClickListener {
            checkTranslationSender()
        }

        binding.ibSubmit.setOnClickListener {
            val inputText = binding.etTranslation.text.toString().trim()
            if (inputText.hasLetter() || inputText == "----") {
                checkTranslationSender()
            } else {
                makeToast(requireContext().getString(R.string.no_given_translation))
            }
        }

        binding.ibShrug.setOnClickListener {
            shrugClick(requireContext(), binding.etTranslation)
            binding.etTranslation.isEnabled = false
            binding.ibShrug.visibility = View.GONE
            binding.tvHint.visibility = View.GONE
            binding.ibEdit.visibility = View.VISIBLE
            checkTranslationSender()
        }

        binding.ibNext.setOnClickListener {
            setUiForTranslation()
            viewModel.sentenceGetter()
            binding.etTranslation.requestFocus()
            showKeyboard(requireActivity())
        }

        binding.ibSecondCheck.setOnClickListener {
            checkTranslationSender()
        }

        binding.ibReverseTranslation.setOnClickListener {
            if (viewModel.reverseTranslation == false) {

                viewModel.reverseTranslation = true
                viewModel.originalSentenceLanguage = targetLanguage
                viewModel.translationSentenceLanguage = nativeLanguage
                viewModel.translationSentenceLanguageInNativeLanguage = nativeLanguageInNativeLanguage
                viewModel.originalSentenceLanguageCode = targetLanguageCode
                viewModel.translationSentenceLanguageCode = nativeLanguageCode

                binding.tvDirectionFrom.text = targetLanguageInNativeLanguage
                binding.tvDirectionTo.text = nativeLanguageInNativeLanguage

                binding.tvOriginalSentence.setOnTouchListener(sentenceTouchlistener())
                binding.tvTranslation.setOnTouchListener(null)
                viewModel.sentenceGetter()
                setUiForTranslation()

            } else {

                viewModel.reverseTranslation = false
                viewModel.originalSentenceLanguage = nativeLanguage
                viewModel.translationSentenceLanguage = targetLanguage
                viewModel.translationSentenceLanguageInNativeLanguage = targetLanguageInNativeLanguage
                viewModel.originalSentenceLanguageCode = nativeLanguageCode
                viewModel.translationSentenceLanguageCode = targetLanguageCode

                binding.tvDirectionFrom.text = nativeLanguageInNativeLanguage
                binding.tvDirectionTo.text = targetLanguageInNativeLanguage

                binding.tvOriginalSentence.setOnTouchListener(null)
                binding.tvTranslation.setOnTouchListener(translationClickListener())
                viewModel.sentenceGetter()
                setUiForTranslation()
            }
            stopToastAnimation(directionAnimator)
            directionAnimator = startToastAnimation(binding.cvTranslationDirection)
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

        binding.tvEvaluation.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val offset = getOffsetForPosition(binding.tvEvaluation, event.x, event.y)
                val word = extractWordAtOffset(binding.tvEvaluation.text.toString(), offset).lowercase(
                    getLocale(targetLanguageCode)
                )
                hidePronunciation()
                val displayWord = shouldShowPronunciation(word, viewModel.wordsOfEvaluation)
                if (displayWord.first) {
                    val match = viewModel.wordsAndCorrections.value.firstOrNull { it.original == displayWord.second }
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

        binding.tvTranslation.setOnTouchListener(translationClickListener())

        binding.tvTarget.setOnTouchListener (sentenceTouchlistener())

        binding.tvCompleteSentence.setOnTouchListener (sentenceTouchlistener())

        binding.tvOriginalSentence.setOnTouchListener(null)
    }

    private fun observerViewModel(){
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.userLanguagesCodes.collect { userLanguagesCodes ->
                    var b = false
                    if (nativeLanguageCode == "--" && targetLanguageCode == "--") {
                        b = true
                    }

                    if (nativeLanguageCode != userLanguagesCodes.first || targetLanguageCode != userLanguagesCodes.second){
                        b = true
                    }

                    if (b){
                        nativeLanguageCode = userLanguagesCodes.first
                        targetLanguageCode = userLanguagesCodes.second
                        if (targetLanguageCode == "un" || nativeLanguageCode == "un") {
                            showRightArrow()
                        } else {
                            nativeLanguage = mainViewModel.userLanguages.value.first
                            targetLanguage = mainViewModel.userLanguages.value.second
                            viewModel.originalSentenceLanguage = nativeLanguage
                            viewModel.originalSentenceLanguageCode = nativeLanguageCode
                            viewModel.translationSentenceLanguage = targetLanguage
                            viewModel.translationSentenceLanguageCode = targetLanguageCode

                            localizedContext = getLocalizedContext(requireContext(), nativeLanguageCode)
                            targetLanguageStringId = getStringId(targetLanguage)
                            targetLanguageInNativeLanguage = localizedContext.getString(targetLanguageStringId)
                            nativeLanguageStringId = getStringId(nativeLanguage)
                            nativeLanguageInNativeLanguage = localizedContext.getString(nativeLanguageStringId)
                            viewModel.translationSentenceLanguageInNativeLanguage = targetLanguageInNativeLanguage

                            lastSelectedLevel = "A1"
                            viewModel.setActiveLevelState("--")
                            setupUI()
                        }
                    }

                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loadingEvaluation.collect { isLoading ->
                    if (isLoading){
                        binding.etTranslation.isEnabled = false
                        if (binding.etTranslation.text.toString() != "----"){
                            val translation = binding.etTranslation.text.toString()
                            binding.etTranslation.visibility = View.GONE
                            replaceBlanksWithDrawable(requireContext(), binding.tvTranslation, translation)
                            binding.tvTranslation.visibility = View.VISIBLE
                        }
                        binding.tvHint.visibility = View.GONE
                        binding.ibEdit.visibility = View.VISIBLE
                        binding.ibEditDone.visibility = View.GONE
                        binding.ibShrugClose.visibility = View.GONE
                        binding.ibShrug.visibility = View.GONE
                        binding.etTranslation.setTextColor(ContextCompat.getColor(requireContext(), R.color.edit_gray))
                        binding.progressBar.visibility = View.VISIBLE
                        binding.ibSubmit.visibility = View.GONE
                        binding.ibNext.visibility = View.GONE
                        binding.cvEvaluation.visibility = View.GONE
                        hidePronunciation()
                        areViewsClickable(false)
                    } else {
                        areViewsClickable(true)
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.explanation.collect { explanation ->
                    binding.tvEvaluation.text = explanation
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isValidTranslationCheck.collect { isValidTranslationCheck ->
                    if (isValidTranslationCheck != null){
                        if (isValidTranslationCheck) {
                            Log.d ("Debug", "isValid: True")
                            // Cümle geçerli, yeşil arka plan yap
                            binding.ibCorrectEvaluation.visibility = View.VISIBLE
                            binding.ibCorrectEvaluation.bringToFront()
                        } else {
                            Log.d ("Debug", "isValid: False")
                            // Cümle geçerli değil, kırmızı arka plan yap
                            binding.ibWrongEvaluation.visibility = View.VISIBLE
                            binding.ibWrongEvaluation.bringToFront()
                        }
                    }

                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.targetTranslation.collect { targetTranslation ->
                    binding.tvTarget.text = targetTranslation
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.showTarget.collect { showTarget ->
                    if (showTarget) {
                        binding.tvTarget.visibility = View.VISIBLE
                        binding.ibTarget.visibility = View.VISIBLE
                        binding.ibDetailedTranslation.visibility = View.VISIBLE
                        binding.ibBlankEvaluation.visibility = View.GONE
                        binding.ibWrongEvaluation.visibility = View.GONE
                        binding.ibCorrectEvaluation.visibility = View.GONE
                    } else {
                        binding.tvTarget.visibility = View.GONE
                        binding.ibTarget.visibility = View.GONE
                        binding.ibDetailedTranslation.visibility = View.GONE
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.showHint.collect { showHint ->
                    if (showHint){
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
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.blinking.collect { blinking ->
                    Log.d("DEBUGG", "Blinking value: $blinking")
                    if (blinking){
                        viewModel.setBlinkingSentence(getString(R.string.creating_sentence))
                        if (blinkingAnimator == null){
                            val views = listOf(binding.tvOriginalSentence, binding.ibOriginalSentenceRefresh, binding.ibReverseTranslation)
                            blinkingAnimator = startBlinkingAnimation(*views.toTypedArray())
                        }
                        areViewsClickable(false)
                    } else {
                        stopBlinkingAnimation(binding.tvOriginalSentence, blinkingAnimator)
                        blinkingAnimator = null
                        areViewsClickable(true)
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.showEvaluationState.collect { state ->
                    if (state.allDone) {
                        if (state.isValidLanguage == true){
                            if (!mainViewModel.isSubscribed.value.first) {
                                mainViewModel.decreaseTranslationCount()
                            }
                            showEvaluationByLength()
                        } else {
                            binding.tvEvaluation.setText(requireContext().getString(R.string.wrong_translation_direction, "\"$targetLanguageInNativeLanguage\""))
                            binding.ibWrongEvaluation.visibility = View.VISIBLE
                            binding.ibWrongEvaluation.bringToFront()
                            showEvaluationByLength()
                        }
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.randomSentence.collect { randomSentence ->
                    binding.tvOriginalSentence.text = randomSentence
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.completeSentence.collect { completeSentence ->
                    binding.tvCompleteSentence.text = completeSentence
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.wordsAndCorrections.collect { misspelledWords ->
                    if (lastProcessedWords != misspelledWords) {
                        highlightTypos(
                            textView = binding.tvTranslation,
                            wordsAndCorrections = misspelledWords
                        )
                        lastProcessedWords = misspelledWords
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.activeLevel
                    .collect { activeLevel ->
                        if (activeLevel == "--"){
                            viewModel.setActiveLevelState("A1")
                            lastSelectedLevel = "A1"
                            setLevel("A1")
                            setUiForTranslation()
                            viewModel.sentenceGetter()
                        } else if (lastSelectedLevel != activeLevel){
                            lastSelectedLevel = activeLevel
                            setLevel(activeLevel)
                            setUiForTranslation()
                            viewModel.sentenceGetter()
                        }

                    }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.hasInternet
                    .collect { boolean ->
                        if (boolean == false){
                            makeToast("İnternet bağlantınızda bir sorun var.")
                            setViewsForError()
                        }
                    }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.apiErrorCritical
                    .collect { boolean ->
                        if (boolean == true){
                            val text = getString(R.string.api_busy) + " " + getString(R.string.try_again_later)
                            makeToast(text)
                            setViewsForError()
                        }
                    }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.apiErrorBasic
                    .collect { boolean ->
                        if (boolean == true){
                            val text = getString(R.string.fail_some_features) + " " + getString(R.string.try_again_later)
                            makeToast(text)
                        }
                    }
            }
        }

        mainViewModel.translation_count.observe(viewLifecycleOwner) { count ->
            if (count != translationCount){
                if (count == 0){
                    showDialog(R.layout.dialog_no_quota, requireActivity(), false)
                }
                translationCount = count ?: 0
            }
        }

    }

    fun sentenceTouchlistener () = View.OnTouchListener { v, event ->
        if (event.action == MotionEvent.ACTION_DOWN) {
            val tv = v as TextView
            val offset = getOffsetForPosition(tv, event.x, event.y)
            val word = extractWordAtOffset(tv.text.toString(), offset).lowercase(getLocale(targetLanguageCode))
            hidePronunciation()
            if (word.hasLetter() ){
                showPronunciation(word)
            }
            v.performClick()
        }
        true
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        _binding?.let { binding ->
            if (hidden) {
                // Fragment gizlendiğinde yapılacak işlemler
                Log.d ("StudyRandom", "random fragment gizlendi")
            } else {
                // Fragment görünür olduğunda yapılacak işlemler
                mainViewModel.checkSubscription()
                mainViewModel.checkAndResetDailyQuota()
                Log.d ("StudyRandom", "random fragment görünür hale geldi")
            }
        }
    }

    fun translationClickListener() = View.OnTouchListener { v, event ->
        if (event.action == MotionEvent.ACTION_DOWN) {
            val offset = getOffsetForPosition(binding.tvTranslation, event.x, event.y)
            val word = extractWordAtOffset(binding.tvTranslation.text.toString(), offset).lowercase(
                getLocale(targetLanguageCode)
            )
            hidePronunciation()
            if (word.isNotEmpty()){
                val match = viewModel.wordsAndCorrections.value.firstOrNull { it.original == word }
                if (match != null && match.original.hasLetter()){
                    if (match.original == match.correction) {
                        showPronunciation(word)
                    } else {
                        showTypoPronunciation(match.original, match.correction)
                    }
                }
            }
            v.performClick()
        }
        true
    }

    fun showRightArrow(){
        binding.topBar.horizontalBar.visibility = View.GONE
        binding.topBar.a1.visibility = View.GONE
        binding.topBar.a2.visibility = View.GONE
        binding.topBar.b1.visibility = View.GONE
        binding.topBar.b2.visibility = View.GONE
        binding.topBar.c1.visibility = View.GONE
        binding.cvTranslation.visibility = View.GONE
        binding.cvOriginalSentence.visibility = View.GONE
        binding.cvEvaluation.visibility = View.GONE
        hidePronunciation()
        binding.ibNext.visibility = View.GONE
        binding.ibSubmit.visibility = View.GONE
        binding.progressBar.visibility = View.GONE
        binding.cvUnknown.root.visibility = View.VISIBLE
        binding.cvTranslationDirection.visibility = View.GONE
    }

    fun setLevel(activeLevel: String){

        val textView = when (activeLevel) {
            "A1" -> binding.topBar.a1
            "A2" -> binding.topBar.a2
            "B1" -> binding.topBar.b1
            "B2" -> binding.topBar.b2
            "C1" -> binding.topBar.c1
            else -> binding.topBar.a1
        }

        binding.topBar.a1.background.setTint(ContextCompat.getColor(requireContext(), R.color.acik_yesil))
        binding.topBar.a2.background.setTint(ContextCompat.getColor(requireContext(), R.color.acik_yesil))
        binding.topBar.b1.background.setTint(ContextCompat.getColor(requireContext(), R.color.acik_yesil))
        binding.topBar.b2.background.setTint(ContextCompat.getColor(requireContext(), R.color.acik_yesil))
        binding.topBar.c1.background.setTint(ContextCompat.getColor(requireContext(), R.color.acik_yesil))

        textView.background.setTint(ContextCompat.getColor(requireContext(), R.color.koyu_yesil))
    }

    fun showEvaluationByLength (){

        binding.progressBar.visibility = View.GONE
        binding.ibNext.visibility = View.VISIBLE
        binding.ibSubmit.visibility = View.GONE
        binding.cvEvaluation.visibility = View.VISIBLE

        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            kisaMesaj(binding.cvEvaluation, binding.clRoot) // Kısa mesaj uzun mesaj mantığının çalışması için ilk önce kısa mesaj ayarlı olması gerekiyor.
            binding.scrollViewEvaluation?.scrollTo(0, 0)
            // cvDeğerlendirmeyi ekran dışına al
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
                if (cardViewBottom > buttonTop - dpToPx(15)) {
                    uzunMesaj(binding.cvEvaluation, binding.clRoot, binding.ibNext)
                } else {
                    kisaMesaj(binding.cvEvaluation, binding.clRoot)
                }
            }
        }
    }

    fun checkTranslationSender(){
        if (mainViewModel.isSubscribed.value.first || (!mainViewModel.isSubscribed.value.first && translationCount > 0)){
            val originalSentence = binding.tvOriginalSentence.text.toString().trim()
            val translationSentence = binding.etTranslation.text.toString().trim()
            if (translationSentence == "----") {
                val queryForNoTranslation = localizedContext.getString(R.string.query_for_no_translation, nativeLanguageInNativeLanguage, originalSentence, viewModel.translationSentenceLanguageInNativeLanguage)
                viewModel.checkTranslationSender(1, queryForNoTranslation, translationSentence, targetLanguage, targetLanguageCode)
            } else if (translationSentence.contains("____")){
                val queryText = getQueryWithBlank(viewModel.reverseTranslation, localizedContext , originalSentence, translationSentence, nativeLanguageInNativeLanguage, viewModel.translationSentenceLanguageInNativeLanguage)
                viewModel.checkTranslationSender(2, queryText, translationSentence, targetLanguage, targetLanguageCode)
            } else {
                val queryText = getQueryNoBlank(viewModel.reverseTranslation, localizedContext, originalSentence, translationSentence, nativeLanguageInNativeLanguage, viewModel.translationSentenceLanguageInNativeLanguage)
                viewModel.checkTranslationSender(3, queryText, translationSentence, targetLanguage, targetLanguageCode)
            }
        }
        else {
            showDialog(R.layout.dialog_no_quota, requireActivity(), false)
            setUiForTranslation()
        }

    }

    fun areViewsClickable (boolean: Boolean){
        binding.ibOriginalSentenceRefresh.isEnabled = boolean
        binding.ibShrug.isEnabled = boolean
        binding.ibReverseTranslation.isEnabled = boolean
        binding.topBar.a1.isEnabled = boolean
        binding.topBar.a2.isEnabled = boolean
        binding.topBar.b1.isEnabled = boolean
        binding.topBar.b2.isEnabled = boolean
        binding.topBar.c1.isEnabled = boolean
        binding.tvHint.isClickable = boolean
        binding.ibSubmit.isClickable = boolean
    }

    fun setViewsForError(){
        val boolean: Boolean
        if (viewModel.randomSentence.value == getString(R.string.creating_sentence)){
            viewModel.setBlinkingSentence("_ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _")
            boolean = false
        } else {
            boolean = true
        }
        // Cümle mevcut ise çevirisini tekrar gönderebilmesi için butonlar aktif kalıyor.
        setUiForTranslation()
        binding.ibOriginalSentenceRefresh.isEnabled = true
        binding.topBar.a1.isEnabled = true
        binding.topBar.a2.isEnabled = true
        binding.topBar.b1.isEnabled = true
        binding.topBar.b2.isEnabled = true
        binding.topBar.c1.isEnabled = true
        binding.tvHint.isClickable = boolean
        binding.ibSubmit.isClickable = boolean
        binding.ibShrug.isClickable = boolean
        binding.ibReverseTranslation.isClickable = boolean
    }

    fun setUiForTranslation(){
        binding.etTranslation.text?.clear()
        binding.etTranslation.isEnabled = true
        binding.etTranslation.visibility = View.VISIBLE
        binding.tvTranslation.visibility = View.GONE
        binding.ibShrugClose.visibility = View.GONE
        binding.ibShrug.visibility = View.VISIBLE
        binding.ibEdit.visibility = View.GONE
        binding.ibEditDone.visibility = View.GONE
        binding.tvHint.visibility = View.VISIBLE
        binding.progressBar.visibility = View.GONE
        binding.ibSubmit.visibility = View.VISIBLE
        binding.cvEvaluation.visibility = View.GONE
        hidePronunciation()
        binding.ibNext.visibility = View.GONE
        binding.topBar.horizontalBar.visibility = View.VISIBLE
        binding.topBar.a1.visibility = View.VISIBLE
        binding.topBar.a2.visibility = View.VISIBLE
        binding.topBar.b1.visibility = View.VISIBLE
        binding.topBar.b2.visibility = View.VISIBLE
        binding.topBar.c1.visibility = View.VISIBLE
        binding.cvTranslation.visibility = View.VISIBLE
        binding.cvOriginalSentence.visibility = View.VISIBLE
        binding.cvUnknown.root.visibility = View.GONE
        binding.cvTranslationDirection.visibility = View.GONE
    }

    private fun makeToast(s: String){
        binding.tvToast.setText(s)
        stopToastAnimation(toastAnimator)
        toastAnimator = startToastAnimation(binding.cvToast)
    }

    fun hidePronunciation(){
        binding.cvPronunciationShort.visibility = View.GONE
        binding.cvPronunciationLong.visibility = View.GONE
        binding.cvTypoPronunciationShort.visibility = View.GONE
        binding.cvTypoPronunciationLong.visibility = View.GONE
    }

    fun showPronunciation(word: String){
        // görünümleri en aşağı veya yukarı taşı
        binding.cvPronunciationShort.visibility = View.VISIBLE
        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        binding.cvPronunciationShort.translationY = screenHeight.toFloat()

        // yazıları ayarla
        binding.tvPronunciation1.setText(word)
        binding.tvPronunciation2.setText(word)
        binding.hsvPronunciation.scrollTo(0, 0)

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