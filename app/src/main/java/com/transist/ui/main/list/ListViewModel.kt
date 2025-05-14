package com.transist.ui.main.list

import androidx.lifecycle.ViewModel
import com.transist.data.model.ExpressionData
import com.transist.data.model.FolderData
import com.transist.data.repository.StudyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.collections.List

class ListViewModel(
    private val studyRepo: StudyRepository
    ) : ViewModel() {

    var lastSelectedFolderId = -1
    var openFolderId = -1

    private val _folders = MutableStateFlow<List<FolderData>>(studyRepo.getAllFoldersByTargetLanguage())
    val folders: StateFlow<List<FolderData>> = _folders

    private val _expressions = MutableStateFlow<List<ExpressionData>>(studyRepo.getAllExpressions())
    val expressions: StateFlow<List<ExpressionData>> = _expressions

    private val _expressionsInFolder = MutableStateFlow<List<ExpressionData>>(studyRepo.getExpressionsByFolder(lastSelectedFolderId))
    val expressionsInFolder: StateFlow<List<ExpressionData>> = _expressionsInFolder

    private val _status = MutableStateFlow<String>("folders")
    val status: StateFlow<String> = _status

    fun setStatus(status: String) {
        _status.value = status
    }

    fun getAllExpressions (){
        _expressions.value = studyRepo.getAllExpressions()
    }

    fun getExpressionsByFolder (){
        _expressionsInFolder.value = studyRepo.getExpressionsByFolder(lastSelectedFolderId)
    }

    fun getAllFolders(){
        _folders.value = studyRepo.getAllFoldersByTargetLanguage()
    }

    fun deleteExpression(expression: ExpressionData) {
        studyRepo.deleteExpression(expression)
    }

    fun deleteFolderAndItsContents(folderId: Int): Boolean {
        return studyRepo.deleteFolderAndItsContents(folderId)
    }

    fun renameFolder(id: Int, newName: String): Boolean {
        return studyRepo.renameFolder(id, newName)
    }

    fun getAllDeletedFolders(): MutableList<FolderData> {
        return studyRepo.getAllDeletedFolders()
    }

    fun getAllDeletedExpressions(): MutableList<ExpressionData> {
        return studyRepo.getAllDeletedExpressions()
    }

    fun restoreFolderAndItsContents(folderId: Int): Boolean {
        return studyRepo.restoreFolderAndItsContents(folderId)
    }

    fun restoreExpression(expressionId: Int): Boolean {
        return studyRepo.restoreExpression(expressionId)
    }

    fun getDeletedExpressionsByFolder(lastSelectedFolderId: Int): MutableList<ExpressionData> {
        return studyRepo.getDeletedExpressionsByFolder(lastSelectedFolderId)
    }

    fun updateActiveStudyFolder() {
        studyRepo.updateActiveStudyFolder(lastSelectedFolderId)
    }

    fun searchExpressions(query: String): List<ExpressionData> {
        return studyRepo.searchExpressions(query)
    }

    fun searchInFolders(query: String): List<ExpressionData> {
        return studyRepo.searchInFolders(query, lastSelectedFolderId)
    }

    fun searchFolders(query: String): MutableList<FolderData> {
        return studyRepo.searchFolders(query)
    }

    fun createFolder(name: String): Int {
        return studyRepo.createFolder(name)
    }

    fun updateActiveAddFolder() {
        studyRepo.updateActiveAddFolder(lastSelectedFolderId)
    }

    fun resetStudyModeStates() {
        studyRepo.resetStudyModeStates()
    }

    fun setStudyModeState(state: String) {
        studyRepo.setStudyModeState(state)
    }

}