package com.transist.ui.main.addedit

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.transist.R
import com.transist.data.remote.response.Sentence

class SentencesAdapter(
    private val sentences: MutableList<Sentence>,
    private val button: Button,
    private val activity: Activity
) : RecyclerView.Adapter<SentencesAdapter.ViewHolder>() {

    private val checkedStates = MutableList(sentences.size) { true }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val text: TextView = itemView.findViewById(R.id.text)
        val remove: ImageButton = itemView.findViewById(R.id.remove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_sentence, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val pos = position
        val sentence = sentences[pos]

        holder.text.text = "${sentence.translation} (${sentence.sentence})"

        holder.remove.setOnClickListener{
            sentences.removeAt(pos)
            // RecyclerView'ı bilgilendir
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, sentences.size) // Kalan pozisyonları güncelle
        }

    }

    // Butonun arka plan rengini güncelleyen fonksiyon
    private fun updateButtonBackground() {
        val selectedCount = getSelectedIndices().size
        if (selectedCount >= 5) {
            button.setBackgroundColor(ContextCompat.getColor(activity, R.color.buton_aktif))
            //button.isEnabled = true
        } else {
            button.setBackgroundColor(ContextCompat.getColor(activity, R.color.buton_inaktif))
            //button.isEnabled = false
        }
    }

    fun updateItem(position: Int, newSentence: Sentence) {

    }

    fun getSelectedIndices(): List<Int> {
        return checkedStates.mapIndexedNotNull { index, isChecked ->
            if (isChecked) index else null
        }
    }

    fun getSelectedSentences(): MutableList<Sentence> {
        return sentences
    }

    override fun getItemCount(): Int {
        return sentences.size
    }
}
