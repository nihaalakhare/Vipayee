package com.example.vipayee.model;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vipayee.R;

import java.util.List;

public class MiniStatementAdapter
        extends RecyclerView.Adapter<MiniStatementAdapter.ViewHolder> {

    private final List<TransactionModel> list;

    public MiniStatementAdapter(List<TransactionModel> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the new layout that matches the image UI
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int i) {
        TransactionModel t = list.get(i);

        // Set Basic Data
        h.tvAmount.setText("₹" + t.amount);
        h.tvRemark.setText(t.remark);
        h.tvDate.setText(t.date);

        // Logic to match the UI Image states
        if ("CREDIT".equals(t.type)) {
            h.tvType.setText("Credited");
//            h.tvActionLabel.setText("Received from");

            // In your image, received (Credit) arrows point Down-Left
            h.ivArrow.setRotation(180f);

            // Optional: If you want the 'Credited' text to be green
            // h.tvType.setTextColor(Color.parseColor("#2E7D32"));
        } else {
            h.tvType.setText("Debited");
//            h.tvActionLabel.setText("Paid to");

            // In your image, paid (Debit) arrows point Up-Right
            h.ivArrow.setRotation(0f);

            // Optional: If you want the 'Debited' text to be red
            // h.tvType.setTextColor(Color.parseColor("#C62828"));
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        // Added tvActionLabel and ivArrow to match the new XML
        TextView tvType, tvAmount, tvRemark, tvDate, tvActionLabel;
        ImageView ivArrow;

        ViewHolder(View v) {
            super(v);
            tvType = v.findViewById(R.id.tvType);
            tvAmount = v.findViewById(R.id.tvAmount);
            tvRemark = v.findViewById(R.id.tvRemark);
            tvDate = v.findViewById(R.id.tvDate);
            tvActionLabel = v.findViewById(R.id.tvActionLabel);
            ivArrow = v.findViewById(R.id.ivArrow);
        }
    }
}