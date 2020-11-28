package backend.util.database;

import android.net.Uri;
import android.util.Pair;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;

public class SetDatabase {
    private DatabaseReference database = FirebaseDatabase.getInstance().getReference();
    private FirebaseStorage storage = FirebaseStorage.getInstance();
    private StorageReference storageRef = storage.getReference();

    public static Pair<String,String> parseName(String filename) {
        int i = filename.lastIndexOf('.');
        if (i > 0 && i < filename.length() - 1) {
            return new Pair<String, String>(filename.substring(0,i), filename.substring(i+1));
        }
        else return null;
    }

    //add a new user to the database
    public void addNewUser(UserProfile userProfile) {
        //System.out.println(database == null);
        DatabaseReference usersRef = database.child("user");
        usersRef.child(userProfile.user_id).setValue(userProfile);
    }

    //add a new bottle to the database
    public void addNewBottle(Bottle_back this_bottle){
        DatabaseReference bottlesRef = database.child("bottle");
        bottlesRef.child(String.valueOf(this_bottle.bottleID)).setValue(this_bottle);
    }

    public void uploadAvatars(String user_id, Uri file) {
        StorageReference targetRef =  storageRef.child("avatars/" + user_id + "/" + file.getLastPathSegment());

        UploadTask uploadTask = targetRef.putFile(file);

        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {

            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

            }
        });
    }

    public void uploadBottleFile(String path, String bottle_id, boolean isVideo) {
        Uri file = Uri.fromFile(new File(path));
        StorageReference targetRef = null;
        if (isVideo) {
            targetRef = storageRef.child("videos/" + bottle_id + "/" + file.getLastPathSegment());
        }
        else {
            targetRef = storageRef.child("images/" + bottle_id + "/" + file.getLastPathSegment());
        }
        UploadTask uploadTask = targetRef.putFile(file);

        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {

            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

            }
        });
    }

    public File downloadBottleFile(String bottle_id, String file_name, boolean isVideo) throws IOException {
        StorageReference targetRef = null;
        Pair<String, String> fileName = parseName(file_name);
        if (fileName == null) {
            return null;
        }
        if (isVideo) {
            targetRef = storageRef.child("videos/" + bottle_id + "/" + file_name);
        }
        else {
            targetRef = storageRef.child("images/" + bottle_id + "/" + file_name);
        }
        File localFile = File.createTempFile(bottle_id + fileName.first, fileName.second);
        targetRef.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                // Local temp file has been created
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
            }
        });
        return localFile;
    }
}
