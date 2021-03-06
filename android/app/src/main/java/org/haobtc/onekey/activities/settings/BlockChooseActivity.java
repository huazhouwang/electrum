package org.haobtc.onekey.activities.settings;

import android.content.SharedPreferences;
import android.view.View;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import org.greenrobot.eventbus.EventBus;
import org.haobtc.onekey.R;
import org.haobtc.onekey.activities.base.BaseActivity;
import org.haobtc.onekey.adapter.QuetationChooseAdapter;
import org.haobtc.onekey.aop.SingleClick;
import org.haobtc.onekey.bean.CNYBean;
import org.haobtc.onekey.event.FirstEvent;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class BlockChooseActivity extends BaseActivity {

    @BindView(R.id.img_back)
    ImageView imgBack;
    @BindView(R.id.recl_Quetation)
    RecyclerView reclQuetation;
    private SharedPreferences preferences;
    private int setBlock;


    @Override
    public int getLayoutId() {
        return R.layout.activity_block_choose;
    }

    @Override
    public void initView() {
        ButterKnife.bind(this);
        preferences = getSharedPreferences("Preferences", MODE_PRIVATE);
        setBlock = preferences.getInt("setBlock", 0);
    }

    @Override
    public void initData() {
        String[] stringArray = getResources().getStringArray(R.array.blockline);
        ArrayList<CNYBean> blockList = new ArrayList<>();
        for (String s : stringArray) {
            CNYBean cnyBean = new CNYBean(s, false);
            blockList.add(cnyBean);
        }
        QuetationChooseAdapter quetationChooseAdapter = new QuetationChooseAdapter(BlockChooseActivity.this, blockList, setBlock);
        quetationChooseAdapter.addHeaderView(generateHeadView());
        reclQuetation.setAdapter(quetationChooseAdapter);
        quetationChooseAdapter.setOnLisennorClick(pos -> {
            preferences.edit().putInt("setBlock", pos).apply();
            preferences.edit().putString("blockServerLine", blockList.get(pos).getName()).apply();
            EventBus.getDefault().post(new FirstEvent("block_check"));
        });
    }

    private View generateHeadView() {
        return View.inflate(this,R.layout.view_block_browser_hint,null);
    }

    @SingleClick
    @OnClick({R.id.img_back})
    public void onViewClicked(View view) {
        if (view.getId() == R.id.img_back) {
            finish();
        }
    }
}





