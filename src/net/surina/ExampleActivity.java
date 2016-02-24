/////////////////////////////////////////////////////////////////////////////
///
/// Example Android Application/Activity that allows processing WAV 
/// audio files with SoundTouch library
///
/// Copyright (c) Olli Parviainen
///
////////////////////////////////////////////////////////////////////////////////
//
// $Id: SoundTouch.java 210 2015-05-14 20:03:56Z oparviai $
//
////////////////////////////////////////////////////////////////////////////////


package net.surina;

import java.io.File;

import net.surina.soundtouch.SoundTouch;
import net.surina.soundtouchexample.R;
import android.app.Activity;
import android.content.Intent;
import android.gesture.Prediction;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Telephony.Sms.Conversations;
import android.text.TextUtils;
import android.text.TextUtils.StringSplitter;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class ExampleActivity extends Activity implements OnClickListener, OnSeekBarChangeListener 
{
	private TextView textViewConsole = null;
	private SeekBar seekBarTempo;
	private SeekBar seekBarPitch;
	private SeekBar seekBarSpeed;
	private TextView textViewTempo;
	private TextView textViewPitch;
	private TextView textViewSpeed;
	
	StringBuilder consoleText = new StringBuilder();

	
	/// Called when the activity is created
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_example);
		
		seekBarTempo = (SeekBar)findViewById(R.id.SeekBar01);
		seekBarPitch = (SeekBar)findViewById(R.id.SeekBar02);
		seekBarSpeed = (SeekBar)findViewById(R.id.SeekBar03);
		seekBarTempo.setOnSeekBarChangeListener(this);
		seekBarPitch.setOnSeekBarChangeListener(this);
		seekBarSpeed.setOnSeekBarChangeListener(this);
		
		textViewTempo = (TextView)findViewById(R.id.TextView1);
		textViewPitch = (TextView)findViewById(R.id.TextView2);
		textViewSpeed = (TextView)findViewById(R.id.TextView3);
		
		textViewConsole = (TextView)findViewById(R.id.textViewResult);
		
		Button buttonProcess = (Button)findViewById(R.id.buttonProcess);
		buttonProcess.setOnClickListener(this);

		checkLibVersion();
	}
		
	/// Function to append status text onto "console box" on the Activity
	public void appendToConsole(final String text)
	{
		// run on UI thread to avoid conflicts
		runOnUiThread(new Runnable() 
		{
		    public void run() 
		    {
				consoleText.append(text);
				consoleText.append("\n");
				textViewConsole.setText(consoleText);
		    }
		});
	}
	

	
	/// print SoundTouch native library version onto console
	protected void checkLibVersion()
	{
		String ver = SoundTouch.getVersionString();
		appendToConsole("SoundTouch native library version = " + ver);
	}



	/// Button click handler
	@Override
	public void onClick(View arg0) 
	{
		switch (arg0.getId())
		{
			case R.id.buttonProcess:
				process();
				break;						
		}
		
	}
	
	
	/// Play audio file
	protected void playWavFile(String fileName)
	{
		File file2play = new File(fileName);
		Intent i = new Intent();
		i.setAction(android.content.Intent.ACTION_VIEW);
		i.setDataAndType(Uri.fromFile(file2play), "audio/wav");
		startActivity(i);		
	}
	
				

	/// Helper class that will execute the SoundTouch processing. As the processing may take
	/// some time, run it in background thread to avoid hanging of the UI.
	protected class ProcessTask extends AsyncTask<ProcessTask.Parameters, Integer, Long>
	{
		/// Helper class to store the SoundTouch file processing parameters
		public final class Parameters {
			String inFileName;
			String outFileName;
			float tempo;
			float pitch;
			float speed;
		}
		
		/// Function that does the SoundTouch processing
		public final long doSoundTouchProcessing(Parameters params) 
		{
			
			SoundTouch st = new SoundTouch();
			st.setTempo(params.tempo);
			st.setPitchSemiTones(params.pitch);
			st.setSpeed(params.speed);
			Log.i("SoundTouch", "process file " + params.inFileName);
			long startTime = System.currentTimeMillis();
			int res = st.processFile(params.inFileName, params.outFileName);
			long endTime = System.currentTimeMillis();
			float duration = (endTime - startTime) * 0.001f;
			
			Log.i("SoundTouch", "process file done, duration = " + duration);
			appendToConsole("Processing done, duration " + duration + " sec.");
			if (res != 0)
			{
				String err = SoundTouch.getErrorString();
				appendToConsole("Failure: " + err);
				return -1L;
			}
			
			// Play file if so is desirable
			playWavFile(params.outFileName);
			return 0L;
		}
		
		/// Overloaded function that get called by the system to perform the background processing
		@Override	
		protected Long doInBackground(Parameters... aparams) 
		{
			return doSoundTouchProcessing(aparams[0]);
		}
		
	}

	
	private int convertStringToInt(String text) {
		String[] list = TextUtils.split(text, ":");
		String type = list[0];
		String value = list[1];
		
		return Integer.parseInt(value);	
	}
	/// process a file with SoundTouch. Do the processing using a background processing
	/// task to avoid hanging of the UI
	protected void process()
	{
		try {
			ProcessTask task = new ProcessTask();
			ProcessTask.Parameters params = task.new Parameters();
			// parse processing parameters
			params.inFileName = "/storage/emulated/0/MIUI/sound_recorder/new.wav";
			params.outFileName = "/storage/emulated/0/MIUI/sound_recorder/new_after.wav";
			
			params.tempo = 0.01f * convertStringToInt(textViewTempo.getText().toString());
			params.pitch = convertStringToInt(textViewPitch .getText().toString());
			params.speed =  convertStringToInt(textViewSpeed.getText().toString());

			// update UI about status
			appendToConsole("Process audio file :" + params.inFileName +" => " + params.outFileName);
			appendToConsole("Tempo = " + params.tempo);
			appendToConsole("Pitch adjust = " + params.pitch);
			
			Toast.makeText(this, "Starting to process file " + params.inFileName + "...", Toast.LENGTH_SHORT).show();

			// start SoundTouch processing in a background thread
			task.execute(params);
//			task.doSoundTouchProcessing(params);	// this would run processing in main thread
			
		} catch (Exception exp) {
			exp.printStackTrace();
		}
	}

	@Override
	public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
		switch(seekBar.getId()) {
		case R.id.SeekBar01:
			textViewTempo.setText("tempo:" + progress);
			break;
		case R.id.SeekBar02:
			textViewPitch.setText("pitch:"+progress);
			break;
		case R.id.SeekBar03:
			textViewSpeed.setText("speed:"+progress);
			break;
		}
	}

	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		
	}

	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		
	}
}