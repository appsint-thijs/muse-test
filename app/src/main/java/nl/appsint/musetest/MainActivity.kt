package nl.appsint.musetest

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateUtils
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.customListAdapter
import com.choosemuse.libmuse.*
import com.choosemuse.libmuse.ConnectionState.*
import nl.appsint.musetest.databinding.ActivityMainBinding
import java.lang.ref.WeakReference

@SuppressLint("SetTextI18n")
class MainActivity : PermissionActivity() {
    private lateinit var museManager: MuseManagerAndroid
    private lateinit var views: ActivityMainBinding
    private lateinit var dialog: MaterialDialog

    private val museListAdapter = MuseListAdapter()
    private var connectTimestamp: Long? = null
    private var disconnectTimestamp: Long? = null

    private val threadHandler = Handler(Looper.getMainLooper())
    private val timer = Timer(WeakReference(this))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        views = ActivityMainBinding.inflate(layoutInflater)
        setContentView(views.root)

        views.log.movementMethod = ScrollingMovementMethod()
        dialog = MaterialDialog(this).show {
            customListAdapter(museListAdapter)
            title(R.string.select_muse)
            cancelable(false)
        }

        timer.schedule()
    }

    override fun onDestroy() {
        super.onDestroy()
        timer.cancel()
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
        views.selectedMuse.text = "Muse: ${muse.macAddress}"

        muse.unregisterAllListeners()
        muse.registerConnectionListener(object : MuseConnectionListener() {
            override fun receiveMuseConnectionPacket(packet: MuseConnectionPacket?, muse: Muse?) {
                appendLog("Changed connection state to ${packet?.currentConnectionState} (was ${packet?.previousConnectionState})")
                views.connectionState.text = "Connection state: ${packet?.currentConnectionState}"

                if (packet?.currentConnectionState == CONNECTED && connectTimestamp == null) {
                    connectTimestamp = System.currentTimeMillis()
                }

                if (packet?.currentConnectionState == DISCONNECTED && disconnectTimestamp == null) {
                    disconnectTimestamp = System.currentTimeMillis()
                }
            }
        })

        muse.registerErrorListener(object : MuseErrorListener() {
            override fun receiveError(error: Error?) {
                appendLog("Received error: ${error?.info} [Code: ${error?.code}] [Type: ${error?.type}]")
            }
        })

        appendLog("Starting to run muse async")
        muse.runAsynchronously()
        dialog.dismiss()
    }

    private fun appendLog(text: String) {
        views.log.append("\n$text")
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

    private class Timer(private val activity: WeakReference<MainActivity>): Runnable {
        override fun run() {
            activity.get()?.apply {
                connectTimestamp?.let {
                    val endTime = disconnectTimestamp ?: System.currentTimeMillis()
                    val value = (endTime - it) / 1000L
                    views.timer.text = "Connection timer: ${DateUtils.formatElapsedTime(value)}"
                }
            }

            schedule()
        }

        fun schedule() {
            activity.get()?.threadHandler?.postDelayed(this, 1000)
        }

        fun cancel() {
            activity.get()?.threadHandler?.removeCallbacks(this)
        }
    }
}
