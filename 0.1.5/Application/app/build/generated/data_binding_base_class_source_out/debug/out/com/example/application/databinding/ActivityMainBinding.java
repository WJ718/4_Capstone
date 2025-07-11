// Generated by view binder compiler. Do not edit!
package com.example.application.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import com.example.application.R;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;

public final class ActivityMainBinding implements ViewBinding {
  @NonNull
  private final LinearLayout rootView;

  @NonNull
  public final Button login;

  @NonNull
  public final LinearLayout main;

  @NonNull
  public final Button signup;

  private ActivityMainBinding(@NonNull LinearLayout rootView, @NonNull Button login,
      @NonNull LinearLayout main, @NonNull Button signup) {
    this.rootView = rootView;
    this.login = login;
    this.main = main;
    this.signup = signup;
  }

  @Override
  @NonNull
  public LinearLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static ActivityMainBinding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static ActivityMainBinding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.activity_main, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static ActivityMainBinding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.login;
      Button login = ViewBindings.findChildViewById(rootView, id);
      if (login == null) {
        break missingId;
      }

      LinearLayout main = (LinearLayout) rootView;

      id = R.id.signup;
      Button signup = ViewBindings.findChildViewById(rootView, id);
      if (signup == null) {
        break missingId;
      }

      return new ActivityMainBinding((LinearLayout) rootView, login, main, signup);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}
