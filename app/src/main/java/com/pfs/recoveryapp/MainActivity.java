package com.pfs.recoveryapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.DownloadListener;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends Activity {

    WebView navegador;
    private ProgressBar barraProgreso;
    ProgressDialog dialogo;
    int cargando = 0;
    Button btnDetener, btnAnterior, btnSiguiente;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        btnDetener = (Button) findViewById(R.id.btnDetener);
        btnAnterior = (Button) findViewById(R.id.btnAnterior);
        btnSiguiente = (Button) findViewById(R.id.btnSiguiente);
        navegador = (WebView) findViewById(R.id.webkit);
        navegador.getSettings().setJavaScriptEnabled(true);
        navegador.getSettings().setBuiltInZoomControls(false);
        //navegador.loadUrl("https://localhost:8449/pfs/login.jsp");
        //navegador.loadUrl("https://192.168.0.143:8449/pfs/login.jsp");
        navegador.loadUrl("https://recovery.pfsgroup.es:8449/pfs/login.jsp");
        navegador.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }
        });
        barraProgreso = (ProgressBar) findViewById(R.id.barraProgreso);
        navegador.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int progreso) {
                barraProgreso.setProgress(0);
                barraProgreso.setVisibility(View.VISIBLE);
                MainActivity.this.setProgress(progreso * 1000);
                barraProgreso.incrementProgressBy(progreso);
                if (progreso == 100) {
                    barraProgreso.setVisibility(View.GONE);
                }
            }

            //no funciona
            @Override
            public boolean onJsAlert(WebView view, String url, String message,
                                     final JsResult result) {
                new AlertDialog.Builder(MainActivity.this).setTitle("Mensaje")
                        .setMessage(message).setPositiveButton
                        (android.R.string.ok, new AlertDialog.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                result.confirm();
                            }
                        }).setCancelable(false).create().show();

                return true;
            }

        });

        navegador.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (cargando <= 0) {
                    dialogo = new ProgressDialog(MainActivity.this);
                    dialogo.setMessage("Cargando...");
                    dialogo.setCancelable(true);
                    dialogo.show();
                    //btnDetener.setEnabled(true);
                    if (comprobarConectividad()) {
                        btnDetener.setEnabled(true);
                    } else {
                        btnDetener.setEnabled(false);
                    }
                    cargando = 1;
                    return;
                } else {
                    dialogo.dismiss();
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                dialogo.dismiss();
                btnDetener.setEnabled(false);
                if (view.canGoBack()) {
                    btnAnterior.setEnabled(true);
                } else {
                    btnAnterior.setEnabled(false);
                }
                if (view.canGoForward()) {
                    btnSiguiente.setEnabled(true);
                } else {
                    btnSiguiente.setEnabled(false);
                }
            }

            @Override
            public void onReceivedError(WebView view, int errorCode,
                                        String description, String failingUrl) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage(description).setPositiveButton("Aceptar",
                        null).setTitle("onReceivedError");
                builder.show();
            }

        });

        navegador.setDownloadListener(new DownloadListener() {
            public void onDownloadStart(final String url, String userAgent, String
                    contentDisposition, String mimetype, long contentLength) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Descarga");
                builder.setMessage("¿Deseas guardar el archivo?");
                builder.setCancelable(false).setPositiveButton("Aceptar",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                URL urlDescarga;
                                try {
                                    urlDescarga = new URL(url);
                                    new DescargarFichero().execute(urlDescarga);
                                } catch (MalformedURLException e) {
                                    e.printStackTrace();
                                }
                            }
                        }).setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
                builder.create().show();
            }
        });
    }

    public void detenerCarga(View v) {
        navegador.stopLoading();
    }

    public void irPaginaAnterior(View v) {
        if (comprobarConectividad()) {
            navegador.goBack();
        }
    }

    public void irPaginaSiguiente(View v) {
        if (comprobarConectividad()) {
            navegador.goForward();
        }
    }

    @Override
    public void onBackPressed() {
        if (navegador.canGoBack()) {
            navegador.goBack();
        }
        else {
            super.onBackPressed();
        }
    }

    private boolean comprobarConectividad() {
        ConnectivityManager connectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        if ((info == null || !info.isConnected() || !info.isAvailable())) {
            Toast.makeText(MainActivity.this, "Oops! No tienes conexión a internet", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class DescargarFichero extends AsyncTask<URL, Integer, Long> {
        private String mensaje;
        @Override
        protected Long doInBackground(URL... url) {
            String urlDescarga = url[0].toString();
            mensaje = "";
            HttpClient httpClient = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(urlDescarga);
            InputStream inputStream = null;
            try {
                HttpResponse httpResponse = httpClient.execute(httpGet);
                BufferedHttpEntity bufferedHttpEntity =
                        new BufferedHttpEntity(httpResponse.getEntity());
                inputStream = bufferedHttpEntity.getContent();
                String fileName = android.os.Environment
                        .getExternalStorageDirectory().getAbsolutePath() + "/descargas";
                File directorio = new File(fileName);
                directorio.mkdirs();
                File file = new File(directorio, urlDescarga.substring(
                        urlDescarga.lastIndexOf("/"),
                        (urlDescarga.indexOf("?")==-1?
                                urlDescarga.length():urlDescarga.indexOf("?"))));
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len = 0;
                while (inputStream.available() > 0 &&
                        (len = inputStream.read(buffer)) != -1) {
                    byteArray.write(buffer, 0, len);
                }
                fileOutputStream.write(byteArray.toByteArray());
                fileOutputStream.flush();
                mensaje = "Guardado en: " + file.getAbsolutePath();
            } catch (Exception ex) {
                mensaje = ex.getClass().getSimpleName() + " " + ex.getMessage();
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                    }
                }
            }
            return (long) 0;
        }

        protected void onPostExecute(Long result) {
            AlertDialog.Builder builder = new
                    AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Descarga");
            builder.setMessage(mensaje);
            builder.setCancelable(true);
            builder.create().show();
        }
    }

}
