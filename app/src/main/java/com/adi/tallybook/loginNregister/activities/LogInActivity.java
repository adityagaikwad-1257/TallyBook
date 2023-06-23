package com.adi.tallybook.loginNregister.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.adi.tallybook.apputilities.UserManager;
import com.adi.tallybook.dashboard.HomeActivity;
import com.adi.tallybook.databinding.ActivityLoginBinding;
import com.google.firebase.auth.FirebaseAuth;

public class LogInActivity extends AppCompatActivity {

    protected ActivityLoginBinding binding;

    @Override
    protected void onStart() {
        super.onStart();
        if (FirebaseAuth.getInstance().getCurrentUser() != null){
            // user is signed in with firebase auth
            if (UserManager.getInstance(this).isUserLoggedIn()){
                // user details are also saved in shared pref
                // good to go to HomeActivity
                Intent intent = new Intent(this, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                startActivity(intent);
                finish();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }
}