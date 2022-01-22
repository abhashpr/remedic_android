package com.example.android.remedicappml;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import androidx.annotation.Nullable;

/** Graphic instance for rendering inference info (latency, FPS, resolution) in an overlay view. */
public class VitalsInfoGraphics extends GraphicOverlay.Graphic {

    private static final int TEXT_COLOR = Color.WHITE;
    private static final float TEXT_SIZE = 60.0f;

    private final Paint textPaint;
    private final Paint bgPaint;
    private final GraphicOverlay overlay;
    private final String beatsPerMinute;
    private final String spo2Index;

    public VitalsInfoGraphics(
            GraphicOverlay overlay,
            String beatsPerMinute,
            String spo2Index,
            @Nullable Integer framesPerSecond) {
        super(overlay);
        this.overlay = overlay;
        this.beatsPerMinute = beatsPerMinute;
        this.spo2Index = spo2Index;
        textPaint = new Paint();
        textPaint.setColor(TEXT_COLOR);
        textPaint.setTextSize(TEXT_SIZE);
        textPaint.setShadowLayer(5.0f, 0f, 0f, Color.BLACK);

        bgPaint = new Paint();
        bgPaint.setColor(Color.GRAY);
        bgPaint.setTextSize(50);  //set text size

        postInvalidate();
    }

    /**
     * Creates an {@link InferenceInfoGraphic} to only display image size.
     */
    public VitalsInfoGraphics(GraphicOverlay overlay) {
        this(overlay, "", "", null);
    }

    @Override
    public synchronized void draw(Canvas canvas) {
        float x = TEXT_SIZE * 0.5f;
        float y = TEXT_SIZE * 1.5f;

        bgPaint.setTextAlign(Paint.Align.CENTER);
        bgPaint.setStrokeWidth(3);
        canvas.drawRect(30, 30, 800, 200, bgPaint);
        canvas.drawText(
                "Heart Rate : " + beatsPerMinute + " bpm",
                x,
                y,
                textPaint);

        canvas.drawText(
                "Oxygen Saturation : " + spo2Index + " %",
                x,
                y + TEXT_SIZE,
                textPaint);

    }
}