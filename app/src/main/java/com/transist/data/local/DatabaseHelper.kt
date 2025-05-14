package com.transist.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.transist.data.model.ExpressionData
import com.transist.data.model.FolderData
import com.transist.data.remote.response.Sentence
import java.util.Locale

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    private val appContext: Context = context.applicationContext // Uygulama context'ini saklamak daha güvenli olabilir

    // Dillerin kodları
    private val languageCodes = listOf(
        "ar", "bn", "bg", "zh-CN", "hr", "cs", "da", "nl", "et", "fi",
        "fr", "de", "el", "he", "hi", "hu", "id", "it", "ja", "ko",
        "lv", "lt", "no", "pl", "pt", "ro", "ru", "sr", "sk", "sl",
        "es", "sw", "sv", "th", "tr", "uk", "vi"
    )

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "database.db"

        private const val TABLE_EXPRESSIONS = "expressions"
        private const val COLUMN_ID_EXPRESSION = "id"
        private const val COLUMN_EXPRESSION = "expression"
        private const val COLUMN_MEANING = "meaning"
        private const val COLUMN_NOTE = "note"
        private const val COLUMN_SENTENCES = "sentences"
        private const val COLUMN_NEXT_SENTENCE_INDEX = "next_sentence_index"
        private const val COLUMN_DATE_CREATED_EXPRESSION = "date_created"
        private const val COLUMN_LANGUAGE_CODE = "language_code"
        private const val COLUMN_FOLDER_ID = "folder_id"
        private const val COLUMN_STATUS = "status"

        private const val TABLE_FOLDERS = "folders"
        private const val COLUMN_ID_FOLDER = "id"
        private const val COLUMN_NAME = "name"
        private const val COLUMN_TARGET_LANGUAGE_FOLDER = "target_language"
        private const val COLUMN_STATUS_ADD = "status_add"
        private const val COLUMN_STATUS_STUDY = "status_study"
        private const val COLUMN_DATE_CREATED_FOLDER = "date_created"

        private const val TABLE_LANGUAGES = "languages"
        private const val COLUMN_NATIVE_LANGUAGE = "native_language_code"
        private const val COLUMN_TARGET_LANGUAGE = "target_language_code"
        private const val COLUMN_DIRECTION = "direction"

        private const val TABLE_WORDS = "words"
        private const val COLUMN_WORD = "word"
        private const val COLUMN_LEVEL = "level"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createExpressionsTableStatement = """
            CREATE TABLE IF NOT EXISTS $TABLE_EXPRESSIONS (
                $COLUMN_ID_EXPRESSION INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_EXPRESSION TEXT NOT NULL,
                $COLUMN_MEANING TEXT,
                $COLUMN_NOTE TEXT,
                $COLUMN_SENTENCES TEXT NOT NULL,
                $COLUMN_NEXT_SENTENCE_INDEX INTEGER DEFAULT 0,
                $COLUMN_DATE_CREATED_EXPRESSION INTEGER NOT NULL,
                $COLUMN_LANGUAGE_CODE TEXT NOT NULL,
                $COLUMN_FOLDER_ID INTEGER NOT NULL,
                $COLUMN_STATUS INTEGER NOT NULL
            )
        """
        db.execSQL(createExpressionsTableStatement)

        createFoldersTable(db)
        createLanguagesTable(db)
        createRecyclerBinExpressionsTable(db)
        createRecyclerBinFoldersTable(db)
        createRecyclerBinExpressionInFolderTable(db)
        createWordsTable(db)

        // Fill languages table
        fillWordsTableByLevel(db, "A1.txt", "A1")
        fillWordsTableByLevel(db, "A2.txt", "A2")
        fillWordsTableByLevel(db, "B1.txt", "B1")
        fillWordsTableByLevel(db, "B2.txt", "B2")
        fillWordsTableByLevel(db, "C1.txt", "C1")
        loadAllLanguageFiles(db)

    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_EXPRESSIONS")
        onCreate(db)
    }

    fun createRecyclerBinExpressionInFolderTable(db: SQLiteDatabase) {
        val createFoldersTableStatement = """
        CREATE TABLE IF NOT EXISTS recycler_bin_expression_in_folder (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            expression TEXT,
            meaning TEXT,
            note TEXT,
            sentences TEXT,
            next_sentence_index INTEGER,
            date_created INTEGER,
            language_code TEXT,
            folder_id INTEGER,
            status INTEGER,
            deletion_date INTEGER
        )
    """
        db.execSQL(createFoldersTableStatement)
    }

    fun createRecyclerBinFoldersTable(db: SQLiteDatabase) {
        val createRecyclerBinFoldersTableStatement = """
        CREATE TABLE IF NOT EXISTS recycler_bin_folders (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT,
            target_language TEXT,
            status_add INTEGER,
            status_study INTEGER,
            date_created INTEGER,
            deletion_date INTEGER
        )
    """
        db.execSQL(createRecyclerBinFoldersTableStatement)
    }

    fun createRecyclerBinExpressionsTable(db: SQLiteDatabase) {
        val createRecyclerBinExpressionsTableStatement = """
        CREATE TABLE IF NOT EXISTS recycler_bin_expressions (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            expression TEXT,
            meaning TEXT,
            note TEXT,
            sentences TEXT,
            next_sentence_index INTEGER,
            date_created INTEGER,
            language_code TEXT,
            folder_id INTEGER,
            status INTEGER,
            deletion_date INTEGER
        )
    """
        db.execSQL(createRecyclerBinExpressionsTableStatement)
    }

    fun createFoldersTable(db: SQLiteDatabase) {
        val createFoldersTableStatement = """
        CREATE TABLE IF NOT EXISTS folders (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT,
            target_language TEXT,
            status_add INTEGER,
            status_study INTEGER,
            date_created INTEGER
        )
    """
        db.execSQL(createFoldersTableStatement)
    }

    fun createLanguagesTable(db: SQLiteDatabase) {
        // Tabloyu oluştur
        val createLanguagesTableStatement = """
        CREATE TABLE IF NOT EXISTS languages (
            native_language_code TEXT,
            target_language_code TEXT,
            direction TEXT
        )
    """
        db.execSQL(createLanguagesTableStatement)

        // Cihaz dili
        val deviceLanguageCode = Locale.getDefault().language
        val targetLanguageCode = "un"

        // Başlangıç verisini ekle
        val insertStatement = """
        INSERT INTO languages (native_language_code, target_language_code, direction)
        VALUES (?, ?, ?)
    """
        val statement = db.compileStatement(insertStatement)
        statement.bindString(1, deviceLanguageCode)
        statement.bindString(2, targetLanguageCode)
        statement.bindString(3, "to")
        statement.executeInsert()
    }

    fun createWordsTable (db: SQLiteDatabase) {
        val createWordsTableStatement = """
            CREATE TABLE IF NOT EXISTS words (
                word TEXT,
                level TEXT,
                language_code TEXT
            )
        """
        db.execSQL(createWordsTableStatement)
    }

    fun fillWordsTableByLevel(db: SQLiteDatabase, fileName: String, level: String){
        val wordReader = TxtFileWordReader(appContext, fileName) // "C1.txt"
        while (true) {
            val word = wordReader.getNextWordFromTxtFile()
            if (word == null) {
                println("All words processed.")
                break
            }
            val firstWord = word.substringBefore(" ")
            val values = ContentValues().apply {
                put("word", firstWord)
                put("level", level)
                put("language_code", "en")
            }
            db.insert("words", null, values)
        }
    }

    private fun loadAllLanguageFiles(db: SQLiteDatabase) {
        for ((index, code) in languageCodes.withIndex()) {
            val fileName = "words_${code}.sql"
            Log.d("DB_INIT", "(${index + 1}/${languageCodes.size}) Loading $fileName ...")
            insertFromAsset(db, fileName)
        }
        Log.d("DB_INIT", "✅ All language files imported successfully.")
    }

    private fun insertFromAsset(db: SQLiteDatabase, fileName: String) {
        try {
            appContext.assets.open(fileName).use { inputStream ->
                val sqlText = inputStream.bufferedReader(Charsets.UTF_8).readText()

                // Dosyadaki komutları ';' ile bölüp sırayla çalıştır
                sqlText.split(";")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .forEach { statement ->
                        try {
                            db.execSQL(statement)
                        } catch (e: Exception) {
                            Log.e("DB_INIT", "Error executing SQL from $fileName: ${e.message}")
                        }
                    }
            }
            Log.d("DB_INIT", "✔ Imported $fileName")
        } catch (e: Exception) {
            Log.e("DB_INIT", "❌ Failed to load $fileName: ${e.message}")
        }
    }

    fun resetStudyModeStates() {
        val db = writableDatabase
        val resetStatement = """
        UPDATE study_mode
        SET state = 0
    """
        db.execSQL(resetStatement)
    }

    fun setStudyModeState(bolum: String) {
        val db = writableDatabase
        val updateStatement = """
        UPDATE study_mode
        SET state = 1
        WHERE mode = ?
    """
        db.execSQL(updateStatement, arrayOf(bolum))
    }

    fun updateLanguages(db: SQLiteDatabase, newNativeLanguage: String, newTargetLanguage: String) {
        val updateStatement = """
        UPDATE languages
        SET native_language_code = ?, target_language_code = ?
    """
        val statement = db.compileStatement(updateStatement)
        statement.bindString(1, newNativeLanguage)
        statement.bindString(2, newTargetLanguage)
        statement.executeUpdateDelete()
    }

    fun readLanguages(db: SQLiteDatabase): Pair<String, String>? {
        val cursor = db.rawQuery("SELECT native_language_code, target_language_code FROM languages LIMIT 1", null)
        var result: Pair<String, String>? = null

        if (cursor.moveToFirst()) {
            val nativeLanguageCode = cursor.getString(cursor.getColumnIndexOrThrow("native_language_code"))
            val targetLanguageCode = cursor.getString(cursor.getColumnIndexOrThrow("target_language_code"))
            result = Pair(nativeLanguageCode, targetLanguageCode)
        }

        cursor.close()
        return result
    }

    fun getRandomWordFromDatabase(level: String, languageCode: String): String? {
        var randomWord: String? = null
        // SQL sorgusu ile rastgele bir satır seçiyoruz
        val query = "SELECT word FROM words WHERE level = ? AND LOWER(language_code) = LOWER(?) ORDER BY RANDOM() LIMIT 1"
        val cursor = readableDatabase.rawQuery(query, arrayOf(level, languageCode))


        // Cursor'dan kelimeyi al
        if (cursor.moveToFirst()) {
            randomWord = cursor.getString(cursor.getColumnIndexOrThrow("word"))
        }

        // Cursor'u kapat
        cursor.close()

        return randomWord
    }

    fun dropTable(db: SQLiteDatabase, tableName: String) {
        val dropTableQuery = "DROP TABLE IF EXISTS $tableName"
        db.execSQL(dropTableQuery)
    }

    fun isTableExists(db: SQLiteDatabase, tableName: String): Boolean {
        val query = """
        SELECT name 
        FROM sqlite_master 
        WHERE type = 'table' AND name = ?
    """
        val cursor = db.rawQuery(query, arrayOf(tableName))
        cursor.use {
            return it.count > 0
        }
    }

    fun getCountToStudyInFolder(): Int {
        val folderId = getActiveStudyFolder().first
        val db = this.readableDatabase

        var count = 0

        // Sorguyu hazırla
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM expressions WHERE folder_id = ? AND status = 0",
            arrayOf(folderId.toString())
        )
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0) // İlk sütun, COUNT(*) sonucudur
        }
        cursor.close()

        return count
    }

    fun getStudiedCountInFolder(): Int {
        val folderId = getActiveStudyFolder().first
        val db = this.readableDatabase

        var count = 0

        // Sorguyu hazırla
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM expressions WHERE folder_id = ? AND status = 1",
            arrayOf(folderId.toString())
        )
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0) // İlk sütun, COUNT(*) sonucudur
        }
        cursor.close()

        return count
    }

    fun getRandomExpressionByFolder (): ExpressionData? {

        val db = this.readableDatabase // veya writableDatabase
        val folderId = getActiveStudyFolder().first
        val selection = "folder_id = ? AND status = ?"
        val selectionArgs = arrayOf(folderId.toString(), "0")

        // Sorguyu çalıştırarak cursor'ı oluştur
        val cursor: Cursor? = db.query(
            "expressions",    // Tablo adı
            null,             // Sütunlar (null = tüm sütunları getirir: SELECT *)
            selection,        // WHERE şartı
            selectionArgs,    // WHERE argümanları (? yerine gelecek değerler)
            null,             // groupBy
            null,             // having
            "RANDOM()",       // Rastgele sıralama
            "1"               // Limit (sadece 1 satır getir)
        )

        var item: ExpressionData? = null
        cursor?.use {
            if (it.moveToFirst()) {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                val expression = cursor.getString(cursor.getColumnIndexOrThrow("expression"))
                val meaning = cursor.getString(cursor.getColumnIndexOrThrow("meaning"))
                val note = cursor.getString(cursor.getColumnIndexOrThrow("note"))
                val sentences =
                    cursor.getString(cursor.getColumnIndexOrThrow("sentences"))?.split("||")
                val nextSentenceIndex =
                    cursor.getInt(cursor.getColumnIndexOrThrow("next_sentence_index"))
                val dateCreated = cursor.getLong(cursor.getColumnIndexOrThrow("date_created"))
                val dilKodu = cursor.getString(cursor.getColumnIndexOrThrow("language_code"))
                val status = cursor.getInt(cursor.getColumnIndexOrThrow("status"))
                val folderName = getFolderNameById(folderId)


                item = ExpressionData(
                    id = id,
                    expression = expression,
                    meaning = meaning,
                    note = note,
                    sentences = sentences,
                    nextSentenceIndex = nextSentenceIndex,
                    dateCreated = dateCreated,
                    dilKodu = dilKodu,
                    folderId = folderId,
                    status = status,
                    folderName = folderName
                )
            }
        }

        return item
    }

    fun getSentenceById(item: ExpressionData): Pair<String, String> {
        var englishSentence = ""
        var turkishSentence = ""


        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM expressions WHERE id = ?", arrayOf(item.id.toString()))

        if (cursor.moveToFirst()) {

            val sentencesString = cursor.getString(cursor.getColumnIndexOrThrow("sentences"))
            val nextSentenceIndex = cursor.getInt(cursor.getColumnIndexOrThrow("next_sentence_index"))

            // Cümleleri ayır
            val sentencesArray = sentencesString.split("||")

            if (nextSentenceIndex < sentencesArray.size) {
                // İlgili cümleyi al
                val sentenceString = sentencesArray[nextSentenceIndex].trim()
                // "Sentence(...)" yapısını ayrıştır
                val englishStart = sentenceString.indexOf("sentence=") + 9
                val englishEnd = sentenceString.indexOf(", translation=")
                englishSentence = sentenceString.substring(englishStart, englishEnd).trim()

                val turkishStart = sentenceString.indexOf("translation=") + 12
                val turkishEnd = sentenceString.indexOf(')', turkishStart)
                turkishSentence = sentenceString.substring(turkishStart, turkishEnd).trim()

            }
        }

        cursor.close()
        db.close()


        return Pair(englishSentence, turkishSentence)

    }

    fun getExpressionByIdNew(id: Int): ExpressionData {
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM expressions WHERE id = ?", arrayOf(id.toString()))
        if (cursor.moveToFirst()) {
            val expression = ExpressionData(
                id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                expression = cursor.getString(cursor.getColumnIndexOrThrow("expression")),
                meaning = cursor.getString(cursor.getColumnIndexOrThrow("meaning")),
                note = cursor.getString(cursor.getColumnIndexOrThrow("note")),
                sentences = cursor.getString(cursor.getColumnIndexOrThrow("sentences"))?.split("||"),
                nextSentenceIndex = cursor.getInt(cursor.getColumnIndexOrThrow("next_sentence_index")),
                dateCreated = cursor.getLong(cursor.getColumnIndexOrThrow("date_created")),
                dilKodu = cursor.getString(cursor.getColumnIndexOrThrow("language_code")),
                folderId = cursor.getInt(cursor.getColumnIndexOrThrow("folder_id")),
                status = cursor.getInt(cursor.getColumnIndexOrThrow("status")),
                folderName = getFolderNameById(cursor.getInt(cursor.getColumnIndexOrThrow("folder_id")))
            )
            cursor.close()
            db.close()
            return expression
        } else {
            cursor.close()
            db.close()
            throw IllegalArgumentException("ID $id ile eşleşen kayıt bulunamadı.")
        }
    }

    fun getAllExpressions(): MutableList<ExpressionData> {
        val expressions = mutableListOf<ExpressionData>()
        val targetLanguageCode = readLanguages(readableDatabase)?.second ?: "un"
        val db = readableDatabase
        val query = "SELECT * FROM expressions WHERE language_code = '$targetLanguageCode' ORDER BY date_created DESC"
        val cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                val expression = cursor.getString(cursor.getColumnIndexOrThrow("expression"))
                val meaning = cursor.getString(cursor.getColumnIndexOrThrow("meaning"))
                val note = cursor.getString(cursor.getColumnIndexOrThrow("note"))
                val sentences = cursor.getString(cursor.getColumnIndexOrThrow("sentences"))?.split("||")
                val nextSentenceIndex = cursor.getInt(cursor.getColumnIndexOrThrow("next_sentence_index"))
                val dateCreated = cursor.getLong(cursor.getColumnIndexOrThrow("date_created"))
                val dilKodu = cursor.getString(cursor.getColumnIndexOrThrow("language_code"))
                val folderId = cursor.getInt(cursor.getColumnIndexOrThrow("folder_id"))
                val status = cursor.getInt(cursor.getColumnIndexOrThrow("status"))
                val folderName = getFolderNameById(folderId)

                // ExpressionData nesnesini oluştur ve listeye ekle
                val expressionData = ExpressionData(
                    id,
                    expression,
                    meaning,
                    note,
                    sentences,
                    nextSentenceIndex,
                    dateCreated,
                    dilKodu,
                    folderId,
                    status,
                    folderName
                )
                expressions.add(expressionData)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()

        return expressions
    }

    fun getAllDeletedExpressions(): MutableList<ExpressionData> {
        val expressionList: MutableList<ExpressionData> = mutableListOf()
        val db = this.readableDatabase
        val targetLanguageCode = readLanguages(readableDatabase)?.second ?: "un"
        val query = "SELECT * FROM recycler_bin_expressions WHERE language_code = '$targetLanguageCode' ORDER BY deletion_date DESC"

        // Sorgu sonucunu bir Cursor nesnesiyle alıyoruz.
        // 'use' bloğu, işlem bittiğinde Cursor'ın otomatik olarak kapatılmasını sağlar.
        db.rawQuery(query, null).use { cursor ->
            // Cursor'da veri varsa ilk satıra git
            if (cursor.moveToFirst()) {
                do {
                    // Sütunlardaki verileri al
                    val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                    val expression = cursor.getString(cursor.getColumnIndexOrThrow("expression"))
                    val meaning = cursor.getString(cursor.getColumnIndexOrThrow("meaning"))
                    val note = cursor.getString(cursor.getColumnIndexOrThrow("note"))
                    val sentences = cursor.getString(cursor.getColumnIndexOrThrow("sentences"))?.split("||")
                    val nextSentenceIndex = cursor.getInt(cursor.getColumnIndexOrThrow("next_sentence_index"))
                    val dateCreated = cursor.getLong(cursor.getColumnIndexOrThrow("date_created"))
                    val dilKodu = cursor.getString(cursor.getColumnIndexOrThrow("language_code"))
                    val folderId = cursor.getInt(cursor.getColumnIndexOrThrow("folder_id"))
                    val status = cursor.getInt(cursor.getColumnIndexOrThrow("status"))
                    val folderName = getFolderNameById(folderId)

                    // ExpressionData nesnesi oluştur ve listeye ekle
                    val expressionData = ExpressionData(
                        id,
                        expression,
                        meaning,
                        note,
                        sentences,
                        nextSentenceIndex,
                        dateCreated,
                        dilKodu,
                        folderId,
                        status,
                        folderName
                    )
                    expressionList.add(expressionData)
                } while (cursor.moveToNext()) // Bir sonraki satıra geç
            }
        }
        // Veritabanı bağlantısını kapatmaya gerek yok, çünkü SQLiteOpenHelper bunu yönetir.
        return expressionList
    }

    fun getDeletedExpressionsByFolder(lastSelectedFolderId: Int): MutableList<ExpressionData> {
        val expressionList: MutableList<ExpressionData> = mutableListOf()
        val db = this.readableDatabase
        val query = "SELECT * FROM recycler_bin_expressions WHERE folder_id = '$lastSelectedFolderId' ORDER BY deletion_date DESC"

        // Sorgu sonucunu bir Cursor nesnesiyle alıyoruz.
        // 'use' bloğu, işlem bittiğinde Cursor'ın otomatik olarak kapatılmasını sağlar.
        db.rawQuery(query, null).use { cursor ->
            // Cursor'da veri varsa ilk satıra git
            if (cursor.moveToFirst()) {
                do {
                    // Sütunlardaki verileri al
                    val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                    val expression = cursor.getString(cursor.getColumnIndexOrThrow("expression"))
                    val meaning = cursor.getString(cursor.getColumnIndexOrThrow("meaning"))
                    val note = cursor.getString(cursor.getColumnIndexOrThrow("note"))
                    val sentences = cursor.getString(cursor.getColumnIndexOrThrow("sentences"))?.split("||")
                    val nextSentenceIndex = cursor.getInt(cursor.getColumnIndexOrThrow("next_sentence_index"))
                    val dateCreated = cursor.getLong(cursor.getColumnIndexOrThrow("date_created"))
                    val dilKodu = cursor.getString(cursor.getColumnIndexOrThrow("language_code"))
                    val folderId = cursor.getInt(cursor.getColumnIndexOrThrow("folder_id"))
                    val status = cursor.getInt(cursor.getColumnIndexOrThrow("status"))
                    val folderName = getFolderNameById(folderId)

                    // ExpressionData nesnesi oluştur ve listeye ekle
                    val expressionData = ExpressionData(
                        id,
                        expression,
                        meaning,
                        note,
                        sentences,
                        nextSentenceIndex,
                        dateCreated,
                        dilKodu,
                        folderId,
                        status,
                        folderName
                    )
                    expressionList.add(expressionData)
                } while (cursor.moveToNext()) // Bir sonraki satıra geç
            }
        }
        // Veritabanı bağlantısını kapatmaya gerek yok, çünkü SQLiteOpenHelper bunu yönetir.
        return expressionList
    }

    fun getAllDeletedFolders(): MutableList<FolderData> {
        val folderList: MutableList<FolderData> = mutableListOf()
        val db = this.readableDatabase
        val targetLanguageCode = readLanguages(readableDatabase)?.second ?: "un"
        val query = "SELECT * FROM recycler_bin_folders WHERE target_language = '$targetLanguageCode' ORDER BY deletion_date DESC"

        // Sorgu sonucunu bir Cursor nesnesiyle alıyoruz.
        // 'use' bloğu, işlem bittiğinde Cursor'ın otomatik olarak kapatılmasını sağlar.
        db.rawQuery(query, null).use { cursor ->
            // Cursor'da veri varsa ilk satıra git
            if (cursor.moveToFirst()) {
                do {
                    // Sütunlardaki verileri al
                    val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                    val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                    val targetLanguage = cursor.getString(cursor.getColumnIndexOrThrow("target_language"))
                    val statusAdd = cursor.getInt(cursor.getColumnIndexOrThrow("status_add"))
                    val statusStudy = cursor.getInt(cursor.getColumnIndexOrThrow("status_study"))
                    val dateCreated = cursor.getInt(cursor.getColumnIndexOrThrow("date_created"))
                    val itemCount =  getExpressionCountInDeletedFolder(id)

                    // ExpressionData nesnesi oluştur ve listeye ekle
                    val expressionData = FolderData(
                        id,
                        name,
                        targetLanguage,
                        statusAdd,
                        statusStudy,
                        dateCreated,
                        itemCount
                    )
                    folderList.add(expressionData)
                } while (cursor.moveToNext()) // Bir sonraki satıra geç
            }
        }
        // Veritabanı bağlantısını kapatmaya gerek yok, çünkü SQLiteOpenHelper bunu yönetir.
        return folderList
    }

    fun restoreExpression(id: Int): Boolean {
        val db = writableDatabase
        var success = false

        db.beginTransaction()
        try {
            db.query(
                "recycler_bin_expressions",
                null, // null -> tüm sütunları seçer
                "id = ?",
                arrayOf(id.toString()),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val valuesToInsert = ContentValues()

                    for (i in 0 until cursor.columnCount) {
                        // "id" ve "deletion_date" dışındaki tüm sütunlar
                        if (cursor.getColumnName(i) != "id"
                            && cursor.getColumnName(i) != "deletion_date"
                            && cursor.getColumnName(i) != "date_created") {
                            valuesToInsert.put(cursor.getColumnName(i), cursor.getString(i))
                        }
                    }
                    valuesToInsert.put("date_created", System.currentTimeMillis())
                    // 4. Adım: Hazırlanan veriyi "recycleBinExpressions" tablosuna ekle.
                    val insertResult = db.insert("expressions", null, valuesToInsert)

                    if (insertResult != -1L) {
                        // 5. Adım: Orijinal kaydı "expressions" tablosundan sil.
                        db.delete("recycler_bin_expressions", "id = ?", arrayOf(id.toString()))
                        success = true
                    }
                }
            }

            if (success) {
                // Tüm adımlar başarılıysa transaction'ı onayla.
                db.setTransactionSuccessful()
            }
        } finally {
            // Transaction'ı sonlandır. setTransactionSuccessful() çağrılmadıysa
            // yapılan tüm değişiklikler otomatik olarak geri alınır (rollback).
            db.endTransaction()
        }

        return success
    }

    fun getExpressionsByFolder(folderID: Int): MutableList<ExpressionData> {
        val expressions = mutableListOf<ExpressionData>()
        val db = readableDatabase
        val query = "SELECT * FROM expressions WHERE folder_id = '$folderID' ORDER BY date_created DESC"
        val cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                val expression = cursor.getString(cursor.getColumnIndexOrThrow("expression"))
                val meaning = cursor.getString(cursor.getColumnIndexOrThrow("meaning"))
                val note = cursor.getString(cursor.getColumnIndexOrThrow("note"))
                val sentences = cursor.getString(cursor.getColumnIndexOrThrow("sentences"))?.split("||")
                val nextSentenceIndex = cursor.getInt(cursor.getColumnIndexOrThrow("next_sentence_index"))
                val dateCreated = cursor.getLong(cursor.getColumnIndexOrThrow("date_created"))
                val dilKodu = cursor.getString(cursor.getColumnIndexOrThrow("language_code"))
                val folderId = cursor.getInt(cursor.getColumnIndexOrThrow("folder_id"))
                val status = cursor.getInt(cursor.getColumnIndexOrThrow("status"))
                val folderName = getFolderNameById(folderId)

                // ExpressionData nesnesini oluştur ve listeye ekle
                val expressionData = ExpressionData(
                    id,
                    expression,
                    meaning,
                    note,
                    sentences,
                    nextSentenceIndex,
                    dateCreated,
                    dilKodu,
                    folderId,
                    status,
                    folderName
                )
                expressions.add(expressionData)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()

        return expressions
    }

    fun getExpressionCountByLanguageCode(languageCode: String): Int {
        val db = readableDatabase
        val query = "SELECT COUNT(*) FROM expressions WHERE language_code = ?"
        val cursor = db.rawQuery(query, arrayOf(languageCode))
        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
        db.close()
        return count
    }

    fun searchExpressions(query: String): List<ExpressionData> {
        val expressions = mutableListOf<ExpressionData>()
        val db = this.readableDatabase
        val targetLanguageCode = readLanguages(readableDatabase)?.second ?: "un"

        val cursor = db.rawQuery(
            "SELECT * FROM expressions WHERE (expression LIKE ? OR meaning LIKE ?) AND language_code = ?",
            arrayOf("%$query%", "%$query%", targetLanguageCode)
        )

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                val expression = cursor.getString(cursor.getColumnIndexOrThrow("expression"))
                val meaning = cursor.getString(cursor.getColumnIndexOrThrow("meaning"))
                val note = cursor.getString(cursor.getColumnIndexOrThrow("note"))
                val sentences = cursor.getString(cursor.getColumnIndexOrThrow("sentences"))?.split("||")
                val nextSentenceIndex = cursor.getInt(cursor.getColumnIndexOrThrow("next_sentence_index"))
                val dateCreated = cursor.getLong(cursor.getColumnIndexOrThrow("date_created"))
                val dilKodu = cursor.getString(cursor.getColumnIndexOrThrow("language_code"))
                val folderId = cursor.getInt(cursor.getColumnIndexOrThrow("folder_id"))
                val status = cursor.getInt(cursor.getColumnIndexOrThrow("status"))
                val folderName = getFolderNameById(folderId)
                expressions.add(ExpressionData(id, expression, meaning, note, sentences, nextSentenceIndex, dateCreated, dilKodu, folderId, status, folderName))

            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()

        return expressions
    }

    fun searchInFolders(query: String, folderId: Int): List<ExpressionData> {
        val expressions = mutableListOf<ExpressionData>()
        val db = this.readableDatabase
        val targetLanguageCode = readLanguages(readableDatabase)?.second ?: "un"

        val cursor = db.rawQuery(
            "SELECT * FROM expressions WHERE (expression LIKE ? OR meaning LIKE ?) AND language_code = ? AND folder_id = ?",
            arrayOf("%$query%", "%$query%", targetLanguageCode, folderId.toString())
        )

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                val expression = cursor.getString(cursor.getColumnIndexOrThrow("expression"))
                val meaning = cursor.getString(cursor.getColumnIndexOrThrow("meaning"))
                val note = cursor.getString(cursor.getColumnIndexOrThrow("note"))
                val sentences = cursor.getString(cursor.getColumnIndexOrThrow("sentences"))?.split("||")
                val nextSentenceIndex = cursor.getInt(cursor.getColumnIndexOrThrow("next_sentence_index"))
                val dateCreated = cursor.getLong(cursor.getColumnIndexOrThrow("date_created"))
                val dilKodu = cursor.getString(cursor.getColumnIndexOrThrow("language_code"))
                val folderId = cursor.getInt(cursor.getColumnIndexOrThrow("folder_id"))
                val status = cursor.getInt(cursor.getColumnIndexOrThrow("status"))
                val folderName = getFolderNameById(folderId)
                expressions.add(ExpressionData(id, expression, meaning, note, sentences, nextSentenceIndex, dateCreated, dilKodu, folderId, status, folderName))

            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()

        return expressions
    }

    fun searchFolders(query: String): MutableList<FolderData> {
        val folders = mutableListOf<FolderData>()
        val db = this.readableDatabase
        val targetLanguageCode = readLanguages(readableDatabase)?.second ?: "un"
        var cursor: Cursor? = null

        try {
            cursor = db.query(
                "folders", // Tablo adı
                null, // Getirilecek sütun(lar)
                "name LIKE ? AND target_language = ?", // Selection (WHERE) - Tüm satırlar için null
                arrayOf("%$query%", targetLanguageCode), // Selection Arguments
                null, // groupBy
                null, // having
                "date_created DESC" // orderBy (ID'ye göre büyükten küçüğe sıralama)
            )

            // 'use' bloğu, işlem bittiğinde veya hata oluştuğunda cursor'ı otomatik olarak kapatır.
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    do {
                        val name = c.getString(c.getColumnIndexOrThrow("name"))
                        val id = c.getInt(c.getColumnIndexOrThrow("id"))
                        val targetLanguage = c.getString(c.getColumnIndexOrThrow("target_language"))
                        val statusAdd = c.getInt(c.getColumnIndexOrThrow("status_add"))
                        val statusStudy = c.getInt(c.getColumnIndexOrThrow("status_study"))
                        val dateCreated = c.getInt(c.getColumnIndexOrThrow("date_created"))
                        val itemCount = getExpressionCountInFolder(id)
                        val folder = FolderData(id, name, targetLanguage, statusAdd, statusStudy, dateCreated, itemCount)
                        folders.add(folder)
                    } while (c.moveToNext())
                }
            }
        } catch (e: Exception) {
            Log.e("DataBaseHelper", "Error getting all folder names", e)
        }

        return folders
    }

    fun deleteExpression(id: Int): Boolean {
        val db = writableDatabase
        var success = false

        db.beginTransaction()
        try {
            // 1. Adım: Silinecek satırı "expressions" tablosundan oku.
            db.query(
                "expressions",
                null, // null -> tüm sütunları seçer
                "id = ?",
                arrayOf(id.toString()),
                null,
                null,
                null
            )?.use { cursor ->
                // Eğer ID'ye karşılık gelen bir satır bulunduysa devam et.
                if (cursor.moveToFirst()) {
                    val valuesToInsert = ContentValues()

                    // 2. Adım: Okunan satırdaki tüm sütunları ContentValues nesnesine kopyala.
                    for (i in 0 until cursor.columnCount) {
                        // "id" sütununu atlıyoruz çünkü recycleBin tablosunda muhtemelen
                        // kendi primary key'i olacaktır. Eğer aynı ID'yi korumak isterseniz bu kontrolü kaldırabilirsiniz.
                        if (cursor.getColumnName(i) != "id") {
                            valuesToInsert.put(cursor.getColumnName(i), cursor.getString(i))
                        }
                    }

                    // 3. Adım: Silinme zamanını ekle.
                    valuesToInsert.put("deletion_date", System.currentTimeMillis())

                    // 4. Adım: Hazırlanan veriyi "recycleBinExpressions" tablosuna ekle.
                    val insertResult = db.insert("recycler_bin_expressions", null, valuesToInsert)
                    // Ekleme işlemi başarılıysa (dönen değer -1 değilse)
                    if (insertResult != -1L) {
                        // 5. Adım: Orijinal kaydı "expressions" tablosundan sil.
                        db.delete("expressions", "id = ?", arrayOf(id.toString()))
                        success = true
                    }
                }
            }

            if (success) {
                // Çöp kutusunu maksimum 20 kayıtla sınırla
                db.execSQL("""
            DELETE FROM recycler_bin_expressions
            WHERE id IN (
             SELECT id FROM recycler_bin_expressions
             ORDER BY deletion_date ASC
             LIMIT (
            SELECT CASE WHEN COUNT(*) > 20 THEN COUNT(*) - 20 ELSE 0 END
            FROM recycler_bin_expressions
             )
             )
            """.trimIndent())
                // Tüm adımlar başarılıysa transaction'ı onayla.
                db.setTransactionSuccessful()
            }

        } finally {
            // Transaction'ı sonlandır. setTransactionSuccessful() çağrılmadıysa
            // yapılan tüm değişiklikler otomatik olarak geri alınır (rollback).
            db.endTransaction()
        }

        return success
    }

    private fun moveExpressionsToRecycleBin(db: SQLiteDatabase, folderId: Int): Boolean {
        db.query(
            "expressions",
            null,
            "folder_id = ?",
            arrayOf(folderId.toString()),
            null, null, null
        )?.use { cursor ->
            if (cursor.count == 0) {
                return true // Taşınacak ifade yoksa, bu adım başarılıdır.
            }

            val currentTime = System.currentTimeMillis()
            while (cursor.moveToNext()) {
                val valuesToInsert = ContentValues()
                for (i in 0 until cursor.columnCount) {
                    if (cursor.getColumnName(i) != "id") {
                        valuesToInsert.put(cursor.getColumnName(i), cursor.getString(i))
                    }
                }
                valuesToInsert.put("deletion_date", currentTime)

                val insertResult = db.insert("recycler_bin_expression_in_folder", null, valuesToInsert)
                if (insertResult == -1L) {
                    return false // Herhangi bir kopyalama başarısız olursa, hemen false dön.
                }
            }
        } ?: return false // Sorgu başarısız olursa (cursor null ise)

        // Tüm ifadeler başarıyla kopyalandıysa, orijinallerini sil.
        db.delete("expressions", "folder_id = ?", arrayOf(folderId.toString()))
        return true
    }

    private fun moveFolderToRecycleBin(db: SQLiteDatabase, folderId: Int): Boolean {
        db.query(
            "folders",
            null,
            "id = ?",
            arrayOf(folderId.toString()),
            null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val valuesToInsert = ContentValues()
                for (i in 0 until cursor.columnCount) {
                    //if (cursor.getColumnName(i) != "id") {
                        valuesToInsert.put(cursor.getColumnName(i), cursor.getString(i))
                    //}
                }
                valuesToInsert.put("deletion_date", System.currentTimeMillis())

                val insertResult = db.insert("recycler_bin_folders", null, valuesToInsert)
                if (insertResult != -1L) {
                    db.delete("folders", "id = ?", arrayOf(folderId.toString()))
                    return true // Başarılı
                }
            }
        }
        return false // Klasör bulunamadı veya ekleme başarısız oldu.
    }

    /**
     * Bir klasörü ve içindeki tüm ifadeleri atomik bir işlemle siler.
     * @param folderId Silinecek klasörün ID'si.
     * @return Tüm işlemler başarıyla tamamlanırsa true, aksi takdirde false döner.
     */
    fun deleteFolderAndItsContents(folderId: Int): Boolean {
        val db = writableDatabase
        var isSuccess = false

        // Tek bir transaction başlatıyoruz.
        db.beginTransaction()
        try {
            // 1. Adım: Klasörün içindeki ifadeleri geri dönüşüm kutusuna taşı ve sil.
            val expressionsMoved = moveExpressionsToRecycleBin(db, folderId)

            // 2. Adım: Sadece ilk adım başarılıysa, klasörün kendisini taşımaya devam et.
            if (expressionsMoved) {
                val folderMoved = moveFolderToRecycleBin(db, folderId)

                // Eğer klasör de başarıyla taşındıysa, tüm işlem başarılıdır.
                if (folderMoved) {
                    isSuccess = true
                }
            }

            if (isSuccess) {
                // Çöp kutusunu maksimum 20 kayıtla sınırla (transaction içinde olsun)
                db.execSQL("""
        DELETE FROM recycler_bin_folders
WHERE id IN (
    SELECT id FROM recycler_bin_folders
    ORDER BY deletion_date ASC
    LIMIT (
        SELECT CASE WHEN COUNT(*) > 20 THEN COUNT(*) - 20 ELSE 0 END
        FROM recycler_bin_folders
    )
)
    """.trimIndent())

                // Tüm adımlar hatasız tamamlandı, transaction'ı onayla
                db.setTransactionSuccessful()
            }

        } finally {
            // Transaction'ı sonlandır.
            // setTransactionSuccessful() çağrılmadıysa, tüm değişiklikler otomatik olarak geri alınır.
            db.endTransaction()
        }

        return isSuccess
    }

    fun restoreFolderAndItsContents(folderId: Int): Boolean {
        val db = writableDatabase
        var isSuccess = false

        // Tek bir transaction başlatıyoruz.
        db.beginTransaction()
        try {
            // 1. Adım: Klasörün içindeki ifadeleri geri dönüşüm kutusuna taşı ve sil.
            val expressionsMoved = restoreExpressionsFromRecycleBin(db, folderId)

            // 2. Adım: Sadece ilk adım başarılıysa, klasörün kendisini taşımaya devam et.
            if (expressionsMoved) {
                val folderMoved = restoreFolderFromRecycleBin(db, folderId)

                // Eğer klasör de başarıyla taşındıysa, tüm işlem başarılıdır.
                if (folderMoved) {
                    isSuccess = true
                }
            }

            if (isSuccess) {
                // Sadece tüm adımlar hatasız tamamlandığında transaction'ı onayla.
                db.setTransactionSuccessful()
            }
        } finally {
            // Transaction'ı sonlandır.
            // setTransactionSuccessful() çağrılmadıysa, tüm değişiklikler otomatik olarak geri alınır.
            db.endTransaction()
        }
        return isSuccess
    }

    private fun restoreExpressionsFromRecycleBin(db: SQLiteDatabase, folderId: Int): Boolean {
        db.query(
            "recycler_bin_expression_in_folder",
            null,
            "folder_id = ?",
            arrayOf(folderId.toString()),
            null, null, null
        )?.use { cursor ->
            if (cursor.count == 0) {
                return true
            }

            val currentTime = System.currentTimeMillis()
            while (cursor.moveToNext()) {
                val expression = cursor.getString(cursor.getColumnIndexOrThrow("expression"))
                val meaning = cursor.getString(cursor.getColumnIndexOrThrow("meaning"))
                val note = cursor.getString(cursor.getColumnIndexOrThrow("note"))
                val sentences = cursor.getString(cursor.getColumnIndexOrThrow("sentences"))
                val nextSentenceIndex = cursor.getInt(cursor.getColumnIndexOrThrow("next_sentence_index"))
                val dilKodu = cursor.getString(cursor.getColumnIndexOrThrow("language_code"))
                val folder_id = cursor.getInt(cursor.getColumnIndexOrThrow("folder_id"))
                val status = cursor.getInt(cursor.getColumnIndexOrThrow("status"))

                // 2. Hedef tablo için ContentValues'i hazırla.
                // (Buradaki "dest_column_..." isimlerini "expressions" tablonuzdaki isimlerle değiştirin)
                val valuesToInsert = ContentValues().apply {
                    put("expression", expression)
                    put("meaning", meaning)
                    put("note", note)
                    put("sentences", sentences)
                    put("next_sentence_index", nextSentenceIndex)
                    put("date_created", currentTime)
                    put("language_code", dilKodu)
                    put("folder_id", folder_id)
                    put("status", status)
                }

                // 3. Veriyi hedef tabloya ekle.
                val insertResult = db.insert("expressions", null, valuesToInsert)
                if (insertResult == -1L) {
                    Log.e("DB_ERROR", "Insert failed for expression: $expression. Values: $valuesToInsert")
                    return false // Hata anında çık.
                }
            }
        } ?: return false

        // Orijinalleri sil
        db.delete("recycler_bin_expression_in_folder", "folder_id = ?", arrayOf(folderId.toString()))
        return true
    }

    private fun restoreFolderFromRecycleBin(db: SQLiteDatabase, folderId: Int): Boolean {
        db.query(
            "recycler_bin_folders",
            null,
            "id = ?",
            arrayOf(folderId.toString()),
            null, null, null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getString(cursor.getColumnIndexOrThrow("id"))
                val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                val targetLanguage = cursor.getString(cursor.getColumnIndexOrThrow("target_language"))
                val statusAdd = cursor.getString(cursor.getColumnIndexOrThrow("status_add"))
                val statusStudy = cursor.getString(cursor.getColumnIndexOrThrow("status_study"))
                //val dateCreated = cursor.getString(cursor.getColumnIndexOrThrow("date_created"))

                // 2. Hedef tablo için ContentValues'i hazırla.
                // (Buradaki "dest_column_..." isimlerini "expressions" tablonuzdaki isimlerle değiştirin)
                val valuesToInsert = ContentValues().apply {
                    put("id", id)
                    put("name", name)
                    put("target_language", targetLanguage)
                    put("status_study", statusStudy)
                    put("status_add", statusAdd)
                    put("date_created", System.currentTimeMillis())
                }
                val insertResult = db.insert("folders", null, valuesToInsert)
                if (insertResult != -1L) {
                    db.delete("recycler_bin_folders", "id = ?", arrayOf(folderId.toString()))
                    return true // Başarılı
                }
            }
        }
        return false // Klasör bulunamadı veya ekleme başarısız oldu.
    }

    fun updateSentenceIndex(item: ExpressionData): Int {

            val sentences = item.sentences
            val sentenceCount = sentences?.size ?: 0
            var nextSentenceIndex = item.nextSentenceIndex

            nextSentenceIndex = if (nextSentenceIndex >= sentenceCount - 1) {
                0  // If the current index is at the last sentence, reset to 0
            } else {
                nextSentenceIndex + 1  // Otherwise, increment the index by 1
            }

            // Update the database with the new timestampNext and timestampLast values
            val db = writableDatabase
            val contentValues = ContentValues().apply {
                put("next_sentence_index", nextSentenceIndex)
            }

            // Update the database row with the new values where id matches the current item.id
            db.update(
                "expressions",        // Table name
                contentValues,        // New values
                "id = ?",             // Where clause
                arrayOf(item.id.toString())  // Where clause arguments (item.id is the current item's ID)
            )
            db.close()
            return nextSentenceIndex

    }

    fun saveSelectedSentences(expression: String, meaning: String, note: String, selectedSentences: List<Sentence>, folderId: Int) {

        val currentTime = System.currentTimeMillis()
        val db = writableDatabase
        val values = ContentValues().apply {
            put("expression", expression)
            put("meaning", meaning)
            put("note", note)
            put("sentences", selectedSentences.joinToString(separator = "||"))
            put("next_sentence_index", 0) // Başlangıçta ilk cümle
            put("date_created", currentTime)
            put("language_code", readLanguages(db)?.second ?: "un")
            put("folder_id", folderId)
            put("status", 0)
        }
        db.insert("expressions", null, values)
        db.close()

    }

    fun updateSelectedSentences(id: Int,
                                folderId: Int,
                                expression: String,
                                meaning: String,
                                note: String,
                                selectedSentences: MutableList<Sentence>,
                                activity: Context): Int {

        if (selectedSentences.size < 1) {
            return 1
        } else {
            val dbHelper = DatabaseHelper(activity)
            val db = dbHelper.writableDatabase

            val values = ContentValues().apply {
                put("expression", expression)
                put("meaning", meaning)
                put("note", note)
                put("sentences", selectedSentences.joinToString(separator = "||"))
                put("folder_id", folderId)
            }

            val selection = "id = ?"
            val selectionArgs = arrayOf(id.toString())

            val updatedRows = db.update("expressions", values, selection, selectionArgs)
            db.close()

            if (updatedRows > 0) {
                return 0
            } else {
                return 2
            }

        }
    }

    fun getAllFoldersByTargetLanguage(): MutableList<FolderData> {
        val folders = mutableListOf<FolderData>()
        val db = this.readableDatabase
        var cursor: Cursor? = null

        try {
            cursor = db.query(
                "folders", // Tablo adı
                null, // Getirilecek sütun(lar)
                "target_language = ?", // Selection (WHERE) - Tüm satırlar için null
                arrayOf(readLanguages(db)?.second ?: "un"), // Selection Arguments
                null, // groupBy
                null, // having
                "date_created DESC" // orderBy (ID'ye göre büyükten küçüğe sıralama)
            )

            // 'use' bloğu, işlem bittiğinde veya hata oluştuğunda cursor'ı otomatik olarak kapatır.
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    do {
                        val name = c.getString(c.getColumnIndexOrThrow("name"))
                        val id = c.getInt(c.getColumnIndexOrThrow("id"))
                        val targetLanguage = c.getString(c.getColumnIndexOrThrow("target_language"))
                        val statusAdd = c.getInt(c.getColumnIndexOrThrow("status_add"))
                        val statusStudy = c.getInt(c.getColumnIndexOrThrow("status_study"))
                        val dateCreated = c.getInt(c.getColumnIndexOrThrow("date_created"))
                        val itemCount = getExpressionCountInFolder(id)
                        val folder = FolderData(id, name, targetLanguage, statusAdd, statusStudy, dateCreated, itemCount)
                        folders.add(folder)
                    } while (c.moveToNext())
                }
            }
        } catch (e: Exception) {
            Log.e("DataBaseHelper", "Error getting all folder names", e)
        }

        return folders
    }

    fun getExpressionCountInFolder(folderId: Int): Int {
        val db = this.readableDatabase
        var count = 0L // DatabaseUtils.queryNumEntries 'Long' döndürür.

        try {
            // DatabaseUtils.queryNumEntries metodu, belirli bir 'WHERE' koşuluna uyan
            // satır sayısını saymak için optimize edilmiş ve en verimli yoldur.
            // SQL'deki "SELECT COUNT(*) FROM expressions WHERE folder = ?" sorgusuna denktir.
            count = DatabaseUtils.queryNumEntries(
                db,
                "expressions",         // Tablo adı
                "folder_id = ?",          // Seçim kriteri (WHERE clause). '?' SQL injection'ı önler.
                arrayOf(folderId.toString())    // Seçim argümanları. '?' yerine güvenli bir şekilde bu değer atanır.
            )
        } catch (e: Exception) {
            // Hata durumunda (örn: tablo bulunamadı) log kaydı oluştur ve 0 döndür.
            Log.e("DataBaseHelper", "Error counting expressions in folder id: $folderId", e)
            return 0
        }
        return count.toInt()
    }

    fun getExpressionCountInDeletedFolder(folderId: Int): Int {
        val db = this.readableDatabase
        var count = 0L // DatabaseUtils.queryNumEntries 'Long' döndürür.

        try {
            // DatabaseUtils.queryNumEntries metodu, belirli bir 'WHERE' koşuluna uyan
            // satır sayısını saymak için optimize edilmiş ve en verimli yoldur.
            // SQL'deki "SELECT COUNT(*) FROM expressions WHERE folder = ?" sorgusuna denktir.
            count = DatabaseUtils.queryNumEntries(
                db,
                "recycler_bin_expression_in_folder",         // Tablo adı
                "folder_id = ?",          // Seçim kriteri (WHERE clause). '?' SQL injection'ı önler.
                arrayOf(folderId.toString())    // Seçim argümanları. '?' yerine güvenli bir şekilde bu değer atanır.
            )
        } catch (e: Exception) {
            // Hata durumunda (örn: tablo bulunamadı) log kaydı oluştur ve 0 döndür.
            Log.e("DataBaseHelper", "Error counting expressions in folder id: $folderId", e)
            return 0
        }
        return count.toInt()
    }

    fun createFolder(name: String): Int {
        // Veritabanına yazma işlemi yapılacağı için 'writableDatabase' kullanılır.
        val db = this.writableDatabase
        val db2 = this.readableDatabase

        // Eklenecek verileri tutmak için bir ContentValues nesnesi oluşturulur.
        // Bu yöntem, SQL sorgularını manuel olarak birleştirmekten daha güvenlidir
        // ve SQL injection saldırılarını önler.
        val values = ContentValues().apply {
            put("name", name)
            put("target_language", readLanguages(db2)?.second ?: "un")
            put("status_add", 0)
            put("status_study", 0)
            put("date_created", System.currentTimeMillis())
        }

        var newRowId: Int = -1
        try {
            // db.insert() metodu, verileri tabloya ekler.
            // Başarılı olursa, yeni eklenen satırın 'id' değerini döndürür.
            // Bir hata oluşursa -1 döndürür.
            newRowId = db.insert("folders", null, values).toInt()
        } catch (e: Exception) {
            Log.e("DataBaseHelper", "Error adding new folder", e)
            return -1
        }

        // newRowId -1'den büyükse, ekleme işlemi başarılı demektir.
        return newRowId
    }

    fun renameFolder(id: Int, newName: String): Boolean {
        // Veritabanına yazma işlemi yapılacağı için 'writableDatabase' kullanılır.
        val db = this.writableDatabase

        // Güncellenecek sütun adı ve yeni değeri içeren bir ContentValues nesnesi oluşturulur.
        // Bu yöntem, SQL injection saldırılarına karşı güvenlidir.
        val values = ContentValues().apply {
            put("name", newName)
        }

        // Güncellemenin hangi satır(lar) için geçerli olacağını belirten WHERE koşulu.
        // '?' bir yer tutucudur ve değeri 'whereArgs' ile güvenli bir şekilde sağlanır.
        val whereClause = "id = ?"
        val whereArgs = arrayOf(id.toString())

        try {
            // db.update() metodu, etkilenen satır sayısını bir Int olarak döndürür.
            val rowsAffected = db.update("folders", values, whereClause, whereArgs)

            // Eğer etkilenen satır sayısı 0'dan büyükse, güncelleme başarılı demektir.
            return rowsAffected > 0
        } catch (e: Exception) {
            Log.e("DataBaseHelper", "Error renaming folder with id $id", e)
            return false
        }
    }

    fun getFolderNameById(id: Int): String {
        // Varsayılan olarak boş bir string döndürelim.
        var folderName = ""
        val db = this.readableDatabase
        var cursor: Cursor? = null

        try {
            // Sorguyu çalıştır: Sadece 'name' sütununu istiyoruz.
            cursor = db.query(
                "folders",              // Tablo adı (kendi tablonuzun adıyla değiştirin)
                arrayOf("name"),        // Getirilecek sütun(lar)
                "id = ?",               // Selection (WHERE) şartı
                arrayOf(id.toString()), // Selection argümanları
                null,                   // groupBy
                null,                   // having
                null                    // orderBy
            )

            // 'use' bloğu, işlem bitince veya hata alınca cursor'ı otomatik kapatır.
            cursor?.use { c ->
                // Eğer cursor'da en az bir satır varsa (yani ID bulunduysa)
                if (c.moveToFirst()) {
                    // "name" sütunundaki değeri al.
                    folderName = c.getString(c.getColumnIndexOrThrow("name"))
                }
            }
        } catch (e: Exception) {
            // Hata durumunda log kaydı oluştur.
            Log.e("DatabaseHelper", "Error while getting folder name by ID", e)
        }
        // 'try-catch' bloğundan sonra cursor zaten 'use' ile kapatılmış olur.
        return folderName
    }

    fun getActiveStudyFolder(): Pair<Int, String> {
        var folderId = -1
        var folderName = "default"
        val db = this.readableDatabase
        var cursor: Cursor? = null

        try {
            cursor = db.query(
                "folders",              // Tablo adı (kendi tablonuzun adıyla değiştirin)
                arrayOf("id, name"),        // Getirilecek sütun(lar)
                "status_study = 1 AND target_language = ?",               // Selection (WHERE) şartı
                arrayOf(readLanguages(db)?.second ?: "un"), // Selection argümanları
                null,                   // groupBy
                null,                   // having
                null                    // orderBy
            )

            cursor?.use { c ->
                if (c.moveToFirst()) {
                    folderId = c.getInt(c.getColumnIndexOrThrow("id"))
                    folderName = c.getString(c.getColumnIndexOrThrow("name"))
                }
            }
        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error while getting folder name by ID", e)
        }
        return Pair(folderId, folderName)
    }

    fun getActiveAddFolder(): Int {
        var folderId = -1
        val db = this.readableDatabase
        var cursor: Cursor? = null
        try {
            cursor = db.query(
                "folders",              // Tablo adı (kendi tablonuzun adıyla değiştirin)
                arrayOf("id"),        // Getirilecek sütun(lar)
                "status_add = 1 AND target_language = ?",               // Selection (WHERE) şartı
                arrayOf(readLanguages(db)?.second ?: "un"), // Selection argümanları
                null,                   // groupBy
                null,                   // having
                null                    // orderBy
            )

            cursor?.use { c ->
                if (c.moveToFirst()) {
                    folderId = c.getInt(c.getColumnIndexOrThrow("id"))
                }
            }
        } catch (e: Exception) {
            // Hata durumunda log kaydı oluştur.
            Log.e("DatabaseHelper", "Error while getting folder name by ID", e)
        }
        return folderId
    }

    fun updateActiveAddFolder(folderId: Int): Boolean {
        val db = this.writableDatabase
        val targetLanguageCode = readLanguages(db)?.second ?: "un"

        // Atomik işlem başlat: Ya iki sorgu da başarılı olur ya da ikisi de geri alınır.
        db.beginTransaction()
        try {
            // --- 1. Adım: Diğer tüm klasörleri pasif yap ---
            // 'status' sütununu '0' yapacak ContentValues nesnesi.
            val passiveValues = ContentValues().apply {
                put("status_add", "0")
            }
            // WHERE şartı: target_language eşleşmeli ama id eşleşmemeli.
            val passiveWhereClause = "target_language = ? AND id != ?"
            val passiveWhereArgs = arrayOf(targetLanguageCode, folderId.toString())

            // Güncelleme sorgusunu çalıştır.
            db.update("folders", passiveValues, passiveWhereClause, passiveWhereArgs)


            // --- 2. Adım: Seçilen klasörü aktif yap ---
            // 'status' sütununu '1' yapacak ContentValues nesnesi.
            val activeValues = ContentValues().apply {
                put("status_add", "1")
            }
            // WHERE şartı: Hem target_language hem de id eşleşmeli.
            val activeWhereClause = "target_language = ? AND id = ?"
            val activeWhereArgs = arrayOf(targetLanguageCode, folderId.toString())

            // Güncelleme sorgusunu çalıştır.
            db.update("folders", activeValues, activeWhereClause, activeWhereArgs)

            // Buraya kadar hata olmadan gelindiyse işlemi başarılı olarak işaretle.
            db.setTransactionSuccessful()
            return true

        } catch (e: Exception) {
            // Herhangi bir hata durumunda log kaydı oluştur.
            Log.e("DatabaseHelper", "Error updating active folder", e)
            return false
        } finally {
            // Transaction'ı bitir. setTransactionSuccessful çağrıldıysa değişiklikler kaydedilir,
            // çağrılmadıysa (hata oluştuysa) geri alınır.
            db.endTransaction()
        }
    }

    fun updateActiveStudyFolder(folderId: Int): Boolean {
        val db = this.writableDatabase
        val targetLanguageCode = readLanguages(db)?.second ?: "un"

        // Atomik işlem başlat: Ya iki sorgu da başarılı olur ya da ikisi de geri alınır.
        db.beginTransaction()
        try {
            // --- 1. Adım: Diğer tüm klasörleri pasif yap ---
            // 'status' sütununu '0' yapacak ContentValues nesnesi.
            val passiveValues = ContentValues().apply {
                put("status_study", "0")
            }
            // WHERE şartı: target_language eşleşmeli ama id eşleşmemeli.
            val passiveWhereClause = "target_language = ? AND id != ?"
            val passiveWhereArgs = arrayOf(targetLanguageCode, folderId.toString())

            // Güncelleme sorgusunu çalıştır.
            db.update("folders", passiveValues, passiveWhereClause, passiveWhereArgs)


            // --- 2. Adım: Seçilen klasörü aktif yap ---
            // 'status' sütununu '1' yapacak ContentValues nesnesi.
            val activeValues = ContentValues().apply {
                put("status_study", "1")
            }
            // WHERE şartı: Hem target_language hem de id eşleşmeli.
            val activeWhereClause = "target_language = ? AND id = ?"
            val activeWhereArgs = arrayOf(targetLanguageCode, folderId.toString())

            // Güncelleme sorgusunu çalıştır.
            db.update("folders", activeValues, activeWhereClause, activeWhereArgs)

            // Buraya kadar hata olmadan gelindiyse işlemi başarılı olarak işaretle.
            db.setTransactionSuccessful()
            return true

        } catch (e: Exception) {
            // Herhangi bir hata durumunda log kaydı oluştur.
            Log.e("DatabaseHelper", "Error updating active folder", e)
            return false
        } finally {
            // Transaction'ı bitir. setTransactionSuccessful çağrıldıysa değişiklikler kaydedilir,
            // çağrılmadıysa (hata oluştuysa) geri alınır.
            db.endTransaction()
        }
    }

    fun updateExpressionStatus(item: ExpressionData): Int {
        // Veritabanına yazma işlemi yapacağımız için writableDatabase kullanıyoruz.
        val db = this.writableDatabase
        var affectedRows = -1

        try {
            // Güncellenecek değerleri tutan nesne: SET status = 1
            val values = ContentValues().apply {
                put("status", 1)
            }

            // Güncelleme şartı: WHERE id = ?
            val whereClause = "id = ?"
            val whereArgs = arrayOf(item.id.toString())

            // Veritabanı güncelleme komutunu çalıştır.
            // Bu metod, etkilenen satır sayısını döndürür.
            affectedRows = db.update("expressions", values, whereClause, whereArgs)

        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error updating expression status", e)
        }

        // 'writableDatabase' genellikle helper'ın yaşam döngüsü tarafından yönetilir,
        // bu yüzden db.close() burada genellikle gerekli değildir.

        return affectedRows
    }

    fun resetExpressionsStatusByFolder(folderId: Int) {
        // Veritabanına yazma işlemi yapacağımız için writableDatabase kullanıyoruz.
        val db = this.writableDatabase

        try {
            // Güncellenecek değer: SET status = 0
            val values = ContentValues().apply {
                put("status", 0)
            }

            // Güncelleme şartı: WHERE folder_id = ?
            // 'expressions' tablonuzdaki yabancı anahtar sütununun adının
            // 'folder_id' olduğunu varsayıyorum. Farklıysa burayı güncelleyin.
            val whereClause = "folder_id = ?"
            val whereArgs = arrayOf(folderId.toString())

            // Veritabanı güncelleme komutunu çalıştır.
            // Bu komut, belirtilen klasöre ait tüm satırları güncelleyecektir.
            db.update("expressions", values, whereClause, whereArgs)

        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error resetting statuses for folder ID: $folderId", e)
        }
        // 'writableDatabase' genellikle helper'ın yaşam döngüsü tarafından yönetilir.
    }

}
