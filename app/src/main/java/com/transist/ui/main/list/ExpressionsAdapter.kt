package com.transist.ui.main.list

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.transist.data.model.ExpressionData
import com.transist.R
import com.transist.util.capitalizeFirstLetter

class ExpressionsAdapter(
    private val context: Context,
    private val onEditClick: (ExpressionData) -> Unit,
    private val onDeleteClick: (ExpressionData) -> Unit
) : ListAdapter<ExpressionData, ExpressionsAdapter.ExpressionViewHolder>(
        ExpressionDiffCallback()
    ) {

    private var selectedPosition: Int = RecyclerView.NO_POSITION // Seçili satırı takip eder

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpressionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expression, parent, false)
        return ExpressionViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExpressionViewHolder, position: Int) {
        val expression = getItem(position)
        holder.bind(expression)
    }

    inner class ExpressionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewExpression: TextView = itemView.findViewById(R.id.textViewExpression)
        private val textViewSpace: TextView = itemView.findViewById(R.id.textViewSpace)
        private val buttonEdit: ImageButton = itemView.findViewById(R.id.buttonEdit)
        private val buttonDelete: ImageButton = itemView.findViewById(R.id.buttonDelete)
        private val buttonMore: ImageButton = itemView.findViewById(R.id.button_more)

        fun bind(expression: ExpressionData) {
            textViewExpression.text = expression.expression.replaceFirstChar { it.uppercase() }

            textViewSpace.text = context.getString(R.string.folder_selected, expression.folderName.capitalizeFirstLetter())

            // Button görünürlüklerini seçili duruma göre ayarla
            if (bindingAdapterPosition == selectedPosition) {
                buttonEdit.visibility = View.VISIBLE
                buttonDelete.visibility = View.VISIBLE
            } else {
                buttonEdit.visibility = View.GONE
                buttonDelete.visibility = View.GONE
            }

            buttonMore.setOnClickListener {
                val previousPosition = selectedPosition
                selectedPosition = if (bindingAdapterPosition == selectedPosition) {
                    RecyclerView.NO_POSITION // Aynı satıra tıklanırsa seçimi kaldır
                } else {
                    bindingAdapterPosition // Yeni seçili satır
                }

                // Önceki ve yeni pozisyonları güncelle
                notifyItemChanged(previousPosition)
                notifyItemChanged(selectedPosition)
            }

            buttonEdit.setOnClickListener { onEditClick(expression) }

            buttonDelete.setOnClickListener {
                val position = bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return@setOnClickListener

                onDeleteClick(expression)

                // Eğer silinen öğe seçili öğeyse, seçimi sıfırla
                if (position == selectedPosition) {
                    selectedPosition = RecyclerView.NO_POSITION
                } else if (position < selectedPosition) {
                    selectedPosition -= 1
                }

            }

        }
    }
}

// 2. DiffUtil.ItemCallback'i oluşturun
class ExpressionDiffCallback : DiffUtil.ItemCallback<ExpressionData>() {
    override fun areItemsTheSame(oldItem: ExpressionData, newItem: ExpressionData): Boolean {
        // Öğelerin benzersiz ID'lerini kontrol et
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: ExpressionData, newItem: ExpressionData): Boolean {
        // ID'ler aynıysa, içeriklerini kontrol et
        return oldItem == newItem
    }
}