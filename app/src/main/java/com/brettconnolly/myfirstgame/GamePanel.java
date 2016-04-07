package com.brettconnolly.myfirstgame;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import java.util.ArrayList;

/**
 * Created by bdcon734 on 4/7/2016.
 */
public class GamePanel extends SurfaceView implements SurfaceHolder.Callback {
    public static final int WIDTH = 856;
    public static final int HEIGHT = 480;
    public static final int MOVESPEED = -5;
    private MainThread thread;
    private Background bg;
    private Player player;
    private ArrayList<SmokePuff> smoke;
    private long smokeStartTimer;
    public GamePanel(Context context) {
        super(context);

        //add the callback to surface holder to intercept events
        getHolder().addCallback(this);

        thread = new MainThread(getHolder(), this);

        //make gamePanel focusable so it can handle events
        setFocusable(true);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height){}

    @Override
    public void surfaceDestroyed(SurfaceHolder holder){
        boolean retry = true;
        int counter = 0;
        while(retry && counter < 1000){
            counter++;
            try{
                thread.setRunning(false);
                thread.join();
                retry = false;
            }catch(InterruptedException e){
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder){
        bg = new Background(BitmapFactory.decodeResource(getResources(), R.drawable.grassbg1));
        player = new Player(BitmapFactory.decodeResource(getResources(), R.drawable.helicopter), 65, 25, 3);
        smoke = new ArrayList<SmokePuff>();
        smokeStartTimer = System.nanoTime();
        //can safely start game loop
        thread.setRunning(true);
        thread.start();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        if(event.getAction()==MotionEvent.ACTION_DOWN) {
            if (!player.getPlaying()) {
                player.setPlaying(true);
            }

            player.setUp(true);
            return true;
        }

        if(event.getAction()==MotionEvent.ACTION_UP) {
            player.setUp(false);
            return true;
        }

        return super.onTouchEvent(event);
    }

    public void update(){
        if (player.getPlaying()) {
            bg.update();
            player.update();
            long elapsed = (System.nanoTime()-smokeStartTimer)/1000000;
            if (elapsed > 120){
                smoke.add(new SmokePuff(player.getX(), player.getY() + 10));
                smokeStartTimer = System.nanoTime();
            }

            for (int i = 0; i < smoke.size(); i++){
                smoke.get(i).update();
                if (smoke.get(i).getX()<-10){
                    smoke.remove(i);
                }
            }
        }
    }
    @Override
    public void draw(Canvas canvas){
        final float scaleFactorX = (float) getWidth()/WIDTH;
        final float scaleFactorY = (float) getHeight()/HEIGHT;
        if(canvas!=null){
            final int savedState = canvas.save();
            canvas.scale(scaleFactorX, scaleFactorY);
            bg.draw(canvas);
            player.draw(canvas);
            for (SmokePuff sp: smoke){
                sp.draw(canvas);
            }
            canvas.restoreToCount(savedState);
        }
    }
}
