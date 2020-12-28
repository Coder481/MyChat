package com.example.mychat;

import android.content.Context;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mychat.databinding.ActivityMessagingBinding;
import com.example.mychat.databinding.MessageLayoutAdapterBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Context context;
    public List<String> messageList;
    private String userMobNo;

    int lastSelectedItemPosition;

    public MessageAdapter(Context context, List<String> messageList, String userMobNo) {
        this.context = context;
        this.messageList = messageList;
        this.userMobNo = userMobNo;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        MessageLayoutAdapterBinding b = MessageLayoutAdapterBinding.inflate(LayoutInflater.from(context)
                ,parent,false);
        return new MessageViewHolder(b);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MessageLayoutAdapterBinding b = ((MessageViewHolder)holder).b;
        String[] strings = messageList.get(position).split("``;;;```&&&#&&@@###");

        if (strings[2].equals(userMobNo)){
            if (strings[0].length() > strings[1].length()){
                String s = " ";
                StringBuilder spaceBuild = new StringBuilder();
                int dif = strings[0].length() - strings[1].length();
                for (int i = 0; i < dif; i++){
                    spaceBuild.append(s);
                }

                if (spaceBuild.length()>50){
                    int spaceDif = spaceBuild.length() - 50;
                    /*for (int j = 0; j<spaceDif;j++){
                        spaceBuild.deleteCharAt(j);
                    }*/
                    spaceBuild.replace(0,spaceDif,"");
                }
                String spaces = spaceBuild.toString();
                b.youTimeTextView.setText(spaces+strings[1]);
            }else {
                b.youTimeTextView.setText(strings[1]);
            }
            b.youMessageTextView.setText(strings[0]);
            b.youTimeTextView.setVisibility(View.VISIBLE);
            b.youMessageTextView.setVisibility(View.VISIBLE);
            b.MessageTextView.setVisibility(View.GONE);
            b.timeTextView.setVisibility(View.GONE);

        }else {
            if (strings[0].length() > strings[1].length()){
                String s = " ";
                StringBuilder spaceBuild = new StringBuilder();
                int dif = strings[0].length() - strings[1].length();
                for (int i = 0; i < dif; i++){
                    spaceBuild.append(s);
                }
                if (spaceBuild.length()>50){
                    int spaceDif = spaceBuild.length() - 50;
                    /*for (int j = 0; j<spaceDif;j++){
                        spaceBuild.deleteCharAt(j);
                    }*/
                    spaceBuild.replace(0,spaceDif,"");
                }

                String spaces = spaceBuild.toString();
                b.timeTextView.setText(strings[1]+spaces);
            }else {
                b.timeTextView.setText(strings[1]);
            }

            b.MessageTextView.setText(strings[0]);
            b.timeTextView.setVisibility(View.VISIBLE);
            b.MessageTextView.setVisibility(View.VISIBLE);
            b.youMessageTextView.setVisibility(View.GONE);
            b.youTimeTextView.setVisibility(View.GONE);
        }
        setUpContextualMenu(b.getRoot());

        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                lastSelectedItemPosition = holder.getAdapterPosition();
                return false;
            }
        });
    }



    @Override
    public int getItemCount() {
        return messageList.size();
    }

    private void setUpContextualMenu(ConstraintLayout root) {
        root.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                if (!(context instanceof MessagingActivity))
                    return;
                MessagingActivity activity = ((MessagingActivity)context);
                activity.getMenuInflater().inflate(R.menu.message_contextual_menu,menu);
            }
        });
    }


    public static class MessageViewHolder extends RecyclerView.ViewHolder{

        private  MessageLayoutAdapterBinding b;

        public MessageViewHolder(@NonNull MessageLayoutAdapterBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
