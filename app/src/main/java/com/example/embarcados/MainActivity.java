package com.example.embarcados;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;


// Load OpenCV native library before using:
//
// - avoid using of "OpenCVLoader.initAsync()" approach - it is deprecated
//   It may load library with different version (from OpenCV Android Manager, which is installed separately on device)
//
// - use "System.loadLibrary("opencv_java4")" or "OpenCVLoader.initDebug()"

public class MainActivity extends AppCompatActivity {
    // Define the pic id
    private static final int pic_id = 666;

    // Define the button and imageview type variable
    Button camera_open_id;
    ImageView click_image_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
                // Create the camera_intent ACTION_IMAGE_CAPTURE
                // it will open the camera for capture the image
                Intent camera_intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);

                // Start the activity with camera_intent,
                // and request pic id
                startActivityForResult(camera_intent, pic_id);
            }
        });
    }

    // This method will help to retrieve the image
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Match the request 'pic id with requestCode
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("D","FON " + String.valueOf(requestCode));
        Log.d("D","FON2 " + String.valueOf(data));

        Uri vid = data.getData();

        Intent openVid = new Intent(Intent.ACTION_VIEW);
        openVid.setDataAndType(vid, "video/*");
        openVid.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);//DO NOT FORGET THIS EVER
        startActivity(openVid);

        if (requestCode == pic_id) {
//            // BitMap is data structure of image file
//            // which stor the image in memory
//            Bitmap photo = (Bitmap) data.getExtras().get("data");
//
//            // Set the image in imageview for display
//            click_image_id.setImageBitmap(photo);
        }
    }
}