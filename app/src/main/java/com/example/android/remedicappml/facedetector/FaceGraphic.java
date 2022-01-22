package com.example.android.remedicappml.facedetector;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;

import com.example.android.remedicappml.GraphicOverlay;
import com.example.android.remedicappml.GraphicOverlay.Graphic;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceLandmark;
import com.google.mlkit.vision.face.FaceLandmark.LandmarkType;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Graphic instance for rendering face position, contour, and landmarks within the associated
 * graphic overlay view.
 */
public class FaceGraphic extends Graphic {
    private static final float FACE_POSITION_RADIUS = 8.0f;
    private static final float ID_TEXT_SIZE = 30.0f;
    private static final float ID_Y_OFFSET = 40.0f;
    private static final float BOX_STROKE_WIDTH = 5.0f;
    private static final int NUM_COLORS = 10;
    private static final int[][] COLORS =
            new int[][] {
                    // {Text color, background color}
                    {Color.BLACK, Color.WHITE},
                    {Color.WHITE, Color.MAGENTA},
                    {Color.BLACK, Color.LTGRAY},
                    {Color.WHITE, Color.RED},
                    {Color.WHITE, Color.BLUE},
                    {Color.WHITE, Color.DKGRAY},
                    {Color.BLACK, Color.CYAN},
                    {Color.BLACK, Color.YELLOW},
                    {Color.WHITE, Color.BLACK},
                    {Color.BLACK, Color.GREEN}
            };

    private final Paint facePositionPaint;
    private final Paint[] idPaints;
    private final Paint[] boxPaints;
    private final Paint[] labelPaints;

    private volatile Face face;

    FaceGraphic(GraphicOverlay overlay, Face face) {
        super(overlay);

        this.face = face;
        final int selectedColor = Color.WHITE;

        facePositionPaint = new Paint();
        facePositionPaint.setColor(selectedColor);

        int numColors = COLORS.length;
        idPaints = new Paint[numColors];
        boxPaints = new Paint[numColors];
        labelPaints = new Paint[numColors];
        for (int i = 0; i < numColors; i++) {
            idPaints[i] = new Paint();
            idPaints[i].setColor(COLORS[i][0] /* text color */);
            idPaints[i].setTextSize(ID_TEXT_SIZE);

            boxPaints[i] = new Paint();
            boxPaints[i].setColor(COLORS[i][1] /* background color */);
            boxPaints[i].setStyle(Paint.Style.STROKE);
            boxPaints[i].setStrokeWidth(BOX_STROKE_WIDTH);

            labelPaints[i] = new Paint();
            labelPaints[i].setColor(COLORS[i][1] /* background color */);
            labelPaints[i].setStyle(Paint.Style.FILL);
        }
    }

    /** Draws the face annotations for position on the supplied canvas. */
    @Override
    public void draw(Canvas canvas) {
        Face face = this.face;
        if (face == null) {
            return;
        }

        // Draws a circle at the position of the detected face, with the face's track id below.
        float x = translateX(face.getBoundingBox().centerX());
        float y = translateY(face.getBoundingBox().centerY());
        canvas.drawCircle(x, y, FACE_POSITION_RADIUS, facePositionPaint);

        // Calculate positions.
        float left = x - scale(face.getBoundingBox().width() / 2.0f);
        float top = y - scale(face.getBoundingBox().height() / 2.0f);
        float right = x + scale(face.getBoundingBox().width() / 2.0f);
        float bottom = y + scale(face.getBoundingBox().height() / 2.0f);
        float lineHeight = ID_TEXT_SIZE + BOX_STROKE_WIDTH;
        float yLabelOffset = (face.getTrackingId() == null) ? 0 : -lineHeight;

        // Decide color based on face ID
        int colorID = (face.getTrackingId() == null) ? 0 : Math.abs(face.getTrackingId() % NUM_COLORS);

        // Calculate width and height of label box
        float textWidth = idPaints[colorID].measureText("ID: " + face.getTrackingId());
        if (face.getSmilingProbability() != null) {
            yLabelOffset -= lineHeight;
            textWidth =
                    Math.max(
                            textWidth,
                            idPaints[colorID].measureText(
                                    String.format(Locale.US, "Happiness: %.2f", face.getSmilingProbability())));
        }
        if (face.getLeftEyeOpenProbability() != null) {
            yLabelOffset -= lineHeight;
            textWidth =
                    Math.max(
                            textWidth,
                            idPaints[colorID].measureText(
                                    String.format(
                                            Locale.US, "Left eye open: %.2f", face.getLeftEyeOpenProbability())));
        }
        if (face.getRightEyeOpenProbability() != null) {
            yLabelOffset -= lineHeight;
            textWidth =
                    Math.max(
                            textWidth,
                            idPaints[colorID].measureText(
                                    String.format(
                                            Locale.US, "Right eye open: %.2f", face.getRightEyeOpenProbability())));
        }

        yLabelOffset = yLabelOffset - 3 * lineHeight;
        textWidth =
                Math.max(
                        textWidth,
                        idPaints[colorID].measureText(
                                String.format(Locale.US, "EulerX: %.2f", face.getHeadEulerAngleX())));
        textWidth =
                Math.max(
                        textWidth,
                        idPaints[colorID].measureText(
                                String.format(Locale.US, "EulerY: %.2f", face.getHeadEulerAngleY())));
        textWidth =
                Math.max(
                        textWidth,
                        idPaints[colorID].measureText(
                                String.format(Locale.US, "EulerZ: %.2f", face.getHeadEulerAngleZ())));
        // Draw labels
        canvas.drawRect(
                left - BOX_STROKE_WIDTH,
                top + yLabelOffset,
                left + textWidth + (2 * BOX_STROKE_WIDTH),
                top,
                labelPaints[colorID]);
        yLabelOffset += ID_TEXT_SIZE;
        canvas.drawRect(left, top, right, bottom, boxPaints[colorID]);
        if (face.getTrackingId() != null) {
            canvas.drawText("ID: " + face.getTrackingId(), left, top + yLabelOffset, idPaints[colorID]);
            yLabelOffset += lineHeight;
        }

//        PointF forehead_ul = new PointF(0, 0);
//        PointF forehead_ur = new PointF(0, 0);
//        PointF forehead_bl = new PointF(0, 0);
//        PointF forehead_br = new PointF(0, 0);
//
//        // Draws all face contours.
//        for (FaceContour contour : face.getAllContours()) {
//            int count = 0;
//            for (PointF point : contour.getPoints()) {
//                // canvas.drawCircle(
//                //        translateX(point.x), translateY(point.y), FACE_POSITION_RADIUS, facePositionPaint);
//                if (contour.getFaceContourType() == 1 && count == 33) {
//                    forehead_ul = point;
//                    canvas.drawCircle(
//                            translateX(point.x), translateY(point.y),
//                            FACE_POSITION_RADIUS, facePositionPaint);
//                }
//
//                if (contour.getFaceContourType() == 1 && count == 2) {
//                    forehead_ur = point;
//                    canvas.drawCircle(
//                            translateX(point.x), translateY(point.y),
//                            FACE_POSITION_RADIUS, facePositionPaint);
//                }
//
//                if (contour.getFaceContourType() == 2 && count == 2) {
//                    forehead_bl = point;
//                    canvas.drawCircle(
//                            translateX(point.x), translateY(point.y),
//                            FACE_POSITION_RADIUS, facePositionPaint);
//                }
//
//                if (contour.getFaceContourType() == 4 && count == 2) {
//                    forehead_br = point;
//                    canvas.drawCircle(
//                            translateX(point.x), translateY(point.y),
//                            FACE_POSITION_RADIUS, facePositionPaint);
//                }
//
//                        // canvas.drawText(
//                //        String.valueOf(count), translateX(point.x), translateY(point.y), idPaints[colorID]);
//                count++;
//            }
//
////            Log.d("LineCoordinates",
////                    String.valueOf(forehead_ul.x) + " " +
////                    String.valueOf(forehead_ul.y) + " " +
////                    String.valueOf(forehead_ur.x) + " " +
////                    String.valueOf(forehead_ur.y)
////            );
//
//        }
//
////        forehead_ul.x = forehead_bl.x;
////        forehead_ur.x = forehead_br.x;
////        forehead_ur.y = forehead_ul.y;
//
//        // int[] xcoords = new ArrayList<Integer>(); // Create an ArrayList object
//        //        xcoords.add((int)forehead_ul.x);
//        //        xcoords.add((int)forehead_ur.x);
//        //        xcoords.add((int)forehead_br.x);
//        //        xcoords.add((int)forehead_bl.x);
//
//
//        int xcoords[] = new int[4];
//
//        xcoords[0] = (int)forehead_ul.x;
//        xcoords[1] = (int)forehead_ur.x;
//        xcoords[2] = (int)forehead_br.x;
//        xcoords[3] = (int)forehead_bl.x;
//
////        int[] ycoords = new ArrayList<Integer>(); // Create an ArrayList object
////        ycoords.add((int)forehead_ul.y);
////        ycoords.add((int)forehead_ur.y);
////        ycoords.add((int)forehead_br.y);
////        ycoords.add((int)forehead_bl.y);
//        int ycoords[] = new int[4];
//
//        ycoords[0] = (int)forehead_ul.y;
//        ycoords[1] = (int)forehead_ur.y;
//        ycoords[2] = (int)forehead_br.y;
//        ycoords[3] = (int)forehead_bl.y;
//
//
//        // Polygon polygon = new Polygon(xcoords, ycoords, 4);
//
//        canvas.drawLine(translateX(forehead_bl.x), translateY(forehead_bl.y),
//                translateX(forehead_br.x), translateY(forehead_br.y),
//                facePositionPaint);
//
//        canvas.drawLine(translateX(forehead_br.x), translateY(forehead_br.y),
//                translateX(forehead_ur.x), translateY(forehead_ur.y),
//                facePositionPaint);
//
//        canvas.drawLine(translateX(forehead_ur.x), translateY(forehead_ur.y),
//                translateX(forehead_ul.x), translateY(forehead_ul.y),
//                facePositionPaint);
//
//        canvas.drawLine(translateX(forehead_ul.x), translateY(forehead_ul.y),
//                translateX(forehead_bl.x), translateY(forehead_bl.y),
//                facePositionPaint);


        // Draws smiling and left/right eye open probabilities.
        if (face.getSmilingProbability() != null) {
            canvas.drawText(
                    "Smiling: " + String.format(Locale.US, "%.2f", face.getSmilingProbability()),
                    left,
                    top + yLabelOffset,
                    idPaints[colorID]);
            yLabelOffset += lineHeight;
        }

        FaceLandmark leftEye = face.getLandmark(FaceLandmark.LEFT_EYE);
        if (face.getLeftEyeOpenProbability() != null) {
            canvas.drawText(
                    "Left eye open: " + String.format(Locale.US, "%.2f", face.getLeftEyeOpenProbability()),
                    left,
                    top + yLabelOffset,
                    idPaints[colorID]);
            yLabelOffset += lineHeight;
        }
        if (leftEye != null) {
            float leftEyeLeft =
                    translateX(leftEye.getPosition().x) - idPaints[colorID].measureText("Left Eye") / 2.0f;
            canvas.drawRect(
                    leftEyeLeft - BOX_STROKE_WIDTH,
                    translateY(leftEye.getPosition().y) + ID_Y_OFFSET - ID_TEXT_SIZE,
                    leftEyeLeft + idPaints[colorID].measureText("Left Eye") + BOX_STROKE_WIDTH,
                    translateY(leftEye.getPosition().y) + ID_Y_OFFSET + BOX_STROKE_WIDTH,
                    labelPaints[colorID]);
            canvas.drawText(
                    "Left Eye",
                    leftEyeLeft,
                    translateY(leftEye.getPosition().y) + ID_Y_OFFSET,
                    idPaints[colorID]);
        }

        FaceLandmark rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE);
        if (face.getRightEyeOpenProbability() != null) {
            canvas.drawText(
                    "Right eye open: " + String.format(Locale.US, "%.2f", face.getRightEyeOpenProbability()),
                    left,
                    top + yLabelOffset,
                    idPaints[colorID]);
            yLabelOffset += lineHeight;
        }
        if (rightEye != null) {
            float rightEyeLeft =
                    translateX(rightEye.getPosition().x) - idPaints[colorID].measureText("Right Eye") / 2.0f;
            canvas.drawRect(
                    rightEyeLeft - BOX_STROKE_WIDTH,
                    translateY(rightEye.getPosition().y) + ID_Y_OFFSET - ID_TEXT_SIZE,
                    rightEyeLeft + idPaints[colorID].measureText("Right Eye") + BOX_STROKE_WIDTH,
                    translateY(rightEye.getPosition().y) + ID_Y_OFFSET + BOX_STROKE_WIDTH,
                    labelPaints[colorID]);
            canvas.drawText(
                    "Right Eye",
                    rightEyeLeft,
                    translateY(rightEye.getPosition().y) + ID_Y_OFFSET,
                    idPaints[colorID]);
        }

        canvas.drawText(
                "EulerX: " + face.getHeadEulerAngleX(), left, top + yLabelOffset, idPaints[colorID]);
        yLabelOffset += lineHeight;
        canvas.drawText(
                "EulerY: " + face.getHeadEulerAngleY(), left, top + yLabelOffset, idPaints[colorID]);
        yLabelOffset += lineHeight;
        canvas.drawText(
                "EulerZ: " + face.getHeadEulerAngleZ(), left, top + yLabelOffset, idPaints[colorID]);

        // Draw facial landmarks
        drawFaceLandmark(canvas, FaceLandmark.LEFT_EYE);
        drawFaceLandmark(canvas, FaceLandmark.RIGHT_EYE);
        drawFaceLandmark(canvas, FaceLandmark.LEFT_CHEEK);
        drawFaceLandmark(canvas, FaceLandmark.RIGHT_CHEEK);
    }

    private void drawFaceLandmark(Canvas canvas, @LandmarkType int landmarkType) {
        FaceLandmark faceLandmark = face.getLandmark(landmarkType);
        if (faceLandmark != null) {
            canvas.drawCircle(
                    translateX(faceLandmark.getPosition().x),
                    translateY(faceLandmark.getPosition().y),
                    FACE_POSITION_RADIUS,
                    facePositionPaint);
        }
    }

}
