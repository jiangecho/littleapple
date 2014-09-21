package com.echo.littleapple;

import java.util.Random;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class GameSurfaceView extends SurfaceView implements SurfaceHolder.Callback{
	
	private static final int COLUMN = 4;
	private static final int OK = 0;
	private static final int FAIL = 1;
	public static final int TIME_OUT = 2;
	private int row;
	private int cellWidth;
	private int cellHeight;
	private int firstCellHeight;
	
	private int width, height;
	
	private Paint linePaint;
	private Paint applePaint;
	
	private int[][] apples = null;
	private Random random;
	private Rect rect;
	private Bitmap bitmapApple;
	private Bitmap bitmapError;
	private Bitmap bitmapClick;
	private Bitmap bitmapMine;
	
	private GameEventListner listner;
	private int status;
	private static final int STATUS_STOP = 0;
	private static final int STATUS_START = 1;
	private static final int STATUS_FAIL = 2;
	
	private int score;
	
	private int moveStepHeight;
	private int moveYOffset = 0;
	private Handler handler;
	private Handler animationHandler;
	private HandlerThread animationHandlerThread;
	private AnimationTask animationTask;
	
	private SoundPool soundPool;
	private int[] sounds;
	private float audioMaxVolumn;
	private float audioCurrentVolumn;
	private float volumnRatio;
	private Context context;
	
	private HandlerThread soundPoolThread;
	private Handler soundPoolHandler;

	int left, top, right, bottom;
	
	private Canvas canvas;
	private SurfaceHolder holder;
	
	//cell types
	private static final int CELL_TYPE_BLANK = 0;
	private static final int CELL_TYPE_APPLE_OK = 1;
	private static final int CELL_TYPE_APPLE_CLICKED = 2;
	private static final int CELL_TYPE_ERROR = 3;
	private static final int CELL_TYPE_MINE = 4;
	
	boolean animationCancled = false;
	
	private int mode = GameActiviy.MODE_CLASSIC;
	private int type = GameActiviy.TYPE_CLASSIC_30S;
	
	private int level = GameActiviy.LEVEL_EASY;

	public GameSurfaceView(Context context) {
		this(context, null);
		
	}
	

	public GameSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}


	public GameSurfaceView(Context context, AttributeSet attrs) {
		super(context, attrs);

		linePaint = new Paint();
		linePaint.setAntiAlias(true);
		
		
		applePaint = new Paint(linePaint);
		
		bitmapApple = BitmapFactory.decodeResource(context.getResources(), R.drawable.apple);
		bitmapError = BitmapFactory.decodeResource(context.getResources(), R.drawable.error);
		bitmapClick = BitmapFactory.decodeResource(context.getResources(), R.drawable.click);
		bitmapMine = BitmapFactory.decodeResource(context.getResources(), R.drawable.mine);
		rect = new Rect();
		
		random = new Random();
		status = STATUS_STOP;
		
		score = 0;

		handler = new Handler();
		animationHandlerThread = new HandlerThread("animation");
		animationHandlerThread.start();
		animationHandler = new Handler(animationHandlerThread.getLooper());
		
		this.context = context;
		// init sound play
		initSoundPool();
		
		holder = getHolder();
		holder.addCallback(this);
		
		animationTask = new AnimationTask();
	}


	private void doDraw() {
		int i, j;
		
			
		canvas = holder.lockCanvas();

		synchronized (holder) {
			canvas.drawColor(Color.WHITE);
			//draw the first horizontal lines, maybe hidden
			canvas.drawLine(0, moveYOffset + firstCellHeight - cellHeight, width, moveYOffset + firstCellHeight - cellHeight, linePaint);
			//draw horizontal lines
			for (i = 0; i < row ; i++) {
				canvas.drawLine(0, moveYOffset + firstCellHeight + cellHeight * i, width, moveYOffset + firstCellHeight + cellHeight * i, linePaint);
				//canvas.drawLine(0,  firstCellHeight + cellHeight * i, width, firstCellHeight + cellHeight * i, linePaint);
			}
			
			//draw vertical lines
			for (i = 0; i < COLUMN ; i++) {
				canvas.drawLine(cellWidth * i, 0, cellWidth * i, height, linePaint);
			}
	
			// draw applse
			for (i = 0; i < row; i++) {
				for (j = 0; j < COLUMN; j++) {
					left = j * cellWidth;
					top = moveYOffset + ((i >= 1) ? (firstCellHeight + (i - 1) * cellHeight) : (firstCellHeight - cellHeight)); 
					right = (j + 1) * cellWidth;
					bottom = moveYOffset + firstCellHeight + i * cellHeight;
					rect.set(left, top, right, bottom);

					if (apples[i][j] == CELL_TYPE_BLANK) {
						// do nothing
					}else if(apples[i][j] == CELL_TYPE_APPLE_OK){
						canvas.drawBitmap(bitmapApple, null, rect, applePaint);
					}else if(apples[i][j] == CELL_TYPE_ERROR){
						canvas.drawBitmap(bitmapError, null, rect, applePaint);
					}else if(apples[i][j] == CELL_TYPE_APPLE_CLICKED){
						canvas.drawBitmap(bitmapClick, null, rect, applePaint);
					}else if (apples[i][j] == CELL_TYPE_MINE) {
						canvas.drawBitmap(bitmapMine, null, rect, applePaint);
					}
				}
				
			}
		
		}
		holder.unlockCanvasAndPost(canvas);
		
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		
		width = MeasureSpec.getSize(widthMeasureSpec);
		cellWidth = width / COLUMN;
		cellHeight = cellWidth;
		
		height = MeasureSpec.getSize(heightMeasureSpec);
		firstCellHeight = height % cellHeight;
		row  = height / cellHeight;
		
		if (firstCellHeight > 0) {
			row  += 1;
		}
		
		moveStepHeight = cellHeight / 10;
		
		if (apples == null) {
			apples = new int[row][COLUMN];
			randomApples();
		}


	}
	
	public void reset(){
		this.score = 0;
		status = STATUS_STOP;
		randomApples();
		moveYOffset = 0;
		doDraw();
	}
	
	public void setMode(int mode){
		this.mode = mode;
	}
	
	public void setType(int type){
		this.type = type;
	}
	
	public void recover(){
		status = STATUS_STOP;
		moveYOffset = 0;

		for (int i = 0; i < row; i++) {
			for (int j = 0; j < COLUMN; j++) {
				if (apples[i][j] == CELL_TYPE_ERROR) {
					apples[i][j] = CELL_TYPE_BLANK;
				}
			}
		}

		doDraw();
	}
	
	private void randomApples(){
		int columnIndex;
		int lastColumnIndex = -1;
		for (int i = 0; i < row; i++) {
			for (int j = 0; j < COLUMN; j++) {
				apples[i][j] = CELL_TYPE_BLANK;
			}
		}
		
		if (type == GameActiviy.TYPE_CLASSIC_DISCONTINUOUS 
				|| type == GameActiviy.TYPE_GRAVITY_DISCONTINUOUS) {
			for (int i = 0; i < row - 1; i++) {
				columnIndex = random.nextInt(COLUMN);
				if (columnIndex == lastColumnIndex) {
					columnIndex = (columnIndex + 1) % COLUMN;
				}
				lastColumnIndex = columnIndex;
				apples[i][columnIndex] = CELL_TYPE_APPLE_OK;
			}
		}
		// should not add any mine in the initial state, otherwise, when the user
		// firstly play mine type, and then play other type, there will be a bug
//		else if(type == GameActiviy.TYPE_GRAVITY_MINE){
//			for (int i = 0; i < row - 1; i++) {
//				columnIndex = random.nextInt(COLUMN);
//				apples[i][columnIndex] = CELL_TYPE_APPLE_OK;
//				
//				if (1 == random.nextInt(row)) {
//					apples[i][columnIndex] = CELL_TYPE_MINE;
//				}
//			}
//			
//		}
		else {
			for (int i = 0; i < row - 1; i++) {
				columnIndex = random.nextInt(COLUMN);
				apples[i][columnIndex] = CELL_TYPE_APPLE_OK;
			}
			
		}

	}
	
	
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		//return super.onTouchEvent(event);
		if (status == STATUS_FAIL) {
			return false;
		}
		
		if (event.getAction() != MotionEvent.ACTION_DOWN) {
			return false;
		}else {
			int x = (int) event.getX();
			int y = (int) event.getY();
			
			int x_index = x / cellWidth;
			int y_index = row - 1 - (height - y) / cellHeight;
			
			if (mode == GameActiviy.MODE_GRAVITY) {
				
				if (y_index < 1) {
					return true;
				}
				
				if (apples[y_index][x_index] == CELL_TYPE_APPLE_CLICKED) {
					if (apples[y_index - 1][x_index] == CELL_TYPE_APPLE_OK) {
						score ++;
						playGameSoundEffect(OK);
						apples[y_index - 1][x_index] = CELL_TYPE_APPLE_CLICKED;
						listner.onScoreUpdate(score);
						return true;
					}
					
				}
				
				//game over
				if ((isBottomAppleRow(x_index, y_index) && apples[y_index][x_index] != CELL_TYPE_APPLE_OK)
						|| apples[y_index][x_index] == CELL_TYPE_MINE) {
					apples[y_index][x_index] = CELL_TYPE_ERROR;
					playGameSoundEffect(FAIL);
					status = STATUS_FAIL;
					doDraw();
					handler.postDelayed(new Runnable() {
						
						@Override
						public void run() {
							// TODO Auto-generated method stub
							if (listner != null) {
								listner.onGameOver();
						
							}
							
						}
					}, 200);
					
					
				} else if(isBottomAppleRow(x_index, y_index) && apples[y_index][x_index] == CELL_TYPE_APPLE_OK){
					score ++;
					playGameSoundEffect(OK);
					apples[y_index][x_index] = CELL_TYPE_APPLE_CLICKED;
					if (status == STATUS_STOP) {
						status = STATUS_START;
						// move down
						startMoveAnimation();
						if (listner != null) {
							listner.onGameStart();
							listner.onScoreUpdate(score);
						}
					}else {
						listner.onScoreUpdate(score);
					}
					
				}else {
					if (status != STATUS_START) {
						return true;
					}
					
					// fault-tolerant
					// very loose
					if (level == GameActiviy.LEVEL_EASY || level == GameActiviy.LEVEL_NORMAL) {
						if (isBottomAppleRow(x_index, y_index - 1) && apples[y_index - 1][x_index] == CELL_TYPE_APPLE_OK) {
							score ++;
							playGameSoundEffect(OK);
							apples[y_index - 1][x_index] = CELL_TYPE_APPLE_CLICKED;
							listner.onScoreUpdate(score);
						}
					// very strict
					}else {
						apples[y_index][x_index] = CELL_TYPE_ERROR;
						playGameSoundEffect(FAIL);
						status = STATUS_FAIL;
						doDraw();
						handler.postDelayed(new Runnable() {
							
							@Override
							public void run() {
								// TODO Auto-generated method stub
								if (listner != null) {
									listner.onGameOver();
							
								}
								
							}
						}, 200);
						
					}

				}
	
				return true;
				
			}else {
				if (y_index < row - 3) {
					return true;
				}
				
				if (moveYOffset > 0) {
					animationCancled = true;
					moveYOffset = 0;
					addNewCell();
					doDraw();
				}
				
				//control fault-tolerant
				if (level == GameActiviy.LEVEL_EASY) {
					
				}
				//game over
				// very loose
				if ((level == GameActiviy.LEVEL_EASY && y_index == row - 2 && apples[y_index][x_index] != CELL_TYPE_APPLE_OK)
						|| (level == GameActiviy.LEVEL_NORMAL && (y_index == row - 2 || y_index == row - 3) && apples[y_index][x_index] != CELL_TYPE_APPLE_OK)
						// very strict
						|| (level == GameActiviy.LEVEL_HARD && apples[y_index][x_index] != CELL_TYPE_APPLE_OK)) {
					
//				}
//				// very loose
////				if (y_index == row - 2 && apples[y_index][x_index] != CELL_TYPE_APPLE_OK) {
////					apples[row - 2][x_index] = CELL_TYPE_ERROR;
//				// most strict
//				if (apples[y_index][x_index] != CELL_TYPE_APPLE_OK) {

					apples[y_index][x_index] = CELL_TYPE_ERROR;
					playGameSoundEffect(FAIL);
					status = STATUS_FAIL;
					doDraw();
					handler.postDelayed(new Runnable() {
						
						@Override
						public void run() {
							// TODO Auto-generated method stub
							if (listner != null) {
								listner.onGameOver();
						
							}
							
						}
					}, 200);
					
					
				} else if(y_index == row - 2 && apples[y_index][x_index] == CELL_TYPE_APPLE_OK){
					score ++;
					playGameSoundEffect(OK);
					apples[row - 2][x_index] = CELL_TYPE_APPLE_CLICKED;
					if (status == STATUS_STOP) {
						status = STATUS_START;
						if (listner != null) {
							listner.onGameStart();
							listner.onScoreUpdate(score);
						}
					}else {
						listner.onScoreUpdate(score);
					}
					
					// move down
					startMoveAnimation();
				}else {
					if (status != STATUS_START) {
						return true;
					}
					if (apples[row - 2][x_index] == CELL_TYPE_APPLE_OK) {
						score ++;
						playGameSoundEffect(OK);
						apples[row - 2][x_index] = CELL_TYPE_APPLE_CLICKED;
						listner.onScoreUpdate(score);
						
						// move down
						startMoveAnimation();
						
					}
				}
				
			}

			return true;
		}
		
	}


	public void setGameEventListener(GameEventListner listner){
		this.listner = listner;
	}
	
	private void startMoveAnimation(){

		animationHandler.post(animationTask);
	}
	
	private void initSoundPool(){
		soundPoolThread = new HandlerThread("test");
		soundPoolThread.start();
		soundPoolHandler = new Handler(soundPoolThread.getLooper(), null);
		soundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 0);
		
		sounds = new int[3];
		sounds[0] = soundPool.load(context, R.raw.ok, 1);
		sounds[1] = soundPool.load(context, R.raw.fail, 1);
		sounds[2] = soundPool.load(context, R.raw.time_out, 1);

		AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
		audioMaxVolumn = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		audioCurrentVolumn = am.getStreamVolume(AudioManager.STREAM_MUSIC);
		
		volumnRatio = audioCurrentVolumn / audioMaxVolumn;
	}
	
	
	public void playGameSoundEffect(final int type){
		//soundPool.pla
		soundPoolHandler.post(new Runnable() {
			
			@Override
			public void run() {
				soundPool.play(sounds[type], volumnRatio, volumnRatio, 1, 0, 1);
				
			}
		});

	}
	
	
	public interface GameEventListner{
		public void onGameOver();
		public void onGameStart();
		public void onScoreUpdate(int score);
	}
	
	public int getScore(){
		return this.score;
	}
	
	public void setScore(int score){
		this.score = score;
	}
	
	public void setLevel(int level){
		this.level = level;
	}
	
	public int getLevel(){
		return this.level;
	}


	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		doDraw();
	}


	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
	}
	
	private class AnimationTask implements Runnable{

		@Override
		public void run() {
			
			if (status != STATUS_START) {
				return;
			}
			if (mode == GameActiviy.MODE_GRAVITY) {
				if (moveYOffset < cellHeight) {
					moveYOffset += moveStepHeight;
					doDraw();
					animationHandler.postDelayed(animationTask, 10);
				}else {
					// the last one move out
					for (int i = 0; i < COLUMN; i++) {
						if (apples[row - 1][i] == CELL_TYPE_APPLE_OK) {
							apples[row - 1][i] = CELL_TYPE_ERROR;
							moveYOffset -= 2 * moveStepHeight;
							status = STATUS_FAIL;
							doDraw();
							playGameSoundEffect(FAIL);
							handler.postDelayed(new Runnable() {
								@Override
								public void run() {
									// TODO Auto-generated method stub
									if (listner != null) {
										listner.onGameOver();
								
									}
									
								}
							}, 200);
							return;
						}
					}
					moveYOffset = 0;
					addNewCell();
					doDraw();
					animationHandler.postDelayed(animationTask, 10);
				}
				return;
			}else {
				if (animationCancled) {
					animationCancled = false;
					return;
				}
				if (moveYOffset < cellHeight) {
					moveYOffset += moveStepHeight;
					doDraw();
					animationHandler.postDelayed(animationTask, 4);
				}else {
					moveYOffset = 0;
					addNewCell();
					doDraw();
				}
				
			}
			
		}
	}
	
	private void addNewCell(){
		for (int i = row - 1; i > 0; i--) {
			for (int j = 0; j < COLUMN; j++) {
				apples[i][j] = apples[i - 1][j]; 
				apples[i - 1][j] = CELL_TYPE_BLANK;
			}
		}
		int x_index = random.nextInt(COLUMN);
		
		if (type == GameActiviy.TYPE_CLASSIC_DISCONTINUOUS || type == GameActiviy.TYPE_GRAVITY_DISCONTINUOUS) {
			int lastColumnIndex = 0;
			for (int i = 0; i < COLUMN; i++) {
				if (apples[1][i] == CELL_TYPE_APPLE_OK) {
					lastColumnIndex = i;
					break;
				}
			}
			if (x_index == lastColumnIndex) {
				x_index = (x_index + 1) % COLUMN;
			}
			apples[0][x_index] = CELL_TYPE_APPLE_OK;
			
		} else if (type == GameActiviy.TYPE_GRAVITY_MINE) {
			if (1 == random.nextInt(row)) {
				apples[0][x_index] = CELL_TYPE_MINE;
			}else {
				apples[0][x_index] = CELL_TYPE_APPLE_OK;
			}
		} else {
			apples[0][x_index] = CELL_TYPE_APPLE_OK;
		}
	}
	
	private boolean isBottomAppleRow(int x_index, int y_index){
		if (apples[y_index][x_index] == CELL_TYPE_APPLE_OK) {
			if (y_index == row - 1) {
				return true;
			}else {
				for (int i = y_index + 1; i < row; i++) {
					for (int j = 0; j < COLUMN; j++) {
						if (apples[i][j] == CELL_TYPE_APPLE_OK) {
							return false;
						}
						
					}
				}
				return true;
			}
		}else {
			boolean tmp1, tmp2;
			if (y_index == row - 1) {
				for (int i = 0; i < COLUMN; i++) {
					if (apples[y_index][i] == CELL_TYPE_APPLE_OK) {
						return true;
					}
				}
				return false;
			}else {
				tmp1 = tmp2 = false;
				for (int i = 0; i < COLUMN; i++) {
					if (apples[y_index][i] == CELL_TYPE_APPLE_OK) {
						tmp1 = true;
					}
				}
				
				if (tmp1) {
					for (int i = y_index + 1; i < row; i++) {
						for (int j = 0; j < COLUMN; j++) {
							if (apples[i][j] == CELL_TYPE_APPLE_OK) {
								return false;
							}
							
						}
					}
					return true;
					
				}else {
					return false;
				}
				
			}
			

		}

	}
	
}
