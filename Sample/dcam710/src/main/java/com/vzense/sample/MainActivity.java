package com.vzense.sample;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.vzense.sdk.ICameraConnectListener;
import com.vzense.sdk.IFrameCallback;
import com.vzense.sdk.PsFrame;
import com.vzense.sdk.PsFrame.WorkMode;
import com.vzense.sdk.PsParameter;
import com.vzense.sdk.dcam710.PsCamera;

import static com.vzense.sdk.PsFrame.PixelFormat.PixelFormatGray16;

public class MainActivity extends Activity {
	private static final boolean DEBUG = true;
	private static final String TAG = "Activity";
	private final Object mSync = new Object();
	private PsCamera mVzenseCamera;
	private FrameCallback mFrameCallback = null;
	private AutoGLPreviewView mPreviewView;
	private Spinner sp_mView = null;
	private Spinner sp_mData = null;
	private Spinner sp_mResolution = null;
	private Spinner sp_mPara = null;
	private EditText mEditPara = null;
	private CheckBox ck_MapRgb;
	private CheckBox ck_MapDepth;
	boolean showDepth = true;
	boolean showIr = false;
	boolean showRgb = false;
	boolean isSpDataFirst = true;
	boolean isSpViewFirst = true;
	boolean isSpResolutionFirst = true;
	boolean isSpParaFirst = true;
	//CameraParameter mPara;
	Bitmap mShowBitmap = null;
	Bitmap mBmpDepth = Bitmap.createBitmap(480, 640, Bitmap.Config.ARGB_8888);
	Bitmap mBmpIr = Bitmap.createBitmap(480, 640, Bitmap.Config.ARGB_8888);
	Bitmap mBmpRgb = Bitmap.createBitmap(480, 640, Bitmap.Config.ARGB_8888);
	private int resolutionIndex = 3;
	private int currentParaType = 0;
	private WorkMode workMode = WorkMode.WORK_MODE_DEPTH30_RGB30;
	String[] viewType_sp = { "DEPTH","IR", "RGB"};
	String[] resolutionString_sp = { "640*360", "640*480", "1280*720","1920*1080"};
	String[] paramsType_sp = { "DepthRange","PulseCount", "Threshold"};
	String[] workMode_sp = { "Depth30_RGB30","IR30_RGB30","Depth30_IR30", "Depth15_IR15_RGB30", "WDRDepth30_RGB30"};

	private static String[] PERMISSIONS_STORAGE = {
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"
    };
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        String android_version = android.os.Build.VERSION.RELEASE;
        int version = Integer.parseInt(android_version.substring(0,1));
        if(version >= 6 ) {
            int permission = this.checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE");
            if (permission != PackageManager.PERMISSION_GRANTED) {
                this.requestPermissions(
                        PERMISSIONS_STORAGE,
                        REQUEST_EXTERNAL_STORAGE
                );
            }
        }
		setContentView(R.layout.activity_main);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		mPreviewView = findViewById(R.id.glv_main);
		mPreviewView.setPreviewSize(480, 640);


		ck_MapRgb = (CheckBox) findViewById(R.id.map_rgb);
		ck_MapRgb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mVzenseCamera.setMapperEnabledDepthToRGB(isChecked);
			}
		});

		ck_MapDepth = (CheckBox) findViewById(R.id.map_depth);
		ck_MapDepth.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				mVzenseCamera.setMapperEnabledRGBToDepth(isChecked);
			}
		});

		mEditPara= (EditText) findViewById(R.id.paraValue);
		mEditPara.setInputType( InputType.TYPE_CLASS_NUMBER);
		Button bSetPara = (Button) findViewById(R.id.setPara);
		bSetPara.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(mVzenseCamera != null){
					if(mEditPara != null){
						String paraValue= mEditPara.getText().toString();
						if(paraValue.equals(""))
						{
							Toast.makeText(MainActivity.this, "please input the value!",Toast.LENGTH_SHORT).show();
							return;
						}
						int parameterValue = Integer.parseInt(paraValue);
						if(0 == currentParaType) {
							//set depthrange
							if(parameterValue >= 0 && parameterValue <= 8){
								mVzenseCamera.setDepthRange(parameterValue);
							} else{
								Toast.makeText(MainActivity.this, "depth range is 0-8",Toast.LENGTH_SHORT).show();
							}
						}else if(1 == currentParaType){
							//set Pulsecount
							if(parameterValue >= 0 && parameterValue <= 600){
								mVzenseCamera.setPulseCount(parameterValue);
							} else{
								Toast.makeText(MainActivity.this, "pulseCount range must 0~600",Toast.LENGTH_SHORT).show();
							}
						}else if(2 == currentParaType){
							//set Threshold
							if(parameterValue >= 0 && parameterValue <= 200){
								mVzenseCamera.setBGThreshold(parameterValue);
							} else{
								Toast.makeText(MainActivity.this, "threshold range must be 0~200",Toast.LENGTH_SHORT).show();
							}
						}
					}

				}
			}
		});

		sp_mPara = (Spinner) findViewById(R.id.spinner_paratype);
		ArrayAdapter<String> mAdapterPara=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, paramsType_sp);
		sp_mPara.setAdapter(mAdapterPara);
		sp_mPara.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
									   int position, long id) {
				if(isSpParaFirst){
					isSpParaFirst = false;
					return;
				}
				String str=parent.getItemAtPosition(position).toString();
				if(str.equals(paramsType_sp[0])){
					currentParaType = 0;
				}else if(str.equals(paramsType_sp[1])) {
					currentParaType = 1;
				}else if(str.equals(paramsType_sp[2])){
					currentParaType = 2;
				}
			}
			@Override
			public void onNothingSelected(AdapterView<?> parent) {

			}
		});

		sp_mView = (Spinner) findViewById(R.id.spinner_viewtype);
        ArrayAdapter<String> mAdapterView=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, viewType_sp);
		sp_mView.setAdapter(mAdapterView);
		sp_mView.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
				if(isSpViewFirst){
					isSpViewFirst = false;
					return;
				}
                String str=parent.getItemAtPosition(position).toString();
                if(str.equals(viewType_sp[0])){
					if(sp_mResolution != null){
						sp_mResolution.setEnabled(false);
					}
                    showDepth = true;
                    showIr = false;
                    showRgb = false;
                }else if(str.equals(viewType_sp[1])) {
					if(sp_mResolution != null){
						sp_mResolution.setEnabled(false);
					}
                    showIr = true;
                    showDepth = false;
                    showRgb = false;
                }else if(str.equals(viewType_sp[2])){
					if(sp_mResolution != null){
						sp_mResolution.setEnabled(true);
					}
                    showIr = false;
                    showDepth = false;
                    showRgb = true;
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

		sp_mResolution = (Spinner) findViewById(R.id.spinner_resolution);
		ArrayAdapter<String> mAdapterResolution=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, resolutionString_sp);
		sp_mResolution.setAdapter(mAdapterResolution);
		sp_mResolution.setEnabled(false);

		sp_mResolution.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
									   int position, long id) {
				if (isSpResolutionFirst) {
					isSpResolutionFirst = false;
					return;
				}
				String str = parent.getItemAtPosition(position).toString();
				if (str.equals(resolutionString_sp[0])) {
					mVzenseCamera.setRgbResolution(3);
					resolutionIndex = 3;
				} else if (str.equals(resolutionString_sp[1])) {
					mVzenseCamera.setRgbResolution(2);
					resolutionIndex = 2;
				} else if (str.equals(resolutionString_sp[2])) {
					mVzenseCamera.setRgbResolution(1);
					resolutionIndex = 1;
				} else if (str.equals(resolutionString_sp[3])) {
					mVzenseCamera.setRgbResolution(0);
					resolutionIndex = 0;
				}
			}
			@Override
			public void onNothingSelected(AdapterView<?> parent) {

			}
		});

		sp_mData = (Spinner) findViewById(R.id.spinner_datatype);
        ArrayAdapter<String> mAdapterData=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, workMode_sp);
		sp_mData.setAdapter(mAdapterData);
		sp_mData.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
            	if(isSpDataFirst){
					isSpDataFirst = false;
            		return;
				}

                String str = parent.getItemAtPosition(position).toString();
                if(str.equals(workMode_sp[0])){
                    if(mVzenseCamera != null){
                        workMode = WorkMode.WORK_MODE_DEPTH30_RGB30;
                        mVzenseCamera.setWorkMode(workMode.ordinal());
					
                        if(ck_MapRgb != null){
							ck_MapRgb.setClickable(true);
                        }
						if(ck_MapDepth != null){
							ck_MapDepth.setClickable(true);
						}
                    }
                }else if(str.equals(workMode_sp[1])) {
					workMode = WorkMode.WORK_MODE_IR30_RGB30;
                    mVzenseCamera.setWorkMode(workMode.ordinal());
                    if(ck_MapRgb != null){
						ck_MapRgb.setClickable(false);
                    }
					if(ck_MapDepth != null){
						ck_MapDepth.setClickable(false);
					}
                }else if(str.equals(workMode_sp[2])){
					if(mVzenseCamera != null) {
						workMode = WorkMode.WORK_MODE_DEPTH30_IR30;
						mVzenseCamera.setWorkMode(workMode.ordinal());
						if (ck_MapRgb != null) {
							ck_MapRgb.setClickable(false);
						}
						if(ck_MapDepth != null){
							ck_MapDepth.setClickable(false);
						}
					}
                }else if(str.equals(workMode_sp[3])){
					if(mVzenseCamera != null) {
						workMode = WorkMode.WORK_MODE_DEPTH15_IR15_RGB30;
						mVzenseCamera.setWorkMode(workMode.ordinal());
					}
                    if(ck_MapRgb != null){
						ck_MapRgb.setClickable(true);
                    }
					if(ck_MapDepth != null){
						ck_MapDepth.setClickable(true);
					}
                }else if(str.equals(workMode_sp[4])){
					if(mVzenseCamera != null) {
						workMode = WorkMode.WORK_MODE_WDRDEPTH30_RGB30;
						mVzenseCamera.setWorkMode(workMode.ordinal());
						PsParameter.PsWDRMode  mode = new PsParameter.PsWDRMode();
						mode.totalRange = 2;
						mode.range1 = 0;
						mode.range1Count = 1;
						mode.range2 = 2;
						mode.range2Count = 1;
						PsParameter.PsThreshold threshold = new PsParameter.PsThreshold();
						threshold.Threshold1 = 1000;
						threshold.Threshold2 = 1800;
						mVzenseCamera.setWDRMode(mode);
						mVzenseCamera.setWDRThreshold(threshold);
						
						PsParameter.PsWDRMode  mode2 = new PsParameter.PsWDRMode();
						mVzenseCamera.getWDRMode(mode2);
						Log.e(TAG,"wdr mode	total:"+ mode2.totalRange + ","+mode2.range1+","+mode2.range1Count+","+mode2.range2Count+","+mode2.range2Count+",");
					}
                    if(ck_MapRgb != null){
						ck_MapRgb.setClickable(true);
                    }
					if(ck_MapDepth != null){
						ck_MapDepth.setClickable(true);
					}
                }
				mVzenseCamera.setMapperEnabledDepthToRGB(false);
				mVzenseCamera.setMapperEnabledRGBToDepth(false);
				mVzenseCamera.setMapperEnabledRGBToIR(false);
				ck_MapRgb.setChecked(false);
				ck_MapDepth.setChecked(false);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mVzenseCamera = new PsCamera();
		if (mVzenseCamera != null) {
			mVzenseCamera.init(this, mOnCameraConnectListener);
		}
		mFrameCallback = new FrameCallback();
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (DEBUG) Log.v(TAG, "onStart:");

		if (mVzenseCamera != null) {
			mVzenseCamera.setFrameCallback(mFrameCallback);
			mVzenseCamera.setBGThreshold(20);   //set threshold value  0-200   0:close
			mVzenseCamera.setDepthRange(0);   //set depth range value  only support 0-8
			workMode = WorkMode.WORK_MODE_DEPTH30_RGB30;
			mVzenseCamera.setMapperEnabledDepthToRGB(false);
			mVzenseCamera.setMapperEnabledRGBToDepth(false);
			mVzenseCamera.setMapperEnabledRGBToIR(false);
			ck_MapRgb.setChecked(false);
			ck_MapDepth.setChecked(false);				
			mVzenseCamera.setWorkMode(workMode.ordinal());
			sp_mData.setSelection(0);
			mVzenseCamera.setRgbResolution(3);//0 :1920x1080 1:1280x720 2:640x480 3:640x360
			resolutionIndex = 3;
			mVzenseCamera.start(this);
		}
	}

	@Override
	protected void onStop() {
		if (DEBUG) Log.v(TAG, "onStop:");
		synchronized (mSync) {
			if (mVzenseCamera != null) {
				mVzenseCamera.stop();
			}
		}
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		if (DEBUG) Log.v(TAG, "onDestroy:");
		if (mVzenseCamera != null) {
			mVzenseCamera.destroy();
			mVzenseCamera = null;
		}

		if(mBmpDepth != null){
			mBmpDepth.recycle();
			mBmpDepth = null;
		}
		if(mBmpIr != null){
			mBmpIr.recycle();
			mBmpIr = null;
		}
		if(mBmpRgb != null){
			mBmpRgb.recycle();
			mBmpRgb = null;
		}
		super.onDestroy();
		System.exit(0);
	}

	private final ICameraConnectListener mOnCameraConnectListener = new ICameraConnectListener() {
		@Override
		public void onAttach() {
			if (DEBUG) Log.e(TAG, "onAttach:");
		}

		@Override
		public void onConnect(int connectStatus) {
			if (DEBUG) Log.e(TAG, "onConnect" + connectStatus);
		}

		@Override
		public void onDisconnect() {
			if (DEBUG) Log.e(TAG, "onDisconnect");
		}

		@Override
		public void onDettach() {
			if (DEBUG) Log.e(TAG, "onDettach");
		}

		@Override
		public void onCancel() {
			if (DEBUG) Log.e(TAG, "onCancel");
		}
		
		@Override
		public void onError() {
			if (DEBUG) Log.e(TAG, "onError");
		}
	};

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Bundle mainBundle = new Bundle();

		mainBundle.putInt("frameShow", sp_mView.getSelectedItemPosition());
		mainBundle.putInt("dataMode", sp_mData.getSelectedItemPosition());
		mainBundle.putInt("resolution", sp_mResolution.getSelectedItemPosition());
		mainBundle.putBoolean("mapRgb", ck_MapRgb.isChecked());
		mainBundle.putBoolean("mapDepth", ck_MapDepth.isChecked());
		outState.putBundle("main", mainBundle);
	}

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			if (0 == msg.what){
				if (mShowBitmap.getWidth() != mPreviewView.getPreviewWidth() || mShowBitmap.getHeight() != mPreviewView.getPreviewHeight()){
					mPreviewView.setPreviewSize(mShowBitmap.getWidth(), mShowBitmap.getHeight());
				}
				mPreviewView.draw(mShowBitmap);
			}
		}
	};

	public class FrameCallback implements IFrameCallback {
		@Override
		public void onFrame(PsFrame DepthFrame,PsFrame IrFrame,PsFrame RgbFrame){
			if(showDepth && null != DepthFrame) {
				int center_L = DepthFrame.frameData.get(DepthFrame.width * DepthFrame.height + DepthFrame.width);
				int center_H = DepthFrame.frameData.get(DepthFrame.width * DepthFrame.height + DepthFrame.width + 1);
				int center = (int)((center_H << 8) | (center_L& 0xFF));
				if (mBmpDepth.getWidth() != DepthFrame.width || mBmpDepth.getHeight() != DepthFrame.height){
					mBmpDepth = Bitmap.createBitmap(DepthFrame.width, DepthFrame.height, Bitmap.Config.ARGB_8888);
				}
				mVzenseCamera.Y16ToRgba_bf(DepthFrame.frameData, mBmpDepth,DepthFrame.width,DepthFrame.height, 1500);
				Canvas canvas = new Canvas(mBmpDepth);
				Paint paint = new Paint();
				paint.setAntiAlias(true);
				paint.setDither(true);
				paint.setTextSize(DepthFrame.width / 24);
				paint.setColor(Color.parseColor("#ff0000"));
				canvas.drawText(String.valueOf(center), (mBmpDepth.getWidth() / 2) - 20, (mBmpDepth.getHeight() / 2 - 10), paint);
				canvas.drawText(".", (mBmpDepth.getWidth() / 2), (mBmpDepth.getHeight() / 2), paint);
				paint.setColor(Color.parseColor("#00ff00"));
				canvas.drawText(DepthFrame.width + "x" + DepthFrame.height, DepthFrame.width / 16, DepthFrame.height / 10, paint);
				mShowBitmap = mBmpDepth;
			}
			if(showIr && null != IrFrame){

				if (mBmpIr.getWidth() != IrFrame.width || mBmpIr.getHeight() != IrFrame.height) {
					mBmpIr = Bitmap.createBitmap(IrFrame.width, IrFrame.height, Bitmap.Config.ARGB_8888);
				}
				
				if (PixelFormatGray16.ordinal() == IrFrame.pixelFormat){
					mVzenseCamera.Y16ToRgba_bf(IrFrame.frameData, mBmpIr,IrFrame.width,IrFrame.height, 3840);
				}else{
					mVzenseCamera.Y8ToRgba_bf(IrFrame.frameData, mBmpIr,IrFrame.width,IrFrame.height);
				}
				Canvas canvas = new Canvas(mBmpIr);
				Paint paint = new Paint();
				paint.setAntiAlias(true);
				paint.setDither(true);
				paint.setTextSize(IrFrame.width / 24);
				paint.setColor(Color.parseColor("#00ff00"));
				canvas.drawText(IrFrame.width + "x" + IrFrame.height, IrFrame.width / 16, IrFrame.height / 10, paint);
				mShowBitmap = mBmpIr;

			}
			if(showRgb && null != RgbFrame){

				if (mBmpRgb.getWidth() != RgbFrame.width || mBmpRgb.getHeight() != RgbFrame.height) {
					mBmpRgb = Bitmap.createBitmap(RgbFrame.width, RgbFrame.height, Bitmap.Config.ARGB_8888);
				}
				mVzenseCamera.RgbToRgba_bf(RgbFrame.frameData,mBmpRgb,RgbFrame.width,RgbFrame.height, RgbFrame.pixelFormat);
				Canvas canvas = new Canvas(mBmpRgb);
				Paint paint = new Paint();
				paint.setAntiAlias(true);
				paint.setDither(true);
				paint.setTextSize(RgbFrame.width / 24);
				paint.setColor(Color.parseColor("#00ff00"));
				canvas.drawText(RgbFrame.width + "x" + RgbFrame.height, RgbFrame.width / 16, RgbFrame.height / 10, paint);
				mShowBitmap = mBmpRgb;
			}

			if (null != mShowBitmap){
				Message fwMsg = mHandler.obtainMessage();
				fwMsg.what = 0;
				mHandler.sendMessage(fwMsg);
			}

		}
	}
}
