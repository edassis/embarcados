package com.example.embarcados;


import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

public class OpenCVUtils {
    public OpenCVUtils() {
        // Load opencv library
        System.loadLibrary("opencv_java4");
    }

    public void ExtractFrames(String args[]) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        VideoCapture cap = new VideoCapture();

        String input = "/Users/Jan/Desktop/Video/Java.mp4";
        String output = "/Users/Jan/Desktop/Video/Output";

        cap.open(input);

        int video_length = (int) cap.get(Videoio.CAP_PROP_FRAME_COUNT);
        int frames_per_second = (int) cap.get(Videoio.CAP_PROP_FPS);
        int frame_number = (int) cap.get(Videoio.CAP_PROP_POS_FRAMES);

        Mat frame = new Mat();

        if (cap.isOpened()) {
            System.out.println("Video is opened");
            System.out.println("Number of Frames: " + video_length);
            System.out.println(frames_per_second + " Frames per Second");
            System.out.println("Converting Video...");

            cap.read(frame);

            while (frame_number <= video_length) {
                Imgcodecs.imwrite(output + "/" + frame_number + ".jpg", frame);
                frame_number++;
            }
            cap.release();

            System.out.println(video_length + " Frames extracted");

        } else {
            System.out.println("Fail");
        }
    }
}