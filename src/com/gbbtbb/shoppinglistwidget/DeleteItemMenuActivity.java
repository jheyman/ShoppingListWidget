package com.gbbtbb.shoppinglistwidget;

import android.os.Bundle;
import android.app.Activity;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

public class DeleteItemMenuActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.i(ShoppingListWidgetProvider.TAG, "PopupMenuActivity creation, source bounds= "+getIntent().getSourceBounds());

		Bundle b = getIntent().getExtras();
		final String itemName = b.getString(ShoppingListWidgetProvider.EXTRA_ITEM_ID);

		this.requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.deleteitem_popup_menu);

		TextView t =  (TextView)findViewById(R.id.textViewItemName);
		t.setText("Effacer \""+itemName+"\"");

		// Attach a pending broadcast intent to the Confirm button, that will get sent to ShoppingListWidgetProvider
		final Intent deleteItemIntent = new Intent(this, ShoppingListWidgetProvider.class);
		deleteItemIntent.setAction(ShoppingListWidgetProvider.DELETEITEM_ACTION);
		// Pass on the incoming extras back into the output delete intent
		deleteItemIntent.putExtras(getIntent().getExtras());
		final PendingIntent deleteItemPendingIntent = PendingIntent.getBroadcast(this, 0, deleteItemIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		Button buttonConfirm = (Button) findViewById(R.id.button_confirm);
		buttonConfirm.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {  	
				try {
					Log.i(ShoppingListWidgetProvider.TAG, "Launching pending Intent for deleting item" + itemName);    		
					deleteItemPendingIntent.send();  
				} 
				catch (CanceledException ce) {
					Log.i(ShoppingListWidgetProvider.TAG, "Exception: "+ce.toString());
				}

				// Close this GUI activity, like a modal dialog would upon clicking on confirmation buttons.
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
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.popup_menu, menu);
		return true;
	}

}
