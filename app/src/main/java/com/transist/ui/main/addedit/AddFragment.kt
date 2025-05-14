package com.transist.ui.main.addedit

import android.animation.Animator
import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ListView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.transist.R
import com.transist.data.remote.response.Sentence
import com.transist.data.repository.ApiRepository
import com.transist.data.repository.LanguageRepository
import com.transist.data.repository.StudyRepository
import com.transist.databinding.FragmentAddBinding
import com.transist.util.showDialog
import com.transist.util.startToastAnimation
import com.transist.util.stopToastAnimation
import kotlinx.coroutines.launch

class AddFragment: Fragment() {

    private lateinit var viewModel: AddEditViewModel

    private var _binding: FragmentAddBinding? = null
    private val binding get() = _binding!!  // Null güvenliği için
    private var directionAnimator: Animator? = null
    var nativeLanguageCode = "un"
    var targetLanguageCode = "un"
    var targetLanguage = "Unknown"
    var nativeLanguage = "Unknown"

    private lateinit var sentencesAdapter: SentencesAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null  // Hafıza sızıntısını önlemek için
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val factory = AddEditViewModelFactory(
            LanguageRepository(requireContext()), StudyRepository(requireContext()), ApiRepository()
        )
        viewModel = ViewModelProvider(this, factory).get(AddEditViewModel::class.java)


        val userLangsCodes = viewModel.userLanguagesCodes.value ?: Pair("un", "un")
        nativeLanguageCode = userLangsCodes.first
        targetLanguageCode = userLangsCodes.second
        val userLangs = viewModel.userLanguages.value ?: Pair("Unknown", "Unknown")
        nativeLanguage = userLangs.first
        targetLanguage = userLangs.second

        setupUI()
        observeViewModel()
    }

    private fun setupUI(){
        binding.ibClear1.visibility = View.GONE
        binding.ibClear2.visibility = View.GONE
        binding.ibClear3.visibility = View.GONE
        binding.cvToast.visibility = View.GONE
        binding.rvSentences.layoutManager = LinearLayoutManager(requireContext())
        viewModel.loadActiveFolder(getString(R.string.default_folder))

        binding.ibClear1.setOnClickListener {
            binding.etExpression.text.clear()
        }

        binding.ibClear2.setOnClickListener {
            binding.etMeaning.text.clear()
        }

        binding.ibClear3.setOnClickListener {
            binding.etNote.text.clear()
        }

        binding.ibSearchHint.setOnClickListener {
            showDialog(R.layout.dialog_search_hint, requireActivity(), false)
        }

        binding.etExpression.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.ibClear1.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.etMeaning.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.ibClear2.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.etNote.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.ibClear3.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.ibChangeDeck.setOnClickListener {
            val dialog = showDialog(R.layout.dialog_list_view, requireActivity(), true)

            val ivEmptyList = dialog.first.findViewById<View>(R.id.iv_empty_list)
            val lvFolders = dialog.first.findViewById<ListView>(R.id.lv_folders)
            val folderList = viewModel.loadFolders()
            val foldersAdapter = FoldersAdapterSelectOne(requireContext(), folderList)
            lvFolders.adapter = foldersAdapter

            if (folderList.isEmpty()) {
                ivEmptyList.visibility = View.VISIBLE
            } else {
                ivEmptyList.visibility = View.GONE
            }

            lvFolders.setOnItemClickListener { _, _, position, _ ->
                val selectedFolder = folderList[position]
                viewModel.selectFolder(selectedFolder.id)
                dialog.second.cancel()
            }
        }

        binding.buttonSave.setOnClickListener {
            val selectedSentences = sentencesAdapter.getSelectedSentences()

            if (selectedSentences.isEmpty()) {
                makeToast(requireContext().getString(R.string.warn_min_sentence_number, 1.toString()))
                return@setOnClickListener
            }

            val expression = binding.etExpression.text.toString().trim()
            val meaning = binding.etMeaning.text.toString().trim()
            val note = binding.etNote.text.toString().trim()

            // ViewModel üzerinden kaydet
            // Hiç klasör yoksa bir "Varsayılan" klasörü oluştur
            if (viewModel.activeFolderId == -1) {
                val newFolderId = viewModel.createFolder(getString(R.string.default_folder))
                viewModel.selectFolder(newFolderId)
            }
            viewModel.saveSentences(expression, meaning, note, selectedSentences, viewModel.activeFolderId)

            // UI güncellemeleri
            val result = Bundle().apply {
                putInt("gonderilenVeri", viewModel.activeFolderId)  // istediğin veri türü olabilir
            }
            parentFragmentManager.setFragmentResult("veri_guncelleme", result)
            parentFragmentManager.popBackStack()
        }

        binding.btnSearch.setOnClickListener {
            hideKeyboard()
            val expression = binding.etExpression.text.toString().trim()
            val meaning = binding.etMeaning.text.toString().trim()
            val not = binding.etNote.text.toString().trim()

            if (expression.isEmpty()) {
                makeToast(getString(R.string.warn_expression_empty))
                return@setOnClickListener
            }

            viewModel.resetChecks()
            viewModel.search(expression, meaning, not, targetLanguage, nativeLanguage)
        }

    }

    private fun observeViewModel(){
        viewModel.activeFolderName.observe(viewLifecycleOwner) { folderName ->
            binding.txtSelectedDeck.text = folderName
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loading.collect { isLoading ->
                    binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                    binding.loadingText.visibility = if (isLoading) View.VISIBLE else View.GONE
                    binding.cvCumleler.visibility = if (isLoading) View.GONE else View.VISIBLE
                    binding.cvKaydet.visibility = if (isLoading) View.GONE else View.VISIBLE
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.sentences.collect { sentences ->
                    // Gelen cümleleri UI’da göster
                    if (sentences != null){
                        if (sentences.isEmpty()){
                            showDialog(R.layout.dialog_add_info, requireActivity(), false)
                            binding.cvCumleler.visibility = View.GONE
                            binding.cvKaydet.visibility = View.GONE
                        } else {
                            displaySentences(sentences)
                        }
                    } else {
                        binding.cvCumleler.visibility = View.GONE
                        binding.cvKaydet.visibility = View.GONE
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.dialogState.collect { state ->
                    if (state.allDone) {
                        showDialogIfNecessary(
                            state.isValidLanguage,
                            state.isValidSpelling,
                            state.isValidMeaning,
                            state.isValidCommonness
                        )
                    }
                }
            }
        }
    }

    fun hideKeyboard() {
        val inputMethodManager = requireActivity().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = requireActivity().currentFocus
        if (view != null) {
            inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun displaySentences(sentences: MutableList<Sentence>) {
        sentencesAdapter = SentencesAdapter(sentences, binding.buttonSave, requireActivity())
        binding.rvSentences.adapter = sentencesAdapter
        binding.progressBar.visibility = View.GONE
        binding.loadingText.visibility = View.GONE
        binding.cvCumleler.visibility = View.VISIBLE
        binding.cvKaydet.visibility = View.VISIBLE
    }

    private fun makeToast(s: String){
        binding.tvToast.setText(s)
        stopToastAnimation(directionAnimator)
        directionAnimator = startToastAnimation(binding.cvToast)
    }

    private fun showDialogIfNecessary(lang: Boolean?, spell: Boolean?, mean: Boolean?, common: Boolean?){
        if (lang != true || spell!= true || mean!= true || common!= true) {
            val dialog = showDialog(R.layout.dialog_search_issues, requireActivity(), false)

            val viewSearchIssue1 = dialog.first.findViewById<View>(R.id.view_search_issue_1)
            val txtSearchIssue1 = dialog.first.findViewById<View>(R.id.txt_search_issue_1)
            val viewSearchIssue2 = dialog.first.findViewById<View>(R.id.view_search_issue_2)
            val txtSearchIssue2 = dialog.first.findViewById<View>(R.id.txt_search_issue_2)
            val viewSearchIssue3 = dialog.first.findViewById<View>(R.id.view_search_issue_3)
            val txtSearchIssue3 = dialog.first.findViewById<View>(R.id.txt_search_issue_3)
            val viewSearchIssue4 = dialog.first.findViewById<View>(R.id.view_search_issue_4)
            val txtSearchIssue4 = dialog.first.findViewById<View>(R.id.txt_search_issue_4)

            if (lang == false){
                viewSearchIssue1.visibility = View.VISIBLE
                txtSearchIssue1.visibility = View.VISIBLE
            } else {
                viewSearchIssue1.visibility = View.GONE
                txtSearchIssue1.visibility = View.GONE
            }

            if (spell == false){
                viewSearchIssue2.visibility = View.VISIBLE
                txtSearchIssue2.visibility = View.VISIBLE
            } else {
                viewSearchIssue2.visibility = View.GONE
                txtSearchIssue2.visibility = View.GONE

            }

            if (mean == false){
                viewSearchIssue3.visibility = View.VISIBLE
                txtSearchIssue3.visibility = View.VISIBLE
            } else {
                viewSearchIssue3.visibility = View.GONE
                txtSearchIssue3.visibility = View.GONE
            }

            if (common == false){
                viewSearchIssue4.visibility = View.VISIBLE
                txtSearchIssue4.visibility = View.VISIBLE
            } else {
                viewSearchIssue4.visibility = View.GONE
                txtSearchIssue4.visibility = View.GONE
            }
        }


    }

}