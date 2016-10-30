package com.pro3.planner;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;

public class RegisterActivity extends AppCompatActivity implements View.OnClickListener{

    private EditText email, password, passwordAgain;
    private Button registerButton;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        email = (EditText) findViewById(R.id.registerEmail);
        password = (EditText) findViewById(R.id.registerPassword);
        passwordAgain = (EditText) findViewById(R.id.registerPasswordAgain);
        registerButton = (Button) findViewById(R.id.registerSubmitButton);
        registerButton.setOnClickListener(this);

        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    //User is signed in/registered successfully
                    Intent i = new Intent(RegisterActivity.this, MainActivity.class);
                    startActivity(i);
                } else {
                    // User is signed out
                }
            }
        };
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    private void register(String email, String password, String passwordAgain){
        //TODO: Strip strings, Passwort Komplexität prüfen

        if (emailValid(email) && passwordValid(password) && passwordValid(passwordAgain)) {
            if(password.equals(passwordAgain)) {
                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                // If sign in fails, display a message to the user. If sign in succeeds
                                // the auth state listener will be notified and logic to handle the
                                // signed in user can be handled in the listener.
                                if (!task.isSuccessful()) {
                                    try{
                                        throw task.getException();
                                    } catch(FirebaseAuthWeakPasswordException e) {
                                        Toast.makeText(RegisterActivity.this, "Register failed! Password too weak!", Toast.LENGTH_LONG).show();
                                    } catch(FirebaseAuthInvalidCredentialsException e) {
                                        Toast.makeText(RegisterActivity.this, "Register failed! Invalid E-Mail address!", Toast.LENGTH_LONG).show();
                                    } catch(FirebaseAuthUserCollisionException e) {
                                        Toast.makeText(RegisterActivity.this, "Register failed! User already exists!", Toast.LENGTH_LONG).show();
                                    } catch(Exception e) {
                                        Log.e("RegisterError", e.getMessage());
                                    }

                                }
                            }
                        });
            } else {
                Toast.makeText(this, "Passwords don't match", Toast.LENGTH_SHORT).show();
            }
        } else{
            Toast.makeText(this, "Register failed. One or more inputs contain invalid characters", Toast.LENGTH_LONG).show();
        }


    }

    private boolean emailValid(String input) {
        //Check if only allowed characters are in the email string
        String pattern = "^[a-zA-Z0-9.@-]*$";
        return input.matches(pattern);
    }

    private boolean passwordValid(String input) {
        //Check if only allowed characters are in the password string
        String pattern = "^[a-zA-Z0-9]*$";
        return input.matches(pattern);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.registerSubmitButton) {
            String sEmail = email.getText().toString();
            String sPassword = password.getText().toString();
            String sPasswordAgain = passwordAgain.getText().toString();

            //Check if a field is empty
            if (sEmail.length() > 0 && sPassword.length() > 0 && sPasswordAgain.length() > 0) {
                register(sEmail, sPassword, sPasswordAgain);
            } else {
                Toast.makeText(this, "Please fill out all fields!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}