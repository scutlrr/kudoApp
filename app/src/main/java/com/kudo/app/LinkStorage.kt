package com.kudo.app

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class LinkStorage(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("kudo_links", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LINKS = "links"
        private const val KEY_NEXT_ID = "next_id"
    }

    fun saveLink(url: String, title: String? = null): LinkItem {
        val links = getLinks().toMutableList()
        val id = prefs.getLong(KEY_NEXT_ID, 1L)
        val item = LinkItem(
            id = id,
            url = url,
            title = title,
            timestamp = System.currentTimeMillis()
        )
        links.add(0, item)
        persistLinks(links)
        prefs.edit().putLong(KEY_NEXT_ID, id + 1).apply()
        return item
    }

    fun getLinks(): List<LinkItem> {
        val json = prefs.getString(KEY_LINKS, "[]") ?: "[]"
        val array = JSONArray(json)
        val list = mutableListOf<LinkItem>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                LinkItem(
                    id = obj.getLong("id"),
                    url = obj.getString("url"),
                    title = obj.optString("title", null),
                    timestamp = obj.getLong("timestamp")
                )
            )
        }
        return list
    }

    fun deleteLink(id: Long) {
        val links = getLinks().toMutableList()
        links.removeAll { it.id == id }
        persistLinks(links)
    }

    fun clearAll() {
        prefs.edit().putString(KEY_LINKS, "[]").apply()
    }

    private fun persistLinks(links: List<LinkItem>) {
        val array = JSONArray()
        links.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("url", item.url)
                put("title", item.title ?: "")
                put("timestamp", item.timestamp)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_LINKS, array.toString()).apply()
    }
}
