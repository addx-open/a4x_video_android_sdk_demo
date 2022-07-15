package com.addx.ai.demo;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;
import android.widget.FrameLayout;

//import com.ai.guard.vicohome.modules.library.LibraryFragment;

public class LibActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        addFragment(R.id.container, new LibraryFragment(), "");
    }

    @Override
    protected int getResid() {
        return R.layout.activity_lib;
    }
}
