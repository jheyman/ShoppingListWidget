package com.gbbtbb.shoppinglistwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.Paint.Align;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

/**
 * Our data observer just notifies an update for all widgets when it detects a change.
 */
class ShoppingListDataProviderObserver extends ContentObserver {
	private AppWidgetManager mAppWidgetManager;
	private ComponentName mComponentName;

	ShoppingListDataProviderObserver(AppWidgetManager mgr, ComponentName cn, Handler h) {
		super(h);
		mAppWidgetManager = mgr;
		mComponentName = cn;
	}

	@Override
	public void onChange(boolean selfChange) {
		// The data has changed, so notify the widget that the collection view needs to be updated.
		// In response, the factory's onDataSetChanged() will be called which will requery the
		// cursor for the new data.
		mAppWidgetManager.notifyAppWidgetViewDataChanged(
				mAppWidgetManager.getAppWidgetIds(mComponentName), R.id.shopping_list);
	}
}

/**
 * The widget's AppWidgetProvider.
 */
public class ShoppingListWidgetProvider extends AppWidgetProvider {
	public static final String TAG = "ShoppingList";
	
	public static String CLICK_ACTION = "com.gbbtbb.shoppinglistwidget.CLICK";
	public static String CLEANLIST_ACTION = "com.gbbtbb.shoppinglistwidget.CLEANLIST";
	public static String ADDITEM_ACTION = "com.gbbtbb.shoppinglistwidget.ADDITEM";
	public static String RELOAD_ACTION = "com.gbbtbb.shoppinglistwidget.RELOAD_LIST";
	public static String RELOAD_ACTION_DONE = "com.gbbtbb.shoppinglistwidget.RELOAD_LIST_DONE";
	public static String DELETEITEM_ACTION = "com.gbbtbb.shoppinglistwidget.DELETEITEM";
	public static String EXTRA_ITEM_ID = "com.gbbtbb.shoppinglistwidget.item";

	private static HandlerThread sWorkerThread;
	private static Handler sWorkerQueue;
	private static ShoppingListDataProviderObserver sDataObserver;
	private static boolean progressBarEnabled = false;

	public ShoppingListWidgetProvider() {
		// Start the worker thread
		sWorkerThread = new HandlerThread("ShoppingListWidgetProvider-worker");
		sWorkerThread.start();
		sWorkerQueue = new Handler(sWorkerThread.getLooper());
	}

	@Override
	public void onEnabled(Context context) {
		Log.i(ShoppingListWidgetProvider.TAG, "onEnabled");

		// Register for external updates to the data to trigger an update of the widget.  When using
		// content providers, the data is often updated via a background service, or in response to
		// user interaction in the main app.  To ensure that the widget always reflects the current
		// state of the data, we must listen for changes and update ourselves accordingly.
		final ContentResolver r = context.getContentResolver();
		if (sDataObserver == null) {
			final AppWidgetManager mgr = AppWidgetManager.getInstance(context);
			final ComponentName cn = new ComponentName(context, ShoppingListWidgetProvider.class);
			sDataObserver = new ShoppingListDataProviderObserver(mgr, cn, sWorkerQueue);
			r.registerContentObserver(ShoppingListDataProvider.CONTENT_URI, true, sDataObserver);
			Log.i(ShoppingListWidgetProvider.TAG, "onEnabled: Registered data observer");
		}
	}

	@Override
	public void onReceive(Context ctx, Intent intent) {
		final String action = intent.getAction();
		
		Log.i(ShoppingListWidgetProvider.TAG, "onReceive: " + action);

		if (action.equals(ADDITEM_ACTION)) {

			final Context context = ctx;
			final Intent i = intent;
			sWorkerQueue.removeMessages(0);
			sWorkerQueue.post(new Runnable() {
				@Override
				public void run() {
					final ContentResolver r = context.getContentResolver();

					// We disable the data changed observer temporarily since each of the updates
					// will trigger an onChange() in our data observer.
					r.unregisterContentObserver(sDataObserver);

					final ContentValues values = new ContentValues();
									
					String newItemName = i.getExtras().getString(ADDITEM_ACTION);

					setLoadingInProgress(context, true);
					
					if (newItemName != null)
					{
						values.put(ShoppingListDataProvider.Columns.ITEM, newItemName);
						r.insert(ShoppingListDataProvider.CONTENT_URI, values);
					}
					else
						Log.i(ShoppingListWidgetProvider.TAG, "onReceive/addItem got null newItemName");
					
					r.registerContentObserver(ShoppingListDataProvider.CONTENT_URI, true, sDataObserver);

					final AppWidgetManager mgr = AppWidgetManager.getInstance(context);
					final ComponentName cn = new ComponentName(context, ShoppingListWidgetProvider.class);
					mgr.notifyAppWidgetViewDataChanged(mgr.getAppWidgetIds(cn), R.id.shopping_list);
				}
			});        	

		} 
		else if (action.equals(CLEANLIST_ACTION)) {

			final Context context = ctx;
			sWorkerQueue.removeMessages(0);
			sWorkerQueue.post(new Runnable() {
				@Override
				public void run() {
					final ContentResolver r = context.getContentResolver();
					String whereClause = "*";
					String[] args = new String[] { "" };
					
					setLoadingInProgress(context,true);
					
					// We disable the data changed observer temporarily since each of the updates
					// will trigger an onChange() in our data observer.
					r.unregisterContentObserver(sDataObserver);
					r.delete(ShoppingListDataProvider.CONTENT_URI, whereClause, args);
					r.registerContentObserver(ShoppingListDataProvider.CONTENT_URI, true, sDataObserver);

					final AppWidgetManager mgr = AppWidgetManager.getInstance(context);
					final ComponentName cn = new ComponentName(context, ShoppingListWidgetProvider.class);
					mgr.notifyAppWidgetViewDataChanged(mgr.getAppWidgetIds(cn), R.id.shopping_list);
				}
			});
		} else if (action.equals(CLICK_ACTION)) {
			
			final String item = intent.getStringExtra(EXTRA_ITEM_ID);

			Bundle b = new Bundle();
			b.putString(EXTRA_ITEM_ID, item); 
			intent.putExtras(b); 
			intent.setClass(ctx, DeleteItemMenuActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		
			ctx.startActivity(intent);
			
		} else if (action.equals(DELETEITEM_ACTION)) {
			
			Bundle b = intent.getExtras();
			final String itemName = b.getString(ShoppingListWidgetProvider.EXTRA_ITEM_ID);
			
			final Context context = ctx;
			sWorkerQueue.removeMessages(0);
			sWorkerQueue.post(new Runnable() {
				@Override
				public void run() {
					final ContentResolver r = context.getContentResolver();
					String whereClause = itemName;
					String[] args = new String[] { "" };
					
					setLoadingInProgress(context, true);
					
					// We disable the data changed observer temporarily since each of the updates
					// will trigger an onChange() in our data observer.
					r.unregisterContentObserver(sDataObserver);
					r.delete(ShoppingListDataProvider.CONTENT_URI, whereClause, args);
					r.registerContentObserver(ShoppingListDataProvider.CONTENT_URI, true, sDataObserver);

					final AppWidgetManager mgr = AppWidgetManager.getInstance(context);
					final ComponentName cn = new ComponentName(context, ShoppingListWidgetProvider.class);
					mgr.notifyAppWidgetViewDataChanged(mgr.getAppWidgetIds(cn), R.id.shopping_list);
				}
			});			
		} 
		else if (action.equals(RELOAD_ACTION) || action.equals(AppWidgetManager.ACTION_APPWIDGET_UPDATE)) {
			
			final Context context = ctx;
			sWorkerQueue.removeMessages(0);
			sWorkerQueue.post(new Runnable() {
				@Override
				public void run() {

					setLoadingInProgress(context, true);

					// Just force a reload by notifying that data has changed
					final AppWidgetManager mgr = AppWidgetManager.getInstance(context);
					final ComponentName cn = new ComponentName(context, ShoppingListWidgetProvider.class);
					mgr.notifyAppWidgetViewDataChanged(mgr.getAppWidgetIds(cn), R.id.shopping_list);
					Log.i(ShoppingListWidgetProvider.TAG, "onReceive: notified appwidget to refresh");
				}
			});				
	    }
		else if (action.equals(RELOAD_ACTION_DONE)) {
			
			Log.i(ShoppingListWidgetProvider.TAG, "processing RELOAD_ACTION_DONE...");
			final Context context = ctx;
			setLoadingInProgress(context, false);
		}

		super.onReceive(ctx, intent);
	}

	private void setLoadingInProgress(Context context, boolean state) {
		
		AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
		ComponentName widgetComponent = new ComponentName(context, ShoppingListWidgetProvider.class);
		int[] widgetIds = widgetManager.getAppWidgetIds(widgetComponent);

		progressBarEnabled = state;
		onUpdate(context, widgetManager, widgetIds);
	}
	
	
    public Bitmap drawTextOnList(Context ctx, String textToDisplay, int background) 
	{

		Bitmap tmpBitmap = BitmapFactory.decodeResource(ctx.getResources(), background);
	    Bitmap mutableBitmap = tmpBitmap.copy(tmpBitmap.getConfig(), true);
	    
	    Canvas myCanvas = new Canvas(mutableBitmap);
	    Typeface myfont = Typeface.createFromAsset(ctx.getAssets(),"fonts/passing_notes.ttf");
	    
	    TextPaint textPaint = new TextPaint();
	    textPaint.setTypeface(myfont);
	    textPaint.setStyle(Paint.Style.FILL);
	    textPaint.setTextSize(30);
	    textPaint.setColor(Color.BLACK);
	    textPaint.setTextAlign(Align.LEFT);
	    textPaint.setAntiAlias(true);
	    textPaint.setSubpixelText(true);

	    StaticLayout sl = new StaticLayout(textToDisplay, textPaint, ((int)90*myCanvas.getWidth())/100, Layout.Alignment.ALIGN_NORMAL, 1, 1, false);

	    myCanvas.save();
	    myCanvas.translate(55, 15);
	    sl.draw(myCanvas);
	    myCanvas.restore();
	    
	    return mutableBitmap;
	}
    	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		Log.i(ShoppingListWidgetProvider.TAG, "onUpdate called");

		final ContentResolver r = context.getContentResolver();
		if (sDataObserver == null) {
			final AppWidgetManager mgr = AppWidgetManager.getInstance(context);
			final ComponentName cn = new ComponentName(context, ShoppingListWidgetProvider.class);
			sDataObserver = new ShoppingListDataProviderObserver(mgr, cn, sWorkerQueue);
			r.registerContentObserver(ShoppingListDataProvider.CONTENT_URI, true, sDataObserver);
			Log.i(ShoppingListWidgetProvider.TAG, "onUpdate: Registered data observer");
		}

		// Update each of the widgets with the remote adapter
		for (int i = 0; i < appWidgetIds.length; ++i) {
			
			Log.i(ShoppingListWidgetProvider.TAG, "onUpdate: recreating RemoteViews for widgetId " + Integer.toString(i));

			// Specify the service to provide data for the collection widget.  Note that we need to
			// embed the appWidgetId via the data otherwise it will be ignored.
			final Intent intent = new Intent(context, ShoppingListWidgetService.class);
			intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
			intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
			final RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_layout);
			rv.setRemoteAdapter(appWidgetIds[i], R.id.shopping_list, intent);          

			// Set the empty view to be displayed if the collection is empty.  It must be a sibling
			// view of the collection view.
			rv.setEmptyView(R.id.shopping_list, R.id.empty_view);
			
			
			rv.setImageViewBitmap(R.id.textShoppingTitle, drawTextOnList(context, "Courses", R.drawable.paperpad_top));
			rv.setImageViewBitmap(R.id.footer, drawTextOnList(context, "", R.drawable.paperpad_bottom));
			

			final Intent onClickIntent = new Intent(context, ShoppingListWidgetProvider.class);
			onClickIntent.setAction(ShoppingListWidgetProvider.CLICK_ACTION);
			onClickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetIds[i]);
			onClickIntent.setData(Uri.parse(onClickIntent.toUri(Intent.URI_INTENT_SCHEME)));
			final PendingIntent onClickPendingIntent = PendingIntent.getBroadcast(context, 0, onClickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
			rv.setPendingIntentTemplate(R.id.shopping_list, onClickPendingIntent);

			// Bind the click intent for the clean list button on the widget
			final Intent cleanListIntent = new Intent(context, CleanListMenuActivity.class);
			cleanListIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			final PendingIntent cleanListPendingIntent = PendingIntent.getActivity(context, 0, cleanListIntent, PendingIntent.FLAG_UPDATE_CURRENT);
			rv.setOnClickPendingIntent(R.id.cleanList, cleanListPendingIntent);

			// Bind the click intent for the addItem list button on the widget
			final Intent addItemIntent = new Intent(context, AddItemMenuActivity.class);
			addItemIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			final PendingIntent addItemPendingIntent = PendingIntent.getActivity(context, 0, addItemIntent, PendingIntent.FLAG_UPDATE_CURRENT);
			rv.setOnClickPendingIntent(R.id.addItem, addItemPendingIntent);
			
            // Bind the click intent for the refresh button on the widget
            final Intent reloadIntent = new Intent(context, ShoppingListWidgetProvider.class);
            reloadIntent.setAction(ShoppingListWidgetProvider.RELOAD_ACTION);
            final PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(context, 0, reloadIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            rv.setOnClickPendingIntent(R.id.reloadList, refreshPendingIntent);
            
            // Show either the reload button of the spinning progress icon, depending of the current state 
            rv.setViewVisibility(R.id.reloadList, progressBarEnabled ? View.GONE : View.VISIBLE);
            rv.setViewVisibility(R.id.loadingProgress, progressBarEnabled ? View.VISIBLE : View.GONE);
                       
            // Update the widget with this newly built RemoveViews
			appWidgetManager.updateAppWidget(appWidgetIds[i], rv);
		}
		super.onUpdate(context, appWidgetManager, appWidgetIds);
	}
}