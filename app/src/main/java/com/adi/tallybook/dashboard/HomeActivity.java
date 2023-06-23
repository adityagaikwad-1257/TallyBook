package com.adi.tallybook.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.adi.tallybook.R;
import com.adi.tallybook.apputilities.FireStoreConstants;
import com.adi.tallybook.apputilities.UserManager;
import com.adi.tallybook.loginNregister.LogInUtil;
import com.adi.tallybook.loginNregister.activities.LogInActivity;
import com.adi.tallybook.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.squareup.picasso.Picasso;

import java.io.File;

import de.hdodenhof.circleimageview.CircleImageView;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        String msg = "";

        UserManager userManager = UserManager.getInstance(this);

        msg += "Name : " + userManager.getString(UserManager.FIRST_NAME) + " " + userManager.getString(UserManager.LAST_NAME);
        msg += "\nPhone number : " + userManager.getString(UserManager.PHONE_NUMBER);

        ((TextView) findViewById(R.id.text_main)).setText(msg);

        File userImageFile = userManager.getUserImage();

        if (userImageFile != null){
            Picasso.get().load(userImageFile)
                    .placeholder(R.color.light_gray)
                    .fit().centerCrop()
                    .into((CircleImageView) findViewById(R.id.user_image_main));
        }else{
            Picasso.get()
                    .load(R.color.light_gray)
                    .into((CircleImageView) findViewById(R.id.user_image_main));
        }

        findViewById(R.id.sign_out).setOnClickListener(v -> {
            LogInUtil.signOut(this);

            Intent intent = new Intent(this, LogInActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            startActivity(intent);
            finish();
        });

        ListenerRegistration listenerRegistration = FirebaseFirestore.getInstance().collection(FireStoreConstants.USERS)
                        .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                .addSnapshotListener((value, error) -> {
                                    User user = value.toObject(User.class);

                                    Picasso.get()
                                            .load(user.getUser_image_url())
                                            .placeholder(R.color.light_gray)
                                            .fit().centerCrop()
                                            .into((CircleImageView) findViewById(R.id.user_image_main));

                                    String message = "";

                                    message += "Name : " + user.getFirst_name() + " " + user.getLast_name();
                                    message += "\nPhone number : " + user.getPhone_number();

                                    ((TextView) findViewById(R.id.text_main)).setText(message);

//                                    userManager.setUserDetails(user, true);
//                                    userManager.saveImage(user.getUser_image_url());
                                });
    }
}