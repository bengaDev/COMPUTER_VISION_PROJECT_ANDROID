package com.example.benjamin.comp_vision_project_test;

import android.Manifest;
import android.app.WallpaperManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Math;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static String TAG = "MainActivity";

    //JavaCameraView myJavaCamera;
    ImageView imageView;
    Button myButton, playButton;
    TextView textResult;
    private static final int PICK_IMAGE = 100;
    Uri imageUri;

    // Matrices used for processing
    Mat mRGBA, imgWB, imgCanny, imgWBInv, imgBit, imgThreshCircles;
    Bitmap imgBitmap, greyBitmap;

    Mat lines = new Mat();

    List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

    private static int MY_CAMERA_REQ_CODE = 100;
    int counter = 0;

    int element_shape = 2; // Corresponds to MORPH_CROSS
    //int element_shape = 0; // Corresponds to MORPH_RECT

    Mat elementDilate = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(10, 10), new Point(-1, -1) );
    Mat elementErode = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(7, 7), new Point(-1, -1));

    Mat erodeElRECT = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(10, 10), new Point(-1, -1));
    Mat erodeElCROSS = Imgproc.getStructuringElement(Imgproc.MORPH_CROSS, new Size(10, 10), new Point(-1, -1));

    Mat dilateElSmall = Imgproc.getStructuringElement(element_shape, new Size(2, 2), new Point(-1, -1));

    ArrayList<Accordo> listaAccordi = createChordList();

    MediaPlayer lionSound, doSound, reSound, miSound, faSound, solSound, laSound, siSound;

    BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch(status){
                case BaseLoaderCallback.SUCCESS:{
                    //myJavaCamera.enableView();
                    break;
                }
                default:{
                    super.onManagerConnected(status);
                    break;
                }
            }
            super.onManagerConnected(status);
        }
    };

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    static {
        if(OpenCVLoader.initDebug()){
            Log.i(TAG, "OpenCV Loaded Successfully");
        } else {
            Log.i(TAG, "OpenCV Not Loaded");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        imageView = (ImageView)findViewById(R.id.imageView);
        myButton = (Button)findViewById(R.id.button);
        textResult = (TextView)findViewById(R.id.sample_text);

        myButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });


        // ----------------------------------------------------------
        // --------------- SOUND PART -------------------------------

        reSound = MediaPlayer.create(this, R.raw.re);
        miSound = MediaPlayer.create(this, R.raw.mi);
        faSound = MediaPlayer.create(this, R.raw.fa);
        solSound = MediaPlayer.create(this, R.raw.sol);
        laSound = MediaPlayer.create(this, R.raw.la);
        siSound = MediaPlayer.create(this, R.raw.si);
        doSound = MediaPlayer.create(this, R.raw.dom);


        playButton = (Button)findViewById(R.id.button2);

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                playChord();
            }
        });
    }

    private void openGallery(){
        Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI); // or .EXTERNAL_CONTENT_URI
        startActivityForResult(gallery, PICK_IMAGE);
    }

    private void playChord(){

        if(textResult.getText() == "DO_m"){
            doSound.start();
        }

        if(textResult.getText() == "RE_m"){
            reSound.start();
        }

        if(textResult.getText() == "MI_m"){
            miSound.start();
        }

        if(textResult.getText() == "FA_m"){
            faSound.start();
        }

        if(textResult.getText() == "SOL_m"){
            solSound.start();
        }

        if(textResult.getText() == "LA_m"){
            laSound.start();
        }

        if(textResult.getText() == "SI_m"){
            siSound.start();
        }


    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == RESULT_OK && requestCode == PICK_IMAGE && data != null){
            imageUri = data.getData();

            //  content://media/external/images/media/66836
            try{
                imgBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
            } catch(IOException e){
                e.printStackTrace();
            }

            int width = imgBitmap.getWidth();
            int height = imgBitmap.getHeight();

            imageView.setImageBitmap(imgBitmap);

            mRGBA = new Mat(height, width, CvType.CV_8UC4);
            Mat mProcessed = new Mat(height, width, CvType.CV_8UC1);



            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inDither = false;
            o.inSampleSize = 4;


            greyBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

            // CONVERSION FROM BITMAP TO MAT
            Utils.bitmapToMat(imgBitmap, mRGBA);

            // PROCESSING
            mProcessed = preProcessing(mRGBA, height, width);
            //Imgproc.cvtColor(mRGBA, imgWB, Imgproc.COLOR_RGB2GRAY);

            Utils.matToBitmap(mProcessed, greyBitmap);

            imageView.setImageBitmap(greyBitmap);

        }


    }

    public Mat preProcessing(Mat rgba, int height, int width){


        matrixInit(width, height);

        Imgproc.cvtColor(rgba, imgWB, Imgproc.COLOR_RGB2GRAY);

        // Blur to remove noise
        Imgproc.medianBlur(imgWB, imgWB, 7);

        // Apply threshold for 'imgCanny' -> matrix that will be used for line detection
        // Apply threshold for 'imgThreshCircles' -> matrix that will be used for circle detection
        Imgproc.threshold(imgWB, imgCanny, 100, 255, Imgproc.THRESH_BINARY_INV);
        Imgproc.threshold(imgWB, imgThreshCircles, 100, 255, Imgproc.THRESH_BINARY_INV);

        // Value 'i' can change based on how many 'erosion cycles' the image needs
        for(int i =0; i<3; i++){
            Imgproc.erode(imgThreshCircles, imgThreshCircles, erodeElRECT);
            Imgproc.erode(imgThreshCircles, imgThreshCircles, erodeElCROSS);
        }

        // Done to erase salt and pepper noise on binary eroded image
        Imgproc.medianBlur(imgThreshCircles, imgThreshCircles, 7);


        // Hough Lines OpenCV algorithm for detection of lines
        Imgproc.HoughLines(imgCanny, lines, 1, Math.PI/2, 400);


        // Find Contours OpenCV algorithm for circle detection
        if(contours.size() > 0){
            contours.clear();
        }
        Imgproc.findContours(imgThreshCircles, contours, new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        // ----------------------------------------------------------
        // --------------- LINE DETECTION ---------------------------

        // optimizedLines : list of lines detected, taking away the ones that are not horizonta or vertical (with a settable margin)
        // optLinesHor/Ver : lists containing either only horizontal or only vertical lines taken from 'optimizedLines'
        ArrayList<Line> optimizedLines = new ArrayList<>();
        ArrayList<HorLine> optLinesHor = new ArrayList<>();
        ArrayList<VerLine> optLinesVert = new ArrayList<>();


        // Filling 'optimizedLines' and drawing the lines for debug purposes
        for( int i = 0; i < lines.rows(); i++ )
        {
            double data[] = lines.get(i, 0);
            optimizedLines.add(new Line(data[0], data[1]));

            double rho = data[0];
            double theta = data[1];

            double cosTheta = Math.cos(theta);
            double sinTheta = Math.sin(theta);
            double x0 = cosTheta * rho;
            double y0 = sinTheta * rho;
            Point pt1 = new Point(x0 + 10000 * (-sinTheta), y0 + 10000 * cosTheta);
            Point pt2 = new Point(x0 - 10000 * (-sinTheta), y0 - 10000 * cosTheta);

            //Imgproc.line( imgCanny, pt1, pt2, new Scalar(255, 0, 0), 2);
        }

        // Filling 'optLinesVer' and 'optLinesHor' depending on their inclination
        for(Line line:optimizedLines){
            if(isHorizontal(line.theta) == false){
                VerLine tempVer = new VerLine(line.rho, line.theta);
                optLinesVert.add(tempVer);
            } else if(isHorizontal(line.theta) == true){
                HorLine tempHor = new HorLine(line.rho, line.theta);
                optLinesHor.add(tempHor);
            }
        }

        // Sorting them by Comparable defined inside 'HorLine' or 'VerLine' classes
        // Sorting them de-facto by distance from screen origin
        Collections.sort(optLinesHor);
        Collections.sort(optLinesVert);

        // Trimming down the lines in order not to have doubles: two lines that are very close
        // -> this case in fact should be seen as a single line
        trimHorLines(optLinesHor);
        trimVerLine(optLinesVert);

        // Getting the first and last horizontal and vertical lines
        HorLine horLineMax = optLinesHor.get(optLinesHor.size() - 1);
        HorLine horLineMin = optLinesHor.get(0);

        VerLine verLineMax = optLinesVert.get(optLinesVert.size() -1);
        VerLine verLineMin = optLinesVert.get(0);

        // For debugging purposes - draw these final result lines
        for(HorLine hLine:optLinesHor){
            Point pt1 = new Point(hLine.xComponent + 10000 * (-hLine.sinTheta), hLine.yComponent + 10000 * hLine.cosTheta);
            Point pt2 = new Point(hLine.xComponent - 10000 * (-hLine.sinTheta), hLine.yComponent - 10000 * hLine.cosTheta);

            Imgproc.line( imgCanny, pt1, pt2, new Scalar(255, 0, 0), 2);
            //Imgproc.line( rgba, pt1, pt2, new Scalar(255, 0, 0), 2);
        }


        for(VerLine vLine:optLinesVert){
            Point pt1 = new Point(vLine.xComponent + 10000 * (-vLine.sinTheta), vLine.yComponent + 10000 * vLine.cosTheta);
            Point pt2 = new Point(vLine.xComponent - 10000 * (-vLine.sinTheta), vLine.yComponent - 10000 * vLine.cosTheta);

            Imgproc.line( imgCanny, pt1, pt2, new Scalar(255, 0, 0), 2);
            //Imgproc.line( rgba, pt1, pt2, new Scalar(255, 0, 0), 2);
        }



        // ----------------------------------------------------------
        // --------------- CIRCLE DETECTION -------------------------

        float[] radius = new float[1];

        // centerList : list of 'Point' that are the centers of detected circles
        // noteList : list of detected notes
        ArrayList<Point> centerList = new ArrayList<>();
        ArrayList<Note> noteList = new ArrayList<>();
        boolean flag = true;

        // The 'findContour' method finds generic contours
        // We need to tell we are looking for circle shaped contours
        // Also populating 'centerList'
        for (int i = 0; i < contours.size(); i++) {
            Point center = new Point();
            MatOfPoint c = contours.get(i);

            MatOfPoint2f c2f = new MatOfPoint2f(c.toArray());
            Imgproc.minEnclosingCircle(c2f, center, radius);

            if((int)radius[0] >= 10 && (int)radius[0] <=200){
                if(flag==true){
                    Imgproc.circle(imgThreshCircles, center, 80, new Scalar(255, 255,0), 3);
                    flag = false;
                }
                // Draw the circle center -- FOR DEBUG
                Imgproc.circle( imgThreshCircles, center, 20, new Scalar(0,0,255), 3); //, 8, 0 );

                // Draw the circle outline -- FOR DEBUG
                Imgproc.circle( imgThreshCircles, center, (int)radius[0], new Scalar(255,0,0), 3); //, 8, 0 );

                // Fill the list of centers
                centerList.add(center);
            }
        }

        // ----------------------------------------------------------
        // --------------- NOTE AND CHORD DETECTION ---------------------------

        // Once we have the list of detected centers we can discover what
        // notes they are by comparing their coordinates with min and max horizontal and vertical lines
        for(Point c:centerList){
            Note tempNote;
            tempNote = calculateNote(horLineMax, horLineMin, verLineMax, verLineMin, c);
            noteList.add(tempNote);
        }

        // Once we have the list of notes, we can get the chord
        Accordo risultato = trovaAccordo(noteList);

        if(risultato != null){
            textResult.setText(risultato.nomeAccordo);
        } else {
            textResult.setText("Accordo non trovato!");
        }


        return rgba;

    }

    boolean isHorizontal(double theta){

        if(theta <= Math.PI/2 + 0.1 && theta >= Math.PI/2 - 0.1){
            return true;
        } else {
            return false;
        }

    }

    void trimHorLines(List<HorLine> horLines){

        HorLine stableLine = new HorLine(0, 0);

        for(int i = 0; i < horLines.size(); i++){
            if(i == 0 || (horLines.get(i).yComponent - stableLine.yComponent) > 80){
                stableLine = horLines.get(i);
            } else {
                horLines.remove(i);
                i--;
            }
        }
    }

    void trimVerLine(List<VerLine> verLines){

        VerLine stableLine = new VerLine(0,0);

        for(int i=0; i< verLines.size(); i++){
            if(i == 0 || (verLines.get(i).xComponent - stableLine.xComponent) > 50){
                stableLine = verLines.get(i);
            } else {
                verLines.remove(i);
                i--;
            }
        }
    }

    Note calculateNote(HorLine horLineMax, HorLine horLineMin, VerLine verLineMax, VerLine verLineMin, Point c){


        // HORIZONTAL - CORDE
        double horDifference = horLineMax.yComponent - horLineMin.yComponent;
        double horInterval = horDifference/5;
        double relativePoint_Y = c.y - horLineMin.yComponent;

        int cordaN = (int)Math.round(relativePoint_Y/horInterval);

        // VERTICAL - TASTI
        double verDifference = verLineMax.xComponent - verLineMin.xComponent;
        double verInterval = verDifference/5;
        double relativePoint_X = c.x - verLineMin.xComponent;

        int tasto = (int)Math.floor(relativePoint_X/verInterval);

        Note tempNote = new Note(tasto, cordaN);

        return  tempNote;
    }

    ArrayList<Accordo> createChordList(){
        ArrayList<Accordo> listaAccordi = new ArrayList<>();


        // DO m
        ArrayList<Note> noteAccordoDO_m = new ArrayList<>();
        noteAccordoDO_m.add(new Note(4, 4));
        noteAccordoDO_m.add(new Note(3,2));
        noteAccordoDO_m.add(new Note(2,1));

        listaAccordi.add(new Accordo("DO_m", noteAccordoDO_m));


        // RE m
        ArrayList<Note> noteAccordoRE_m = new ArrayList<>();
        noteAccordoRE_m.add(new Note(3, 3));
        noteAccordoRE_m.add(new Note(3,5));
        noteAccordoRE_m.add(new Note(2,4));

        listaAccordi.add(new Accordo("RE_m", noteAccordoRE_m));


        // MI m
        ArrayList<Note> noteAccordoMI_m = new ArrayList<>();
        noteAccordoMI_m.add(new Note(3, 1));
        noteAccordoMI_m.add(new Note(3,2));
        noteAccordoMI_m.add(new Note(4,3));

        listaAccordi.add(new Accordo("MI_m", noteAccordoMI_m));


        // FA m
        ArrayList<Note> noteAccordoFA_m = new ArrayList<>();
        noteAccordoFA_m.add(new Note(4, 0));
        noteAccordoFA_m.add(new Note(4,5));
        noteAccordoFA_m.add(new Note(3,3));
        noteAccordoFA_m.add(new Note(2,1));
        noteAccordoFA_m.add(new Note(2,2));

        listaAccordi.add(new Accordo("FA_m", noteAccordoFA_m));


        // SOL m
        ArrayList<Note> noteAccordoSOL_m = new ArrayList<>();
        noteAccordoSOL_m.add(new Note(3, 1));
        noteAccordoSOL_m.add(new Note(2,0));
        noteAccordoSOL_m.add(new Note(2,5));


        listaAccordi.add(new Accordo("SOL_m", noteAccordoSOL_m));


        // LA m
        ArrayList<Note> noteAccordoLA_m = new ArrayList<>();
        noteAccordoLA_m.add(new Note(3, 2));
        noteAccordoLA_m.add(new Note(3,3));
        noteAccordoLA_m.add(new Note(3,4));


        listaAccordi.add(new Accordo("LA_m", noteAccordoLA_m));


        // SI m
        ArrayList<Note> noteAccordoSI_m = new ArrayList<>();
        noteAccordoSI_m.add(new Note(3, 0));
        noteAccordoSI_m.add(new Note(3,5));
        noteAccordoSI_m.add(new Note(1,2));
        noteAccordoSI_m.add(new Note(1,3));
        noteAccordoSI_m.add(new Note(1,4));

        listaAccordi.add(new Accordo("SI_m", noteAccordoSI_m));


        return listaAccordi;
    }

    Accordo trovaAccordo(ArrayList<Note> noteList){

        int contatore = 0;

        for(Accordo possibileAccordo:listaAccordi){
            for(Note nota:noteList){
                if(possibileAccordo.containNote(nota) == true){
                    contatore++;
                }
            }

            if(contatore == noteList.size()){
                return possibileAccordo;
            }
            contatore = 0;
        }

        return null;
    }

    @Override
    protected  void onPause(){
        super.onPause();

        /*if(myJavaCamera != null){
            myJavaCamera.disableView();
        }*/
    }

    @Override
    protected  void onDestroy(){
        super.onDestroy();

        /*if(myJavaCamera != null){
            myJavaCamera.disableView();
        }*/
    }

    @Override
    protected void onResume(){
        super.onResume();

        if(OpenCVLoader.initDebug()){
            Log.i(TAG, "OpenCV Loaded Successfully");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        } else {
            Log.i(TAG, "OpenCV Not Loaded");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback);
        }
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    /*public String stringEx() {
        return "hello world";
    }*/


    public void matrixInit(int width, int height) {

        mRGBA = new Mat(height, width, CvType.CV_8UC4);
        imgWB = new Mat(height, width, CvType.CV_8UC1);
        imgBit = new Mat(height, width, CvType.CV_8UC1);
        imgWBInv = new Mat(height, width, CvType.CV_8UC1);
        imgCanny = new Mat(height, width, CvType.CV_8UC1);
        imgThreshCircles = new Mat(height, width, CvType.CV_8UC1);
    }

    @Override
    public void onCameraViewStarted(int a, int b){

    }

    @Override
    public void onCameraViewStopped() {

        mRGBA.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        counter++;

        mRGBA = inputFrame.rgba();

        Imgproc.cvtColor(mRGBA, imgWB, Imgproc.COLOR_RGB2GRAY);



        //Imgproc.bilateralFilter(imgWB, imgWBInv, 15, 80, 80, Core.BORDER_DEFAULT);
        //Imgproc.GaussianBlur(imgWB, imgGaussian, new Size(5, 5), 0);

        Imgproc.threshold(imgWB, imgBit, 80, 255, Imgproc.THRESH_BINARY_INV);
        //Imgproc.adaptiveThreshold(imgWB, imgBit, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY_INV, 15, -2);

        //int horSize = imgBit.rows() / 30;
        //int vertSize = imgBit.rows() / 30;
        //horizontalStructure = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(horSize, 1));

        //Imgproc.erode(imgBit, imgBit, horizontalStructure);
        //Imgproc.dilate(imgBit, imgBit, horizontalStructure);


        //Imgproc.Canny(imgWB, imgCanny, 100, 150); //,  3,  false);
        //Imgproc.threshold(imgWB, imgWBInv, 90, 255, Imgproc.THRESH_BINARY_INV);


        //Imgproc.dilate(imgCanny, imgCanny, elementDilate);
        //Imgproc.erode(imgCanny, imgCanny, elementErode);

        Imgproc.dilate(imgBit, imgBit, elementDilate);

        if(counter == 30){
            counter = 0;
            //Core.bitwise_not(imgWB, imgWB);
            Imgproc.HoughLines(imgBit, lines, 1, Math.PI/2, 150);

            // CIRCLE DETECTION
            //Imgproc.HoughCircles(imgWB, circles, Imgproc.CV_HOUGH_GRADIENT, 1, 5, 30, 150, 10, 100);

            Log.i(TAG, lines.toString());

        }

        for( int i = 0; i < lines.rows(); i++ )
        {
            double data[] = lines.get(i, 0);
            double rho = data[0];
            double theta = data[1];

            double cosTheta = Math.cos(theta);
            double sinTheta = Math.sin(theta);
            double x0 = cosTheta * rho;
            double y0 = sinTheta * rho;
            Point pt1 = new Point(x0 + 10000 * (-sinTheta), y0 + 10000 * cosTheta);
            Point pt2 = new Point(x0 - 10000 * (-sinTheta), y0 - 10000 * cosTheta);

            Imgproc.line( imgBit, pt1, pt2, new Scalar(255, 0, 0), 2);

        }

        /*for(int i=0; i < circles.rows(); i++){
            double data[] = circles.get(i, 0);


            Imgproc.circle(imgWB, new Point((int)data[0], (int)data[1]), (int)data[2], new Scalar(255, 0, 0));
        }*/


        return imgBit;
    }
}

class Line implements Comparable<Line>{
    public double rho, theta;

    Line(double rho, double theta){
        this.rho = rho;
        this.theta = theta;
    }

    double getRho(){
        return this.rho;
    }


    @Override
    public int compareTo(@NonNull Line o) {

        double rhoReceived = o.getRho();

        return this.rho > rhoReceived ? 1 : -1;
    }
}

class HorLine extends Line{

    double sinTheta, cosTheta;
    double yComponent, xComponent;


    HorLine(double rho, double theta){
        super(rho, theta);

        sinTheta = Math.sin(theta);
        cosTheta = Math.cos(theta);

        yComponent = sinTheta * rho;
        xComponent = cosTheta * rho;
        //this.rho = rho;
        //this.theta = theta;
    }

    @Override
    public int compareTo(@NonNull Line o){

        double yComponentReceived = ((HorLine)o).yComponent;

        return this.yComponent > yComponentReceived ? 1 : -1;
    }
}


class VerLine extends Line{

    double cosTheta, sinTheta;
    double xComponent, yComponent;


    VerLine(double rho, double theta){
        super(rho, theta);

        cosTheta = Math.cos(theta);
        sinTheta = Math.sin(theta);

        xComponent = cosTheta * rho;
        yComponent = sinTheta * rho;
        //this.rho = rho;
        //this.theta = theta;
    }

    @Override
    public int compareTo(@NonNull Line o){

        double xComponentReceived = ((VerLine)o).xComponent;

        return this.xComponent > xComponentReceived ? 1 : -1;
    }
}

class Note {

    int tasto, corda;

    Note(int t, int c){
        tasto = t;
        corda = c;
    }

}

class Accordo {

    String nomeAccordo;
    ArrayList<Note> noteAccordo;

    Accordo(String nome, ArrayList<Note> note){
        nomeAccordo = nome;
        noteAccordo = note;
    }

    boolean containNote(Note n){
        for(Note nota:noteAccordo){
            if(n.tasto==nota.tasto && n.corda==nota.corda){
                return true;
            }
        }
        return false;
    }


}