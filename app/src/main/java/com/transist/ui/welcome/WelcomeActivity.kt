package com.transist.ui.welcome

import android.animation.Animator
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.transist.ui.main.ActivityMain
import com.transist.R
import com.transist.data.repository.LanguageRepository
import com.transist.data.repository.PreferencesRepository
import com.transist.databinding.WelcomeActivityBinding
import com.transist.util.getLocalizedContext
import com.transist.util.getLocalizedString
import com.transist.util.getStringId
import com.transist.util.initTtsWithTargetLanguageSupport
import com.transist.util.pronunciationClick
import com.transist.util.showDialogNoVoiceEngine
import com.transist.util.startToastAnimation
import com.transist.util.stopToastAnimation
import java.util.Locale

class WelcomeActivity : AppCompatActivity() {

    private lateinit var viewModel: WelcomeViewModel
    private var _binding: WelcomeActivityBinding? = null
    private val binding get() = _binding!!  // For null safety
    private var toastAnimator: Animator? = null
    private lateinit var tts: TextToSpeech
    private var isTTsExist = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = WelcomeActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Adım: Sistem padding’lerini devre dışı bırakıyorum.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.main_activity)

        val rootView = findViewById<ConstraintLayout>(R.id.main_root)
        // 2. Adım: Top bar ve bottom bar'a göre yeniden padding veriyorum.
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val languageRepo = LanguageRepository(applicationContext)
        val prefRepo = PreferencesRepository(applicationContext)
        viewModel = WelcomeViewModel(languageRepo, prefRepo)

        binding.welcome1.clWelcome1.visibility = View.VISIBLE
        binding.welcome2.clWelcome2.visibility = View.GONE
        binding.cvToast.visibility = View.GONE
        binding.welcome2.progressBar.visibility = View.GONE

        // Read all languages for language selection on the Welcome 2 page.
        val languageList = viewModel.getListOfLanguages()
        val languageListInNative = viewModel.getLanguageListInNative(languageList)
        val adapterSpinner = ArrayAdapter(this, R.layout.spinner_item_layout, languageListInNative)
        adapterSpinner.setDropDownViewResource(R.layout.spinner_item_layout)
        binding.welcome2.spinnerLsNative.adapter = adapterSpinner
        binding.welcome2.spinnerLsTarget.adapter = adapterSpinner

        // Set the default languages to spinners on the Welcome 2 page.
        val anaDilIndex = languageList.indexOfFirst { it.code == viewModel.nativeLanguageCode }
        binding.welcome2.spinnerLsNative.setSelection(anaDilIndex)
        val hedefDilIndex = languageList.indexOfFirst { it.code == viewModel.targetLanguageCode }
        binding.welcome2.spinnerLsTarget.setSelection(hedefDilIndex)

        // Get the index of "Unknown" in the list.
        val unKnownIndex = languageList.indexOfFirst { it.code == "un" }

        // Welcome1 sayfasındaki örnek cümle işlemleri
        val exampleSentenceInNativeLanguage = this.getString(R.string.example_sentence)
        var exampleSentenceInTargetLanguage = ""
        var exampleSentenceInTargetLanguageWithBlank = ""
        var targetLanguage = ""
        if (viewModel.nativeLanguageCode == "en"){
            targetLanguage = "Spanish"
            exampleSentenceInTargetLanguage = getLocalizedString(this, Locale("es"), R.string.example_sentence)
            exampleSentenceInTargetLanguageWithBlank = getLocalizedString(this, Locale("es"), R.string.example_sentence_blank)
        } else {
            targetLanguage = "English"
            exampleSentenceInTargetLanguage = getLocalizedString(this, Locale("en"), R.string.example_sentence)
            exampleSentenceInTargetLanguageWithBlank = getLocalizedString(this, Locale("en"), R.string.example_sentence_blank)
        }

        val targetLanguageStringId = getStringId(targetLanguage)
        val localizedContext = getLocalizedContext(this, viewModel.nativeLanguageCode)
        val targetLanguageInNativeLanguage = localizedContext.getString(targetLanguageStringId)

        binding.welcome1.tvEvaluation.text =
            this.getString(R.string.example_evaluation_sentence, targetLanguageInNativeLanguage, exampleSentenceInNativeLanguage, exampleSentenceInTargetLanguage)

        binding.welcome1.et2.setText(exampleSentenceInTargetLanguage)
        binding.welcome1.et4.setText(exampleSentenceInTargetLanguageWithBlank)

        // Önce tüm yüklü ses motorlarını bulup, sonra herhangi birinde dil dosyası yüklü mü diye bakıyoruz.
        initTtsWithTargetLanguageSupport(this, viewModel.targetLanguageCode) { tts1 ->
            if (tts1 != null) {
                isTTsExist = true
                tts = tts1
            } else {
                // ❌ Hiçbir motor Türkçe desteklemiyor, kullanıcı ayarlara yönlendirilecek
                isTTsExist = false
            }
        }

        val targetLanguageContext = getLocalizedContext(this, viewModel.targetLanguageCode)
        binding.welcome1.tvPronunciation.text = targetLanguageContext.getString(R.string.example_word)

        binding.welcome1.ibPronunciation.setOnClickListener{
            if (isTTsExist) {
                pronunciationClick(binding.welcome1.tvPronunciation.text.toString(), tts)
            } else {
                showDialogNoVoiceEngine(this)
            }
        }

        // Transition from Welcome 1 page to Welcome 2 page.
        binding.welcome1.ibInfoNext.setOnClickListener {
            binding.welcome1.clWelcome1.visibility = View.GONE
            binding.welcome2.clWelcome2.visibility = View.VISIBLE
        }

        // Returning from Welcome 2 page to Welcome 1 page.
        binding.welcome2.ibLsBack.setOnClickListener {
            binding.welcome1.clWelcome1.visibility = View.VISIBLE
            binding.welcome2.clWelcome2.visibility = View.GONE
        }

        // Welcome2 sayfasındaki dil ayarlarını kaydet ve ActivityMain'e geçiş işlemleri
        binding.welcome2.ibLsDone.setOnClickListener {
            if (viewModel.nativeLanguageCode == "un" || viewModel.targetLanguageCode == "un"){
                makeToast(this.getString(R.string.warn_select_language))
            } else {
                binding.welcome2.ibLsDone.visibility = View.GONE
                binding.welcome2.progressBar.visibility = View.VISIBLE
                viewModel.saveLanguages()
                viewModel.setFirstLaunchDone()
                startActivity(Intent(this, ActivityMain::class.java))
                finish()
            }
        }

        // Changing the main language on the Welcome-2 page.
        binding.welcome2.spinnerLsNative.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedItem = parent?.getItemAtPosition(position) as String
                val selectedCode = languageList[position].code
                Log.d ("WelcomeActivityyy", "selectedCode: $selectedCode")
                val selectedTextView = view as? TextView

                val targetLanguage = binding.welcome2.spinnerLsTarget.selectedItem.toString()
                if ((selectedItem == targetLanguage)){
                    viewModel.targetLanguageCode = "un"
                    binding.welcome2.spinnerLsTarget.setSelection(unKnownIndex)
                }

                viewModel.nativeLanguageCode = languageList.find { it.code == selectedCode }?.code ?: "un"

                if (selectedItem == getString(R.string.unknown)) {
                    selectedTextView?.setTextColor(ContextCompat.getColor(this@WelcomeActivity,
                        R.color.red
                    ))
                } else {
                    selectedTextView?.setTextColor(ContextCompat.getColor(this@WelcomeActivity,
                        R.color.textColorPrimary
                    )) // Diğer seçenekler için varsayılan renk
                }

            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Changing the target language on the Welcome-2 page.
        binding.welcome2.spinnerLsTarget.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedItem = parent?.getItemAtPosition(position) as String
                val selectedCode = languageList[position].code
                val selectedTextView = view as? TextView

                val nativeLanguage = binding.welcome2.spinnerLsNative.selectedItem.toString()

                if (selectedItem == nativeLanguage){
                    viewModel.nativeLanguageCode = "un"
                    binding.welcome2.spinnerLsNative.setSelection(unKnownIndex)
                }

                viewModel.targetLanguageCode = languageList.find { it.code == selectedCode }?.code ?: "un"

                if (selectedItem == getString(R.string.unknown)) {
                    selectedTextView?.setTextColor(ContextCompat.getColor(this@WelcomeActivity,
                        R.color.red
                    ))
                } else {
                    selectedTextView?.setTextColor(ContextCompat.getColor(this@WelcomeActivity,
                        R.color.textColorPrimary
                    )) // Diğer seçenekler için varsayılan renk
                }

            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        // TTS kaynaklarını serbest bırak
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        _binding = null  // To prevent memory leaks
    }

    // Start the animations needed for the Toast message
    private fun makeToast(s: String){
        binding.tvToast.setText(s)
        stopToastAnimation(toastAnimator)
        toastAnimator = startToastAnimation(binding.cvToast)
    }
}