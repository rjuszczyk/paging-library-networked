package com.example.radek.movielist.adapter

import android.annotation.SuppressLint
import android.support.v7.util.DiffUtil
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import com.example.radek.R
import com.example.radek.movielist.NetResult

@SuppressLint("SetTextI18n")
class MyPagedListAdapter(
        retryListener: RetryListener,
        diffCallback: DiffUtil.ItemCallback<NetResult>
) : AbsPagedListAdapter<NetResult>(retryListener, diffCallback) {

    override fun createProgressViewHolder(parent: ViewGroup): ProgressViewHolder {
        val tv = ProgressBar(parent.context)
        val height = (parent.context.resources.displayMetrics.density*50).toInt()
        tv.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
        return ProgressViewHolder(tv)
    }

    override fun createFailedViewHolder(parent: ViewGroup,retryListener: RetryListener): FailedViewHolder<NetResult> {
        val tv = Button(parent.context)
        val height = (parent.context.resources.displayMetrics.density*50).toInt()
        tv.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height)
        return MyFailedViewHolder(tv, retryListener)
    }

    override fun createItemViewHolder(parent: ViewGroup): ItemViewHolder<NetResult> {

        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_movie, parent, false)

        return MyItemViewHolder(view )
    }

    class MyItemViewHolder(itemView: View) : ItemViewHolder<NetResult>(itemView) {
        private val title:TextView = itemView.findViewById(R.id.title)
        private val voteCount:TextView = itemView.findViewById(R.id.vote_count)
        private val voteAverage:TextView = itemView.findViewById(R.id.vote_average)
        private val releaseDate:TextView = itemView.findViewById(R.id.release_date)

        override fun bind(item: NetResult?) {
            if (item != null) {
                title.text = item.title
                voteCount.text = item.voteCount
                voteAverage.text = item.voteAverage
                releaseDate.text = item.releaseDate
            } else {
                title.text = "loading"
                voteCount.text = ""
                voteAverage.text = ""
                releaseDate.text = ""
            }
        }
    }

    class MyFailedViewHolder(
            itemView: View,
            retryListener: RetryListener
    ) : FailedViewHolder<NetResult>(itemView) {

        init {
            itemView.setOnClickListener({retryListener.retryCalled()})
        }

        override fun bind(cause: Throwable) {
            (itemView as TextView).text = cause.message
        }
    }
}