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
        CommonLogic.doOnNonNull(this.getContext(), (context) -> CommonLogic.openUrl(context, "https://ellipszist.github.io"));
    }

    @OnClick(R.id.button_gplay)
    void gplay() {
        CommonLogic.openInPlayStore(this.getActivity());
    }

    @OnClick(R.id.button_qq_group_1)
    void joinQQ() {
        String baseUrl = "mqqopensdkapi://bizAgent/qm/qr?url=http%3A%2F%2Fqm.qq.com%2Fcgi-bin%2Fqm%2Fqr%3Ffrom%3Dapp%26p%3Dandroid%26k%3D";
        DialogUtils.showListItemsDialog(imgHeart, R.string.button_qq_group_text, R.array.qq_group_list, (dialog, position) -> {
            switch (position){
                case 0:
                    CommonLogic.doOnNonNull(this.getContext(), (context) -> CommonLogic.openUrl(context, baseUrl + "AAflCLHiWw1haM1obu_f-CpGsETxXc6b"));
                    break;
                case 1:
                    CommonLogic.doOnNonNull(this.getContext(), (context) -> CommonLogic.openUrl(context, baseUrl + "kshK7BavcS2jXZ6exDvezc18ksLB8YsM"));
                    break;
                default:
                    CommonLogic.doOnNonNull(this.getContext(), (context) -> CommonLogic.openUrl(context, baseUrl + "zqsWYGBuAxPx0n9RI_ONs-7NA1Mm48QY"));
                    break;
            }
        });
    }

    @OnClick(R.id.button_privacy_policy)
    void privacyPolicy() {
        CommonLogic.showPrivacyPolicy(imgHeart, (dialog, dialogAction) -> {
        });
    }

}
