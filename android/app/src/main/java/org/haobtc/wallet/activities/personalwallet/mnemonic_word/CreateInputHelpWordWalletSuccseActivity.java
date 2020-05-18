package org.haobtc.wallet.activities.personalwallet.mnemonic_word;

import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.chaquo.python.PyObject;
import com.google.gson.Gson;
import com.yzq.zxinglibrary.encode.CodeCreator;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.haobtc.wallet.MainActivity;
import org.haobtc.wallet.R;
import org.haobtc.wallet.activities.base.BaseActivity;
import org.haobtc.wallet.aop.SingleClick;
import org.haobtc.wallet.bean.GetCodeAddressBean;
import org.haobtc.wallet.event.FirstEvent;
import org.haobtc.wallet.utils.Daemon;
import org.haobtc.wallet.utils.MyDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class CreateInputHelpWordWalletSuccseActivity extends BaseActivity {

    @BindView(R.id.img_backCreat)
    ImageView imgBackCreat;
    @BindView(R.id.tet_who_wallet)
    TextView tetWhoWallet;
    @BindView(R.id.img_Orcode)
    ImageView imgOrcode;
    @BindView(R.id.tet_Preservation)
    TextView tetPreservation;
    @BindView(R.id.btn_Finish)
    Button btnFinish;
    private Bitmap bitmap;
    private MyDialog myDialog;

    @Override
    public int getLayoutId() {
        return R.layout.activity_create_input_help_word_wallet_succse;
    }

    @Override
    public void initView() {
        ButterKnife.bind(this);
        EventBus.getDefault().register(this);
        myDialog = MyDialog.showDialog(this);

    }

    @Override
    public void initData() {
        myDialog.show();
    }

    @SingleClick
    @OnClick({R.id.img_backCreat, R.id.tet_Preservation, R.id.btn_Finish})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.img_backCreat:
                finish();
                break;
            case R.id.tet_Preservation:
                boolean toGallery = saveBitmap(bitmap);
                if (toGallery) {
                    mToast(getString(R.string.preservationbitmappic));
                } else {
                    Toast.makeText(this, R.string.preservationfail, Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btn_Finish:
                mIntent(MainActivity.class);
                finishAffinity();
                break;
        }
    }

    private void mGeneratecode() {
        PyObject walletAddressShowUi = null;
        try {
            walletAddressShowUi = Daemon.commands.callAttr("get_wallet_address_show_UI");
        } catch (Exception e) {
            e.printStackTrace();
            myDialog.dismiss();
            return;
        }
        if (walletAddressShowUi != null) {
            String strCode = walletAddressShowUi.toString();
            Log.i("strCode", "mGenerate--: " + strCode);
            myDialog.dismiss();
            Gson gson = new Gson();
            GetCodeAddressBean getCodeAddressBean = gson.fromJson(strCode, GetCodeAddressBean.class);
            String qr_data = getCodeAddressBean.getQr_data();
            bitmap = CodeCreator.createQRCode(qr_data, 248, 248, null);
            imgOrcode.setImageBitmap(bitmap);
        }
    }

    public boolean saveBitmap(Bitmap bitmap) {
        try {
            File filePic = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + System.currentTimeMillis() + ".jpg");
            if (!filePic.exists()) {
                filePic.getParentFile().mkdirs();
                filePic.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(filePic);
            boolean success = bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
            return success;

        } catch (IOException ignored) {
            return false;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void event(FirstEvent updataHint) {
        String createSinglePass = updataHint.getMsg();
        if (createSinglePass.equals("createSinglePass")) {
            //get Or code
            mGeneratecode();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}