package com.z299studio.wordchain;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Random;

import com.google.android.gms.games.Games;
import com.google.example.games.basegameutils.BaseGameActivity;

import android.net.Uri;
import android.os.Bundle;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.TextSwitcher;
import android.widget.TextView;

public class HomeActivity extends BaseGameActivity implements AnimationListener, TextWatcher{
	
	private static final String DATA_FILE = "data";
	public static final String SP_FILE = "game";
	public static final String ITEM_GAME_STATUS = "Status";
	public static final String ITEM_GAME_SCORE = "Score";
	public static final String ITEM_GAME_BEST = "Best";
	public static final String ITEM_LAST_WORD = "LastWord";
	public static final String ITEM_USER_INPUT = "CurrentInput";
	public static final String ITEM_GOOGLE_GAMES = "GoogleGameService";
	
	public static final int GAME_ONGOING = 1;
	public static final int GAME_OVER = 0;
	private GameServiceManager mGSM;
	public static SharedPreferences SP;
	public int mStatus;
	
	protected DatabaseHelper mDBH;
	protected boolean mUseGoogleService;
	protected String mSentences[] = new String[6];
	protected int mInitialStat[] = new int[26];
	protected int mLengthSpan[] = {0, 0, 0};
	protected boolean mIgnoreAni;
	protected boolean mAniStarted;
	private int mBonusSpanInt[] = {0, 3, 8};
	private String mBonusSpan[] = {null, "3 !", "8 !"};
	private EditText mKeyboard;
	private TextView  mBestView;
	private TextSwitcher mScoreView, mBonusView;
	private TextView mWordView;
	private int mScore, mBest;
	private String mText;
	private String mFirstLetter;
	private Hashtable<String, Boolean> mHistory = new Hashtable<String, Boolean>();
	private static final Random RNG = new Random();
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		SP = this.getSharedPreferences(SP_FILE, 0);
		mUseGoogleService = SP.getBoolean(ITEM_GOOGLE_GAMES, false);
		mGSM = new GameServiceManager(this, mUseGoogleService);
		mStatus = SP.getInt(ITEM_GAME_STATUS, GAME_OVER);
		mHelper.setConnectOnStart(mUseGoogleService);
		mDBH = new DatabaseHelper(this);
		
		try {
			mDBH.createDatabase();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		initUiControls();
	}
	
	protected void initUiControls() {
		mKeyboard = (EditText)findViewById(R.id.input);
		mKeyboard.addTextChangedListener(this);
		mWordView = (TextView)findViewById(R.id.word);
		mScoreView = (TextSwitcher)findViewById(R.id.score);
		mBonusView = (TextSwitcher)findViewById(R.id.bonus);
		mBestView = (TextView)findViewById(R.id.best);
		mBest = SP.getInt(ITEM_GAME_BEST, 0);
		mBestView.setText(String.valueOf(mBest));
		
		if(mStatus == GAME_ONGOING) {
			mText = HomeActivity.SP.getString(HomeActivity.ITEM_LAST_WORD, "hello");
			mScore = HomeActivity.SP.getInt(HomeActivity.ITEM_GAME_SCORE, 0);			
			mScoreView.setText(String.valueOf(mScore));
			String s = HomeActivity.SP.getString(HomeActivity.ITEM_USER_INPUT, "");
			mKeyboard.setText(s);
			mKeyboard.setSelection(s.length());
			restoreHistory();
			mGSM.loadProgress();
		}
		else {
			resetGame();
		}
		
		mScoreView.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.flip_in));
		mScoreView.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.flip_out));
		
		mBonusView.setInAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
		Animation ani = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
		ani.setAnimationListener(this);
		mBonusView.setOutAnimation(ani);
		mWordView.setText(mText);
	}
	
	protected void restoreHistory() {
		try {
			File file = new File(getFilesDir()+"/"+DATA_FILE);
			long size = file.length();
			byte[] buffer = new byte[(int) size];
			FileInputStream fis = openFileInput(DATA_FILE);
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
		mLengthSpan[0] = SP.getInt("STAT0", 0);
		mLengthSpan[1] = SP.getInt("STAT1", 0);
		mLengthSpan[2] = SP.getInt("STAT2", 0);
		String s = SP.getString("STATInit", "");
		String ss[] = s.split(",");
		if(ss.length > 0) {
			for(int i = 0; i < 26; i++) {
				mInitialStat[i] = Integer.valueOf(ss[i]);
			}
		}
	}
	
	public void onNewGame(View v) {
		if(mStatus == GAME_ONGOING) {
			new AlertDialog.Builder(HomeActivity.this)
				.setTitle(R.string.new_game)
				.setMessage(R.string.reset_ask2)
				.setPositiveButton(R.string.new_game, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) { 
						resetGame();
					}
				})
				.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) { 
					}
				})
				.show();
		}
		else {
			resetGame();
		}
	}
	
	protected void resetGame() {
		mHistory.clear();
		mScore = 0;
		mScoreView.setText(String.valueOf(0));
		mText = mDBH.getText(RNG.nextInt(56088));
		mKeyboard.setText(String.valueOf(mText.charAt(mText.length()-1)));
		mKeyboard.setSelection(1);
		mWordView.setText("");
		mStatus = GAME_ONGOING;
		mLengthSpan[0] = mLengthSpan[1] = mLengthSpan[2] = 0;
		for(int i = 0; i < 26; i++) {
			mInitialStat[i] = 0;
		}
	}
	
	@Override
	public void onPause() {
		super.onPause();
		SharedPreferences.Editor editor = SP.edit();
		editor.putInt(HomeActivity.ITEM_GAME_STATUS, mStatus);
		if(mStatus == GAME_OVER) {
			return;
		}
		editor.putInt(HomeActivity.ITEM_GAME_SCORE, mScore);
		editor.putString(HomeActivity.ITEM_LAST_WORD, mText);
		editor.putString(HomeActivity.ITEM_USER_INPUT, mKeyboard.getText().toString());
		editor.putInt("STAT0", mLengthSpan[0]);
		editor.putInt("STAT1", mLengthSpan[1]);
		editor.putInt("STAT2", mLengthSpan[2]);
		String value = "";
		for(int i : mInitialStat) {
			value += String.valueOf(i) + ",";
		}
		editor.putString("STATInit", value);
		editor.commit();
		
		try {
			FileOutputStream fos = openFileOutput(DATA_FILE, Context.MODE_PRIVATE);
			fos.write("".getBytes());
			for(String s : mHistory.keySet()) {
				fos.write((s + "\n").getBytes());
			}
			fos.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		mGSM.saveProgress();
		if(mUseGoogleService) {
			mGSM.saveLocal();
		}
	}
	
	public boolean onSubmit(final String text) {
		if(mDBH.checkText(text) == false) {
			onGameOver();
			return false;
		}
		else if(mHistory.containsKey(text)) {			
			new AlertDialog.Builder(this)
		    .setTitle(R.string.repeat_title)
		    .setMessage(R.string.repeat_input)
		    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, int which) { 
		            mKeyboard.setText(text.substring(0, 1));
		            mKeyboard.setSelection(1);
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
		mGSM.onCheck(this, text, (len+mBonusSpanInt[bonus]), false);
		mScoreView.setText(String.valueOf(mScore));
		mWordView.setText(text);
		mKeyboard.setText(String.valueOf(text.charAt(text.length()-1)));
		mKeyboard.setSelection(1);
		return true;
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
		new AlertDialog.Builder(this)
	    .setTitle(R.string.game_over)
	    .setMessage(text)
	    .setPositiveButton(R.string.new_game, new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int which) { 
	            resetGame();
	        }
	     })
	    .setNegativeButton(R.string.leaderboard, new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int which) { 
	            onLeaderboard(null);
	        }
	     })
	     .setNeutralButton(R.string.share, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				onShare(null);
			}
		})
	    .show();
		mHistory.clear();
		mStatus = GAME_OVER;
		if(mScore > mBest) {
			SharedPreferences.Editor editor = HomeActivity.SP.edit();
			editor.putInt(HomeActivity.ITEM_GAME_BEST, mScore);
			editor.commit();
			mBest = mScore;
		}
		mGSM.gameOverCheck(this, mScore);
		mGSM.reset();
		mScore = 0;
	}
	
//	protected void startGame() {
//		if(mStatus == GAME_ONGOING) {
//			new AlertDialog.Builder(HomeActivity.this)
//		    .setTitle(R.string.new_game)
//		    .setMessage(R.string.reset_ask1)
//		    .setPositiveButton(R.string.new_game, new DialogInterface.OnClickListener() {
//		        public void onClick(DialogInterface dialog, int which) { 
//		        	mStatus = GAME_OVER;
//		        	switchFragment();
//		        }
//		     })
//
//		    .setNegativeButton(R.string.resume_game, new DialogInterface.OnClickListener() {
//		        public void onClick(DialogInterface dialog, int which) { 
//		        	mStatus = GAME_ONGOING;
//		        	switchFragment();
//		        }
//		     })
//		     .show();
//		}
//		
//	}
	
	public void onButtonClicked(View view) {
		switch(view.getId()) {
		case R.id.fab:
			onSubmit(mKeyboard.getText().toString());
			break;
		}
		
	}
	
	public void onAchievement(View v) {
		if(mUseGoogleService) {
			if(isSignedIn()) {
				startActivityForResult(Games.Achievements.getAchievementsIntent(getApiClient()), 299);
			}
		}
		else {
			beginUserInitiatedSignIn();
		}
	}
	
	@Override
	public void onStart() {
		super.onStart();
		mGSM.loadProgress();
	}
	
	public void onLeaderboard(View v) {
		if(mUseGoogleService) {
			if(isSignedIn()) {
				startActivityForResult(Games.Leaderboards.getLeaderboardIntent(getApiClient(), mGSM.LB_ID), 299);		
			}
		}
		else {
			beginUserInitiatedSignIn();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.home, menu);
		return true;
	}

	@Override
	public void onSignInFailed() {		
	}

	@Override
	public void onSignInSucceeded() {
		if(!mUseGoogleService) {
			SharedPreferences.Editor edit = SP.edit();
			mUseGoogleService = true;
			edit.putBoolean(ITEM_GOOGLE_GAMES, mUseGoogleService);
			edit.commit();
			mGSM = new GameServiceManager(this);
		}
		else {
			batchUpdate();			
		}	
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle presses on the action bar items
	    switch (item.getItemId()) {
	    case R.id.action_achieve:
	    	onAchievement(null);
	    	return true;
	    	
	    case R.id.action_lb:
	    	onLeaderboard(null);
	    	return true;
	    	
	    case R.id.action_new:
	    	onNewGame(null);
	    	return true;
	    case R.id.action_rate:
	    	onRate(null);
	    	return true;
	    	
	    case R.id.action_share:
	    	onShare(null);
	    	break;
	    	
	    case android.R.id.home: {
	    	onBackPressed();
	    	}
	    	break;
	    
	    default:
	            return super.onOptionsItemSelected(item);
	    }
	    return true;
	}
	
	public void onRate(View v) {
		Uri uri = Uri.parse("market://details?id=" + getPackageName());
		Intent rateIntent = new Intent(Intent.ACTION_VIEW, uri);
		try {
		  startActivity(rateIntent);
		} catch (ActivityNotFoundException e) {
		  startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + this.getPackageName())));
		}
	}
	
	public void onShare(View v) {
		Intent sendIntent = new Intent();
		sendIntent.setAction(Intent.ACTION_SEND);
		sendIntent.putExtra(Intent.EXTRA_TEXT, getResources().getText(R.string.share_content));
		sendIntent.setType("text/plain");
		startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.send_to)));
	}
	
	public class GameServiceManager {
		public boolean mEnabled;
		public SharedPreferences mSP;
		public int[] mIntIds = {
				R.string.achievement_alphabet_explorer, // 0
				R.string.achievement_always_right,  // 1
				R.string.achievement_babbler,  // 2
				R.string.achievement_college_student, // 3
				R.string.achievement_high_school_student,  // 4
				R.string.achievement_i_like_it,  // 5
				R.string.achievement_i_love_it,  // 6
				R.string.achievement_long_word_master,  // 7
				R.string.achievement_love_to_play,  // 8
				R.string.achievement_newbie,  // 9
				R.string.achievement_obsess_over_it,  // 10
				R.string.achievement_first_perfect_chain,  // 11
				R.string.achievement_pupil,  // 12
				R.string.achievement_secondary_school_student,  // 13
				R.string.achievement_word_master,  // 14
				R.string.achievement_second_perfect_chain  // 15
		};
		public static final int NUMBER_NEWBIE = 5;
		public static final int NUMBER_LOVER = 20;
		public String[] mAchievements;
		public int[] mUnsync;
		public String LB_ID;
		
		public int mHighScore;		
		private char mStartChar;
		private int mStartCharRepeat;		
		private char mPerfectInitial;		
		private int mWordlengthRepeat;		
		private boolean mDeleteUsed;		
		private boolean mInitials[] = new boolean[26];	
		private int mInitialCount = 0;
		private int mWordCounter;
		private int mTotalScore;
		
		public GameServiceManager(Context context) {
			Resources r = context.getResources();
			mEnabled = true;
			mSP = context.getSharedPreferences("GameData", 0);
			mHighScore = mSP.getInt("HighScore", 0);
			mUnsync = new int[mIntIds.length];
			mAchievements = new String[mIntIds.length];
			for(int i = 0; i< mIntIds.length; ++i) {
				mAchievements[i] = r.getString(mIntIds[i]);
			}
			LB_ID = r.getString(R.string.leaderboard_high_scores);
			loadLocal();
		}
		public GameServiceManager(Context context, boolean useGameService) {
			mEnabled = useGameService;
			if(mEnabled) {
				Resources r = context.getResources();
				mSP = context.getSharedPreferences("GameData", 0);
				mHighScore = mSP.getInt("HighScore", 0);
				mUnsync = new int[mIntIds.length];
				mAchievements = new String[mIntIds.length];
				for(int i= 0; i < mIntIds.length; ++i) {
					mAchievements[i] = r.getString(mIntIds[i]);
				}
				LB_ID = r.getString(R.string.leaderboard_high_scores);
				loadLocal();
			}
		}
		
		public void loadLocal() {
			for(int i = 0; i < mUnsync.length; ++i) {
				mUnsync[i] = mSP.getInt("ACHIEVEMENT_"+String.valueOf(i), 0);
			}
			mHighScore = mSP.getInt("HighScore", 0);
		}
		
		public void saveLocal() {
			SharedPreferences.Editor editor = mSP.edit();
			for(int i = 0; i < mUnsync.length; ++i) {
				if(mUnsync[i] != 0) {
					editor.putInt("ACHIEVEMENT_"+String.valueOf(i), mUnsync[i]);
				}
			}
			editor.putInt("HighScore", mHighScore);
			editor.commit();
		}
		
		public void saveProgress() {
			if(mEnabled) {
				SharedPreferences.Editor editor = mSP.edit();
				editor.putString("StartChar", String.valueOf(mStartChar));
				editor.putInt("StartCharRepeat", mStartCharRepeat);
				editor.putString("StartCharAZ", String.valueOf(mPerfectInitial));
				editor.putInt("LongWordRepeat", mWordlengthRepeat);
				editor.putInt("WordCounter", mWordCounter);
				editor.putInt("TotalScore", mTotalScore);
				String value = "";
				for(int i = 0; i < 26; ++i) {
					if(mInitials[i]) {
						value += String.valueOf((char)(i+'a'));
					}
				}
				editor.putString("A2ZKeys", value);
				editor.commit();
			}
		}
		
		public void loadProgress() {
			if(mEnabled) {
				String value;
				mStartChar = mSP.getString("StartChar", "0").charAt(0);
				mStartCharRepeat = mSP.getInt("StartCharRepeat", 0);
				mPerfectInitial = mSP.getString("StartCharAZ",  "a").charAt(0);
				mWordlengthRepeat = mSP.getInt("LongWordRepeat", 0);
				mWordCounter = mSP.getInt("WordCounter", 0);
				mTotalScore = mSP.getInt("TotalScore", 0);
				value = mSP.getString("A2ZKeys", "");
				int i, n = value.length();
				for(i = 0; i < n; ++i) {
					mInitials[value.charAt(i) - 'a'] = true;
				}
			}
		}
		
		public void reset() {
			if(mEnabled) {
				mHighScore = mTotalScore = 0;
				mStartChar = '0';
				mStartCharRepeat = 0;
				mPerfectInitial = 'a';
				mWordlengthRepeat = 0;
				mDeleteUsed = false;
				mWordCounter = 0;
				mWordCounter = 0;
				for(int i = 0; i< 26; ++i) {
					mInitials[i] = false;
				}
			}
		}
		
		public void onCheck(HomeActivity activity, String text, int score, boolean deleted) {
			if(!mEnabled) {
				return;
			}
			if(text != null) {
				mWordCounter++;
				mTotalScore += score;
				char c = text.charAt(0);
				if(mUnsync[11] == 0) {
					if(c == 'a' || (c - mPerfectInitial) == 1) {
						mPerfectInitial = c;
					}
					if(mUnsync[11] == 0 && mPerfectInitial == 'g') {
						activity.unlockAchievement(mAchievements[11], 11);
					}
				}
				if(mUnsync[15] == 0) {
					if(c == 'j' || (c - mPerfectInitial) == 1) {
						mPerfectInitial = c;
					}
					if(mUnsync[15] == 0 && mPerfectInitial == 'p') {
						activity.unlockAchievement(mAchievements[15], 15);
					}
				}
				
				if(mUnsync[10] == 0) {
					if(mStartChar==c) {
						mStartCharRepeat++;
					}
					else {
						mStartCharRepeat = 1;
						mStartChar=c;
					}
					if(mStartCharRepeat>7) {
						activity.unlockAchievement(mAchievements[5], 5);
					}
					if(mStartCharRepeat>13) {
						activity.unlockAchievement(mAchievements[6], 6);
					}
					if(mStartCharRepeat>19) {
						activity.unlockAchievement(mAchievements[10], 10);
					}
				}
				if(mUnsync[7] == 0) {
					if(text.length() > 9) {
						mWordlengthRepeat++;
					}
					else {
						mWordlengthRepeat = 0;
					}
					if(mWordlengthRepeat > 9) {
						activity.unlockAchievement(mAchievements[7], 7);
					}
				}
				if(mWordCounter >= 20 && mUnsync[2] == 0) {
					unlockAchievement(mAchievements[2], 2);
				}
				if(mWordCounter >= 50 && mUnsync[12] == 0) {
					unlockAchievement(mAchievements[12], 12);
				}
				if(mWordCounter >= 100 && mUnsync[13] == 0) {
					unlockAchievement(mAchievements[13], 13);
				}
				if(mWordCounter >= 180 && mUnsync[4] == 0) {
					unlockAchievement(mAchievements[4], 4);
				}
				if(mWordCounter >= 300 && mUnsync[3] == 0) {
					unlockAchievement(mAchievements[3], 3);
				}
				if(mUnsync[0] == 0) {
					if(!mInitials[(int)(c - 'a')]) {
						mInitials[(int)(c - 'a')] = true;
						mInitialCount++;
					}
					if(mInitialCount == 26) {
						unlockAchievement(mAchievements[0], 0);
					}
				}
			}
			if(!mDeleteUsed && deleted) {
				mDeleteUsed = true;
			}
		}
		public void gameOverCheck(HomeActivity activity, int score) {
			if(!mEnabled) {
				return;
			}
			if(score > mHighScore) {
				mHighScore = score;
				activity.submitScore(score);
			}
			if(!mDeleteUsed && mUnsync[1] == 0) {
				Games.Achievements.unlock(activity.getApiClient(), mAchievements[1]);
			}	
			incrementAchievement(mAchievements[8], 1, 8);
			incrementAchievement(mAchievements[9], 1, 9);
			if(mTotalScore>0) {
				incrementAchievement(mAchievements[14], mTotalScore, 14);
			}
			mTotalScore = 0;
			
		}
	}
	
	public void incrementAchievement(String id, int delta, int iid) {
		if(mGSM.mUnsync[iid] >=0) {
			if(isSignedIn()) {
				Games.Achievements.increment(getApiClient(), id, delta);
			}
			else {
				mGSM.mUnsync[iid] += delta;
			}
		}
	}
	public void unlockAchievement(String id, int iid) {
		if(mGSM.mUnsync[iid] >= 0) {
			if(isSignedIn()) {
				Games.Achievements.unlock(getApiClient(), id);
				mGSM.mUnsync[iid] = -1;
			}
			else {
				mGSM.mUnsync[iid] = 1;
			}
		}
	}
	
	public void submitScore(int score) {
		if(isSignedIn()) {
			Games.Leaderboards.submitScore(getApiClient(), mGSM.LB_ID, score);
			mGSM.mHighScore = 0;
		}
		else {
			beginUserInitiatedSignIn();
		}
	}
	
	public void batchUpdate( ){
		if(mGSM.mHighScore > 0) {
			Games.Leaderboards.submitScore(getApiClient(), mGSM.LB_ID, mGSM.mHighScore);
			mGSM.mHighScore = 0;
		}
		for(int i= 0; i < mGSM.mUnsync.length; ++i) {
			if(mGSM.mUnsync[i] > 0) {
				if(i==8 || i == 9 || i == 14) {
					Games.Achievements.increment(getApiClient(), mGSM.mAchievements[i], mGSM.mUnsync[i] );
				}
				else {
					Games.Achievements.unlock(getApiClient(), mGSM.mAchievements[i]);
				}
			}
		}
		
	}
	
//	@Override
//	public void onCheck(String text, int score, boolean deleted) {
//		mGSM.onCheck(this, text, score, deleted);
//		
//	}
//
//	@Override
//	public void onGameOver(int score) {
//		mGSM.gameOverCheck(this, score);
//		mGSM.reset();
//	}
//
//	@Override
//	public void saveProgress() {
//		mGSM.saveProgress();		
//	}
//
//	@Override
//	public void loadProgress() {
//		mGSM.loadProgress();
//		
//	}
//
//	@Override
//	public void reset() {
//		mGSM.reset();
//	}

	@Override
	public void onAnimationEnd(Animation animation) {
		if(mAniStarted) {
			mIgnoreAni = true;
			mAniStarted=false;
			mBonusView.setText(null);
		}
	}

	@Override
	public void onAnimationRepeat(Animation animation) {}

	@Override
	public void onAnimationStart(Animation animation) {
		if(!mIgnoreAni) {
			mAniStarted = true;
			mIgnoreAni = false;
		}
		else {
			mIgnoreAni = false;
		}
		
	}

	@Override
	public void afterTextChanged(Editable s) {
		if(s.toString().length() == 1) {
			mFirstLetter = s.toString();
		}
		else if(s.toString().length() == 0) {
			mKeyboard.setText(mFirstLetter);
			mKeyboard.setSelection(1);
		}
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {	}
}
