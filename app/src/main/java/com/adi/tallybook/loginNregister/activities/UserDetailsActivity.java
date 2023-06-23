package com.adi.tallybook.loginNregister.activities;

import static com.adi.tallybook.apputilities.AppUtils.AVATARS;
import static com.adi.tallybook.apputilities.AppUtils.isConnectedToInternet;
import static com.adi.tallybook.apputilities.FireStoreConstants.FIREBASE_UID;
import static com.adi.tallybook.loginNregister.LogInInterface.PHONE_KEY;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.adi.tallybook.R;
import com.adi.tallybook.apputilities.UserManager;
import com.adi.tallybook.apputilities.dialogutils.ErrorDialog;
import com.adi.tallybook.apputilities.dialogutils.LoadingDialog;
import com.adi.tallybook.dashboard.HomeActivity;
import com.adi.tallybook.databinding.SelectAvatarDialogBinding;
import com.adi.tallybook.databinding.ToastLayoutBinding;
import com.adi.tallybook.databinding.UserBasicDetailsBinding;
import com.adi.tallybook.loginNregister.LogInUtil;
import com.adi.tallybook.models.User;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserDetailsActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "aditya";

    UserBasicDetailsBinding binding;
    
    LoadingDialog loadingDialog;

    ActivityResultLauncher<String> imagePicker;  // to select user image from their device

    private boolean isAvatar = false; // if user has selected avatar instead of image from their device
    private boolean isImageSelected = false; // if user has selected either Avatar or image from device

    private Dialog selectionDialog; // dialog for user to select user image or avatar

    private SelectAvatarDialogBinding selectionBinding; // image selection layout

    private ArrayList<String> avatarList; // avatar list from firebase
    private ArrayList<CircleImageView> avatarViewList; // 6 avatars on selection dialog

    private User user; // user details to be stored in this object directly

    private Uri selectedImageUri;

    private static final String USER_KEY = "user_key";
    private static final String SELECTED_URI = "selected_uri";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = UserBasicDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (savedInstanceState != null){
            // it was a configuration change
            user = (User) savedInstanceState.getSerializable(USER_KEY);

            if (user == null) user = new User(); // if user still null

            selectedImageUri = savedInstanceState.getParcelable(SELECTED_URI);
        }else{
            // fresh activity
            user = new User();

            // getting phone number from bundle
            user.setPhone_number(getIntent().getStringExtra(PHONE_KEY));
            
            // getting firebase uid from bundle
            user.setUser_fid(getIntent().getStringExtra(FIREBASE_UID));
        }

        initViews();

        initSelectionDialog();

        clickEvents();
    }

    /*
        saving instance as configuring change is undergoing
     */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putSerializable(USER_KEY, user);
        outState.putParcelable(SELECTED_URI, selectedImageUri);
        super.onSaveInstanceState(outState);
    }

    /*
        initialize views
     */
    private void initViews(){
        // configuring isAvatar and isImageSelected flags after configuration change
        if (selectedImageUri != null){
            isImageSelected = true;
            isAvatar = false;

            binding.selectImageTv.setVisibility(View.GONE);
            Picasso.get()
                    .load(selectedImageUri)
                    .placeholder(R.color.light_gray)
                    .fit().centerCrop()
                    .into(binding.userImage);
        }else if (user.getUser_image_url() != null && !user.getUser_image_url().isEmpty()){
            isImageSelected = true;
            isAvatar = true;

            binding.selectImageTv.setVisibility(View.GONE);
            Picasso.get()
                    .load(user.getUser_image_url())
                    .placeholder(R.color.light_gray)
                    .fit().centerCrop()
                    .into(binding.userImage);

        }else{
            isImageSelected = false;
            isAvatar = false;

            binding.selectImageTv.setVisibility(View.VISIBLE);
        }

        // initializing members variables
        avatarList = new ArrayList<>();
        avatarViewList = new ArrayList<>();

        // to way data binding
        binding.setUser(user);
        
        loadingDialog = LoadingDialog.create(this);

        // registering image picker launcher
        imagePicker = registerForActivityResult(new ActivityResultContracts.GetContent(),
                result -> {
                    if (result != null){
                        selectionDialog.dismiss();

                        binding.selectImageTv.setVisibility(View.GONE); // disabling select image prompt to user

                        Picasso.get()
                                .load(result)
                                .fit()
                                .centerCrop()
                                .into(binding.userImage);

                        isAvatar = false; // as image is selected from user's device
                        isImageSelected = true;

                        // as selected image is now from user device
                        // thus making user image = null
                        selectedImageUri = result;
                        user.setUser_image_url(null);
                    }
                });
    }

    /*
        register click events
     */
    private void clickEvents(){

        binding.info.setOnClickListener(this::showInfoToast);

        binding.userImageLayout.setOnClickListener(v -> selectionDialog.show()); // showing user dialog to select either avatar or image from their device

        binding.saveDetails.setOnClickListener(v -> saveUserDetails());
    }

    /*
        validate user user input details and save data to firebase
     */
    private void saveUserDetails(){
        if (!isConnectedToInternet(this)){
            // not connected to internet
            return;
        }

        if (isImageSelected & validateUserInput()){
            // user selected an image
            // and all inputs are valid

            if (isAvatar && user.getUser_image_url() != null){
                // image selected is Avatar

                // saving avatar locally
                UserManager.getInstance(this).saveImage(user.getUser_image_url());
                
                // good to save user details locally and to fire store
                saveUserDetails(user);
            }else if (selectedImageUri != null){
                // image selected from device
                // need to upload to firebase storage first

                loadingDialog.show("Looking good!");

                LogInUtil.uploadUserImage(this, selectedImageUri, user.getUser_fid(),
                        imageUrl -> {
                            // image upload success callback

                            // saving user image locally
                            UserManager.getInstance(this).saveImage(selectedImageUri);

                            user.setUser_image_url(imageUrl);
                            // go to save user details

                            saveUserDetails(user);
                        },
                        e -> {
                            // image upload failed callback
                            loadingDialog.dismiss();
                            Toast.makeText(this, "Image upload failed, Try again!", Toast.LENGTH_SHORT).show();
                        });
            }else{
                Toast.makeText(this, "something went wrong", Toast.LENGTH_SHORT).show();

                // reset user image view
                binding.selectImageTv.setVisibility(View.VISIBLE);
                binding.userImage.setImageResource(R.color.light_gray);

                // reset flags
                isImageSelected = false;
                isAvatar = false;
                user.setUser_image_url(null);
                selectedImageUri = null;

            }
        }else if (!isImageSelected){
            // user did not select an image
            selectionDialog.show();
        }
    }
    
    // calling util function
    // this function internally also locally saves the user details to shared pref
    private void saveUserDetails(User user){
        if (loadingDialog.isShowing()) loadingDialog.changeMessage("Saving details");
        else loadingDialog.show("Saving details");
        
        LogInUtil.saveUserDetails(this,
                user,
                () -> {
                    // success call back
                    loadingDialog.dismiss();

                    // good to go to HomeActivity

                    Intent intent = new Intent(this, HomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                    startActivity(intent);
                    finish();
                }, e -> {
                    // failure call back
                    loadingDialog.dismiss();
                    
                    String error_msg = "Something gone wrong while saving your details.\n";
                    error_msg += (e.getLocalizedMessage() == null)?"":e.getLocalizedMessage();
                    
                    ErrorDialog.create(this).show(R.raw.error, 
                            "SOMETHING WENT WRONG", 
                            error_msg,
                            "ok", null);
                });
    }

    /*
        validate details entered by user in edit boxes
     */
    private boolean validateUserInput() {
        boolean allOkay = true;

        if (user.getFirst_name() == null || !user.getFirst_name().trim().matches("^[a-zA-Z]+")){
            allOkay = false;
            binding.firstName.setError("Invalid Eg. Adi2 tya");
        }
        if (user.getLast_name() == null || !user.getLast_name().trim().matches("^[a-zA-Z]+")){
            allOkay = false;
            binding.lastName.setError("Invalid Eg. Gaik3. wad");
        }

        // email is optional
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()){
            user.setEmail(""); // don't want to insert null value to database
        }else if (!Patterns.EMAIL_ADDRESS.matcher(user.getEmail().trim()).matches()){
            // invalid email
            allOkay = false;
            binding.email.setError("Invalid email address");
        }

        return allOkay;
    }

    /*
        initializing dialog view for user to select profile image from available avatars or select image from device
     */
    private void initSelectionDialog(){
        selectionDialog = new Dialog(this);

        // inflating selection dialog layout
        selectionBinding = SelectAvatarDialogBinding.inflate(getLayoutInflater());

        // loading avatar views into avatarViewList
        avatarViewList.add(selectionBinding.avatar1);
        avatarViewList.add(selectionBinding.avatar2);
        avatarViewList.add(selectionBinding.avatar3);
        avatarViewList.add(selectionBinding.avatar4);
        avatarViewList.add(selectionBinding.avatar5);
        avatarViewList.add(selectionBinding.avatar6);

        selectionDialog.setContentView(selectionBinding.getRoot());
        selectionDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // click events
        selectionBinding.fromDevice.setOnClickListener(v -> imagePicker.launch("image/*"));

        // click event to every avatar image
        selectionBinding.avatar1.setOnClickListener(this);
        selectionBinding.avatar2.setOnClickListener(this);
        selectionBinding.avatar3.setOnClickListener(this);
        selectionBinding.avatar4.setOnClickListener(this);
        selectionBinding.avatar5.setOnClickListener(this);
        selectionBinding.avatar6.setOnClickListener(this);

        // loading avatar list from firebase
        FirebaseDatabase.getInstance().getReference()
                .child(AVATARS).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot avatar: snapshot.getChildren()){
                            avatarList.add(avatar.getValue(String.class));
                        }

                        loadAvatars();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.d(TAG, "onCancelled: avatar list");
                        loadAvatars();
                    }
                });

    }

    /*
        showing avatars from avatars list
     */
    private void loadAvatars() {
        selectionBinding.avatarLoading.setVisibility(View.GONE); // avatars loaded

        if (avatarList.size() > 0){
            // avatars found

            selectionBinding.avatarsLayout.setVisibility(View.VISIBLE);

            // loading avatars in avatar views of selection dialog

            for (int i = 0; i<avatarViewList.size(); i++){
                Picasso.get()
                        .load(avatarList.get(i))
                        .placeholder(R.color.light_gray)
                        .fit()
                        .centerCrop()
                        .into(avatarViewList.get(i));
            }

        }else{
            // no avatars
            selectionBinding.noAvatars.setVisibility(View.VISIBLE);
        }
    }

    /*
        avatar images in user image selection dialog click listener
     */
    @Override
    public void onClick(View v) {
        try{
            // getting tag of avatar selected
            int tag = Integer.parseInt(String.valueOf(v.getTag()));

            if (tag < avatarList.size()){

                selectionDialog.dismiss();

                binding.selectImageTv.setVisibility(View.GONE);

                Picasso.get()
                        .load(avatarList.get(tag))
                        .placeholder(R.color.light_gray)
                        .fit().centerCrop()
                        .into(binding.userImage);

                isAvatar = true; // as avatar has been selected
                isImageSelected = true;

                // as selected image is now avatar
                user.setUser_image_url(avatarList.get(tag));
                selectedImageUri = null;

            }else throw new Exception("tag of avatar image view > avatar list size\ntag : " + tag +
                    "\navatar list size : " + avatarList.size());

        }catch (Exception e){
            Log.d(TAG, "onClick: avatar image click " + e.getMessage());
            Toast.makeText(this, "Avatar not available :(", Toast.LENGTH_SHORT).show();
        }

    }

    /*
            show user that their data is safe with us with toast
         */
    private void showInfoToast(View view){
        Toast toast = new Toast(getApplicationContext());

        // inflating custom toast layout
        ToastLayoutBinding toastBinding = ToastLayoutBinding.inflate(getLayoutInflater());
        toastBinding.message.setText(getString(R.string.data_safe_msg));

        toast.setGravity(Gravity.TOP|Gravity.END, 0, view.getBottom());
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(toastBinding.getRoot());

        toast.show();
    }

}