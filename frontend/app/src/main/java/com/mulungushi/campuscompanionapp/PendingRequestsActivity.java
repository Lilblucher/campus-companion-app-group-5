package com.mulungushi.campuscompanionapp;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mulungushi.campuscompanionapp.network.ApiClient;
import com.mulungushi.campuscompanionapp.network.ApiService;
import com.mulungushi.campuscompanionapp.network.MessageResponse;
import com.mulungushi.campuscompanionapp.network.RequestsResponse;
import com.mulungushi.campuscompanionapp.util.Ui;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PendingRequestsActivity extends BaseActivity {

    private ApiService api;
    private LinearLayout list;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        api = ApiClient.getClient(this).create(ApiService.class);
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(Ui.PAGE_BG);
        LinearLayout root = Ui.vbox(this);
        sv.addView(root);
        setContentView(sv);
        root.addView(Ui.header(this, "Pending Requests"));
        list = Ui.vbox(this);
        int p = Ui.dp(this, 16);
        list.setPadding(p, 0, p, p);
        root.addView(list);
        reload();
    }

    private void reload() {
        list.removeAllViews();
        list.addView(Ui.placeholder(this, "Loading…"));
        api.getRequests("pending").enqueue(new Callback<RequestsResponse>() {
            @Override public void onResponse(Call<RequestsResponse> c, Response<RequestsResponse> r) {
                list.removeAllViews();
                if (!r.isSuccessful() || r.body() == null || r.body().requests == null) { list.addView(Ui.placeholder(PendingRequestsActivity.this, "Couldn't load requests.")); return; }
                if (r.body().requests.isEmpty()) { list.addView(Ui.placeholder(PendingRequestsActivity.this, "No pending requests.")); return; }
                for (RequestsResponse.Request req : r.body().requests) list.addView(card(req));
            }
            @Override public void onFailure(Call<RequestsResponse> c, Throwable t) { list.removeAllViews(); list.addView(Ui.placeholder(PendingRequestsActivity.this, "Network error.")); }
        });
    }

    private View card(RequestsResponse.Request req) {
        LinearLayout card = Ui.card(this);

        // Header: icon chip + type · time
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);

        ImageView icon = new ImageView(this);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(Ui.dp(this, 40), Ui.dp(this, 40));
        icon.setLayoutParams(ilp);
        icon.setBackgroundResource(R.drawable.cc_iconchip_blue);
        icon.setPadding(Ui.dp(this, 9), Ui.dp(this, 9), Ui.dp(this, 9), Ui.dp(this, 9));
        icon.setImageResource(R.drawable.ic_ms_swap_horiz);
        icon.setColorFilter(0xFF0457DD);
        head.addView(icon);

        LinearLayout headTexts = new LinearLayout(this);
        headTexts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams htlp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        htlp.leftMargin = Ui.dp(this, 12);
        headTexts.setLayoutParams(htlp);
        TextView type = Ui.text(this, "Group change", Ui.TEXT, 14, true);
        headTexts.addView(type);
        headTexts.addView(Ui.text(this, req.student_name + " · " + req.student_number, Ui.MUTED, 12, false));
        head.addView(headTexts);
        card.addView(head);

        String from = req.current_group_name == null ? "Unassigned" : req.current_group_name;
        TextView detail = Ui.text(this, from + "  →  " + (req.requested_group_name == null ? "?" : req.requested_group_name), Ui.NAVY, 13, true);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        dlp.topMargin = Ui.dp(this, 10);
        detail.setLayoutParams(dlp);
        card.addView(detail);
        if (req.reason != null && !req.reason.isEmpty()) card.addView(Ui.text(this, "Reason: " + req.reason, Ui.MUTED, 12, false));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rlp.topMargin = Ui.dp(this, 12);
        row.setLayoutParams(rlp);

        Button decline = pillButton("Decline", R.drawable.cc_btn_outline_crimson, 0xFFE51937);
        Button approve = pillButton("Approve", R.drawable.cc_btn_navy, 0xFFFFFFFF);
        LinearLayout.LayoutParams blp1 = new LinearLayout.LayoutParams(0, Ui.dp(this, 44), 1f);
        blp1.rightMargin = Ui.dp(this, 6);
        decline.setLayoutParams(blp1);
        LinearLayout.LayoutParams blp2 = new LinearLayout.LayoutParams(0, Ui.dp(this, 44), 1f);
        blp2.leftMargin = Ui.dp(this, 6);
        approve.setLayoutParams(blp2);

        approve.setOnClickListener(v -> decide(req.request_id, true));
        decline.setOnClickListener(v -> decide(req.request_id, false));
        row.addView(decline); row.addView(approve);
        card.addView(row);
        return card;
    }

    private Button pillButton(String text, int bgRes, int textColor) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(text);
        b.setTextColor(textColor);
        b.setBackgroundResource(bgRes);
        b.setTypeface(b.getTypeface(), Typeface.BOLD);
        return b;
    }

    private void decide(long id, boolean approve) {
        Callback<MessageResponse> cb = new Callback<MessageResponse>() {
            @Override public void onResponse(Call<MessageResponse> c, Response<MessageResponse> r) {
                if (r.isSuccessful()) { Toast.makeText(PendingRequestsActivity.this, approve ? "Approved" : "Rejected", Toast.LENGTH_SHORT).show(); reload(); }
                else Toast.makeText(PendingRequestsActivity.this, r.body() != null && r.body().error != null ? r.body().error : "Failed (" + r.code() + ")", Toast.LENGTH_SHORT).show();
            }
            @Override public void onFailure(Call<MessageResponse> c, Throwable t) { Toast.makeText(PendingRequestsActivity.this, "Network error", Toast.LENGTH_SHORT).show(); }
        };
        if (approve) api.approveRequest(id).enqueue(cb); else api.rejectRequest(id).enqueue(cb);
    }
}
