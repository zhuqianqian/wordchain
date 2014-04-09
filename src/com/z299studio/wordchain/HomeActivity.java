package com.z299studio.wordchain;

import java.io.IOException;

import com.google.android.gms.games.Games;
import com.google.example.games.basegameutils.BaseGameActivity;

import android.net.Uri;
import android.os.Bundle;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class HomeActivity extends BaseGameActivity implements GameFragment.GameListener {
	
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
	
	private GameFragment mGameFragment;
	private MenuFragment mMenuFragment;
	
	public ActionBar mActionBar;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mActionBar = getActionBar();
		mGameFragment = new GameFragment();
		mMenuFragment = new MenuFragment();
		mGameFragment.setListner(this);
        getSupportFragmentManager().beginTransaction().add(R.id.fragment_container,
                mMenuFragment).commit();
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
	}
	
	public DatabaseHelper getDatabaseHelper() {
		return mDBH;
	}
	
	public void onNewGame(View v) {
		if(v!=null) {
			switchFragment();
		}
		else {
			GameFragment gf;
			try {
				gf = (GameFragment)getSupportFragmentManager().findFragmentByTag("TAG_GAME");
				if(gf!=null) {
					if(mStatus == GAME_ONGOING) {
						new AlertDialog.Builder(HomeActivity.this)
					    .setTitle(R.string.new_game)
					    .setMessage(R.string.reset_ask2)
					    .setPositiveButton(R.string.new_game, new DialogInterface.OnClickListener() {
					        public void onClick(DialogInterface dialog, int which) { 
					        	mGameFragment.resetGame();
					        }
					     })
					    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
					        public void onClick(DialogInterface dialog, int which) { 
					            // do nothing
					        }
					     })
					     .show();
					}
					else {
						gf.resetGame();
					}
				}
				else {
					startGame();
				}
			} catch(ClassCastException e) {
				startGame();
			}
		}
	}
	
	protected void startGame() {
		if(mStatus == GAME_ONGOING) {
			new AlertDialog.Builder(HomeActivity.this)
		    .setTitle(R.string.new_game)
		    .setMessage(R.string.reset_ask1)
		    .setPositiveButton(R.string.new_game, new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, int which) { 
		        	mStatus = GAME_OVER;
		        }
		     })

		    .setNegativeButton(R.string.resume_game, new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, int which) { 
		        	mStatus = GAME_ONGOING;
		        }
		     })
		     .show();
		}
		switchFragment();
	}
	
	private void switchFragment() {
		FragmentTransaction ft = 
		getSupportFragmentManager().beginTransaction();
		ft.setCustomAnimations(R.anim.enter, R.anim.exit, R.anim.pop_enter, R.anim.pop_exit);
		ft.replace(R.id.fragment_container, mGameFragment, "TAG_GAME");
		ft.addToBackStack(null);
        ft.commit();
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
	    	getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, mMenuFragment).commit();
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
	
	@Override
	public void onPause() {
		super.onPause();
		if(mUseGoogleService) {
			mGSM.saveLocal();
		}
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
				editor.commit();
			}
		}
		
		public void loadProgress() {
			if(mEnabled) {
				mStartChar = mSP.getString("StartChar", "0").charAt(0);
				mStartCharRepeat = mSP.getInt("StartCharRepeat", 0);
				mPerfectInitial = mSP.getString("StartCharAZ",  "a").charAt(0);
				mWordlengthRepeat = mSP.getInt("LongWordRepeat", 0);
				mWordCounter = mSP.getInt("WordCounter", 0);
				mTotalScore = mSP.getInt("TotalScore", 0);
			}
		}
		
		public void reset() {
			if(mEnabled) {
				mHighScore = 0;
				mStartChar = '0';
				mStartCharRepeat = 0;
				mPerfectInitial = 'a';
				mWordlengthRepeat = 0;
				mDeleteUsed = false;
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
	
	@Override
	public void onCheck(String text, int score, boolean deleted) {
		mGSM.onCheck(this, text, score, deleted);
		
	}

	@Override
	public void onGameOver(int score) {
		mGSM.gameOverCheck(this, score);
		
	}

	@Override
	public void saveProgress() {
		mGSM.saveProgress();		
	}

	@Override
	public void loadProgress() {
		mGSM.loadProgress();
		
	}

	@Override
	public void reset() {
		mGSM.reset();
	}
}
