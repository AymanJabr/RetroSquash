package com.example.retrosquash;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.Random;

public class MainActivity extends Activity {


    SquashCourtView squashCourtView;

    //Sound
    private SoundPool soundPool;
    int sample1 = -1;
    int sample2 = -1;
    int sample3 = -1;
    int sample4 = -1;

    //Display
    Canvas canvas;
    Display display;
    Point size;
    int screenWidth;
    int screenHeight;

    //Game components
    int racketWidth;
    int racketHeight;
    Point racketPosition;
    Point ballPosition;
    int ballWidth;

    //for "seeing" ball movements
    boolean ballIsMovingLeft;
    boolean ballIsMovingRight;
    boolean ballIsMovingUp;
    boolean ballIsMovingDown;

    //for "seeing" racket movements
    boolean racketIsMovingRight;
    boolean racketIsMovingLeft;

    //stats
    long lastFrameTime;
    int fps;
    int score;
    int lives;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
        //sets the content view from the SquashCour View
        // ????????????????????????
        squashCourtView = new SquashCourtView(this);
        setContentView(squashCourtView);

        //Sound Code
        soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
        try{
            //Create objects of the classes that will be used to retrieve the sound files
            AssetManager assetManager = getAssets();
            AssetFileDescriptor descriptor;

            //get and store values of the sound file that we want to use
            descriptor = assetManager.openFd("sample1.ogg");
            sample1 = soundPool.load(descriptor,0);
            descriptor = assetManager.openFd("sample2.ogg");
            sample2 = soundPool.load(descriptor,0);
            descriptor = assetManager.openFd("sample3.ogg");
            sample3 = soundPool.load(descriptor,0);
            descriptor = assetManager.openFd("sample4.ogg");
            sample4 = soundPool.load(descriptor,0);

        } catch (IOException e){
            Log.e("TRY/CATCH: ", "ERROR IN LOADING SOUND FILES");
        }

        //get the screen size in pixels
        display = getWindowManager().getDefaultDisplay();
        size = new Point();
        display.getSize(size);
        screenHeight = size.y;
        screenWidth = size.x;

        //set the game object sizes the lives values
        racketPosition = new Point();
        racketPosition.x = screenWidth/2;
        racketPosition.y = screenHeight - 20;
        racketWidth = screenWidth/8;
        racketHeight = 10;
        ballWidth = screenWidth/35;
        ballPosition = new Point();
        ballPosition.x = screenWidth/2;
        ballPosition.y = ballWidth + 1;

        lives = 3;
    }


    class SquashCourtView extends SurfaceView implements Runnable{
        Thread ourThread = null;
        SurfaceHolder ourHolder;
        volatile boolean playingSquash;
        Paint paint;

        public SquashCourtView(Context context){
            super(context);
            ourHolder = getHolder();
            paint = new Paint();
            ballIsMovingDown = true;

            //send the ball in a random direction horizontally
            Random randomNumber = new Random();
            int ballDirection = randomNumber.nextInt(3);
            switch (ballDirection) {
                case 0:
                    ballIsMovingLeft = true;
                    ballIsMovingRight = false;
                    break;
                case 1:
                    ballIsMovingRight = true;
                    ballIsMovingLeft = false;
                    break;
                case 2:
                    ballIsMovingLeft = false;
                    ballIsMovingRight = false;
                    break;
            }
        }

        @Override
        public void run() {

            while (playingSquash) {
                updateCourt();
                drawCourt();
                controlFPS();
            }

        }


        public void updateCourt() {

            //change position of racket
            if (racketIsMovingRight) {
                racketPosition.x = racketPosition.x + 10;
            }

            if (racketIsMovingLeft) {
                racketPosition.x = racketPosition.x - 10;
            }

            //detect collisions
            if (ballPosition.x + ballWidth > screenWidth) {
                ballIsMovingLeft = true;
                ballIsMovingRight = false;
                soundPool.play(sample1, 1, 1, 0, 0, 1);
            }

            if (ballPosition.x < 0) {
                ballIsMovingRight = true;
                ballIsMovingLeft = false;
                soundPool.play(sample1, 1, 1, 0, 0, 1);
            }

            //edge of the ball has hit bottom of screen
            if (ballPosition.y > screenHeight - ballWidth) {
                lives -= 1;
                if (lives == 0) {
                    lives = 3;
                    score = 0;
                    soundPool.play(sample4, 1, 1, 0, 0, 1);
                }
                ballPosition.y = 1 + ballWidth;


                //set the next horizontal direction the ball is gonna fall at
                // Isn't this already implemented above??????
                Random randomNumber = new Random();
                int startX = randomNumber.nextInt(screenWidth - ballWidth) + 1;
                ballPosition.x = startX + ballWidth;

                int ballDirection = randomNumber.nextInt(3);
                switch (ballDirection) {
                    case 0:
                        ballIsMovingLeft = true;
                        ballIsMovingRight = false;
                        break;
                    case 1:
                        ballIsMovingRight = true;
                        ballIsMovingLeft = false;
                        break;
                    case 2:
                        ballIsMovingLeft = false;
                        ballIsMovingRight = false;
                        break;
                }
            }

            //when ball hits the top of the screen
            if (ballPosition.y <= 0) {
                ballIsMovingDown = true;
                ballIsMovingUp = false;
                ballPosition.y = 1;
                soundPool.play(sample2, 1, 1, 0, 0, 1);
            }

            //this sounds kinda stupid??????
            if (ballIsMovingDown) {
                ballPosition.y += 10;
            }else{
                ballPosition.y -= 13;
            }

            if (ballIsMovingLeft) {
                ballPosition.x -= 8;
            } else {
                ballPosition.x += 8;
            }


            //make sure the racket doesn't move too much to the right or left
            if(racketPosition.x >= screenWidth){
                racketPosition.x = screenWidth - 1;
            }
            if (racketPosition.x <= 0){
                racketPosition.x = 1;
            }


            //when the ball hits the racket
            if (ballPosition.y + ballWidth >= (racketPosition.y - racketHeight / 2)) {
                int halfRacket = racketWidth/2;
                if (ballPosition.x + ballWidth > (racketPosition.x - halfRacket) &&
                        ballPosition.x - ballWidth < (racketPosition.x + halfRacket)) {
                    soundPool.play(sample3, 1, 1, 0, 0, 1);
                    score++;
                    ballIsMovingUp = true;
                    ballIsMovingDown = false;

                    if (ballPosition.x > racketPosition.x) {
                        ballIsMovingRight = true;
                        ballIsMovingLeft = false;
                    } else {
                        ballIsMovingLeft = true;
                        ballIsMovingRight = false;
                    }
                }
            }
        }

        public void drawCourt() {
            if (ourHolder.getSurface().isValid()) {
                canvas = ourHolder.lockCanvas();
                canvas.drawColor(Color.BLACK);
                paint.setColor(Color.argb(255,255,255,255));
                paint.setTextSize(45);
                canvas.drawText("Score: " + score + "\n Lives: " + lives + "\n fps: " + fps,20, 40, paint);

                //draw the racket
                canvas.drawRect(racketPosition.x - (racketWidth / 2),
                        racketPosition.y - (racketHeight/2),
                        racketPosition.x + (racketWidth/2),
                        racketPosition.y + racketHeight,paint);

                //draw the ball
                canvas.drawRect(ballPosition.x, ballPosition.y,
                        ballPosition.x + ballWidth, ballPosition. y + ballWidth, paint);

                ourHolder.unlockCanvasAndPost(canvas);
            }
        }

        public void controlFPS() {
            long timeThisFrame = (System.currentTimeMillis() - lastFrameTime);
            long timeToSleep = 15 - timeThisFrame;
            if (timeThisFrame > 0) {
                fps = (int) (1000 / timeThisFrame);
            }
            if (timeToSleep > 0) {
                try {
                    ourThread.sleep(timeToSleep);
                } catch (InterruptedException e) {
                    Log.e("ERRRROOOOOORRRRR: ", " error in the sleeping funcitonality of the thread");

                }
            }

            lastFrameTime = System.currentTimeMillis();
        }

        public void pause() {
            playingSquash = false;
            try {
                ourThread.join();
            } catch (InterruptedException e) {
                Log.e("ERRRROOOORRRR", " Error in joining threads together");
            }
        }

        public void resume() {
            playingSquash = true;
            ourThread = new Thread(this);
            ourThread.start();
        }

        @Override
        public boolean onTouchEvent(MotionEvent motionEvent) {
            switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    if (motionEvent.getX() >= screenWidth/2) {
                        Log.e("HEERRRRRRRRRREEEEEE: ",motionEvent.getX() + "  /  " + racketPosition.x);
                        racketIsMovingLeft = false;
                        racketIsMovingRight = true;
                    } else {
                        racketIsMovingRight = false;
                        racketIsMovingLeft = true;
                    }
                    break;

                case MotionEvent.ACTION_UP:
                    racketIsMovingLeft = false;
                    racketIsMovingRight = false;
                    break;
            }
            return true;
        }

    }


    @Override
    protected void onStop() {
        super.onStop();
        squashCourtView.pause();
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        squashCourtView.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        squashCourtView.resume();
    }

    public boolean onKeyDown(int keyCode, KeyEvent keyEvent ) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            squashCourtView.pause();
            finish();
            return true;
        }
        return false;
    }



}
