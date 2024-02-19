package com.example.ackpineexample

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import androidx.core.content.FileProvider
import com.example.ackpineexample.databinding.ActivityMainBinding
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import ru.solrudev.ackpine.installer.createSession
import ru.solrudev.ackpine.session.SessionResult
import ru.solrudev.ackpine.session.await
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.cert.Certificate
import javax.net.ssl.SSLContext
import kotlin.coroutines.cancellation.CancellationException

class MainActivity : Activity() {
    private lateinit var binding : ActivityMainBinding
    private lateinit var downloadButton: Button
    private lateinit var installButton: Button

    private var outputFile :File? = null

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        downloadButton = binding.downloadButton /*다운로드 버튼*/
        installButton = binding.installButton //설치 버튼

        setContentView(binding.root)


        //다운로드 버튼 클릭 시
        binding.downloadButton.setOnClickListener {
            GlobalScope.launch(Dispatchers.IO){
                val downloadJop = async { downloadApk() } //apk 파일 다운로드 실행
                val result = downloadJop.await()
                /* apk 파일 다운로드 후 isntall을 위해 async await 사용 */
                if (result){
                    installApk() //apk 파일 설치 실행
                }
            }
        }


    }
    /*서버에 업로드된 apk 파일 다운로드 실행*/
    private  fun downloadApk() : Boolean {

        Log.e("JINNN", "download Start")

        // 저장할 파일 경로
        outputFile = File(Environment.getExternalStorageDirectory().absolutePath+"/Download", "your-app.apk")


        val url = URL("APK_URL")
        val connection: HttpURLConnection = url.openConnection() as HttpURLConnection /* http */

        // 연결 타임아웃 및 읽기 타임아웃 설정
        connection.connectTimeout = 60000 // 연결 타임아웃 60초
        connection.readTimeout = 60000    // 읽기 타임아웃 60초

        // InputStream을 통해 데이터 읽기
        val input: InputStream = BufferedInputStream(url.openStream())

        // OutputStream을 통해 파일에 데이터 쓰기
        val output: OutputStream = FileOutputStream(outputFile)
        val data = ByteArray(1024)
        var count: Int
        while (input.read(data).also { count = it } != -1) {
            output.write(data, 0, count)
        }

        // 자원 정리
        output.flush()
        output.close()
        input.close()
        return true
    }
    /*다운로드 받은 apk 파일 설치*/
    private suspend fun installApk(){
        Log.i("JINNN", "install Start")
        // 설치할 APK 파일 경로
        val apkFileUri: Uri? = outputFile?.let {
            FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                it
            )
        }
        try {
            /* apk install 실행 - ackpine 라이브러리 사용*/
            val packageInstaller = ru.solrudev.ackpine.installer.PackageInstaller.getInstance(this)
            when (val result = apkFileUri?.let { packageInstaller.createSession(it).await() }) {
                is SessionResult.Success -> println("Success")
                is SessionResult.Error -> println(result.cause.message)
                else -> {}
            }
        } catch (_: CancellationException) {
            println("Cancelled")
        } catch (exception: Exception) {
            println(exception)
        }
    }




}