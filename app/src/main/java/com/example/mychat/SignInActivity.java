package com.example.mychat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.mychat.databinding.ActivitySignInBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SignInActivity extends AppCompatActivity {

    // View Binding
    private ActivitySignInBinding b;


    // Phone Authentication
    private FirebaseAuth mFirebaseAuth;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallback;
    private String mVerificationId;
    private MyApp app;
    private String userMobNo;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        app = (MyApp)getApplicationContext();

        setUpPhoneSignBtn();

        // Initialize FirebaseAuth
        mFirebaseAuth = FirebaseAuth.getInstance();

        // Initialize phone auth callbacks
        phoneAuthCallbacks();
    }





    /** Setting up phone sign-in feature  **/
    private void phoneAuthCallbacks() {
        // The callback to detect the verification status
        mCallback = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                // This callback will be invoked in two situations
                // 1 - Instant verification: Im some cases the phone number can be instantly
                //      verified without needing to send or enter a verification code
                // 2 - Auto-Retrieval : On some devices Google play services can automatically
                //           detect the incoming verification code and perform verification without user action

                signInWithPhoneAuthCredentials(phoneAuthCredential);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                if (e instanceof FirebaseAuthInvalidCredentialsException){
                    Toast.makeText(SignInActivity.this, "Invalid phone number!", Toast.LENGTH_SHORT).show();
                }else if (e instanceof FirebaseTooManyRequestsException){
                    Toast.makeText(SignInActivity.this, "Quota Exceeded!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                // The SMS verification code has been sent to the provided phone number , we
                // now need to ask the user to enter the code and then construct the credentials
                // by combining the code with a verification ID.
                mVerificationId = verificationId;
            }
        };
    }

    private void setUpPhoneSignBtn() {
        b.phoneSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                b.phoneSignIn.setVisibility(View.GONE);
                b.continueWithPnoButn.setVisibility(View.VISIBLE);
                b.phoneNoEditText.setVisibility(View.VISIBLE);

                setUpContinueWithPnoBtn();
            }
        });
    }

    private void setUpContinueWithPnoBtn() {
        b.continueWithPnoButn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                userMobNo = b.phoneNoEditText.getText().toString().trim();
                if (userMobNo.isEmpty() || userMobNo.length() < 10){
                    b.phoneNoEditText.setError("Enter a valid number");
                    b.phoneNoEditText.requestFocus();
                    return;
                }
                if (app.isOffline()){
                    Toast.makeText(SignInActivity.this, " You are offline!", Toast.LENGTH_SHORT).show();
                    return;
                }
                //app.showLoadingDialog(SignInActivity.this);
                sendVerificationCode(userMobNo);
                b.continueWithPnoButn.setVisibility(View.GONE);
                b.phoneNoEditText.setVisibility(View.GONE);
                b.verifyCodeEditText.setVisibility(View.VISIBLE);
                b.verifyButton.setVisibility(View.VISIBLE);
                setUpVerifyBtn(userMobNo);
            }
        });
    }

    private void setUpVerifyBtn(String mobNo) {

        b.verifyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String verCode = b.verifyCodeEditText.getText().toString().trim();
                if (verCode.isEmpty() || verCode.length() < 6){
                    b.verifyCodeEditText.setError("Enter Valid Code");
                    b.verifyCodeEditText.requestFocus();
                    return;
                }

                verifyVerificationCode(verCode);
            }
        });

    }

    private void verifyVerificationCode(String verCode) {
        // creating the credentials
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId,verCode);

        // signIn the user
        signInWithPhoneAuthCredentials(credential);
    }

    private void signInWithPhoneAuthCredentials(PhoneAuthCredential credential) {
        mFirebaseAuth.signInWithCredential(credential)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        addUserPhNoToFirebase(userMobNo);

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(SignInActivity.this, "Failure\n"+e, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void addUserPhNoToFirebase(String mobNo) {

        Map<String , Object> map = new HashMap<>();
        map.put("TS",new Timestamp(new Date()));

        app.db.collection(Constants.USER_COLLECTION).document("+91"+mobNo)
                .set(map)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //app.hideLoadingDialog();
                        /*app.db.collection(Constants.USER_COLLECTION).document("+91"+mobNo).collection(Constants.USER_CONTACTS_COLLECTION).document("Hello")
                                .set(map)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {

                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(SignInActivity.this, "Sign In Failed! \nPlease sign in again", Toast.LENGTH_SHORT).show();
                                    }
                                });*/
                        Toast.makeText(SignInActivity.this, "Welcome!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(SignInActivity.this,MainActivity.class).putExtra("User MobNo",mobNo));
                        finish();

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //app.hideLoadingDialog();
                        Toast.makeText(SignInActivity.this, "Sign In Failed! \nPlease sign in again", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendVerificationCode(String mobNo) {

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mFirebaseAuth)
                .setPhoneNumber("+91"+mobNo)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(mCallback)
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }
}