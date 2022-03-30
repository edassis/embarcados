package com.example.embarcados;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Point;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.MatOfByte;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoWriter;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Color;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends CameraActivity implements CvCameraViewListener2 {
    private static final String TAG = "OCVSample::Activity";
    private static final Scalar FACE_RECT_COLOR = new Scalar(0, 255, 0, 255);
    private static final Scalar ROI_RECT_COLOR = new Scalar(0, 0, 255, 255);

    private CameraBridgeViewBase mOpenCvCameraView;
    private boolean mIsJavaCamera = true;
    private MenuItem mItemSwitchCamera = null;

    private Mat mCurrentRGBA;
    private Mat mCurrentGray;
    //    private MediaRecorder mRecorder;
    private VideoWriter mVideoWriter;
    private ImageView mVideoCameraButton;
    private String mVideoFileName;
    private boolean mIsRecording;
    private File mCascadeFile;
    private CascadeClassifier mJavaDetector;
    private float                  mRelativeFaceSize   = 0.2f;
    private int                    mAbsoluteFaceSize   = 0;
    private Rect mROIRect;
    private ArrayList<Mat> mROIFrameBuffer;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");

                    // initialize opencv variables on manager!
                    mCurrentRGBA = new Mat();
                    mCurrentGray = new Mat();
                    mIsRecording = false;
//        mRecorder = new MediaRecorder();
                    mVideoWriter = new VideoWriter();
                    mROIRect = new Rect();
                    mROIFrameBuffer = new ArrayList<Mat>();

                    // Load native library after(!) OpenCV initialization
                    System.loadLibrary("detection_based_tracker");

                    try {
                        // load cascade file from application resources
                        InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
                        FileOutputStream os = new FileOutputStream(mCascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                        if (mJavaDetector.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaDetector = null;
                        } else
                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());

                        cascadeDir.delete();

                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
                    }

                    mOpenCvCameraView.enableView();
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
        mOpenCvCameraView.setCameraIndex(1);        // Frontal camera
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

                    if (mIsRecording) {
                        if (!mVideoWriter.isOpened()) {
                            _openVideoFile();
                        }
                    } else { // Not recording (disable recording)
                        if(mVideoWriter.isOpened()) {
                            _closeVideoFile();
                        }
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
        mCurrentRGBA = inputFrame.rgba().clone();
        Imgproc.cvtColor(mCurrentRGBA, mCurrentGray, Imgproc.COLOR_RGBA2GRAY);

//        // Flip 90 degrees anti clockwise
//        Mat mRGBAT = mCurrentRGBA.t();
//        Core.flip(mRGBAT, mRGBAT, 1);
//        Imgproc.resize(mRGBAT, mRGBAT,  mCurrentRGBA.size());
//        mCurrentRGBA = mRGBAT;

//        // Draw rectangle on screen
//        int w = mCurrentRGBA.width();
//        int h = mCurrentRGBA.height();
//        Imgproc.rectangle(mCurrentRGBA, new Point( w/2-100, h/2-100),
//                new Point(w/2+100, h/2+100),
//                new Scalar( 255, 0, 0 ), 3);

        // Face detection
        if (mAbsoluteFaceSize == 0) {
            int height = mCurrentGray.rows();
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }
        }

        MatOfRect faces = new MatOfRect();

        if (mJavaDetector != null)
                mJavaDetector.detectMultiScale(mCurrentGray, faces, 1.1, 2, 2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
                        new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());


        Rect[] facesArray = faces.toArray();
        for (int i = 0; i < facesArray.length; i++) {
            double[] roi_tl = {facesArray[i].tl().x + 100, facesArray[i].tl().y + 100};
            double[] roi_br = {facesArray[i].br().x - 100, facesArray[i].br().y - 100};

            if(i == 0) {      // First iteration determines ROI to save
                mROIRect = new Rect(new Point(roi_tl), new Point(roi_br));

                if(mROIFrameBuffer.size() >= 2) {
                    Mat aux = _getROIFrame(mCurrentRGBA);
                    mROIFrameBuffer.set(0, mROIFrameBuffer.get(1));
                    mROIFrameBuffer.set(1, aux);
                } else {
                    mROIFrameBuffer.add(_getROIFrame(mCurrentRGBA));
                }
            }

            Imgproc.rectangle(mCurrentRGBA, new Point(roi_tl), new Point(roi_br), ROI_RECT_COLOR, 2);
            Imgproc.rectangle(mCurrentRGBA, facesArray[i].tl(), facesArray[i].br(), FACE_RECT_COLOR, 3);
        }

        // Store frames
        if (mIsRecording) {
            saveVideoFrame();
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
        Uri uriB = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        Uri uriR = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        if (uri == null) return;

        try {
            OutputStream os = getContentResolver().openOutputStream(uri);

            Mat frameToStore = new Mat();
            Imgproc.cvtColor(mCurrentRGBA, frameToStore, Imgproc.COLOR_RGBA2BGR);
//
//            int width = frameToStore.width();
//            int height = frameToStore.height();

//            Rect roi = new Rect(width/2-100, height/2-100, 200, 200);
//            Mat cropped = _getROIFrame(frameToStore);

//            List<Mat> bgr = new ArrayList<Mat>(3);
//            Core.split(cropped, bgr);

            MatOfByte matOfByte = new MatOfByte();
            Imgcodecs.imencode(".jpg", frameToStore, matOfByte);
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

    public void saveVideoFrame() {
        mVideoWriter.write(mCurrentRGBA);
    }

    public void _saveROI() {
        // Ensures at least 2 frames in buffer
        if(mROIFrameBuffer.size() < 2) return;

        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss", Locale.getDefault());
        String curDateTime = sdf.format(new Date());
        String ROIFileName = "ROI_" + curDateTime + ".jpg";
//        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/" + "opencv");
        values.put(MediaStore.Images.Media.IS_PENDING, true);

        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        // Get frame
        Mat ROIFrame1 = mROIFrameBuffer.get(0);
        Mat ROIFrame2 = mROIFrameBuffer.get(1);
        Mat ROIResult = new Mat();

        // Get frames' difference
        Core.subtract(ROIFrame2, ROIFrame1, ROIResult);

        List<Mat> bgr = new ArrayList<Mat>(3);
        Core.split(ROIResult, bgr);

        // Store into file
        try {
            OutputStream os = getContentResolver().openOutputStream(uri);

            MatOfByte matOfByte = new MatOfByte();

            Imgproc.cvtColor(ROIResult, ROIResult, Imgproc.COLOR_RGBA2BGR);

            Imgcodecs.imencode(".jpg", ROIResult, matOfByte);
            os.write(matOfByte.toArray());

            os.close();

            values.put(MediaStore.Images.Media.IS_PENDING, false);
            getContentResolver().update(uri, values, null, null);

            Log.d(TAG, "Imagem " + ROIFileName + " salva!");
            Toast.makeText(this, ROIFileName + " salva!", Toast.LENGTH_SHORT).show();
        } catch(Exception e) {
            Log.d(TAG, "Falha ao salvar ROI.");
            e.printStackTrace();
            finish();
            System.exit(0);
        }

        mROIFrameBuffer.clear();
    }

    public void _openVideoFile() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss", Locale.getDefault());
        String curDateTime = sdf.format(new Date());

        mVideoFileName = "SampleVideo_" + curDateTime + ".avi";

        File path = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File videoFile = new File(path, mVideoFileName);
        if(!videoFile.exists()) {
            try {
                videoFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Log.d(TAG, "arquivo existe " + mVideoFileName + ": " + (videoFile.exists() ? "Sim" : "Não"));
        Log.d(TAG, "pode escrever no arquivo " + mVideoFileName + ": " + (videoFile.canWrite() ? "Sim" : "Não"));

        Size screenSize = new Size(mCurrentRGBA.width(), mCurrentRGBA.height());
        Log.d(TAG, "Video file path: " + videoFile.getAbsolutePath());

        mVideoWriter.open(videoFile.getAbsolutePath(), VideoWriter.fourcc('M', 'J', 'P', 'G'), 10, screenSize);

        if (mVideoWriter.isOpened()) {
            Log.d(TAG, "Sucesso ao abrir arquivo de video para escrita.");
            Toast.makeText(getApplicationContext(), mVideoFileName + " aberto!", Toast.LENGTH_SHORT).show();
        } else {
            Log.d(TAG, "Erro ao abrir arquivo de video para escrita.");
        }

        mVideoCameraButton.setColorFilter(Color.RED);
    }

    public void _closeVideoFile() {
        Toast.makeText(getApplicationContext(), mVideoFileName + " salvo!", Toast.LENGTH_SHORT).show();
        mVideoWriter.release();

        // Tell the media scanner about the new file so that it is
        // immediately available to the user.
        MediaScannerConnection.scanFile(getApplicationContext(),
                new String[]{mVideoWriter.toString()}, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("ExternalStorage", "Scanned " + path + ":");
                        Log.i("ExternalStorage", "-> uri=" + uri);
                    }
                }
        );
        mVideoCameraButton.setColorFilter(Color.WHITE);
    }


    public Mat _getROIFrame(Mat fullFrame) {
        return new Mat(fullFrame, mROIRect);
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
