package com.addx.ai.demo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;

import com.ai.guard.vicohome.BaseFragment;
import com.ai.guard.vicohome.modules.home.CameraFragment;
import com.ai.guard.vicohome.modules.library.LibraryFragment;
import com.ai.guard.vicohome.modules.mine.account.MineFragment;
import com.ai.guard.vicohome.modules.mine.account.TestMineFragment;

import java.util.List;

public class DeviceListActivity extends BaseActivity {
    protected BaseFragment deviceFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("dd","DeviceListActivity==onCreate===code:"+this.hashCode()+"==="+(savedInstanceState == null));
        if (savedInstanceState != null) {
            FragmentManager sfm = getSupportFragmentManager();
            List<Fragment> fragments = sfm.getFragments();
            for (Fragment fragment : fragments) {
                if (fragment instanceof CameraFragment) {
                    Log.d("dd","DeviceListActivity==onCreate=savedInstanceState != null");
                    deviceFragment = (BaseFragment) fragment;
                }
            }
        } else {
            Log.d("dd","DeviceListActivity==onCreate=savedInstanceState == null");
            deviceFragment = new CameraFragment();
            addFragment(R.id.fragment_container, deviceFragment, null);
        }
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

    }
    @Override
    protected int getResid() {
        return R.layout.activity_device_list;
    }
}
