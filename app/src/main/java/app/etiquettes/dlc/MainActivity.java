package app.etiquettes.dlc;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.sunmi.peripheral.printer.InnerPrinterCallback;
import com.sunmi.peripheral.printer.InnerPrinterManager;
import com.sunmi.peripheral.printer.SunmiPrinterService;

/**
 * Coquille Android : affiche l'interface (assets/index.html) et lui donne
 * acces a l'imprimante integree Sunmi via l'objet JavaScript "Android".
 */
public class MainActivity extends Activity {

    private WebView web;
    private volatile SunmiPrinterService printer;
    private volatile String bindError = "";

    private final InnerPrinterCallback printerCallback = new InnerPrinterCallback() {
        @Override
        protected void onConnected(SunmiPrinterService service) {
            printer = service;
            bindError = "";
        }

        @Override
        protected void onDisconnected() {
            printer = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setTextZoom(100);
        web.setWebViewClient(new WebViewClient());
        web.addJavascriptInterface(new Bridge(), "Android");
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");

        bindPrinter();
    }

    private void bindPrinter() {
        try {
            boolean ok = InnerPrinterManager.getInstance().bindService(this, printerCallback);
            if (!ok) {
                bindError = "Service d'impression Sunmi introuvable";
            }
        } catch (Exception e) {
            bindError = String.valueOf(e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        try {
            InnerPrinterManager.getInstance().unBindService(this, printerCallback);
        } catch (Exception ignored) {
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        // Laisse d'abord l'interface fermer un panneau ouvert.
        web.evaluateJavascript(
                "(window.onAndroidBack && window.onAndroidBack()) ? 'handled' : 'exit'",
                new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                        if (value == null || !value.contains("handled")) {
                            finish();
                        }
                    }
                });
    }

    /** Methodes appelables depuis la page : Android.status(), Android.printImage(...) */
    private class Bridge {

        /** Etat imprimante : 1 = prete, 4 = plus de papier, 6 = capot ouvert, -1 = non connectee... */
        @JavascriptInterface
        public int status() {
            SunmiPrinterService p = printer;
            if (p == null) {
                return -1;
            }
            try {
                return p.updatePrinterState();
            } catch (Exception e) {
                return -2;
            }
        }

        @JavascriptInterface
        public String lastError() {
            return bindError;
        }

        /**
         * Imprime une image PNG (base64) de 384 points de large.
         * labelMode = true pour des etiquettes adhesives pre-decoupees.
         * Retourne "ok" ou un message d'erreur.
         */
        @JavascriptInterface
        public String printImage(String base64Png, int copies, boolean labelMode) {
            SunmiPrinterService p = printer;
            if (p == null) {
                bindPrinter();
                return "Imprimante non connectée. " + bindError;
            }
            try {
                String data = base64Png;
                int comma = data.indexOf(',');
                if (comma >= 0) {
                    data = data.substring(comma + 1);
                }
                byte[] bytes = Base64.decode(data, Base64.DEFAULT);
                Bitmap decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (decoded == null) {
                    return "Image d'étiquette illisible";
                }
                // Fond blanc opaque, format attendu par l'imprimante.
                Bitmap bmp = Bitmap.createBitmap(decoded.getWidth(), decoded.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bmp);
                canvas.drawColor(Color.WHITE);
                canvas.drawBitmap(decoded, 0f, 0f, null);

                int n = Math.max(1, Math.min(copies, 20));
                for (int i = 0; i < n; i++) {
                    if (labelMode) {
                        p.labelLocate();
                        p.printBitmap(bmp, null);
                        p.labelOutput();
                    } else {
                        p.printBitmap(bmp, null);
                        p.lineWrap(4, null);
                    }
                }
                return "ok";
            } catch (Exception e) {
                return "Erreur d'impression : " + e.getMessage();
            }
        }
    }
}
