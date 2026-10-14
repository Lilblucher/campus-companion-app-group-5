package com.mulungushi.campuscompanionapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.mulungushi.campuscompanionapp.network.ApiClient;
import com.mulungushi.campuscompanionapp.network.ApiService;
import com.mulungushi.campuscompanionapp.network.SessionManager;
import com.mulungushi.campuscompanionapp.network.StudentProfile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileActivity extends BaseActivity {

    private static final String PREFS = "profile_prefs";
    private static final String KEY_PHOTO = "profile_photo_path";

    private TextView segCS, segIT, segDS;
    private int selectedProgramme = 0;
    private TextView tvInitials;
    private ImageView ivPhoto;
    private TextInputEditText etName, etNumber;

    private final ActivityResultLauncher<String> pickPhoto =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) applyPhoto(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        findViewById(R.id.btnClose).setOnClickListener(v -> finish());
        findViewById(R.id.btnBell).setOnClickListener(v ->
                startActivity(new Intent(this, NotificationsActivity.class)));

        etName = findViewById(R.id.etName);
        etNumber = findViewById(R.id.etNumber);

        tvInitials = findViewById(R.id.tvAvatarInitials);
        ivPhoto = findViewById(R.id.ivAvatarPhoto);

        segCS = findViewById(R.id.segCS);
        segIT = findViewById(R.id.segIT);
        segDS = findViewById(R.id.segDS);
        segCS.setOnClickListener(v -> selectProgramme(0));
        segIT.setOnClickListener(v -> selectProgramme(1));
        segDS.setOnClickListener(v -> selectProgramme(2));
        selectProgramme(0);

        findViewById(R.id.btnCamera).setOnClickListener(v -> pickPhoto.launch("image/*"));
        findViewById(R.id.btnChoosePhoto).setOnClickListener(v -> pickPhoto.launch("image/*"));
        findViewById(R.id.btnRemovePhoto).setOnClickListener(v -> removePhoto());

        loadSavedPhoto();
        loadProfile();

        findViewById(R.id.btnSave).setOnClickListener(v ->
                Snackbar.make(v, "Profile saved", Snackbar.LENGTH_SHORT).show());
    }

    private void loadProfile() {
        String savedName = SessionManager.getInstance(this).getUserName();
        if (savedName != null && !savedName.isEmpty()) {
            etName.setText(savedName);
            setInitials(savedName);
        }

        ApiService api = ApiClient.getClient(this).create(ApiService.class);
        api.getMe().enqueue(new Callback<StudentProfile>() {
            @Override
            public void onResponse(Call<StudentProfile> call, Response<StudentProfile> response) {
                if (!response.isSuccessful() || response.body() == null) return;
                fillProfile(response.body());
            }

            @Override
            public void onFailure(Call<StudentProfile> call, Throwable t) {}
        });
    }

    private void fillProfile(StudentProfile p) {
        String name = p.getName() != null ? p.getName() : "";
        String number = p.getStudentNumber() != null ? p.getStudentNumber() : "";
        String code = p.getProgrammeCode();

        etName.setText(name);
        etNumber.setText(number);
        setInitials(name);

        if (code != null) {
            switch (code) {
                case "CS": selectProgramme(0); break;
                case "IT": selectProgramme(1); break;
                case "DS": selectProgramme(2); break;
            }
        }
    }

    private void setInitials(String name) {
        if (name == null || name.trim().isEmpty()) {
            tvInitials.setText("?");
            return;
        }
        String[] parts = name.trim().split("\\s+");
        String s = parts[0].substring(0, 1);
        if (parts.length > 1) s += parts[parts.length - 1].substring(0, 1);
        tvInitials.setText(s.toUpperCase());
    }

    private void applyPhoto(Uri uri) {
        try {
            InputStream in = getContentResolver().openInputStream(uri);
            if (in == null) return;
            Bitmap bmp = BitmapFactory.decodeStream(in);
            in.close();
            if (bmp == null) return;

            File photoFile = new File(getFilesDir(), "profile_photo.jpg");
            FileOutputStream out = new FileOutputStream(photoFile);
            bmp.compress(Bitmap.CompressFormat.JPEG, 85, out);
            out.close();

            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putString(KEY_PHOTO, photoFile.getAbsolutePath()).apply();

            showPhoto(bmp);
        } catch (Exception ignored) {}
    }

    private void loadSavedPhoto() {
        String path = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_PHOTO, null);
        if (path == null) return;
        File f = new File(path);
        if (!f.exists()) return;
        Bitmap bmp = BitmapFactory.decodeFile(path);
        if (bmp != null) showPhoto(bmp);
    }

    private void showPhoto(Bitmap bmp) {
        ivPhoto.setImageBitmap(bmp);
        ivPhoto.setVisibility(View.VISIBLE);
        tvInitials.setVisibility(View.GONE);
    }

    private void removePhoto() {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().remove(KEY_PHOTO).apply();
        File f = new File(getFilesDir(), "profile_photo.jpg");
        if (f.exists()) f.delete();
        ivPhoto.setVisibility(View.GONE);
        tvInitials.setVisibility(View.VISIBLE);
    }

    private void selectProgramme(int index) {
        selectedProgramme = index;
        TextView[] segs = {segCS, segIT, segDS};
        for (int i = 0; i < segs.length; i++) {
            if (i == index) {
                segs[i].setBackgroundResource(R.drawable.cc_segment_selected);
                segs[i].setTextColor(0xFFFFFFFF);
            } else {
                segs[i].setBackgroundResource(R.drawable.cc_segment_unselected);
                segs[i].setTextColor(getColor(R.color.cc_text_secondary));
            }
        }
    }
}
