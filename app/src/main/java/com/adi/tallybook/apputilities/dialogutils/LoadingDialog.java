package com.adi.tallybook.apputilities.dialogutils;

import android.animation.Animator;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.adi.tallybook.R;
import com.airbnb.lottie.LottieAnimationView;

public class LoadingDialog{

    private final Dialog dialog;

    /*
        private constructor
     */
    private LoadingDialog(Context context){
        /*
            initiating dialog
         */
        dialog = new Dialog(context);
        dialog.setContentView(R.layout.loading_dialog_layout);
        dialog.setCancelable(false);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }


    public static LoadingDialog create(Context context){
        return new LoadingDialog(context);
    }

    /*
        showing loading dialog
     */
    public void show(String message){
        dismiss(); // dismissing dialog if already showing

        // setting raw res to lottie illustration
        LottieAnimationView illustration = dialog.findViewById(R.id.loading_illustration);

        illustration.setAnimation(R.raw.loading);
        illustration.loop(true);
        illustration.playAnimation();

        if (message != null){
            ((TextView) dialog.findViewById(R.id.message_dialog)).setText(message);
            (dialog.findViewById(R.id.message_dialog)).setVisibility(View.VISIBLE);
        }else{
            (dialog.findViewById(R.id.message_dialog)).setVisibility(View.GONE);
        }

        dialog.setCancelable(false);
        dialog.create();
        dialog.show();
    }

    public void showDone(String message){
        // setting raw res to lottie illustration
        LottieAnimationView illustration = dialog.findViewById(R.id.loading_illustration);

        illustration.setAnimation(R.raw.done);
        illustration.loop(false);
        illustration.setSpeed(1.5f);
        illustration.playAnimation();

        illustration.addAnimatorListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(@NonNull Animator animation) {

            }

            @Override
            public void onAnimationEnd(@NonNull Animator animation) {
                dismiss(); // dismissing when animation is complete
            }

            @Override
            public void onAnimationCancel(@NonNull Animator animation) {

            }

            @Override
            public void onAnimationRepeat(@NonNull Animator animation) {

            }
        });

        if (message != null){
            ((TextView) dialog.findViewById(R.id.message_dialog)).setText(message);
            (dialog.findViewById(R.id.message_dialog)).setVisibility(View.VISIBLE);
        }else{
            (dialog.findViewById(R.id.message_dialog)).setVisibility(View.GONE);
        }

        dialog.setCancelable(true);
        dialog.create();
        dialog.show();
    }

    public void changeMessage(String message){
        if (message != null){
            ((TextView) dialog.findViewById(R.id.message_dialog)).setText(message);
            (dialog.findViewById(R.id.message_dialog)).setVisibility(View.VISIBLE);
        }else{
            (dialog.findViewById(R.id.message_dialog)).setVisibility(View.GONE);
        }
    }

    public boolean isShowing(){
        return dialog.isShowing();
    }

    public void dismiss(){
        if (dialog.isShowing()) dialog.dismiss();
    }
}
