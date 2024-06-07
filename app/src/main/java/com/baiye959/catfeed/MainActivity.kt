package com.baiye959.catfeed

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.baiye959.catfeed.ui.theme.CatFeedTheme
import androidx.media3.common.MediaItem
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CatFeedTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LiveStreamScreen()
                }
            }
        }
    }
}



@OptIn(UnstableApi::class) @Composable
fun LiveStreamScreen() {
    val context = LocalContext.current

    // 拉取直播流
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val dataSourceFactory = DefaultHttpDataSource.Factory()
            val hlsMediaSource: MediaSource = HlsMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri("https://d1--cn-gotcha204.bilivideo.com/live-bvc/560341/live_4505367_8136424_2500/index.m3u8"))
            setMediaSource(hlsMediaSource)
            prepare()
            playWhenReady = true
        }
    }
    // 应用结束时释放exoPlayer资源
    DisposableEffect(
        key1 = exoPlayer
    ) {
        onDispose { exoPlayer.release() }
    }


    var remainHeight by remember { mutableStateOf( "hello") }

    // 定时更新remainHeight
    LaunchedEffect(Unit) {
        while (true) {
            remainHeight = simpleGetUse()
            delay(1000) // 每隔1秒执行一次
        }
    }

    // 页面绘制
    val image = painterResource(R.drawable.bg)
    Image(
        painter = image,
        contentDescription = null,
        contentScale = ContentScale.Crop
    )
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        AndroidView(factory = {
            PlayerView(context).apply {
                player = exoPlayer
            }
        },
            modifier = Modifier
                .size(400.dp, 250.dp)
                .padding(10.dp)
        )

        Text(
            text = remainHeight,
            fontSize = 30.sp,
            lineHeight = 30.sp,
            modifier = Modifier
                .padding(20.dp, 10.dp)
                .align(alignment = Alignment.End)
        )
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
        append("value: $value")
    }.toString()
}