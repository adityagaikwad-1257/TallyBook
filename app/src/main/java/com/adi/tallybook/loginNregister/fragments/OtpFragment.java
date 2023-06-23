package com.adi.tallybook.loginNregister.fragments;

import static android.provider.Telephony.Sms.Intents.SMS_RECEIVED_ACTION;
import static com.adi.tallybook.apputilities.AppUtils.isConnectedToInternet;
import static com.adi.tallybook.apputilities.FireStoreConstants.FIREBASE_UID;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.adi.tallybook.R;
import com.adi.tallybook.apputilities.AppUtils;
import com.adi.tallybook.apputilities.UserManager;
import com.adi.tallybook.apputilities.dialogutils.ErrorDialog;
import com.adi.tallybook.apputilities.dialogutils.LoadingDialog;
import com.adi.tallybook.dashboard.HomeActivity;
import com.adi.tallybook.databinding.FragmentOtpBinding;
import com.adi.tallybook.loginNregister.LogInInterface;
import com.adi.tallybook.loginNregister.LogInUtil;
import com.adi.tallybook.loginNregister.activities.UserDetailsActivity;
import com.adi.tallybook.models.User;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

public class OtpFragment extends Fragment implements LogInInterface {
    private static final String TAG = "aditya";

    protected FragmentOtpBinding binding;

    private PhoneAuthProvider.ForceResendingToken resendingToken;
    private String phoneNumber;
    private String verificationId = "";

    private LoadingDialog loadingDialog;

    /*
        Broadcast receiver to listen to incoming sms
        registered -> onAttach();
        unregistered -> onDetach();
     */
    private final BroadcastReceiver smsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (SMS_RECEIVED_ACTION.equals(intent.getAction())){
                for (SmsMessage s: Telephony.Sms.Intents.getMessagesFromIntent(intent)){
                    if (s.getMessageBody().contains("verification code.")){
                        String otp = s.getMessageBody().substring(0, 6);
                        setOtpIntoEditBoxes(otp);
                    }
                }
            }
        }
    };

    public OtpFragment(){
        // required empty constructor
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        context.registerReceiver(smsReceiver, new IntentFilter(SMS_RECEIVED_ACTION)); // registering SMS receiver
    }

    @Override
    public void onDetach() {
        super.onDetach();
        requireContext().unregisterReceiver(smsReceiver); // unregistering SMS receiver
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentOtpBinding.inflate(inflater, container, false);

        inti();

        clickEvents();

        return binding.getRoot();
    }

    private void inti() {
        // retrieving phone number from bundle received
        Bundle args = getArguments();
        if (args != null && args.getString(PHONE_KEY, null) != null){
            phoneNumber = args.getString(PHONE_KEY);
            binding.phoneTv.setText(phoneNumber);
        }

        // retrieving ForceResendToken
        if (args != null && args.getParcelable(RESEND_TOKEN_KEY) != null){
            resendingToken = args.getParcelable(RESEND_TOKEN_KEY);
            if (resendingToken != null) binding.resendOtp.setVisibility(View.VISIBLE);
        }

        // retrieving verification code
        if (args != null && args.getString(OTP_VERIFICATION_CODE_KEY) != null){
            verificationId = args.getString(OTP_VERIFICATION_CODE_KEY);
        }

        // initiating loading dialog
        loadingDialog = LoadingDialog.create(requireContext());

        setUpOtpEditTexts();
    }

    private void clickEvents(){
        binding.changeIt.setOnClickListener(v -> Navigation.findNavController(v).popBackStack());

        binding.resendOtp.setOnClickListener(this::resendOtp);

        binding.verifyOtp.setOnClickListener(v -> logInWithPhone());
    }

    private void logInWithPhone(){
        if (!isConnectedToInternet(requireContext())){
            // not connected to internet
            return;
        }

        loadingDialog.show("Verifying otp");
        binding.verifyOtp.setEnabled(false);

        LogInUtil.logInWithPhone(verificationId, getOtpFromInput(),
                authResult -> {
                    // success call back
                    loadingDialog.dismiss();

                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user != null){
                        // all is okay
                        // signed in successfully

                        // go to UserDetailsActivity or HomeActivity
                        Activity activity = getActivity();

                        if (activity != null){

                            if (authResult.getAdditionalUserInfo() != null && authResult.getAdditionalUserInfo().isNewUser()){
                                // all good to go to UserDetailsActivity

                                Intent intent = new Intent(activity, UserDetailsActivity.class);
                                intent.putExtra(PHONE_KEY, user.getPhoneNumber()); // sending phone number
                                intent.putExtra(FIREBASE_UID, user.getUid());
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                                startActivity(intent);
                                activity.finish();
                            }else{
                                // already a user
                                // good to go to HomeActivity

                                // also update user details into shared preference from fire store server

                                loadingDialog.show("Fetching your details");

                                LogInUtil.getUserDetails(user.getUid(),
                                        userDetails -> {
                                        // got the user details successfully

                                            if (userDetails != null){
                                                /*
                                                    saving user details to local at this moment is
                                                    not necessary as user would be taken to the
                                                    home activity where user local details would be updated anyway
                                                 */

                                                // save user details locally
                                                UserManager manager = UserManager.getInstance(requireContext());

                                                manager.setUserDetails(userDetails, true);

                                                // good to go to Home screen
                                                Intent intent = new Intent(activity, HomeActivity.class);
                                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                                                startActivity(intent);
                                                activity.finish();
                                            }else{
                                                // No user details found on server
                                                // redirecting user to save new details

                                                Intent intent = new Intent(activity, UserDetailsActivity.class);
                                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                                                intent.putExtra(PHONE_KEY, user.getPhoneNumber()); // sending phone number
                                                intent.putExtra(FIREBASE_UID, user.getUid());

                                                startActivity(intent);
                                                activity.finish();
                                            }

                                    }, e -> {
                                            // something went wrong while fetching user
                                            // redirecting user to save new details
                                            Intent intent = new Intent(activity, UserDetailsActivity.class);
                                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                                            intent.putExtra(PHONE_KEY, user.getPhoneNumber()); // sending phone number
                                            intent.putExtra(FIREBASE_UID, user.getUid());

                                            startActivity(intent);
                                            activity.finish();
                                    });
                            }
                        }else{
                            // no host for the activity
                            checkOtpBoxes(); // enabling/disabling depending upon otp edit texts states
                            Toast.makeText(requireContext(), "Something went wrong, Try again.", Toast.LENGTH_SHORT).show();
                        }

                    }else{
                        // something went wrong while signing in user

                        checkOtpBoxes(); // enabling/disabling depending upon otp edit texts states

                        Toast.makeText(requireContext(), "Something went wrong, Try again.", Toast.LENGTH_SHORT).show();
                    }

                },
                exception ->{
                    // fail call back
                    loadingDialog.dismiss();
                    checkOtpBoxes(); // enabling/disabling depending upon otp edit texts states

                    // showing error
                    String title;
                    String message;

                    if (exception.getLocalizedMessage() != null){
                        if (exception.getLocalizedMessage().contains("invalid")){
                            title = "Invalid OTP";
                            message = "Please make sure you are entering the same OTP received via SMS from TALLY BOOK.";
                        }else if(exception.getLocalizedMessage().contains("disabled") && 
                                exception.getLocalizedMessage().contains("account")){
                            title = "Account Disabled :(";
                            message = "Your account at TALLY BOOK has been disabled by administrator.";
                        }else{
                            title = "Something went wrong";
                            message = exception.getLocalizedMessage();
                        }
                    }else{
                        title = "something went wrong";
                        message = "It's not you, It's us.\nKindly try again later";
                    }
                    
                    ErrorDialog.create(requireContext()).show(R.raw.error, 
                            title, message, "ok", null);

                });
    }

    private void resendOtp(View view){
        if (!isConnectedToInternet(requireContext())){
            // not connected to internet
            return;
        }

        loadingDialog.show("Resending OTP"); // showing loading view

        LogInUtil.verifyPhone(phoneNumber, getActivity(), this/*Call backs*/, resendingToken);
    }

    /*
        filling opt into edit boxes received from Broad cast receiver
     */
    private void setOtpIntoEditBoxes(String otp) {
        if (verifyOtp(otp)){
            binding.otp1.setText(String.valueOf(otp.charAt(0)));
            binding.otp2.setText(String.valueOf(otp.charAt(1)));
            binding.otp3.setText(String.valueOf(otp.charAt(2)));
            binding.otp4.setText(String.valueOf(otp.charAt(3)));
            binding.otp5.setText(String.valueOf(otp.charAt(4)));
            binding.otp6.setText(String.valueOf(otp.charAt(5)));

            // log in with phone
            logInWithPhone();
        }
    }

    /*
        checks if the otp contains all digits
     */
    private boolean verifyOtp(String otp) {

        for (int i = 0; i < otp.length(); i++) {
            if (!(otp.charAt(i) >= '0' && otp.charAt(i) <= '9')) return false;
        }

        return true;
    }

    public String getOtpFromInput(){
        return "" + binding.otp1.getText().toString() +
                binding.otp2.getText().toString() +
                binding.otp3.getText().toString() +
                binding.otp4.getText().toString() +
                binding.otp5.getText().toString() +
                binding.otp6.getText().toString();
    }

    /*
        checks every otp edit box and if all boxes are filled
        then enable verify button else disable it
     */
    private void checkOtpBoxes(){
        boolean allOkay = true; // check for all edit boxes are filed

        if (binding.otp1.getText().toString().trim().isEmpty()){
            allOkay = false;
        }else if (binding.otp2.getText().toString().trim().isEmpty()){
            allOkay = false;
        }else if (binding.otp3.getText().toString().trim().isEmpty()){
            allOkay = false;
        }else if (binding.otp4.getText().toString().trim().isEmpty()){
            allOkay = false;
        }else if (binding.otp5.getText().toString().trim().isEmpty()){
            allOkay = false;
        }else if (binding.otp6.getText().toString().trim().isEmpty()){
            allOkay = false;
        }

        // enable verify button
        // disable verify button
        binding.verifyOtp.setEnabled(allOkay);
    }

    /*
        ( resend OTP ) verify phone number call backs
     */
    // called when OTP is resent successfully
    @Override
    public void onCodeSent(@NonNull String s, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken, String phoneNumber) {
        Log.d(TAG, "onCodeSent: otp ");
        loadingDialog.showDone("OTP SENT");

        verificationId = s; // updating verification id
    }

    // Google services automatically reads the incoming code and Auto-verifies, thus this function is called
    // This method is called after onCodeSent()
    @Override
    public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
        Log.d(TAG, "onVerificationCompleted: otp");
        loadingDialog.dismiss(); // dismissing loading view
    }

    // called when resend OTP failed
    @Override
    public void onVerificationFailed(@NonNull FirebaseException e) {
        Log.d(TAG, "onVerificationFailed: " + e.getMessage());
        loadingDialog.dismiss(); // dismissing loading view

        String error_message = e.getLocalizedMessage() == null?"":e.getLocalizedMessage();

        // showing error dialog
        ErrorDialog.create(requireContext()).show(R.raw.error,
                "SOMETHING WENT WRONG", error_message, "Ok", null);
    }

    /*
       setting text change listener for every edit text for otp
    */
    private void setUpOtpEditTexts() {
        binding.otp1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().trim().isEmpty()){
                    binding.otp2.requestFocus();
                }

                // check if all otp boxes are filled
                checkOtpBoxes();
            }
        });

        binding.otp2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().trim().isEmpty()){
                    binding.otp3.requestFocus();
                }else{
                    binding.otp1.requestFocus();
                }

                // check if all otp boxes are filled
                checkOtpBoxes();
            }
        });

        binding.otp3.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().trim().isEmpty()){
                    binding.otp4.requestFocus();
                }else{
                    binding.otp2.requestFocus();
                }

                // check if all otp boxes are filled
                checkOtpBoxes();
            }
        });

        binding.otp4.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().trim().isEmpty()){
                    binding.otp5.requestFocus();
                }else{
                    binding.otp3.requestFocus();
                }

                // check if all otp boxes are filled
                checkOtpBoxes();
            }
        });

        binding.otp5.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().trim().isEmpty()){
                    binding.otp6.requestFocus();
                }else{
                    binding.otp4.requestFocus();
                }

                // check if all otp boxes are filled
                checkOtpBoxes();
            }
        });

        binding.otp6.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().trim().isEmpty()){
                    // last otp inserted
                    AppUtils.closeKeyboard(getActivity());
                }else{
                    binding.otp5.requestFocus();
                }

                // check if all otp boxes are filled
                checkOtpBoxes();
            }
        });
    }
}
