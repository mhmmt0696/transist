package com.transist.ui.main.list

import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.EditText
import android.widget.ImageView
import android.widget.ListView
import android.widget.SearchView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.transist.data.model.ExpressionData
import com.transist.data.model.FolderData
import com.transist.R
import com.transist.data.repository.StudyRepository
import com.transist.databinding.FragmentListBinding
import com.transist.ui.main.MainViewModel
import com.transist.ui.main.addedit.AddFragment
import com.transist.ui.main.addedit.EditFragment
import com.transist.ui.main.study.folder.StudyFolderFragment
import com.transist.util.animateTextChange
import com.transist.util.capitalizeFirstLetter
import com.transist.util.dpToPx
import com.transist.util.flipViews
import com.transist.util.scaleIn
import com.transist.util.scaleOut
import com.transist.util.showDialog
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class ListFragment: Fragment() {
    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!  // Null güvenliği için

    private lateinit var viewModel: ListViewModel
    private lateinit var expressionsAdapter: ExpressionsAdapter
    private lateinit var foldersAdapter: FoldersAdapter
    private lateinit var expressionsInFolderAdapter: ExpressionsAdapter

    var nativeLanguageCode = "--"
    var targetLanguageCode = "--"
    private val mainViewModel: MainViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null  // Hafıza sızıntısını önlemek için
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val factory = ListViewModelFactory(StudyRepository(requireContext()))
        viewModel = ViewModelProvider(this, factory).get(ListViewModel::class.java)

        setupUI()
        observerViewModel()
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
                        if (nativeLanguageCode == "un" || targetLanguageCode == "un"){
                            viewModel.setStatus("un")
                        } else {
                            viewModel.getAllExpressions()
                            viewModel.getAllFolders()
                            viewModel.getExpressionsByFolder()
                            viewModel.setStatus("folders")
                            binding.ibGoToStudy.visibility = View.GONE
                        }
                    }

                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.folders.collect { folders ->
                    foldersAdapter.submitList(folders){
                        binding.recyclerViewFolders.smoothScrollToPosition(0)
                    }
                    if (viewModel.status.value == "folders"){
                        if (folders.size == 0){
                            binding.cvEmptyList.visibility = View.VISIBLE
                        } else {
                            binding.cvEmptyList.visibility = View.GONE
                        }
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.expressions.collect { expressions ->
                    expressionsAdapter.submitList(expressions){
                        binding.recyclerViewExpressions.smoothScrollToPosition(0)
                    }
                    if (viewModel.status.value == "expressions"){
                        if (expressions.size == 0){
                            binding.cvEmptyList.visibility = View.VISIBLE
                        } else {
                            binding.cvEmptyList.visibility = View.GONE
                        }
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.expressionsInFolder.collect { expressionsInFolder ->
                    expressionsInFolderAdapter.submitList(expressionsInFolder){
                        binding.recyclerViewExpressionsInFolder.smoothScrollToPosition(0)
                    }
                    if (viewModel.status.value == "expressionsInFolder"){
                        if (expressionsInFolder.size == 0){
                            binding.cvEmptyList.visibility = View.VISIBLE
                            scaleOut(binding.ibGoToStudy)
                        } else {
                            binding.cvEmptyList.visibility = View.GONE
                            scaleIn(binding.ibGoToStudy)
                        }
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.status.collect { status ->
                    if (status == "un") {
                        binding.ibAdd.visibility = View.GONE
                        binding.searchViewInFolder.visibility = View.GONE
                        binding.searchViewFolders.visibility = View.GONE
                        binding.searchViewExpressions.visibility = View.GONE
                        binding.cvEmptyList.visibility = View.GONE
                        binding.ibCreateFolder.visibility = View.GONE
                        binding.ibItemView.visibility = View.GONE

                        binding.recyclingBin.visibility = View.GONE
                        binding.recyclerViewExpressions.visibility = View.GONE
                        binding.recyclerViewFolders.visibility = View.GONE
                        binding.recyclerViewExpressionsInFolder.visibility = View.GONE
                        binding.txtListeBaslik.visibility = View.GONE
                        binding.cvUnknown.cvUnknown.visibility = View.VISIBLE
                    } else {
                        binding.recyclingBin.visibility = View.VISIBLE
                        binding.recyclerViewExpressions.visibility = View.VISIBLE
                        binding.recyclerViewFolders.visibility = View.VISIBLE
                        binding.recyclerViewExpressionsInFolder.visibility = View.VISIBLE
                        binding.txtListeBaslik.visibility = View.VISIBLE
                        binding.cvUnknown.cvUnknown.visibility = View.GONE
                    }

                    if (status == "folders") {
                        showRecyclerView(binding.recyclerViewFolders)
                        showSearchView(binding.searchViewFolders)
                        binding.ibItemView.visibility = View.VISIBLE
                        binding.ibFolderView.visibility = View.GONE
                        animateTextChange(binding.txtListeBaslik, getString(R.string.liste_baslik_folders))
                        flipViews(binding.ibAdd, binding.ibCreateFolder)
                        binding.ibBackToFolders.visibility = View.GONE
                        if (viewModel.folders.value.size == 0) {
                            binding.cvEmptyList.visibility = View.VISIBLE
                        } else {
                            binding.cvEmptyList.visibility = View.GONE
                        }
                    }

                    if (status == "expressions"){
                        animateTextChange(binding.txtListeBaslik, getString(R.string.liste_baslik_items))
                        flipViews(binding.ibCreateFolder, binding.ibAdd)
                        showSearchView(binding.searchViewExpressions)
                        showRecyclerView(binding.recyclerViewExpressions)
                        binding.ibGoToStudy.visibility = View.GONE
                        binding.ibBackToFolders.visibility = View.GONE
                        binding.ibFolderView.visibility = View.VISIBLE
                        binding.ibItemView.visibility = View.GONE
                        if (viewModel.expressions.value.size == 0){
                            binding.cvEmptyList.visibility = View.VISIBLE
                        } else {
                            binding.cvEmptyList.visibility = View.GONE
                        }
                    }

                    if (status == "expressionsInFolder"){
                        showRecyclerView(binding.recyclerViewExpressionsInFolder)
                        showSearchView(binding.searchViewInFolder)
                        binding.searchViewInFolder.setQuery("", false)
                        flipViews(binding.ibCreateFolder, binding.ibAdd)
                        binding.ibBackToFolders.visibility = View.VISIBLE
                        binding.ibFolderView.visibility = View.GONE
                        binding.ibItemView.visibility = View.GONE
                        if (viewModel.expressionsInFolder.value.size == 0) {
                            binding.cvEmptyList.visibility = View.VISIBLE
                            binding.ibGoToStudy.visibility = View.GONE
                        } else {
                            binding.cvEmptyList.visibility = View.GONE
                            binding.ibGoToStudy.visibility = View.VISIBLE
                            scaleIn(binding.ibGoToStudy)
                        }
                    }
                }
            }
        }

    }

    private fun setupUI(){
        binding.cvToast.visibility = View.GONE
        binding.recyclerViewExpressions.layoutManager = LinearLayoutManager(activity)
        binding.recyclerViewExpressionsInFolder.layoutManager = LinearLayoutManager(activity)
        binding.recyclerViewFolders.layoutManager = LinearLayoutManager(activity)
        expressionsAdapter = ExpressionsAdapter(
            requireContext(),
            onEditClick = { expression ->
                itemEditClicked(expression.id)
            },
            onDeleteClick = { expression ->
                itemDeleteClicked(expression)
            }
        )
        binding.recyclerViewExpressions.adapter = expressionsAdapter
        expressionsInFolderAdapter = ExpressionsAdapter(
            requireContext(),
            onEditClick = { expression ->
                itemEditClicked(expression.id)
            },
            onDeleteClick = { expression ->
                itemDeleteClicked(expression)
            }
        )
        binding.recyclerViewExpressionsInFolder.adapter = expressionsInFolderAdapter

        foldersAdapter = FoldersAdapter(
            requireContext(),
            onEditClicked = { folder ->
                folderEditClicked(folder)
            },
            onItemClicked = { clickedFolder ->
                folderItemClicked(clickedFolder)
            },
            // 2. Lambda: onDeleteClicked için
            onDeleteClicked = { folderId ->
                folderDeleteClicked(folderId)
            }
        )
        binding.recyclerViewFolders.adapter = foldersAdapter

        parentFragmentManager.setFragmentResultListener("veri_guncelleme", viewLifecycleOwner) { _, bundle ->

            viewModel.getAllExpressions()

            viewModel.getAllFolders()

            viewModel.getExpressionsByFolder()

            val addEditFolderId = bundle.getInt("gonderilenVeri")

            if (addEditFolderId == viewModel.openFolderId){
                removeStudyFolderFragment()
            }
        }

        binding.ibItemView.setOnClickListener {
            viewModel.setStatus("expressions")
        }

        binding.ibFolderView.setOnClickListener {
            viewModel.setStatus("folders")
        }

        binding.ibBackToFolders.setOnClickListener {
            scaleOut(binding.ibGoToStudy)
            viewModel.lastSelectedFolderId = -1
            viewModel.getExpressionsByFolder()
            viewModel.setStatus("folders")
        }

        binding.recyclingBin.setOnClickListener {
            if (viewModel.status.value == "expressionsInFolder"){
                // folder içindeyiz
                showRecyclerSpecialFolder()
            } else if (viewModel.status.value == "folders"){
                // folder görünümü
                showRecyclerFolders()
            } else {
                // item görünümü
                showRecyclerExpressions()
            }
        }

        binding.ibGoToStudy.setOnClickListener {
            viewModel.updateActiveStudyFolder()
            if (viewModel.openFolderId == -1){
                // Açık klasör yok ise yeni oluştur.
                val studyFolderFragment = StudyFolderFragment()
                parentFragmentManager.beginTransaction()
                    .hide(this@ListFragment)
                    .add(R.id.fragment_container, studyFolderFragment, "studyFolder")
                    .commit()
                viewModel.openFolderId = viewModel.lastSelectedFolderId
            } else {
                // Açık klasör açılmak istenen klasör ile aynı ise
                if (viewModel.openFolderId == viewModel.lastSelectedFolderId){
                    // studyFolder'ı görünür yap.
                    val studyFolderFragment = parentFragmentManager.findFragmentByTag("studyFolder")
                    if (studyFolderFragment != null){
                        parentFragmentManager.beginTransaction()
                            .hide(this@ListFragment)
                            .show(studyFolderFragment)
                            .commit()
                    }
                } else { // Açık klasör açılmak istenen klasörden farklı ise
                    // studyFolder'ı kapat.
                    val studyFolderFragment = parentFragmentManager.findFragmentByTag("studyFolder")
                    if (studyFolderFragment != null) {
                        parentFragmentManager.beginTransaction()
                            .remove(studyFolderFragment)
                            .commitNow()
                    }
                    // Yenisini aç.
                    val newStudyFolderFragment = StudyFolderFragment()
                    parentFragmentManager.beginTransaction()
                        .hide(this@ListFragment)
                        .add(R.id.fragment_container, newStudyFolderFragment, "studyFolder")
                        .commit()
                    viewModel.openFolderId = viewModel.lastSelectedFolderId
                }
            }

        }

        binding.searchViewExpressions.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.equals("")){
                    viewModel.getAllExpressions()
                    expressionsAdapter.submitList(viewModel.expressions.value){
                        binding.recyclerViewExpressions.smoothScrollToPosition(0)
                    }
                } else {
                    val searchResults = viewModel.searchExpressions(newText ?: "")
                    expressionsAdapter.submitList(searchResults)
                }
                return true
            }
        })

        binding.searchViewInFolder.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.equals("")){
                    viewModel.getExpressionsByFolder()
                    expressionsInFolderAdapter.submitList(viewModel.expressionsInFolder.value){
                        binding.recyclerViewExpressionsInFolder.smoothScrollToPosition(0)
                    }
                } else {
                    val searchResults = viewModel.searchInFolders(newText ?: "")
                    expressionsInFolderAdapter.submitList(searchResults)
                }
                return true
            }
        })

        binding.searchViewFolders.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.equals("")){
                    viewModel.getAllFolders()
                    foldersAdapter.submitList(viewModel.folders.value){
                        binding.recyclerViewFolders.smoothScrollToPosition(0)
                    }
                } else {
                    val searchResults = viewModel.searchFolders(newText ?: "")
                    foldersAdapter.submitList(searchResults)
                }
                return true
            }
        })

        binding.ibCreateFolder.setOnClickListener {
            // Dialog için bir LayoutInflater oluştur
            val dialog = showDialog(R.layout.dialog_create_folder, requireActivity(), false)

            // Dialog içindeki öğelere erişim
            val etCreateFolder = dialog.first.findViewById<EditText>(R.id.et_create_folder_name)
            val btnOk = dialog.first.findViewById<TextView>(R.id.tv_ok)
            val btnCancel = dialog.first.findViewById<TextView>(R.id.tv_cancel)

            btnOk.setOnClickListener {
                val text = etCreateFolder.text.toString()
                if (text.isNotEmpty()) {
                    viewModel.createFolder(text)
                    viewModel.getAllFolders()

                    // Listeyi güncelle
                    foldersAdapter.submitList(viewModel.folders.value)  {
                        // BU BLOK, LİSTE GÜNCELLENDİKTEN SONRA ÇALIŞIR.
                        // RecyclerView'ı listenin en başına (yeni eklenen öğeye) yumuşak bir şekilde kaydır.
                        binding.recyclerViewFolders.smoothScrollToPosition(0)
                    }
                    dialog.second.cancel()
                }
            }

            btnCancel.setOnClickListener{
                dialog.second.cancel()
            }

        }

        binding.ibAdd.setOnClickListener {
            if (viewModel.status.value == "expressionsInFolder"){
                viewModel.updateActiveAddFolder()
            }
            val addFragment = AddFragment()
            parentFragmentManager.beginTransaction()
                .hide(this@ListFragment)
                .add(R.id.fragment_container, addFragment, "add")
                .addToBackStack(null) // Makes it reversible
                .commit()
        }
    }

    private val keyboardListener = ViewTreeObserver.OnGlobalLayoutListener {
        val rect = Rect()
        binding.root.getWindowVisibleDisplayFrame(rect)
        val screenHeight = binding.root.height
        val keyboardHeight = screenHeight - rect.height()
        val isKeyboardVisible = keyboardHeight > screenHeight * 0.15
        Log.d("keyboardListener-listele", "listele")
        val layoutParams = binding.recyclerViewExpressions.layoutParams as ViewGroup.MarginLayoutParams

        if (isKeyboardVisible && layoutParams.bottomMargin != keyboardHeight + dpToPx(10).roundToInt()) {
            Log.d("keyboardListener-listele", "klavye açıldı: $keyboardHeight")

            layoutParams.bottomMargin = keyboardHeight + dpToPx(10).roundToInt()
            binding.recyclerViewExpressions.layoutParams = layoutParams
        }
        if (!isKeyboardVisible && layoutParams.bottomMargin != dpToPx(52).roundToInt()){
            Log.d("keyboardListener-listele", "klavye kapandı: $keyboardHeight")

            layoutParams.bottomMargin = dpToPx(52).roundToInt()
            binding.recyclerViewExpressions.layoutParams = layoutParams
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        _binding?.let { binding ->
            if (hidden) {
                // Fragment gizlendiğinde
                binding.root.viewTreeObserver.removeOnGlobalLayoutListener(keyboardListener)
            } else {
                // Fragment görünür olduğunda
                binding.root.viewTreeObserver.addOnGlobalLayoutListener(keyboardListener)
            }
        }
    }

    fun showRecyclerFolders(){
        val dialog = showDialog(R.layout.dialog_list_view, requireActivity(), true)

        val listView = dialog.first.findViewById<ListView>(R.id.lv_folders)
        val emptyList = dialog.first.findViewById<ImageView>(R.id.iv_empty_list)
        val deletedFolders = viewModel.getAllDeletedFolders()
        val recyclerAdapter = FolderRecyclerAdapter(requireContext(), deletedFolders)
        listView.adapter = recyclerAdapter

        if (deletedFolders.size == 0){
            emptyList.visibility = View.VISIBLE
        } else {
            emptyList.visibility = View.GONE
        }

        listView.setOnItemClickListener { parent, view, position, id ->
            val selectedFolder = deletedFolders[position]
            viewModel.restoreFolderAndItsContents(selectedFolder.id)
            dialog.second.cancel()
            viewModel.getAllFolders()
            viewModel.getAllExpressions()
            viewModel.getExpressionsByFolder()
        }
    }

    fun showRecyclerExpressions(){
        val dialog = showDialog(R.layout.dialog_recycler_bin, requireActivity(), true)

        val listView = dialog.first.findViewById<ListView>(R.id.lv_folders)
        val emptyList = dialog.first.findViewById<ImageView>(R.id.iv_empty_list)
        val deletedExpressions = viewModel.getAllDeletedExpressions()
        val recyclerAdapter = ExpressionRecyclerAdapter(requireContext(), deletedExpressions)
        listView.adapter = recyclerAdapter

        if (deletedExpressions.size == 0){
            emptyList.visibility = View.VISIBLE
        } else {
            emptyList.visibility = View.GONE
        }

        listView.setOnItemClickListener { parent, view, position, id ->
            val selectedExpression = deletedExpressions[position]
            viewModel.restoreExpression(selectedExpression.id)
            dialog.second.cancel()
            viewModel.getAllExpressions()
            viewModel.getAllFolders()
            viewModel.getExpressionsByFolder()
        }
    }

    fun showRecyclerSpecialFolder(){
        val dialog = showDialog(R.layout.dialog_list_view, requireActivity(), true)

        val listView = dialog.first.findViewById<ListView>(R.id.lv_folders)
        val emptyList = dialog.first.findViewById<ImageView>(R.id.iv_empty_list)
        val deletedExpressions = viewModel.getDeletedExpressionsByFolder(viewModel.lastSelectedFolderId)
        val recyclerAdapter = ExpressionRecyclerAdapter(requireContext(), deletedExpressions)
        listView.adapter = recyclerAdapter

        if (deletedExpressions.size == 0){
            emptyList.visibility = View.VISIBLE
        } else {
            emptyList.visibility = View.GONE
        }

        listView.setOnItemClickListener { parent, view, position, id ->
            val selectedExpression = deletedExpressions[position]
            viewModel.restoreExpression(selectedExpression.id)
            dialog.second.cancel()
            viewModel.getExpressionsByFolder()
            viewModel.getAllExpressions()
            viewModel.getAllFolders()
        }

    }

    fun showRecyclerView (recyclerView: RecyclerView){
        binding.recyclerViewFolders.visibility = View.GONE
        binding.recyclerViewExpressions.visibility = View.GONE
        binding.recyclerViewExpressionsInFolder.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
    }

    fun showSearchView (searchView: SearchView){
        binding.searchViewInFolder.visibility = View.GONE
        binding.searchViewFolders.visibility = View.GONE
        binding.searchViewExpressions.visibility = View.GONE

        searchView.setQuery("", false)
        searchView.visibility = View.VISIBLE
    }

    private fun folderEditClicked (folder: FolderData) {
        // Dialog için bir LayoutInflater oluştur

        val d = showDialog(R.layout.dialog_create_folder, requireActivity(), false)

        // Dialog içindeki öğelere erişim
        val tvCreateFolderTitle = d.first.findViewById<TextView>(R.id.tv_create_folder_title)
        val etCreateFolder = d.first.findViewById<EditText>(R.id.et_create_folder_name)
        val btnOk = d.first.findViewById<TextView>(R.id.tv_ok)
        val btnCancel = d.first.findViewById<TextView>(R.id.tv_cancel)

        tvCreateFolderTitle.setText(getString(R.string.rename_folder))
        etCreateFolder.setText(folder.name)

        btnOk.setOnClickListener {
            val text = etCreateFolder.text.toString()
            if (text.isNotEmpty()) {
                viewModel.renameFolder(folder.id, text)
                viewModel.getAllFolders()
                // open folder id ile adı değişen klasör id aynı ise studyFolder'ı kapat.
                if (viewModel.openFolderId == folder.id){
                    removeStudyFolderFragment()
                }
                d.second.cancel()
            }
        }

        btnCancel.setOnClickListener{
            d.second.cancel()
        }
    }

    private fun folderItemClicked(folder: FolderData){
        animateTextChange(binding.txtListeBaslik, getString(R.string.folder_selected, folder.name.capitalizeFirstLetter()))
        viewModel.lastSelectedFolderId = folder.id
        viewModel.getExpressionsByFolder()
        viewModel.setStatus("expressionsInFolder")
    }

    private fun itemEditClicked(id: Int){
        val editFragment = EditFragment.newInstance(id)
        parentFragmentManager.beginTransaction()
            .hide(this@ListFragment)
            .add(R.id.fragment_container, editFragment, "edit")
            .addToBackStack(null) // Makes it reversible
            .commit()
    }

    private fun itemDeleteClicked(expression: ExpressionData){

        viewModel.deleteExpression(expression)

        viewModel.getAllExpressions()
        viewModel.getAllFolders()
        viewModel.getExpressionsByFolder()

        // open folder id ile silinen item folder id aynı ise studyFolder'ı kapat.
        if (viewModel.openFolderId == expression.folderId){
            removeStudyFolderFragment()
        }

    }

    private fun folderDeleteClicked (folderId: Int): Boolean{

        val success = viewModel.deleteFolderAndItsContents(folderId)

        viewModel.getExpressionsByFolder()
        viewModel.getAllExpressions()
        viewModel.getAllFolders()

        // open folder id ile silinen klasör id aynı ise studyFolder'ı kapat.
        if (viewModel.openFolderId == folderId){
            removeStudyFolderFragment()
        }

        return success
    }

    private fun removeStudyFolderFragment(){
        viewModel.openFolderId = -1
        val studyFolderFragment = parentFragmentManager.findFragmentByTag("studyFolder")
        if (studyFolderFragment != null) {
            parentFragmentManager.beginTransaction()
                .remove(studyFolderFragment)
                .commit()
        }
    }





}
