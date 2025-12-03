package com.astrantiabooks.controller.activity;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.astrantiabooks.R;
import com.astrantiabooks.view.AccountFragment;
import com.astrantiabooks.view.ExploreFragment;
import com.astrantiabooks.view.HomeFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView navView = findViewById(R.id.nav_view);
        navView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();
            if (id == R.id.nav_home) selectedFragment = new HomeFragment();
            else if (id == R.id.nav_explore) selectedFragment = new ExploreFragment();
            else if (id == R.id.nav_account) selectedFragment = new AccountFragment();

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction().replace(R.id.container, selectedFragment).commit();
            }
            return true;
        });

        // Default fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.container, new HomeFragment()).commit();
        }
    }
}