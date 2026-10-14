package com.mulungushi.campuscompanionapp;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class AccountsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accounts);

        findViewById(R.id.btnBack).setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, 0);
        });

        findViewById(R.id.btnBell).setOnClickListener(v ->
                startActivity(new Intent(this, NotificationsActivity.class)));

        // Position the 50% marker at the centre of the progress bar
        ProgressBar progressBar = findViewById(R.id.progressTuition);
        View marker = findViewById(R.id.marker50);
        progressBar.post(() -> {
            int halfW = progressBar.getWidth() / 2;
            ViewGroup.MarginLayoutParams lp =
                    (ViewGroup.MarginLayoutParams) marker.getLayoutParams();
            lp.leftMargin = halfW;
            marker.setLayoutParams(lp);
        });

        populatePayments();
    }

    /* -------- hardcoded sample data -------- */

    private void populatePayments() {
        LinearLayout list = findViewById(R.id.paymentList);

        String[][] payments = {
                {"Tuition payment",   "12 Jan 2026 - REF-20260112-001", "K3,000"},
                {"Tuition payment",   "15 Mar 2026 - REF-20260315-002", "K2,000"},
                {"Library clearance", "20 May 2026 - REF-20260520-003", "K1,000"},
        };

        for (String[] p : payments) {
            list.addView(buildPaymentRow(p[0], p[1], p[2]));
        }
    }

    private View buildPaymentRow(String label, String meta, String amount) {
        int dp16 = dp(16), dp12 = dp(12), dp10 = dp(10), dp48 = dp(48), dp40 = dp(40);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(R.drawable.cc_card_bordered);
        row.setPadding(dp16, dp12, dp16, dp12);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rowLp.bottomMargin = dp(8);
        row.setLayoutParams(rowLp);

        // Icon chip
        ImageView icon = new ImageView(this);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(dp48, dp48);
        iconLp.setMarginEnd(dp12);
        icon.setLayoutParams(iconLp);
        icon.setBackgroundResource(R.drawable.cc_iconchip_blue);
        icon.setImageResource(R.drawable.ic_ms_receipt_long);
        icon.setColorFilter(ContextCompat.getColor(this, R.color.cc_navy));
        icon.setPadding(dp12, dp12, dp12, dp12);
        row.addView(icon);

        // Text column
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        col.setLayoutParams(colLp);

        TextView tvLabel = new TextView(this);
        tvLabel.setText(label);
        tvLabel.setTextColor(ContextCompat.getColor(this, R.color.cc_text_primary));
        tvLabel.setTextSize(14);
        tvLabel.setTypeface(tvLabel.getTypeface(), Typeface.BOLD);
        col.addView(tvLabel);

        TextView tvMeta = new TextView(this);
        tvMeta.setText(meta);
        tvMeta.setTextColor(ContextCompat.getColor(this, R.color.cc_text_muted));
        tvMeta.setTextSize(12);
        col.addView(tvMeta);

        row.addView(col);

        // Amount
        TextView tvAmount = new TextView(this);
        tvAmount.setText(amount);
        tvAmount.setTextColor(ContextCompat.getColor(this, R.color.cc_success));
        tvAmount.setTextSize(14);
        tvAmount.setTypeface(tvAmount.getTypeface(), Typeface.BOLD);
        row.addView(tvAmount);

        return row;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
