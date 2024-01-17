package com.example.subham;


import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.os.Environment;
import android.os.StatFs;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.format.DateFormat;
import android.util.SparseArray;
import android.util.TimingLogger;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback,Detector.Processor,CameraSource.PictureCallback {

    CameraSource cameraSource;
    FaceDetector faceDetector;
    SurfaceView surfaceView;
    SurfaceView transparentView;
    final int CameraID = 1001;
    int deviceHeight, deviceWidth;

    boolean closed_eyes = false, captured = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        surfaceView = findViewById(R.id.surfaceview);
        transparentView = findViewById(R.id.transparentview);

        faceDetector = new FaceDetector.Builder(getApplicationContext())
                .setTrackingEnabled(true)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setMode(FaceDetector.FAST_MODE)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .setTrackingEnabled(true)
                .build();
        cameraSource = new CameraSource.Builder(this, faceDetector)
                .setRequestedPreviewSize(1024, 768)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setAutoFocusEnabled(true)
                .setRequestedFps(30)
                .build();

        surfaceView.getHolder().addCallback(this);
        transparentView.getHolder().addCallback(this);

        transparentView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        transparentView.setZOrderMediaOverlay(true);

        deviceWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
        deviceHeight = Resources.getSystem().getDisplayMetrics().heightPixels;

        faceDetector.setProcessor(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        try {
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.CAMERA}, CameraID);
                return;
            }

            cameraSource.start(surfaceView.getHolder());
        } catch (IOException e) {

        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        cameraSource.stop();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case CameraID: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted

                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    try {
                        cameraSource.start(surfaceView.getHolder());
                    } catch (IOException e) {
                    }
                }
            }
        }
    }


    @Override
    public void release() {

    }

    @Override
    public void receiveDetections(Detector.Detections detections) {

        SparseArray detectedFaces = detections.getDetectedItems();

        if (detectedFaces.size() != 0) {


            for (int i = 0; i < detectedFaces.size(); i++) {
                final Face face = (Face) detectedFaces.valueAt(i);

                try {
                    synchronized (surfaceView.getHolder()) {
                        DrawRectangle(face.getPosition().x, face.getPosition().y, face.getWidth(), face.getHeight());
                    }
                } catch (Exception e) {

                }
                
                if (captured) {
                    cameraSource.takePicture(null, this);
                    captured = false;
                    closed_eyes = false;
                }
            }


        }

    }


    public void DrawRectangle(float x, float y, float width, float height) {

        Canvas canvas = transparentView.getHolder().lockCanvas(null);
        canvas.drawColor(0, PorterDuff.Mode.CLEAR);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.GREEN);
        paint.setStrokeWidth(3);
        Rect rec = new Rect((int) x, (int) y + 150, (int) x + (int) width, (int) y + (int) height + 120);
        canvas.drawRect(rec, paint);
        transparentView.getHolder().unlockCanvasAndPost(canvas);
    }

    @Override
    public void onPictureTaken(byte[] data) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                MediaPlayer mediaPlayer = MediaPlayer.create(MainActivity.this, R.raw.picturetaken);
                mediaPlayer.start();
            }
        };
        Thread thread = new Thread(r);
        thread.start();

//        SimpleDateFormat simpleDateFormat=new SimpleDateFormat("hh:mm:ss");
//        String time=simpleDateFormat.format(Calendar.getInstance().getTime());
//        Log.d("myTag", time);
        TimingLogger timings = new TimingLogger("myTag", "method");
        // ... do some work A ...


        new Thread(new SMS()).start();
        new Thread(new SaveImage(data)).start();

        timings.addSplit("time taken to search for remaining memory and save image and play music");
        timings.dumpToLog();
    }




//    public static String getCalculatedDate(String dateFormat, int days) {
//        Calendar cal = Calendar.getInstance();
//        SimpleDateFormat s = new SimpleDateFormat(dateFormat);
//        cal.add(Calendar.DAY_OF_YEAR, days);
//        return s.format(new Date(cal.getTimeInMillis()));
//    }
}

class SMS implements Runnable {
    @Override
    public void run() {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage("+91-xxxxxxxxxx", null, "message", null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class SaveImage extends Thread implements Runnable{
    private final byte[] data;

    public SaveImage(byte[] data) {
        this.data=data;
    }

    @Override
    public void run() {
        Bitmap bitmapPicture = BitmapFactory.decodeByteArray(data, 0, data.length);
        Bitmap bitmap = Bitmap.createScaledBitmap(bitmapPicture, 768, 1024, true);
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yy hh:mm:ss");
        String currentDateandTime = sdf.format(Calendar.getInstance().getTime());
        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        paint.getTextAlign();
        Canvas canvas = new Canvas(bitmap);
        paint.setTextSize(35);
        float width = 450;
        float height = paint.measureText("yY");
        canvas.drawText(currentDateandTime, width, height + 950, paint);
        canvas.drawBitmap(bitmap, 0, 0, paint);


        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        long storage = availableBlocks * blockSize;


        if (storage < 2000000000) {
//            String pastday = getCalculatedDate("MM-dd-yy", 0);
//            Toast.makeText(this, "" + pastday, Toast.LENGTH_SHORT).show();
//            String file_path = Environment.getExternalStorageDirectory() + "/Pictures/Pictures/" + "Image-" + pastday + ".jpg";
//            Toast.makeText(this, "" + file_path, Toast.LENGTH_SHORT).show();
//            File fdelete = new File(file_path);
//            if (fdelete.exists())
//                fdelete.delete();
//                    sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
//                            Uri.parse("file://" + Environment.getExternalStorageDirectory())));


            // Used to examplify deletion of files more than 1 month old
// Note the L that tells the compiler to interpret the number as a Long
            final long MAXFILEAGE = 1000 * 60 * 60 * 24; // 2 days in milliseconds

// Get file handle to the directory. In this case the application files dir
            File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/Pictures");

// Obtain list of files in the directory.
// listFiles() returns a list of File objects to each file found.
            File[] files = dir.listFiles();

// Loop through all files
            for (File f : files) {

                // Get the last modified date
                Long lastmodified = f.lastModified();

                // Do stuff here to deal with the file..
                // For instance delete older files of specific time
                if (lastmodified + MAXFILEAGE < System.currentTimeMillis()) {
                    f.delete();
                }
            }
            save(bitmap);
        } else {
            save(bitmap);
        }
    }

    private void save(Bitmap bitmap) {
        Date date = new Date();
        CharSequence file_name = DateFormat.format("MM-dd-yy hh:mm:ss", date.getTime());
        String root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
        File myDir = new File(root + "/Pictures");
        myDir.mkdirs();
        String fname = "Image-" + file_name + ".jpg";
        File file = new File(myDir, fname);
        FileOutputStream outputStream;
        try {
            outputStream = new FileOutputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(outputStream);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos);
            bos.flush();
            bos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }
}
