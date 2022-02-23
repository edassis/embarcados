package com.example.embarcados;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.MatOfByte;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoWriter;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Color;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends CameraActivity implements CvCameraViewListener2 {
    private static final String TAG = "OCVSample::Activity";

    private CameraBridgeViewBase mOpenCvCameraView;
    private boolean mIsJavaCamera = true;
    private MenuItem mItemSwitchCamera = null;

    private Mat mCurrentRGBA;
    private Mat mCurrentGray;
    //    private MediaRecorder mRecorder;
    private VideoWriter mVideoWriter;
    private ImageView mVideoCameraButton;
    private boolean mIsRecording;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();

                    // initialize opencv variables on manager!
                    mCurrentRGBA = new Mat();
                    mCurrentGray = new Mat();
                    mIsRecording = false;
//        mRecorder = new MediaRecorder();
                    mVideoWriter = new VideoWriter();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /**
     * Called when the activity is first created.
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.main_java_surface_view);

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);

        mVideoCameraButton = (ImageView) findViewById(R.id.video_button);

        // Permits us to make image processing before saving
//        mRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
//        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
//        CamcorderProfile camcorderProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
//        mRecorder.setProfile(camcorderProfile);

        mVideoCameraButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    mIsRecording = !mIsRecording;
                    File videoFile;

                    if (mIsRecording) {
                        if (!mVideoWriter.isOpened()) {
                            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss", Locale.getDefault());
                            String curDateTime = sdf.format(new Date());

                            String fileName = "SampleVideo_" + curDateTime + ".avi";

//                            ContentValues values = new ContentValues();
//                            values.put(MediaStore.Video.Media.MIME_TYPE, "video/avi");
//                            values.put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
//                            values.put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis());
//                            values.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/" + "opencv");
//                            values.put(MediaStore.Video.Media.IS_PENDING, true);
//
//                            Uri uri = getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
//                            File root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
//                            File path = getExternalFilesDir(null);
                            File path = Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_PICTURES);
                            videoFile = new File(path, fileName);
                            if(!videoFile.exists()) {
                                try {
                                    videoFile.createNewFile();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
//                            File file = new File(uri);
//                            File opencvDir = commonDocumentDirPath("opencv");
//                            File file = new File(opencvDir, "fileName");
                            Log.d(TAG, "arquivo existe? " + fileName + " " + (videoFile.exists() ? "Sim" : "Não"));
                            Log.d(TAG, "pode escrever no arquivo " + fileName + " " + (videoFile.canWrite() ? "Sim" : "Não"));

                            Size screenSize = new Size(640, 480);
                            Log.d(TAG, "Video file path: " + videoFile.getAbsolutePath());
//                            mVideoWriter.open(uri.getPath(), VideoWriter.fourcc('M','J','P','G'), 30, screenSize);
                            mVideoWriter.open(videoFile.getAbsolutePath(), VideoWriter.fourcc('M', 'J', 'P', 'G'), 30, screenSize);
                            if (mVideoWriter.isOpened()) {
                                Log.d(TAG, "Sucesso ao abrir arquivo de video para escrita.");
                                Context context = getApplicationContext();
                                Toast.makeText(context, fileName + " aberto!", Toast.LENGTH_SHORT).show();
                            } else {
                                Log.d(TAG, "Erro ao abrir arquivo de video para escrita.");
                            }
                        }
                        mVideoCameraButton.setColorFilter(Color.RED);
                    } else { // Not recording (disable recording)
                        mVideoWriter.release();
                        mVideoCameraButton.setColorFilter(Color.WHITE);

                        // Tell the media scanner about the new file so that it is
                        // immediately available to the user.
                        MediaScannerConnection.scanFile(getApplicationContext(),
                                new String[] { mVideoWriter.toString() }, null,
                                new MediaScannerConnection.OnScanCompletedListener() {
                                    public void onScanCompleted(String path, Uri uri) {
                                        Log.i("ExternalStorage", "Scanned " + path + ":");
                                        Log.i("ExternalStorage", "-> uri=" + uri);
                                    }
                                }
                        );
                    }
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mOpenCvCameraView);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        Mat src = inputFrame.rgba().clone();
        Mat dst = new Mat();

//        double angle = 90;  // or 270
//        Size src_sz = src.size();
//        Size dst_sz = new Size(src_sz.height, src_sz.width);
//
//        int len = Math.max(src.cols(), src.rows());
//        Point center = new Point(len/2., len/2.);
//        Mat rot_mat = Imgproc.getRotationMatrix2D(center, angle, 1.0);
//        Imgproc.warpAffine(src, dst, rot_mat, dst_sz);

//        m_current_RGBA = dst;
        mCurrentRGBA = src;

        Imgproc.cvtColor(mCurrentRGBA, mCurrentGray, Imgproc.COLOR_RGBA2GRAY);

        if (mIsRecording) {
            saveVideo();
        }

        return mCurrentRGBA;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
//        super.onTouchEvent(event);
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            savePicture();
        }
        return true;
    }

    public void savePicture() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss", Locale.getDefault());
        String curDateTime = sdf.format(new Date());

        String fileName = "SampleImage_" + curDateTime + ".jpg";

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/" + "opencv");
        values.put(MediaStore.Images.Media.IS_PENDING, true);

        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri == null) return;

        try {
            OutputStream os = getContentResolver().openOutputStream(uri);

            MatOfByte matOfByte = new MatOfByte();
            Imgcodecs.imencode(".jpg", mCurrentGray, matOfByte);

            os.write(matOfByte.toArray());
            os.close();

            values.put(MediaStore.Images.Media.IS_PENDING, false);
            getContentResolver().update(uri, values, null, null);

            Log.d(TAG, "Imagem " + fileName + " salva!");
            Toast.makeText(this, fileName + " salva!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.d(TAG, "Falha ao salvar imagem.");
            e.printStackTrace();
            finish();
            System.exit(0);
        }
    }

    public void saveVideo() {
        mVideoWriter.write(mCurrentRGBA);
    }

    public static File commonDocumentDirPath(String FolderName) {
        File dir = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/" + FolderName);
        } else {
            dir = new File(Environment.getExternalStorageDirectory() + "/" + FolderName);
        }

        // Make sure the path directory exists.
        if (!dir.exists()) {
            // Make it, if it doesn't exit
            boolean success = dir.mkdirs();
            if (!success) {
                dir = null;
            }
        }
        return dir;
    }
}
