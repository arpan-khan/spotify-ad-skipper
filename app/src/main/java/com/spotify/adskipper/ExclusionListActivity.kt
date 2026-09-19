package com.spotify.adskipper

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ExclusionListActivity : AppCompatActivity() {

    private data class AppEntry(
        val packageName: String,
        val label: String,
        val icon: Drawable
    )

    private lateinit var allApps: MutableList<AppEntry>
    private lateinit var visibleApps: MutableList<AppEntry>
    private lateinit var excludedPackages: MutableSet<String>
    private lateinit var adapter: AppListAdapter
    private var query: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exclusion_list)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        excludedPackages = HashSet(ExclusionPreferences.getExcludedPackages(this))
        allApps = loadInstallableApps().toMutableList()
        sortApps()
        visibleApps = allApps.toMutableList()

        adapter = AppListAdapter()
        findViewById<ListView>(R.id.lvApps).adapter = adapter

        findViewById<EditText>(R.id.etSearch).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                query = s?.toString().orEmpty()
                refreshVisibleApps()
            }
        })
    }

    private fun sortApps() {
        allApps.sortWith(compareByDescending<AppEntry> { excludedPackages.contains(it.packageName) }
            .thenBy { it.label.lowercase() })
    }

    private fun refreshVisibleApps() {
        visibleApps = if (query.isBlank()) {
            allApps.toMutableList()
        } else {
            allApps.filter { it.label.contains(query, ignoreCase = true) }.toMutableList()
        }
        adapter.notifyDataSetChanged()
    }

    private fun loadInstallableApps(): List<AppEntry> {
        val pm = packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfos = pm.queryIntentActivities(launcherIntent, 0)

        return resolveInfos
            .map { it.activityInfo.applicationInfo }
            .distinctBy { it.packageName }
            .filter { it.packageName != packageName }
            .filter { it.packageName != SPOTIFY_PACKAGE }
            .map { appInfo ->
                AppEntry(
                    packageName = appInfo.packageName,
                    label = appInfo.loadLabel(pm).toString(),
                    icon = appInfo.loadIcon(pm)
                )
            }
    }

    private inner class AppListAdapter : BaseAdapter() {

        override fun getCount(): Int = visibleApps.size

        override fun getItem(position: Int): AppEntry = visibleApps[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(this@ExclusionListActivity)
                .inflate(R.layout.item_exclusion_app, parent, false)

            val entry = visibleApps[position]

            view.findViewById<ImageView>(R.id.ivAppIcon).setImageDrawable(entry.icon)
            view.findViewById<TextView>(R.id.tvAppName).text = entry.label

            val checkBox = view.findViewById<CheckBox>(R.id.cbExcluded)
            checkBox.setOnCheckedChangeListener(null)
            checkBox.isChecked = excludedPackages.contains(entry.packageName)
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    excludedPackages.add(entry.packageName)
                } else {
                    excludedPackages.remove(entry.packageName)
                }
                ExclusionPreferences.setExcludedPackages(this@ExclusionListActivity, excludedPackages)
                sortApps()
                refreshVisibleApps()
            }

            view.setOnClickListener {
                checkBox.isChecked = !checkBox.isChecked
            }

            return view
        }
    }

    private companion object {
        const val SPOTIFY_PACKAGE = "com.spotify.music"
    }
}
