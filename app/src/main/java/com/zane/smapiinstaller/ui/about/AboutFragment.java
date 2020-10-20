package com.zane.smapiinstaller.ui.about;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.microsoft.appcenter.crashes.Crashes;
import com.zane.smapiinstaller.R;
import com.zane.smapiinstaller.constant.Constants;
import com.zane.smapiinstaller.databinding.FragmentAboutBinding;
import com.zane.smapiinstaller.logic.CommonLogic;
import com.zane.smapiinstaller.utils.DialogUtils;

import androidx.fragment.app.Fragment;

/**
 * @author Zane
 */
public class AboutFragment extends Fragment {

    private FragmentAboutBinding binding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentAboutBinding.inflate(inflater, container, false);
        binding.buttonRelease.setOnClickListener(v -> release());
        binding.buttonPrivacyPolicy.setOnClickListener(v -> privacyPolicy());
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void release() {
        CommonLogic.doOnNonNull(this.getContext(), (context) -> CommonLogic.openUrl(context, Constants.RELEASE_URL));
    }

    private void gplay() {
        CommonLogic.openInPlayStore(this.getActivity());
    }

}
