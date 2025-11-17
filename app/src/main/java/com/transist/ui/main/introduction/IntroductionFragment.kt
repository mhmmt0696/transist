package com.transist.ui.main.introduction

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.transist.R
import com.transist.data.repository.StudyRepository
import com.transist.databinding.WelcomeActivityPage1Binding
import com.transist.ui.main.list.ListViewModel
import com.transist.ui.main.list.ListViewModelFactory
import com.transist.util.getLocalizedContext
import com.transist.util.getLocalizedString
import com.transist.util.getStringId
import com.transist.util.initTtsWithTargetLanguageSupport
import com.transist.util.pronunciationClick
import com.transist.util.showDialogNoVoiceEngine
import java.util.Locale

class IntroductionFragment: Fragment() {

    private var _binding: WelcomeActivityPage1Binding? = null
    private val binding get() = _binding!!  // Null güvenliği için

    private lateinit var tts: TextToSpeech
    private var isTTsExist = false

    companion object {

        private const val ARG_NATIVE = "native"
        private const val ARG_TARGET = "target"

        fun newInstance(nativeLanguageCode: String, targetLanguageCode: String): IntroductionFragment {
            val fragment = IntroductionFragment()
            val bundle = Bundle()
            bundle.putString(ARG_NATIVE, nativeLanguageCode)
            bundle.putString(ARG_TARGET, targetLanguageCode)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = WelcomeActivityPage1Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null  // Hafıza sızıntısını önlemek için
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nativeLanguageCode = arguments?.getString(ARG_NATIVE) ?: "tr"
        val targetLanguageCode = arguments?.getString(ARG_TARGET) ?: "en"

        // Welcome1 sayfasındaki örnek cümle işlemleri
        val exampleSentenceInNativeLanguage = this.getString(R.string.example_sentence)
        var exampleSentenceInTargetLanguage = ""
        var exampleSentenceInTargetLanguageWithBlank = ""
        var targetLanguage = ""
        if (nativeLanguageCode == "en"){
            targetLanguage = "Spanish"
            exampleSentenceInTargetLanguage = getLocalizedString(requireContext(), Locale("es"), R.string.example_sentence)
            exampleSentenceInTargetLanguageWithBlank = getLocalizedString(requireContext(), Locale("es"), R.string.example_sentence_blank)
        } else {
            targetLanguage = "English"
            exampleSentenceInTargetLanguage = getLocalizedString(requireContext(), Locale("en"), R.string.example_sentence)
            exampleSentenceInTargetLanguageWithBlank = getLocalizedString(requireContext(), Locale("en"), R.string.example_sentence_blank)
        }

        val targetLanguageStringId = getStringId(targetLanguage)
        val localizedContext = getLocalizedContext(requireContext(), nativeLanguageCode)
        val targetLanguageInNativeLanguage = localizedContext.getString(targetLanguageStringId)

        binding.tvEvaluation.text =
            this.getString(R.string.example_evaluation_sentence, targetLanguageInNativeLanguage, exampleSentenceInNativeLanguage, exampleSentenceInTargetLanguage)

        binding.et2.setText(exampleSentenceInTargetLanguage)
        binding.et4.setText(exampleSentenceInTargetLanguageWithBlank)

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

        val targetLanguageContext = getLocalizedContext(requireContext(), targetLanguageCode)
        binding.tvPronunciation.text = targetLanguageContext.getString(R.string.example_word)

        binding.ibPronunciation.setOnClickListener{
            if (isTTsExist) {
                pronunciationClick(binding.tvPronunciation.text.toString(), tts)
            } else {
                showDialogNoVoiceEngine(requireContext())
            }
        }

        // Transition from Welcome 1 page to Welcome 2 page.
        binding.ibInfoNext.setOnClickListener {
            parentFragmentManager.popBackStack()
        }





    }





}