package com.danielrosehill.familypager.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class PushoverApi {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    data class ApiResult(val success: Boolean, val message: String)

    suspend fun sendMessage(
        token: String,
        userKey: String,
        message: String,
        title: String = "Family Pager",
        priority: Int = 0,
        sound: String = "pushover",
        retry: Int? = null,
        expire: Int? = null
    ): ApiResult = withContext(Dispatchers.IO) {
        try {
            val bodyBuilder = FormBody.Builder()
                .add("token", token)
                .add("user", userKey)
                .add("message", message)
                .add("title", title)
                .add("priority", priority.toString())
                .add("sound", sound)

            if (priority == 2) {
                bodyBuilder.add("retry", (retry ?: 60).toString())
                bodyBuilder.add("expire", (expire ?: 3600).toString())
            }

            val request = Request.Builder()
                .url("https://api.pushover.net/1/messages.json")
                .post(bodyBuilder.build())
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            if (response.isSuccessful) {
                ApiResult(true, "Page sent!")
            } else {
                val json = JSONObject(body)
                val errors = json.optJSONArray("errors")
                val errorMsg = errors?.let { (0 until it.length()).map { i -> it.getString(i) }.joinToString(", ") }
                    ?: "Unknown error"
                ApiResult(false, errorMsg)
            }
        } catch (e: Exception) {
            ApiResult(false, e.message ?: "Network error")
        }
    }

    suspend fun validateUser(token: String, userKey: String): ApiResult = withContext(Dispatchers.IO) {
        try {
            val body = FormBody.Builder()
                .add("token", token)
                .add("user", userKey)
                .build()

            val request = Request.Builder()
                .url("https://api.pushover.net/1/users/validate.json")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                ApiResult(true, "Valid!")
            } else {
                ApiResult(false, "Invalid token or user key")
            }
        } catch (e: Exception) {
            ApiResult(false, e.message ?: "Network error")
        }
    }

    suspend fun login(email: String, password: String, twofa: String? = null): Result<LoginResult> =
        withContext(Dispatchers.IO) {
            try {
                val bodyBuilder = FormBody.Builder()
                    .add("email", email)
                    .add("password", password)
                if (twofa != null) {
                    bodyBuilder.add("twofa", twofa)
                }

                val request = Request.Builder()
                    .url("https://api.pushover.net/1/users/login.json")
                    .post(bodyBuilder.build())
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""
                val json = JSONObject(responseBody)

                when {
                    response.isSuccessful -> {
                        Result.success(
                            LoginResult(
                                secret = json.getString("secret"),
                                userId = json.getString("id"),
                                needs2fa = false
                            )
                        )
                    }
                    response.code == 412 -> {
                        Result.success(LoginResult(secret = "", userId = "", needs2fa = true))
                    }
                    else -> {
                        Result.failure(Exception("Login failed: ${response.code}"))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun registerDevice(secret: String, deviceName: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val body = FormBody.Builder()
                    .add("secret", secret)
                    .add("name", deviceName)
                    .add("os", "O")
                    .build()

                val request = Request.Builder()
                    .url("https://api.pushover.net/1/devices.json")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""
                val json = JSONObject(responseBody)

                if (response.isSuccessful) {
                    Result.success(json.getString("id"))
                } else {
                    Result.failure(Exception("Device registration failed"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun fetchMessages(secret: String, deviceId: String): Result<List<PushoverMessage>> =
        withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("https://api.pushover.net/1/messages.json?secret=$secret&device_id=$deviceId")
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""
                val json = JSONObject(responseBody)

                if (response.isSuccessful) {
                    val messages = json.getJSONArray("messages")
                    val result = mutableListOf<PushoverMessage>()
                    for (i in 0 until messages.length()) {
                        val msg = messages.getJSONObject(i)
                        result.add(
                            PushoverMessage(
                                id = msg.getLong("id"),
                                title = msg.optString("title", ""),
                                message = msg.optString("message", ""),
                                priority = msg.optInt("priority", 0),
                                date = msg.optLong("date", 0)
                            )
                        )
                    }
                    Result.success(result)
                } else {
                    Result.failure(Exception("Failed to fetch messages"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun deleteMessages(secret: String, deviceId: String, highestId: Long): ApiResult =
        withContext(Dispatchers.IO) {
            try {
                val body = FormBody.Builder()
                    .add("secret", secret)
                    .add("message", highestId.toString())
                    .build()

                val request = Request.Builder()
                    .url("https://api.pushover.net/1/devices/$deviceId/update_highest_message.json")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    ApiResult(true, "Cleared")
                } else {
                    ApiResult(false, "Failed to clear messages")
                }
            } catch (e: Exception) {
                ApiResult(false, e.message ?: "Error")
            }
        }

    data class LoginResult(val secret: String, val userId: String, val needs2fa: Boolean)

    data class PushoverMessage(
        val id: Long,
        val title: String,
        val message: String,
        val priority: Int,
        val date: Long
    )
}
