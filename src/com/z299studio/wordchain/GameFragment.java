package com.z299studio.wordchain;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Hashtable;
import java.util.Random;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.TextSwitcher;
import android.widget.TextView;

public class GameFragment extends Fragment implements InputView.OnInputListener, AnimationListener{
	
	public interface GameListener {
		public void onLeaderboard(View v);
		public void onShare(View v);
		public void onCheck(String text, int score, boolean deleted);
		public void onGameOver(int score);
		public void saveProgress();
		public void loadProgress();
		public void reset();
	}
	protected String mSentences[] = new String[6];
	protected int mInitialStat[] = new int[26];
	protected int mLengthSpan[] = {0, 0, 0};
	protected boolean mIgnoreAni;
	protected boolean mAniStarted;
	private int mBonusSpanInt[] = {0, 3, 8};
	private String mBonusSpan[] = {null, "3 !", "8 !"};
	private GameListener mListener;
	private InputView mKeyboard;
	private TextView  mBestView;
	private TextSwitcher mScoreView, mBonusView;
	private TextView mWordView;
	private int mScore, mBest;
	DatabaseHelper mDataHelper;
	private String mText;
	SharedPreferences.Editor mEdit;
	private static final String DATA_FILE = "data";
	private HomeActivity mActivity;
	private Hashtable<String, Boolean> mHistory = new Hashtable<String, Boolean>();
	private static final Random RNG = new Random();
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	int ids[] = {R.string.not_a_word, R.string.max_used, R.string.min_used,
    			R.string.word_count_12, R.string.word_count_6, R.string.word_count_1};
    	
        View v = inflater.inflate(R.layout.fragment_game, container, false);
        mActivity = (HomeActivity)getActivity();
        Resources r = getResources();
		int color = r.getColor(R.color.white);
		mKeyboard = (InputView)v.findViewById(R.id.keyboard);
		mKeyboard.mTextView = (TextView)v.findViewById(R.id.input);
		mKeyboard.setOnInputListener(this);
		mKeyboard.setPaint(20, color, (int)(r.getDimension(R.dimen.word_input)));
		mScoreView = (TextSwitcher)v.findViewById(R.id.score);
		mBestView = (TextView)v.findViewById(R.id.best);
		mWordView = (TextView)v.findViewById(R.id.word);
		mBonusView = (TextSwitcher)v.findViewById(R.id.bonus);
		mDataHelper = mActivity.getDatabaseHelper();
		for(int i = 0; i < ids.length; ++i) {
			mSentences[i] = r.getString(ids[i]);
		}
//		mActivity.mActionBar.setDisplayHomeAsUpEnabled(true);
		mActivity.mActionBar.setHomeButtonEnabled(true);
		mActivity.mActionBar.setIcon(R.drawable.home_back);
        return v;
    }
    
    @Override
    public void onStart() {
    	super.onStart();
    	mBest = HomeActivity.SP.getInt(HomeActivity.ITEM_GAME_BEST, 0);
		mBestView.setText(String.valueOf(mBest));
		if(mActivity.mStatus == HomeActivity.GAME_ONGOING) {
			mText = HomeActivity.SP.getString(HomeActivity.ITEM_LAST_WORD, "hello");
			mScore = HomeActivity.SP.getInt(HomeActivity.ITEM_GAME_SCORE, 0);			
			mScoreView.setText(String.valueOf(mScore));
			String s = HomeActivity.SP.getString(HomeActivity.ITEM_USER_INPUT, "hello");
			mKeyboard.restore(s);
			restoreHistory();
			mListener.loadProgress();
		}
		else {
			resetGame();
			mScoreView.setText(String.valueOf(0));
			mText = mDataHelper.getText(RNG.nextInt(56088));
			mHistory.put(mText, Boolean.valueOf(true));
			mKeyboard.reset(mText.charAt(mText.length()-1));
			mActivity.mStatus = HomeActivity.GAME_ONGOING;
		}
		mScoreView.setInAnimation(AnimationUtils.loadAnimation((Context)mActivity, R.anim.flip_in));
		mScoreView.setOutAnimation(AnimationUtils.loadAnimation((Context)mActivity, R.anim.flip_out));
		
		mBonusView.setInAnimation(AnimationUtils.loadAnimation((Context)mActivity, android.R.anim.fade_in));
		Animation ani = AnimationUtils.loadAnimation((Context)mActivity, android.R.anim.fade_out);
		ani.setAnimationListener(this);
		mBonusView.setOutAnimation(ani);
		mWordView.setText(mText);
    }
	
    public void setListner(GameListener l) {
    	mListener = l;
    }
	protected void restoreHistory() {
		try {
			File file = new File(mActivity.getFilesDir()+"/"+DATA_FILE);
			long size = file.length();
			byte[] buffer = new byte[(int) size];
			FileInputStream fis = mActivity.openFileInput(DATA_FILE);
			fis.read(buffer);
			fis.close();
			String s = new String(buffer, "UTF-8");
			String keys[] = s.split("\n");
			for(String k : keys) {
				mHistory.put(k, Boolean.valueOf(true));
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		mLengthSpan[0] = HomeActivity.SP.getInt("STAT0", 0);
		mLengthSpan[1] = HomeActivity.SP.getInt("STAT1", 0);
		mLengthSpan[2] = HomeActivity.SP.getInt("STAT2", 0);
		String s = HomeActivity.SP.getString("STATInit", "");
		String ss[] = s.split(",");
		if(ss.length > 0) {
			for(int i = 0; i < 26; i++) {
				mInitialStat[i] = Integer.valueOf(ss[i]);
			}
		}
	}
	
	public void resetGame() {
		mHistory.clear();
		mListener.reset();
		mScore = 0;
		mScoreView.setText(String.valueOf(0));
		mText = mDataHelper.getText(RNG.nextInt(56088));
		mKeyboard.reset(mText.charAt(mText.length()-1));
		mHistory.put(mText, Boolean.valueOf(true));
		mWordView.setText(mText);
		mActivity.mStatus = HomeActivity.GAME_ONGOING;
		mLengthSpan[0] = mLengthSpan[1] = mLengthSpan[2] = 0;
		for(int i = 0; i < 26; i++) {
			mInitialStat[i] = 0;
		}
	}
	
	@Override
	public boolean onSubmit(final String text) {

		if(mDataHelper.checkText(text) == false) {
			onGameOver();
			return false;
		}
		else if(mHistory.containsKey(text)) {			
			new AlertDialog.Builder(mActivity)
		    .setTitle(R.string.repeat_title)
		    .setMessage(R.string.repeat_input)
		    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, int which) { 
		            GameFragment.this.clearInput(text);
		        }
		     })
		     .show();
			return false;
		}
		int bonus,
			len = text.length();
		bonus = len / 6;
		if(bonus >= mBonusSpan.length) {
			bonus = mBonusSpan.length - 1;
		}
		mLengthSpan[bonus] += 1;
		mInitialStat[text.charAt(0)-'a'] += 1;
		mHistory.put(text, Boolean.valueOf(true));
		mText = text;
		mScore += len;
		if(bonus > 0) {
			mBonusView.setText("+"+mBonusSpan[bonus]);
			mScore += mBonusSpanInt[bonus];
		}
		mListener.onCheck(text, (len+mBonusSpanInt[bonus]), false);
		mScoreView.setText(String.valueOf(mScore));
		mWordView.setText(text);
		return true;
	}
	public void clearInput(String text) {
		mKeyboard.restore(String.valueOf(text.charAt(0)));
	}
	public void onGameOver() {
		int min, max, minIndex, maxIndex;
		String text;
		max = mInitialStat[0];
		min = 1000000;
		minIndex = maxIndex = 0;
		for(int i = 1; i < 26; ++i) {
			if(max < mInitialStat[i] ){
				max = mInitialStat[i];
				maxIndex = i;
			}
			else if(min > mInitialStat[i] && mInitialStat[i]>0) {
				min = mInitialStat[i];
				minIndex = i; 
			}
		}
		if(min == 1000000) {
			min = 0;
		}
		text = mSentences[0];
		if(max> 0) {
			 text += mSentences[1] + String.valueOf((char)(maxIndex+'A')) + " ("+String.valueOf(max) +")";
		}
		if(min>0) {
			  text += mSentences[2] + String.valueOf((char)(minIndex + 'A')) + " ("+String.valueOf(min) +")";
		}
		text =	text  
			  +mSentences[3] + String.valueOf(mLengthSpan[2])
			  +mSentences[4] + String.valueOf(mLengthSpan[1])
			  +mSentences[5] + String.valueOf(mLengthSpan[0]);
		new AlertDialog.Builder(mActivity)
	    .setTitle(R.string.game_over)
	    .setMessage(text)
	    .setPositiveButton(R.string.new_game, new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int which) { 
	            GameFragment.this.resetGame();
	        }
	     })
	    .setNegativeButton(R.string.leaderboard, new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int which) { 
	            mListener.onLeaderboard(null);
	        }
	     })
	     .setNeutralButton(R.string.share, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				mListener.onShare(null);
			}
		})
	     .show();
		mHistory.clear();
		mActivity.mStatus = HomeActivity.GAME_OVER;
		if(mScore > mBest) {
			mEdit = HomeActivity.SP.edit();
			mEdit.putInt(HomeActivity.ITEM_GAME_BEST, mScore);
			mEdit.commit();
			mBest = mScore;
		}
		mKeyboard.gameOver();
		mListener.onGameOver(mScore);
		mScore = 0;
	}
	
	@Override
	public void onPause() {
		super.onPause();
		mEdit = HomeActivity.SP.edit();
		mEdit.putInt(HomeActivity.ITEM_GAME_SCORE, mScore);
		mEdit.putInt(HomeActivity.ITEM_GAME_STATUS, mActivity.mStatus);
		mEdit.putString(HomeActivity.ITEM_LAST_WORD, mText);
		mEdit.putString(HomeActivity.ITEM_USER_INPUT, mKeyboard.mTextView.getText().toString());
		mEdit.putInt("STAT0", mLengthSpan[0]);
		mEdit.putInt("STAT1", mLengthSpan[1]);
		mEdit.putInt("STAT2", mLengthSpan[2]);
		String value = "";
		for(int i : mInitialStat) {
			value += String.valueOf(i) + ",";
		}
		mEdit.putString("STATInit", value);
		mEdit.commit();
		
		try {
			FileOutputStream fos = mActivity.openFileOutput(DATA_FILE, Context.MODE_PRIVATE);
			fos.write("".getBytes());
			for(String s : mHistory.keySet()) {
				fos.write((s + "\n").getBytes());
			}
			fos.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		mListener.saveProgress();
	}

	@Override
	public boolean onKeyStroke(int key) {
		if(key == InputView.KEY_DEL) {
			mListener.onCheck(null, 0, true);
		}
		return false;
	}

	@Override
	public void onAnimationEnd(Animation animation) {
	//	Log.w("onAnimationEnd", "mIgnoreAni:"+String.valueOf(mIgnoreAni) + ", and mAniStarted:"+String.valueOf(mAniStarted));
		if(mAniStarted) {
			mIgnoreAni = true;
			mAniStarted=false;
			mBonusView.setText(null);
		}		
	}

	@Override
	public void onAnimationRepeat(Animation animation) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAnimationStart(Animation animation) {
//		Log.w("onAnimationStart", "mIgnoreAni:"+String.valueOf(mIgnoreAni) + ", and mAniStarted:"+String.valueOf(mAniStarted));
		if(!mIgnoreAni) {
			mAniStarted = true;
			mIgnoreAni = false;
		}
		else {
			mIgnoreAni = false;
		}
	}
}
