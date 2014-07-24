package com.tfp.mrclam;

import java.util.Random;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;


class ClamView extends SurfaceView implements SurfaceHolder.Callback {
    class ClamThread extends Thread {
 

        /**
         * Member (state) fields
         */
        /** The drawable to use as the background of the animation canvas */
        private Bitmap mBackgroundImage;
        
        //Clam images and other fields
        private BitmapDrawable mClamOpenImage;
        private BitmapDrawable mClamClosedImage;
        private BitmapDrawable mClamDazedImage;
        
        private BitmapDrawable mDiverHelmImage;
        private BitmapDrawable mDiverArmImage;
        private BitmapDrawable mDiverArmInjuredImage;
        
        private int mClamHeight;
        private int mClamWidth;
        
        private Rect mClamRect;
        
        
        private Rect mDiverRect;
        private BitmapDrawable mDiverImage;
        
        //Constants for scaling clam for screen size
        //TODO tweak clam size
        private final double CLAMXSCALE = 10;
        private final double CLAMYSCALE = 1.42;
        
        private SoundPool sp;
        private int soundIds[];

        private MediaPlayer mMusicPlayer;
       

        /**
         * Current height of the surface/canvas.
         *
         * @see #setSurfaceSize
         */
        private int mCanvasHeight = 1;

        /**
         * Current width of the surface/canvas.
         *
         * @see #setSurfaceSize
         */
        private int mCanvasWidth = 1;
        
        private SurfaceHolder mSurfaceHolder;
        private Handler mHandler;
        
        /*
         * State-tracking constants
         */
        private int mMode;
        
        public static final int STATE_LOSE = 1;
        public static final int STATE_PAUSE = 2;
        public static final int STATE_READY = 3;
        public static final int STATE_RUNNING = 4;
        public static final int STATE_WIN = 5;
             
                
        /** Indicate whether the surface has been created & is ready to draw */
        private boolean mRun = false;

        private final Object mRunLock = new Object();

        public ClamThread(SurfaceHolder surfaceHolder, Context context,
                Handler handler) {
            // get handles to some important objects
            mSurfaceHolder = surfaceHolder;
            mHandler = handler;
            mContext = context;

            loadImages(context.getResources());
            loadSounds();
            
            playMusic();

            mClamWidth = mClamOpenImage.getIntrinsicWidth();
            mClamHeight = mClamOpenImage.getIntrinsicHeight();
                       
            mClamRect = new Rect();
            mDiverRect = new Rect();
        }
        
		private void loadImages(Resources res){
  
        	// cache handles to our key sprites & other drawables
            mClamOpenImage = (BitmapDrawable) res.getDrawable(R.drawable.clam_open);
            mClamClosedImage = (BitmapDrawable)res.getDrawable(R.drawable.clam_closed);
            mClamDazedImage = (BitmapDrawable)res.getDrawable(R.drawable.clam_dazed);
            
            mDiverHelmImage = (BitmapDrawable) res.getDrawable(R.drawable.diver_helm);
            mDiverArmImage = (BitmapDrawable) res.getDrawable(R.drawable.diver_arm);
            mDiverArmInjuredImage = (BitmapDrawable) res.getDrawable (R.drawable.diver_arm_injured);
            
            for (int i=0; i<4; i++){
            	mDivers[i].setImage(mDiverHelmImage,  mDiverArmImage, mDiverArmInjuredImage);
            }
            
            // load background image as a Bitmap instead of a Drawable b/c
            // we don't need to transform it and it's faster to draw this way
            mBackgroundImage = BitmapFactory.decodeResource(res, R.drawable.ocean);
        }
		
		private void loadSounds(){
			
			sp = new SoundPool(3, AudioManager.STREAM_MUSIC, 0);
			
			soundIds = new int[3];
			
			soundIds[0] = sp.load(mContext, R.raw.ouch1_sound, 1);
			soundIds[1] = sp.load(mContext, R.raw.ouch2_sound, 1);
			soundIds[2] = sp.load(mContext, R.raw.snap_sound, 1);
			
			//TODO credit music
			mMusicPlayer = MediaPlayer.create(mContext, R.raw.music);
		}
		
		public void playSound(int soundId, int i){
			if (!mIsSound) return;
			
			switch (soundId){
			case SOUND_SNAP: 	sp.play(soundIds[2], 1, 1, 1, 0, 1);
								break;
			case SOUND_OUCH: 	sp.play(soundIds[i], 1, 1, 1, 0, 1);
								break;
			}
		}
		
		public void playMusic(){
			if (mIsMusic){
				mMusicPlayer.setLooping(true);
				mMusicPlayer.start();
			}
		}
		
		private void clearSounds(){
			sp.release();
			mMusicPlayer.release();
		}

        /**
         * Starts the game.
         */
        public void doStart() {
            synchronized (mSurfaceHolder) {
            	setState(STATE_RUNNING);
            }
        }

        /**
         * Pauses the animation.
         */
        public void pause() {
            synchronized (mSurfaceHolder) {
                if (mMode == STATE_RUNNING) setState(STATE_PAUSE);
            }
        }

        /**
         * Restores game state from the indicated Bundle. Typically called when
         * the Activity is being restored after having been previously
         * destroyed.
         *
         * @param savedState Bundle containing the game state
         */
        public synchronized void restoreState(Bundle savedState) {
            synchronized (mSurfaceHolder) {
                
            }
        }

        @Override
        public void run() {
        	if (mMode == STATE_READY) setState(STATE_RUNNING);
        	
        	Canvas c;
        	
        	long timeStart;
        	long timeEnd;
        	long elapsedTime;
        	int sleepFor;
        	
            while (mRun) {
            	
            	timeStart = System.currentTimeMillis();
            	
               	c = null;
                try {
                    c = mSurfaceHolder.lockCanvas(null);
                    synchronized (mSurfaceHolder) {
                        if (mMode == STATE_RUNNING) tick();
                        // Critical section. Do not allow mRun to be set false until
                        // we are sure all canvas draw operations are complete.
                        //
                        // If mRun has been toggled false, inhibit canvas operations.
                        synchronized (mRunLock) {
                            if (mRun) doDraw(c);
                        }
                    }
                } finally {
                    // do this in a finally so that if an exception is thrown
                    // during the above, we don't leave the Surface in an
                    // inconsistent state
                    if (c != null) {
                        mSurfaceHolder.unlockCanvasAndPost(c);
                    }
                }
                
                timeEnd = System.currentTimeMillis();
                elapsedTime = timeEnd-timeStart;
                sleepFor = (int) ((1000/FPS)-elapsedTime);
                
                if (sleepFor > 0){
                	try{
                		ClamThread.sleep(sleepFor);
                	}catch(InterruptedException e){}
                }
                
            }
        }

        /**
         * Dump game state to the provided Bundle. Typically called when the
         * Activity is being suspended.
         *
         * @return Bundle with this view's state
         */
        /**public Bundle saveState(Bundle map) {
            synchronized (mSurfaceHolder) {
                if (map != null) {
                    map.putInt(KEY_DIFFICULTY, Integer.valueOf(mDifficulty));
                    map.putDouble(KEY_X, Double.valueOf(mX));
                    map.putDouble(KEY_Y, Double.valueOf(mY));
                    map.putDouble(KEY_DX, Double.valueOf(mDX));
                    map.putDouble(KEY_DY, Double.valueOf(mDY));
                    map.putDouble(KEY_HEADING, Double.valueOf(mHeading));
                    map.putInt(KEY_LANDER_WIDTH, Integer.valueOf(mLanderWidth));
                    map.putInt(KEY_LANDER_HEIGHT, Integer
                            .valueOf(mLanderHeight));
                    map.putInt(KEY_GOAL_X, Integer.valueOf(mGoalX));
                    map.putInt(KEY_GOAL_SPEED, Integer.valueOf(mGoalSpeed));
                    map.putInt(KEY_GOAL_ANGLE, Integer.valueOf(mGoalAngle));
                    map.putInt(KEY_GOAL_WIDTH, Integer.valueOf(mGoalWidth));
                    map.putInt(KEY_WINS, Integer.valueOf(mWinsInARow));
                    map.putDouble(KEY_FUEL, Double.valueOf(mFuel));
                }
            }
            return map;
        }**/


        /**
         * Used to signal the thread whether it should be running or not.
         * Passing true allows the thread to run; passing false will shut it
         * down if it's already running. Calling start() after this was most
         * recently called with false will result in an immediate shutdown.
         *
         * @param b true to run, false to shut down
         */
        public void setRunning(boolean b) {
            // Do not allow mRun to be modified while any canvas operations
            // are potentially in-flight. See doDraw().
            synchronized (mRunLock) {
                mRun = b;
            }
        }

        /**
         * Sets the game mode. That is, whether we are running, paused, in the
         * failure state, in the victory state, etc.
         *
         * @see #setState(int, CharSequence)
         * @param mode one of the STATE_* constants
         */
        public void setState(int mode) {
            synchronized (mSurfaceHolder) {
                mMode = mode;
            }
        }

        /**
         * Sets the game mode. That is, whether we are running, paused, in the
         * failure state, in the victory state, etc.
         *
         * @param mode one of the STATE_* constants
         * @param message string to add to screen or null
         */
        /**public void setState(int mode, CharSequence message) {
			synchronized (mSurfaceHolder) {
                mMode = mode;

                if (mMode == STATE_RUNNING) {
                    Message msg = mHandler.obtainMessage();
                    Bundle b = new Bundle();
                    b.putString("text", "");
                    b.putInt("viz", View.INVISIBLE);
                    msg.setData(b);
                    mHandler.sendMessage(msg);
                } else {
                    Resources res = mContext.getResources();
                    CharSequence str = "";
                    if (mMode == STATE_READY)
                        str = res.getText(R.string.mode_ready);
                    else if (mMode == STATE_PAUSE)
                        str = res.getText(R.string.mode_pause);
                    else if (mMode == STATE_LOSE)
                        str = res.getText(R.string.mode_lose);
                    else if (mMode == STATE_WIN)
                        str = res.getString(R.string.mode_win_prefix)
                                + mWinsInARow + " "
                                + res.getString(R.string.mode_win_suffix);

                    if (message != null) {
                        str = message + "\n" + str;
                    }

                    if (mMode == STATE_LOSE) mWinsInARow = 0;

                    Message msg = mHandler.obtainMessage();
                    Bundle b = new Bundle();
                    b.putString("text", str.toString());
                    b.putInt("viz", View.VISIBLE);
                    msg.setData(b);
                    mHandler.sendMessage(msg);
                }
            }
        }**/

        /* Callback invoked when the surface dimensions change. */
        public void setSurfaceSize(int width, int height) {
            // synchronized to make sure these all change atomically
            synchronized (mSurfaceHolder) {
                mCanvasWidth = width;
                mCanvasHeight = height;
                
                mClamWidth = (int) Math.round(mCanvasWidth/CLAMXSCALE);
                mClamHeight = (int)Math.round(mClamWidth/CLAMYSCALE);

                // don't forget to resize the background image
	            
                mBackgroundImage = Bitmap.createScaledBitmap (mBackgroundImage, width, height, false);
                
                mClamRect.set((width-mClamWidth)/2, (height-mClamHeight)/2,
                		(width+mClamWidth)/2, (height+mClamHeight)/2);
                
                mClamOpenImage.setBounds(mClamRect);
                mClamClosedImage.setBounds(mClamRect);
                mClamDazedImage.setBounds(mClamRect);
                
                for (int i=0; i<4;i++){
                	mDivers[i].setSize(width, height);
                }
                
            }
        }

        /**
         * Resumes from a pause.
         */
        public void unpause() {
            // Move the real time clock up to now
            synchronized (mSurfaceHolder) {
            }
            setState(STATE_RUNNING);
        }


        private void doDraw(Canvas canvas) {
        	if (canvas==null) return;
            // Draw the background image. Operations on the Canvas accumulate
            // so this is like clearing the screen.
            canvas.drawBitmap(mBackgroundImage, 0, 0, null);
            
            if (mClamState == CLAM_OPEN){
            	mClamOpenImage.draw(canvas);
            	           	
            }else if (mClamState == CLAM_CLOSED){
            	mClamClosedImage.draw(canvas);
            	
            }else if (mClamState == CLAM_DAZED){
            	mClamDazedImage.draw(canvas);
            }
            
            for (int i=0; i<4;i++){
            	
            	if (mDivers[i].getArmRect(mDiverRect)){
            		mDiverImage = mDivers[i].getArmImage();
            		mDiverImage.setBounds(mDiverRect);
            		mDiverImage.draw(canvas);
            	}
            	
            	if (mDivers[i].getHeadRect(mDiverRect)){
            		mDiverImage = mDivers[i].getHeadImage();
            		mDiverImage.setBounds(mDiverRect);
            		mDiverImage.draw(canvas);
            	}
            }
            
            
        }

        /**
         * Non-drawing actions performed every tick, i.e. movement and animation.
         */
        private void tick() {
        	        	
        	for (int i=0; i<4; i++){
        		if (mDivers[i].isActive()){
        			
        			if (mDivers[i].checkLose()){
        				//endgame condition
        				mEndgame = true;
        				thread.pause();
        				((GameActivity)mContext).endgame(mScore);
        			}
        			
        			mDivers[i].doTick();
        		}
        	}
        	
        	if (mEndgame) return;
        	
        	if (mScore <= 0){
        		if (!mDivers[0].isActive()){
        			mDivers[0].init(-1, true);
        			mNumDivers++;
        		}
        		
        	}
        	
        	if (mClamState == CLAM_CLOSED){
        		mClamClosedCounter++;
        		
        		//check for biting hands
        		for (int i=0; i<4; i++){
        			if (!mEndgame && mDivers[i].isActive()){
        				if (mDivers[i].checkCollision(mClamWidth, mClamHeight)){
        					mClamMissed = false;
        					playSound(SOUND_OUCH, r.nextInt(2));
        					addScore();
        				}
        			}
        		}
        		
        		//check if the bite missed
        		if (mClamClosedCounter >= 2){
        			if (mClamMissed) {
        				setClamState(CLAM_DAZED);
        				mClamDazedCounter = 0;
        			} else{
        				setClamState(CLAM_OPEN);
        			}
        		}
        		
        	//TODO decide on daze time length
        	} else if (mClamState == CLAM_DAZED){
        		mClamDazedCounter++;
        		if (mClamDazedCounter > 20){
        			setClamState(CLAM_OPEN);
        		}
        	}
        	
        	
        	//diver stuff
        	if (mNumDivers < 3 && mScore>0 &&(mSpawnTimer < 0 || mNumDivers == 0)){
        		
        		if ((mNumDivers>0 && r.nextInt(7)==0) || (mNumDivers ==0 && r.nextInt(4)==0) ||
        				(mNumDivers == 0 && mSpawnTimer < 0)){
        			
        			int c = r.nextInt(4);
        			if (!mDivers[c].isActive()){
        				int dif = (int)Math.floor(mScore/5);
        				if (r.nextFloat()>=0.6) dif++;
        				
        				//TODO tweak difficulty level
        				
        				mDivers[c].init(dif, false);
        				mNumDivers++;
        			}
        		}
        		if (mSpawnTimer <0 && mNumDivers > 0) mSpawnTimer = mSpawnGap;
        	}
        	mSpawnTimer--;  	
        	
        }
    }
    
    private void addScore(){
    	mScore++;
    	mNumDivers--;
    
    	
    	if (mScore%5==0){
    		//TODO tweak spawn gap
    		mSpawnGap -= 1;
    	}
    }

    /** Handle to the application context, used to e.g. fetch Drawables. */
    private Context mContext;


    /** The thread that actually draws the animation */
    private ClamThread thread;
    
    //Clam state tracking
    private int mClamState;
    
    private static final int CLAM_OPEN = 0;
    private static final int CLAM_CLOSED = 1;
    private static final int CLAM_DAZED = 2;
    
    private int mClamClosedCounter;
    private int mClamDazedCounter;
    private boolean mClamMissed;
    
    private Diver[] mDivers;
    
    Random r;
    private int mScore;
    private int mSpawnTimer;
    private int mSpawnGap;
    
    private int mNumDivers;
    
    private final int SOUND_SNAP = 0;
    private final int SOUND_OUCH = 1;
    
    private boolean mEndgame;
    
    private boolean mIsMusic;
    private boolean mIsSound;


    //TODO pick delay
	private final long FPS = 25;
    
    public ClamView(Context context){
    	super(context);
    	getHolder().addCallback(this);
    }
    
    public ClamView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        getHolder().addCallback(this);
    }
    
    public ClamView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // register our interest in hearing about changes to our surface
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        
        mDivers = new Diver[4];
        for (int i=0; i < 4; i++){
        	mDivers[i] = new Diver(i);
        }
        
        mIsMusic = ((GameActivity)context).getMusic();
        mIsSound = ((GameActivity)context).getSound();
        
        // create thread only; it's started in surfaceCreated()
        thread = new ClamThread(holder, context, new Handler());
        
        r = new Random();

        mScore = 0;
        //TODO set mspawngap = 40
        mSpawnGap = 40;
        mSpawnTimer = mSpawnGap;
        
        mNumDivers = 0;
        
        mEndgame = false;
        
        setFocusable(true); // make sure we get key events
    }
    
    public void reset(){
    	mScore = 0;
    	mSpawnGap = 40;
    	mSpawnTimer = mSpawnGap;
    	
    	mNumDivers = 0;
    	
    	mEndgame = false;
        
    	setClamState(CLAM_OPEN);
    	for (int i=0; i < 4; i++){
        	mDivers[i].reset();
        }
    	thread.unpause();
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent e){
    	    	
    	int action = e.getAction();
    	
    	if (action == MotionEvent.ACTION_DOWN){
    		
    		if (!mEndgame && mClamState == CLAM_OPEN){
    			setClamState(CLAM_CLOSED);
    			
    			thread.playSound(SOUND_SNAP, 0);
    			
    			mClamClosedCounter = 0;
    			mClamMissed = true;
    		}
    		
    	}
    	return true;
    }

    /**
     * Fetches the animation thread corresponding to this ClamView.
     *
     * @return the animation thread
     */
    public ClamThread getThread() {
        return thread;
    }
    
    private void setClamState(int state){
    	mClamState = state;
    }

    /**
     * Standard window-focus override. Notice focus lost so we can pause on
     * focus lost. e.g. user switches to take a call.
     */
    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        if (!hasWindowFocus) thread.pause();
    }


    /* Callback invoked when the surface dimensions change. */
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
            int height) {
        thread.setSurfaceSize(width, height);
    }

    /*
     * Callback invoked when the Surface has been created and is ready to be
     * used.
     */
    public void surfaceCreated(SurfaceHolder holder) {
        // start the thread here so that we don't busy-wait in run()
        // waiting for the surface to be created
    	
        thread.setRunning(true);
        thread.start();
    }

    /*
     * Callback invoked when the Surface has been destroyed and must no longer
     * be touched. WARNING: after this method returns, the Surface/Canvas must
     * never be touched again!
     */
    public void surfaceDestroyed(SurfaceHolder holder) {
        // we have to tell thread to shut down & wait for it to finish, or else
        // it might touch the Surface after we return and explode
    	
        boolean retry = true;
        thread.setRunning(false);
        while (retry) {
            try {
                thread.join();
                retry = false;
            } catch (InterruptedException e) {
            }
        }
        thread.clearSounds();
        ((GameActivity)mContext).exit();
    }

    /**
     * Constitutes the four divers that can appear on screen.
     * @author Adam
     *
     */
    private class Diver{
    	
    	private int mPosition;
    	private int mState;
    	
    	private int mScreenWidth;
    	private int mScreenHeight;
    	
    	private static final int DIVER_INACTIVE = 0;
    	private static final int DIVER_INCOMING = 1;
    	private static final int DIVER_STANDBY = 2;
    	private static final int DIVER_ASSAULT = 3;
    	private static final int DIVER_INJURED = 4;
    	
    	private static final int TOP_LEFT = 0;
    	private static final int TOP_RIGHT = 1;
    	private static final int BOT_RIGHT = 2;
    	private static final int BOT_LEFT = 3;
    	
    	private float mHeadX;
    	private float mHeadY;
    	private int mHeadStartX;
    	
    	private float mArmX;
    	private float mArmY;
    	
    	private float mHeadSpeedY;
    	private float mArmSpeed;
    	
    	private int mArmDestX;
    	
    	private int mHeadWidth;
    	private int mHeadHeight;
    	private int mArmWidth;
    	private int mArmHeight;
    	
    	//Boolean to tell if arm is approaching or not during standby
    	private boolean mApproach;
    	private int mArmStandbyDest;
    	private int mArmStandbyCount;
    	
    	private boolean mLose;
    	
    	private Random r;
    	
    	private BitmapDrawable[] mHeadImageColours;
    	private BitmapDrawable[] mArmImageColours;
    	private BitmapDrawable[] mArmInjuredImageColours;
    	
    	Matrix m;
    	
    	//TODO nicer colours
    	//red, green, blue, yellow, orange, violet
    	private final int[] mColours = {Color.argb(255,255,25,25), Color.argb(255, 36, 242, 6),
    		Color.argb(255,0,0,255), Color.argb(255,241,54,10), Color.argb(255,255,155,45),
    		Color.argb(255,184,0,217)};
    	private int mCurrentColour;    	
    	
    	private boolean mFirst;
    	
    	
    	/**
    	 * Up to four divers can be active on screen at once, each with a position (0-4),
    	 * starting at the top left and moving clockwise.
    	 * @param pos, dif
    	 */
    	public Diver (int pos){
    		r = new Random();
    		mLose = false;
    		mPosition = pos;
    		m = new Matrix();
    		setState(DIVER_INACTIVE);
    	}
    	
    	public void setSize (int screenWidth, int screenHeight){
    		mScreenWidth = screenWidth;
    		mScreenHeight = screenHeight;
    		
    		mHeadHeight = (int)Math.round(screenHeight/2.2);
    		mHeadWidth = (int)Math.round(mHeadHeight/1.08);
    		mHeadStartX = (int) Math.round(-2*mHeadWidth/3);
    		
    		mHeadSpeedY = mHeadHeight/30;
    		
    		mArmHeight = (int)Math.round(screenHeight/1.67);
    		mArmWidth = (int)Math.round(mArmHeight*1.06);
    		mArmDestX = (int)Math.round(screenWidth/1.82 - mArmWidth);
    	}
    	
		public void setImage (BitmapDrawable headImage, BitmapDrawable armImage, BitmapDrawable armInjuredImage){

    		if (mPosition == TOP_LEFT){
    			m.preScale(-1,-1);
    		}else if (mPosition == TOP_RIGHT){
    			m.preScale(1,-1);
    		}else if (mPosition == BOT_RIGHT){
    			m.preScale(1, 1);
    		}else{
    			m.preScale(-1, 1);
    		}
    		
    		//populate different coloured images
    		
    		Canvas c = new Canvas();
    		Paint paint = new Paint();
    		paint.setFilterBitmap(false);
    		Bitmap result;
    		Bitmap r;
    		Bitmap dst;
    		Bitmap src;
    		
    		mHeadImageColours = new BitmapDrawable[mColours.length];
    		src = headImage.getBitmap();
    		dst = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), m, true);
    		result = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
    		
    		for (int i=0; i<mColours.length; i++){
    			r = result.copy(Bitmap.Config.ARGB_8888, true);
    			c.setBitmap(r);
    			paint.setColorFilter(new PorterDuffColorFilter(mColours[i], Mode.MULTIPLY));
    			c.drawBitmap(dst, 0, 0, paint);
    			mHeadImageColours[i] = new BitmapDrawable(getResources(), r);
    		}
 
    		mArmImageColours = new BitmapDrawable[mColours.length];
    		src = armImage.getBitmap();
    		dst = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), m, true);
    		result = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
    		
    		for (int i=0; i<mColours.length; i++){
    			r = result.copy(Bitmap.Config.ARGB_8888, true);
    			c.setBitmap(r);
    			paint.setColorFilter(new PorterDuffColorFilter(mColours[i], Mode.MULTIPLY));
    			c.drawBitmap(dst, 0, 0, paint);
    			mArmImageColours[i] = new BitmapDrawable(getResources(), r);
    		}
    		
    		mArmInjuredImageColours = new BitmapDrawable[mColours.length];
    		src = armInjuredImage.getBitmap();
    		dst = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), m, true);
    		result = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
    		
    		for (int i=0; i<mColours.length; i++){
    			r = result.copy(Bitmap.Config.ARGB_8888, true);
    			c.setBitmap(r);
    			paint.setColorFilter(new PorterDuffColorFilter(mColours[i], Mode.MULTIPLY));
    			c.drawBitmap(dst, 0, 0, paint);
    			mArmInjuredImageColours[i] = new BitmapDrawable(getResources(), r);
    		}   		
    		
    	}
    	
    	private void setState (int state){
    		mState = state;
    	}
    	
		public void init(int dif, boolean first){
    		//TODO balance difficulty
    		if (mState == DIVER_INACTIVE){
    			mArmSpeed = (float) ((1+dif*0.15)*(mArmHeight/47));
    			
    			mHeadX = mHeadStartX;
    			mHeadY = -mHeadHeight;
    			
    			mFirst = first;
    			
    			mCurrentColour = r.nextInt(mColours.length);
    			
    			setState(DIVER_INCOMING);
    		}
    	}
    	
    	/**
    	 * Processes that should be done every tick, i.e. updating movement and checking for endgame.
    	 */
    	public void doTick(){
    		
    		if (mState == DIVER_INCOMING){
    			if (mHeadY >= -mHeadSpeedY){
    				mHeadY = 0;
    				mHeadX = 0;
    				
    				mApproach = false;
    				
    				mArmX = mArmDestX - mArmHeight;
    				mArmY = -mArmHeight;
    				
    				mArmStandbyDest = (int)mArmX+1;
    				
    				mArmStandbyCount = r.nextInt(100)+25;
    				
    				if (mFirst) setState(DIVER_ASSAULT);
    				else setState(DIVER_STANDBY);
    			}else{
    				mHeadY += mHeadSpeedY;
    				mHeadX = mHeadStartX*(-mHeadY/mHeadHeight);
    			}
    			
    		}else if (mState == DIVER_STANDBY){
    			mArmStandbyCount--;
    			if (mArmStandbyCount < 0){
    				setState(DIVER_ASSAULT);
    			}else{
	    			if (mApproach){
	    				mArmX += mArmSpeed;
	    				mArmY += mArmSpeed;
	    				if (mArmX >= mArmStandbyDest){
	    					mApproach = false;
	    					float dx = mArmX - ((mArmDestX-100) - mArmHeight);
	    					mArmStandbyDest = (int) Math.round(mArmX - dx/3 - r.nextInt((int)Math.round(dx/3)));
	    				}
	    			}else{
	    				mArmX -= mArmSpeed;
	    				mArmY -= mArmSpeed;
	    				if (mArmX <= mArmStandbyDest){
	    					mApproach = true;
	    					float dx = mArmDestX-mArmX;
	        				mArmStandbyDest = (int) Math.round(mArmX + dx/3 + r.nextInt((int)Math.round(dx/3)));
	    				}
	    			}
    			}
    		}else if (mState == DIVER_ASSAULT){
    			if (mArmX >= mArmDestX){
    				mLose = true;
    			}else{
    				if (mArmX >= mArmDestX - mArmSpeed){
    					mArmX = mArmDestX;
    					mArmY = 0;
    				}else{
    					mArmX += mArmSpeed;
    					mArmY += mArmSpeed;
    				}
    			}
    		}else if (mState == DIVER_INJURED){
    			if (mArmY > -mArmHeight || mHeadY > -mHeadHeight){
    				mArmX -= mArmSpeed;
    				mArmY -= mArmSpeed;
    				
	    			mHeadY -= mHeadSpeedY*1.3;
	    			mHeadX = mHeadStartX*(-mHeadY/mHeadHeight);
	    			return;
    			}

    			reset();
    			
    		}
    	}
    	
    	public boolean getHeadRect (Rect rect){
    		if (mState == DIVER_INACTIVE) return false;
    		
    		if (mPosition == TOP_LEFT || mPosition == TOP_RIGHT){
    			rect.top = (int)Math.round(mHeadY);
    			rect.bottom = (int)Math.round(mHeadY+mHeadHeight);
    		}else{
    			rect.top = (int)Math.round(mScreenHeight-mHeadY-mHeadHeight);
    			rect.bottom = (int)Math.round(mScreenHeight-mHeadY);
    		}
    		
    		if (mPosition == TOP_LEFT || mPosition == BOT_LEFT){
    			
    			rect.left = (int)Math.round(mHeadX);
    			rect.right = (int)Math.round(mHeadX + mHeadWidth);
    		}else{
    			rect.left = (int)Math.round(mScreenWidth-mHeadX-mHeadWidth);
    			rect.right = (int)Math.round(mScreenWidth-mHeadX);
    		}
    		
    		return true;
    	}
    	
    	public boolean getArmRect (Rect rect){
    		if (mState == DIVER_INACTIVE || mState == DIVER_INCOMING) return false;
    		
    		if (mPosition == TOP_LEFT || mPosition == TOP_RIGHT){
    			rect.top = (int)Math.round(mArmY);
    			rect.bottom = (int)Math.round(mArmY+mArmHeight);
    		}else{
    			rect.top = (int)Math.round(mScreenHeight-mArmY-mArmHeight);
    			rect.bottom = (int)Math.round(mScreenHeight-mArmY);
    		}
    		
    		if (mPosition == TOP_LEFT || mPosition == BOT_LEFT){    			
    			rect.left = (int)Math.round(mArmX);
    			rect.right = (int)Math.round(mArmX + mArmWidth);
    		}else{
    			rect.left = (int)Math.round(mScreenWidth-mArmX-mArmWidth);
    			rect.right = (int)Math.round(mScreenWidth-mArmX);
    		}
    		
    		return true;
    	}
    	
    	public boolean checkCollision(int clamWidth, int clamHeight){
    		//TODO tweak hitbox
    		if ((mState == DIVER_ASSAULT || mState == DIVER_STANDBY) && mArmX > mArmDestX-clamWidth/4 && mArmY > (mScreenHeight)/2-mArmHeight){
    			setState(DIVER_INJURED);
    			mLose = false;
    			return true;
    		}
    		return false;
    	}
    	
    	public boolean checkLose(){
    		return mLose;
    	}
    	
    	public boolean isActive(){
    		return !(mState == DIVER_INACTIVE);
    	}
    	
    	public BitmapDrawable getHeadImage(){    
    		return mHeadImageColours[mCurrentColour];    		
    	}
    	
    	public BitmapDrawable getArmImage(){
    		if (mState == DIVER_INJURED) return mArmInjuredImageColours[mCurrentColour];
    		
    		return mArmImageColours[mCurrentColour];
    	}
    	
    	public void reset(){
    		setState(DIVER_INACTIVE);
    		mLose = false;
    	}
    }
    
    
}
