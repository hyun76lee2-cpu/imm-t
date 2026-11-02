package com.imm.meter;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

/**
 * 화면과 계산은 assets/imm-meter.html(웹 앱과 같은 코드)이 담당하고,
 * 이 액티비티는 가속도 센서를 최고 속도로 읽어 100 ms마다 묶어서 넘겨준다.
 * 넘기는 형식: [t(초), x, y, z(gal), t, x, y, z, ...]
 */
public class MainActivity extends Activity implements SensorEventListener {

    private static final float MS2_TO_GAL = 100f;
    private static final long FLUSH_MS = 100;
    private static final int FILE_REQUEST = 1001;

    /** CSV 파일 선택 결과를 웹 화면의 <input type="file">에 돌려줄 콜백 */
    private ValueCallback<Uri[]> fileCallback;

    private WebView web;
    private SensorManager sensorManager;
    private Sensor accel;
    private Sensor gyro;                       // 없으면 null (기울어짐 보정 불가)
    private volatile float gx = 0f, gy = 0f, gz = 0f;
    private volatile boolean running = false;

    private final Object lock = new Object();
    private final StringBuilder buffer = new StringBuilder();
    private int buffered = 0;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable flush = new Runnable() {
        @Override
        public void run() {
            String data;
            synchronized (lock) {
                data = buffer.toString();
                buffer.setLength(0);
                buffered = 0;
            }
            if (!data.isEmpty() && web != null) {
                // 자이로가 있으면 [t, x, y, z, gx, gy, gz] 7개씩, 없으면 [t, x, y, z] 4개씩
                String fn = gyro != null ? "nativeSamples7" : "nativeSamples";
                web.evaluateJavascript("window." + fn + " && window." + fn + "([" + data + "])", null);
            }
            if (running) handler.postDelayed(this, FLUSH_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accel = sensorManager == null ? null : sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyro = sensorManager == null ? null : sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        web.addJavascriptInterface(new Bridge(), "ImmNative");
        // WebView는 기본으로 파일 선택 창을 열지 않아서, CSV 불러오기를 위해 직접 연결한다
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                try {
                    startActivityForResult(Intent.createChooser(intent, "CSV 파일 선택"), FILE_REQUEST);
                } catch (Exception e) {
                    fileCallback = null;
                    return false;
                }
                return true;
            }
        });
        setContentView(web);
        web.loadUrl("file:///android_asset/imm-meter.html");
    }

    private void startSensor() {
        if (running || accel == null) return;
        running = true;
        sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_FASTEST);
        if (gyro != null) sensorManager.registerListener(this, gyro, SensorManager.SENSOR_DELAY_FASTEST);
        handler.postDelayed(flush, FLUSH_MS);
    }

    private void stopSensor() {
        running = false;
        if (sensorManager != null) sensorManager.unregisterListener(this);
        handler.removeCallbacks(flush);
        synchronized (lock) {
            buffer.setLength(0);
            buffered = 0;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent e) {
        if (!running) return;
        if (e.sensor.getType() == Sensor.TYPE_GYROSCOPE) {   // 가장 최근 자이로 값을 가속도 샘플에 붙여 보낸다
            gx = e.values[0]; gy = e.values[1]; gz = e.values[2];
            return;
        }
        double t = e.timestamp / 1e9;   // 부팅 후 경과 시간(초), 단조 증가
        synchronized (lock) {
            if (buffered > 0) buffer.append(',');
            buffer.append(t).append(',')
                  .append(e.values[0] * MS2_TO_GAL).append(',')
                  .append(e.values[1] * MS2_TO_GAL).append(',')
                  .append(e.values[2] * MS2_TO_GAL);
            if (gyro != null) buffer.append(',').append(gx).append(',').append(gy).append(',').append(gz);
            buffered++;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_REQUEST) {
            if (fileCallback != null) {
                Uri[] result = (resultCode == RESULT_OK && data != null && data.getData() != null)
                        ? new Uri[]{ data.getData() } : null;
                fileCallback.onReceiveValue(result);
                fileCallback = null;
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        stopSensor();
        if (web != null) {
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }

    /** 자바스크립트에서 window.ImmNative.start() / stop() / info() 로 호출 */
    private class Bridge {
        @JavascriptInterface
        public boolean start() {
            if (accel == null) return false;
            runOnUiThread(MainActivity.this::startSensor);
            return true;
        }

        @JavascriptInterface
        public void stop() {
            runOnUiThread(MainActivity.this::stopSensor);
        }

        @JavascriptInterface
        public String info() {
            if (accel == null) return "가속도 센서 없음";
            return accel.getName() + ", 최대 " + Math.round(accel.getMaximumRange() * 100) + " gal, 분해능 "
                    + String.format(java.util.Locale.US, "%.2f", accel.getResolution() * 100) + " gal"
                    + (gyro != null ? ", 자이로 " + gyro.getName() : ", 자이로 없음");
        }
    }
}
