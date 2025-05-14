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
import com.transist.data.model.FolderData
import com.transist.R

class FoldersAdapter (
    private val context: Context,
    private val onItemClicked: (FolderData) -> Unit,
    private val onDeleteClicked: (Int) -> Boolean,
    private val onEditClicked: (FolderData) -> Unit
)
    : ListAdapter<FolderData, FoldersAdapter.FolderViewHolder>(FolderDiffCallback()) {

    private var selectedPosition: Int = RecyclerView.NO_POSITION // Seçili satırı takip eder

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_folder, parent, false)
        return FolderViewHolder(view)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        val folder = getItem(position) // currentList ile güncel listeyi alıyoruz
        holder.bind(folder, onItemClicked)
    }

    override fun getItemCount(): Int = currentList.size // currentList ile güncel listeyi alıyoruz

    inner class FolderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textViewFolderName: TextView = itemView.findViewById(R.id.textViewExpression)
        private val textViewItemCount: TextView = itemView.findViewById(R.id.textViewSpace)
        private val buttonEdit: ImageButton = itemView.findViewById(R.id.buttonEdit)
        private val buttonDelete: ImageButton = itemView.findViewById(R.id.buttonDelete)
        private val buttonMore: ImageButton = itemView.findViewById(R.id.button_more)

        fun bind(folder: FolderData, onItemClicked: (FolderData) -> Unit) {
            // klasör adı değiştirme, klasör silme işlemleri yapılacak
            // klasör eleman sayısı bul ve yaz

            val folderName = folder.name[0].uppercaseChar() + folder.name.substring(1)
            textViewFolderName.text = folderName
            textViewItemCount.text = context.getString(R.string.item_count, folder.itemCount.toString())

            // Button görünürlüklerini seçili duruma göre ayarla
            if (bindingAdapterPosition == selectedPosition) {
                buttonEdit.visibility = View.VISIBLE
                buttonDelete.visibility = View.VISIBLE
            } else {
                buttonEdit.visibility = View.GONE
                buttonDelete.visibility = View.GONE
            }

            // Satırın tamamına tıklama dinleyicisi ekle
            itemView.setOnClickListener {
                // Tıklanan öğenin verisiyle birlikte lambda fonksiyonunu çağır
                onItemClicked(folder)
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

            buttonEdit.setOnClickListener {
                onEditClicked(folder)
            }

            buttonDelete.setOnClickListener {
                val position = bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return@setOnClickListener

                val success = onDeleteClicked(folder.id)

                if (success) {
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
}

class FolderDiffCallback : DiffUtil.ItemCallback<FolderData>() {

    override fun areItemsTheSame(oldItem: FolderData, newItem: FolderData): Boolean {
        // Öğelerin benzersiz ID'lerini karşılaştır
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: FolderData, newItem: FolderData): Boolean {
        // İçeriğin aynı olup olmadığını karşılaştır
        return oldItem == newItem
    }
}
