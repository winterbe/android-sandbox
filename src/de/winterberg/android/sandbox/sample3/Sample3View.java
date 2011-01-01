package de.winterberg.android.sandbox.sample3;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * @author Benjamin Winterberg
 */
public class Sample3View extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = "Sample3";

    public static final int GREEN = Color.rgb(22, 245, 156);
    public static final int BLUE = Color.rgb(22, 167, 245);

    public static final float MARGIN = 5;
    public static final float STROKE_WIDTH = 5;
    public static final float CIRCLE_RADIUS = 20;


    class SurfaceThread extends Thread {
        private static final float PHYS_VELOCITY_START = 100f;      // pixel per seconds
        private static final float PHYS_VELOCITY_LOSS = 25f;        // per second

        private final Context context;
        private final SurfaceHolder surfaceHolder;

        private Paint boundsPaint;
        private Paint ballPaint;

        private boolean running = false;

        private int surfaceWidth;
        private int surfaceHeight;

        private RectF bounds;
        private PointF position;

        private long lastTimestamp;

        private int degree;
        private float velocity;


        SurfaceThread(Context context, SurfaceHolder surfaceHolder) {
            super();
            this.context = context;
            this.surfaceHolder = surfaceHolder;
            initPaints();
        }

        private void initPaints() {
            boundsPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            boundsPaint.setColor(BLUE);
            boundsPaint.setStyle(Paint.Style.STROKE);
            boundsPaint.setStrokeWidth(STROKE_WIDTH);

            ballPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            ballPaint.setColor(GREEN);
            ballPaint.setStyle(Paint.Style.STROKE);
            ballPaint.setStrokeWidth(STROKE_WIDTH);
        }

        @Override
        public void run() {
            Log.d(TAG, "surface thread running");
            while (running) {
                Canvas canvas = null;
                try {
                    canvas = surfaceHolder.lockCanvas();
                    synchronized (surfaceHolder) {
                        updatePhysics();
                        doDraw(canvas);
                    }
                } finally {
                    if (canvas != null)
                        surfaceHolder.unlockCanvasAndPost(canvas);
                }
            }
            Log.d(TAG, "surface thread stopped");
        }

        private void updatePhysics() {
            long now = System.currentTimeMillis();
            long duration = now - lastTimestamp;
            lastTimestamp = now;

            // use center as starting position
            if (position == null) {
                position = new PointF(bounds.centerX(), bounds.centerY());
                velocity = PHYS_VELOCITY_START;
                degree = 90;
                return;
            }

            // do nothing without velocity
            if (velocity == 0f)
                return;

            // choke velocity until it reaches zero
            float loss = (PHYS_VELOCITY_LOSS / 1000) * duration;
            velocity = velocity - loss;
            if (velocity <= 0) {
                velocity = 0f;
                return;
            }

            float r = (duration / 1000f) * velocity;
            float x = (float) (r * Math.cos(degree));
            float y = (float) (r * Math.sin(degree));

            float nextX = position.x + x;
            float nextY = position.y + y;
            position = new PointF(nextX, nextY);


        }

        private void doDraw(Canvas canvas) {
            clearScreen(canvas);
            drawBackground(canvas);
            drawBall(canvas);
        }

        private void drawBall(Canvas canvas) {
            Path circle = new Path();
            circle.addCircle(position.x, position.y, CIRCLE_RADIUS, Path.Direction.CW);
            canvas.drawPath(circle, ballPaint);
        }

        private void clearScreen(Canvas canvas) {
            canvas.drawColor(Color.BLACK);
        }

        private void drawBackground(Canvas canvas) {
            Rect bounds = canvas.getClipBounds();
            RectF rectF = new RectF(MARGIN, MARGIN, bounds.right - MARGIN, bounds.bottom - MARGIN);
            canvas.drawRoundRect(rectF, 15, 15, boundsPaint);
        }

        public void setRunning(boolean running) {
            this.running = running;
        }

        public void setSurfaceSize(int width, int height) {
            synchronized (surfaceHolder) {
                // size
                this.surfaceWidth = width;
                this.surfaceHeight = height;

                // bounds
                float left = MARGIN + STROKE_WIDTH + CIRCLE_RADIUS;
                float top = MARGIN + STROKE_WIDTH + CIRCLE_RADIUS;
                float right = surfaceWidth - MARGIN - STROKE_WIDTH - CIRCLE_RADIUS;
                float bottom = surfaceHeight - MARGIN - STROKE_WIDTH - CIRCLE_RADIUS;
                bounds = new RectF(left, top, right, bottom);
            }
        }
    }


    private Context context;

    private SurfaceThread thread;


    public Sample3View(Context context, AttributeSet attrs) {
        super(context, attrs);

        SurfaceHolder holder = getHolder();
        holder.addCallback(this);

        this.context = context;
        this.thread = new SurfaceThread(context, holder);
    }

    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
        Log.d(TAG, "surfaceChanged");
        thread.setSurfaceSize(width, height);
    }

    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        Log.d(TAG, "surfaceCreated");
        thread.setRunning(true);
        thread.start();
    }

    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        Log.d(TAG, "surfaceDestroyed");
        boolean retry = true;
        thread.setRunning(false);
        while (retry) {
            try {
                thread.join();
                retry = false;
            } catch (InterruptedException e) {
                // nothing
            }
        }
    }
}