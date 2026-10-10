package com.serviceapp.adapters;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.serviceapp.R;
import com.serviceapp.models.Message;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private final List<Message> messages;
    private final String currentUserId;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public MessageAdapter(List<Message> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        Message msg = messages.get(position);
        return msg.getSenderId().equals(currentUserId) ? VIEW_TYPE_SENT : VIEW_TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent, parent, false);
            return new SentViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_received, parent, false);
            return new ReceivedViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message msg = messages.get(position);
        String time = timeFormat.format(new Date(msg.getTimestamp()));

        if (holder instanceof SentViewHolder) {
            SentViewHolder svh = (SentViewHolder) holder;
            svh.tvMessage.setText(msg.getContent());
            svh.tvTime.setText(time);
            if (Message.TYPE_IMAGE.equals(msg.getType()) && !TextUtils.isEmpty(msg.getImageUrl())) {
                svh.ivImage.setVisibility(View.VISIBLE);
                Glide.with(holder.itemView.getContext()).load(msg.getImageUrl())
                        .centerCrop().into(svh.ivImage);
            } else {
                svh.ivImage.setVisibility(View.GONE);
            }
        } else if (holder instanceof ReceivedViewHolder) {
            ReceivedViewHolder rvh = (ReceivedViewHolder) holder;
            rvh.tvMessage.setText(msg.getContent());
            rvh.tvTime.setText(time);
            if (Message.TYPE_IMAGE.equals(msg.getType()) && !TextUtils.isEmpty(msg.getImageUrl())) {
                rvh.ivImage.setVisibility(View.VISIBLE);
                Glide.with(holder.itemView.getContext()).load(msg.getImageUrl())
                        .centerCrop().into(rvh.ivImage);
            } else {
                rvh.ivImage.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() { return messages.size(); }

    static class SentViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;
        ImageView ivImage;
        public SentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_message);
            tvTime = itemView.findViewById(R.id.tv_time);
            ivImage = itemView.findViewById(R.id.iv_image);
        }
    }

    static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;
        ImageView ivImage;
        public ReceivedViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_message);
            tvTime = itemView.findViewById(R.id.tv_time);
            ivImage = itemView.findViewById(R.id.iv_image);
        }
    }
}
