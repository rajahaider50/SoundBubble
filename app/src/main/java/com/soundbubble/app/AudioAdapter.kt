package com.soundbubble.app

import android.media.MediaPlayer
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.soundbubble.app.databinding.ItemAudioBinding
import java.io.File

class AudioAdapter(
    private var files: List<File>,
    private val onDelete: (File) -> Unit
) : RecyclerView.Adapter<AudioAdapter.VH>() {

    private var player: MediaPlayer? = null

    inner class VH(val binding: ItemAudioBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemAudioBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val file = files[position]
        holder.binding.tvName.text = file.name

        holder.binding.btnPlay.setOnClickListener {
            playFile(file, holder.itemView.context)
        }

        holder.binding.btnDelete.setOnClickListener {
            onDelete(file)
        }
    }

    private fun playFile(file: File, context: android.content.Context) {
        player?.release()
        player = AudioRouter.play(context, file)
    }

    override fun getItemCount() = files.size

    fun updateData(newFiles: List<File>) {
        files = newFiles
        notifyDataSetChanged()
    }

    fun release() {
        player?.release()
        player = null
    }
}
