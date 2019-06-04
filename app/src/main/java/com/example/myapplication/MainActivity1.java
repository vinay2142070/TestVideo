package com.example.myapplication;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import com.example.myapplication.videocompression.MediaController;

import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import pub.devrel.easypermissions.EasyPermissions;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
public class MainActivity1 extends AppCompatActivity implements EasyPermissions.PermissionCallbacks{
    private static final String TAG = MainActivity1.class.getSimpleName();
    private static final int REQUEST_VIDEO_CAPTURE = 300;
    private static final int READ_REQUEST_CODE = 200;
    private Uri uri;
    private String pathToOriginalVideo;
    private VideoView displayRecordedVideoOriginal;
    private VideoView displayRecordedVideoCompress;
    private static final String SERVER_PATH = "http://192.168.1.105:8080";
    TextView tv_videosizeOriginal;
    TextView tv_videosizeCompress;
    private ProgressDialog progressDialog;

    TextView txtProgressPercent,txtProgressPercentUpload;
    ProgressBar progressBar,progressBarUpload;
    Button btnDownloadFile;

    DownloadZipFileTask downloadZipFileTask;
    private static String ORIGINAL_FILE_NAME="Video_original";
    private static String COMPRESS_FILE_NAME="Video_compress";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main1);
        displayRecordedVideoOriginal = (VideoView)findViewById(R.id.video_display_original);
        tv_videosizeOriginal = (TextView) findViewById(R.id.tv_videosize_original);
        displayRecordedVideoCompress = (VideoView)findViewById(R.id.video_display_compress);
        tv_videosizeCompress = (TextView) findViewById(R.id.tv_videosize_compress);
        txtProgressPercent = (TextView) findViewById(R.id.txtProgressPercent);
        progressBar = findViewById(R.id.progressBar);
        txtProgressPercentUpload = (TextView) findViewById(R.id.txtProgressPercentupload);
        progressBarUpload = findViewById(R.id.progressBarupload);
        displayRecordedVideoOriginal.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Intent intent = new Intent(Intent.ACTION_VIEW );
                intent.setDataAndType(uri, "video/*");
                startActivity(intent);
                return true;
            }
        });


        displayRecordedVideoCompress.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Intent intent = new Intent(Intent.ACTION_VIEW );
                intent.setDataAndType(Uri.fromFile(new File(MediaController.cachedFile.getPath())), "video/*");
                startActivity(intent);
                return true;
            }
        });

        Button captureVideoButton = (Button)findViewById(R.id.capture_video);
        captureVideoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent videoCaptureIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);

                if(videoCaptureIntent.resolveActivity(getPackageManager()) != null){
                    videoCaptureIntent.putExtra(android.provider.MediaStore.EXTRA_VIDEO_QUALITY, 0);
                    startActivityForResult(videoCaptureIntent, REQUEST_VIDEO_CAPTURE);
                }
            }
        });


        Button uploadVideoButton = (Button)findViewById(R.id.upload_video);
        uploadVideoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(pathToOriginalVideo == null)
                {

                    Toast.makeText(getApplicationContext(),"Please Capture Video",Toast.LENGTH_LONG).show();

                }
                else
                {
                    uploadVideoToServer(MediaController.cachedFile.getPath());
                }
            }
        });

        Button downloadVideoButton = (Button)findViewById(R.id.download_video);
        downloadVideoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadZipFile();
            }
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == Activity.RESULT_OK && requestCode == REQUEST_VIDEO_CAPTURE){
            uri = data.getData();
            if(EasyPermissions.hasPermissions(MainActivity1.this, android.Manifest.permission.READ_EXTERNAL_STORAGE)){
                displayRecordedVideoOriginal.setVideoURI(uri);
                displayRecordedVideoOriginal.setBackgroundColor(Color.TRANSPARENT);
                android.widget.MediaController mediaController = new
                        android.widget.MediaController(this);
                mediaController.setAnchorView(displayRecordedVideoOriginal);
                displayRecordedVideoOriginal.setMediaController(mediaController);



                //displayRecordedVideo.start();



                pathToOriginalVideo = getRealPathFromURIPath(uri, MainActivity1.this);
                Log.d(TAG, "Recorded Video Path " + pathToOriginalVideo);
                File file=new File(pathToOriginalVideo);
                Log.e("before compression",file.getAbsolutePath()+"");
                long size = file.length();
                tv_videosizeOriginal.setText("Video Size:"+String.valueOf((size/1000)/1000)+" MB");

             //Compress video
                new VideoCompressor().execute();



            }else{
                EasyPermissions.requestPermissions(MainActivity1.this, getString(R.string.read_file), READ_REQUEST_CODE, Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, MainActivity1.this);
    }
    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        if(uri != null){
            if(EasyPermissions.hasPermissions(MainActivity1.this, android.Manifest.permission.READ_EXTERNAL_STORAGE)){
                displayRecordedVideoOriginal.setVideoURI(uri);
                displayRecordedVideoOriginal.start();

                pathToOriginalVideo = getRealPathFromURIPath(uri, MainActivity1.this);
                Log.d(TAG, "Recorded Video Path " + pathToOriginalVideo);
                Toast.makeText(getApplicationContext(),"File saved to "+pathToOriginalVideo,Toast.LENGTH_SHORT).show();



                //Store the video to your server
               // uploadVideoToServer(pathToOriginalVideo);

            }
        }
    }
    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        Log.d(TAG, "User has denied requested permission");
    }


    private String getRealPathFromURIPath(Uri contentURI, Activity activity) {
        Cursor cursor = activity.getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) {
            return contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            return cursor.getString(idx);
        }
    }
    private class VideoCompressor extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(MainActivity1.this);
            progressDialog.setIndeterminate(true);
            progressDialog.setMessage("Compressing Video...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            return MediaController.getInstance().convertVideo(pathToOriginalVideo);
        }

        @Override
        protected void onPostExecute(Boolean compressed) {
            super.onPostExecute(compressed);
            if (progressDialog != null && progressDialog.isShowing())
                progressDialog.dismiss();
            if (compressed) {
                Log.e("Compression", "Compressed successfully!");
                Log.e("Compressed File Path", "" + MediaController.cachedFile.getPath());
                displayRecordedVideoCompress.setVideoURI(Uri.fromFile(new File(MediaController.cachedFile.getPath())));
                displayRecordedVideoCompress.setBackgroundColor(Color.TRANSPARENT);
                android.widget.MediaController mediaController = new
                        android.widget.MediaController(MainActivity1.this);
                mediaController.setAnchorView(displayRecordedVideoCompress);
                displayRecordedVideoCompress.setMediaController(mediaController);
                File f = new File(MediaController.cachedFile.getPath());
                long size = f.length();
                tv_videosizeCompress.setText("Video Size:"+String.valueOf(size/1000)+" KB");
                //displayRecordedVideo.start();


                Log.d(TAG, "Compressed Video Path " + MediaController.cachedFile.getPath());
                File file=new File(MediaController.cachedFile.getPath());
                Log.e("After compression",file.getAbsolutePath()+"");

            }


        }
    }



    private void uploadVideoToServer(String pathToVideoFile){
       /* progressDialog = new ProgressDialog(MainActivity1.this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Uploading Video...");
        progressDialog.setCancelable(false);;
        progressDialog.show();*/
       final File videoFile = new File(pathToVideoFile);

        ProgressRequestBody videoBody =new ProgressRequestBody(videoFile, "video", new ProgressRequestBody.UploadCallbacks() {
            @Override
            public void onProgressUpdate(int percentage,long fileLength) {
                progressBarUpload.setProgress(percentage);
                txtProgressPercentUpload.setText("Progress "+String.valueOf(percentage+1)+"%");
            }

            @Override
            public void onError() {

            }

            @Override
            public void onFinish() {
                progressBar.setProgress(100);


            }
        });
        MultipartBody.Part vFile = MultipartBody.Part.createFormData("file", videoFile.getName(), videoBody);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(SERVER_PATH)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        VideoInterface vInterface = retrofit.create(VideoInterface.class);
        Call<ResultObject>  serverCom = vInterface.uploadVideoToServer(vFile);

        serverCom.enqueue(new Callback<ResultObject>() {
            @Override
            public void onResponse(Call<ResultObject> call, Response<ResultObject> response) {

                /*if (progressDialog != null && progressDialog.isShowing())
                    progressDialog.dismiss();*/
                Toast.makeText(getApplicationContext(),response.body().getSuccess(),Toast.LENGTH_SHORT).show();
                Log.e(TAG,"Beforedelete1:"+videoFile);
                Log.e(TAG,"Beforedelete2:"+pathToOriginalVideo);
                if(!deleteDir(videoFile))Log.e(TAG,"failed to delete:"+videoFile);
                if(!deleteDir(new File(pathToOriginalVideo)))Log.e(TAG,"failed to delete:"+new File(pathToOriginalVideo));


            }
            @Override
            public void onFailure(Call<ResultObject> call, Throwable t) {
                Log.d(TAG, "Error message " + t.getMessage());
                Toast.makeText(getApplicationContext(),"Error message " + t.getMessage(),Toast.LENGTH_SHORT).show();
                /*if (progressDialog != null && progressDialog.isShowing())
                    progressDialog.dismiss();*/
            }
        });
    }
    private void downloadZipFile() {

        VideoInterface downloadService = createService(VideoInterface.class, SERVER_PATH);
        Call<ResponseBody> call = downloadService.downloadFileByUrl("downloadFile/"+COMPRESS_FILE_NAME+".mp4");

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, final Response<ResponseBody> response) {
                if (response.isSuccessful()) {

//                    boolean writtenToDisk = writeResponseBodyToDisk(response.body());
//                if(writtenToDisk) Toast.makeText(getApplicationContext(), "File downloaded successfully", Toast.LENGTH_SHORT).show();
                      downloadZipFileTask = new DownloadZipFileTask();
                    downloadZipFileTask.execute(response.body());
                } else {
                    Log.d(TAG, "Connection failed " + response.errorBody());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                t.printStackTrace();
                Log.e(TAG, t.getMessage());
            }
        });
    }

    public <T> T createService(Class<T> serviceClass, String baseUrl) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(new OkHttpClient.Builder().build())
                .build();
        return retrofit.create(serviceClass);
    }

    private class DownloadZipFileTask extends AsyncTask<ResponseBody, Pair<Integer, Long>, String> {
        File destinationFile;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected String doInBackground(ResponseBody... urls) {
            //Copy you logic to calculate progress and call
            saveToDisk(urls[0], COMPRESS_FILE_NAME+"_downloaded"+".mp4");
            return null;
        }

        protected void onProgressUpdate(Pair<Integer, Long>... progress) {

            Log.d("API123", progress[0].second + " ");

            if (progress[0].first == 100)
            {
                Toast.makeText(getApplicationContext(), "File downloaded successfully", Toast.LENGTH_SHORT).show();
                txtProgressPercent.setText("Progress ("+String.valueOf((progress[0].first/1000)/1000)+"MB/"+String.valueOf((progress[0].second/1000)/1000)+"MB)  100%");
            }


            if (progress[0].second > 0) {
                int currentProgress = (int) ((double) progress[0].first / (double) progress[0].second * 100);
                progressBar.setProgress(currentProgress);

                txtProgressPercent.setText("Progress ("+String.valueOf((progress[0].first/1000)/1000)+"MB/"+String.valueOf((progress[0].second/1000)/1000)+"MB)" + currentProgress + "%");

            }

            if (progress[0].first == -1) {
                Toast.makeText(getApplicationContext(), "Download failed", Toast.LENGTH_SHORT).show();
            }

        }

        public void doProgress(Pair<Integer, Long> progressDetails) {
            publishProgress(progressDetails);
        }

        @Override
        protected void onPostExecute(String result) {



            Intent intent = new Intent(Intent.ACTION_VIEW );
            intent.setDataAndType(Uri.fromFile(destinationFile), "video/*");
            startActivity(intent);

        }

        private void saveToDisk(ResponseBody body, String filename) {
            try {

                 destinationFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename);

                InputStream inputStream = null;
                OutputStream outputStream = null;

                try {

                    inputStream = body.byteStream();
                    outputStream = new FileOutputStream(destinationFile);
                    byte data[] = new byte[4096];
                    int count;
                    int progress = 0;
                    long fileSize = body.contentLength();
                    Log.d(TAG, "File Size=" + fileSize);
                    while ((count = inputStream.read(data)) != -1) {
                        outputStream.write(data, 0, count);
                        progress += count;
                        Pair<Integer, Long> pairs = new Pair<>(progress, fileSize);
                        downloadZipFileTask.doProgress(pairs);
                        Log.d(TAG, "Progress: " + progress + "/" + fileSize + " >>>> " + (float) progress / fileSize);
                    }

                    outputStream.flush();

                    Log.d(TAG, destinationFile.getParent());
                    Pair<Integer, Long> pairs = new Pair<>(100, 100L);
                    downloadZipFileTask.doProgress(pairs);
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                    Pair<Integer, Long> pairs = new Pair<>(-1, Long.valueOf(-1));
                    downloadZipFileTask.doProgress(pairs);
                    Log.d(TAG, "Failed to save the file!");
                    return;
                } finally {
                    if (inputStream != null) inputStream.close();
                    if (outputStream != null) outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "Failed to save the file!");
                return;
            }
        }
    }



    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    Log.e(TAG,"Failed to delete");
                    return false;
                }
            }
            return dir.delete();
        } else if(dir!= null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
        }
    }

}

