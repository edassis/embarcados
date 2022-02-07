package com.example.embarcados;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import org.opencv.videoio.VideoCapture;

import java.net.URI;
import java.net.URISyntaxException;


// Load OpenCV native library before using:
//
// - avoid using of "OpenCVLoader.initAsync()" approach - it is deprecated
//   It may load library with different version (from OpenCV Android Manager, which is installed separately on device)
//
// - use "System.loadLibrary("opencv_java4")" or "OpenCVLoader.initDebug()"

public class MainActivity extends AppCompatActivity {
    // Define the pic id
    private static final int REQUEST_VIDEO_CAPTURE = 666;
//    private static final int REQUEST_OK = 0;

    // Define the button and imageview type variable
    Button camera_open_id;
    ImageView click_image_id;

//    private static final Uri videoUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Load opencv library
        System.loadLibrary("opencv_java4");

        // By ID we can get each component
        // which id is assigned in XML file
        // get Buttons and imageview.
        camera_open_id = (Button)findViewById(R.id.camera_button);
        click_image_id = (ImageView)findViewById(R.id.click_image);

        // Camera_open button is for open the camera
        // and add the setOnClickListener in this button
        camera_open_id.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakeVideoIntent();
            }
        });
    }

    private void dispatchTakeVideoIntent() {
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
//        if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
//        }
    }

    // This method will help to retrieve the image
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        if (requestCode == REQUEST_VIDEO_CAPTURED && resultCode == RESULT_OK) {

        // Match the request 'pic id with requestCode
        super.onActivityResult(requestCode, resultCode, data);
//        Log.d("Debug",String.valueOf(requestCode));
//        Log.d("Debug",String.valueOf(data));

        Uri videoUri = data.getData();

        Log.d("Debug", videoUri.getEncodedPath());

//        // Play video on default application
//        Intent openVid = new Intent(Intent.ACTION_VIEW);
//        openVid.setDataAndType(vid, "video/*");
//        openVid.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);//DO NOT FORGET THIS EVER
//        startActivity(openVid);

        // Get video path

//        /* Try to open the file for "read" access using the
//        * returned URI. If the file isn't found, write to the
//        * error log and return.
//        */
//        ParcelFileDescriptor inputPFD;
//        try {
//            /*
//             * Get the content resolver instance for this context, and use it
//             * to get a ParcelFileDescriptor for the file.
//             */
//            inputPFD = getContentResolver().openFileDescriptor(videoUri, "r");
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//            Log.d("Debug", "File not found.");
//            return;
//        }
//        // Get a regular file descriptor for the file
//        FileDescriptor videoFD = inputPFD.getFileDescriptor();
//        FileInputStream videoInputStream = new FileInputStream(videoFD);
        // What to do with fileinputstream? Better get file's string path.
        URI videoJavaURI = null;
        try {
            videoJavaURI = new URI(videoUri.getPath());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        VideoCapture videoCap = new VideoCapture(videoUri.getEncodedPath());
        if(videoCap.isOpened()) {
            Log.d("Debug", "Video capture opened.");
        } else {
            Log.d("Debug", "Video capture fail.");
        }

        // Extract 2 frames from files using opencv

//        if (requestCode == pic_id) {
//            // BitMap is data structure of image file
//            // which stor the image in memory
//            Bitmap photo = (Bitmap) data.getExtras().get("data");
//
//            // Set the image in imageview for display
//            click_image_id.setImageBitmap(photo);
//        }
    }
}