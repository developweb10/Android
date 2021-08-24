package com.app.tourguide.ui.mapBox

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.app.tourguide.R
import com.app.tourguide.listeners.onItemClickedListener
import com.app.tourguide.ui.mapBox.response.DataItem
import com.squareup.picasso.Callback
import com.squareup.picasso.NetworkPolicy
import com.squareup.picasso.Picasso
import com.tonyodev.fetch2.Download
import com.tonyodev.fetch2.Status
import kotlinx.android.synthetic.main.dialog_spot_detail.view.*
import java.io.File
import java.util.*


class SpotListAdapter(internal var context: Context, internal var mRegionRegionSpot: List<DataItem>, val playVideo: (String) -> Unit) : RecyclerView.Adapter<SpotListAdapter.SpotListViewHolder>() {


    private lateinit var listener: onItemClickedListener
    // private lateinit var actionListener: ActionListener
    fun onItemClickedListener(listener: onItemClickedListener) {
        this.listener = listener
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SpotListViewHolder {
        //inflate the layout file
        val view = LayoutInflater.from(parent.context).inflate(R.layout.dialog_spot_detail, parent, false) as ConstraintLayout
        return SpotListViewHolder(view)
    }

    override fun onBindViewHolder(holder: SpotListViewHolder, position: Int) {
        try {
            holder.layout.tvSpotTitle.text = mRegionRegionSpot[position].spotName
            /*Picasso.get()
                    .load(mRegionRegionSpot[position].vThumbnail)
                    .networkPolicy(NetworkPolicy.OFFLINE)
                    .into(holder.layout.ivSpotImage, object : Callback {
                        override fun onSuccess() {
                        }

                        override fun onError(e: Exception?) {
                            Picasso.get().load(mRegionRegionSpot[position].vThumbnail)
                                    .into(holder.layout.ivSpotImage, object : Callback {
                                        override fun onSuccess() {

                                        }

                                        override fun onError(e: Exception?) {
                                            print("Couldn't fetch data")
                                        }
                                    })
                        }
                    })*/

            val imgFile = File(getImageUri(position).toString())
            if (imgFile.exists()) {
                holder.layout.ivSpotImage.setImageBitmap(BitmapFactory.decodeFile(imgFile.absolutePath))
            }

            holder.layout.ivSpotImage.setOnClickListener {
                val fileName = mRegionRegionSpot[position].sVideo!!.substring(mRegionRegionSpot[position].sVideo!!.lastIndexOf('/') + 1)
                val path = Environment.getExternalStorageDirectory().absolutePath + File.separator + "TourGuide/$fileName"
                val file = File(path)
                var uri1: Uri? = null
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    uri1 = Uri.parse(file.path)
                } else {
                    uri1 = Uri.fromFile(file)
                }
                //playVideo(uri1.toString())
                listener.onItemClickListener(position, uri1.toString())
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }


    }


    override fun getItemCount(): Int {
        return mRegionRegionSpot.size
    }

    class SpotListViewHolder(val layout: ConstraintLayout) : RecyclerView.ViewHolder(layout)


    /**
     * Method to return file uri
     */
    private fun getImageUri(position: Int): Uri {
        val fileName = mRegionRegionSpot[position].vThumbnail!!.substring(mRegionRegionSpot[position].vThumbnail!!.lastIndexOf('/') + 1)
        val path = Environment.getExternalStorageDirectory().absolutePath + File.separator + "TourGuide/Images/$fileName"
        val file = File(path)
        val uri: Uri?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            uri = Uri.parse(file.path)
        } else {
            uri = Uri.fromFile(file)
        }
        return uri
    }
}