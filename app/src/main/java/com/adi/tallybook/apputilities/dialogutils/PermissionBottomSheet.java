package com.adi.tallybook.apputilities.dialogutils;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.adi.tallybook.databinding.PermissionBsBinding;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class PermissionBottomSheet extends BottomSheetDialogFragment {

    private String permissionName, permissionMessage
            , positiveMessage, negativeMessage;

    private PermissionBsBinding binding;
    private View.OnClickListener positiveClickListener, negativeClickListener;

    public PermissionBottomSheet(){
        // required
    }

    public PermissionBottomSheet(String permissionName, String permissionMessage, String positiveMessage, String negativeMessage) {
        this.permissionName = permissionName;
        this.permissionMessage = permissionMessage;
        this.positiveMessage = positiveMessage;
        this.negativeMessage = negativeMessage;
    }

    public void setPositiveClickListener(View.OnClickListener positiveClickListener) {
        this.positiveClickListener = positiveClickListener;
    }

    public void setNegativeClickListener(View.OnClickListener negativeClickListener) {
        this.negativeClickListener = negativeClickListener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = PermissionBsBinding.inflate(inflater, container, false);

        requireDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        
        init();

        clickEvents();

        return binding.getRoot();
    }

    private void clickEvents() {
        binding.positiveMessage.setOnClickListener(v -> {
            if (positiveClickListener != null) positiveClickListener.onClick(v);
        });

        binding.negativeMessage.setOnClickListener(v -> {
            if (negativeClickListener != null) negativeClickListener.onClick(v);
        });
    }

    private void init() {
        // initiating view
        binding.permissionName.setText(permissionName);
        binding.permissionMessage.setText(permissionMessage);
        binding.positiveMessage.setText(positiveMessage);
        binding.negativeMessage.setText(negativeMessage);
    }

}
