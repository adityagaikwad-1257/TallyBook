package com.adi.tallybook.dashboard.fragments;

import static com.adi.tallybook.dashboard.utils.GotoContactUtil.checkContactsPermission;
import static com.adi.tallybook.dashboard.utils.GotoContactUtil.showEducationalUI;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.adi.tallybook.R;
import com.adi.tallybook.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    // binding
    FragmentHomeBinding binding;

    // Launcher for contacts permission
    ActivityResultLauncher<String> contactPermissionLauncher;

    // Launcher for visiting Application info activity, settings
    ActivityResultLauncher<Intent> applicationSettingLauncher;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        clickEvents();

        registerLaunchers();

        return binding.getRoot();
    }

    private void clickEvents() {
        // contacts fab button click
        binding.contacts.setOnClickListener(this::gotoContactsFragment);
    }

    private void gotoContactsFragment(View v){
        if (checkContactsPermission(this)) {
            // permission already granted
            Navigation.findNavController(v).navigate(R.id.action_homeFragment_to_contactsFragment);
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)) {
            // permission denied
            // request for permission normally
            // show educational UI, explaining why this permission is required

            showEducationalUI(
                    requireContext(),
                    (view) -> {
                        // positive button click listener
                        contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS);
                    },
                    getString(R.string.contacts_permission),
                    getString(R.string.contacts_permission_message),
                    "Continue",
                    getParentFragmentManager()
            ); // which eventually asks for permission
        }else{
            // request permission normally
            contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS);
        }
    }

    private void registerLaunchers() {
        contactPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        Navigation.findNavController(binding.getRoot()).navigate(R.id.action_homeFragment_to_contactsFragment);
                    }else if (!shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)){
                        // user denied the permission
                        // the user not only refused permission, but also decided not to be asked again
                        // from here user should be taken to the applications setting to grant the permission from there

                        showEducationalUI(
                                requireContext(),
                                v -> {
                                    // sending user to application setting page
                                    Intent intent = new Intent();
                                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                    Uri uri = Uri.fromParts("package", requireActivity().getPackageName(), null);
                                    intent.setData(uri);
                                    applicationSettingLauncher.launch(intent);
                                },
                                getString(R.string.contacts_permission),
                                getString(R.string.contacts_permission_settings_message),
                                "Settings",
                                getParentFragmentManager()
                                );
                    }
                });

        applicationSettingLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (checkContactsPermission(this)){
                        // after sending user to the application setting page
                        // user granted the permission there
                        // thus, navigating to contacts fragment

                        Navigation.findNavController(binding.getRoot())
                                .navigate(R.id.action_homeFragment_to_contactsFragment);
                    }else {
                        Toast.makeText(requireContext(), "Permission not granted.", Toast.LENGTH_SHORT).show();
                    }
                });

    }

}