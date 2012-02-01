package org.dpadgett.timer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class WorldClockFragment extends Fragment {
	
	private DanResourceFinder finder;
	private Context context;
	private Handler uiHandler;
	private final ClockListAdapter clocksListAdapter;
    private final List<String> clockList;
	
	public WorldClockFragment() {
		clockList = new ArrayList<String>();
		clocksListAdapter = new ClockListAdapter();
	}

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
    	uiHandler = new Handler();
        View rootView = inflater.inflate(R.layout.world_clock, container, false);
        finder = DanWidgets.finderFrom(rootView);
        context = rootView.getContext();
        LinearLayout addClockView = (LinearLayout) finder.findViewById(R.id.addClockView);
        addClockView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				view.setBackgroundResource(
						Resources.getSystem().getIdentifier("list_selector_holo_dark", "drawable", "android"));
						//android.R.drawable.list_selector_background);
				uiHandler.post(new Runnable() {
					@Override
					public void run() {
						newClockDialog(-1);
					}
				});
			}
        });
		ListView clocksList = (ListView) finder.findViewById(R.id.clocksList);
		clocksList.setAdapter(clocksListAdapter);
		clocksList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				newClockDialog(position);
			}
		});
        return rootView;
    }

    private void newClockDialog(final int position) {
    	AlertDialog.Builder builder = new AlertDialog.Builder(context);
    	builder.setTitle("Select a timezone");
    	Set<Integer> timezones = new TreeSet<Integer>();
    	final Map<Integer, List<String>> offsetToID = new HashMap<Integer, List<String>>();
    	final long currentTime = System.currentTimeMillis();
    	for (String timezone : TimeZone.getAvailableIDs()) {
    		int millisOffset = TimeZone.getTimeZone(timezone).getOffset(currentTime);
			timezones.add(millisOffset);
			if (!offsetToID.containsKey(millisOffset)) {
				offsetToID.put(millisOffset, new ArrayList<String>());
			}
			offsetToID.get(millisOffset).add(timezone);
    	}
    	if (position > -1) {
	    	builder.setPositiveButton("Remove", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					clockList.remove(position);
					clocksListAdapter.notifyDataSetChanged();
				}
	    	});
    	}
    	LinearLayout tzView = (LinearLayout) LayoutInflater.from(context)
			.inflate(R.layout.timezone_picker_dialog, (ViewGroup) finder.findViewById(R.id.layout_root));

    	final List<String> initialItems = new ArrayList<String>();
    	initialItems.add("GMT");
    	initialItems.add("UTC");
    	final ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, R.layout.timezone_dialog_list_item,
    			initialItems);
    	ListView timezoneList = (ListView) tzView.findViewById(R.id.timezoneList);
    	timezoneList.setAdapter(adapter);
    	
    	final TextView sliderView = (TextView) tzView.findViewById(R.id.timezoneLabel);

    	final SeekBar timezoneSeeker = (SeekBar) tzView.findViewById(R.id.timezoneSeeker);
    	final List<Integer> timezonesList = new ArrayList<Integer>(timezones);
    	timezoneSeeker.setMax(timezonesList.size() - 1);
    	if (position > -1) {
    		int offset = TimeZone.getTimeZone(clockList.get(position)).getOffset(currentTime);
    		timezoneSeeker.setProgress(timezonesList.indexOf(offset));
    	} else {
    		timezoneSeeker.setProgress(timezonesList.indexOf(0));
    	}
    	timezoneSeeker.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
    		
    		// initialize the timezoneSeeker
    		{
    			onProgressChanged(timezoneSeeker, timezoneSeeker.getProgress(), false);
    		}
    		
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				adapter.clear();
				adapter.addAll(offsetToID.get(timezonesList.get(progress)));
				int millisOffset = timezonesList.get(progress);
				String offset = String.format("%02d:%02d", Math.abs(millisOffset / 1000 / 60 / 60), Math.abs(millisOffset / 1000 / 60) % 60);
				if (millisOffset / 1000 / 60 / 60 < 0) {
					offset = "-" + offset;
				} else {
					offset = "+" + offset;
				}
				sliderView.setText("UTC Offset: " + offset);
			}

			@Override public void onStartTrackingTouch(SeekBar seekBar) { }
			@Override public void onStopTrackingTouch(SeekBar seekBar) { }
    	});
    	builder.setView(tzView);
    	final AlertDialog alert = builder.create();

    	timezoneList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int selectedPosition, long id) {
				String timezone = adapter.getItem(selectedPosition);
		    	addNewClock(timezone, position);
		    	alert.dismiss();
			}
    	});

        alert.show();
    }

    /**
	 * Adds a new clock to the view
	 */
	private void addNewClock(String timeZone, int position) {
		if (position == -1) {
			clockList.add(timeZone);
		} else {
			clockList.set(position, timeZone);
		}
		clocksListAdapter.notifyDataSetChanged();
	}
	
	private class ClockListAdapter extends BaseAdapter {
		 
	    public ClockListAdapter() {
	    }
	 
	    public int getCount() {
	        return clockList.size();
	    }
	 
	    public String getItem(int position) {
	        return clockList.get(position);
	    }
	 
	    public long getItemId(int position) {
	        return clockList.get(position).hashCode();
	    }
	 
	    public View getView(int position, View convertView, ViewGroup parent) {
	        final String timezone = clockList.get(position);
	 
	        LinearLayout newClock =
	        		(LinearLayout) LayoutInflater.from(context)
	        			.inflate(R.layout.single_world_clock, parent, false);
	 
			AnalogClockWithTimezone analogClock =
					(AnalogClockWithTimezone) newClock.findViewById(R.id.analogClock);
			analogClock.setTimezone(timezone);

			final TextView clock = (TextView) newClock.findViewById(R.id.digitalClock);
			analogClock.addOnTickListener(new AnalogClockWithTimezone.OnTickListener() {
				@Override
				public void onTick() {
					updateClockTextView(clock, timezone);
				}
			});
	 
			TextView timezoneText = (TextView) newClock.findViewById(R.id.timezone);
			timezoneText.setText(timezone);
			updateClockTextView(clock, timezone);
	        return newClock;
	    }
	 
	}
	
	private void updateClockTextView(TextView clockToUpdate, String timezone) {
		SimpleDateFormat sdf = new SimpleDateFormat("h:mm:ss a");
		Date newDate = new Date(); // as a fallback
		sdf.setTimeZone(TimeZone.getTimeZone(timezone));
		String toText = sdf.format(newDate).toLowerCase();
		clockToUpdate.setText(toText);
	}
}
