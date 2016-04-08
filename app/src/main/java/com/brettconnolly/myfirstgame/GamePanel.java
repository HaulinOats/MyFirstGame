package com.brettconnolly.myfirstgame;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Random;

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
    private ArrayList<Missile> missiles;
    private ArrayList<TopBorder> topBorder;
    private ArrayList<BottomBorder> bottomBorder;
    private long smokeStartTimer;
    private long missileStartTime;
    private Random rand = new Random();
    private int maxBorderHeight;
    private int minBorderHeight;
    private boolean topDown = true;
    private boolean botDown = true;
    private boolean newGameCreated;
    //increase to make difficulty lower, lower to increase difficulty
    private int progressDenom = 20;

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
        missiles = new ArrayList<Missile>();
        topBorder = new ArrayList<TopBorder>();
        bottomBorder = new ArrayList<BottomBorder>();
        smokeStartTimer = System.nanoTime();
        missileStartTime = System.nanoTime();
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
            //calculate min/max height
            maxBorderHeight  = 30 +  player.getScore()/progressDenom;
            //cap max border height
            if (maxBorderHeight>HEIGHT/4){
                maxBorderHeight = HEIGHT/4;
            }
            minBorderHeight = 5 + player.getScore()/progressDenom;

            //check top border collision
            for (int i=0; i < topBorder.size(); i++){
                if (collision(topBorder.get(i), player)){
                    player.setPlaying(false);
                }
            }

            //check bottom border collision
            for (int i=0; i < bottomBorder.size(); i++){
                if (collision(bottomBorder.get(i), player)){
                    player.setPlaying(false);
                }
            }

            //update top border
            this.updateTopBorder();
            //update bottom border
            this.updateBottomBorder();

            //add missiles on timer
            long missilesElapsed = (System.nanoTime()-missileStartTime)/1000000;
            if (missilesElapsed > (2000 - player.getScore()/4)){
                //first missile goes down the middle
                if (missiles.size()==0){
                    missiles.add(new Missile(BitmapFactory.decodeResource(getResources(), R.drawable.missile), WIDTH + 10, HEIGHT/2, 45, 15, player.getScore(), 13));
                } else {
                    missiles.add(new Missile(BitmapFactory.decodeResource(getResources(), R.drawable.missile), WIDTH + 10, (int) (rand.nextDouble()*(HEIGHT-maxBorderHeight * 2) + maxBorderHeight), 45, 15, player.getScore(), 13));
                }

                missileStartTime = System.nanoTime();
            }

            //loop through every missile
            for (int i = 0; i < missiles.size(); i++){
                //update missile
                missiles.get(i).update();
                //stop game is missle collides with player
                if(collision(missiles.get(i), player)){
                    missiles.remove(i);
                    player.setPlaying(false);
                    break;
                }
                //remove missile if off screen
                if(missiles.get(i).getX() < -100){
                    missiles.remove(i);
                    break;
                }
            }

            //add smoke puffs
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
        } else {
            newGameCreated = false;
            if (!newGameCreated){
                newGame();
            }
        }
    }
    public boolean collision(GameObject a, GameObject b){
        if (Rect.intersects(a.getRectangle(), b.getRectangle())){
            return true;
        }
        return false;
    }
    @SuppressLint("MissingSuperCall")
    @Override
    public void draw(Canvas canvas){
        final float scaleFactorX = (float) getWidth()/WIDTH;
        final float scaleFactorY = (float) getHeight()/HEIGHT;
        if(canvas!=null){
            final int savedState = canvas.save();
            canvas.scale(scaleFactorX, scaleFactorY);
            bg.draw(canvas);
            player.draw(canvas);
            //draw smoke puffs
            for (SmokePuff sp: smoke){
                sp.draw(canvas);
            }
            //draw missiles
            for (Missile m: missiles){
                m.draw(canvas);
            }
            canvas.restoreToCount(savedState);

            //draw top border
            for(TopBorder tb: topBorder){
                tb.draw(canvas);
            }
            //draw bottom border
            for(BottomBorder bb: bottomBorder){
                bb.draw(canvas);
            }
        }
    }

    public void updateTopBorder(){
        //every 50 points, insert randomly placed top blocks
        if (player.getScore()%50 == 0){
            topBorder.add(new TopBorder(BitmapFactory.decodeResource(getResources(), R.drawable.brick),
                    topBorder.get(topBorder.size()-1).getX()+20,
                    0, (int) ((rand.nextDouble() * (maxBorderHeight)) + 1)));
            for (int i = 0; i < topBorder.size();i++){
                topBorder.get(i).update();
                if (topBorder.get(i).getX()<20){
                    topBorder.remove(i);

                    //checks which direction the border is moving
                    if (topBorder.get(topBorder.size()-1).getHeight()>maxBorderHeight){
                        topDown = false;
                    }
                    if (topBorder.get(topBorder.size()-1).getHeight()<=minBorderHeight){
                        topDown = true;
                    }
                    //new border will have larger/smaller height
                    if (topDown){
                        topBorder.add(new TopBorder(BitmapFactory.decodeResource(getResources(),R.drawable.brick), topBorder.get(topBorder.size()-1).getX()+20,
                                0, topBorder.get(topBorder.size()-1).getHeight()+1));
                    }else {
                        topBorder.add(new TopBorder(BitmapFactory.decodeResource(getResources(),R.drawable.brick), topBorder.get(topBorder.size()-1).getX()+20,
                                0, topBorder.get(topBorder.size()-1).getHeight()-1));
                    }
                }
            }
        }
    }
    public void updateBottomBorder(){
        if (player.getScore()%40 == 0){
            bottomBorder.add(new BottomBorder(BitmapFactory.decodeResource(getResources(), R.drawable.brick), bottomBorder.get(bottomBorder.size()-1).getX()+20, (int) ((rand.nextDouble() * (maxBorderHeight)) + (HEIGHT-maxBorderHeight))));
        }

        //update bottom border
        for (int i = 0;i < bottomBorder.size();i++) {
            bottomBorder.get(i).update();
            if (bottomBorder.get(i).getX() < -20) {
                bottomBorder.remove(i);

                if (bottomBorder.get(bottomBorder.size() - 1).getHeight() > maxBorderHeight) {
                    botDown = false;
                }
                if (bottomBorder.get(bottomBorder.size() - 1).getHeight() <= minBorderHeight) {
                    botDown = true;
                }
                //new border will have larger/smaller height
                if (botDown) {
                    bottomBorder.add(new BottomBorder(BitmapFactory.decodeResource(getResources(), R.drawable.brick),
                            bottomBorder.get(bottomBorder.size() - 1).getX() + 20,
                            bottomBorder.get(bottomBorder.size() - 1).getHeight() + 1));
                } else {
                    bottomBorder.add(new BottomBorder(BitmapFactory.decodeResource(getResources(), R.drawable.brick),
                            bottomBorder.get(bottomBorder.size() - 1).getX() + 20,
                            bottomBorder.get(bottomBorder.size() - 1).getHeight() - 1));
                }
            }
        }

    }
    public void newGame(){
        bottomBorder.clear();
        topBorder.clear();
        missiles.clear();
        smoke.clear();

        minBorderHeight = 5;
        maxBorderHeight = 30;
        player.resetDY();

        player.resetScore();
        player.setY(HEIGHT/2);

        //create initial top border
        for (int i = 0; i* 20 < WIDTH + 40; i++){
            if (i == 0) {
                topBorder.add(new TopBorder(BitmapFactory.decodeResource(getResources(), R.drawable.brick),
                        i * 20, 0, 10));
            } else {
                topBorder.add(new TopBorder(BitmapFactory.decodeResource(getResources(), R.drawable.brick),
                        i * 20, 0, topBorder.get(i-1).getHeight()+1));
            }
        }
        //create initial top border
        for (int i = 0; i* 20 < WIDTH + 40; i++){
            if (i == 0) {
                bottomBorder.add(new BottomBorder(BitmapFactory.decodeResource(getResources(), R.drawable.brick),
                        i * 20, HEIGHT - minBorderHeight));
            } else {
                bottomBorder.add(new BottomBorder(BitmapFactory.decodeResource(getResources(), R.drawable.brick),
                        i * 20, bottomBorder.get(i-1).getY()-1));
            }
        }

        newGameCreated = true;
    }
}
