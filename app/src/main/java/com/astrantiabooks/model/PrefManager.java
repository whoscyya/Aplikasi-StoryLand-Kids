package com.astrantiabooks.models;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;

public class PrefManager {
    private static final String PREF_NAME = "AstrantiaSession";
    private static final String KEY_USER = "user_data";
    private static final String KEY_IS_LOGIN = "is_login";

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private Context context;

    public PrefManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    // 1. Simpan User saat Login
    public void saveUser(User user) {
        Gson gson = new Gson();
        String json = gson.toJson(user); // Ubah object User jadi String
        editor.putString(KEY_USER, json);
        editor.putBoolean(KEY_IS_LOGIN, true);
        editor.apply();

        // Update juga memori RAM (Static Variable)
        LocalData.currentUser = user;
    }

    // 2. Ambil User saat Aplikasi Dibuka
    public User getUser() {
        String json = pref.getString(KEY_USER, null);
        if (json != null) {
            return new Gson().fromJson(json, User.class);
        }
        return null;
    }

    // 3. Cek apakah sudah login
    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGIN, false);
    }

    // 4. Logout (HAPUS DATA SESI)
    public void logout() {
        editor.clear();
        editor.apply();
        LocalData.currentUser = null; // PENTING: Bersihkan variabel static
    }
}