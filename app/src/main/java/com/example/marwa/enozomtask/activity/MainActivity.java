package com.example.marwa.enozomtask.activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.ButtonBarLayout;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;

import com.example.marwa.enozomtask.R;
import com.example.marwa.enozomtask.interfaces.FileChooser;
import com.example.marwa.enozomtask.model.Message;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements FileChooser {
    private static final int PICK_IMAGE_REQUEST = 234;
    private static final int PICK_VIDEO_REQUEST = 567;
    private static final int CAMERA_REQUEST = 890;
    private static final int MY_PERMISSIONS_REQUEST = 2;
    private static final int MY_PERMISSIONS_REQUEST_Camera = 3;
      int permissionCAMERA;
      int permissionStorage;

    private PopupWindow mPopupWindow;
    private RelativeLayout mRelativeLayout;
    private Uri filePath;
    private RelativeLayout attachCorrect;
    private EditText subject;
    private EditText message;
    private ButtonBarLayout send;
    private LayoutInflater inflater;
    private View customView;
    private TextView attachFile;
    private ImageButton cancel;
    String fileName;
    FirebaseStorage storage;
    StorageReference storageReference;
    FirebaseOptions opts;
    Uri downLoadUri;
    String fileType;
    StorageReference riversRef;

    private DatabaseReference mDatabase;
    private DatabaseReference myDatabaseReference;

    int storedPreference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        attachCorrect = (RelativeLayout) findViewById(R.id.done);
        cancel = (ImageButton) findViewById(R.id.cancel);
        subject = (EditText) findViewById(R.id.subject);
        message = (EditText) findViewById(R.id.message);
        attachFile = (TextView) findViewById(R.id.attachfile);
        mRelativeLayout = (RelativeLayout) findViewById(R.id.r1);
        send = (ButtonBarLayout) findViewById(R.id.send);
        final ButtonBarLayout attach = (ButtonBarLayout) findViewById(R.id.attach);
        inflater = (LayoutInflater) getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);
        customView = inflater.inflate(R.layout.custom_popup, null);
        mPopupWindow = new PopupWindow(customView, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        mPopupWindow.setAnimationStyle(R.style.PopupWindowAnimation);
        mPopupWindow.setOutsideTouchable(true);



        storage = FirebaseStorage.getInstance();
        opts = FirebaseApp.getInstance().getOptions();

        Log.i("getBucket", "Bucket = " + opts.getStorageBucket());
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

        storageReference = storage.getReferenceFromUrl("gs://enozomtask-3c261.appspot.com");

        mDatabase = FirebaseDatabase.getInstance().getReference();
        myDatabaseReference = FirebaseDatabase.getInstance().getReference("messages");

        //---------------------------------cancel button ------------------------------------------------//
        cancel.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (downLoadUri != null) {
                    AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this);
                    builder1.setMessage("Do you want to delete Attachment?");
                    builder1.setCancelable(true);
                    builder1.setPositiveButton(
                            "Yes",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    StorageReference photoRef = storage.getReferenceFromUrl(downLoadUri.toString());
                                    photoRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            // File deleted successfully
                                            Log.i("sucess", "onSuccess: deleted file");
                                            attachCorrect.setVisibility(View.INVISIBLE);
                                            downLoadUri = null;
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception exception) {
                                            // Uh-oh, an error occurred!
                                            Log.i("fail", "onFailure: did not delete file");
                                        }
                                    });
                                    dialog.cancel();
                                }
                            });

                    builder1.setNegativeButton(
                            "No",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });
                    AlertDialog alert11 = builder1.create();
                    alert11.show();
                }
            }
        });
//--------------------------------------- send Button -------------------------------------------//
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isConnected(getApplicationContext())) {
                    Toast.makeText(getApplicationContext(), "send Message", Toast.LENGTH_LONG).show();
                    attachCorrect.setVisibility(View.INVISIBLE);
                    Message msg = new Message();
                    if (!subject.getText().toString().isEmpty()) {
                        msg.setMessageSubject(subject.getText().toString());
                    } else {
                        msg.setMessageSubject("Empty");

                    }
                    if (!message.getText().toString().isEmpty()) {
                        msg.setMessageContent(message.getText().toString());
                    } else {
                        msg.setMessageContent("Empty");
                    }
                    if (downLoadUri != null) {
                        msg.setAttachmentUrl(downLoadUri.toString());
                    } else {
                        msg.setAttachmentUrl("Empty");
                    }
                    myDatabaseReference.push().setValue(msg);
                    downLoadUri = null;
                    subject.setText("");
                    message.setText("");
                } else {
                    getConnctionCheckResult(getApplicationContext());
                }
            }
        });
//--------------------------------------- attach Button -------------------------------------------//
        attach.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= 21) {
                    mPopupWindow.setElevation(5.0f);
                }
                mPopupWindow.showAsDropDown(toolbar, 30, -30);
            }
        });
//--------------------------------------- gallery Button -------------------------------------------//

        ImageButton gallery = (ImageButton) customView.findViewById(R.id.gallery);
        gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fileType = "image";
                mPopupWindow.dismiss();
                Toast.makeText(getApplicationContext(), "Gallery", Toast.LENGTH_LONG).show();
                showFileChooser("image/*", PICK_IMAGE_REQUEST);
            }
        });
//--------------------------------------- camera Button -------------------------------------------//

        ImageButton camera = (ImageButton) customView.findViewById(R.id.camera);
        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fileType = "image";
                mPopupWindow.dismiss();
                Log.i("check",String.valueOf(permissionCAMERA == PackageManager.PERMISSION_GRANTED));
                permissionCAMERA = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA);

                if (permissionCAMERA == PackageManager.PERMISSION_GRANTED) {
                    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent, CAMERA_REQUEST);
                    Toast.makeText(getApplicationContext(), "camera", Toast.LENGTH_LONG).show();
                } else {
                    List<String> listPermissionsNeeded = new ArrayList<>();
                    listPermissionsNeeded.add(Manifest.permission.CAMERA);
                    ActivityCompat.requestPermissions(MainActivity.this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), MY_PERMISSIONS_REQUEST_Camera);
                }
            }
        });
        //--------------------------------------- video Button -------------------------------------------//
        ImageButton video = (ImageButton) customView.findViewById(R.id.video);
        video.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fileType = "video";
                mPopupWindow.dismiss();
                Toast.makeText(getApplicationContext(), "video", Toast.LENGTH_LONG).show();
                showFileChooser("video/*", PICK_VIDEO_REQUEST);
            }
        });
    }

    //-------------------------------show File Chooser-----------------------------------------//
    @Override
    public void showFileChooser(String type, int request) {
        Intent intent = new Intent();
        intent.setType(type);
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select"), request);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mPopupWindow.dismiss();
        if ((requestCode == PICK_IMAGE_REQUEST || requestCode == PICK_VIDEO_REQUEST || requestCode == CAMERA_REQUEST) && resultCode == RESULT_OK && data != null && data.getData() != null) {
            filePath = data.getData();
            permissionStorage = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);

            if (permissionStorage == PackageManager.PERMISSION_GRANTED) {

                String realPath = getPath(this, filePath);
                Log.i("FileRealPath", getPath(this, filePath));
                File f = new File(realPath);
                Log.i("fileSize", String.valueOf(f.length()));

                if (f.length() < (5 * 1048576)) {
                    fileName = realPath.substring(realPath.lastIndexOf("/") + 1);
                    if (isConnected(getApplicationContext())) {
                        uploadFile();

                    } else {
                        attachCorrect.setVisibility(View.INVISIBLE);
                        getConnctionCheckResult(getApplicationContext());
                    }

                } else {
                    Toast.makeText(getApplicationContext(), "File is too large", Toast.LENGTH_LONG).show();
                }
            } else {
                List<String> listPermissionsNeeded = new ArrayList<>();
                listPermissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
                ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), MY_PERMISSIONS_REQUEST);
            }
        }
    }

    //---------------------------ON Resume-------------------------------------//
    @Override
    protected void onResume() {
        super.onResume();
        getConnctionCheckResult(this);
    }

    //---------------------Check internet connection------------------------------//
    private static boolean isConnected(Context context) {
        boolean isConnected = false;

        ConnectivityManager connectivity = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo activeNetwork = connectivity.getActiveNetworkInfo();
            isConnected = activeNetwork != null &&
                    activeNetwork.isConnectedOrConnecting();
        }
        return isConnected;
    }

    //---------------------connection Result ------------------------------//
    private void getConnctionCheckResult(Context context) {

        Boolean statee = isConnected(context);
        if (!statee) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage("Connect to wifi or quit")
                    .setCancelable(false)
                    .setPositiveButton("Connect to WIFI", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                        }
                    })
                    .setNegativeButton("Quit", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        }
    }
    //-----------------------------Upload File --------------------------------------//
    private void uploadFile() {
        //if there is a file to upload
        if (filePath != null) {
            //displaying a progress dialog while upload is going on

            final ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Uploading");
            progressDialog.show();

            if (fileType.equals("image")) {
                riversRef = storageReference.child("images/image.jpg");
            } else {
                riversRef = storageReference.child("videos/video.mp4");
            }
            riversRef.putFile(filePath)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            //if the upload is successfull
                            //hiding the progress dialog
                            progressDialog.dismiss();
                            //and displaying a success toast
                            // Toast.makeText(getApplicationContext(), "File Uploaded " + taskSnapshot.getDownloadUrl(), Toast.LENGTH_LONG).show();
                            downLoadUri = taskSnapshot.getDownloadUrl();
                            attachFile.setText("Attachment: " + fileName);
                            attachCorrect.setVisibility(View.VISIBLE);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            //if the upload is not successfull
                            //hiding the progress dialog
                            progressDialog.dismiss();
                            progressDialog.setCanceledOnTouchOutside(false);
                            //and displaying error message
                            Toast.makeText(getApplicationContext(), exception.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                            //calculating progress percentage
                            double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                            //displaying percentage in progress dialog
                            progressDialog.setMessage("Uploaded " + ((int) progress) + "%...");
                        }
                    });
        }
        //if there is not any file
        else {
            Toast.makeText(getApplicationContext(), "Can't upload attachment", Toast.LENGTH_LONG).show();
            //you can display an error toast
        }

    }


//--------------------------------Get Real Path ------------------------------------//

    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }

        } finally {
            if (cursor != null)
                cursor.close();
        }

        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }
}
