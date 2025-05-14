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
import com.transist.databinding.FragmentEditBinding
import com.transist.util.capitalizeFirstLetter
import com.transist.util.convertToSentenceList
import com.transist.util.showDialog
import com.transist.util.startToastAnimation
import com.transist.util.stopToastAnimation
import kotlinx.coroutines.launch

class EditFragment: Fragment() {

    // List fragment'dan EditFragment'e geçişte id bilgisini alıyoruz.
    companion object {
        fun newInstance(expressionId: Int): EditFragment {
            val fragment = EditFragment()
            val args = Bundle()
            args.putInt("expressionId", expressionId)
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var viewModel: AddEditViewModel
    private var _binding: FragmentEditBinding? = null
    private val binding get() = _binding!!  // Null güvenliği için

    private lateinit var targetLanguageCode: String
    private lateinit var targetLanguage: String
    private lateinit var nativeLanguageCode: String
    private lateinit var nativeLanguage: String
    private var directionAnimator: Animator? = null
    private lateinit var sentencesAdapter: SentencesAdapter
    private var selectedFolderId = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEditBinding.inflate(inflater, container, false)
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
        binding.btnTemizle1.visibility = View.GONE
        binding.btnTemizle2.visibility = View.GONE
        binding.btnTemizle3.visibility = View.GONE
        binding.cvToast.visibility = View.GONE
        binding.recyclerViewSentences.layoutManager = LinearLayoutManager(activity)

        binding.btnTemizle1.setOnClickListener {
            binding.editTextIfade.text.clear()
        }

        binding.btnTemizle2.setOnClickListener {
            binding.editTextAnlam.text.clear()
        }

        binding.btnTemizle3.setOnClickListener {
            binding.editTextNot.text.clear()
        }

        binding.editTextIfade.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.btnTemizle1.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.editTextAnlam.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.btnTemizle2.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.editTextNot.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.btnTemizle3.visibility = if (s.isNullOrEmpty()) View.GONE else View.VISIBLE
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        val id = arguments?.getInt("expressionId") ?: 1
        val item = viewModel.getExpressionByIdNew(id)

        if (item.expression != ""){
            binding.btnTemizle1.visibility = View.VISIBLE
        }
        if (item.meaning != ""){
            binding.btnTemizle2.visibility = View.VISIBLE
        }
        if (item.note != ""){
            binding.btnTemizle3.visibility = View.VISIBLE
        }
        binding.editTextIfade.setText(item.expression)
        binding.editTextAnlam.setText(item.meaning)
        binding.editTextNot.setText(item.note)

        displaySentences(convertToSentenceList(item.sentences))

        selectedFolderId = item.folderId
        binding.txtSelectedDeck.text = viewModel.getFolderNameById(selectedFolderId).capitalizeFirstLetter()

        binding.buttonCancel.setOnClickListener {
            parentFragmentManager.popBackStack()  // En üstteki fragment'ı kapatır, bir önceki görünür
        }

        binding.buttonYenile.setOnClickListener {
            hideKeyboard()
            val expression = binding.editTextIfade.text.toString().trim()
            val meaning = binding.editTextAnlam.text.toString().trim()
            val not = binding.editTextNot.text.toString().trim()

            if (expression.isEmpty()) {
                makeToast(getString(R.string.warn_expression_empty))
                return@setOnClickListener
            }

            viewModel.resetChecks()
            viewModel.search(expression, meaning, not, targetLanguage, nativeLanguage)
        }

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
                binding.txtSelectedDeck.text = selectedFolder.name
                dialog.second.cancel()
            }
        }

        binding.buttonUpdate.setOnClickListener {
            val selectedSentences = sentencesAdapter.getSelectedSentences()

            if (selectedSentences.isEmpty()) {
                makeToast(requireContext().getString(R.string.warn_min_sentence_number, 1.toString()))
                return@setOnClickListener
            }

            val expression = binding.editTextIfade.text.toString().trim()
            val meaning = binding.editTextAnlam.text.toString().trim()
            val note = binding.editTextNot.text.toString().trim()

            val i = viewModel.updateSelectedSentences(id, selectedFolderId, expression, meaning, note, selectedSentences)
            if (i == 0){
                val result = Bundle().apply {
                    putInt("gonderilenVeri", selectedFolderId)  // istediğin veri türü olabilir
                }
                parentFragmentManager.setFragmentResult("veri_guncelleme", result)
                parentFragmentManager.popBackStack()
            } else if (i == 1){
                makeToast(requireContext().getString(R.string.warn_min_sentence_number, 1.toString()))
            } else if (i == 2){
                makeToast(requireContext().getString(R.string.error_occured))
            }
        }

        viewModel.initSentences(sentencesAdapter.getSelectedSentences())

    }

    private fun observeViewModel(){

        viewModel.activeFolderName.observe(viewLifecycleOwner) { folderName ->
            if (folderName == null) {
                binding.txtSelectedDeck.text = getString(R.string.default_folder)
            } else {
                binding.txtSelectedDeck.text = folderName
            }
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

    private fun displaySentences(sentences: MutableList<Sentence>) {
        sentencesAdapter = SentencesAdapter(sentences, binding.buttonUpdate, requireActivity())
        binding.recyclerViewSentences.adapter = sentencesAdapter
        binding.recyclerViewSentences.visibility = View.VISIBLE
    }

    fun hideKeyboard() {
        val inputMethodManager = requireActivity().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = requireActivity().currentFocus
        if (view != null) {
            inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
        }
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
