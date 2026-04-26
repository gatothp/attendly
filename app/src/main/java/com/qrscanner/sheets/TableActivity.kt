package com.qrscanner.sheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.qrscanner.sheets.databinding.ActivityTableBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TableActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTableBinding
    private val db by lazy { ScanDatabase(applicationContext) }
    private val records = mutableListOf<ScanRecord>()
    private lateinit var adapter: ScanAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTableBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        adapter = ScanAdapter(records)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.btnUpload.setOnClickListener { uploadAll() }
        loadRecords()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun loadRecords() {
        val all = db.getAll()
        records.clear()
        records.addAll(all)
        adapter.notifyDataSetChanged()
        updateEmptyState()
    }

    private fun updateEmptyState() {
        val empty = records.isEmpty()
        binding.tvEmpty.visibility = if (empty) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (empty) View.GONE else View.VISIBLE
        binding.btnUpload.isEnabled = !empty
        binding.tvCount.text = if (empty) "" else "${records.size} record(s)"
    }

    private fun uploadAll() {
        val scriptUrl = prefs().getString("script_url", "").orEmpty()
        val sheetName = prefs().getString("sheet_name", "Sheet1").orEmpty()

        if (scriptUrl.isEmpty()) {
            Toast.makeText(this, getString(R.string.msg_configure_sheet), Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnUpload.isEnabled = false
        binding.tvStatus.text = getString(R.string.label_uploading)

        val snapshot = records.toList()

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                SheetsHelper.appendRows(scriptUrl, sheetName, snapshot)
            }

            result.onSuccess {
                withContext(Dispatchers.IO) { db.deleteAll() }
                loadRecords()
                binding.tvStatus.text = getString(R.string.label_upload_done)
                Toast.makeText(
                    this@TableActivity,
                    getString(R.string.msg_upload_success),
                    Toast.LENGTH_SHORT
                ).show()
            }.onFailure { e ->
                binding.btnUpload.isEnabled = records.isNotEmpty()
                binding.tvStatus.text = "Error: ${e.message}"
                Toast.makeText(
                    this@TableActivity,
                    "Upload failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun prefs() = getSharedPreferences("prefs", MODE_PRIVATE)
}

// ---------------------------------------------------------------------------
// Adapter
// ---------------------------------------------------------------------------

class ScanAdapter(private val items: List<ScanRecord>) :
    RecyclerView.Adapter<ScanAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvRowNum: TextView = view.findViewById(R.id.tvRowNum)
        val tvTimestamp: TextView = view.findViewById(R.id.tvTimestamp)
        val tvId: TextView = view.findViewById(R.id.tvId)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_scan_row, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvRowNum.text = (position + 1).toString()
        holder.tvTimestamp.text = item.timestamp
        holder.tvId.text = item.scannedId
        val bgColor = if (position % 2 == 0)
            holder.itemView.context.getColor(R.color.tableRowEven)
        else
            holder.itemView.context.getColor(R.color.tableRowOdd)
        holder.itemView.setBackgroundColor(bgColor)
    }

    override fun getItemCount() = items.size
}
