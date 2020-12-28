package com.example.mychat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.example.mychat.databinding.ActivityMainBinding;
import com.example.mychat.databinding.AddNewUserLayoutBinding;
import com.example.mychat.databinding.ContactListLayoutBinding;
import com.example.mychat.databinding.DeleteContactDialogLayoutBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    // Phone Authentication
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;

    // View Binding
    private ActivityMainBinding b;
    private AddNewUserLayoutBinding addNewUserBinding;

    private MyApp app;
    private String userMobNo;
    private AlertDialog addNewUserDialog;
    private long userContactsNumber;
    private String userContactsName;
    private boolean isContactsAdded = false;
    private ContactListLayoutBinding itemBinding;
    private List<String> contactsList = new ArrayList<>();
    private SharedPreferences msharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        app = (MyApp)getApplicationContext();

        /*Intent signInIntent = getIntent();
        userMobNo = signInIntent.getStringExtra("User MobNo");*/


        // Initialize Firebase Auth
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();

        if (mFirebaseUser == null){
            // Not signed in , launch the sign Activity
            startActivity(new Intent(this,SignInActivity.class));
            finish();
            return;
        }

        userMobNo = mFirebaseUser.getPhoneNumber();
        //userMobNo = "+918696401008";
        refreshContactsList();
    }





    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_options_menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.refresh_btn);
        item.setEnabled(isContactsAdded);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.signout_btn:
                askForConfirmation();
                return true;
            case R.id.add_newuser_btn:
                addNewUser();
                return true;
            case R.id.refresh_btn:
                refreshContactsList();
                return true;
            case R.id.delete_contact_btn:
                showDeleteContactDialog();
                return true;
            case R.id.share_btn:
                try {
                    shareApk();
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showDeleteContactDialog() {
        DeleteContactDialogLayoutBinding binding = DeleteContactDialogLayoutBinding.inflate(getLayoutInflater());
        new AlertDialog.Builder(this)
                .setTitle("Enter Phone Number to delete")
                .setView(binding.getRoot())
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String numberStr = binding.deleteContactEditText.getText().toString().trim();
                        if ((numberStr.length()<13) ||  (!numberStr.contains("+91")) || (!isContactInList(numberStr))){
                            Toast.makeText(MainActivity.this, "Enter valid number", Toast.LENGTH_SHORT).show();
                        }else {
                            deleteContact(numberStr);
                        }
                    }
                })
                .show();
    }

    private boolean isContactInList(String numberStr) {
        Gson gson = new Gson();
        msharedPref = getSharedPreferences("contacts",MODE_PRIVATE);
        String json = msharedPref.getString("contactsList",null);
        if (json!=null){
            List<String> ctList  = gson.fromJson(json,new TypeToken<List<String>>(){}.getType());
            return ctList.contains(numberStr);
        }
        return false;
    }

    private void deleteContact(String num) {
        if (app.isOffline()){
            Toast.makeText(this, "Can't Delete. You are offline!", Toast.LENGTH_SHORT).show();
            return;
        }
        app.db.collection(Constants.USER_COLLECTION).document(userMobNo)
                .collection(Constants.USER_CONTACTS_COLLECTION).document(num)
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        refreshContactsList();
                        Toast.makeText(MainActivity.this, "Contact Deleted", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, "Contact can't be deleted. Following may be some issues:\n1. Network Problem\n2. Server Side Issue\n3. Invalid Number\n   Please retry again ", Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void refreshContactsList() {
        app.showLoadingDialog(this);
        app.db.collection(Constants.USER_COLLECTION).document(userMobNo)
                .collection(Constants.USER_CONTACTS_COLLECTION)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {

                        b.contactListLinearLayout.removeAllViews();
                        for (DocumentSnapshot doc : queryDocumentSnapshots){
                            itemBinding = ContactListLayoutBinding.inflate(getLayoutInflater());
                            if (doc.exists()){
                                try {
                                    String name = doc.get("Name").toString();
                                    String number = doc.get("Contact").toString();
                                    itemBinding.contactNameTextView.setText(name+"");
                                    itemBinding.contactNumberTextView.setText(number+"");
                                    setUpContactOnClickListener(number,name);
                                    b.contactListLinearLayout.addView(itemBinding.getRoot());
                                }catch (Exception e){
                                    Toast.makeText(MainActivity.this, "Add new contacts", Toast.LENGTH_SHORT).show();
                                }

                            }else {
                                Toast.makeText(MainActivity.this, "Add new contacts", Toast.LENGTH_SHORT).show();
                            }
                        }
                        app.hideLoadingDialog();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, "Refresh Again!", Toast.LENGTH_SHORT).show();
                        app.hideLoadingDialog();
                    }
                });
    }

    private void setUpContactOnClickListener(String number, String name) {
        itemBinding.moveToImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this,MessagingActivity.class)
                        .putExtra("ContactNo",number).putExtra("ContactName",name).putExtra("UserContactNumber",userMobNo));
            }
        });
    }

    private void addNewUser() {
        addNewUserBinding = AddNewUserLayoutBinding.inflate(getLayoutInflater());
        addNewUserDialog = new AlertDialog.Builder(this)
                .setTitle("Add User Details")
                .setMessage("Make sure this contact also uses My Chat app if not you can share the apk")
                .setView(addNewUserBinding.getRoot())
                .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        userContactsName = addNewUserBinding.contactNameEditText.getText().toString().trim();
                        String contNum = addNewUserBinding.contactNumberEditText.getText().toString().trim();
                        if (contNum.length() != 10 || userContactsName.length()==0){
                            Toast.makeText(MainActivity.this, "Invalid Contact Details", Toast.LENGTH_SHORT).show();
                        }else {
                            userContactsNumber = Long.parseLong(contNum);
                            addNewUserToFirebase(userContactsName, contNum);
                        }

                    }
                })
                .setNeutralButton("Share", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            shareApk();
                        } catch (PackageManager.NameNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                })
                .show();
    }

    private void shareApk() throws PackageManager.NameNotFoundException {
        ApplicationInfo appInfo = getApplicationContext().getApplicationInfo();
        String filePath = appInfo.publicSourceDir;

        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        Uri uri = Uri.parse(filePath);
        sharingIntent.setType("*/*");
        sharingIntent.putExtra(Intent.EXTRA_STREAM,uri);
        sharingIntent.putExtra(Intent.EXTRA_TEXT,"Click on the link to download apk  https://github.com/Coder481/MyChat/releases/download/Latest/app-debug.apk");
        startActivity(Intent.createChooser(sharingIntent,"Share app using"));
        //File file = new File();

        /*try {
            File tempFile = new File(getExternalCacheDir()+"/ExtractedApk");
            if (!tempFile.isDirectory()){
                if (!tempFile.mkdirs()){
                    return;
                }
            }
            tempFile = new File(tempFile.getPath()+"/"+"myChat"+".apk");
            if (!tempFile.exists()){
                if (!tempFile.createNewFile()){
                    return;
                }
            }

            InputStream in = new FileInputStream();
            OutputStream out = new FileOutputStream(tempFile);

        }catch (IOException e){
            e.printStackTrace();
        }*/
    }

    private void addNewUserToFirebase(String contactName, String contactNumber) {
        Map<String,Object> userMap = new HashMap<>();
        userMap.put("Name",contactName);
        userMap.put("Contact","+91"+contactNumber);
        app.db.collection(Constants.USER_COLLECTION).document(userMobNo)
                .collection(Constants.USER_CONTACTS_COLLECTION).document("+91"+contactNumber)
                .set(userMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        saveNewContactLocally("+91"+contactNumber);
                        Toast.makeText(MainActivity.this, "Contact Added!", Toast.LENGTH_SHORT).show();
                        refreshContactsList();
                        isContactsAdded = true;
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, "Contact cannot save", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveNewContactLocally(String s) {
        contactsList.add(s);
        msharedPref = getSharedPreferences("contacts",MODE_PRIVATE);
        Gson gson = new Gson();
        msharedPref.edit()
                .putString("contactsList",gson.toJson(contactsList))
                .apply();
    }


    /** Setting up sign out feature **/
    private void askForConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Are You sure to sign out?")
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setUpsignOut();
                    }
                })
                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(MainActivity.this, "Cancelled", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void setUpsignOut() {
        mFirebaseAuth.signOut();
        startActivity(new Intent(this,SignInActivity.class));
        finish();

    }
}