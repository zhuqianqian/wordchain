package com.z299studio.wordchain;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class MenuFragment extends Fragment {
	
	private int mStatus;
	private Button mButtonNew;
	private HomeActivity mActivity;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_main, container, false);
		mActivity = (HomeActivity)getActivity();
		mButtonNew = (Button)v.findViewById(R.id.new_game);
		mActivity.mActionBar.setDisplayHomeAsUpEnabled(false);
		return v;
	}
	
	@Override
	public void onStart() {
		super.onStart();
		mStatus = HomeActivity.SP.getInt(HomeActivity.ITEM_GAME_STATUS, HomeActivity.GAME_OVER);
		if(mStatus == HomeActivity.GAME_ONGOING) {
			mButtonNew.setText(R.string.resume_game);
		}
	}
}
