package com.example.mychat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mychat.databinding.ActivityMessagingBinding;
import com.example.mychat.databinding.MessageLayoutAdapterBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Context context;
    private List<String> messageList;

    public MessageAdapter(Context context, List<String> messageList) {
        this.context = context;
        this.messageList = messageList;
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
        b.MessageTextView.setText(strings[0]);
        b.timeTextView.setText(strings[1]);
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder{

        private  MessageLayoutAdapterBinding b;

        public MessageViewHolder(@NonNull MessageLayoutAdapterBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
