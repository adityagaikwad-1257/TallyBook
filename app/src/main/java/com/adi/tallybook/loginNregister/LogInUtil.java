package com.adi.tallybook.loginNregister;

import static com.adi.tallybook.apputilities.FireStoreConstants.USERS;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.adi.tallybook.apputilities.AppUtils;
import com.adi.tallybook.apputilities.UserManager;
import com.adi.tallybook.models.User;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class LogInUtil {

    // send otp
    public static void verifyPhone(String phoneNumber, Activity activity, LogInInterface login, @Nullable PhoneAuthProvider.ForceResendingToken resendingToken){
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
                        .setPhoneNumber(phoneNumber)       // Phone number to verify
                        .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                        .setActivity(activity)          // Activity (for callback binding)
                        .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                            @Override
                            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                                if (login != null) login.onVerificationCompleted(phoneAuthCredential);
                            }

                            @Override
                            public void onVerificationFailed(@NonNull FirebaseException e) {
                                if (login != null) login.onVerificationFailed(e);
                            }

                            @Override
                            public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                                if (login != null) login.onCodeSent(s, forceResendingToken, phoneNumber);
                            }
                        })
                        .setForceResendingToken(resendingToken)// OnVerificationStateChangedCallbacks
                        .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private static final String TAG = "aditya";

    // log in with phone
    public static void logInWithPhone(String verificationId, String otp, LogInSuccess success, LogInFailed fail){
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, otp);

        FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnSuccessListener(authResult -> {
                    Log.d(TAG, "logInWithPhone: successful log in ");

                    success.success(authResult);

                }).addOnFailureListener(e -> {
                    Log.d(TAG, "logInWithPhone: failed log in" + e.getLocalizedMessage());

                    fail.fail(e);

                });
    }

    public static void uploadUserImage(Context context, @NonNull Uri file, @NonNull String uid, @NonNull SuccessListener success, @NonNull FailureListener failed){
        StorageReference storageReference = FirebaseStorage.getInstance().getReference()
                .child(AppUtils.USER_PROFILE_IMAGES).child(uid);

        storageReference.putFile(file)
                .continueWithTask(task -> {
                    // when file uploading is completed
                    if (!task.isSuccessful()){
                        // file upload was not successful
                        throw (task.getException() != null)?task.getException(): new Exception("Upload Task was incomplete due to some anonymous reason");
                    }

                    // file upload was successful
                    // thus now getting the download url
                    Log.d(TAG, "uploadUserImage: file uploaded successfully" );
                    return storageReference.getDownloadUrl();
                })
                .addOnCompleteListener(task -> {
                    // when we get the
                    if (!task.isSuccessful()){
                        // something gone wrong while either uploading image or getting the reference of the image

                        String error_msg = "uploadUserImage: ****ERROR**** after uploading user profile image error while getting the reference \nerror msg : ";
                        error_msg += (task.getException() != null)?task.getException().getMessage():"null";
                        Log.d(TAG,  error_msg);

                        // failed call back
                        failed.failed(task.getException());
                        return;
                    }

                    // successfully got the url to the image uploaded
                    Log.d(TAG, "uploadUserImage: successfully got uploaded user image the url ");

                    // success call back

                    success.success(task.getResult().toString());
                });

    }

    // save details to fire store
    public static void saveUserDetails(@Nullable Context context,  @NonNull User user, @NonNull VoidSuccessListener success, @NonNull FailureListener failure){
        DocumentReference user_doc = FirebaseFirestore.getInstance().collection(USERS)
                .document(user.getUser_fid());

        // user on tally book flag
        user.setOnTallyBook(true);

        user_doc.set(user)
                .addOnSuccessListener(v -> {
                    // user data saved successfully to firestore

                    // saving it locally
                    UserManager.getInstance(context).setUserDetails(user, true);

                    success.success();
                }).addOnFailureListener(failure::failed);
    }

    // get user details with specified uid
    public static void getUserDetails(@NonNull String uid, @NonNull GetUserSuccess success, @NonNull FailureListener failureListener){
        FirebaseFirestore.getInstance()
                .collection(USERS)
                .document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    User user = documentSnapshot.toObject(User.class);

                    success.success(user);
                })
                .addOnFailureListener(failureListener::failed);
    }

    public static void signOut(Context context){
        FirebaseAuth.getInstance().signOut();
        UserManager.getInstance(context).setUserDetails(new User(), false);

        // deleting user image
        File file = new File(context.getFilesDir(), UserManager.USER_PROFILE_IMAGE);
        file.deleteOnExit();
    }

    @FunctionalInterface
    public interface GetUserSuccess{
        void success(User user);
    }

    @FunctionalInterface
    public interface VoidSuccessListener{
        void success();
    }

    @FunctionalInterface
    public interface SuccessListener{
        void success(String imageUrl);
    }

    @FunctionalInterface
    public interface FailureListener{
        void failed(Exception e);
    }

    @FunctionalInterface
    public interface LogInSuccess{
        void success(AuthResult authResult);
    }

    @FunctionalInterface
    public interface LogInFailed{
        void fail(Exception e);
    }
}
