package com.example.arcam;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.PixelCopy;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.AugmentedFace;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.Sun;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.Material;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.Renderable;
import com.google.ar.sceneform.rendering.Texture;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.AugmentedFaceNode;
import com.google.ar.sceneform.ux.TransformableNode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static androidx.core.content.PermissionChecker.PERMISSION_GRANTED;

public class MainActivity extends AppCompatActivity {
    private int REQUEST_CODE_PERMISSIONS=101;
    private String permissionList[]=new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
    private Session mSession;
    private FaceArFragment faceArFragment;
    private ModelRenderable faceRegionsRenderable;
    private ArFragment rearFragment;
    private final HashMap<AugmentedFace, AugmentedFaceNode> faceNodeMap = new HashMap<>();
    private Button capbtn, flipbtn, modelbtn, climg;
    private Texture faceMeshTexture;
    private ImageView r,bk,b,y,w;
    private int filterClickCount=0;
    private ModelRenderable andyRenderable;
    // Set to true ensures requestInstall() triggers installation if necessary.
    private boolean mUserRequestedInstall = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toast toast=Toast.makeText(getApplicationContext(),"Welcome to ARCam App",Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.BOTTOM| Gravity.CENTER_HORIZONTAL,0,250);
        toast.show();
        getSupportActionBar().hide();
        climg=findViewById(R.id.clickedimg);
        capbtn=findViewById(R.id.capture);
        flipbtn=findViewById(R.id.flip);
        modelbtn=findViewById(R.id.model);
        r=findViewById(R.id.red);
        bk=findViewById(R.id.black);
        b=findViewById(R.id.blue);
        y=findViewById(R.id.yellow);
        w=findViewById(R.id.white);

        // ARCore requires camera permission to operate.
        if(!permissionsGranted())
        {
            ActivityCompat.requestPermissions(this, permissionList,REQUEST_CODE_PERMISSIONS);
        }

        // Ensure Google Play Services for AR and ARCore device profile data are
        // installed and up to date.
        try {
            if (mSession == null) {
                switch (ArCoreApk.getInstance().requestInstall(this, mUserRequestedInstall)) {
                    case INSTALLED:
                        // Success, safe to create the AR session.
                        mSession = new Session(this);
                        break;
                    case INSTALL_REQUESTED:
                        // When this method returns `INSTALL_REQUESTED`:
                        // 1. This activity will be paused.
                        // 2. The user is prompted to install or update Google Play
                        //    Services for AR (market://details?id=com.google.ar.core).
                        // 3. ARCore downloads the latest device profile data.
                        // 4. This activity is resumed. The next invocation of
                        //    requestInstall() will either return `INSTALLED` or throw an
                        //    exception if the installation or update did not succeed.
                        mUserRequestedInstall = false;
                        return;
                }
            }
        } catch (UnavailableUserDeclinedInstallationException | UnavailableDeviceNotCompatibleException e) {
            // Display an appropriate message to the user and return gracefully.
            Toast.makeText(this, "TODO: handle exception " + e, Toast.LENGTH_LONG)
                    .show();
            return;
        } catch (UnavailableArcoreNotInstalledException e) {
            e.printStackTrace();
        } catch (UnavailableSdkTooOldException e) {
            e.printStackTrace();
        } catch (UnavailableApkTooOldException e) {
            e.printStackTrace();
        }
        //front camera
        faceArFragment = (FaceArFragment) getSupportFragmentManager().findFragmentById(R.id.mask_fragment);
        //rear camera
        rearFragment=(ArFragment) getSupportFragmentManager().findFragmentById(R.id.rear_fragment);
        //Disables Plane Renderer dots
        rearFragment.getArSceneView().getPlaneRenderer().setVisible(false);
        // Disables Plane Surface Detection
        rearFragment.getPlaneDiscoveryController().hide();
        // These lines disable the AR tutorial
        rearFragment.getPlaneDiscoveryController().setInstructionView(null);
        //open front camera initially
        rearFragment.getView().setVisibility(View.GONE);
        // When you build a Renderable, Sceneform loads its resources in the background while returning
        // a CompletableFuture. Call thenAccept(), handle(), or check isDone() before calling get().
        ModelRenderable.builder()
                .setSource(this, R.raw.andy)
                .build()
                .thenAccept(renderable -> andyRenderable = renderable)
                .exceptionally(
                        throwable -> {
                            Toast toast2 =
                                    Toast.makeText(this, "Unable to load andy renderable", Toast.LENGTH_LONG);
                            toast2.setGravity(Gravity.CENTER, 0, 0);
                            toast2.show();
                            return null;
                        });


        // Load the face regions renderable.
        // This is a skinned model that renders 3D objects mapped to the regions of the augmented face.
        ModelRenderable.builder()
                .setSource(MainActivity.this, R.raw.fox_face)
                .build()
                .thenAccept(
                        modelRenderable -> {
                            faceRegionsRenderable = modelRenderable;
                            modelRenderable.setShadowCaster(false);
                            modelRenderable.setShadowReceiver(false);
                        });
        //Load the texture of the model
        Texture.builder()
                .setSource(MainActivity.this, R.drawable.fox_face_mesh_texture)
                .build()
                .thenAccept(texture -> faceMeshTexture = texture);

        //Clicking the button to apply the model on face
        modelbtn.setOnClickListener(v -> {
                filterClickCount++;
        });

        //Creating the AR Scene for the face
        ArSceneView sceneView = faceArFragment.getArSceneView();
        sceneView.setCameraStreamRenderPriority(Renderable.RENDER_PRIORITY_FIRST);
        Scene scene = sceneView.getScene();
        scene.addOnUpdateListener(
                (FrameTime frameTime) -> {
                    if(rearFragment.getView().getVisibility()==View.GONE) {
                        Log.i("frontUpdate",""+filterClickCount);
                        if (faceRegionsRenderable == null) {
                            return;
                        }

                        Collection<AugmentedFace> faceList =
                                sceneView.getSession().getAllTrackables(AugmentedFace.class);

                        // Make new AugmentedFaceNodes for any new faces.
                        if (filterClickCount % 2 == 1) {
                            for (AugmentedFace face : faceList) {
                                if (!faceNodeMap.containsKey(face)) {
                                    AugmentedFaceNode faceNode = new AugmentedFaceNode(face);
                                    faceNode.setParent(scene);
                                    faceNode.setFaceRegionsRenderable(faceRegionsRenderable);
                                    faceNodeMap.put(face, faceNode);
                                }
                            }

                            // Remove any AugmentedFaceNodes associated with an AugmentedFace that stopped tracking.
                            Iterator<Map.Entry<AugmentedFace, AugmentedFaceNode>> iter =
                                    faceNodeMap.entrySet().iterator();
                            while (iter.hasNext()) {
                                Map.Entry<AugmentedFace, AugmentedFaceNode> entry = iter.next();
                                AugmentedFace face = entry.getKey();
                                if (face.getTrackingState() == TrackingState.STOPPED) {
                                    AugmentedFaceNode faceNode = entry.getValue();
                                    faceNode.setParent(null);
                                    iter.remove();
                                }
                            }
                            modelbtn.setText("ON");
                        }
                        //to remove the face filter
                        else {
                            Iterator<Map.Entry<AugmentedFace, AugmentedFaceNode>> iter =
                                    faceNodeMap.entrySet().iterator();
                            while (iter.hasNext()) {
                                Map.Entry<AugmentedFace, AugmentedFaceNode> entry = iter.next();
                                AugmentedFaceNode faceNode = entry.getValue();
                                faceNode.setParent(null);
                                iter.remove();
                            }
                            modelbtn.setText("OFF");
                        }
                    }
                });

        rearFragment.getArSceneView().getScene().addOnUpdateListener(new Scene.OnUpdateListener() {
            @Override
            public void onUpdate(FrameTime frameTime) {
                if (rearFragment.getView().getVisibility() == View.VISIBLE) {

                    Log.i("rearUpdate",""+filterClickCount);
                    if (filterClickCount % 2 == 1) {
                        rearFragment.setOnTapArPlaneListener(
                                (HitResult hitResult, Plane plane, MotionEvent motionEvent1) -> {
                                    if (andyRenderable == null) {
                                        return;
                                    }
                                    // Create the Anchor.
                                    Anchor anchor = hitResult.createAnchor();
                                    AnchorNode anchorNode = new AnchorNode(anchor);
                                    anchorNode.setParent(rearFragment.getArSceneView().getScene());

                                    // Create the transformable andy and add it to the anchor.
                                    TransformableNode andy = new TransformableNode(rearFragment.getTransformationSystem());
                                    andy.setParent(anchorNode);
                                    andy.setRenderable(andyRenderable);

                                });
                        r.setVisibility(View.VISIBLE);
                        bk.setVisibility(View.VISIBLE);
                        b.setVisibility(View.VISIBLE);
                        y.setVisibility(View.VISIBLE);
                        w.setVisibility(View.VISIBLE);
                        modelbtn.setText("ON");
                    } else {
                        List<Node> children = new ArrayList<>(rearFragment.getArSceneView().getScene().getChildren());
                        for (Node node : children) {
                            if (node instanceof AnchorNode) {
                                if (((AnchorNode) node).getAnchor() != null) {
                                    ((AnchorNode) node).getAnchor().detach();
                                }
                            }
                            if (!(node instanceof Camera) && !(node instanceof Sun)) {
                                node.setParent(null);
                            }
                        }
                        r.setVisibility(View.GONE);
                        bk.setVisibility(View.GONE);
                        b.setVisibility(View.GONE);
                        y.setVisibility(View.GONE);
                        w.setVisibility(View.GONE);
                        modelbtn.setText("OFF");
                    }
                }
            }
        }
        );
        modelColor();
        capture();
        gallery();
        flip();
    }

    private void modelColor() {
            r.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(rearFragment.getView().getVisibility()==View.VISIBLE) {
                        ModelRenderable renderableCopy = andyRenderable.makeCopy();
                        Material changedMaterial = renderableCopy.getMaterial().makeCopy();
                        changedMaterial.setFloat3("baseColorTint", new Color(android.graphics.Color.RED));
                        andyRenderable.setMaterial(changedMaterial);
                    }
                }
            });
            w.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(rearFragment.getView().getVisibility()==View.VISIBLE) {
                        ModelRenderable renderableCopy = andyRenderable.makeCopy();
                        Material changedMaterial = renderableCopy.getMaterial().makeCopy();
                        changedMaterial.setFloat3("baseColorTint", new Color(android.graphics.Color.WHITE));
                        andyRenderable.setMaterial(changedMaterial);
                    }
                }
            });
            bk.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(rearFragment.getView().getVisibility()==View.VISIBLE) {
                        ModelRenderable renderableCopy = andyRenderable.makeCopy();
                        Material changedMaterial = renderableCopy.getMaterial().makeCopy();
                        changedMaterial.setFloat3("baseColorTint", new Color(android.graphics.Color.BLACK));
                        andyRenderable.setMaterial(changedMaterial);
                    }
                }
            });
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(rearFragment.getView().getVisibility()==View.VISIBLE) {
                        ModelRenderable renderableCopy = andyRenderable.makeCopy();
                        Material changedMaterial = renderableCopy.getMaterial().makeCopy();
                        changedMaterial.setFloat3("baseColorTint", new Color(android.graphics.Color.BLUE));
                        andyRenderable.setMaterial(changedMaterial);
                    }
                }
            });
            y.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(rearFragment.getView().getVisibility()==View.VISIBLE) {
                        ModelRenderable renderableCopy = andyRenderable.makeCopy();
                        Material changedMaterial = renderableCopy.getMaterial().makeCopy();
                        changedMaterial.setFloat3("baseColorTint", new Color(android.graphics.Color.YELLOW));
                        andyRenderable.setMaterial(changedMaterial);
                    }
                }
            });
    }

    private Vector3 getScreenCenter(ArFragment rearFragment) { return new Vector3(rearFragment.getView().getWidth() / 2f, rearFragment.getView().getHeight() / 2f, 0f); }

    void capture(){
        capbtn.setOnClickListener(v -> {

            ArSceneView view;
            //capture rear camera image
            if(rearFragment.getView().getVisibility()==View.GONE){
                view=faceArFragment.getArSceneView();
            }
            //capture front camera image
            else{
                view=rearFragment.getArSceneView();
                rearFragment.getArSceneView().getPlaneRenderer().setVisible(false);
            }
            //Blink on capturing image

            AlphaAnimation animation1 = new AlphaAnimation(0.2f, 1.0f);
            animation1.setDuration(400);
            animation1.setFillAfter(true);
            v.startAnimation(animation1);


            // Create a bitmap the size of the scene view.
            final Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(),
                    Bitmap.Config.ARGB_8888);

            // Create a handler thread to offload the processing of the image.
            final HandlerThread handlerThread = new HandlerThread("PixelCopier");
            handlerThread.start();
            // Make the request to copy.
            PixelCopy.request(view, bitmap, (copyResult) -> {
                if (copyResult == PixelCopy.SUCCESS) {
                    try {
                        saveBitmapToDisk(bitmap);
                    } catch (IOException e) {
                        Toast toast = Toast.makeText(MainActivity.this, e.toString(),
                                Toast.LENGTH_LONG);
                        toast.show();
                        return;
                    }
                } else {
                    Toast toast = Toast.makeText(MainActivity.this,
                            "Failed to copyPixels: " + copyResult, Toast.LENGTH_LONG);
                    toast.show();
                }
                handlerThread.quitSafely();
            }, new Handler(handlerThread.getLooper()));
        });
    }
    //to view the saved images in a grid view
    void gallery(){
        climg.setOnClickListener(v -> {
            Intent intent=new Intent(v.getContext(),Gallery.class);
            Bundle bundle=new Bundle();
            bundle.putString("file",getBatchDirectoryName());
            intent.putExtras(bundle);
            startActivity(intent);
        });
    }


    //to switch between front and rear camera
    private void flip() {
        flipbtn.setOnClickListener(v -> {
            //front to rear
            if(rearFragment.getView().getVisibility()==View.GONE){
                rearFragment.getView().setVisibility(View.VISIBLE);
                faceArFragment.getView().setVisibility(View.GONE);
                modelbtn.setText("OFF");
                filterClickCount=0;
            }
            //rear to front
            else{
                rearFragment.getView().setVisibility(View.GONE);
                filterClickCount=0;
                modelbtn.setText("OFF");
                faceArFragment.getView().setVisibility(View.VISIBLE);
            }
        });
    }

    //saving the captured image in storage
    private void saveBitmapToDisk(Bitmap bitmap) throws IOException {
        File pictureFile = new File(getBatchDirectoryName(), new Date().toString() + ".jpeg");
        try {
            FileOutputStream oStream = new FileOutputStream(pictureFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, oStream);
            oStream.flush();
            oStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //make a new directory for this app's images and save clicked images there
    private String getBatchDirectoryName() {
        String app_folder_path = Environment.getExternalStorageDirectory().toString()+"/ARCamApp";

        File dir = new File(app_folder_path);
        if(!dir.exists())
            dir.mkdir();
        return app_folder_path;
    }

    //check if permissions granted
    private boolean permissionsGranted() {
        for(String permission:permissionList){
            if(ContextCompat.checkSelfPermission(this, permission)!=PERMISSION_GRANTED)
                return false;
        }
        return true;
    }

    //prompt for permissions if not granted
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)!=PERMISSION_GRANTED)
            Toast.makeText(this,"Please grant the required permission.",Toast.LENGTH_LONG).show();
    }

}