package com.inniopia.funnylabs_sdk;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.components.containers.NormalizedKeypoint;
import com.google.mediapipe.tasks.vision.facedetector.FaceDetectorResult;
import com.inniopia.funnylabs_sdk.ui.CommonPopupView;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MainFragment extends Fragment implements EnhanceFaceDetector.DetectorListener {

    //View Variable
    private PreviewView mCameraView;
    private OverlayView mTrackingOverlayView;
    private ProgressBar mProgressBar;
    private BpmAnalysisViewModel mBpmAnalysisViewModel;
    private CommonPopupView mGuidePopupView;
    private TextView mGuidePopupText;

    //Camera Property variable
    private EnhanceFaceDetector faceDetector;
    private ExecutorService mFrontCameraExecutor;
    private ProcessCameraProvider mCameraProvider;
    private Preview mPreview;
    private ImageAnalysis mImageAnalysis;
    private Camera mCamera;
    public Bitmap mOriginalBitmap;
    private long lastFrameUtcTimeMs = -1;

    private int CAMERA_RATIO = AspectRatio.RATIO_16_9;
    private int ROTATION = Surface.ROTATION_0;

    private int sNthFrame = 0;

    private boolean isStopPredict = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBpmAnalysisViewModel = new BpmAnalysisViewModel(new Application(), requireContext());

        mFrontCameraExecutor = Executors.newSingleThreadExecutor();

        mFrontCameraExecutor.execute(
                () -> {
                    faceDetector = new EnhanceFaceDetector(requireContext(), this);
                    faceDetector.setupFaceDetector();
                }
        );


    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_main, container, false);

        //Face Detection
        mCameraView = view.findViewById(R.id.view_finder);
        mTrackingOverlayView = view.findViewById(R.id.tracking_overlay);
        mProgressBar = view.findViewById(R.id.progress);

        //Face Guide Popup
        View viewNoDetectionPopup = inflater.inflate(R.layout.layout_detection_popup, container, false);
        mGuidePopupView = new CommonPopupView(requireContext(),viewNoDetectionPopup);
        mGuidePopupText = viewNoDetectionPopup.findViewById(R.id.text_face_popup);
        mGuidePopupText.setText(R.string.face_no_detection);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        requireActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mCameraView.post(new Runnable() {
            @Override
            public void run() {
                setUpCamera(mCameraView.getSurfaceProvider());
            }
        });
    }

    private void setUpCamera(Preview.SurfaceProvider surfaceProvider){
        ListenableFuture<ProcessCameraProvider> cameraProvider
                = ProcessCameraProvider.getInstance(requireContext());

        cameraProvider.addListener(new Runnable() {
            @Override
            public void run() {
                try{
                    mCameraProvider = cameraProvider.get();
                    bindCameraUseCases(surfaceProvider);
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void bindCameraUseCases(Preview.SurfaceProvider surfaceProvider){
        ProcessCameraProvider cameraProvider = mCameraProvider;
        CameraSelector cameraSelector =
                new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build();
        mPreview = new Preview.Builder()
                .setTargetAspectRatio(CAMERA_RATIO)
                .setTargetRotation(ROTATION)
                .build();

        mImageAnalysis = new ImageAnalysis.Builder().setTargetAspectRatio(CAMERA_RATIO)
                 .setTargetRotation(ROTATION)
                 .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                 .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                 .build();

        mImageAnalysis.setAnalyzer(
                mFrontCameraExecutor, faceDetector::detectLiveStreamFrame);
        cameraProvider.unbindAll();

        try{
            mCamera = cameraProvider.bindToLifecycle(this, cameraSelector, mPreview, mImageAnalysis);
            mPreview.setSurfaceProvider(surfaceProvider);
        } catch (Exception e){
            e.printStackTrace();
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
/*
                if(mTrackingOverlayView.isBigSize(box)){
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
*/
                float x = box.right;
                float start_x = box.left;
                RectF rectF = new RectF(start_x, box.top, x, box.bottom);
                Rect rect = new Rect();
                rectF.round(rect);
                Bitmap croppedFaceBitmap = Bitmap.createBitmap(mOriginalBitmap, rect.left, rect.top, rect.width(), rect.height());
                faceImageModel = new FaceImageModel(croppedFaceBitmap, getLastFrameUtcTimeMs());
                Log.d("Result", "Nth Frame : " + sNthFrame);
                sNthFrame ++;
                mBpmAnalysisViewModel.addFaceImageModel(faceImageModel);
            }
            if (mTrackingOverlayView != null) {
                FaceDetectorResult result = resultBundle.getResults().get(0);
                mTrackingOverlayView.setResults(result,
                        resultBundle.inputImageWidth,
                        resultBundle.inputImageHeight);
                mTrackingOverlayView.invalidate();
            }
            if(mProgressBar.getProgress() != (sNthFrame/(Vital.BATCH_SIZE * Vital.FRAME_WINDOW_SIZE / 100))){
                updateProgressBar(sNthFrame/(Vital.BATCH_SIZE * Vital.FRAME_WINDOW_SIZE / 100));
            }
        } catch (Exception e){
            e.printStackTrace();
        }
        readyForNextImage();
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
        lastFrameUtcTimeMs = System.currentTimeMillis();
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

    public long getLastFrameUtcTimeMs(){
        return lastFrameUtcTimeMs;
    }

    private void updateProgressBar(int progress){
        if((progress == mProgressBar.getMin())
                && (mProgressBar.getProgress() == mProgressBar.getMin())) return;
        mProgressBar.setProgress(progress);
        mProgressBar.invalidate();
    }
}