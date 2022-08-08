package com.addx.ai.demo;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;

//import com.ai.guard.vicohome.modules.library.LibraryFragment;
//import com.ai.guard.vicohome.modules.mine.account.TestMineFragment;

public class MineActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        addFragment(R.id.container, new TestMineFragment(), "");
    }

    @Override
    protected int getResid() {
        return R.layout.activity_mine;
    }
}


