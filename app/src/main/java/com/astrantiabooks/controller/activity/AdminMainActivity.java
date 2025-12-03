package com.astrantiabooks.controller.activity;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.astrantiabooks.R;
import com.astrantiabooks.view.AdminAccountFragment;
import com.astrantiabooks.view.AdminAddFragment;
import com.astrantiabooks.view.AdminHomeFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class AdminMainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_main);

        BottomNavigationView navView = findViewById(R.id.nav_view_admin);
        navView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();
            if (id == R.id.nav_admin_home) selectedFragment = new AdminHomeFragment();
            else if (id == R.id.nav_admin_add) selectedFragment = new AdminAddFragment();
            else if (id == R.id.nav_admin_account) selectedFragment = new AdminAccountFragment();

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction().replace(R.id.container_admin, selectedFragment).commit();
            }
            return true;
        });

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.container_admin, new AdminHomeFragment()).commit();
        }
    }
}