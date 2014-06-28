package com.example.aruyo;

import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

public class CamPreView extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback, Camera.AutoFocusCallback {
	private static final String TAG = "CamPreView";
	private OnCodeScannedListener mCaller;
	private Camera mCamera;
	private SurfaceHolder mHolder;
	private List<Size> prevSizeList;
	private boolean isScanning = false;
	private boolean isFocusDone = false;

    public interface OnCodeScannedListener{
    	void onCodeScanned(String scannedCode);
    }

	public CamPreView(Context context, AttributeSet attrs) {
		super(context, attrs);
		if(!isInEditMode()){
			mCamera = null;
			int camNo = Camera.getNumberOfCameras();
			CameraInfo cameraInfo = new CameraInfo();
			for(int i = 0; i < camNo; ++i){
				Camera.getCameraInfo(i, cameraInfo);
				if(cameraInfo.facing == CameraInfo.CAMERA_FACING_BACK){
					mCamera = Camera.open(i);
					break;
				}
			}
			mHolder = getHolder();
			mHolder.addCallback(this);
//			mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}
	}
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		try {
			if(mCamera!=null){
				mCamera.setPreviewDisplay(mHolder);
				mCamera.setDisplayOrientation(90);
				Parameters camParams = mCamera.getParameters();
				prevSizeList = camParams.getSupportedPreviewSizes();
			}
		} catch (IOException e) {
			Log.v(TAG, "setPreviewDisplay() failed");
		}
	}
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		Log.v(TAG, "surfaceChanged, w:" + width + ", h:" + height);
		if(mHolder.getSurface() == null) return;
		if(mCamera == null) return;
		mCamera.stopPreview();
		Parameters camParams = mCamera.getParameters();
		int maxW = -1;
		int maxH = -1;
		int maxSize = 0;
		for(Size prevSize : prevSizeList){
			if(prevSize.width <= width && prevSize.height <= height){
				int compSize = prevSize.width * prevSize.height;
				if(compSize > maxSize){
					maxSize = compSize;
					maxH = prevSize.height;
					maxW = prevSize.width;
				}
			}
		}
		if(maxW > 0 && maxH > 0){
			Log.v(TAG, "set preview size, w:" + maxW + ", h:" + maxH);
			camParams.setPreviewSize(maxW, maxH);
		}
		mCamera.startPreview();
	}
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.v(TAG, "surfaceDestroyed");
		if(mCamera != null){
			mCamera.setPreviewCallback(null);
			mCamera.stopPreview();
			mCamera.cancelAutoFocus();
			mCamera.release();
			mCamera = null;
		}
	}
	@Override
	public void onAutoFocus(boolean success, Camera camera) {
		Log.v(TAG, "onAutoFocus:" + (success?"ok":"ng"));
		if(success){
			isFocusDone = true;
			mCamera.cancelAutoFocus();
			camera.autoFocus(null);
			return;
		}
	}
	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		if(!isFocusDone) return;
		Log.v(TAG, "onPreviewFrame");
		Camera.Parameters camParams = camera.getParameters();
		Size prevSize = camParams.getPreviewSize();
		camera.setPreviewCallback(null);
		isScanning = false;
		isFocusDone = false;
		PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(data, 
			prevSize.width,prevSize.height,
			0, 0, prevSize.width, prevSize.height, false
		);
		if(source != null){
			BinaryBitmap bmp = new BinaryBitmap(new HybridBinarizer(source));
			MultiFormatReader reader = new MultiFormatReader();
			try {
				final boolean decode = true;
				String decodedStr;
				Result rawResult;
				if(decode){
					Map<DecodeHintType, Object>hints = new EnumMap<DecodeHintType, Object>(DecodeHintType.class);
					hints.put(DecodeHintType.CHARACTER_SET, "SJIS");
					rawResult = reader.decode(bmp, hints);
					decodedStr = rawResult.getText();
				}else{
					rawResult = reader.decode(bmp);
					decodedStr = rawResult.getText();
				}
				byte[] resultBytes = rawResult.getRawBytes();
				for(byte b : resultBytes){
					Log.v(TAG, "b:" + (b & 0xff));
				}
				Log.v(TAG, "decodedStr:" + decodedStr);
				mCaller.onCodeScanned(decodedStr);
				return;
			} catch (NotFoundException e) {
				Log.v(TAG, "barcode not found:" + e.getMessage());
				e.printStackTrace();
			}
			mCaller.onCodeScanned("failed scan");
		}
	}
	public void startCapture(OnCodeScannedListener caller){
		mCaller = caller;
		Log.v(TAG, "startCapture");
		if(isScanning){
			Log.v(TAG, "isScanning");
			return;
		}
		isScanning = true;
		mCamera.setPreviewCallback(this);
		mCamera.cancelAutoFocus();
		mCamera.autoFocus(this);
	}
}