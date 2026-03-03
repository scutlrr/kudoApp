package com.kudo.app

import android.content.ContentResolver
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

object ApiClient {

    private const val BASE_URL = "http://43.153.24.111:8900"
    private val client = OkHttpClient()
    private val gson = Gson()

    // ========== Links ==========

    suspend fun createLink(url: String, title: String? = null): LinkItem = withContext(Dispatchers.IO) {
        val json = gson.toJson(mapOf("url" to url, "title" to (title ?: "")))
        val body = json.toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url("$BASE_URL/api/links").post(body).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw IOException("API error: ${response.code}")
        gson.fromJson(response.body?.string(), LinkItem::class.java)
    }

    suspend fun getLinks(): List<LinkItem> = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$BASE_URL/api/links").get().build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw IOException("API error: ${response.code}")
        val type = object : TypeToken<List<LinkItem>>() {}.type
        gson.fromJson(response.body?.string(), type)
    }

    suspend fun deleteLink(linkId: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$BASE_URL/api/links/$linkId").delete().build()
        client.newCall(request).execute()
    }

    suspend fun clearLinks() = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$BASE_URL/api/links").delete().build()
        client.newCall(request).execute()
    }

    // ========== Documents ==========

    suspend fun uploadDocument(
        contentResolver: ContentResolver,
        uri: Uri,
        fileName: String,
        mimeType: String
    ): DocItem = withContext(Dispatchers.IO) {
        val inputStream = contentResolver.openInputStream(uri)
            ?: throw IOException("Cannot open file")
        val bytes = inputStream.readBytes()
        inputStream.close()

        val fileBody = bytes.toRequestBody(mimeType.toMediaType())
        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", fileName, fileBody)
            .build()

        val request = Request.Builder()
            .url("$BASE_URL/api/documents")
            .post(multipartBody)
            .build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw IOException("API error: ${response.code}")
        gson.fromJson(response.body?.string(), DocItem::class.java)
    }

    suspend fun getDocuments(): List<DocItem> = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$BASE_URL/api/documents").get().build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw IOException("API error: ${response.code}")
        val type = object : TypeToken<List<DocItem>>() {}.type
        gson.fromJson(response.body?.string(), type)
    }

    suspend fun deleteDocument(docId: String) = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$BASE_URL/api/documents/$docId").delete().build()
        client.newCall(request).execute()
    }

    // ========== Chat ==========

    suspend fun chat(
        message: String,
        linkIds: List<String> = emptyList(),
        docIds: List<String> = emptyList(),
        history: List<ChatMessage> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        val payload = mapOf(
            "message" to message,
            "link_ids" to linkIds,
            "doc_ids" to docIds,
            "history" to history.map { mapOf("role" to it.role, "content" to it.content) }
        )
        val json = gson.toJson(payload)
        val body = json.toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url("$BASE_URL/api/chat").post(body).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw IOException("API error: ${response.code}")
        val result = gson.fromJson(response.body?.string(), Map::class.java)
        result["reply"] as? String ?: "无回复"
    }
}
