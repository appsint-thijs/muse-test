package nl.appsint.musetest

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.customListAdapter
import com.choosemuse.libmuse.Muse
import com.choosemuse.libmuse.MuseListener
import com.choosemuse.libmuse.MuseManagerAndroid
import nl.appsint.musetest.databinding.ActivityMainBinding

class MainActivity : PermissionActivity() {
    private lateinit var museManager: MuseManagerAndroid
    private lateinit var views: ActivityMainBinding
    private lateinit var dialog: MaterialDialog

    private val museListAdapter = MuseListAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        views = ActivityMainBinding.inflate(layoutInflater)
        setContentView(views.root)

        dialog = MaterialDialog(this).show {
            customListAdapter(museListAdapter)
            title(R.string.select_muse)
            cancelable(false)
        }
    }

    override fun onPermissionsInitialized() {
        initializeMuse()
    }

    private fun initializeMuse() {
        museManager = MuseManagerAndroid.getInstance()
        museManager.setContext(this)
        museManager.setMuseListener(object : MuseListener() {
            override fun museListChanged() {
                museListAdapter.muses = museManager.muses
            }
        })

        museManager.startListening()
        dialog.show()
    }

    private fun onMuseSelected(muse: Muse) {
        dialog.dismiss()
    }

    private inner class MuseListItem(parent: ViewGroup): RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_item_muse, parent, false)) {
        fun bind(muse: Muse) {
            itemView.findViewById<TextView>(R.id.name).text = muse.name
            itemView.findViewById<TextView>(R.id.mac).text = muse.macAddress
            itemView.setOnClickListener {
                onMuseSelected(muse)
            }
        }
    }

    private inner class MuseListAdapter: RecyclerView.Adapter<MuseListItem>() {
        var muses: ArrayList<Muse>? = null
            @SuppressLint("NotifyDataSetChanged")
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MuseListItem {
            return MuseListItem(parent)
        }

        override fun onBindViewHolder(holder: MuseListItem, position: Int) {
            muses?.get(position)?.let { muse ->
                holder.bind(muse)
            }
        }

        override fun getItemCount(): Int {
            return muses?.size ?: 0
        }
    }
}