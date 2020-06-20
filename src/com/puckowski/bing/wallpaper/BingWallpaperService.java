package com.puckowski.bing.wallpaper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import android.util.DisplayMetrics;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.WindowManager;

public class BingWallpaperService extends WallpaperService 
{
	@Override
	public Engine onCreateEngine() 
	{
		return new BingWallpaperEngine();
	}   

	private class BingWallpaperEngine extends Engine 
		implements SharedPreferences.OnSharedPreferenceChangeListener 
	{	
		private final String mResourceURL = "http://www.bing.com/hp?&MKT=";
		
		private final Handler mHandler = new Handler();
		private final Runnable mRunnable = new Runnable() 
		{
			public void run() 
			{	
				drawWallpaper();
				
				checkForUpdate();
			}
		};
		
		private Context mContext;
		private Paint mPaint = new Paint();
		
		private boolean mVisible = true;
	
		private int mReferenceDay;
		private int mReferenceX;
		
		private Bitmap mReferenceBitmap;
		private int mBitmapWidth;
						
		private int mDisplayWidth;
		private int mDisplayHeight;
		
		private boolean mWifiDownloadOnly;				
					
		public BingWallpaperEngine() 
		{	
			mContext = getApplicationContext();
			
			WindowManager windowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
			
			DisplayMetrics mDisplayMetrics = new DisplayMetrics();
	        windowManager.getDefaultDisplay().getMetrics(mDisplayMetrics);
			
	        mDisplayWidth = mDisplayMetrics.widthPixels;
			mDisplayHeight = mDisplayMetrics.heightPixels;
			
			mReferenceDay = -1;

			SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(BingWallpaperService.this);
			
			sharedPreferences.registerOnSharedPreferenceChangeListener(this);
			onSharedPreferenceChanged(sharedPreferences, null);
			
			mHandler.post(mRunnable);
		}
		
		public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
		{        	    	
			mWifiDownloadOnly = prefs.getBoolean("wifidownloadonly", false);	
		}
		
		@Override
        public void onCreate(SurfaceHolder surfaceHolder) 
		{
            super.onCreate(surfaceHolder);
            
            setTouchEventsEnabled(true);
        }

		@Override
		public void onDestroy() 
		{
			super.onDestroy();
			
			mHandler.removeCallbacks(mRunnable);
		}
		 
		@Override
		public void onVisibilityChanged(boolean visible) 
		{
			mVisible = visible;
			
			if(mVisible) 
			{
				drawWallpaper();
			}
		    else 
		    {
				mHandler.removeCallbacks(mRunnable);
		    }
		}

		@Override
		public void onSurfaceDestroyed(SurfaceHolder mSurfaceHolder) 
		{
			super.onSurfaceDestroyed(mSurfaceHolder);
			
			mVisible = false;
			mHandler.removeCallbacks(mRunnable);
		}

		@Override
        public void onSurfaceCreated(SurfaceHolder holder) 
		{
            super.onSurfaceCreated(holder);
        }
		
		@Override
		public void onSurfaceChanged (SurfaceHolder mSurfaceHolder, int format, int width, int height)
		{
			super.onSurfaceChanged(mSurfaceHolder, format, width, height);
			
			drawWallpaper();
		}

		@Override
		public void onOffsetsChanged (float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, 
				int xPixelOffset, int yPixelOffset)
		{
			if(mReferenceBitmap != null)
			{
				mReferenceX = (int)((mDisplayWidth - mBitmapWidth) * (0.00 + xOffset));
			}
			
			drawWallpaper();
		}
		
		@Override
	    public void onTouchEvent(MotionEvent event) 
		{
	        super.onTouchEvent(event);
	    }

		public void drawWallpaper()
		{
			final SurfaceHolder mSurfaceHolder = getSurfaceHolder();
			Canvas mCanvas = null;

			try 
			{		
				mCanvas = mSurfaceHolder.lockCanvas();	

				if(mCanvas != null) 
				{
					if(mReferenceBitmap == null) 
					{
						try 
						{
							String bingSource = getBingSource();
							URL mWallpaperURL = getWallpaperURL(bingSource);

							mReferenceBitmap = loadBitmapFromURL(mWallpaperURL);
						} 
						catch(Exception exception) 
						{
						}
					} 

					if(mReferenceBitmap != null) 
					{	
						mCanvas.drawBitmap(mReferenceBitmap, mReferenceX, 0, mPaint);		
					}
				}
			}
			finally
			{
				if(mCanvas != null)
				{
					mSurfaceHolder.unlockCanvasAndPost(mCanvas);
				}
			}

			mHandler.removeCallbacks(mRunnable);

			if(mVisible) 
			{	
				mHandler.postDelayed(mRunnable, 1000);
			}
		}

		private void checkForUpdate()
		{
			Calendar mCalendar = Calendar.getInstance();
			int mCurrentDay = mCalendar.get(Calendar.DAY_OF_MONTH);

			if(mCurrentDay != mReferenceDay) 
			{
				mReferenceBitmap = null;
				
				mReferenceDay = mCurrentDay;
			}
		}
		
		public String getBingSource() 
		{
			if(mWifiDownloadOnly == true) 
			{
				if(isWifiEnabled() == true)
				{
					;
				}
				else
				{
					return null;
				}
			}

			StringBuilder bingSource = new StringBuilder(25000);
			//String bingSource = "";

			try 
			{				
				HttpClient httpClient = new DefaultHttpClient();
			    HttpContext localContext = new BasicHttpContext();
			    HttpGet httpGet = new HttpGet(mResourceURL + "en-US");
			    HttpResponse httpResponse = httpClient.execute(httpGet, localContext);

			    BufferedReader bufferedReader = new BufferedReader(
			        new InputStreamReader(
			            httpResponse.getEntity().getContent()
			        )
			    );

			    String nextLine = null;
			    while ((nextLine = bufferedReader.readLine()) != null) 
			    {
			        bingSource.append(nextLine); // + "\n";
			    }
			} 
			catch(Exception exception) 
			{
			}  
		    
			return bingSource.toString();
		}
		
		private URL getWallpaperURL(String source) 
		{
			URL url = null;
			
            while(true) 
            {
                if(source.contains("url:") != true || source == null)
                {
                    break;
                }  

                int backgroundImageTag = source.indexOf("url:");
                
                try 
                {
                    int indexOpen = source.indexOf("'", backgroundImageTag);
                    int indexClose = source.indexOf("'", (indexOpen +1));

                    String imageDirectory = source.substring((indexOpen + 1), indexClose);
                    source = source.substring((indexClose + 1), source.length());

                    url = new URL("http://www.bing.com" + imageDirectory);
                    
                    break;
                }
                catch(Exception exception) 
                {
                    break;
                }
            }
            
            return url;
		}

		private Bitmap loadBitmapFromURL(URL url) 
		{
			if(mWifiDownloadOnly == true && isWifiEnabled() == false)
			{
				return null;
			}
			
		    try 
		    {
		        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		       
		        connection.setDoInput(true);
		        connection.connect();
		        
		        InputStream input = connection.getInputStream();
		        
		        Bitmap myBitmap = BitmapFactory.decodeStream(input);
		        int mWidth = myBitmap.getWidth();
		        int mHeight = myBitmap.getHeight();
		        
		        mBitmapWidth = mWidth;

		        if(mHeight < mDisplayHeight)
		        {
		        	while(mHeight < mDisplayHeight) 
		        	{
		        		mHeight++;
		        		mWidth++;
		        	}
		        }
		        else
		        {
		        	while(mHeight > mDisplayHeight) 
		        	{
		        		mHeight--;
		        		mWidth--;
		        	}
		        }
		        
		        Bitmap mScaledBitmap = Bitmap.createScaledBitmap(myBitmap, mWidth, mHeight, true);
		        
		        return mScaledBitmap;
		    } 
		    catch(IOException IOException) 
		    {
		    }
		    
		    return null;
		}
		
		public boolean isWifiEnabled() 
		{
			try 
			{
				WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
			
				return wifiManager.isWifiEnabled();
			} 
			catch(Exception exception) 
			{
				return false;
			}
	    }	
	}
}

