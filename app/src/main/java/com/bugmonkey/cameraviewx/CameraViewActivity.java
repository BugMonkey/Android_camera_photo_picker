package com.bugmonkey.cameraviewx;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraState;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import androidx.lifecycle.Observer;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaActionSound;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.bugmonkey.cameraviewx.databinding.ActivityMainBinding;
import com.google.common.util.concurrent.ListenableFuture;
import com.bugmonkey.cameraviewx.matisse.Matisse;
import com.bugmonkey.cameraviewx.matisse.MimeType;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class CameraViewActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final int REQUEST_CODE_GALLERY = 11;
    private static final int REQUEST_CODE_PREVIEW = 12;
    public static final int REQUEST_CAMERA_VIEW_ACTIVITY = 13;
    private static final String TAG = "CameraX";


    private String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private ActivityMainBinding viewBinding;

    private ImageCapture imageCapture;

    private ExecutorService cameraExecutor;

    private ProcessCameraProvider cameraProvider;

    private Preview preview;

    /**
     * 是否是后置摄像头
     */
    private boolean isBackCamera = true;

    /**
     * 是否是全屏显示（16：9）
     */
    private boolean isFullPhoto = true;

    private Camera camera;

    public static void start(Activity context) {
        Intent starter = new Intent(context, CameraViewActivity.class);
        context.startActivityForResult(starter,REQUEST_CAMERA_VIEW_ACTIVITY);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        viewBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());

        cameraExecutor = Executors.newSingleThreadExecutor();

        REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO};
        if (allPermissionsGranted()){
            startCamera();
        }else{
            ActivityCompat.requestPermissions(
                    this,REQUIRED_PERMISSIONS , REQUEST_CODE_PERMISSIONS);
        }



        initListener();



    }

    private void initListener() {
        viewBinding.ivGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Matisse.from(CameraViewActivity.this).choose(MimeType.ofImage()).maxOriginalSize(1).capture(false).theme(R.style.Matisse).forResult(REQUEST_CODE_GALLERY);
            }
        });

        final SpringAnimation animX = new SpringAnimation(viewBinding.imageCaptureButton, SpringAnimation.SCALE_X, 0.9f);
        final SpringAnimation animY = new SpringAnimation(viewBinding.imageCaptureButton, SpringAnimation.SCALE_Y, 0.9f);



        viewBinding.imageCaptureButton.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:

                    animX.getSpring().setStiffness(SpringForce.STIFFNESS_HIGH);
                    animX.getSpring().setDampingRatio(SpringForce.DAMPING_RATIO_LOW_BOUNCY);
                    animX.getSpring().setFinalPosition(0.9f);

                    animY.getSpring().setStiffness(SpringForce.STIFFNESS_HIGH);
                    animY.getSpring().setDampingRatio(SpringForce.DAMPING_RATIO_LOW_BOUNCY);
                    animY.getSpring().setFinalPosition(0.9f);

                    animX.start();
                    animY.start();
                    return  false;
                case MotionEvent.ACTION_MOVE:
                    return false;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
//                        可以要可以不要
//                        animX.cancel();
//                        animY.cancel();

                    animX.getSpring().setFinalPosition(1);
                    animY.getSpring().setFinalPosition(1);

                    animX.start();
                    animY.start();
            }
            return false;
        });

        viewBinding.imageCaptureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePhoto();
            }
        });

        viewBinding.flChangeCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(viewBinding.ivPreview.getVisibility() != View.GONE){
                    return;
                }
                startPropertyAnim();
                isBackCamera = !isBackCamera;
                takeCurrentFrame();
                resetCamera();
            }
        });

        viewBinding.flExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        viewBinding.flFullscreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(viewBinding.ivPreview.getVisibility() != View.GONE){
                    return;
                }

                isFullPhoto = !isFullPhoto;

                viewBinding.ivFullscreen.setSelected(!isFullPhoto);

                takeCurrentFrame();

                ConstraintUtil.ConstraintBegin begin = new ConstraintUtil(viewBinding.getRoot()).beginWithAnim();
                begin.clear(R.id.fl_container);
                begin.Left_toLeftOf(R.id.fl_container,ConstraintSet.PARENT_ID);
                begin.Right_toRightOf(R.id.fl_container,ConstraintSet.PARENT_ID);



                if(isFullPhoto){
                    begin.setDimensionRatio(R.id.fl_container,null);
                    begin.Top_toTopOf(R.id.fl_container,ConstraintSet.PARENT_ID);
                    begin.Bottom_toBottomOf(R.id.fl_container,ConstraintSet.PARENT_ID);

                }else {
                    begin.setDimensionRatio(R.id.fl_container,"3:4");
                    begin.Top_toBottomOf(R.id.fl_container,R.id.fl_exit);
                }


                begin.commit();

                resetCamera();

            }
        });

        viewBinding.viewFinder.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                FocusMeteringAction action = new FocusMeteringAction.Builder(viewBinding.viewFinder.getMeteringPointFactory().createPoint(event.getX(), event.getY())).build();
                if(camera != null){
                    camera.getCameraControl().startFocusAndMetering(action);
                }
//                showTapView(((int) event.getX()), ((int) event.getY()));
                return false;
            }
        });

    }



    @Override
    protected void onResume() {
        super.onResume();
        getPicFromP();
    }

    private boolean allPermissionsGranted() {
        return  Arrays.stream(REQUIRED_PERMISSIONS).allMatch(s -> ContextCompat.checkSelfPermission(this,s) == PackageManager.PERMISSION_GRANTED);
    }

    private void takePhoto() {
        if (imageCapture == null){
            return;
        }

        //文件保存路径
        String name = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault())
                .format(System.currentTimeMillis());
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/"+getString(getApplicationInfo().labelRes));
        }
        ImageCapture.OutputFileOptions options = new ImageCapture.OutputFileOptions.Builder(getContentResolver(),MediaStore.Images.Media.EXTERNAL_CONTENT_URI,contentValues).build();

        playSound();

        startZoomPropertyAnim();
        imageCapture.takePicture(options, cameraExecutor, new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                Log.e(TAG, "onImageSaved" + outputFileResults.getSavedUri());
                ImagePreviewActivity.start(CameraViewActivity.this,outputFileResults.getSavedUri(),REQUEST_CODE_PREVIEW);

            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e(TAG, exception.getLocalizedMessage());
            }
        });
    }

    private void takeCurrentFrame(){
        if (imageCapture == null){
            return;
        }
        Bitmap previewBitmap = viewBinding.viewFinder.getBitmap();
        if(previewBitmap == null){
            return;
        }
        viewBinding.ivPreview.setImageBitmap(fastblur(previewBitmap,0.2f,50));
        viewBinding.ivPreview.setVisibility(View.VISIBLE);
    }

    /**
     * 拍照音效
     */
    private void playSound(){
        MediaActionSound sound = new MediaActionSound();
        sound.play(MediaActionSound.SHUTTER_CLICK);
    }

    /**
     * 显示对焦框
     * @param x
     * @param y
     */
    private void showTapView(int x, int y) {
        PopupWindow popupWindow = new PopupWindow(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ImageView imageView = new ImageView(this);
        imageView.setImageResource(R.drawable.ic_focus_view);
        popupWindow.setContentView(imageView);
        popupWindow.showAsDropDown(viewBinding.viewFinder, x, y);
        viewBinding.viewFinder.postDelayed(popupWindow::dismiss, 800);
        viewBinding.viewFinder.playSoundEffect(SoundEffectConstants.CLICK);
    }

    // 旋转动画
    private void startPropertyAnim() {
        // 第二个参数"rotation"表明要执行旋转
        // 0f -> 360f，从旋转360度，也可以是负值，负值即为逆时针旋转，正值是顺时针旋转。
        ObjectAnimator anim = ObjectAnimator.ofFloat(viewBinding.flChangeCamera, "rotation", 0f, 180f);

        // 动画的持续时间，执行多久？
        anim.setDuration(500);

        // 正式开始启动执行动画
        anim.start();
    }

    // 放大缩小动画
    private void startZoomPropertyAnim() {
        final SpringAnimation anim = new SpringAnimation(viewBinding.imageCaptureButton ,SpringAnimation.ALPHA,1);
        anim.getSpring().setDampingRatio(SpringForce.DAMPING_RATIO_HIGH_BOUNCY);
        anim.getSpring().setStiffness(SpringForce.STIFFNESS_VERY_LOW);
        anim.start();

        // 正式开始启动执行动画
        anim.start();
    }


    /**
     * 启动相机
     */
    private void startCamera() {
        ListenableFuture cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                     cameraProvider = (ProcessCameraProvider)cameraProviderFuture.get();

                     resetCamera();


                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                    Log.e(TAG, "Use case binding failed" + e.getLocalizedMessage());
                }

            }
        },ContextCompat.getMainExecutor(this));
    }

    /**
     * 重置相机
     */
    private void resetCamera() {
        preview = new Preview.Builder().setTargetAspectRatio(isFullPhoto ? AspectRatio.RATIO_16_9 : AspectRatio.RATIO_4_3).build();
        imageCapture = new ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).setTargetAspectRatio(isFullPhoto ? AspectRatio.RATIO_16_9 : AspectRatio.RATIO_4_3).build();

        cameraProvider.unbindAll();
        camera = cameraProvider.bindToLifecycle(CameraViewActivity.this,isBackCamera ? CameraSelector.DEFAULT_BACK_CAMERA : CameraSelector.DEFAULT_FRONT_CAMERA,preview,imageCapture);
        camera.getCameraInfo().getCameraState().observe(CameraViewActivity.this, new Observer<CameraState>() {
            @Override
            public void onChanged(CameraState cameraState) {
                Log.e(TAG,cameraState.getType().toString());
                switch (cameraState.getType()){
                    case CLOSED:
                        viewBinding.ivPreview.setVisibility(View.VISIBLE);
                        break;
                    case OPEN:
                        viewBinding.ivPreview.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                viewBinding.ivPreview.setVisibility(View.GONE);
                            }
                        },600);
                        break;

                }
            }
        });
        preview.setSurfaceProvider(viewBinding.viewFinder.getSurfaceProvider());
    }

    /**
     * 获取系统相册第一张照片
     */
    private void getPicFromP() {

        viewBinding.ivGallery.setImageURI(Uri.fromFile(new File(getLastPhotoByPath(CameraViewActivity.this))));

    }
    /**
     * 本次查询的就是针对 相机里面的图片进行搜查,获得最近一排的一张照片,的路径

     * @return
     */
    public static String getLastPhotoByPath(Context context) {

        Cursor myCursor = null;
        String pathLast="";
        // Create a Cursor to obtain the file Path for the large image
        String[] largeFileProjection = {
                MediaStore.Images.ImageColumns._ID,
                MediaStore.Images.ImageColumns.DATA,
                MediaStore.Images.ImageColumns.ORIENTATION,
                MediaStore.Images.ImageColumns.DATE_TAKEN };
        String largeFileSort = MediaStore.Images.ImageColumns._ID + " DESC";
        myCursor =
//					BaseApplication.getInstance().
                context.getContentResolver().query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        largeFileProjection, null, null, largeFileSort);

        if (myCursor.getCount()<1) {
            myCursor.close();
            return pathLast;
        }
        while (myCursor.moveToNext()) {
            String data = myCursor.getString(myCursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
            File f = new File(data);
            if (f.exists()) {//第一个图片文件，就是最近一次拍照的文件；
                pathLast=f.getPath();
                System.out.println("f.getPath() = "+pathLast);
                myCursor.close();
                return pathLast;
            }
        }
        myCursor.close();
        return pathLast;

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){
            switch (requestCode){
                case REQUEST_CODE_GALLERY:
                    //相册选择
                     ImagePreviewActivity.start(CameraViewActivity.this,Matisse.obtainResult(data).get(0),REQUEST_CODE_PREVIEW);

                    break;
                case REQUEST_CODE_PREVIEW:
                    setResult(RESULT_OK,data);
                    finish();
                    break;
            }
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraProvider.unbindAll();
        cameraExecutor.shutdown();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if(allPermissionsGranted()){
                startCamera();
            }else {
                Toast.makeText(this,
                        "Permissions not granted by the user.",
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }

    }

    public Bitmap fastblur(Bitmap sentBitmap, float scale, int radius) {

        int width = Math.round(sentBitmap.getWidth() * scale);
        int height = Math.round(sentBitmap.getHeight() * scale);
        sentBitmap = Bitmap.createScaledBitmap(sentBitmap, width, height, false);

        Bitmap bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);

        if (radius < 1) {
            return (null);
        }

        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        int[] pix = new int[w * h];
        Log.e("pix", w + " " + h + " " + pix.length);
        bitmap.getPixels(pix, 0, w, 0, 0, w, h);

        int wm = w - 1;
        int hm = h - 1;
        int wh = w * h;
        int div = radius + radius + 1;

        int r[] = new int[wh];
        int g[] = new int[wh];
        int b[] = new int[wh];
        int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
        int vmin[] = new int[Math.max(w, h)];

        int divsum = (div + 1) >> 1;
        divsum *= divsum;
        int dv[] = new int[256 * divsum];
        for (i = 0; i < 256 * divsum; i++) {
            dv[i] = (i / divsum);
        }

        yw = yi = 0;

        int[][] stack = new int[div][3];
        int stackpointer;
        int stackstart;
        int[] sir;
        int rbs;
        int r1 = radius + 1;
        int routsum, goutsum, boutsum;
        int rinsum, ginsum, binsum;

        for (y = 0; y < h; y++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            for (i = -radius; i <= radius; i++) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))];
                sir = stack[i + radius];
                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);
                rbs = r1 - Math.abs(i);
                rsum += sir[0] * rbs;
                gsum += sir[1] * rbs;
                bsum += sir[2] * rbs;
                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }
            }
            stackpointer = radius;

            for (x = 0; x < w; x++) {

                r[yi] = dv[rsum];
                g[yi] = dv[gsum];
                b[yi] = dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm);
                }
                p = pix[yw + vmin[x]];

                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[(stackpointer) % div];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi++;
            }
            yw += w;
        }
        for (x = 0; x < w; x++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            yp = -radius * w;
            for (i = -radius; i <= radius; i++) {
                yi = Math.max(0, yp) + x;

                sir = stack[i + radius];

                sir[0] = r[yi];
                sir[1] = g[yi];
                sir[2] = b[yi];

                rbs = r1 - Math.abs(i);

                rsum += r[yi] * rbs;
                gsum += g[yi] * rbs;
                bsum += b[yi] * rbs;

                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }

                if (i < hm) {
                    yp += w;
                }
            }
            yi = x;
            stackpointer = radius;
            for (y = 0; y < h; y++) {
                // Preserve alpha channel: ( 0xff000000 & pix[yi] )
                pix[yi] = ( 0xff000000 & pix[yi] ) | ( dv[rsum] << 16 ) | ( dv[gsum] << 8 ) | dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (x == 0) {
                    vmin[y] = Math.min(y + r1, hm) * w;
                }
                p = x + vmin[y];

                sir[0] = r[p];
                sir[1] = g[p];
                sir[2] = b[p];

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[stackpointer];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi += w;
            }
        }

        Log.e("pix", w + " " + h + " " + pix.length);
        bitmap.setPixels(pix, 0, w, 0, 0, w, h);

        return (bitmap);
    }
}