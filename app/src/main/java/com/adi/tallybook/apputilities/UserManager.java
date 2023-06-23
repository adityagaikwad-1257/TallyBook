package com.adi.tallybook.apputilities;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.adi.tallybook.models.User;
import com.google.firebase.storage.FirebaseStorage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class UserManager {
    private static final String TAG = "aditya";

    private static final String USER_PREF_NAME = "user_details";

    // user details constants
    public static final String FIRST_NAME = "first_name";
    public static final String LAST_NAME = "last_name";
    public static final String PHONE_NUMBER = "phone_number";
    public static final String EMAIL = "email_id";
    public static final String IS_LOGGED_IN = "is_logged_in";
    public static final String USER_FIREBASE_ID = "fid";
    public static final String USER_PROFILE_IMAGE = "user_profile_img.jgp";

    private final SharedPreferences sharedPreferences;

    private final Context context;

    private UserManager(Context context){
        sharedPreferences = context.getSharedPreferences(USER_PREF_NAME, Context.MODE_PRIVATE);
        this.context = context;
    }

    public static UserManager getInstance(Context context){
        return new UserManager(context);
    }

    public void setData(String key, String value){
        sharedPreferences.edit()
                .putString(key, value)
                .apply();
    }

    @Nullable
    public String getString(String key){
        return sharedPreferences.getString(key, null);
    }


    public void setBooleanData(String key, boolean value){
        sharedPreferences.edit()
                .putBoolean(key, value)
                .apply();
    }

    public boolean getBooleanValues(String key){
        return sharedPreferences.getBoolean(key, false);
    }

    public boolean isUserLoggedIn(){
        return getBooleanValues(IS_LOGGED_IN);
    }

    public void setUserDetails(@NonNull User user, boolean isLoggedIn){
        setData(FIRST_NAME, user.getFirst_name());
        setData(LAST_NAME, user.getLast_name());
        setData(PHONE_NUMBER, user.getPhone_number());
        setData(EMAIL, user.getEmail());
        setData(USER_FIREBASE_ID, user.getUser_fid());
        setBooleanData(IS_LOGGED_IN, isLoggedIn);
        Log.d(TAG, "setUserDetails: user details saved locally");
    }

    // saves image from the image url of the firebase into local file
    public void saveImage(String imageUrl){
        File file = new File(context.getFilesDir(), USER_PROFILE_IMAGE);
        // if file not exists this code creates one

        // downloading file from Firebase storage

        FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl).getFile(file)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()){
                        String error_msg = "Unable to save user image into internal storage due to ";
                        error_msg += (task.getException() != null)?task.getException().getLocalizedMessage():" some anonymous reason";
                        Log.d(TAG, "saveImage: error " + error_msg);
                        return;
                    }

                    Log.d(TAG, "saveImage: User image saved locally");
                }).addOnFailureListener(e -> {
                    String error_msg = "Unable to save user image into internal storage due to " + e.getLocalizedMessage();
                    Log.d(TAG, "saveImage: error " + error_msg);
                });
    }

    // saves image locally in the internal storage of the device
    public void saveImage(Uri result) {
        try (FileOutputStream fos = context.openFileOutput(USER_PROFILE_IMAGE, Context.MODE_PRIVATE)) {
            // openFileOutput() creates new file if already doesn't exist

            byte[] bytes = readBytes(context.getContentResolver().openInputStream(result));

            fos.write(bytes);
            Log.d(TAG, "saveToInternals: user image file saved locally");
        }catch (IOException e){
            Log.d(TAG, "saveImage: unable to save file locally due to " + e.getMessage());
        }
    }

    // return user image file stored locally in internal storage of app
    public File getUserImage(){
        File userImageFile = new File(context.getFilesDir(), USER_PROFILE_IMAGE);
        if (userImageFile.exists()) return userImageFile;
        else return null;
    }

    private byte[] readBytes(InputStream inputStream) throws IOException {
        // this dynamically extends to take the bytes you read
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

        // this is storage overwritten on each iteration with bytes
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        // we need to know how may bytes were read to write them to the byteBuffer
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }

        // and then we can return your byte array.
        return byteBuffer.toByteArray();
    }
}
