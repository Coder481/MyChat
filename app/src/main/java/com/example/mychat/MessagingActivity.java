package com.example.mychat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

import com.example.mychat.databinding.ActivityMessagingBinding;
import com.example.mychat.databinding.MessageLayoutAdapterBinding;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessagingActivity extends AppCompatActivity {

    private ActivityMessagingBinding b;
    private String contactNumber;
    private String contactName;
    private List<String> messagesList = new ArrayList<>();
    private MyApp app;
    private String userMobNo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityMessagingBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        app = (MyApp)getApplicationContext();

        Intent intent = getIntent();
        contactNumber = intent.getStringExtra("ContactNo");
        contactName = intent.getStringExtra("ContactName");
        userMobNo = intent.getStringExtra("UserContactNumber");

        getMessagesFromFirebase();
        setUpSendBtn();
    }

    private void getMessagesFromFirebase() {
        if (app.isOffline()){
            Toast.makeText(this, "You Are Offline!", Toast.LENGTH_SHORT).show();
            return;
        }
        app.db.collection(Constants.CHAT_COLLECTION).document(userMobNo)
                .collection(Constants.MESSAGE_COLLECTION).document(contactNumber)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()){
                            messagesList = (List<String>) documentSnapshot.get("chats");
                            setUpAdapter();
                        }else {
                            Toast.makeText(MessagingActivity.this, "Start Chatting With "+contactName, Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MessagingActivity.this, "Failure!\n"+e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setUpSendBtn() {
        b.sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = b.messageEditText.getText().toString();
                if (message.length() == 0){
                    Toast.makeText(MessagingActivity.this, "Enter text to send!", Toast.LENGTH_SHORT).show();
                }else{
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm a");
                    String format = simpleDateFormat.format(new Date());
                    messagesList.add(message+"``;;;```&&&#&&@@###"+format);
                    b.messageEditText.setText("");
                    setUpAdapter();
                    addMessageListToFirebase(messagesList);
                }
            }
        });
    }

    private void addMessageListToFirebase(List<String> messagesList) {
        MessageLayoutAdapterBinding itemBinding = MessageLayoutAdapterBinding.inflate(getLayoutInflater());

        if (app.isOffline()){
            Toast.makeText(this, "Message can't sent\nYou are Offline", Toast.LENGTH_SHORT).show();
            return;
        }
        Map<String,Object> map = new HashMap<>();
        map.put("chats",messagesList);
        app.db.collection(Constants.CHAT_COLLECTION).document(userMobNo)
                .collection(Constants.MESSAGE_COLLECTION).document(contactNumber)
                .set(map)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        app.db.collection(Constants.CHAT_COLLECTION).document(contactNumber)
                                .collection(Constants.MESSAGE_COLLECTION).document(userMobNo)
                                .set(map)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Toast.makeText(MessagingActivity.this, "Send!", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(MessagingActivity.this, "Message can't sent", Toast.LENGTH_SHORT).show();
                                    }
                                });

                    }
                });

    }

    private void setUpAdapter() {
        MessageAdapter adapter = new MessageAdapter(this,messagesList);
        b.messageRecyclerView.setAdapter(adapter);
        b.messageRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        b.messageRecyclerView.addItemDecoration(new DividerItemDecoration(this,DividerItemDecoration.VERTICAL));
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar!=null){
            actionBar.setTitle(contactName);
        }

        return super.onPrepareOptionsMenu(menu);
    }
}