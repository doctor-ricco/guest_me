package com.example.guestme;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class HostAdapter extends RecyclerView.Adapter<HostAdapter.HostViewHolder> {
    private List<HostModel> hosts = new ArrayList<>();

    public void setHosts(List<HostModel> hosts) {
        this.hosts = hosts;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_host, parent, false);
        return new HostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HostViewHolder holder, int position) {
        HostModel host = hosts.get(position);
        holder.bind(host);
    }

    @Override
    public int getItemCount() {
        return hosts.size();
    }

    static class HostViewHolder extends RecyclerView.ViewHolder {
        private ImageView hostImage;
        private TextView hostName;
        private TextView hostLocation;
        private TextView hostDescription;
        private TextView matchPercentage;

        public HostViewHolder(@NonNull View itemView) {
            super(itemView);
            hostImage = itemView.findViewById(R.id.hostImage);
            hostName = itemView.findViewById(R.id.hostName);
            hostLocation = itemView.findViewById(R.id.hostLocation);
            hostDescription = itemView.findViewById(R.id.hostDescription);
            matchPercentage = itemView.findViewById(R.id.matchPercentage);
        }

        public void bind(HostModel host) {
            hostName.setText(host.getFullName());
            hostLocation.setText(String.format("%s, %s", host.getCity(), host.getCountry()));
            hostDescription.setText(host.getDescription());
            matchPercentage.setText(String.format("%.0f%% Match", host.getMatchPercentage()));

            if (host.getPhotoUrl() != null && !host.getPhotoUrl().isEmpty()) {
                Picasso.get()
                        .load(host.getPhotoUrl())
                        .placeholder(R.drawable.profile)
                        .error(R.drawable.profile)
                        .into(hostImage);
            }
        }
    }
} 