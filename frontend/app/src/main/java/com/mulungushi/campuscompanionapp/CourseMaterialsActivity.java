package com.mulungushi.campuscompanionapp;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class CourseMaterialsActivity extends BaseActivity {

    private static class PdfFile {
        final String name;
        final String size;
        PdfFile(String name, String size) { this.name = name; this.size = size; }
    }

    private static class CourseSection {
        final String code;
        final String title;
        final PdfFile[] files;
        CourseSection(String code, String title, PdfFile[] files) {
            this.code = code; this.title = title; this.files = files;
        }
    }

    private final CourseSection[] courses = {
        new CourseSection("ICT 361", "Software Engineering", new PdfFile[]{
            new PdfFile("Course outline", "0.4 MB"),
            new PdfFile("Lecture notes Weeks 1-6", "3.8 MB"),
            new PdfFile("Lab manual", "2.4 MB"),
            new PdfFile("Past exam paper 2025", "1.1 MB"),
        }),
        new CourseSection("CSC 311", "Algorithms & Data Structures", new PdfFile[]{
            new PdfFile("Course outline", "0.4 MB"),
            new PdfFile("Lecture notes Weeks 1-6", "3.8 MB"),
            new PdfFile("Lab manual", "2.4 MB"),
            new PdfFile("Past exam paper 2025", "1.1 MB"),
        }),
        new CourseSection("CSC 312", "Computer Networks", new PdfFile[]{
            new PdfFile("Course outline", "0.4 MB"),
            new PdfFile("Lecture notes Weeks 1-6", "3.8 MB"),
            new PdfFile("Lab manual", "2.4 MB"),
            new PdfFile("Past exam paper 2025", "1.1 MB"),
        }),
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_materials);

        findViewById(R.id.btnBack).setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, 0);
        });

        findViewById(R.id.btnBell).setOnClickListener(v ->
                startActivity(new Intent(this, NotificationsActivity.class)));

        populateCourses();
    }

    private void populateCourses() {
        LinearLayout courseList = findViewById(R.id.courseList);
        int totalPdfs = 0;
        for (CourseSection c : courses) totalPdfs += c.files.length;

        TextView tvSaved = findViewById(R.id.tvSavedCount);
        tvSaved.setText("0 of " + totalPdfs + " PDFs saved. Saved files open without a connection.");

        for (CourseSection course : courses) {
            courseList.addView(buildCourseSection(course));
        }
    }

    private View buildCourseSection(CourseSection course) {
        LinearLayout section = new LinearLayout(this);
        section.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.bottomMargin = dp(20);
        section.setLayoutParams(slp);

        // Header row: code + title, saved count, chevron
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hlp.bottomMargin = dp(10);
        header.setLayoutParams(hlp);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(course.code + " " + course.title);
        tvTitle.setTextColor(ContextCompat.getColor(this, R.color.cc_text_primary));
        tvTitle.setTextSize(14);
        tvTitle.setTypeface(tvTitle.getTypeface(), Typeface.BOLD);
        LinearLayout.LayoutParams ttlp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tvTitle.setLayoutParams(ttlp);
        header.addView(tvTitle);

        TextView tvCount = new TextView(this);
        tvCount.setText("0/" + course.files.length + " saved");
        tvCount.setTextColor(ContextCompat.getColor(this, R.color.cc_text_muted));
        tvCount.setTextSize(12);
        header.addView(tvCount);

        ImageView chevron = new ImageView(this);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(dp(20), dp(20));
        clp.setMarginStart(dp(4));
        chevron.setLayoutParams(clp);
        chevron.setImageResource(R.drawable.ic_ms_chevron_right);
        chevron.setColorFilter(ContextCompat.getColor(this, R.color.cc_text_muted));
        header.addView(chevron);

        section.addView(header);

        // PDF rows
        for (PdfFile pdf : course.files) {
            section.addView(buildPdfRow(pdf));
        }

        return section;
    }

    private View buildPdfRow(PdfFile pdf) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(R.drawable.cc_card_bordered);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(64));
        rlp.bottomMargin = dp(6);
        row.setLayoutParams(rlp);

        // PDF icon chip
        ImageView icon = new ImageView(this);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(dp(40), dp(40));
        icon.setLayoutParams(ilp);
        icon.setBackgroundResource(R.drawable.cc_iconchip_red);
        icon.setImageResource(R.drawable.ic_ms_picture_as_pdf);
        icon.setColorFilter(ContextCompat.getColor(this, R.color.cc_crimson));
        icon.setPadding(dp(9), dp(9), dp(9), dp(9));
        row.addView(icon);

        // Text column
        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tlp.setMarginStart(dp(12));
        texts.setLayoutParams(tlp);

        TextView tvName = new TextView(this);
        tvName.setText(pdf.name);
        tvName.setTextColor(ContextCompat.getColor(this, R.color.cc_text_primary));
        tvName.setTextSize(14);
        tvName.setSingleLine(true);
        tvName.setEllipsize(android.text.TextUtils.TruncateAt.END);
        texts.addView(tvName);

        TextView tvMeta = new TextView(this);
        tvMeta.setText("PDF · " + pdf.size);
        tvMeta.setTextColor(ContextCompat.getColor(this, R.color.cc_text_muted));
        tvMeta.setTextSize(12);
        texts.addView(tvMeta);

        row.addView(texts);

        // Download button
        ImageView btnDl = new ImageView(this);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(dp(48), dp(48));
        btnDl.setLayoutParams(blp);
        btnDl.setBackgroundResource(R.drawable.cc_circle_download);
        btnDl.setImageResource(R.drawable.ic_ms_download);
        btnDl.setColorFilter(ContextCompat.getColor(this, R.color.cc_navy));
        btnDl.setPadding(dp(12), dp(12), dp(12), dp(12));
        btnDl.setContentDescription("Download " + pdf.name);
        btnDl.setOnClickListener(v ->
                Toast.makeText(this, "Downloading " + pdf.name + "...", Toast.LENGTH_SHORT).show());
        row.addView(btnDl);

        return row;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
