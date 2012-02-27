package org.dpadgett.timer;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;

public class TimerActivity extends Activity {

	static final String ACTION_SHOW_DIALOG = "org.dpadgett.timer.CountdownFragment.SHOW_DIALOG";
	static final String ACTION_DISMISS_DIALOG = "org.dpadgett.timer.CountdownFragment.DISMISS_DIALOG";
	
	private static enum Tab {
		WORLD_CLOCK("World Clock", WorldClockFragment.class),
		STOPWATCH("Stopwatch", StopwatchFragment.class),
		COUNTDOWN("Countdown", CountdownFragment.class);

		private final String title;
		private final Class<? extends Fragment> clazz;

		private Tab(String title, Class<? extends Fragment> clazz) {
			this.title = title;
			this.clazz = clazz;
		}
		
		private String getTitle() {
			return title;
		}
		
		private Class<? extends Fragment> getFragmentClass() {
			return clazz;
		}
	}
	
	private AlertDialog alarmDialog;
	private TabsAdapter mTabsAdapter;

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.main);
        
        ViewPager mViewPager = new ViewPager(this);
        mViewPager.setId(R.id.viewPager);
        setContentView(mViewPager);

        final ActionBar bar = getActionBar();
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        bar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);
        bar.setDisplayShowHomeEnabled(false);

        mTabsAdapter = new TabsAdapter(this, mViewPager);
        for (Tab tab : Tab.values()) {
	        mTabsAdapter.addTab(bar.newTab().setText(tab.getTitle()),
	                tab.getFragmentClass(), null);
        }

        if (savedInstanceState != null) {
            bar.setSelectedNavigationItem(savedInstanceState.getInt("tab", 0));
        }

		alarmDialog = new AlertDialog.Builder(this)
				.setTitle("Countdown timer finished")
				.setPositiveButton("Dismiss",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
								Intent intent = new Intent(TimerActivity.this, AlarmService.class)
									.putExtra("startAlarm", false).putExtra("fromFragment", true)
									.setAction("stopAlarm");
								TimerActivity.this.startService(intent);
							}
						})
				.setCancelable(false)
				.create();

		getApplicationContext().registerReceiver(showDialogReceiver, new IntentFilter(ACTION_SHOW_DIALOG));
		getApplicationContext().registerReceiver(dismissDialogReceiver, new IntentFilter(ACTION_DISMISS_DIALOG));
    }

	@Override
	protected void onDestroy() {
		super.onDestroy();
    	getApplicationContext().unregisterReceiver(showDialogReceiver);
    	getApplicationContext().unregisterReceiver(dismissDialogReceiver);
	}

    private BroadcastReceiver showDialogReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (alarmDialog != null) {
				alarmDialog.show();
			}

			CountdownFragment countdown =
					(CountdownFragment) mTabsAdapter.getItem(Tab.COUNTDOWN.ordinal());
			countdown.toggleInputMode();
		}
    };

    private BroadcastReceiver dismissDialogReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// alarmDialog could be null due to a variety of race conditions
			if (alarmDialog != null) {
				alarmDialog.dismiss();
			}
		}
    };
}