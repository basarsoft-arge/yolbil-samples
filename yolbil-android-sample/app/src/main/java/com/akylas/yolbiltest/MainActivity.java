    package com.akylas.yolbiltest;

    import android.app.ProgressDialog;
    import android.content.DialogInterface;
    import android.content.Intent;
    import android.net.Uri;
    import android.os.AsyncTask;
    import android.os.Build;
    import android.os.Bundle;
    import android.os.Environment;
    import android.os.Handler;
    import android.os.Looper;
    import android.provider.Settings;
    import android.util.Log;
    import android.widget.Toast;

    import androidx.appcompat.app.AlertDialog;
    import androidx.appcompat.app.AppCompatActivity;

    import com.akylas.yolbiltest.ui.main.SecondFragment;
    import com.basarsoft.yolbil.core.License;
    import com.basarsoft.yolbil.core.LicenseResult;
    import com.basarsoft.yolbil.core.MapPos;
    import com.basarsoft.yolbil.utils.LicenseGate;

    public class MainActivity extends AppCompatActivity {
        private final String TAG = "MainActivity";
        private volatile boolean licenseCheckTriggered = false;
        private int licenseRetryCount = 0;
        private final Handler licenseRetryHandler = new Handler(Looper.getMainLooper());

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            Log.d(TAG, "onCreate");
            setContentView(R.layout.main_activity);
            if (savedInstanceState == null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, SecondFragment.newInstance())
                        .commitNow();
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    // If you don't have access, launch a new activity to show the user the system's dialog
                    // to allow access to the external storage
                } else {
                    /*Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    Uri uri = Uri.fromParts("package", this.getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);*/
                }
            }
        }

        @Override
        protected void onPostResume() {
            Log.d(TAG, "onPostResume");
            super.onPostResume();
        }

        @Override
        protected void onStart() {
            Log.d(TAG, "onStart");
            super.onStart();
        }

        @Override
        protected void onStop() {
            Log.d(TAG, "onStop");
            super.onStop();
        }

        @Override
        protected void onDestroy() {
            Log.d(TAG, "onDestroy");
            super.onDestroy();
        }

        // Lisans kontrol akisi burada baslar.
        @SuppressWarnings("deprecation")
        private void performLicenseDownload() {
            if (!isYolbilNativeReady()) {
                scheduleLicenseRetry();
                return;
            }
            new LicenseDownloadTask().execute();
        }

        public void runLicenseCheckIfNeeded() {
            if (licenseCheckTriggered) {
                return;
            }
            licenseCheckTriggered = true;
            performLicenseDownload();
        }

        private boolean isYolbilNativeReady() {
            try {
                new MapPos(0.0, 0.0);
                return true;
            } catch (UnsatisfiedLinkError ignored) {
                return false;
            }
        }

        private void scheduleLicenseRetry() {
            int maxRetries = 30;
            if (licenseRetryCount >= maxRetries) {
                logWarn("License kontrolu atlandi: native kitaplik yuklenemedi.");
                return;
            }
            licenseRetryCount += 1;
            licenseRetryHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    performLicenseDownload();
                }
            }, 100L);
        }

        private String getDeviceIMEI() {
            try {
                String imei = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
                if (imei == null || imei.trim().isEmpty()) {
                    return "53242341231";
                }
                return imei;
            } catch (Exception e) {
                logWarn("IMEI alma hatası: " + e.getMessage());
                return "53242341231";
            }
        }

        @SuppressWarnings("deprecation")
        private class LicenseDownloadTask extends AsyncTask<Void, Void, LicenseResult> {
            private ProgressDialog progressDialog;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                if (isFinishing()) {
                    return;
                }
                progressDialog = new ProgressDialog(MainActivity.this);
                progressDialog.setMessage("License kontrol ediliyor...");
                progressDialog.setCancelable(false);
                progressDialog.show();
            }
            // Firma Project id verilmesi lazım
            @Override
            protected LicenseResult doInBackground(Void... params) {
                try {
                    int projectId = //  project id'inizi veriniz;
                    String imei = getDeviceIMEI();
                    String savePath = getFilesDir().getAbsolutePath() + "/";

                    logInfo("License parametreleri - ProjectId: " + projectId + ", IMEI: " + imei);
                    logInfo("License save path: " + savePath);

                    // Lisans nesnesini olusturur; dosya kayit yolu uygulamanin internal storage dizinidir.
                    License license = new License(projectId, imei, savePath);
                    return license.checkLicense();
                } catch (Exception e) {
                    logError("License kontrol hatası: " + e.getMessage(), e);
                    return LicenseResult.ERROR;
                }
            }

            @Override
            protected void onPostExecute(LicenseResult result) {
                super.onPostExecute(result);
                LicenseGate.notifyLicenseCheckResult(result == LicenseResult.OK);

                if (progressDialog != null && progressDialog.isShowing()) {
                    try {
                        progressDialog.dismiss();
                    } catch (Exception e) {
                        logWarn("ProgressDialog kapatma hatası: " + e.getMessage());
                    }
                }

                String message;
                String toastMessage;
                boolean isSuccess;

                switch (result) {
                    case OK:
                        message = "License başarıyla kontrol edildi ve geçerli!";
                        toastMessage = "License başarılı";
                        isSuccess = true;
                        break;
                    case NOT_FOUND:
                        message = "License dosyası bulunamadı veya geçersiz parametreler.";
                        toastMessage = "License bulunamadı";
                        isSuccess = false;
                        break;
                    case UNAUTHORIZED:
                        message = "License geçersiz veya süresi dolmuş.";
                        toastMessage = "License geçersiz";
                        isSuccess = false;
                        break;
                    case NETWORK_ERROR:
                        message = "Network bağlantısı hatası. İnternet bağlantınızı kontrol edin.";
                        toastMessage = "Network hatası";
                        isSuccess = false;
                        break;
                    case SERVER_ERROR:
                        message = "Sunucu hatası. Lütfen daha sonra tekrar deneyin.";
                        toastMessage = "Sunucu hatası";
                        isSuccess = false;
                        break;
                    case PERMISSION_DENIED:
                        message = "Dosya yazma izni hatası. Uygulama izinlerini kontrol edin.";
                        toastMessage = "İzin hatası";
                        isSuccess = false;
                        break;
                    case INVALID_HASH:
                        message = "License dosyası bozuk veya geçersiz.";
                        toastMessage = "Geçersiz hash";
                        isSuccess = false;
                        break;
                    case EXPIRED:
                        message = "License süresi dolmuş.";
                        toastMessage = "License süresi doldu";
                        isSuccess = false;
                        break;
                    case CORRUPTED:
                        message = "License dosyası bozuk.";
                        toastMessage = "Bozuk license";
                        isSuccess = false;
                        break;
                    case ERROR:
                    default:
                        message = "License kontrol sırasında beklenmeyen hata oluştu.";
                        toastMessage = "Bilinmeyen hata";
                        isSuccess = false;
                        break;
                }

                logInfo("License işlem sonucu: " + message);

                if (!isFinishing()) {
                    Toast.makeText(MainActivity.this, toastMessage, Toast.LENGTH_LONG).show();
                }

                if (!isSuccess && !isFinishing()) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("License Durumu")
                            .setMessage(message + "\n\nHata kodu: " + result)
                            .setPositiveButton("Tamam", null)
                            .setNegativeButton("Tekrar Dene", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    performLicenseDownload();
                                }
                            })
                            .show();
                }
            }

            @Override
            protected void onCancelled() {
                super.onCancelled();
                if (progressDialog != null && progressDialog.isShowing()) {
                    try {
                        progressDialog.dismiss();
                    } catch (Exception e) {
                        logWarn("ProgressDialog kapatma hatası: " + e.getMessage());
                    }
                }
            }
        }

        private void logInfo(String message) {
            Log.i("License", message);
        }

        private void logWarn(String message) {
            Log.w("License", message);
        }

        private void logError(String message, Throwable throwable) {
            Log.e("License", message, throwable);
        }
    }
