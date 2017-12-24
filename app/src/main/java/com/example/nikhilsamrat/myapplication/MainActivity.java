package com.example.nikhilsamrat.myapplication;

import android.app.Application;
import android.content.Context;
import android.content.pm.ActivityInfo;
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
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Size;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;


public class MainActivity extends AppCompatActivity {
    public MediaCodec mediaCodec;
    public MediaMuxer mediaMuxer;
    public MediaMuxer mediaMuxer1 = null;
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
    public ProgressBar P;
    public TextView t1,t2;
    public Queue<ByteBuffer> buffers = new LinkedList<>();
    public Queue<ByteBuffer> buffers1;
    public Queue<MediaCodec.BufferInfo> bufferInfos = new LinkedList<>();
    public Queue<MediaCodec.BufferInfo> bufferInfos1;
    public ByteBuffer[] A = new ByteBuffer[2];
    public MediaCodec.BufferInfo[] B = new MediaCodec.BufferInfo[2];
    public int kframes=0,overflow=0,frames=0;
    public int x =0,y=0,time=10,count=0;
    public boolean b = false;
    public MediaFormat mediaFormat;
    public File direct;
    public timer test;
    public int videoindex;
    public listener Orientation;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        B1 = findViewById(R.id.button1);
        T = findViewById(R.id.surfaceView1);
        P = findViewById(R.id.progressBar);
        t1 = findViewById(R.id.textView);
        t2 = findViewById(R.id.textView1);
        t1.setText("0:"+String.valueOf(2*time));

        B1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                click();
            }
        });
        B1.setVisibility(View.INVISIBLE);

        direct = new File(Environment.getExternalStorageDirectory() + "/Download/new");

        if(!direct.exists())
        {
            direct.mkdir(); //directory is created;
        }

        settexture();
        test = new timer();
        // test.start();
        Orientation = new listener(this);
        Orientation.enable();
    }

    public class timer extends Thread{

        @Override
        public void run() {

        }

        public void set(){
            B1.setVisibility(View.VISIBLE);
        }
    }

    public class listener extends OrientationEventListener{

        public int orie=0;

        public listener(Context context) {
            super(context);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            orie=orientation;
        }
    }

    private void setcodec() {

        MediaFormat format = MediaFormat.createVideoFormat("video/mp4v-es", 640, 480);
        format.setInteger(format.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        format.setInteger(format.KEY_BIT_RATE,  10000000);
        format.setInteger(format.KEY_FRAME_RATE, 30);
        format.setInteger(format.KEY_CAPTURE_RATE, 30);
        format.setInteger(format.KEY_I_FRAME_INTERVAL, 1);
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
                if(info.flags==1) addframe();
                if(mediaMuxer1!=null){
                    if(info.flags==1){
                        if(count>0&&count<=time){
                            P.setProgress(P.getProgress()+5);
                            if (P.getProgress() / 5 < 10)
                                t2.setText("0:0" + String.valueOf(P.getProgress() / 5));
                            else
                                t2.setText("0:" + String.valueOf(P.getProgress() / 5));
                        }
                        count++;
                    }
                    mediaMuxer1.writeSampleData(videoindex,codec.getOutputBuffer(index),info);
                    if(count>time){
                        mediaMuxer1.stop();
                        mediaMuxer1=null;
                        B1.setVisibility(View.VISIBLE);
                        P.setProgress(50);
                        t2.setText("0:10");
                        warn("asd");
                        count=0;
                    }

                }
                write(codec.getOutputBuffer(index),info);
                codec.releaseOutputBuffer(index,false);
            }

            @Override
            public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {

            }

            @Override
            public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
                mediaFormat = format;
            }
        });

        try{
            mediaCodec.configure(format, null, null,MediaCodec.CONFIGURE_FLAG_ENCODE);
        }catch (Exception e){
            Toast.makeText(this,e.toString(),Toast.LENGTH_LONG).show();
        }
        codecsurface = mediaCodec.createInputSurface();

        mediaCodec.start();
    }

    private void addframe() {
        if(bufferInfos.size()>0){
            kframes++;
            if(kframes>time){
                overflow++;
                kframes=0;
            }else if(P.getProgress()<50) {
                P.setProgress(P.getProgress() + 5);
                if (P.getProgress() / 5 < 10)
                    t2.setText("0:0" + String.valueOf(P.getProgress() / 5));
                else
                    t2.setText("0:" + String.valueOf(P.getProgress() / 5));
                if (kframes == time)
                    B1.setVisibility(View.VISIBLE);
            }


            if(overflow>0){
                bufferInfos.remove();
                while(bufferInfos.peek().flags!=1){
                    bufferInfos.remove();
                }
                int i=0;
                while(files(kframes,i).exists()){
                    files(kframes,i).delete();
                    i++;
                }
                files(kframes,i).delete();

                try{
                    RandomAccessFile r = new RandomAccessFile(files((kframes+1)%(time+1),0),"rw");
                    if(bufferInfos.peek().size!=r.getChannel().size())
                        warn(String.valueOf(bufferInfos.peek().size)+ " "+ String.valueOf(r.getChannel().size()));
                    r.getChannel().close();
                }catch (FileNotFoundException e){

                }catch (IOException e){

                }
            }

            frames=0;
        }
    }

    int first=0;
    private void write(ByteBuffer b, MediaCodec.BufferInfo info) {
        // warn(String.valueOf(info.presentationTimeUs)+" "+String.valueOf(info.size) +" "+ String.valueOf(info.offset)+
        // " "+String.valueOf(info.flags));
        //buffers.add(c.getOutputBuffer(i));
        if(first<1){

            try {
                File fs = new File("/storage/emulated/0/Download/new/" + "i" );
                FileChannel fc = new FileOutputStream(fs, false).getChannel();
                fc.write(b);
                fc.close();
            } catch (FileNotFoundException e) {
                warn(e.getLocalizedMessage());
            } catch (IOException e) {
                warn(e.getLocalizedMessage());
            }
            B[first]=info;
            first++;
        }
        else {

            try {
                FileChannel fc = new FileOutputStream(files(kframes,frames), false).getChannel();
                fc.write(b);
                fc.close();
            } catch (FileNotFoundException e) {
                warn(e.getLocalizedMessage());
            } catch (IOException x) {
                warn(x.getLocalizedMessage());
            }
            bufferInfos.add(info);
            frames++;
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


    private void click(){

            B1.setVisibility(View.INVISIBLE);
            int kf=0,f=0;

            try {
                File fs;
                FileChannel fc;

                if(overflow>0)
                {
                    kf= kframes+1;
                    if(kf>time)kf=0;
                }
                else kf=0;
                try {
                    mediaMuxer = new MediaMuxer("/storage/emulated/0/Download/" + String.valueOf(x) + ".mp4",
                            MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                videoindex = mediaMuxer.addTrack(mediaFormat);
                int orientation = Orientation.orie;
                if(orientation<45 || orientation >=315)
                    mediaMuxer.setOrientationHint(90);
                if(orientation>=45 && orientation < 135)
                    mediaMuxer.setOrientationHint(180);
                if(orientation>=135 && orientation< 225)
                    mediaMuxer.setOrientationHint(270);
                if(orientation>=225 && orientation<315)
                    mediaMuxer.setOrientationHint(0);
                warn(String.valueOf(orientation));
                mediaMuxer.start();
                try {
                    RandomAccessFile r = new RandomAccessFile("/storage/emulated/0/Download/new/" + "i",
                            "rw");
                    fc = r.getChannel();
                    ByteBuffer bf = ByteBuffer.allocate((int) fc.size());
                    fc.read(bf);
                    mediaMuxer.writeSampleData(videoindex, bf, B[0]);
                    fc.close();
                }catch (FileNotFoundException e) {
                }

                bufferInfos1 = new LinkedList(bufferInfos);

                x++;
                while (!bufferInfos1.isEmpty()) {
                        fs = files(kf,f);
                        if(!fs.exists()){
                            fs.delete();
                            f=0;
                            kf++;
                            if(kf>time)kf=0;
                            fs = files(kf,f);
                        }

                        fc = new FileInputStream(fs).getChannel();
                        ByteBuffer bf = ByteBuffer.allocate((int) fc.size());
                        fc.read(bf);

                        try {
                            mediaMuxer.writeSampleData(videoindex, bf, bufferInfos1.peek());
                        }
                        catch (Exception e) {
                             e.printStackTrace();
                        }
                        bufferInfos1.remove();
                        fc.close();
                        f++;

                }

            } catch (FileNotFoundException e) {
                warn(e.getLocalizedMessage());
            } catch (IOException x) {
                warn(x.getLocalizedMessage());
            }
            mediaMuxer1 = mediaMuxer;
           // mediaMuxer.stop();
            warn("asd");
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
        surfaceTexture.setDefaultBufferSize(imagedimension.getWidth(),imagedimension.getHeight());
        Surface surface = new Surface(surfaceTexture);
        surfaces = new LinkedList<>();
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
    }

    private File files(int x, int y){
        File fs = new File("/storage/emulated/0/Download/new/" + String.valueOf(x) +"_" + String.valueOf(y));
        return fs;
    }

}