package com.inniopia.funnylabs_sdk;

import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.components.containers.NormalizedKeypoint;
import com.google.mediapipe.tasks.vision.facedetector.FaceDetectorResult;
import com.inniopia.funnylabs_sdk.ui.AutoFitSurfaceView;
import com.inniopia.funnylabs_sdk.ui.CommonPopupView;
import com.inniopia.funnylabs_sdk.utils.CameraUtils;
import com.inniopia.funnylabs_sdk.utils.ImageUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

public class MainFragment extends Fragment implements EnhanceFaceDetector.DetectorListener {

    //View Variable
    private PreviewView mCameraView;
    private OverlayView mTrackingOverlayView;
    private ProgressBar mProgressBar;
    private BpmAnalysisViewModel mBpmAnalysisViewModel;
    private CommonPopupView mGuidePopupView;
    private TextView mGuidePopupText;
    private CountDownTimer mCalibrationTimer;
    private TextView mCountdownTextView;
    private AlertDialog mCountdownPopup;

    //Camera2 variable
    private CameraCharacteristics characteristics;
    private CameraManager cameraManager;
    private ImageReader imageReader;
    private HandlerThread cameraThread;
    private HandlerThread imageReaderThread;
    private CameraDevice Camera;
    private String cameraId;
    private CameraCaptureSession cameraCaptureSession;
    private AutoFitSurfaceView autoFitSurfaceView;
    private Handler cameraHandler;
    private Handler imageReaderHandler;


    //Camera Property variable
    private EnhanceFaceDetector faceDetector;
    private ExecutorService mFrontCameraExecutor;
    public Bitmap mOriginalBitmap;

    private int sNthFrame = 0;
    private int mRotation = 0;
    private final Rect FaceBox = new Rect();

    private boolean isFixFaceBox = false;
    private boolean isStopPredict = false;
    private boolean calibrationFinish = false;
    private boolean isFinishAnalysis = false;
    private boolean calibrationTimerStart = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBpmAnalysisViewModel = new BpmAnalysisViewModel(new Application(), requireContext());

        mFrontCameraExecutor = Executors.newSingleThreadExecutor();

        faceDetector = new EnhanceFaceDetector(requireContext(), this);
        faceDetector.setupFaceDetector();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_main, container, false);

        FrameLayout cameraContainer = view.findViewById(R.id.container_surface);
        View surfaceView = LayoutInflater.from(requireContext()).inflate(
                R.layout.layout_surface_container, cameraContainer, false);
        cameraContainer.addView(surfaceView);

        initThread();

        //Camera2
        autoFitSurfaceView = view.findViewById(R.id.view_finder_camera2);

        //Face Detection
        mTrackingOverlayView = view.findViewById(R.id.tracking_overlay);
        mProgressBar = view.findViewById(R.id.progress);

        //Face Guide Popup
        View viewNoDetectionPopup = inflater.inflate(R.layout.layout_detection_popup, container, false);
        mGuidePopupView = new CommonPopupView(requireContext(),viewNoDetectionPopup);
        mGuidePopupText = viewNoDetectionPopup.findViewById(R.id.text_face_popup);
        mGuidePopupText.setText(R.string.face_no_detection);

        initCalibrationView();
        initCalibrationTimer();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requireActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        cameraId = chooseCamera();
        autoFitSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
                Size screenSize = CameraUtils.getDisplaySmartSize(autoFitSurfaceView.getDisplay()).getSize();
                Size previewSize = CameraUtils.getPreviewOutputSize(
                        autoFitSurfaceView.getDisplay()
                        , characteristics
                        , SurfaceHolder.class);
                Config.IMAGE_READER_WIDTH = previewSize.getWidth();
                Config.IMAGE_READER_HEIGHT = previewSize.getHeight();
                autoFitSurfaceView.setAspectRatio(
                        screenSize.getWidth(),
                        screenSize.getHeight()
                );
                Log.d("Jupiter", String.format("Width : %d, Height : %d", previewSize.getWidth(), previewSize.getHeight()));
//                autoFitSurfaceView.getHolder().setFixedSize(previewSize.getWidth(), previewSize.getHeight());
                mTrackingOverlayView.setFullsize(autoFitSurfaceView.getWidth(), autoFitSurfaceView.getHeight());
                view.post(() -> initCamera());
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                holder.getSurface();
            }
        });
    }

    private void initThread(){
        cameraThread = new HandlerThread("CameraThread");
        cameraThread.start();
        cameraHandler = new Handler(cameraThread.getLooper());
        imageReaderThread = new HandlerThread("imageReaderThread");
        imageReaderThread.start();
        imageReaderHandler = new Handler(imageReaderThread.getLooper());
    }
    private void initCamera() {
        openCamera(cameraManager, cameraId, cameraHandler);
        imageReader = ImageReader.newInstance(autoFitSurfaceView.getWidth(), autoFitSurfaceView.getHeight(), Config.PIXEL_FORMAT, Config.IMAGE_BUFFER_SIZE);
        createCaptureSession(Camera, Arrays.asList(autoFitSurfaceView.getHolder().getSurface(), imageReader.getSurface()), cameraHandler);
    }

    private String chooseCamera(){
        cameraManager = (CameraManager) requireContext().getApplicationContext()
                .getSystemService(Context.CAMERA_SERVICE);
        try {
            for(String cameraId : cameraManager.getCameraIdList()){
                characteristics = cameraManager.getCameraCharacteristics(cameraId);
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
                mRotation = (sensorOrientation + rotation + 360) % 360;

                if(map != null){
                    int lens = characteristics.get(CameraCharacteristics.LENS_FACING);
                    if(Config.USE_CAMERA_DIRECTION == Config.CAMERA_DIRECTION_FRONT){
                        if(lens == CameraCharacteristics.LENS_FACING_FRONT){
                            return cameraId;
                        }
                    }else{
                        if(lens == CameraCharacteristics.LENS_FACING_BACK){
                            return cameraId;
                        }
                    }
                }
            }
        }catch (CameraAccessException e){
            logCameraAccessException(e);
        }
        return null;
    }
    private void openCamera(CameraManager manager, String cameraId, Handler handler) {
        if (ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        try{
            manager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    Camera = camera;
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    refreshFragment();
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {

                }

            }, handler);
        } catch (CameraAccessException e){
            e.printStackTrace();
            logCameraAccessException(e);
        }
    }

    private void createCaptureSession(CameraDevice camera, List<Surface> targets, Handler handler){
        try{
            camera.createCaptureSession(targets, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        cameraCaptureSession = session;
                        CaptureRequest.Builder requestBuilder = null;
                        requestBuilder = Camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        requestBuilder.addTarget(autoFitSurfaceView.getHolder().getSurface());
                        requestBuilder.addTarget(imageReader.getSurface());
                        cameraCaptureSession.setRepeatingRequest(requestBuilder.build(), null, cameraHandler);
                    } catch (CameraAccessException e) {
                        logCameraAccessException(e);
                    }
                    imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                        @Override
                        public void onImageAvailable(ImageReader reader) {
                            Image inputImage = reader.acquireLatestImage();
                            if(inputImage == null) {
                                return;
                            }
                            if(isFinishAnalysis){
                                inputImage.close();
                                return;
                            }
                            inputImage.getPlanes();
                            Bitmap tempImage = ImageUtils.convertYUV420ToARGB8888(inputImage);
                            if(sNthFrame == 0 && !calibrationTimerStart){
                                startCalibrationTimer();
                                calibrationTimerStart = true;
                                inputImage.close();
                                return;
                            }
                            if(!calibrationFinish) {
                                inputImage.close();
                                return;
                            }
                            inputImage.close();

                            Bitmap bitmapImage = tempImage.copy(Bitmap.Config.ARGB_8888, false);
                            if(Config.USE_CAMERA_DIRECTION == Config.CAMERA_DIRECTION_FRONT){
                                Matrix rotateMatrix = new Matrix();
                                Matrix flipMatrix = new Matrix();
                                rotateMatrix.postRotate(-90);
                                flipMatrix.setScale(-1, 1);
                                bitmapImage = Bitmap.createBitmap(
                                        bitmapImage, 0, 0, bitmapImage.getWidth(), bitmapImage.getHeight(), rotateMatrix, false);
                                bitmapImage = Bitmap.createBitmap(
                                        bitmapImage, 0, 0, bitmapImage.getWidth(), bitmapImage.getHeight(), flipMatrix, false);
                            }

                            if(isFixFaceBox){
                                Bitmap faceImage = Bitmap.createBitmap(bitmapImage, FaceBox.left, FaceBox.top, FaceBox.width(), FaceBox.height());
                                isFinishAnalysis = mBpmAnalysisViewModel.addFaceImageModel(new FaceImageModel(faceImage, System.currentTimeMillis()));
                                Log.d("Result", "Nth Frame : " + sNthFrame);
                                if(mProgressBar.getProgress() != (sNthFrame/(Vital.BATCH_SIZE * Vital.FRAME_WINDOW_SIZE / 100))){
                                    updateProgressBar(sNthFrame/(Vital.BATCH_SIZE * Vital.FRAME_WINDOW_SIZE / 100));
                                }
                                sNthFrame ++;
                                if(isFinishAnalysis){
                                    Intent intent = new Intent(getContext(), ResultActivity.class);
                                    getContext().startActivity(intent);
                                }
                            } else{
                                MPImage image = new BitmapImageBuilder(bitmapImage).build();
                                faceDetector.detectAsync(image, bitmapImage , System.currentTimeMillis());
                            }

                        }
                    }, imageReaderHandler);

                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            }, handler);
        } catch (CameraAccessException e){
            logCameraAccessException(e);
        }

    }

    private Runnable postInferenceCallback;

    public void processImage(MPImage image, EnhanceFaceDetector.ResultBundle resultBundle){
        postInferenceCallback = image::close;
        if(sNthFrame > Vital.BATCH_SIZE * Vital.FRAME_WINDOW_SIZE){
            return;
        }
        List<FaceDetectorResult> faceDetectorResults = resultBundle.getResults();

        FaceImageModel faceImageModel = null;

        try {
            if (faceDetectorResults.get(0).detections().size() >= 1) {
                RectF box = faceDetectorResults.get(0).detections().get(0).boundingBox();
                List<NormalizedKeypoint> facePoints = faceDetectorResults.get(0).detections().get(0).keypoints().get();

                if(mTrackingOverlayView.isOutBoundary(box)){
                    if(!isStopPredict) {
                        stopPrediction(Config.TYPE_OF_OUT);
                    }
                    readyForNextImage();
                    return;
                } else if(mTrackingOverlayView.isBigSize(box)){
                    if(!isStopPredict) {
                        stopPrediction(Config.TYPE_OF_BIG);
                    }
                    readyForNextImage();
                    return;
                } else if (mTrackingOverlayView.isSmallSize(box)) {
                    if(!isStopPredict) {
                        stopPrediction(Config.TYPE_OF_SMALL);
                    }
                    readyForNextImage();
                    return;
                }
                isStopPredict = false;
                mGuidePopupView.dismiss();

                if(!isFixFaceBox) {
                    box.round(FaceBox);
                    isFixFaceBox = true;
                    if (mTrackingOverlayView != null) {
                        FaceDetectorResult result = resultBundle.getResults().get(0);
                        mTrackingOverlayView.setResults(result,
                                resultBundle.inputImageWidth,
                                resultBundle.inputImageHeight);
                        mTrackingOverlayView.invalidate();
                    }
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        readyForNextImage();
    }

    private void initCalibrationView(){
        mCountdownTextView = new TextView(getContext());
        mCountdownTextView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        mCountdownTextView.setPadding(40, 40, 40, 40);
        mCountdownTextView.setTextSize(25);
    }

    private void initCalibrationTimer(){
        mCalibrationTimer = new CountDownTimer(3999, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                mCountdownTextView.setText(String.valueOf(millisUntilFinished/1000));
            }

            @Override
            public void onFinish() {
                calibrationFinish = true;
                mCountdownPopup.dismiss();
            }
        };
    }
    private void startCalibrationTimer(){
        if(mCountdownPopup == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            mCountdownPopup = builder.setTitle("Countdown Timer")
                    .setView(mCountdownTextView)
                    .setCancelable(false)
                    .create();
        }
        mCountdownPopup.show();
        mCalibrationTimer.start();
    }

    protected void readyForNextImage() {
        if (postInferenceCallback != null) {
            postInferenceCallback.run();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        try{
            mFrontCameraExecutor.shutdown();
            mFrontCameraExecutor.awaitTermination(
                    Long.MAX_VALUE,
                    TimeUnit.NANOSECONDS
            );
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onResults(MPImage input, Bitmap original, EnhanceFaceDetector.ResultBundle resultBundle) {
        mOriginalBitmap = original;
        if(sNthFrame <= 600){
            processImage(input, resultBundle);
        }
    }

    private void stopPrediction(String type){
        if(type.equals(Config.TYPE_OF_BIG)){
            mGuidePopupText.setText(R.string.face_big_detection);
        }else if(type.equals(Config.TYPE_OF_SMALL)){
            mGuidePopupText.setText(R.string.face_no_detection);
        }else if(type.equals(Config.TYPE_OF_OUT)){
            mGuidePopupText.setText(R.string.face_out_detection);
        }
        mTrackingOverlayView.clear();
        sNthFrame = 0;
        updateProgressBar(mProgressBar.getMin());
        mGuidePopupView.show();
        isStopPredict = true;
    }

    @Override
    public void onError(String error, int errorCode) {

    }

    private void updateProgressBar(int progress){
        if((progress == mProgressBar.getMin())
                && (mProgressBar.getProgress() == mProgressBar.getMin())) return;
        mProgressBar.setProgress(progress);
        mProgressBar.invalidate();
    }

    private void logCameraAccessException(Exception e){
        Log.e("Camera", "Can not accessed in Camera : " + e.getMessage());
    }

    private void refreshFragment() {
        getParentFragmentManager().beginTransaction()
                .detach(this)
                .attach(this)
                .commit();
    }
}