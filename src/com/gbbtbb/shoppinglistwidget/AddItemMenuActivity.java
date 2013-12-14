package com.gbbtbb.shoppinglistwidget;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
/*
 * Activity implementing the pop-up menu for adding an item to the shopping list
 */
public class AddItemMenuActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.requestWindowFeature(Window.FEATURE_NO_TITLE);

		Log.i(ShoppingListWidgetProvider.TAG, "AddItemMenuActivity creation, source bounds= "+getIntent().getSourceBounds());

		setContentView(R.layout.additem_popup_menu);

		final EditText input =  (EditText)findViewById(R.id.textViewItemName);

		Button buttonConfirm = (Button) findViewById(R.id.button_confirm);

		buttonConfirm.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Editable value = input.getText();
				Log.i(ShoppingListWidgetProvider.TAG, "AddItemMenuActivity: new item to add is " + value.toString());

				final Intent addItemIntent = new Intent(AddItemMenuActivity.this, ShoppingListWidgetProvider.class);
				addItemIntent.setAction(ShoppingListWidgetProvider.ADDITEM_ACTION);

				final Bundle extras = new Bundle();
				extras.putString(ShoppingListWidgetProvider.ADDITEM_ACTION, value.toString());
				addItemIntent.putExtras(extras);
				final PendingIntent addItemPendingIntent = PendingIntent.getBroadcast(AddItemMenuActivity.this, 0, addItemIntent, PendingIntent.FLAG_UPDATE_CURRENT);

				try {
					Log.i(ShoppingListWidgetProvider.TAG, "AddItemMenuActivity: launching addItem pending Intent");    		
					addItemPendingIntent.send();
				} 
				catch (CanceledException ce) {
					Log.i(ShoppingListWidgetProvider.TAG, "AddItemMenuActivity: addItemPendingIntent.send Exception: "+ce.toString());
				}

				finish();
			}
		});

		Button buttonCancel = (Button) findViewById(R.id.button_cancel);

		buttonCancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Log.i(ShoppingListWidgetProvider.TAG, "Canceled delete item");    		
				// Close this GUI activity
				finish();
			}
		});		

		WindowManager.LayoutParams wmlp = this.getWindow().getAttributes();
		wmlp.gravity = Gravity.TOP | Gravity.LEFT;

		// Get the x,y coordinates of the button that triggered this dialog, and align the dialog to it.
		Rect r = getIntent().getSourceBounds();

		wmlp.x = r.left;  //x position
		wmlp.y = r.top;   //y position
		this.getWindow().setAttributes(wmlp);  	
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.popup_menu, menu);
		return true;
	}
}
