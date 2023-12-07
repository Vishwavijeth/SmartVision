package com.example.application

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.application.fragments.ModelnameAlgo

class ModelAdapter(private val dataList: ArrayList<ModelnameAlgo>): RecyclerView.Adapter<ModelAdapter.ViewHolderClass>()
{

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderClass
    {
        val itemview = LayoutInflater.from(parent.context).inflate(R.layout.custom_textview, parent, false)
        return ViewHolderClass(itemview)
    }

    override fun getItemCount(): Int {
        TODO("Not yet implemented")
    }

    override fun onBindViewHolder(holder: ViewHolderClass, position: Int)
    {
        val currentItem = dataList[position]
        //holder.algoImage.setImageResource(currentItem)
    }

    class ViewHolderClass(itemView: View): RecyclerView.ViewHolder(itemView)
    {
        val algoImage: ImageView = itemView.findViewById(R.id.algoimage)
        val modelname: TextView = itemView.findViewById(R.id.customModelTextview)
        val algotype: TextView = itemView.findViewById(R.id.algotype)
    }
}