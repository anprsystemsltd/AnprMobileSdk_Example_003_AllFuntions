package com.example.anprmobilesdk_003;

import java.text.SimpleDateFormat;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.anprsystemsltd.sdk.mobile.ANPR;
import com.anprsystemsltd.sdk.mobile.CameraInput;
import com.anprsystemsltd.sdk.mobile.Event;
import com.anprsystemsltd.sdk.mobile.Result;
import com.anprsystemsltd.sdk.mobile.Tools;

public class MainActivity extends Activity
{
	private Context context;		// application context
	private Context activityContext;	// activity context 

	private ANPR anprSdk;			// this is the SDK class
	
	private CameraInput cameraInput;	// camera control class
	
	private int screenWidth, screenHeight;

	private int zoomMode, zoomMin, zoomMax;	// zoom params if supported
	
	private CameraInput.Found[] founds;
	
	@Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        
        this.setContentView(R.layout.main_activity);
        
        context = getApplicationContext();	// get application context
        activityContext = this;
        
        DisplayMetrics displayMetrics = new DisplayMetrics();		// get screen dimensions
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenHeight = displayMetrics.heightPixels;
        screenWidth = displayMetrics.widthPixels;
        
        founds = new CameraInput.Found[4];		// store last four founds
 
        anprSdk = new ANPR(context, sdkListener);	// create new SDK object

        ANPR.Parameters parameters = new ANPR.Parameters();				// create SDK initial parameters
							
        parameters.licenseMode = ANPR.Parameters.LICENSE_MODE_ONLINE;	// SDK licence managed via WEB portal

        parameters.requestNationality = "IND";		// at first run SDK will download the Indian ANPR native library into device file system (in background)

        anprSdk.init(parameters);	// init SDK
        
    }	
	

	private ANPR.EventListener sdkListener = new ANPR.EventListener() 	// listener to handle SDK events
	{
		@Override
		public void onEvent(Event event)
		{
			switch (event.type)		// event.code determines what type of event
			{
				case ANPR.EventListener.EVENT_TYPE_LIBRARY:	// SDK opened the native ANPR library
					Log.i("Event Library", (String)event.object);	// Log the opened library name
					break;
					
				case ANPR.EventListener.EVENT_TYPE_LICENCE:	// SDK checked the licence
					Log.i("Event Licence", String.valueOf(event.success));	// Log licencing successful or not
													// if licencing not successful everything work but the first two character of recognized plate are replaced to "XX"
					break;
					
				case ANPR.EventListener.EVENT_TYPE_INIT:		// SDK initialized
					Log.i("Event Init", String.valueOf(event.success));	// Log initializing successful or not
															// if not successfull, see event.result for the reason
					if (event.success == true)	// if initializing successful
					{
						startCamera();			// start the camera (see below)
					}
					else						// if initializing not successfull
					{
						exitWithError(event.result.errorMessage, event.result.data);
					}
					break;
			}
			
		}
	};
	

	private void startCamera()
	{
        cameraInput = new CameraInput(context, cameraListener, CameraInput.MODE_WITH_PREVIEW);	// create new camera object with display camera live preview
		FrameLayout lay = (FrameLayout)findViewById(R.id.Layout_Main_Preview);	
		cameraInput.assignPreview(lay);											// set this layout to show camera preview
        
        CameraInput.Parameters cameraInputParameters = CameraInput.Parameters.CreateDefault(context);	// create camera initial parameters with default values
        if (cameraInputParameters.result.code == Result.OK)
        {
        	ANPR.AnprParameters anprParameters = new ANPR.AnprParameters();	// set the recognizing parameters
        	anprParameters.detectSquarePlates = 1;	// detect two lines plates (for example motorbikes) default:0
        	anprParameters.useLightCorrection = 1;  // use if plates possible unevenly lit (default:1)
        	anprParameters.detectWhiteOnBlack = 1;  // detect inverse color plates (default:1)
        	
        	anprParameters.filter = new ANPR.AnprParameters.Filter();	// this filters the plates if set
        	
        	anprParameters.filter.minPlateHeight = 16;  // plates min height pixel (default:0)
        	anprParameters.filter.maxPlateHeight = 60;	// plates max height pixel (default:unlimited)
        												// filters to small (too far)  or too big plates
        	
        	anprParameters.filter.minPlateLen = 5;	// min characters count in plates (default:0)
        	anprParameters.filter.maxPlateLen = 10;	// max characters count in plates (default:unlimited)
        											// filters fragments plates (too short) and other texts in the environment (too long)
        	
        	anprParameters.filter.minConfidence = 80;	// filters uncertain plates (default:0)
        	
        	cameraInput.setAnprParameters(anprParameters);	// adapt the anpr parameters
        	
        	
        	cameraInputParameters.infoFormat = new CameraInput.Parameters.WriteInfoFormat();		// set this to writing infos into pictures
        	cameraInputParameters.infoFormat.format = CameraInput.Parameters.WriteInfoFormat.ELEMENT_PLATE + " Time:" + CameraInput.Parameters.WriteInfoFormat.ELEMENT_TIME + " " + CameraInput.Parameters.WriteInfoFormat.ELEMENT_LOCATION;	// info string format
        	cameraInputParameters.infoFormat.timeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");	// info time format

        	TextView text = (TextView)this.findViewById(R.id.Text_Main_GpsStatus);	// GPS indicator
        	text.setTextSize(TypedValue.COMPLEX_UNIT_PX, (int)(screenHeight * 0.05));
           	if (CameraInput.LocationGPS.GetSupportedMode(context) != CameraInput.LocationGPS.Parameters.MODE_NONE)	// if location supported
           	{
            	cameraInputParameters.locationParameters = new CameraInput.LocationGPS.Parameters(CameraInput.LocationGPS.Parameters.MODE_BOTH);	// set this to using GPS (requires extra permissions in manifest)
            	cameraInputParameters.locationParameters.stringFormat.setModeNames(new String[]{"None", "NET", "SAT"});
            	cameraInputParameters.locationParameters.stringFormat.round = 4;	// round GPS coordinate to decimal
            	cameraInputParameters.locationParameters.reportMinDistance = 10;	// set GPS sensitivity to 10 meters

           		text.setText("GPS: NET");
           		text.setTextColor(Color.YELLOW);
           	}
           	else
           	{
           		text.setText("GPS: NONE");
           		text.setTextColor(Color.RED);
           	}

            Result result = cameraInput.init(cameraInputParameters);	// init camera
            if (result.code == Result.OK)								// if initializing successed
            {
            	zoomMode = cameraInput.getSupportedZoomMode();
               	if (zoomMode >= CameraInput.ZOOM_MODE_DIRECT)	// if supports zoom
            	{
                	cameraInput.showZoomDisplay();		// write zoom to preview

                	SeekBar seek = (SeekBar)this.findViewById(R.id.Seek_Main_Resolution);	// SeekBar for Zoom control
                    int previewWidth = (int)(screenWidth * 0.75);
                    int margin = (int)(previewWidth * 0.15);
                    int width = previewWidth - 2 * margin;
                    FrameLayout.LayoutParams flp = (FrameLayout.LayoutParams)seek.getLayoutParams();
                    flp.width = width;
                    flp.leftMargin = margin;
                    flp.bottomMargin = margin / 2;
                    List<Integer> zooms = cameraInput.getSupportedZoomRatios();
                    zoomMin = 100;
                    zoomMax = zooms.get(zooms.size() - 1);
                    seek.setMax(zoomMax - zoomMin);
                    seek.setVisibility(View.VISIBLE);
                    seek.setOnSeekBarChangeListener(new OnSeekBarChangeListener()
                    {
						@Override
						public void onProgressChanged(SeekBar seekBar,	int progress, boolean fromUser) 
						{
							int value = seekBar.getProgress() + zoomMin;
							cameraInput.writeSelectZoomDisplay(value);	// write desired zoom
						}

						@Override
						public void onStartTrackingTouch(SeekBar seekBar) 
						{
						}

						@Override
						public void onStopTrackingTouch(SeekBar seekBar) 
						{
							int value = seekBar.getProgress() + zoomMin;
							cameraInput.setZoomAsValue(value, zoomMode);	// set zoom
						}
                    	
                    });
            	}

               	cameraInput.startVideo();			// start camera preview
                cameraInput.startRecognizing();		// start recognition
            	
            }
			else
			{
				exitWithError(result.errorMessage, result.data);
			}

        
        }
		else
		{
			exitWithError(cameraInputParameters.result.errorMessage, cameraInputParameters.result.data);
		}




	}

	private CameraInput.EventListener cameraListener = new CameraInput.EventListener() {

		@Override
		public void onEvent(Event event) {
			switch (event.type) {
				case CameraInput.EventListener.EVENT_TYPE_CAMERA_PARAMETERS:		// camera initialized with this android camera parameters
					Camera.Parameters parameters = (Camera.Parameters)event.object;
					Size size = parameters.getPreviewSize();	// Log camera resolution
					Log.i("Camera parameter", String.valueOf(size.width) + "X" + String.valueOf(size.height));

					break;

				case CameraInput.EventListener.EVENT_TYPE_CAMERA_FRAME:	// event every camera preview frame
					break;

				case CameraInput.EventListener.EVENT_TYPE_CAMERA_ZOOM:		// when zoom changed
					boolean finished = (Boolean)event.object2;	// zooming process finished
                	SeekBar seek = (SeekBar)findViewById(R.id.Seek_Main_Resolution);	// SeekBar for Zoom control
					seek.setEnabled(finished);
					break;

				case CameraInput.EventListener.EVENT_TYPE_CAMERA_PLATE_FOUND:	// when found licence plate
					final CameraInput.Found found = (CameraInput.Found)event.object;	// found object
					Log.i("Plate found", found.plate);	// Log plate
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							setTitle(found.plate);	// set to title
							cameraInput.stopRecognizing();	// pause to search plates
							drawNewFound(found);
						}
					});
					break;

				case CameraInput.EventListener.EVENT_TYPE_CAMERA_LOCATION_SATELLITE_STATUS_CHANGED:	// when GPS state changed
					boolean satReady = event.success;							// satellite is ready
					Log.i("Satellite status", String.valueOf(event.success));
					TextView text = (TextView)findViewById(R.id.Text_Main_GpsStatus);	// GPS indicator
					if (satReady == true)
					{
						text.setText("GPS: SAT");
						text.setTextColor(Color.GREEN);
					}
					else
					{
						text.setText("GPS: NET");
						text.setTextColor(Color.YELLOW);
					}
					break;

				case CameraInput.EventListener.EVENT_TYPE_CAMERA_LOCATION_POSITION_CHANGED:
					CameraInput.LocationGPS.Result pos = (CameraInput.LocationGPS.Result)event.object;
					Log.i("Location position", pos.toString());
					break;


			}
		}
	};

	private void exitWithError(String aTitle, String aMessage)
	{
		Handler handler = new Handler()
		{
			public void handleMessage(Message mes)
			{
				finish();	// exit
			}
		};
		Tools.ShowMessageDialog(activityContext, aTitle, aMessage, handler);	// show error
	}


	private void drawNewFound(CameraInput.Found aFound)
	{
		founds[0] = aFound;
		ImageView image = (ImageView)this.findViewById(R.id.Image_Main_Preview);	// draw full image
		Bitmap bitmap = founds[0].getFullImage();
		image.setImageBitmap(bitmap);
		image.setVisibility(View.VISIBLE);	// show image

		image = (ImageView)this.findViewById(R.id.Image_Main_Plate);	// draw plate image
		bitmap = founds[0].frame.anprResult.normalizedPlateImage;
		image.setImageBitmap(bitmap);
		
		
		Handler handler = new Handler()		// after 3 secs run this handler to scroll images and continue plate searching
		{
			public void handleMessage(Message mes)
			{
				for (int i = 3; i > 0; i--)
				{
					founds[i] = founds[i - 1];
					if (founds[i] != null)
					{
						drawFound(i);			
					}
				}
				
				ImageView image = (ImageView)findViewById(R.id.Image_Main_Preview);	// hide full image
				image.setVisibility(View.GONE);
				image = (ImageView)findViewById(R.id.Image_Main_Plate);		// clear plate image
				image.setImageBitmap(null);
				
				cameraInput.startRecognizing();		// continue to search plates
			}
		};
		handler.sendEmptyMessageDelayed(0, 3000);
	}
	
	
	private void drawFound(int aIndex)
	{
		int resID = getResources().getIdentifier("Image_Main_FoundFull_" + String.valueOf(aIndex),	"id", getPackageName());
		ImageView image = (ImageView)this.findViewById(resID);
		image.setImageBitmap(founds[aIndex].getFullImage());
		
		resID = getResources().getIdentifier("Image_Main_FoundPlate_" + String.valueOf(aIndex),	"id", getPackageName());
		image = (ImageView)this.findViewById(resID);
		image.setImageBitmap(founds[aIndex].frame.anprResult.normalizedPlateImage);

		resID = getResources().getIdentifier("Text_Main_Plate_" + String.valueOf(aIndex),	"id", getPackageName());
		TextView text = (TextView)this.findViewById(resID);
		text.setTextSize(TypedValue.COMPLEX_UNIT_PX, (int)(screenHeight * 0.3 * 0.25 * 0.6));
		text.setText(founds[aIndex].plate);
	}
	
	
	public void infoClickListener(View aView)
	{
		String tag = (String)aView.getTag();
		if (tag != null)
		{
			int index = -1;
			try
			{
				index = Integer.valueOf(tag);
			}
			catch (Exception e)
			{
				
			}
			
			if (index != -1)
			{
				CameraInput.Found found = founds[index];
				if (found != null)
				{
					cameraInput.stopRecognizing();
					InfoDialog dialog = new InfoDialog(this, (int)(screenWidth * 0.85), (int)(screenHeight * 0.85), found, new OnCancelListener()
					{
						@Override
						public void onCancel(DialogInterface dialog) 
						{
							cameraInput.startRecognizing();
						}
					});
					dialog.show();
				}
			}
			
		}
	}
	
	
	
	
	protected void onDestroy()
	{
	    super.onDestroy();
	    
	    if (cameraInput != null)	// close camera
	    {
	    	cameraInput.close();
	    	cameraInput = null;
	    }
	    if (anprSdk != null)		// close SDK
	    {
	    	anprSdk.close();
	    	anprSdk = null;
	    }
	    System.exit(0);
	}
	
	
}
