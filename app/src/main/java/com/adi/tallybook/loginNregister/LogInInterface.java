package com.adi.tallybook.loginNregister;

import androidx.annotation.NonNull;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

public interface LogInInterface {
    String PHONE_KEY = "phone_key";
    String OTP_VERIFICATION_CODE_KEY = "otp_verification";
    String RESEND_TOKEN_KEY = "resend_otp";

    String PHONE_REGEX = "^[6-9]\\d{9}$";

    void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential);

    void onVerificationFailed(@NonNull FirebaseException e);

    void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken, String phoneNumber);
}
