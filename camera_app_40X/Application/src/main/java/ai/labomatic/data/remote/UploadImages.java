package ai.labomatic.data.remote;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import ai.labomatic.data.local.ClientsDatabaseHandler;
import ai.labomatic.data.model.Folder;
import ai.labomatic.data.model.Image;
import ai.labomatic.R;
import ai.labomatic.ui.LabomaticCamera.CreatePatient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnPausedListener;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class UploadImages extends Activity {

    // Constants
    private static final String TAG = "UploadImagesActivity::";
    public String FOLDER_NAME = null;

    // UI elements
    public Button uploadFilesButton;
    public Button signInButton;

    // Database handler instance
    public ClientsDatabaseHandler db;

    // Firebase
    private FirebaseAuth mAuth;

    // Progressbar
    public ProgressBar progressBar;

    // Firebase storage
    public FirebaseStorage storage;
    public StorageReference storageRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_images);
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // Intent information
        FOLDER_NAME = getIntent().getStringExtra("folderName");
        Log.d(TAG, "Folder to read: " + FOLDER_NAME);
        // UI elements
        progressBar = (ProgressBar) findViewById(R.id.progress_bar_uploading);
        progressBar.setProgress(0);
        progressBar.setMax(100);

        uploadFilesButton = (Button) findViewById(R.id.read_folders_button);
        uploadFilesButton.setEnabled(false);
        signInButton = (Button) findViewById(R.id.sign_in_button);
        // Firebase auth
        mAuth = FirebaseAuth.getInstance();
        // Firebase storage
        storage = FirebaseStorage.getInstance();
        storageRef = storage.getReference();
        // Init db
        db = new ClientsDatabaseHandler(this);
        // Listeners
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Authenticate to firebase
                signInToFirebase("lozuwaucb@gmail.com", "lozapython35");
            }
        });

        uploadFilesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // List of Images
                List<Image> imgs = db.readImagesByFolderName(FOLDER_NAME);
                if (imgs.size() == 0){
                    Log.e(TAG, "No images to upload");
                    /**
                     * TODO: Add message telling the user that the machine has failed and will not
                     * perform the diagnostic.
                    */
                    Intent createPatientActivity = new Intent(UploadImages.this, CreatePatient.class);
                    startActivity(createPatientActivity);
                } else{
                    // Upload each image
                    for (Image img: imgs){
                        uploadImageWithProgressBar(img);
                    }
                }
            }
        });
    }

    @Override
    public void onStart(){
        super.onStart();
    }

    public void uploadImage(Image img){
        // Variables
        final String imageName = img.getImageName();
        final String folderName = img.getFolderName();
        final String pathToFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                + File.separator + folderName + File.separator + imageName;
        Log.d(TAG, imageName + "," + folderName);
        // Set a Uri
        Uri file = Uri.fromFile(new File(pathToFile));
        // Create a reference
        StorageReference imgRef = storageRef.child("images/"+file.getLastPathSegment());
        UploadTask uploadTask = imgRef.putFile(file);
        // Register observers to listen for when the download is done or if it fails
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.i(TAG, "Failed to upload image.");
//                showToast("Failed to upload image");
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                // Get download URI
                Uri downloadUrl = taskSnapshot.getDownloadUrl();
                Log.i(TAG, "Success " + downloadUrl.toString());
                // Remove the file physically
                File imageFile = new File(pathToFile);
                boolean deleted = imageFile.delete();
                if (deleted){
                    Log.i(TAG, "File succesfully deleted.");
                    // Given the file was removed, now delete its field from the db
                    db.deleteImage(new Image(imageName, folderName));
                } else{
                    Log.i(TAG, "File could not be deleted.");
                }
            }
        });
    }

    public void uploadImageWithProgressBar(Image img) {
        // Variables
        final String imageName = img.getImageName();
        final String folderName = img.getFolderName();
        final String pathToFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                + File.separator + folderName + File.separator + imageName;
        Log.d(TAG, imageName + "," + folderName);
        // Set a Uri
        Uri file = Uri.fromFile(new File(pathToFile));
        // Create a storage reference
        StorageReference imgRef = storageRef.child("images/"+file.getLastPathSegment());
        UploadTask uploadTask = imgRef.putFile(file);
        // Register observers to listen for when the download is done or if it fails
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.e(TAG, "Failed to upload image.");
                // showToast("Failed to upload image");
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type,
                // and download URL.
                // Get download URI
                Uri downloadUrl = taskSnapshot.getDownloadUrl();
                Log.i(TAG, "Success " + downloadUrl.toString());
                // Remove the file physically
                File imageFile = new File(pathToFile);
                boolean deleted = imageFile.delete();
                if (deleted){
                    Log.i(TAG, "File succesfully deleted.");
                    // Given the file was removed, now delete its field from the db
                    db.deleteImage(new Image(imageName, folderName));
                } else{
                    Log.i(TAG, "File could not be deleted.");
                }
                // Check if the database is finally clean, if so then start create patient activity
                List<Image> imgs = db.readImagesByFolderName(FOLDER_NAME);
                // If there are no more images associated with the current folder,
                // then start CreatePatientActivity and remove the folder from the db.
                if (imgs.size() == 0){
                    // Remove folder from db
                    db.deleteFolder(new Folder(FOLDER_NAME));
                    // Start CreatePatientActivity
                    Intent createPatientActivity = new Intent(UploadImages.this,
                            CreatePatient.class);
                    startActivity(createPatientActivity);
                } else{
                    // Keep uploading the images
                    Log.d(TAG, "Images that are still missing: " + String.valueOf(imgs.size()));
                }
            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                showToast("Upload is " + String.valueOf(progress) + "% done");
                int currentprogress = (int) progress;
                progressBar.setProgress(currentprogress);
            }
        }).addOnPausedListener(new OnPausedListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onPaused(UploadTask.TaskSnapshot taskSnapshot) {
                System.out.println("Upload is paused");
            }
        });
    }

    public List<Folder> readFolders(boolean show){
        List<Folder> allFolders = new ArrayList<Folder>();
        allFolders = db.readAllFolders();
        if (show) {
            for (Folder fold : allFolders) {
                String log = "Id: " + fold.getId() + " Folder name: " + fold.getfolderName();
                // showToast(log);
            }
        } else {
            // do nothing
        }
        return allFolders;
    }

    public void showToast(String message){
        Toast.makeText(UploadImages.this, message, Toast.LENGTH_SHORT).show();
    }

    public void signInToFirebase(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("DEBUG:AUTH:FIREBASE", "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            uploadFilesButton.setEnabled(true);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("DEBUG:AUTH:FIREBASE", "signInWithEmail:failure", task.getException());
                            Toast.makeText(UploadImages.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                            uploadFilesButton.setEnabled(false);
                        }
                    }
                });
    }

}
