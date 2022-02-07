package com.example.embarcados;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.WindowManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

public class Frames extends CameraActivity implements CvCameraViewListener2 {
    private static final String TAG = "OCVSample::Activity";

    private CameraBridgeViewBase mOpenCvCameraView;
    private boolean              mIsJavaCamera = true;
    private MenuItem             mItemSwitchCamera = null;

    private Vector<Mat> mRGBA;
    private Vector<Mat> mGray;
    
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public Frames() {
        mRGBA = new Vector<Mat>();
        mGray = new Vector<Mat>();

        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.tutorial1_surface_view);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_java_surface_view);

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
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
        mRGBA.add(inputFrame.rgba().clone());

        Mat grayFrame = new Mat();
        Imgproc.cvtColor(inputFrame.rgba(), grayFrame, Imgproc.COLOR_RGBA2GRAY);
        mGray.add(grayFrame);

        File file = new File(Environment.getExternalStorageDirectory() + "/SampleImage_"
                + mRGBA.size()+".png");

        Log.d(TAG, "Arquivo existe: " + file.exists());
        Log.d(TAG, "Escrever no arquivo: " + file.canWrite());

        if (!file.exists()) {
            try {
                boolean success = file.createNewFile();
                Log.d(TAG, "Arquivo criado: " + success);
            } catch (IOException e) {
                Log.d(TAG, "Falha ao criar arquivo.");
                e.printStackTrace();
            }
        }

        Log.d(TAG, "Escrever no arquivo2: " + file.canWrite());
//        file.setWritable(true);

        FileOutputStream fileOS = null;
        try {
            fileOS = new FileOutputStream(file);

            MatOfByte matOfByte = new MatOfByte();
            Imgcodecs.imencode(".png", grayFrame, matOfByte);

            fileOS.write(matOfByte.toArray());
            fileOS.close();
        } catch (Exception e) {
            Log.d(TAG, "Falha ao salvar imagem.");
            e.printStackTrace();
        }

        return inputFrame.rgba();
    }
}

// Last visited questions
// https://www.tutorialspoint.com/how-to-convert-opencv-mat-object-to-bufferedimage-object-using-java
// https://stackoverflow.com/questions/28426927/mat-to-byte-conversion-not-working-in-java
// https://stackoverflow.com/questions/37209656/android-using-outputstream-and-mat-buffer-to-create-a-mp4-video
// https://stackoverflow.com/questions/43611426/how-to-convert-mat-to-byte-array-store-the-value-and-then-convert-it-back-again
// https://www.android-examples.com/save-store-image-to-external-storage-android-example-tutorial/

// Pass image bundled with Intent is limited to 500KB
// https://stackoverflow.com/questions/38551452/android-give-image-with-intent
