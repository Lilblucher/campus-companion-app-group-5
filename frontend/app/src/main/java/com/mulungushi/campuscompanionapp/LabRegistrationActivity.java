package com.mulungushi.campuscompanionapp;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class LabRegistrationActivity extends BaseActivity {

    private static final String OWN_STUDENT_NUMBER = "210482910";
    private static final int MAX_TEAM_SIZE = 4;

    /* ---------- sample data ---------- */

    private static class LabSlot {
        final String name;
        final String time;
        final int seatsLeft;
        LabSlot(String name, String time, int seatsLeft) {
            this.name = name;
            this.time = time;
            this.seatsLeft = seatsLeft;
        }
    }

    private static class Teammate {
        final String studentNumber;
        final String name;
        final String department;
        Teammate(String studentNumber, String name, String department) {
            this.studentNumber = studentNumber;
            this.name = name;
            this.department = department;
        }
    }

    /* Known students lookup (hardcoded sample data) */
    private static final Teammate[] KNOWN_STUDENTS = {
        new Teammate("210398712", "Mwansa Banda", "CS"),
        new Teammate("210501234", "Chanda Phiri", "IT"),
        new Teammate("210674589", "Bwalya Tembo", "CS"),
        new Teammate("210812345", "Kasonde Mwila", "ENG"),
    };

    private final LabSlot[] labSlots = {
        new LabSlot("Lab A", "Mon 08:00 - 10:00", 3),
        new LabSlot("Lab B", "Tue 14:00 - 16:00", 1),
        new LabSlot("Lab C", "Thu 10:00 - 12:00", 6),
        new LabSlot("Lab D", "Fri 08:00 - 10:00", 0),
    };

    private final List<Teammate> teamMembers = new ArrayList<>();
    private int selectedSlotIndex = -1;
    private boolean submitted = false;

    private LinearLayout containerTeamMembers;
    private TextView tvMemberCount;
    private EditText etTeammateNumber;
    private TextView tvAddError;
    private GridLayout gridSlots;
    private TextView btnSubmit;
    private LinearLayout addTeammateRow;

    // Success state
    private View heroCard;
    private View teamCard;
    private View btnSubmitView;
    private LinearLayout successCard;
    private TextView tvSuccessTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lab_registration);

        containerTeamMembers = findViewById(R.id.containerTeamMembers);
        tvMemberCount = findViewById(R.id.tvMemberCount);
        etTeammateNumber = findViewById(R.id.etTeammateNumber);
        tvAddError = findViewById(R.id.tvAddError);
        gridSlots = findViewById(R.id.gridSlots);
        btnSubmit = findViewById(R.id.btnSubmit);
        addTeammateRow = findViewById(R.id.addTeammateRow);
        heroCard = findViewById(R.id.heroCard);
        teamCard = findViewById(R.id.teamCard);
        successCard = findViewById(R.id.successCard);
        tvSuccessTitle = findViewById(R.id.tvSuccessTitle);

        findViewById(R.id.btnBack).setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, 0);
        });

        findViewById(R.id.btnBell).setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationsActivity.class));
            overridePendingTransition(0, 0);
        });

        findViewById(R.id.btnAddTeammate).setOnClickListener(v -> addTeammate());
        btnSubmit.setOnClickListener(v -> submitRegistration());

        renderTeam();
        renderSlots();
    }

    /* ---- Add teammate validation ---- */

    private void addTeammate() {
        tvAddError.setVisibility(View.GONE);
        String input = etTeammateNumber.getText().toString().trim();

        if (TextUtils.isEmpty(input)) {
            showAddError("Enter a student number.");
            return;
        }
        if (input.length() != 9) {
            showAddError("Student number must be 9 digits.");
            return;
        }
        if (input.equals(OWN_STUDENT_NUMBER)) {
            showAddError("You can't add yourself.");
            return;
        }
        // Already in team?
        for (Teammate t : teamMembers) {
            if (t.studentNumber.equals(input)) {
                showAddError("Already in your team.");
                return;
            }
        }
        if (teamMembers.size() + 1 >= MAX_TEAM_SIZE) { // +1 for team lead
            showAddError("Team is full (max " + MAX_TEAM_SIZE + ").");
            return;
        }
        // Look up
        Teammate found = null;
        for (Teammate s : KNOWN_STUDENTS) {
            if (s.studentNumber.equals(input)) {
                found = s;
                break;
            }
        }
        if (found == null) {
            showAddError("Student not found. Check the number and try again.");
            return;
        }

        teamMembers.add(found);
        etTeammateNumber.setText("");
        renderTeam();
    }

    private void showAddError(String msg) {
        tvAddError.setText(msg);
        tvAddError.setVisibility(View.VISIBLE);
    }

    /* ---- Render team members ---- */

    private void renderTeam() {
        containerTeamMembers.removeAllViews();

        // Team lead (always first, no remove button)
        containerTeamMembers.addView(
            buildMemberRow("CM", "Chileshe Mulenga (You)",
                OWN_STUDENT_NUMBER + " · CS · Team lead", false));

        for (Teammate t : teamMembers) {
            String initials = initials(t.name);
            String meta = t.studentNumber + " · " + t.department;
            containerTeamMembers.addView(buildMemberRow(initials, t.name, meta, true));
        }

        int total = 1 + teamMembers.size();
        tvMemberCount.setText(total + "/" + MAX_TEAM_SIZE + " members");
        btnSubmit.setText("Register team (" + total + ")");

        // Hide add row if full
        if (total >= MAX_TEAM_SIZE) {
            addTeammateRow.setVisibility(View.GONE);
        } else {
            addTeammateRow.setVisibility(View.VISIBLE);
        }
    }

    private View buildMemberRow(String initials, String name, String meta, boolean showRemove) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(60));
        rlp.bottomMargin = dp(2);
        row.setLayoutParams(rlp);

        // Crimson avatar circle with initials
        TextView avatar = new TextView(this);
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(dp(40), dp(40));
        avatar.setLayoutParams(alp);
        avatar.setBackground(ContextCompat.getDrawable(this, R.drawable.cc_avatar_crimson));
        avatar.setGravity(Gravity.CENTER);
        avatar.setText(initials);
        avatar.setTextColor(0xFFFFFFFF);
        avatar.setTextSize(13);
        avatar.setTypeface(avatar.getTypeface(), Typeface.BOLD);
        row.addView(avatar);

        // Name + meta
        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tlp.leftMargin = dp(12);
        texts.setLayoutParams(tlp);

        TextView tvName = new TextView(this);
        tvName.setText(name);
        tvName.setTextColor(ContextCompat.getColor(this, R.color.cc_text_primary));
        tvName.setTextSize(14);
        tvName.setTypeface(tvName.getTypeface(), Typeface.BOLD);
        texts.addView(tvName);

        TextView tvMeta = new TextView(this);
        tvMeta.setText(meta);
        tvMeta.setTextColor(ContextCompat.getColor(this, R.color.cc_text_secondary));
        tvMeta.setTextSize(12);
        LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        mlp.topMargin = dp(2);
        tvMeta.setLayoutParams(mlp);
        texts.addView(tvMeta);

        row.addView(texts);

        // Remove button
        if (showRemove) {
            ImageView btnRemove = new ImageView(this);
            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(dp(36), dp(36));
            btnRemove.setLayoutParams(blp);
            btnRemove.setImageResource(R.drawable.ic_ms_close);
            btnRemove.setColorFilter(ContextCompat.getColor(this, R.color.cc_text_muted));
            btnRemove.setPadding(dp(6), dp(6), dp(6), dp(6));
            btnRemove.setBackground(ContextCompat.getDrawable(this,
                android.R.drawable.list_selector_background));
            btnRemove.setContentDescription("Remove " + name);
            // Find teammate by initials+name to remove
            final String memberName = name;
            btnRemove.setOnClickListener(v -> {
                for (int i = 0; i < teamMembers.size(); i++) {
                    if (teamMembers.get(i).name.equals(memberName)) {
                        teamMembers.remove(i);
                        break;
                    }
                }
                renderTeam();
            });
            row.addView(btnRemove);
        }

        return row;
    }

    /* ---- Render lab slot grid ---- */

    private void renderSlots() {
        gridSlots.removeAllViews();
        for (int i = 0; i < labSlots.length; i++) {
            gridSlots.addView(buildSlotTile(i));
        }
    }

    private View buildSlotTile(int index) {
        LabSlot slot = labSlots[index];
        boolean isFull = slot.seatsLeft == 0;
        boolean isSelected = index == selectedSlotIndex;

        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);

        GridLayout.LayoutParams glp = new GridLayout.LayoutParams();
        glp.width = 0;
        glp.height = GridLayout.LayoutParams.WRAP_CONTENT;
        glp.columnSpec = GridLayout.spec(index % 2, 1f);
        glp.rowSpec = GridLayout.spec(index / 2);
        glp.setMargins(dp(4), dp(4), dp(4), dp(4));
        tile.setLayoutParams(glp);

        tile.setMinimumWidth(dp(140));
        tile.setMinimumHeight(dp(90));
        tile.setPadding(dp(12), dp(14), dp(12), dp(14));

        if (isSelected && !isFull) {
            tile.setBackgroundResource(R.drawable.cc_lab_slot_selected);
        } else {
            tile.setBackgroundResource(R.drawable.cc_lab_slot);
        }

        if (isFull) {
            tile.setAlpha(0.45f);
        } else {
            tile.setAlpha(1.0f);
        }

        // Lab name
        TextView tvName = new TextView(this);
        tvName.setText(slot.name);
        tvName.setTextColor(ContextCompat.getColor(this, R.color.cc_text_primary));
        tvName.setTextSize(15);
        tvName.setTypeface(tvName.getTypeface(), Typeface.BOLD);
        tvName.setGravity(Gravity.CENTER);
        tile.addView(tvName);

        // Time
        TextView tvTime = new TextView(this);
        tvTime.setText(slot.time);
        tvTime.setTextColor(ContextCompat.getColor(this, R.color.cc_text_secondary));
        tvTime.setTextSize(12);
        tvTime.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tlp.topMargin = dp(4);
        tvTime.setLayoutParams(tlp);
        tile.addView(tvTime);

        // Seats info
        TextView tvSeats = new TextView(this);
        if (isFull) {
            tvSeats.setText("Full");
            tvSeats.setTextColor(ContextCompat.getColor(this, R.color.cc_crimson));
        } else {
            tvSeats.setText(slot.seatsLeft + " seats left");
            tvSeats.setTextColor(ContextCompat.getColor(this, R.color.cc_success));
        }
        tvSeats.setTextSize(12);
        tvSeats.setTypeface(tvSeats.getTypeface(), Typeface.BOLD);
        tvSeats.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        slp.topMargin = dp(6);
        tvSeats.setLayoutParams(slp);
        tile.addView(tvSeats);

        if (!isFull) {
            tile.setOnClickListener(v -> {
                if (submitted) return;
                selectedSlotIndex = index;
                renderSlots();
            });
        }

        return tile;
    }

    /* ---- Submit ---- */

    private void submitRegistration() {
        if (submitted) return;
        if (selectedSlotIndex < 0) {
            showAddError("Select a lab slot first.");
            return;
        }

        submitted = true;
        LabSlot chosen = labSlots[selectedSlotIndex];

        // Show success, hide form elements
        heroCard.setVisibility(View.GONE);
        teamCard.setVisibility(View.GONE);
        btnSubmit.setVisibility(View.GONE);
        // hide the "Choose a lab slot" title + grid
        // We navigate up from gridSlots to hide the title too
        View slotTitle = null;
        ViewGroup parent = (ViewGroup) gridSlots.getParent();
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child instanceof TextView) {
                CharSequence txt = ((TextView) child).getText();
                if (txt != null && txt.toString().equals("Choose a lab slot")) {
                    slotTitle = child;
                    break;
                }
            }
        }
        if (slotTitle != null) slotTitle.setVisibility(View.GONE);
        gridSlots.setVisibility(View.GONE);

        tvSuccessTitle.setText("Team registered for " + chosen.name + " (" + chosen.time + ")");
        successCard.setVisibility(View.VISIBLE);
    }

    /* ---- Utilities ---- */

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private static String initials(String name) {
        if (name == null || name.isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }
}
