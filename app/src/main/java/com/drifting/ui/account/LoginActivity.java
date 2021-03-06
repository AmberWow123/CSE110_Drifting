package com.drifting.ui.account;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.drifting.ui.NavBar;
import com.example.drifting.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.drifting.util.authentication.CredentialAuthenticator;
import com.drifting.util.connectivity.ConnectionChecker;
import com.drifting.container.BagData;
import com.drifting.container.DrifterData;
import com.drifting.database.SetDatabase;


public class LoginActivity extends AppCompatActivity {

    private LoginViewModel loginViewModel;
    private DatabaseReference UserRef;

    FirebaseUser firebaseUser;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        loginViewModel = new ViewModelProvider(this, new LoginViewModelFactory())
                .get(LoginViewModel.class);
        Context context = getApplicationContext();

        final EditText usernameEditText = findViewById(R.id.username_forget);
        final EditText passwordEditText = findViewById(R.id.password);
        final Button loginButton = findViewById(R.id.login);
        final Button forgotButton = findViewById(R.id.forgot_password);
        final Button registerButton = findViewById(R.id.signup_text);
        final ProgressBar loadingBar = findViewById(R.id.loadingBar);

        // to underline the "Register now" text
        TextView textView = (TextView) findViewById(R.id.sign_up);
        textView.setPaintFlags(textView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        loginViewModel.getLoginFormState().observe(this, new Observer<LoginFormState>() {
            @Override
            public void onChanged(@Nullable LoginFormState loginFormState) {
                if (loginFormState == null) {
                    return;
                }
            }
        });


        loginViewModel.getLoginResult().observe(this, new Observer<LoginResult>() {
            @Override
            public void onChanged(@Nullable LoginResult loginResult) {
                if (loginResult == null) {
                    return;
                }
                if (loginResult.getError() != null) {
                    showLoginFailed(loginResult.getError());
                }
                if (loginResult.getSuccess() != null) {
                    updateUiWithUser(loginResult.getSuccess());
                }
                setResult(Activity.RESULT_OK);

                //Complete and destroy login activity once successful
                finish();
            }
        });

        TextWatcher afterTextChangedListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // ignore
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // ignore
            }

            @Override
            public void afterTextChanged(Editable s) {
                loginViewModel.loginDataChanged(usernameEditText.getText().toString(),
                        passwordEditText.getText().toString());
            }
        };
        usernameEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean isConnected = ConnectionChecker.isInternetConnected(getApplicationContext());

                if(!isConnected){
                    Toast.makeText(getApplicationContext(), "Please check Internet connection", Toast.LENGTH_SHORT).show();
                    return false;
                }

                CredentialAuthenticator ca = new CredentialAuthenticator();
                String feedback = ca.validate(usernameEditText.getText().toString(),
                        passwordEditText.getText().toString());

                Toast.makeText(LoginActivity.this, feedback, Toast.LENGTH_SHORT).show();

                FirebaseAuth mAuth = FirebaseAuth.getInstance();

                if (ca.isValid()) {
                    Task task = mAuth.signInWithEmailAndPassword(usernameEditText.getText().toString(),
                            passwordEditText.getText().toString()).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                loadingBar.setVisibility((View.GONE));
                                UserRef = FirebaseDatabase.getInstance().getReference().child("user").child(mAuth.getCurrentUser().getUid());

                                SetDatabase sd = new SetDatabase();
                                sd.get_sent_bottles(BagData.sentBottle);
                                sd.get_picked_bottles(BagData.pickedBottle);


                                UserRef.addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        String name = snapshot.child("user_name").getValue() != null ? snapshot.child("user_name").getValue().toString() : "unspecified";
                                        Toast.makeText(LoginActivity.this, "Welcome, " + name, Toast.LENGTH_LONG).show();
                                        DrifterData.username = name;
                                        // Toast.makeText(LoginActivity.this, "Welcome, " + mAuth.getCurrentUser().getUid(), Toast.LENGTH_LONG).show();
                                        UserRef.removeEventListener(this);
                                    }
                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });
                                openHomepageActivity();
                            } else {
                                loadingBar.setVisibility((View.GONE));
                                Toast.makeText(LoginActivity.this, "Login failed. Please check your credentials", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                }
                loadingBar.setVisibility((View.GONE));
                return false;
            }
        });

        // login button listener
        loginButton.setOnClickListener(new View.OnClickListener() {

           @Override
           public void onClick(View v) {

               boolean isConnected = ConnectionChecker.isInternetConnected(getApplicationContext());

               if(!isConnected){
                   Toast.makeText(getApplicationContext(), "Please check Internet connection", Toast.LENGTH_SHORT).show();
                   return;
               }


               CredentialAuthenticator ca = new CredentialAuthenticator();
               String feedback = ca.validate(usernameEditText.getText().toString(),
                       passwordEditText.getText().toString());

               Toast.makeText(LoginActivity.this, feedback, Toast.LENGTH_SHORT).show();

               FirebaseAuth mAuth = FirebaseAuth.getInstance();


               if(ca.isValid()) {
                   loadingBar.setVisibility(View.VISIBLE);
                   Task task = mAuth.signInWithEmailAndPassword(usernameEditText.getText().toString(),
                           passwordEditText.getText().toString()).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                       @Override
                       public void onComplete(@NonNull Task<AuthResult> task) {
                           if (task.isSuccessful()) {
                               loadingBar.setVisibility((View.GONE));
                               UserRef = FirebaseDatabase.getInstance().getReference().child("user").child(mAuth.getCurrentUser().getUid());
                               SetDatabase sd = new SetDatabase();
                               sd.get_sent_bottles(BagData.sentBottle);
                               sd.get_picked_bottles(BagData.pickedBottle);
                               UserRef.addValueEventListener(new ValueEventListener() {
                                   @Override
                                   public void onDataChange(@NonNull DataSnapshot snapshot) {
                                       String name = snapshot.child("user_name").getValue() != null ? snapshot.child("user_name").getValue().toString() : "unspecified";
                                       Toast.makeText(LoginActivity.this, "Welcome, " + name, Toast.LENGTH_LONG).show();
                                       DrifterData.username = name;
                                       // Toast.makeText(LoginActivity.this, "Welcome, " + mAuth.getCurrentUser().getUid(), Toast.LENGTH_LONG).show();
                                       UserRef.removeEventListener(this);
                                   }
                                   @Override
                                   public void onCancelled(@NonNull DatabaseError error) {

                                   }
                               });
                               openHomepageActivity();
                           } else {
                               loadingBar.setVisibility((View.GONE));
                               Toast.makeText(LoginActivity.this, "Login failed. Please check your credentials", Toast.LENGTH_LONG).show();
                           }
                       }
                   });
               }
               loadingBar.setVisibility((View.GONE));

           }
       });

        forgotButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openForgotPasswordActivity();
            }
        });

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openRegisterActivity();
            }
        });
    }

    public void openHomepageActivity() {
        Intent intent = new Intent(this, NavBar.class);
        startActivity(intent);
        finish();
    }

    public void openForgotPasswordActivity() {
        Intent intent = new Intent(this, ForgotPasswordActivity.class);
        startActivity(intent);
    }

    public void openRegisterActivity() {
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }

    private void updateUiWithUser(LoggedInUserView model) {
        model.setDisplayname("John Snow");
        String welcome = getString(R.string.welcome) + model.getDisplayName();
        // TODO : initiate successful logged in experience
        Toast.makeText(getApplicationContext(), welcome, Toast.LENGTH_LONG).show();
    }

    private void showLoginFailed(@StringRes Integer errorString) {
        Toast.makeText(getApplicationContext(), errorString, Toast.LENGTH_SHORT).show();
    }
}
