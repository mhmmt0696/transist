package com.transist.ui.main.addedit

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.transist.R
import com.transist.data.model.FolderData

class FoldersAdapterSelectOne(context: Context, folders: List<FolderData>) :
    ArrayAdapter<FolderData>(context, 0, folders) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = LayoutInflater.from(context)
        val view = convertView ?: inflater.inflate(android.R.layout.simple_list_item_2, parent, false)

        val folder = getItem(position) ?: return view

        view.findViewById<TextView>(android.R.id.text1).text = folder.name
        val count = folder.itemCount
        view.findViewById<TextView>(android.R.id.text2).text = context.getString(R.string.item_count, count.toString())

        return view
    }
}
