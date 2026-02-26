package com.kiduyu.klaus.kiduyutv.utils;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

/**
 * Created by Kiduyu Klaus on 2/27/2026.
 */
public class DialogClass {

    private AlertDialog loadingDialog;
    private LinearLayout layout;

    public void showLoadingDialog(Activity activity, String messageText) {
        if (loadingDialog != null && loadingDialog.isShowing()) return;

        layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 60, 60, 60);
        layout.setGravity(Gravity.CENTER);
        layout.setBackgroundColor(Color.TRANSPARENT);

        ProgressBar progressBar = new ProgressBar(activity);
        progressBar.setIndeterminate(true);

        TextView message = new TextView(activity);
        message.setText(messageText != null ? messageText : "Loading...");
        message.setTextColor(Color.WHITE);
        message.setPadding(0, 30, 0, 0);
        message.setGravity(Gravity.CENTER);

        layout.addView(progressBar);
        layout.addView(message);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setView(layout);
        builder.setCancelable(false);

        loadingDialog = builder.create();

        if (loadingDialog.getWindow() != null) {
            loadingDialog.getWindow().setBackgroundDrawable(
                    new ColorDrawable(Color.TRANSPARENT)
            );
        }

        loadingDialog.show();
    }

    public void hideLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }
}