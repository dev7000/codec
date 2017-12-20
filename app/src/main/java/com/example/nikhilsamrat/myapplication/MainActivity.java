package com.example.nikhilsamrat.myapplication;

import android.app.Application;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.CamcorderProfile;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;


public class MainActivity extends AppCompatActivity {
    public MediaCodec mediaCodec;
    public MediaMuxer mediaMuxer;
    public CameraDevice cameraDevice;
    public CameraCaptureSession captureSession;
    public CaptureRequest.Builder Builder1;
    public CaptureRequest.Builder Builder2;
    public CaptureRequest.Builder Builder3;
    public CameraManager manager;
    public Size imagedimension;
    public List<Surface> surfaces;
    public Surface codecsurface;
    public Handler handler;
    public HandlerThread thread;
    public TextureView T;
    public Button B1;
    public Button B2;
    public EditText text;
    public Queue<ByteBuffer> buffers = new LinkedList<>();
    public Queue<ByteBuffer> buffers1;
    public Queue<MediaCodec.BufferInfo> bufferInfos = new LinkedList<>();
    public Queue<MediaCodec.BufferInfo> bufferInfos1;
    public ByteBuffer[] A = new ByteBuffer[2];
    public MediaCodec.BufferInfo[] B = new MediaCodec.BufferInfo[2];
    public int frames=0;
    public int x = 3;
    public boolean b = false;
    public int videoindex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        B1 = findViewById(R.id.button1);
        B2 = findViewById(R.id.button2);
        T = findViewById(R.id.surfaceView1);
        text = findViewById(R.id.editText);

        B1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                click();
            }
        });

        B2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });


        settexture();

    }

    private void setcodec() {

        CamcorderProfile profile;

        MediaFormat format = MediaFormat.createVideoFormat("video/mp4v-es", 640, 480);
        format.setInteger(format.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(format.KEY_BIT_RATE,  10000000);
        format.setInteger(format.KEY_FRAME_RATE, 30);
        format.setInteger(format.KEY_CAPTURE_RATE, 30);
        format.setInteger(format.KEY_I_FRAME_INTERVAL, 10);
        try {
            mediaCodec = MediaCodec.createEncoderByType("video/mp4v-es");
        }catch (IOException e){
            e.printStackTrace();
        }

        mediaCodec.setCallback(new MediaCodec.Callback() {
            @Override
            public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {

            }

            @Override
            public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
                write(codec.getOutputBuffer(index),info);
                codec.releaseOutputBuffer(index,false);
            }

            @Override
            public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {

            }

            @Override
            public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
                videoindex = mediaMuxer.addTrack(format);
                mediaMuxer.setOrientationHint(90);
            }
        });

        try{
            mediaCodec.configure(format, null, null,MediaCodec.CONFIGURE_FLAG_ENCODE);
        }catch (Exception e){
            Toast.makeText(this,e.toString(),Toast.LENGTH_LONG).show();
        }
        codecsurface = mediaCodec.createInputSurface();

        try{
            mediaMuxer = new MediaMuxer("/storage/emulated/0/Download/"+ String.valueOf(x) +".mp4",
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            mediaCodec.start();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private void write(ByteBuffer b, MediaCodec.BufferInfo info) {
       // warn(String.valueOf(info.presentationTimeUs)+" "+String.valueOf(info.size) +" "+ String.valueOf(info.offset)+
        // " "+String.valueOf(info.flags));

        if(frames<150){
            if (x < 2) {
                A[x] = b;
                B[x]= info;
                x++;
            }else {
                buffers.add(b);
                bufferInfos.add(info);
                frames++;
            }
        }else{
          //  buffers.remove();
           // bufferInfos.remove();
            buffers.add(b);
            bufferInfos.add(info);
        }
    }
    private void settexture(){
        T.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                openmanager();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });
    }

    int z=0;
    private void click(){
        if(z==0) {
            try{
                //mediaCodec.stop();
                mediaMuxer.start();
                buffers1=new LinkedList(buffers);
                bufferInfos1= new LinkedList(bufferInfos);
              /**  bufferInfos1.peek().presentationTimeUs = 0;
                bufferInfos1.peek().size = 30;
                bufferInfos1.peek().flags = 2;
                MediaCodec.BufferInfo buffer = bufferInfos1.remove();
                bufferInfos1.peek().flags = 1;**/
                //x=0;
                try {
                    while (!bufferInfos1.isEmpty()) {
                        if(x<2){
                            mediaMuxer.writeSampleData(videoindex, A[x], B[x]);
                            x++;
                        }
                        else
                        mediaMuxer.writeSampleData(videoindex, buffers1.poll(), bufferInfos1.poll());
                    }
                }catch (Exception e){
                    warn(e.getMessage());
                }
                mediaMuxer.stop();
                warn("asd");
            }catch (Exception e){
                Toast.makeText(this,e.toString(),Toast.LENGTH_LONG).show();
            }
           // z=1;
        }else {
            mediaCodec.stop();
            try{
                mediaMuxer.stop();
            }catch (IllegalStateException e){
                Toast.makeText(this,e.getLocalizedMessage(),Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
            z=0;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        thread.quitSafely();
        try{
            thread.join();
            thread =null;
            handler=null;
        }catch (InterruptedException e){
            e.printStackTrace();
        }
        cameraDevice.close();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if(T.isAvailable())
        openCamera();
        thread = new HandlerThread("Camera Background");
        thread.start();
        handler = new Handler(thread.getLooper());
    }

    private void openmanager() {
        manager = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
        String CameraId;
       try{
           CameraId = manager.getCameraIdList()[0];
           CameraCharacteristics cameraCharacteristics = manager.getCameraCharacteristics(CameraId);
           StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
           imagedimension = map.getOutputSizes(256)[1];
           setcodec();
           //warn();
           openCamera();
       }catch (CameraAccessException e){
            e.printStackTrace();
       }

    }

    private void openCamera(){
        try {
            manager.openCamera("0", new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    cameraDevice = camera;
                    createPreview();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    camera.close();
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    camera.close();
                }
            }, handler);
        }catch (CameraAccessException e){

        }
    }


    private void createPreview() {
        SurfaceTexture surfaceTexture = T.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(T.getHeight(),T.getWidth());
        Surface surface = new Surface(surfaceTexture);
        surfaces = new LinkedList<Surface>();
        surfaces.add(surface);
        surfaces.add(codecsurface);
        try{
            Builder1 = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            Builder1.addTarget(surface);
            Builder1.addTarget(codecsurface);
            cameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    captureSession = session;
                    try{
                        captureSession.setRepeatingRequest(Builder1.build(), null, handler);
                    }catch (CameraAccessException e){

                    }
                }
                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    //warn();
                }
            }, null);
        }catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void warn(String z) {
        Toast.makeText(this,z,Toast.LENGTH_SHORT).show();
       // text.setText(z);
    }

}