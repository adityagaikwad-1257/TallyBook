package com.adi.tallybook.apputilities.dialogutils;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.adi.tallybook.R;
import com.airbnb.lottie.LottieAnimationView;

/* single ton */
public class ErrorDialog {

    private final Dialog dialog;

    private View.OnClickListener positiveClickListener, negativeClickListener;

    private ErrorDialog(Context context){
        dialog = new Dialog(context);
        dialog.setContentView(R.layout.error_dialog_layout);
        dialog.setCancelable(true);

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    // making it single ton
    public static synchronized ErrorDialog create(Context context){
        return new ErrorDialog(context);
    }

    public void show(@NonNull Integer rawRes, @NonNull String title,
                     @NonNull String message, @NonNull String positive_btn,
                     String negative_btn){

        // initiating views in dialog
        ((LottieAnimationView) dialog.findViewById(R.id.error_illustration)).setAnimation(rawRes); // illustration (lottie)
        ((TextView) dialog.findViewById(R.id.error_title)).setText(title);
        ((TextView) dialog.findViewById(R.id.error_message)).setText(message);
        ((TextView) dialog.findViewById(R.id.positive_btn)).setText(positive_btn);

        if (negative_btn == null) dialog.findViewById(R.id.negative_btn).setVisibility(View.GONE);
        else {
            dialog.findViewById(R.id.negative_btn).setVisibility(View.VISIBLE);
            ((TextView) dialog.findViewById(R.id.negative_btn)).setText(negative_btn);
        }

        // click events
        dialog.findViewById(R.id.positive_btn).setOnClickListener(v ->{

            dismiss();

            if (positiveClickListener != null) positiveClickListener.onClick(v);
        });

        dialog.findViewById(R.id.negative_btn).setOnClickListener(v -> {

            dismiss();

            if (negativeClickListener != null) negativeClickListener.onClick(v);
        });

        dialog.create();
        dialog.show();
    }

    public void dismiss(){
        if (dialog.isShowing()) dialog.dismiss();
    }

    public void setPositiveClickListener(View.OnClickListener positiveClickListener) {
        this.positiveClickListener = positiveClickListener;
    }

    public void setNegativeClickListener(View.OnClickListener negativeClickListener) {
        this.negativeClickListener = negativeClickListener;
    }
}
