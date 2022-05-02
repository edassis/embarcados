package com.example.embarcados;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
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
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Set;

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
    private Queue<Mat> mROIFrameBuffer;

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
                    mROIFrameBuffer = new LinkedList<Mat>();

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
//        mOpenCvCameraView.setCameraIndex(1);        // Frontal camera
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
            Imgproc.rectangle(mCurrentRGBA, facesArray[i].tl(), facesArray[i].br(), FACE_RECT_COLOR, 3);
        }

        if(facesArray.length > 0 && facesArray[0].width > 200  && facesArray[0].height > 200) {      // First iteration determines ROI to save
            Point center = new Point(new double[]{facesArray[0].x + facesArray[0].width/2.0,
                    facesArray[0].y + facesArray[0].height/2.0});
            double[] roi_tl = {center.x - 80, facesArray[0].tl().y};
            double[] roi_br = {center.x + 80, facesArray[0].tl().y + 70};
            mROIRect = new Rect(new Point(roi_tl), new Point(roi_br));

            mROIFrameBuffer.add(new Mat(mCurrentRGBA, mROIRect));
            if(mROIFrameBuffer.size() > 2) {
                mROIFrameBuffer.remove();
            }

            Imgproc.rectangle(mCurrentRGBA, new Point(roi_tl), new Point(roi_br), ROI_RECT_COLOR, 2);
        }


        // Store frames
        if (mIsRecording) {
            _saveVideoFrame();
        }

        return mCurrentRGBA;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
//        super.onTouchEvent(event);
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            savePicture();
            saveROI();
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

    public void _saveVideoFrame() {
        mVideoWriter.write(mCurrentRGBA);
    }

    public void saveROI() {
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
        Mat ROIFrame1 = mROIFrameBuffer.remove();
        Mat ROIFrame2 = mROIFrameBuffer.remove();
        Mat ROIResult = new Mat();

        // Get frames' difference
        Core.subtract(ROIFrame2, ROIFrame1, ROIResult);

        // DCT red/blue
        ArrayList<Mat> dcts = _getDCT(ROIResult);

        // Diagonal traverse
        ArrayList<ArrayList<Double>> peaksPanels = new ArrayList<>();
        for(int i = 0; i < dcts.size(); i++) {
            double[] diagTraverse = _ZigZag(dcts.get(i));

            // put 0 in the last 20% values
            for (int j = (int)(0.8*diagTraverse.length+1); j < diagTraverse.length; j++) {
//            Log.d(TAG, "Before: "+diagTraverse[i]);
                diagTraverse[j] = 0;
//            Log.d(TAG, "After: "+diagTraverse[i]);
            }
            ArrayList<Double> peaks = _getPeaks(diagTraverse);
            peaksPanels.add(peaks);
        }

        double stO2 = _getSaturation(peaksPanels.get(0), peaksPanels.get(1));

        // save array into file
//        StringBuilder log = new StringBuilder();
//        for (double v : diagTraverse) {
//            log.append(String.format("%.4f ", v));
//        }
//        saveToText(log.toString());

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

    public ArrayList<Mat> _getDCT(Mat frame) {
        ArrayList<Mat> dcts = new ArrayList<>();
        List<Mat> bgr = new ArrayList<Mat>(3);
        Core.split(frame, bgr);

        ArrayList<Mat> planes = new ArrayList<Mat>();
        planes.add(bgr.get(2));
        planes.add(bgr.get(0));

//        Mat ROIGray = new Mat();
//        Imgproc.cvtColor(ROIResult, ROIGray, Imgproc.COLOR_RGBA2GRAY);

        int m = Core.getOptimalDFTSize(frame.rows());
        int n = Core.getOptimalDFTSize(frame.cols());

        for(int i = 0; i < 2; i++) {
            Mat padded = new Mat();
            Core.copyMakeBorder(planes.get(i), padded, 0, m - frame.rows(), 0, n - frame.cols(), Core.BORDER_CONSTANT, Scalar.all(0));
            padded.convertTo(padded, CvType.CV_32F, 1./255, 0);

            Mat ROIDCT = new Mat();
            Core.dct(padded, ROIDCT);
            dcts.add(ROIDCT);
        }

        return dcts;
    }

    public double[] _ZigZag(Mat matrix) {
//        // Check for empty matrices
//        if (matrix == null || matrix.length == 0) {
//            return new int[0];
//        }
        // Variables to track the size of the matrix
        int N = matrix.rows();
        int M = matrix.cols();

        // Indices that will help us progress through
        // the matrix, one element at a time.
        int row = 0, column = 0;

        // As explained in the article, this is the variable
        // that helps us keep track of what direction we are
        // processing the current diagonal
        int direction = 1;

        // The final result array
        double[] result = new double[N*M];
        int r = 0;

        // The uber while loop which will help us iterate over all
        // the elements in the array.
        while (row < N && column < M) {
            // First and foremost, add the current element to
            // the result matrix.
            double[] pixel = matrix.get(row, column);
            result[r++] = pixel[0]; // GreyScale so value in first channel.

            // Move along in the current diagonal depending upon
            // the current direction.[i, j] -> [i - 1, j + 1] if
            // going up and [i, j] -> [i + 1][j - 1] if going down.
            int new_row = row + (direction == 1 ? -1 : 1);
            int new_column = column + (direction == 1 ? 1 : -1);

            // Checking if the next element in the diagonal is within the
            // bounds of the matrix or not. If it's not within the bounds,
            // we have to find the next head.
            if (new_row < 0 || new_row == N || new_column < 0 || new_column == M) {

                // If the current diagonal was going in the upwards
                // direction.
                if (direction == 1) {

                    // For an upwards going diagonal having [i, j] as its tail
                    // If [i, j + 1] is within bounds, then it becomes
                    // the next head. Otherwise, the element directly below
                    // i.e. the element [i + 1, j] becomes the next head
                    row += (column == M - 1 ? 1 : 0) ;
                    column += (column < M - 1 ? 1 : 0);
                } else {
                    // For a downwards going diagonal having [i, j] as its tail
                    // if [i + 1, j] is within bounds, then it becomes
                    // the next head. Otherwise, the element directly below
                    // i.e. the element [i, j + 1] becomes the next head
                    column += (row == N - 1 ? 1 : 0);
                    row += (row < N - 1 ? 1 : 0);
                }
                // Flip the direction
                direction = 1 - direction;

            } else {
                row = new_row;
                column = new_column;
            }
        }
        return result;
    }

    public ArrayList<Double> _getPeaks(double[] diagTraverse) {
        // Get 5 greater peaks
        Set<Integer> peaks_idx = new HashSet<Integer>();
        for (int k = 0; k < 5; k++) {   // 5 passes
            int max_idx = -1;    // Guarantee an valid max
            double max = -1;

            for (int i = 0; i < diagTraverse.length; i++) { // Traverse vector
                if (max_idx == -1 || diagTraverse[i] > max) { // New peak candidate
                    boolean isPeak = true;
                    for (int j = 1; j <= 7; j++) { // Check neighbours to guarantee 7px dist
                        if (peaks_idx.contains(i-j) || peaks_idx.contains(i) || peaks_idx.contains(i+j)) {
                            isPeak = false;
                            break;
                        }
                    }

                    if(isPeak) {
                        max_idx = i;
                        max = diagTraverse[max_idx];
                    }
                }
            }
            peaks_idx.add(max_idx);
        }

        ArrayList<Double> peaks = new ArrayList<Double>();
        for(int idx : peaks_idx) {
            Log.d(TAG, "Peaks: "+diagTraverse[idx]);
            peaks.add(diagTraverse[idx]);
        }
        // peaks on descending order
        Collections.sort(peaks, Collections.reverseOrder());

        return peaks;
    }

    public double _getSaturation(ArrayList<Double> peaksRed, ArrayList<Double> peaksBlue) {
        // DC
        double dcRed = peaksRed.get(0);
        double dcBlue = peaksBlue.get(0);

//        Log.d(TAG, "DC " + dc);
        // AC
        double acRed = 0;
        double acBlue = 0;
        for(int i = 1; i < peaksRed.size(); i++) {
//            Log.d(TAG, "Peak "+i+": "+peaks.get(i));
            acRed += peaksRed.get(i);
        }
        acRed /= peaksRed.size();

        for(int i = 1; i < peaksBlue.size(); i++) {
            acBlue += peaksBlue.get(i);
        }
        acBlue /= peaksBlue.size();

//        Log.d(TAG, "AC " + ac);
        // R
        double R = (dcRed/acRed)/(dcBlue/acBlue);
        // StO2
        double stO2 = 100 - R * 0.015;
        Log.d(TAG, "SatO2 " + stO2);

        return stO2;
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

    public void saveToText(String content) {
        File videoFile = openFile("Log");

        try {
            FileWriter writer = new FileWriter(videoFile);
            writer.append(content);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Toast.makeText(this, "Data has been written to Report File", Toast.LENGTH_SHORT).show();
    }

    public static File openFile(String fileName) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss", Locale.getDefault());
        String curDateTime = sdf.format(new Date());

        fileName = fileName + "_" + curDateTime + ".txt";

        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File videoFile = new File(path, fileName);

        if(!videoFile.exists()) {
            try {
                videoFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return videoFile;
    }
}
