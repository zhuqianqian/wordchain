package com.z299studio.wordchain;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.MotionEvent;
import android.view.View.OnTouchListener;
import android.widget.TextView;

public class InputView extends View implements OnTouchListener {
	public static final int KEY_DEL = 26;
	public static final int KEY_OK = 27;
	
	public TextView mTextView;
	private char[] mChars;
	private int mCurrentChar = 0;
	private boolean mRespond = true;
	private int mHeight, mWidth;
	private Paint mPaintLine = new Paint();
	private Paint mPaintText = new Paint();
	private int mBtnGap;
	private int mTileX, mTileY, mTileX1, mTileY1;
	protected OnInputListener mListener;
	private int mMinKeyHeight;
	private int mKeyTextSize;
	private float mRadius = 2.0f;
	private int mTextVerticalOffset;
	private int[] mTouchYBound = new int[3];
	private int[] mTouchXBound0 = new int[11];
	private int[] mTouchXBound1 = new int[10];
	private RectF mRectF = new RectF();
	private String[] mKeys= {"Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P", //9
						"A", "S", "D", "F", "G", "H", "J", "K", "L", // 18
						  "Z", "X", "C", "V", "B", "N", "M", "DEL", "OK"}; //25
	private int mKeyPressed = -1;;
	private int mColorPressed;
	public interface OnInputListener {
		public boolean onKeyStroke(int key);
		public boolean onSubmit(String text);
	}
	
	public InputView(Context context) {
		super(context);
		loadParameters(context);
	}
	public InputView(Context context, AttributeSet attrs) {
		super(context, attrs);
		loadParameters(context);
	}
	public InputView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		loadParameters(context);
	}
	
	public void setOnInputListener(OnInputListener l) {
		mListener = l;
	}
	
	public void loadParameters(Context context) {
		Resources r = context.getResources();
		mMinKeyHeight = (int) (r.getDimension(R.dimen.min_key_height) + 0.5f);
		mKeyTextSize = (int) (r.getDimension(R.dimen.key_text_size)+0.5f);
		mColorPressed = r.getColor(R.color.key_pressed);
		mBtnGap = (int)(r.getDimension(R.dimen.key_x_interval) + 0.5f);
		setOnTouchListener(this);
	}
	public void setPaint(int maxChar, int color, int size) {
		Rect rect = new Rect();
		mPaintLine.setColor(color);
		mPaintText.setColor(color);
		mPaintLine.setStyle(Paint.Style.STROKE);
		mPaintText.setTextSize(mKeyTextSize);
		mPaintText.setTextAlign(Paint.Align.CENTER);
		mPaintText.setStyle(Paint.Style.FILL_AND_STROKE);
		mPaintText.setAntiAlias(true);
		mPaintText.getTextBounds(mKeys[0], 0, 1, rect);
		mTextVerticalOffset = (rect.bottom - rect.top) / 2;
		
		mChars = new char[maxChar];
	}
	
	@Override
	public void onSizeChanged(int w, int h, int oldw, int oldh) {
		mWidth = w;
		mHeight = h;
		measureSize();
	}
	
	public void reset( char c) {
		mTextView.setText(String.valueOf(c));
		mChars[0] = c;
		mCurrentChar = 1;
		mRespond = true;
	}
	
	protected void measureSize() {
		if(mWidth >0 ) {
			int i;
			mTileX = mWidth / 10 - 1;
			mTileY = mHeight / 3 - 1;
			if(mTileY > mTileX) {
				mTileY = mTileX;
			}
			else {
				mTileX = mTileY;
			}
			if(mTileY < mMinKeyHeight) {
				mTileY = mMinKeyHeight;
			}
			mTileX1 = mTileX + mBtnGap;
			mTileY1 = mTileY + 10*mBtnGap;
			mTouchYBound[2] = mHeight - mTileY1;
			mTouchYBound[1] = mTouchYBound[2] - mTileY1;
			mTouchYBound[0] = mTouchYBound[1] - mTileY1;
			mTouchXBound0[0] = (mWidth - mTileX1 * 10) / 2;
			for(i = 1; i < 11; ++i) {
				mTouchXBound0[i] = mTouchXBound0[i-1] + mTileX1;
			}
			mTouchXBound1[0] = mTouchXBound0[0] + mTileX/2;
			for(i = 1; i < 10; ++i) {
				mTouchXBound1[i] = mTouchXBound1[i-1] + mTileX1;
			}
		}
	}
	
	public void onDraw(Canvas canvas) {
		int xBegin, yBegin, yEnd;
		int xText, yText;
		int color;
		int i;
		xBegin = mTouchXBound0[0] + mTileX1 + mTileX/2;
		yBegin = mHeight - mTileY1;
		yEnd = yBegin + mTileY;
		if(mKeyPressed==26) {
			mPaintLine.setStyle(Paint.Style.FILL);
			color = mPaintLine.getColor();
			mPaintLine.setColor(mColorPressed);
		    mRectF.set(0,  yBegin, xBegin - mBtnGap, yEnd);
		    canvas.drawRoundRect(mRectF, mRadius, mRadius, mPaintLine);
			mPaintLine.setColor(color);
			mPaintLine.setStyle(Paint.Style.STROKE);
		}
		xText = xBegin + mTileX / 2; yText = yBegin + mTileY / 2 + mTextVerticalOffset;
		canvas.drawText(mKeys[26], mTileX/2, yText, mPaintText);
		for(i = 19; i < 26; ++i) {
			mRectF.set(xBegin, yBegin, xBegin+mTileX- mBtnGap, yEnd);
			if(mKeyPressed == i) {
				mPaintLine.setStyle(Paint.Style.FILL);
				color = mPaintLine.getColor();
				mPaintLine.setColor(mColorPressed);
				canvas.drawRoundRect(mRectF, mRadius, mRadius, mPaintLine);
				canvas.drawText(mKeys[i], xText, yText, mPaintText);
				mPaintLine.setColor(color);
				mPaintLine.setStyle(Paint.Style.STROKE);
			}
			canvas.drawRoundRect(mRectF, mRadius, mRadius, mPaintLine);
			canvas.drawText(mKeys[i], xText, yText, mPaintText);
			xBegin += mTileX1;	xText += mTileX1;
		}
		if(mKeyPressed == 27) {
			mPaintLine.setStyle(Paint.Style.FILL);
			color = mPaintLine.getColor();
			mPaintLine.setColor(mColorPressed);
			mRectF.set(xBegin, yBegin, mWidth, yEnd);
			canvas.drawRoundRect(mRectF, mRadius, mRadius, mPaintLine);
			mPaintLine.setColor(color);
			mPaintLine.setStyle(Paint.Style.STROKE);
		}
		canvas.drawText(mKeys[27], xText + mTileX/2, yText, mPaintText);
		xBegin = mTouchXBound1[0];
		yBegin -= mTileY1;
		yEnd = yBegin + mTileY;
		xText = mTileX; yText -= mTileY1;
		for(i = 10; i < 19; ++i) {
			mRectF.set(xBegin, yBegin, xBegin+mTileX-mBtnGap, yEnd);
			if(mKeyPressed == i) {
				mPaintLine.setStyle(Paint.Style.FILL);
				color = mPaintLine.getColor();
				mPaintLine.setColor(mColorPressed);
				canvas.drawRoundRect(mRectF, mRadius, mRadius, mPaintLine);
				canvas.drawText(mKeys[i], xText, yText, mPaintText);
				mPaintLine.setColor(color);
				mPaintLine.setStyle(Paint.Style.STROKE);
			}
			canvas.drawRoundRect(mRectF, mRadius, mRadius, mPaintLine);
			canvas.drawText(mKeys[i], xText, yText, mPaintText);
			xBegin += mTileX1;	xText += mTileX1;
		}
		xBegin = mTouchXBound0[0];
		yBegin -= mTileY1;
		yEnd = yBegin + mTileY;
		xText = xBegin + mTileX / 2; yText -= mTileY1;
		for(i = 0; i < 10; ++i) {
			mRectF.set(xBegin, yBegin, xBegin+mTileX-mBtnGap, yEnd);
			if(mKeyPressed == i) {
				mPaintLine.setStyle(Paint.Style.FILL);
				color = mPaintLine.getColor();
				mPaintLine.setColor(mColorPressed);
				canvas.drawRoundRect(mRectF, mRadius, mRadius, mPaintLine);
				canvas.drawText(mKeys[i], xText, yText, mPaintText);
				mPaintLine.setColor(color);
				mPaintLine.setStyle(Paint.Style.STROKE);
			}
			canvas.drawRoundRect(mRectF, mRadius, mRadius, mPaintLine);
			canvas.drawText(mKeys[i],  xText, yText, mPaintText);
			xBegin += mTileX1;	xText += mTileX1;
		}
	}

	@Override
	public boolean onTouch(View arg0, MotionEvent arg1) {
	//	Log.w("onTouch", "Triggered");
		if(mRespond) {
		int actionCode = arg1.getAction();
		int x,y, yIndex, xIndex;
		x = (int) arg1.getX();
		y = (int) arg1.getY();
		mKeyPressed = -1;
		yIndex = (y - mTouchYBound[0]) / mTileY1;
		if(actionCode == MotionEvent.ACTION_DOWN || actionCode == MotionEvent.ACTION_MOVE) {
			if(y < mHeight && y > mTouchYBound[0]) {
				
				if(yIndex == 0 && x > mTouchXBound0[0] && x < mTouchXBound0[10]) {
					xIndex = x / mTileX1;
					mKeyPressed = xIndex;
				}
				else if(yIndex==1 && x > mTouchXBound1[0] && x < mTouchXBound1[9]) {
					xIndex = (x - mTouchXBound1[0])/mTileX1;
					mKeyPressed = 10+xIndex;
				}
				else if(yIndex==2) {
					if(x < mTouchXBound1[1]) {
						mKeyPressed = 26;
					}
					else if(x > mTouchXBound1[8]) {
						mKeyPressed  = 27;
					}
					else {
						xIndex = (x - mTouchXBound1[1]) / mTileX1;
						mKeyPressed = 19+xIndex;
					}
				}
			}
		}
		else if(actionCode == MotionEvent.ACTION_CANCEL || actionCode == MotionEvent.ACTION_UP)
		{
			if(y < mHeight && y > mTouchYBound[0]) {
				if(yIndex == 0) {
					xIndex = x / mTileX1;
					mKeyPressed = xIndex;
				}
				else if(yIndex==1 && x > mTouchXBound1[0] && x < mTouchXBound1[9]) {
					xIndex = (x - mTouchXBound1[0])/mTileX1;
					mKeyPressed = 10+xIndex;
				}
				else if(yIndex==2) {
					if(x < mTouchXBound1[1]) {
						mKeyPressed = 26;
					}
					else if(x > mTouchXBound1[8]) {
						mKeyPressed  = 27;
					}
					else {
						xIndex = (x - mTouchXBound1[1]) / mTileX1;
						mKeyPressed = 19+xIndex;
					}
				}
			}
			if(mKeyPressed >= 0) {
				if(mKeyPressed < 26) {
					onKeyStroke(mKeys[mKeyPressed].charAt(0));
				}
				else {
					onKeyStroke(mKeyPressed);
				}
				//invalidate();
			}
			mKeyPressed = -1;
		}
	//	if(mKeyPressed >= 0) {
			invalidate();
	//	}
		}
		return true;
	}
	
	public void restore(String text) {
		mTextView.setText(text);
		for(mCurrentChar = 0; mCurrentChar < text.length(); ++mCurrentChar) {
			mChars[mCurrentChar] = text.charAt(mCurrentChar);
		}
		mRespond = true;
	}
	
	public void gameOver() {
		mRespond = false;
	}
	
	public void onKeyStroke(int key) {
		if(!mListener.onKeyStroke(key)) {
			if(key == KEY_DEL) {
				if(mCurrentChar > 1) {
					mCurrentChar -= 1;
					mTextView.setText(String.copyValueOf(mChars, 0, mCurrentChar));
				}
			//	invalidate();
			}
			else if(key == KEY_OK) {
				if(mListener != null) {
					if(mListener.onSubmit(String.copyValueOf(mChars, 0, mCurrentChar))) {
						mChars[0] = mChars[mCurrentChar-1];
						mCurrentChar = 1;
						mTextView.setText(String.copyValueOf(mChars, 0, mCurrentChar));
					}
				}
			}
			else {
				if(mCurrentChar < mChars.length) {
					mChars[mCurrentChar] = (char)(key+0x20);
					mCurrentChar += 1;
					mTextView.setText(String.copyValueOf(mChars, 0, mCurrentChar));
				}
			}
		}
	}
}
