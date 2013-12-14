package com.gbbtbb.shoppinglistwidget;

import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.Paint.Align;
import android.os.Bundle;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.gbbtbb.shoppinglistwidget.ShoppingListDataProvider.Columns;

/**
 * This is the service that provides the factory to be bound to the collection service.
 */
public class ShoppingListWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new StackRemoteViewsFactory(this.getApplicationContext(), intent);
    }
}

/**
 * This is the factory that will provide data to the collection widget.
 */
class StackRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
    private Context mContext;
    private Cursor mCursor;
    public StackRemoteViewsFactory(Context context, Intent intent) {
        mContext = context;
        intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
    }

    public void onCreate() {
        // Initialize the list with some empty items, to make the list look good
    	final MatrixCursor c = new MatrixCursor(new String[]{ Columns.ID, Columns.ITEM, Columns.QUANTITY });       

    	for (int i=0; i<25; i++) {
    		c.addRow(new Object[]{i, "", ""});
    	}
    	mCursor = c;
    }

    public void onDestroy() {
        if (mCursor != null) {
            mCursor.close();
        }
    }

    public int getCount() {
        return mCursor.getCount();
    }

    public Bitmap drawTextOnList(String textToDisplay) 
	{

		Bitmap tmpBitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.paperpad_one_row);
	    Bitmap mutableBitmap = tmpBitmap.copy(tmpBitmap.getConfig(), true);
	    
	    Canvas myCanvas = new Canvas(mutableBitmap);
	    Typeface myfont = Typeface.createFromAsset(mContext.getAssets(),"fonts/passing_notes.ttf");
	    
	    TextPaint textPaint = new TextPaint();
	    textPaint.setTypeface(myfont);
	    textPaint.setStyle(Paint.Style.FILL);
	    textPaint.setTextSize(20);
	    textPaint.setColor(Color.BLACK);
	    textPaint.setTextAlign(Align.LEFT);
	    textPaint.setAntiAlias(true);
	    textPaint.setSubpixelText(true);

	    StaticLayout sl = new StaticLayout(textToDisplay, textPaint, ((int)90*myCanvas.getWidth())/100, Layout.Alignment.ALIGN_NORMAL, 1, 1, false);

	    myCanvas.save();
	    myCanvas.translate(65, 0);
	    sl.draw(myCanvas);
	    myCanvas.restore();
	    
	    return mutableBitmap;
	}
    
    public RemoteViews getViewAt(int position) {
        // Get the data for this position from the content provider
        String item = "Unknown Item";
        //String temp = "Unknown quantity";
        if (mCursor.moveToPosition(position)) {
            final int itemColIndex = mCursor.getColumnIndex(ShoppingListDataProvider.Columns.ITEM);
            //final int quantityColIndex = mCursor.getColumnIndex(ShoppingListDataProvider.Columns.QUANTITY);
            item = mCursor.getString(itemColIndex);
            //temp = mCursor.getString(quantityColIndex);
        }

        final String formatStr = mContext.getResources().getString(R.string.item_format_string);
        final int itemId = R.layout.widget_item;
        RemoteViews rv = new RemoteViews(mContext.getPackageName(), itemId);
        //rv.setTextViewText(R.id.widget_item, String.format(formatStr, item));  
        rv.setImageViewBitmap(R.id.widget_item, drawTextOnList(String.format(formatStr, item)));
        
        // Set the click intent so that we can handle it
        // Do not set a click handler on dummy items that are there only to make the list look good when empty
        if (item.compareTo("")!=0) {
        final Intent fillInIntent = new Intent();
        final Bundle extras = new Bundle();
        extras.putString(ShoppingListWidgetProvider.EXTRA_ITEM_ID, item);
        fillInIntent.putExtras(extras);
        rv.setOnClickFillInIntent(R.id.widget_item, fillInIntent);
        }

        return rv;
    }
    public RemoteViews getLoadingView() {
        return null;
    }

    public int getViewTypeCount() {
        return 1;
    }

    public long getItemId(int position) {
        return position;
    }

    public boolean hasStableIds() {
        return true;
    }

    public void onDataSetChanged() {
        Log.i(ShoppingListWidgetProvider.TAG, "onDataSetChanged called");
    	// Refresh the cursor
        if (mCursor != null) {
            mCursor.close();
        }
        mCursor = mContext.getContentResolver().query(ShoppingListDataProvider.CONTENT_URI, null, null,null, null);
                
    	final Intent doneIntent = new Intent(mContext, ShoppingListWidgetProvider.class);
    	doneIntent.setAction(ShoppingListWidgetProvider.RELOAD_ACTION_DONE);
		final PendingIntent donePendingIntent = PendingIntent.getBroadcast(mContext, 0, doneIntent, PendingIntent.FLAG_UPDATE_CURRENT);
              
        try {
			Log.i(ShoppingListWidgetProvider.TAG, "onDataSetChanged: launching pending Intent for loading done");    		
			donePendingIntent.send();
		} 
		catch (CanceledException ce) {
			Log.i(ShoppingListWidgetProvider.TAG, "onDataSetChanged: Exception: "+ce.toString());
		}        
    }
}