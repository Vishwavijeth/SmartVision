
package com.example.application.fragments

import android.annotation.SuppressLint
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.application.OverlayView_is
import androidx.viewbinding.ViewBinding
import com.example.application.databinding.ItemColorLabelsBinding

class ColorLabelsAdapter : RecyclerView.Adapter<ColorLabelsAdapter.ViewHolder>() {
    private var coloredLabels: List<OverlayView_is.ColorLabel> = emptyList()

    @SuppressLint("NotifyDataSetChanged")
    fun updateResultLabels(coloredLabels: List<OverlayView_is.ColorLabel>) {
        this.coloredLabels = coloredLabels
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ItemColorLabelsBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(label: String, rgbColor: Int) {
            with(binding) {
                tvLabel.text = label
                val drawable = flBackgroundLabel.background.mutate() as GradientDrawable
                drawable.setColor(rgbColor)
                drawable.invalidateSelf()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemColorLabelsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        coloredLabels[position].let {
            holder.bind(it.label, it.getColor())
        }
    }

    override fun getItemCount(): Int = coloredLabels.size
}
