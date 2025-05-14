package com.transist.data.repository

import android.content.Context
import com.transist.data.model.ExpressionData
import com.transist.data.model.FolderData
import com.transist.data.local.DatabaseHelper
import com.transist.data.remote.response.Sentence
import com.transist.util.capitalizeFirstLetter

class StudyRepository(private val context: Context) {

    private val dbHelper = DatabaseHelper(context)

    fun getActiveFolder(): Pair<String, Int> {
        val folderId = dbHelper.getActiveAddFolder()
        val folderName = dbHelper.getFolderNameById(folderId).capitalizeFirstLetter()
        return Pair(folderName, folderId)
    }

    fun getAllFoldersByTargetLanguage(): List<FolderData> {
        return dbHelper.getAllFoldersByTargetLanguage()
    }

    fun updateActiveAddFolder(folderId: Int) {
        dbHelper.updateActiveAddFolder(folderId)
    }

    fun updateActiveStudyFolder(folderId: Int) {
        dbHelper.updateActiveStudyFolder(folderId)
    }

    fun getExpressionByIdNew(id: Int): ExpressionData {
        return dbHelper.getExpressionByIdNew(id)
    }

    fun getFolderNameById(id: Int): String {
        return dbHelper.getFolderNameById(id)
    }

    fun updateSelectedSentences(id: Int,
                                folderId: Int,
                                expression: String,
                                meaning: String,
                                note: String,
                                selectedSentences: MutableList<Sentence>): Int {
        return dbHelper.updateSelectedSentences(id, folderId, expression, meaning, note, selectedSentences, context)
    }

    fun saveSelectedSentences(
        expression: String,
        meaning: String,
        note: String,
        selectedSentences: List<Sentence>,
        folderId: Int
    ) {
        dbHelper.saveSelectedSentences(expression, meaning, note, selectedSentences, folderId)
    }

    fun deleteExpression(expression: ExpressionData) {
        dbHelper.deleteExpression(expression.id)
    }

    fun getAllExpressions(): List<ExpressionData> {
        return dbHelper.getAllExpressions()
    }

    fun getExpressionsByFolder(folderId: Int): List<ExpressionData> {
        return dbHelper.getExpressionsByFolder(folderId)
    }

    fun deleteFolderAndItsContents(folderId: Int): Boolean {
        return dbHelper.deleteFolderAndItsContents(folderId)
    }

    fun renameFolder(id: Int, newName: String): Boolean {
        return dbHelper.renameFolder(id, newName)
    }

    fun getAllDeletedFolders (): MutableList<FolderData> {
        return dbHelper.getAllDeletedFolders()
    }

    fun getAllDeletedExpressions (): MutableList<ExpressionData> {
        return dbHelper.getAllDeletedExpressions()
    }

    fun restoreFolderAndItsContents(folderId: Int): Boolean {
        return dbHelper.restoreFolderAndItsContents(folderId)
    }

    fun restoreExpression(expressionId: Int): Boolean {
        return dbHelper.restoreExpression(expressionId)
    }

    fun getDeletedExpressionsByFolder(lastSelectedFolderId: Int): MutableList<ExpressionData> {
        return dbHelper.getDeletedExpressionsByFolder(lastSelectedFolderId)
    }

    fun searchExpressions(query: String): List<ExpressionData> {
        return dbHelper.searchExpressions(query)
    }

    fun searchInFolders(query: String, folderId: Int): List<ExpressionData> {
        return dbHelper.searchInFolders(query, folderId)
    }

    fun searchFolders(query: String): MutableList<FolderData> {
        return dbHelper.searchFolders(query)
    }

    fun createFolder(name: String): Int {
        return dbHelper.createFolder(name)
    }

    fun resetStudyModeStates() {
        dbHelper.resetStudyModeStates()
    }

    fun setStudyModeState(bolum: String) {
        dbHelper.setStudyModeState(bolum)
    }

    fun getRandomWord(activeLevel: String, languageCode: String): String?{
        return dbHelper.getRandomWordFromDatabase(activeLevel, languageCode)
    }

    fun getRandomExpressionByFolder(): ExpressionData? {
        return dbHelper.getRandomExpressionByFolder()
    }

    fun getExpressionCountByLanguageCode(targetLanguageCode: String): Int {
        return dbHelper.getExpressionCountByLanguageCode(targetLanguageCode)
    }

    fun updateSentenceIndex(item: ExpressionData): Int {
        return dbHelper.updateSentenceIndex(item)
    }

    fun getSentenceById(item: ExpressionData): Pair<String, String>{
        return dbHelper.getSentenceById(item)
    }

}
