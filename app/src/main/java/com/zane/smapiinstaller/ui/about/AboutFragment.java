package com.zane.smapiinstaller.ui.about;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.microsoft.appcenter.crashes.Crashes;
import com.zane.smapiinstaller.R;
import com.zane.smapiinstaller.constant.Constants;
import com.zane.smapiinstaller.logic.CommonLogic;
import com.zane.smapiinstaller.utils.DialogUtils;

import androidx.fragment.app.Fragment;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * @author Zane
 */
public class AboutFragment extends Fragment {

    ImageView imgHeart;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_about, container, false);
        ButterKnife.bind(this, root);
        return root;
    }

    @OnClick(R.id.button_release)
    void release() {
        CommonLogic.doOnNonNull(this.getContext(), (context) -> CommonLogic.openUrl(context, Constants.RELEASE_URL));
    }

    @OnClick(R.id.button_gplay)
    void gplay() {
        CommonLogic.openInPlayStore(this.getActivity());
    }

    @OnClick(R.id.button_privacy_policy)
    void privacyPolicy() {
        CommonLogic.showPrivacyPolicy(imgHeart, (dialog, dialogAction) -> {
        });
    }
}
