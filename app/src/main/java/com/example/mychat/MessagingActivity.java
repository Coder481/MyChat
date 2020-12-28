package com.example.mychat;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
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
    private MessageAdapter adapter;

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

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.delete_message_btn){
            deleteMessage();
        }
        return super.onContextItemSelected(item);
    }

    private void deleteMessage() {
        new AlertDialog.Builder(this)
                .setTitle("Sure to delete message?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        String[] strings = messagesList.get(adapter.lastSelectedItemPosition).split("``;;;```&&&#&&@@###");
                        if (!canMessageBeDelete(strings[1])){
                            Toast.makeText(MessagingActivity.this, "Message can be deleted within 24 hours", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (strings[0].equals(getString(R.string.deleteMessageString))){
                            Toast.makeText(MessagingActivity.this, "This message was already deleted", Toast.LENGTH_SHORT).show();
                            return;
                        }


                        messagesList.remove(adapter.lastSelectedItemPosition);
                        String str = getString(R.string.deleteMessageString)+"``;;;```&&&#&&@@###"+strings[1]+"``;;;```&&&#&&@@###"+strings[2];

                        messagesList.add(adapter.lastSelectedItemPosition,str);
                        addMessageListToFirebase(messagesList);

                        adapter.notifyItemChanged(adapter.lastSelectedItemPosition);
//                        adapter.notifyItemRemoved(adapter.lastSelectedItemPosition);
                    }
                })
                .show();
    }

    private boolean canMessageBeDelete(String s) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/mm/yyyy hh:mm a");
        String format = simpleDateFormat.format(new Date());
        String[] bfrStrings = s.split(" ");
        String[] aftrStrings = format.split(" ");
        int bfrHr = Integer.parseInt(bfrStrings[1].split(":")[0])
                , bfrMin = Integer.parseInt(bfrStrings[1].split(":")[1])
               , aftHr = Integer.parseInt(aftrStrings[1].split(":")[0])
                , aftMin = Integer.parseInt(aftrStrings[1].split(":")[1]);
        if (bfrStrings[2].equals("PM")){
            bfrHr = bfrHr + 12;
        }
        if (aftrStrings[2].equals("PM")){
            aftHr = aftHr + 12;
        }

        bfrMin = bfrMin + (bfrHr*60);
        aftMin = aftMin + (aftHr*60);
        if (bfrStrings[0].equals(aftrStrings[0]) && aftMin>=bfrMin){
            return true;
        }
        return bfrMin >= aftMin;
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
                    if (message.equals(getString(R.string.deleteMessageString))){
                        Toast.makeText(MessagingActivity.this, "Sorry you can't write this message as this message is same as deleted message", Toast.LENGTH_LONG).show();
                        return;
                    }
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/mm/yyyy hh:mm a");
                    String format = simpleDateFormat.format(new Date());
                    messagesList.add(message+"``;;;```&&&#&&@@###"+format+"``;;;```&&&#&&@@###"+userMobNo);
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
                                        Toast.makeText(MessagingActivity.this, "Task Done!", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(MessagingActivity.this, "Task can't be completed", Toast.LENGTH_SHORT).show();
                                    }
                                });

                    }
                });

    }

    private void setUpAdapter() {
        adapter = new MessageAdapter(this,messagesList,userMobNo);
        b.messageRecyclerView.setAdapter(adapter);
        b.messageRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        //b.messageRecyclerView.addItemDecoration(new DividerItemDecoration(this,DividerItemDecoration.VERTICAL));
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar!=null){
            actionBar.setTitle(contactName);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.message_activity_options_menu,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.refresh_messages_btn){
            getMessagesFromFirebase();
            Toast.makeText(this, "Refreshed", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}