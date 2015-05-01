package edu.elon.cs.mobile.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.FontMetrics;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {

	private Context context;
	private SurfaceHolder surfaceHolder;
	private GameViewThread thread;
	private static final int GRID_SIZE = 7;
	private static final int GRID_PLUS_PADDING = GRID_SIZE + 2;

	private enum Direction { NORTH, WEST, SOUTH,  EAST;
	//holy crap whaaat
	public Direction getNext() {
	return values()[(ordinal() + 1) % values().length];
	}
	};

	public GameView(Context context, AttributeSet attrs) {
		super(context, attrs);

		this.context = context;
		surfaceHolder = getHolder();
		surfaceHolder.addCallback(this);

		thread = new GameViewThread(context);
	}

	// SurfaceHolder.Callback
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (thread.getState() == Thread.State.TERMINATED) {
			thread = new GameViewThread(context);
		}

		thread.setIsRunning(true);
		thread.start();
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// System.out.println("onSizeChanged: width " + width + ", height "
		// + height);
		thread.height = height / GRID_PLUS_PADDING;
		thread.width = width / GRID_PLUS_PADDING;
		thread.createGemRects();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		boolean retry = true;
		thread.setIsRunning(false);

		while (retry) {
			try {
				thread.join();
				retry = false;
			} catch (InterruptedException e) {
			}
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		thread.onTouchEvent(event);
		return true;
	}

	// Game Loop Thread
	private class GameViewThread extends Thread {

		private Direction fallDirection = GameView.Direction.WEST;
		private int score;// score for player
		private int moveCounter = 30; // amount of matches left
		private boolean isRunning;
		private long lastTime;
		private float touchX, touchY;
		private int frames; // for framerate
		private Gem[][] gBoard; // the gameboard
		private ArrayList<Integer> colors; // colors of possible gems
		private Random random; // to create new random gems
		private long nextUpdate;
		private RectF selectedRect; // the current rectangle the user is
									// touching
		private int currentColor; // the current color of the chain the user is
									// creating
		// copyonwrite to avoid ConcurrentModificationException
		private CopyOnWriteArrayList<Gem> selectedList; // the chain of gems the
														// user has
		// valid gems
		private float width; // width of one tile
		private float height; // height of one tile

		public GameViewThread(Context context) {
			random = new Random();
			isRunning = false;
			frames = 0;
			colors = new ArrayList<Integer>();
			colors.add(Color.RED);
			colors.add(Color.CYAN);
			colors.add(Color.GREEN);
			colors.add(Color.BLACK);
			// colors.add(Color.YELLOW);
			// colors.add(Color.argb(255, 255, 0, 255));// purple
			selectedList = new CopyOnWriteArrayList<Gem>();
			createBoard();
		}

		private void createBoard() {
			gBoard = new Gem[GRID_SIZE][GRID_SIZE];
			for (int i = 0; i < GRID_SIZE; i++) {
				for (int j = 0; j < GRID_SIZE; j++) {
					// i think i swapped i and j for row col?
					gBoard[i][j] = new Gem(getRandomColor(), i, j);
				}
			}
		}

		private int getRandomColor() {
			return colors.get(random.nextInt(colors.size()));
		}

		public void setIsRunning(boolean isRunning) {
			this.isRunning = isRunning;
		}

		private void doDraw(Canvas canvas) {
			// Draw the background...
			Paint background = new Paint();
			background.setColor(Color.WHITE);
			canvas.drawRect(0, 0, getWidth(), getHeight(), background);

			// Draw the board...

			// Define colors for the grid lines
			Paint dark = new Paint();
			dark.setColor(Color.BLACK);

			Paint light = new Paint();
			light.setColor(Color.YELLOW);

			// Draw the minor grid lines
			for (int i = 1; i < GRID_PLUS_PADDING; i++) {
				canvas.drawLine(width, i * height, getWidth() - width, i
						* height, dark); // width
				canvas.drawLine(i * width, height, i * width, getHeight()
						- (height * 1), dark); // height

			}
			//draw the falling direction marker
			drawFallMarker(canvas);

			// Draw the gems...

			// Define color and style for numbers
			Paint foreground = new Paint(Paint.ANTI_ALIAS_FLAG);
			foreground.setStyle(Style.FILL);
			foreground.setTextSize(height * 0.75f);
			foreground.setTextScaleX(width / height);
			foreground.setTextAlign(Paint.Align.CENTER);

			// Draw the number in the center of the tile
			FontMetrics fm = foreground.getFontMetrics();
			// Centering in X: use alignment (and X at midpoint)
			float x = width / 2;
			// Centering in Y: measure ascent/descent first
			float y = height / 2 - (fm.ascent + fm.descent) / 2;
			for (int i = 1; i < 8; i++) {
				for (int j = 1; j < 8; j++) {
					foreground.setColor(gBoard[i - 1][j - 1].getColor());
					canvas.drawText("G", i * width + x, j * height + y,
							foreground);
				}
			}

			Paint selectorPaint = new Paint();
			selectorPaint.setColor(Color.GRAY);
			selectorPaint.setAlpha(150);
			for (Gem g : selectedList) {
				canvas.drawRect(g.getRect(), selectorPaint);
			}

			if (selectedRect != null) {
				foreground.setColor(Color.YELLOW);
				canvas.drawRect(selectedRect, foreground);
			}
			
			dark.setStyle(Style.FILL);
			dark.setTextSize(height * 0.50f);
			dark.setTextScaleX(width / height);
			dark.setTextAlign(Paint.Align.CENTER);
			// draw score and move counter
			canvas.drawText("Score is: " + score, width +60, height - 20 ,
					dark);

			// draw score and move counter
						canvas.drawText("Moves left: " + moveCounter, getWidth() - width - 80, height - 20 ,
								dark);
		}

		private void createGemRects() {
			for (int i = 0; i < 7; i++) {
				for (int j = 0; j < 7; j++) {
					gBoard[i][j].setRect(new RectF((i + 1) * width, (j + 1)
							* height, ((i + 1) * width) + width,
							((j + 1) * height) + height));
					// System.out.println(gBoard[i][j].getRect());
				}
			}
		}
		
		

		private void doUpdate(double elapsed) {
			fall();
			// board.doUpdate(elapsed);
		}

		// the marker right now shows which direction it is currently falling, 
		//changing to be the direction it will fall
		private void drawFallMarker(Canvas canvas) {
			Paint paint = new Paint();
			paint.setColor(Color.RED);
			switch (fallDirection) {
			case WEST:
				canvas.drawLine(0, height, getWidth(), height, paint); // width
				canvas.drawCircle(getWidth()/2, height - 20, 10, paint);
				break;
			case NORTH:
				canvas.drawLine(width, 0, width, getHeight(), paint); // height

				canvas.drawCircle(width - 20, getHeight() /2, 10, paint);
				break;
			case EAST:
				canvas.drawLine(0, height * (GRID_SIZE + 1), getWidth(), height * (GRID_SIZE+1), paint); // width

				canvas.drawCircle(getWidth()/2, getHeight() - 20, 10, paint);
				break;
			case SOUTH:
				canvas.drawLine(width * (GRID_SIZE + 1), 0, width* (GRID_SIZE+1), getHeight(), paint); // height

				canvas.drawCircle(getWidth() - 20, getHeight() /2, 10, paint);
				break;
			}
		}

		private void fall() {
			switch (fallDirection) {
			case NORTH:
				for (int i = 0; i < gBoard.length; i++) {
					for (int j = 0; j < gBoard[0].length - 1; j++) {
						Gem me = gBoard[i][j];
						// System.out.println(me);
						Gem below = gBoard[i][j + 1];
						if (below.getColor() == 0) {
							below.setColor(me.getColor());
							me.setColor(0);
						}
					}
				}
				//north works
				for (int i = 0; i < gBoard.length; i++) {
					Gem me = gBoard[i][0];
					if(me.getColor()==0) {
						me.setColor(getRandomColor());
					}
				}
				break;
			case EAST:
				for (int i = 0; i < gBoard.length - 1; i++) {
					for (int j = 0; j < gBoard[0].length; j++) {
						Gem me = gBoard[i][j];
						// System.out.println(me);
						Gem below = gBoard[i + 1][j];
						if (below.getColor() == 0) {
							below.setColor(me.getColor());
							me.setColor(0);
						}
					}
				}
				// east works
				for (int i = 0; i < gBoard.length; i++) {
					Gem me = gBoard[0][i];
					if(me.getColor()==0) {
						me.setColor(getRandomColor());
					}
				}
				break;
			case SOUTH:
				for (int i = 0; i < gBoard.length; i++) {
					for (int j = 1; j < gBoard[0].length; j++) {
						Gem me = gBoard[i][j];
						// System.out.println(me);
						Gem below = gBoard[i][j - 1];
						if (below.getColor() == 0) {
							below.setColor(me.getColor());
							me.setColor(0);
						}
					}
				}
				//south works
				for (int i = 0; i < gBoard[0].length; i++) {
					Gem me = gBoard[i][gBoard[0].length-1];
					if(me.getColor()==0) {
						me.setColor(getRandomColor());
					}
				}
				break;
			case WEST:
				for (int i = 1; i < gBoard.length; i++) {
					for (int j = 0; j < gBoard[0].length; j++) {
						Gem me = gBoard[i][j];
						// System.out.println(me);
						Gem below = gBoard[i - 1][j];
						if (below.getColor() == 0) {
							below.setColor(me.getColor());
							me.setColor(0);
						}
					}
				}
				//west works
				for (int i = 0; i < gBoard.length; i++) {
					Gem me = gBoard[gBoard.length -1][i];
					if(me.getColor()==0) {
						me.setColor(getRandomColor());
					}
				}
				break;
			}
		}

		private boolean checkAdjacent(int i, int j) {
			// System.out.println("--- checkAdjacent ---");
			for (int row = -1; row <= 1; ++row) {
				for (int col = -1; col <= 1; ++col) {
					// System.out.println("row: " + row + " col: " + col +
					// " i: "
					// + i + " j: " + j);
					// dont check yourself
					if (row != 0 || col != 0) {
						// array bounds checking
						if (row + i < gBoard.length && row + i >= 0) {
							if (col + j < gBoard[0].length && col + j >= 0) {
								// check if this neighbor is in the list
								// System.out.println("passed array bounds check");
								if (selectedList.contains(gBoard[row + i][col
										+ j])) {
									// System.out.println("added to list");
									return true;
								}
							}
						}
					}
				}
			}

			return false;
		}

		public void onTouchEvent(MotionEvent event) {

			int action = event.getActionMasked();

			int actionIndex = event.getActionIndex(); // pointer (i.e., finger)

			if (action == MotionEvent.ACTION_DOWN
					|| action == MotionEvent.ACTION_MOVE) {
				touchX = event.getX(actionIndex);
				touchY = event.getY(actionIndex);

				for (int i = 0; i < gBoard.length; i++) {
					for (int j = 0; j < gBoard[0].length; j++) {
						Gem gem = gBoard[i][j];
						if (gem.getRect().contains(touchX, touchY)) {

							// if this is a new color
							if (currentColor == -1) {
								currentColor = gem.getColor();
								selectedRect = gem.getRect();
								selectedList.add(gem);

								// if i currently moved to a new rect
								// i can check before see
							} else if (selectedRect != gem.getRect()) {
								// System.out.println("looking at new rect");
								// this is the same color as the currently
								// looked color
								if (currentColor == gem.getColor()) {
									selectedRect = gem.getRect();
									if (!selectedList.contains(gem)
											&& checkAdjacent(i, j)) {
										selectedList.add(gem);
									}

								}
							}

						}

					}
				}
				// System.out.println("X: " + touchX + " Y: " + touchY);
			}
			// user is done trying to chain
			if (action == MotionEvent.ACTION_UP) {
				// reset the chain
				currentColor = -1;
				selectedRect = null;
				if (selectedList.size() > 3) {
					// System.out.println(selectedList);
					for (Gem g : selectedList) {
						// clear out gems
						g.setColor(0);
					}

					// add to score
					score += selectedList.size() * 10;
					moveCounter--;
					fallDirection = fallDirection.getNext();
				}

				selectedList.clear();
				// dont check top row

			}

		}

		private void updateFPS(long now) {
			float fps = 0.0f;
			++frames;
			float overtime = now - nextUpdate;
			if (overtime > 0) {
				fps = frames / (1 + overtime / 1000.0f);
				frames = 0;
				nextUpdate = System.currentTimeMillis() + 1000;
				//System.out.println("FPS: " + fps);
			}
		}

		@Override
		public void run() {

			lastTime = System.currentTimeMillis() + 100;

			while (isRunning) {
				Canvas canvas = null;
				try {
					canvas = surfaceHolder.lockCanvas();
					if (canvas == null) {
						isRunning = false;
						continue;
					}

					synchronized (surfaceHolder) {
						long now = System.currentTimeMillis();
						double elapsed = (now - lastTime) / 1000.0;
						lastTime = now;

						updateFPS(now);
						doUpdate(elapsed);
						doDraw(canvas);
					}
				} finally {
					if (canvas != null) {
						surfaceHolder.unlockCanvasAndPost(canvas);
					}
				}
			}
		}
	}
}