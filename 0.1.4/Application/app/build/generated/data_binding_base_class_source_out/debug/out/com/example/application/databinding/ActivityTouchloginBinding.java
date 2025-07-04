// Generated by view binder compiler. Do not edit!
package com.example.application.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import com.example.application.R;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;

public final class ActivityTouchloginBinding implements ViewBinding {
  @NonNull
  private final LinearLayout rootView;

  @NonNull
  public final Button loginBtn;

  @NonNull
  public final EditText loginEmail;

  @NonNull
  public final TextView loginGuideText;

  @NonNull
  public final LinearLayout loginLayout;

  @NonNull
  public final EditText loginPassword;

  @NonNull
  public final TextView loginTitle;

  private ActivityTouchloginBinding(@NonNull LinearLayout rootView, @NonNull Button loginBtn,
      @NonNull EditText loginEmail, @NonNull TextView loginGuideText,
      @NonNull LinearLayout loginLayout, @NonNull EditText loginPassword,
      @NonNull TextView loginTitle) {
    this.rootView = rootView;
    this.loginBtn = loginBtn;
    this.loginEmail = loginEmail;
    this.loginGuideText = loginGuideText;
    this.loginLayout = loginLayout;
    this.loginPassword = loginPassword;
    this.loginTitle = loginTitle;
  }

  @Override
  @NonNull
  public LinearLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static ActivityTouchloginBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static ActivityTouchloginBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.activity_touchlogin, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static ActivityTouchloginBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.loginBtn;
      Button loginBtn = ViewBindings.findChildViewById(rootView, id);
      if (loginBtn == null) {
        break missingId;
      }

      id = R.id.login_email;
      EditText loginEmail = ViewBindings.findChildViewById(rootView, id);
      if (loginEmail == null) {
        break missingId;
      }

      id = R.id.loginGuideText;
      TextView loginGuideText = ViewBindings.findChildViewById(rootView, id);
      if (loginGuideText == null) {
        break missingId;
      }

      LinearLayout loginLayout = (LinearLayout) rootView;

      id = R.id.login_password;
      EditText loginPassword = ViewBindings.findChildViewById(rootView, id);
      if (loginPassword == null) {
        break missingId;
      }

      id = R.id.loginTitle;
      TextView loginTitle = ViewBindings.findChildViewById(rootView, id);
      if (loginTitle == null) {
        break missingId;
      }

      return new ActivityTouchloginBinding((LinearLayout) rootView, loginBtn, loginEmail,
          loginGuideText, loginLayout, loginPassword, loginTitle);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}
