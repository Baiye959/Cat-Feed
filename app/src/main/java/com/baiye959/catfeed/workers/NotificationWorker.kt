package com.baiye959.catfeed.workers

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.baiye959.catfeed.MainActivity
import com.baiye959.catfeed.R
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class NotificationWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params){

    @kotlin.OptIn(DelicateCoroutinesApi::class)
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun doWork(): Result {
        val job = GlobalScope.launch {
            sendNotification2(applicationContext)
        }

        // 等待协程完成任务
        runBlocking {
            job.join()
        }

        return Result.success()
    }
    // 异步网络请求
    suspend fun simpleGetUse(context: Context): String {
        val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        val apiUrl = prefs.getString("onenet_api_url", "https://iot-api.heclouds.com/datapoint/history-datapoints?product_id=TRZ54Siy6T&device_name=test_pi")!!
        val apiToken = prefs.getString("onenet_api_token", "version=2022-05-01&res=products%2FTRZ54Siy6T&et=1725627611&method=sha1&sign=qmCyahbefgl1qGXAjF5x%2BoYzEwQ%3D")!!

        return suspendCancellableCoroutine { continuation ->
            val tag = "simpleGetUse"
            val okHttpClient = OkHttpClient()
            val requestBuilder = Request.Builder()
                .url(apiUrl)
                .addHeader("Authorization", apiToken)

            okHttpClient.newCall(requestBuilder.build()).enqueue(object : Callback {
                @OptIn(UnstableApi::class) override fun onFailure(call: Call, e: IOException) {
                    Log.d(tag, "go failure ${e.message}")
                    continuation.resumeWithException(e)
                }

                @OptIn(UnstableApi::class)
                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        val result = simpleDealData(response)
                        Log.d(tag, result)
                        continuation.resume(result)
                    } else {
                        val result = "failure ${response.message}"
                        Log.d(tag, result)
                        continuation.resume(result)
                    }
                }
            })
        }
    }

    // 该函数用于简单处理返回的信息
    private fun simpleDealData(response: Response): String {
        val responseBody = response.body?.string() ?: return "No response body"

        val jsonObject = JSONObject(responseBody)
        val value = jsonObject.getJSONObject("data")
            .getJSONArray("datastreams")
            .getJSONObject(0)
            .getJSONArray("datapoints")
            .getJSONObject(0)
            .getInt("value")

        return StringBuilder().apply {
            append("$value")
        }.toString()
    }

    // 发送通知
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun sendNotification(context: Context, title: String, content: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, "default_channel")
            .setSmallIcon(R.drawable.cat_icon)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    context as Activity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1
                )
                return
            }
            notify(1, builder.build())
        }
    }

    // 根据请求数据判断是否发送通知
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    suspend fun sendNotification2(context: Context) {
        val remainHeight = withContext(Dispatchers.IO) {
            simpleGetUse(context).toInt()
        }
        if (remainHeight <= 10) {
            val remainPercent = (remainHeight / 30.0 * 100).toInt()
            sendNotification(context, "猫粮剩余量不足！", "猫粮只剩$remainPercent%！请您及时补充！")
        }
    }
}