package com.baiye959.catfeed

import android.Manifest
import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.baiye959.catfeed.ui.theme.CatFeedTheme
import androidx.media3.common.MediaItem
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
//import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
//import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.ui.PlayerView
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import org.json.JSONObject
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.datasource.rtmp.RtmpDataSource

class MainActivity : ComponentActivity() {
    private lateinit var exoPlayer: ExoPlayer

    @OptIn(UnstableApi::class) @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CatFeedTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LiveStreamScreen(exoPlayer)
                }
            }
        }

        // Initialize ExoPlayer
        exoPlayer = ExoPlayer.Builder(this).build().apply {
            val dataSourceFactory = RtmpDataSource.Factory()
            val rtmpMediaSource: MediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri("rtmp://10.26.44.75:1935/live/test"))
            setMediaSource(rtmpMediaSource)
            prepare()
            playWhenReady = true
        }

        createNotificationChannel()
    }

    override fun onStart() {
        super.onStart()
        exoPlayer.playWhenReady = true
    }

    override fun onStop() {
        super.onStop()
        exoPlayer.playWhenReady = false
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer.release()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "default_channel",
                "Default Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "This is the default notification channel"
            }
            val notificationManager: NotificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}



@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(UnstableApi::class) @Composable
fun LiveStreamScreen(exoPlayer: ExoPlayer) {
    val context = LocalContext.current

    // 拉取直播流
//    val exoPlayer = remember {
//        ExoPlayer.Builder(context).build().apply {
//            val dataSourceFactory = DefaultHttpDataSource.Factory()
//            val hlsMediaSource: MediaSource = HlsMediaSource.Factory(dataSourceFactory)
//                .createMediaSource(MediaItem.fromUri("https://cn-fjfz-fx-01-05.bilivideo.com/live-bvc/257543/live_4505367_8136424/index.m3u8"))
//            setMediaSource(hlsMediaSource)
//            prepare()
//            playWhenReady = true
//        }
//    }
//    val exoPlayer = remember {
//        ExoPlayer.Builder(context).build().apply {
//            val dataSourceFactory = RtmpDataSource.Factory()
//            val rtmpMediaSource: MediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
//                .createMediaSource(MediaItem.fromUri("rtmp://10.26.44.75:1935/live/test"))
//            setMediaSource(rtmpMediaSource)
//            prepare()
//            playWhenReady = true
//        }
//    }
    // 应用结束时释放exoPlayer资源
    DisposableEffect(
        key1 = exoPlayer
    ) {
        onDispose { exoPlayer.release() }
    }


    var remainHeight by remember { mutableStateOf( "hello") }
    val imageResource = when (remainHeight) {
        "0","1","2","3","4","5","6","7","8","9","10" -> R.drawable.bottle_low
        "11","12","13","14","15","16","17","18","19","20" -> R.drawable.bottle_middle
        else -> R.drawable.bottle_high
    }

    // 定时更新remainHeight
    LaunchedEffect(Unit) {
        while (true) {
            remainHeight = simpleGetUse()
            delay(3000) // 每隔3秒执行一次
        }
    }

    // 页面绘制
    // 自定义字体
    val customFontFamily = FontFamily(
        Font(resId = R.font.ubuntu, weight = FontWeight.Normal)
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        AndroidView(factory = {
            PlayerView(context).apply {
                player = exoPlayer
            }
        },
            modifier = Modifier
                .size(400.dp, 250.dp)
                .padding(bottom = 10.dp)
        )

        Text(
            text = "Cat Feed",
            fontSize = 50.sp,
            lineHeight = 50.sp,
            fontWeight = FontWeight.Bold,
            style = TextStyle(fontFamily = customFontFamily),
            modifier = Modifier
                .align(alignment = CenterHorizontally)
                .padding(bottom = 10.dp)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(30.dp, CenterHorizontally),
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp, 10.dp)
        ) {
            Button(
                onClick = {
                    sendNotification(
                        context = context,
                        title = "Hello world!",
                        content = "This is a notification"
                    )
                },
                colors = ButtonDefaults.buttonColors(Color(0xFF6BB9E6)),
                modifier = Modifier
                    .weight(1f)
//                    .height(50.dp)
            ) {
                Text("发送通知")
            }
            Button(
                onClick = { /*TODO*/ },
                colors = ButtonDefaults.buttonColors(Color(0xFFF8B551)),
                modifier = Modifier
                    .weight(1f)
//                    .height(50.dp)
            ) {
                Text("按钮2")
            }
        }

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painter = painterResource(imageResource),
                contentDescription = remainHeight,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = remainHeight,
                fontSize = 40.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(alignment = Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            )
        }
    }
}

// 异步网络请求
suspend fun simpleGetUse(): String {
    return suspendCancellableCoroutine { continuation ->
        val tag = "simpleGetUse"
        val okHttpClient = OkHttpClient()
        val requestBuilder = Request.Builder()
            .url("https://iot-api.heclouds.com/datapoint/history-datapoints?product_id=TRZ54Siy6T&device_name=test_pi")
            .addHeader("Authorization", "version=2022-05-01&res=products%2FTRZ54Siy6T&et=1725627611&method=sha1&sign=qmCyahbefgl1qGXAjF5x%2BoYzEwQ%3D")

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

//该函数用于简单处理返回的信息
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
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            println("没有权限")
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