package com.gbbtbb.shoppinglistwidget;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

public class ShoppingListDataProvider extends ContentProvider {

	public static final Uri CONTENT_URI = Uri.parse("content://com.gbbtbb.shoppinglistwidget.provider");
	public static class Columns {
		public static final String ID = "_id";
		public static final String ITEM = "item";
		public static final String QUANTITY = "quantity";
	}

	@Override
	public boolean onCreate() {
		return true;
	}

	@Override
	public synchronized Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		assert(uri.getPathSegments().isEmpty());

		final MatrixCursor c = new MatrixCursor(new String[]{ Columns.ID, Columns.ITEM, Columns.QUANTITY });       

		String result = httpRequest("http://88.181.199.137:8081/shoppinglist.php", null);

		// Parse the received JSON data
		try {
			JSONObject jdata = new JSONObject(result);
			JSONArray jArray = jdata.getJSONArray("items");

			for(int i=0;i<jArray.length();i++){
				JSONObject jobj = jArray.getJSONObject(i);
				String item = jobj.getString(Columns.ITEM);
				String quantity = jobj.getString(Columns.QUANTITY);
				c.addRow(new Object[]{i, item, quantity });
			}

		} catch(JSONException e){
			Log.e(ShoppingListWidgetProvider.TAG, "Error parsing data "+e.toString());
		}

		// Add a few empty items so that the list looks good even with no item present
		for(int i=0;i<25;i++){
			c.addRow(new Object[]{i, "", ""});
		}

		return c;
	}

	@Override
	public String getType(Uri uri) {
		return "vnd.android.cursor.dir/com.gbbtbb.shoppinglistitems";
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {

		String newItemName = (String)values.get(Columns.ITEM);

		ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		nameValuePairs.add(new BasicNameValuePair("newitem",newItemName));

		httpRequest("http://88.181.199.137:8081/shoppinglist_insert.php", nameValuePairs);

		Log.i(ShoppingListWidgetProvider.TAG, "ShoppingListDataProvider: requested to add new item: " + newItemName);
		return null;
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		Log.i(ShoppingListWidgetProvider.TAG, "ShoppingListDataProvider: request for deleting " + selection + selectionArgs[0]);

		ArrayList<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		nameValuePairs.add(new BasicNameValuePair("whereClause",selection));
		httpRequest("http://88.181.199.137:8081/shoppinglist_delete.php", nameValuePairs);
		return 0;
	}

	private String httpRequest(String url, ArrayList<NameValuePair> nameValuePairs) {
		String result = "";
		InputStream is = null;

		// Do remote HTTP POST request
		try {
			HttpParams httpParameters = new BasicHttpParams();

			// Set the timeout in milliseconds until a connection is established.
			// The default value is zero, that means the timeout is not used. 
			int timeoutConnection = 30000;
			HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);

			// Set the default socket timeout (SO_TIMEOUT) 
			// in milliseconds which is the timeout for waiting for data.
			int timeoutSocket = 30000;
			HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);

			HttpClient httpclient = new DefaultHttpClient();
			HttpPost httppost = new HttpPost(url);
			if (nameValuePairs != null) {
				httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs, HTTP.UTF_8));
			}
			HttpResponse response = httpclient.execute(httppost);
			HttpEntity entity = response.getEntity();
			is = entity.getContent();
		} catch(Exception e) {
			Log.e(ShoppingListWidgetProvider.TAG, "httpRequest: Error in http connection "+e.toString());
		}

		// Convert response to string
		try {
			// Encode the (iso-8859-1) input as UTF-8 so that it is properly processed on the server side
			BufferedReader reader = new BufferedReader(new InputStreamReader(is,"UTF-8"),8);
			StringBuilder sb = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
			is.close();
			result=sb.toString();
			Log.i(ShoppingListWidgetProvider.TAG, "httpRequest: received "+ result);
		} catch(Exception e){
			Log.e(ShoppingListWidgetProvider.TAG, "httpRequest: Error converting result "+e.toString());
		}

		return result;
	}

	@Override
	public synchronized int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		assert(uri.getPathSegments().size() == 1);

		// not implemented right now.

		getContext().getContentResolver().notifyChange(uri, null);
		return 1;
	}

}