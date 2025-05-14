package com.transist.ui.main.list

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.transist.data.model.FolderData
import com.transist.R

class FolderRecyclerAdapter(
    private val context: Context,
    folders: MutableList<FolderData>
) : ArrayAdapter<FolderData>(context, 0, folders) {

    // 1. ViewHolder Sınıfını Oluşturun
    // Bu sınıf, item_list_view.xml içindeki view'ları referans olarak tutar.
    private class ViewHolder {
        lateinit var textView1: TextView
        lateinit var textView2: TextView
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val holder: ViewHolder

        // 2. convertView'i kontrol edin (View Geri Dönüşümü)
        if (convertView == null) {
            // Eğer view daha önce oluşturulmamışsa, XML'i inflate et (şişir)
            view = LayoutInflater.from(context).inflate(R.layout.item_list_view, parent, false)

            // Yeni bir ViewHolder oluştur
            holder = ViewHolder()

            // ViewHolder içindeki view'ları ID ile sadece bir kez bul
            holder.textView1 = view.findViewById(R.id.text_view_title)
            holder.textView2 = view.findViewById(R.id.text_view_subtitle)

            // ViewHolder'ı view'a bir "etiket" (tag) olarak ekle
            view.tag = holder
        } else {
            // Eğer view daha önce oluşturulmuşsa, onu ve etiketindeki ViewHolder'ı tekrar kullan
            view = convertView
            holder = convertView.tag as ViewHolder
        }

        // 3. Veriyi Alın ve View'lara Güvenli Bir Şekilde Atayın
        val folder = getItem(position)

        folder?.let { data ->
            // Veriyi ViewHolder'daki TextView'lara ata
            holder.textView1.text = data.name

            holder.textView2.text = context.getString(R.string.item_count, folder.itemCount.toString())
        }

        return view
    }
}
