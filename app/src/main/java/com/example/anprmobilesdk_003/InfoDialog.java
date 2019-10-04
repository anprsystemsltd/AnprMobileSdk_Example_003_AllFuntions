package com.example.anprmobilesdk_003;

import java.lang.reflect.Field;

import com.anprsystemsltd.sdk.mobile.CameraInput;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class InfoDialog extends Dialog
{
	
	private static String sdkName = "com.anprsystemsltd.sdk.mobile";
	
	
	
	private CameraInput.Found found;
	
	private Context context;
	
	private int width, height;
	private int margin;
	private int rowHeight;
	private int textSize;

	public InfoDialog(Context aContext, int aWidth, int aHeight, CameraInput.Found aFound, OnCancelListener aCancelListener)
	{
		super(aContext, true, aCancelListener);

		this.getWindow().requestFeature(Window.FEATURE_NO_TITLE);

		this.setContentView(R.layout.info_dialog);
		
		context = aContext;
		found = aFound;
		width = aWidth;
		height = aHeight;
		

		LinearLayout layout = (LinearLayout)this.findViewById(R.id.Layout_Info_Root);
		layout.getLayoutParams().width = width;
		layout.getLayoutParams().height = height;
		
		margin = (int)(aHeight * 0.02);
		rowHeight = (int)(aHeight * 0.1);
		textSize = (int)(rowHeight * 0.6);
		
		found = aFound;

		ImageView image = (ImageView)this.findViewById(R.id.Image_Info_Full);	// camera full image
		image.setImageBitmap(found.getFullImage());
		
		image = (ImageView)this.findViewById(R.id.Image_Info_Anpr);	// ANPR image (gray scaled)
		image.setImageBitmap(found.frame.anprInput.getAnprBitmap());
		
		image = (ImageView)this.findViewById(R.id.Image_Info_Plate);	// normalized plate image (gray scaled)
		image.setImageBitmap(found.frame.anprResult.normalizedPlateImage);
		
		layout = (LinearLayout)this.findViewById(R.id.Layout_Info_Found);
		layout.addView(createRow("found", found));
		
		int deb = 0;
		deb++;
		
		
		
	}
	
	
	private LinearLayout createRow(final String aName, final Object aValue)
	{
		LinearLayout ret = new LinearLayout(context);
		LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		llp.leftMargin = margin;
		llp.rightMargin = margin;
		llp.topMargin = margin;
		ret.setLayoutParams(llp);
		
		TextView text = new TextView(context);
		llp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
		llp.gravity = Gravity.LEFT;
		text.setLayoutParams(llp);
		text.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
		text.setTextColor(Color.WHITE);
		text.setTypeface(null, Typeface.BOLD);
		text.setText(aName + " = ");
		text.setGravity(Gravity.CENTER_VERTICAL);
		ret.addView(text);
		
		text = new TextView(context);
		llp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
		llp.gravity = Gravity.LEFT;
		text.setLayoutParams(llp);
		text.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
		text.setTextColor(Color.WHITE);
		text.setGravity(Gravity.CENTER_VERTICAL);
		ret.addView(text);
		String vn = String.valueOf(aValue);
		if (vn.startsWith(sdkName))
		{
			text.setText("CLICK HERE to view details");
			ret.setOnClickListener(new View.OnClickListener() 
			{
				@Override
				public void onClick(View v) 
				{
					
					ListDialog listDialog = new ListDialog(aValue, aName);
					listDialog.show();
					
				}
			});
		}
		else
		{
			text.setText(vn);
		}
		
		return ret;
	}
	
	
	
	
	private class ListDialog extends Dialog
	{
		
		public ListDialog(Object aObject, String aParent)
		{
			super(context);
			this.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
			
			FrameLayout lay = new FrameLayout(context);
			this.setContentView(lay);

			ScrollView scroll = new ScrollView(context);
			FrameLayout.LayoutParams flp = new FrameLayout.LayoutParams(width, height);
			scroll.setLayoutParams(flp);
			lay.addView(scroll);

			LinearLayout layout = new LinearLayout(context);
			layout.setOrientation(LinearLayout.VERTICAL);
			flp = new FrameLayout.LayoutParams(width, height);
			layout.setLayoutParams(flp);
			layout.setOrientation(LinearLayout.VERTICAL);
			scroll.addView(layout);
			
			Class cls = aObject.getClass();
			Field[] fields = cls.getFields();
			for (int i = 0; i < fields.length; i++)
			{
				try
				{
					Field field = fields[i];
					String name = field.getName();
					Object value = field.get(aObject);
					layout.addView(createRow(aParent + "." + name, value));
				}
				catch (Exception e)
				{
					
				}
			}

			
			
		}
		
	}
	
	
}
