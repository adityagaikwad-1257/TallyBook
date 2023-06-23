package com.adi.tallybook.loginNregister.fragments;

import static com.adi.tallybook.apputilities.AppUtils.isConnectedToInternet;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.adi.tallybook.BuildConfig;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.adi.tallybook.R;
import com.adi.tallybook.apputilities.AppUtils;
import com.adi.tallybook.apputilities.dialogutils.ErrorDialog;
import com.adi.tallybook.apputilities.dialogutils.LoadingDialog;
import com.adi.tallybook.apputilities.dialogutils.PermissionBottomSheet;
import com.adi.tallybook.databinding.FragmentLoginBinding;
import com.adi.tallybook.loginNregister.LogInInterface;
import com.adi.tallybook.loginNregister.LogInUtil;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

public class LogInFragment extends Fragment implements LogInInterface{
    private static final String TAG = "aditya";
    
    protected FragmentLoginBinding binding;
    private LoadingDialog loadingDialog;

    // required sms permission launcher
    private ActivityResultLauncher<String> permissionLauncher;
    
    public LogInFragment(){
        // required empty constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);

        init();

        clickEvents();

        return binding.getRoot();
    }

    private void init() {
        // text change listener phone edit text to remove the error if any
        binding.phoneEt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                /*
                    removing error
                */
                binding.phoneLayout.setError(null);
                binding.phoneLayout.setErrorEnabled(false);
            }
        });

        // initializing loading dialog
        loadingDialog = LoadingDialog.create(requireContext());

        // showing version
        String version = "VERSION " +  BuildConfig.VERSION_NAME;
        binding.version.setText(version);

        // registering request permission launcher
        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    (isGranted) -> {
                        // anyway sending OTP
                        sendOtp();
                });
    }

    private void clickEvents() {
        binding.getOtp.setOnClickListener(v -> {
            AppUtils.closeKeyboard(getActivity());
            if (isPhoneValid()){
                if (!isConnectedToInternet(requireContext())) {
                    // not connected to internet
                    return;
                }

                if (checkSmsPermission()) sendOtp();
                else showPermissionBottomSheet();
            }
        });
    }

    /*
        show bottom sheet asking permission to read sms
     */
    private void showPermissionBottomSheet() {
        PermissionBottomSheet permissionBottomSheet = new PermissionBottomSheet(
                "1. READ INCOMING SMS", "(To automatically read the SMS and retrieve OTP)",
                "ALLOW PERMISSIONS", "Don't Allow, Enter OTP manually"
        );

        // user want to grant permission
        permissionBottomSheet.setPositiveClickListener(v -> {
            permissionBottomSheet.dismissAllowingStateLoss(); // closing bottom sheet
            permissionLauncher.launch(Manifest.permission.RECEIVE_SMS);
        });

        permissionBottomSheet.setNegativeClickListener(v -> {
            permissionBottomSheet.dismissAllowingStateLoss(); // closing bottom sheet
            sendOtp();
        });

        permissionBottomSheet.show(getParentFragmentManager(), "SMS_PERMISSION");
    }

    /*
        checks for permission to READ_SMS
     */
    private boolean checkSmsPermission() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    /*
        validates if the user has entered a valid phone number
     */
    private boolean isPhoneValid(){

        if (!binding.phoneEt.getText().toString().trim().matches(PHONE_REGEX)){
            binding.phoneLayout.setError("Invalid phone number");
            return false;
        }

        return true;
    }

    /*
        sends otp on the user's phone
     */
    private void sendOtp() {
        /*
            showing loading view
         */
        loadingDialog.show("Sending OTP");

        /*
            send otp stuff
         */
        String phoneNumber = getString(R.string.country_code) + binding.phoneEt.getText().toString();

        LogInUtil.verifyPhone(phoneNumber, requireActivity(), this /* Call backs */, null);
    }
    
    private void gotoOtpFrag(String phoneNumber, String verificationCode, PhoneAuthProvider.ForceResendingToken forceResendingToken){
        /*
            go to OTP fragment
         */
        Bundle args = new Bundle();
        args.putString(PHONE_KEY, phoneNumber); // passing phone number
        args.putString(OTP_VERIFICATION_CODE_KEY, verificationCode); // passing phone number
        args.putParcelable(RESEND_TOKEN_KEY , forceResendingToken); // force resend token

        Navigation.findNavController(binding.getOtp).navigate(R.id.action_logInFragment_to_otpFragment, args);
    }

    // verify phone number callbacks ( send OTP )
    @Override
    public void onCodeSent(@NonNull String verificationCode, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken, String phoneNumber) {
        Log.d(TAG, "onCodeSent: log in");
        loadingDialog.dismiss(); // dismissing loading view
        gotoOtpFrag(phoneNumber, verificationCode, forceResendingToken);
    }

    @Override
    public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
        Log.d(TAG, "onVerificationCompleted: ");
        loadingDialog.dismiss(); // dismissing loading view
    }

    @Override
    public void onVerificationFailed(@NonNull FirebaseException e) {
        Log.d(TAG, "onVerificationFailed: " + e.getMessage());
        loadingDialog.dismiss(); // dismissing loading view

        String error_message = e.getLocalizedMessage() == null?"":e.getLocalizedMessage();

        // showing error dialog
        ErrorDialog.create(requireContext()).show(R.raw.error,
                "SOMETHING WENT WRONG", error_message, "Ok", null);
    }
}
