package com.example.android.remedicappml;

import static java.lang.Math.ceil;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class CropImage {

    private static final String TAG = "CropImage";
    private static final String VIDEO_DIRECTORY_NAME = "RemedicApp";

    private List<Face> faces;
    private Bitmap bitmap;

    public ViewModel vm;

    // Freq + timer variable
    private static long startTime = 0;
    private double SamplingFreq;

    // SPO2 variables
    private static double RedBlueRatio = 0;
    double Stdr = 0;
    double Stdb = 0;
    double sumred = 0;
    double sumblue = 0;
    public int o2;

    // Arraylist
    public ArrayList<Double> RedAvgList = new ArrayList<Double>();
    public ArrayList<Double> BlueAvgList = new ArrayList<Double>();
    public int counter = 0;

    private Context context;
    private final ArrayList<String> vitals = new ArrayList<String>(2);

    public CropImage() {
        vitals.add(0, "0.0");
        vitals.add(1, "0.0");
    }

    public ArrayList<String> setVariables(ByteBuffer data, FrameMetadata frameMetadata,
                                          List<Face> faces, ViewModel vm) {
        this.faces = faces;
        this.vm = vm;

        for (Face face: faces) {
            if (data == null) {
                Log.d(TAG, "Image Byte Buffer was NULL !! ");
            } else {
                this.bitmap = BitmapUtils.getBitmap(data, frameMetadata);
                // if (this.bitmap != null) writeToDirectory();
                return convertToMatrix(this.bitmap, face);
            }
        }
        return vitals;
    }

    public ArrayList<String> convertToMatrix(Bitmap bitmap, Face face) {
        MatOfPoint mPoints = new MatOfPoint();
        Mat src = new Mat();
        Utils.bitmapToMat(bitmap, src);

        Mat mask =  new Mat(new Size(src.cols(), src.rows()), CvType.CV_8UC1);
        mask.setTo(new Scalar(0.0));
        Scalar white = new Scalar(255, 255, 255);

        // Mat gray = new Mat();
        // Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGBA2GRAY);

        // Imgproc.Canny(gray, gray, 50, 200);
        // List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        // Mat hierarchy = new Mat();

        // find contours:
        // Imgproc.findContours(gray, contours, hierarchy,
        // Imgproc.RETR_TREE,Imgproc.CHAIN_APPROX_SIMPLE);

        // List<FaceContour> contours = getAllContours(face);

        // for (int contourIdx = 0; contourIdx < contours.size(); contourIdx++) {
        // Imgproc.drawContours(src, contours, contourIdx,
        // new Scalar(0, 0, 255), -1);
        // }
        // Log.d(TAG, String.valueOf(contours));

        // create a blank temp bitmap:
        // Bitmap tempBmp1 = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(),
                                                // bitmap.getConfig());
        // Utils.matToBitmap(src, tempBmp1);
        // writeToDirectory(tempBmp1);

        // for (FaceContour contoursFace: face.getAllContours()) {
        //    if (contoursFace.getFaceContourType() == 1)
        //       mPoints = convertToMOP(contoursFace);
        //       Imgproc.fillConvexPoly(mask, mPoints, white);
        // }

        PointF forehead_ul = new PointF(0, 0);
        PointF forehead_ur = new PointF(0, 0);
        PointF forehead_bl = new PointF(0, 0);
        PointF forehead_br = new PointF(0, 0);

        MatOfPoint polyPoints = new MatOfPoint();
        List<Point> points = new ArrayList<>();

        // Draws all face contours.
        for (FaceContour contour : face.getAllContours()) {
            int count = 0;
            for (PointF point : contour.getPoints()) {
                if (contour.getFaceContourType() == 1 && count == 33) {
                    forehead_ul = point;
                    points.add(new Point((int) point.x, (int) point.y));
                }

                if (contour.getFaceContourType() == 1 && count == 2) {
                    forehead_ur = point;
                    points.add(new Point((int) point.x, (int) point.y));
                }

                if (contour.getFaceContourType() == 2 && count == 2) {
                    forehead_bl = point;
                    points.add(new Point((int) point.x, (int) point.y));
                }

                if (contour.getFaceContourType() == 4 && count == 2) {
                    forehead_br = point;
                    points.add(new Point((int) point.x, (int) point.y));
                }
                count++;
            }
        }

        polyPoints.fromList(points);
        Imgproc.fillConvexPoly(mask, polyPoints, white);

        List<Mat> lab_list = new ArrayList(3);
        Core.split(src, lab_list);

        Scalar RedAvg = Core.mean(lab_list.get(2), mask);
        // Log.d(TAG, String.valueOf(RedAvg));
        RedAvgList.add(RedAvg.val[0]);

        long endTime = System.currentTimeMillis();
        double totalTimeInSecs = (endTime - startTime) / 1000d; //to convert time to seconds
        counter++;

        if (totalTimeInSecs >= 10) { //when 30 seconds of measuring passes do the following " we chose 30 seconds to take half sample since 60 seconds is normally a full sample of the heart beat

            startTime = System.currentTimeMillis();
            SamplingFreq = (counter / totalTimeInSecs);
            //Double[] Red = RedAvgList.toArray(new Double[RedAvgList.size()]);
            //Double[] Blue = BlueAvgList.toArray(new Double[BlueAvgList.size()]);
            Double[] Red = RedAvgList.toArray(new Double[RedAvgList.size()]);
            double HRFreq = FFT.FFT(Red, counter, SamplingFreq);
            double bpm = (int) ceil(HRFreq * 60);


            counter = 0;
            // Log.d(TAG, Thread.currentThread().getName());
            Log.d(TAG, "Heart Beat (per minute) : " + (String.valueOf(bpm)));
            vitals.set(0, String.valueOf(bpm));
            vitals.set(1, "0.0");

        }
        Log.d(TAG, String.valueOf(vitals));
        return vitals;

        // Core.bitwise_and(lab_list.get(0), mask, lab_list.get(0));
        // Core.bitwise_and(lab_list.get(1), mask, lab_list.get(1));
        // Core.bitwise_and(lab_list.get(2), mask, lab_list.get(2));
        // Core.merge(lab_list, mask);

         // fillArea(src, mPoints);
         // create a blank temp bitmap:
         // Bitmap tempBmp1 = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(),
         // bitmap.getConfig());
         // Utils.matToBitmap(mask, tempBmp1);
         // writeToDirectory(tempBmp1);
    }

    public void fillArea(Mat src, MatOfPoint points) {
        Scalar color = new Scalar(0, 0, 255);
        Imgproc.fillConvexPoly (src, points, color);
    }

    // public List<FaceContour> getAllContours(Face face) {
    //    return face.getAllContours();
    // }

    /*  Save the byte array to a local file
     *  Create directory and return file returning video file
     */
    public static File getOutputMediaFile() {
        // External sdcard file location
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory(),
                VIDEO_DIRECTORY_NAME);
        // Create storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(TAG, "Oops! Failed create "
                        + VIDEO_DIRECTORY_NAME + " directory");
                return null;
            }
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(new Date());
        File mediaFile;

        mediaFile = new File(mediaStorageDir.getPath() + File.separator
                + "IMAGE_" + timeStamp + ".bmp");
        return mediaFile;
    }

    public void writeToDirectory(Bitmap bm) {

        File file = getOutputMediaFile();
        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        try {
            bm.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            Log.e(TAG, "Could not write to the storage file.", e);
        }
    }

    public MatOfPoint convertToMOP(FaceContour contours) {
        MatOfPoint mPoints = new MatOfPoint();
        List<Point> points = new ArrayList<>();

        for (PointF c : contours.getPoints()) {
            points.add(new Point((int) c.x, (int) c.y));
        }

        // Log.d(TAG, String.valueOf(points));
        // Log.d(TAG, String.valueOf(contours));

        mPoints.fromList(points);
        return mPoints;
    }
}
