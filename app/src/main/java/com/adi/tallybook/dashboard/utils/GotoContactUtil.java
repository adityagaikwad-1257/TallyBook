package com.adi.tallybook.dashboard.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.adi.tallybook.R;
import com.adi.tallybook.apputilities.dialogutils.PermissionBottomSheet;

public abstract class GotoContactUtil {

    public static void showEducationalUI(Context context, View.OnClickListener clickListener,
            String permission, String permissionMessage,
            String positiveText, FragmentManager fragmentManager) {

        PermissionBottomSheet permissionDialog = new PermissionBottomSheet(permission, permissionMessage
                , positiveText, "Not now"
        );

        permissionDialog.setPositiveClickListener(v -> {
            if (permissionDialog.isAdded() && permissionDialog.isVisible()) permissionDialog.dismiss();
            clickListener.onClick(v);
        });

        permissionDialog.setNegativeClickListener(v -> {
            if (permissionDialog.isAdded() && permissionDialog.isVisible()) permissionDialog.dismiss();
        });

        permissionDialog.show(fragmentManager, context.getString(R.string.contacts_permission));
    }

    public static boolean checkContactsPermission(Fragment fragment) {
        return ContextCompat.checkSelfPermission(fragment.requireActivity(), Manifest.permission.READ_CONTACTS) ==
                PackageManager.PERMISSION_GRANTED;
    }

}
