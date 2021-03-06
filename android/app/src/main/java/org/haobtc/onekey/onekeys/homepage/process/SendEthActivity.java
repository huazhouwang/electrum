package org.haobtc.onekey.onekeys.homepage.process;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.ViewModelProvider;

import com.google.common.base.Strings;
import com.lxj.xpopup.XPopup;
import com.tbruyelle.rxpermissions2.RxPermissions;
import com.yzq.zxinglibrary.android.CaptureActivity;
import com.yzq.zxinglibrary.bean.ZxingConfig;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.haobtc.onekey.R;
import org.haobtc.onekey.activities.base.MyApplication;
import org.haobtc.onekey.aop.SingleClick;
import org.haobtc.onekey.asynctask.BusinessAsyncTask;
import org.haobtc.onekey.bean.CurrentFeeDetails;
import org.haobtc.onekey.bean.LocalWalletInfo;
import org.haobtc.onekey.bean.MainSweepcodeBean;
import org.haobtc.onekey.bean.PyResponse;
import org.haobtc.onekey.bean.TemporaryTxInfo;
import org.haobtc.onekey.bean.TransactionInfoBean;
import org.haobtc.onekey.business.qrdecode.QRDecode;
import org.haobtc.onekey.business.wallet.AccountManager;
import org.haobtc.onekey.business.wallet.SystemConfigManager;
import org.haobtc.onekey.constant.Constant;
import org.haobtc.onekey.constant.PyConstant;
import org.haobtc.onekey.constant.Vm;
import org.haobtc.onekey.event.ButtonRequestConfirmedEvent;
import org.haobtc.onekey.event.ButtonRequestEvent;
import org.haobtc.onekey.event.ChangePinEvent;
import org.haobtc.onekey.event.CustomizeFeeRateEvent;
import org.haobtc.onekey.event.ExitEvent;
import org.haobtc.onekey.event.GetFeeEvent;
import org.haobtc.onekey.event.GotPassEvent;
import org.haobtc.onekey.event.SecondEvent;
import org.haobtc.onekey.manager.PyEnv;
import org.haobtc.onekey.ui.activity.SoftPassActivity;
import org.haobtc.onekey.ui.activity.VerifyPinActivity;
import org.haobtc.onekey.ui.base.BaseActivity;
import org.haobtc.onekey.ui.dialog.TransactionConfirmDialog;
import org.haobtc.onekey.ui.dialog.UnBackupTipDialog;
import org.haobtc.onekey.ui.dialog.custom.CustomCenterDialog;
import org.haobtc.onekey.ui.dialog.custom.CustomEthFeeDialog;
import org.haobtc.onekey.ui.dialog.custom.CustomWatchWalletDialog;
import org.haobtc.onekey.ui.widget.PasteEditText;
import org.haobtc.onekey.ui.widget.PointLengthFilter;
import org.haobtc.onekey.utils.ClipboardUtils;
import org.haobtc.onekey.viewmodel.AppWalletViewModel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Objects;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnFocusChange;
import butterknife.OnTextChanged;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.schedulers.Schedulers;

import static org.haobtc.onekey.constant.Constant.WALLET_BALANCE;

/**
 * @author liyan
 */
public class SendEthActivity extends BaseActivity implements BusinessAsyncTask.Helper, PasteEditText.OnPasteCallback, CustomEthFeeDialog.onCustomInterface {

    private static final String EXT_BALANCE = WALLET_BALANCE;
    private static final String EXT_WALLET_NAME = "hdWalletName";
    private static final String EXT_SCAN_ADDRESS = "addressScan";
    private static final String EXT_SCAN_AMOUNT = "amountScan";

    public static void start(Context context, String balance, String name, @Nullable String address, @Nullable String amount) {
        Intent intent = new Intent(context, SendEthActivity.class);
        intent.putExtra(EXT_BALANCE, balance);
        intent.putExtra(EXT_WALLET_NAME, name);
        if (!TextUtils.isEmpty(address)) {
            intent.putExtra(EXT_SCAN_ADDRESS, address);
            intent.putExtra(EXT_SCAN_AMOUNT, amount);
        }
        context.startActivity(intent);
    }

    static final int RECOMMENDED_FEE_RATE = 0;
    static final int SLOW_FEE_RATE = 1;
    static final int FAST_FEE_RATE = 2;
    static final int CUSTOMIZE_FEE_RATE = 3;
    private static final int DEFAULT_TX_SIZE = 220;
    @BindView(R.id.edit_amount)
    EditText editAmount;
    @BindView(R.id.btn_next)
    Button btnNext;
    @BindView(R.id.img_scan)
    ImageView imgScan;
    @BindView(R.id.checkbox_slow)
    CheckBox checkboxSlow;
    @BindView(R.id.view_slow)
    View viewSlow;
    @BindView(R.id.checkbox_recommend)
    CheckBox checkboxRecommend;
    @BindView(R.id.view_recommend)
    View viewRecommend;
    @BindView(R.id.checkbox_fast)
    CheckBox checkboxFast;
    @BindView(R.id.view_fast)
    View viewFast;
    @BindView(R.id.img_back)
    ImageView imgBack;
    @BindView(R.id.edit_receiver_address)
    PasteEditText editReceiverAddress;
    @BindView(R.id.paste_address)
    TextView pasteAddress;
    @BindView(R.id.switch_coin_type)
    TextView switchCoinType;
    @BindView(R.id.text_max_amount)
    TextView textMaxAmount;
    @BindView(R.id.text_balance)
    TextView textBalance;
    @BindView(R.id.text_customize_fee_rate)
    TextView textCustomizeFeeRate;
    @BindView(R.id.text_fee_in_btc_0)
    TextView textFeeInBtc0;
    @BindView(R.id.text_fee_in_cash_0)
    TextView textFeeInCash0;
    @BindView(R.id.text_spend_time_0)
    TextView textSpendTime0;
    @BindView(R.id.linear_slow)
    RelativeLayout linearSlow;
    @BindView(R.id.text_fee_in_btc_1)
    TextView textFeeInBtc1;
    @BindView(R.id.text_fee_in_cash_1)
    TextView textFeeInCash1;
    @BindView(R.id.text_spend_time_1)
    TextView textSpendTime1;
    @BindView(R.id.linear_recommend)
    RelativeLayout linearRecommend;
    @BindView(R.id.text_fee_in_btc_2)
    TextView textFeeInBtc2;
    @BindView(R.id.text_fee_in_cash_2)
    TextView textFeeInCash2;
    @BindView(R.id.text_spend_time_2)
    TextView textSpendTime2;
    @BindView(R.id.linear_fast)
    RelativeLayout linearFast;
    @BindView(R.id.linear_rate_selector)
    LinearLayout linearRateSelector;
    @BindView(R.id.checkbox_custom)
    CheckBox checkboxCustom;
    @BindView(R.id.text_fee_customize_in_btc)
    TextView textFeeCustomizeInBtc;
    @BindView(R.id.text_fee_customize_in_cash)
    TextView textFeeCustomizeInCash;
    @BindView(R.id.text_customize_spend_time)
    TextView textCustomizeSpendTime;
    @BindView(R.id.text_rollback)
    TextView textRollback;
    @BindView(R.id.linear_customize)
    LinearLayout linearCustomize;
    private int screenHeight;
    private boolean mIsSoftKeyboardShowing;
    private RxPermissions rxPermissions;
    private SharedPreferences preferences;
    private String hdWalletName;
    private String baseUnit;
    private String currencySymbols;
    private String showWalletType;
    private String signedTx;
    private String rawTx;
    private TransactionConfirmDialog confirmDialog;
    private CurrentFeeDetails mCurrentFeeDetails;
    private String tempFastTransaction;
    private String tempSlowTransaction;
    private String tempRecommendTransaction;
    private double currentFeeRate;
    private BigDecimal minAmount;
    private BigDecimal decimalBalance;
    private int scale;
    private boolean addressInvalid;
    private String amount;
    private boolean isFeeValid;
    private boolean isCustom = false;
    private boolean isSetBig;
    private String balance;
    private BigDecimal maxAmount;
    private int selectFlag = 0;
    private boolean isResume;
    private double slowRate;
    private double normalRate;
    private double fastRate;
    private AppWalletViewModel mAppWalletViewModel;
    private static final int REQUEST_SCAN_CODE = 0;
    private boolean signClickable = true;
    io.reactivex.disposables.Disposable subscribe;
    private final CompositeDisposable mCompositeDisposable = new CompositeDisposable();
    private SystemConfigManager mSystemConfigManager;
    private AccountManager mAccountManager;
    private boolean isClickPaste;
    // 钱包类型：btc || eth
    private static final String walletType = Constant.ETH;
    private int mGasLimit = 0;
    private CustomEthFeeDialog mCustomEthFeeDialog;
    private String currentWalletName;
    private LocalWalletInfo localWalletByName;
    private String mCusFee;
    private String mCusFeeRate;

    /**
     * init
     */
    @Override
    public void init() {
        initData();
        initView();
    }

    private void initView() {
        initShowDefaultView();
        getDefaultFee(true);
        editAmount.setFilters(new InputFilter[]{new PointLengthFilter(scale, maxNum -> showToast(R.string.accuracy_num))});
        String addressScan = getIntent().getStringExtra(EXT_SCAN_ADDRESS);
        if (!TextUtils.isEmpty(addressScan)) {
            editReceiverAddress.setText(addressScan);
            String amountScan = getIntent().getStringExtra(EXT_SCAN_AMOUNT);
            String amountConvert = new QRDecode().getAmountByPythonResultAmount(amountScan);
            String amountStr = checkAndConvertAmount(amountConvert);
            if (amountStr == null) {
                getAddressIsValid();
            } else {
                editAmount.setText(amountStr);
                keyBoardHideRefresh();
            }
        } else {
            //whether backup
            boolean walletBackup = mAccountManager.getWalletBackup();
            if (!walletBackup) {
                new XPopup.Builder(mContext)
                        .dismissOnTouchOutside(false)
                        .isDestroyOnDismiss(true)
                        .asCustom(new UnBackupTipDialog(mContext, getString(R.string.receive_unbackup_tip), new UnBackupTipDialog.onClick() {
                            @Override
                            public void onBack() {
                                finish();
                            }
                        })).show();
            }
        }
        if (Constant.BTC_WATCH.equals(showWalletType)) {
            showWatchTipDialog();
        }
        mAppWalletViewModel.currentWalletBalance.observe(this, mBalance -> {
            balance = mBalance.getBalance();
            if (!Strings.isNullOrEmpty(balance)) {
                decimalBalance = BigDecimal.valueOf(Double.parseDouble(balance));
            }
            textBalance.setText(String.format("%s%s", balance, baseUnit));
        });
        switchCoinType.setText(Constant.COIN_TYPE_ETH);
        editReceiverAddress.setOnPasteCallback(this);
        registerLayoutChangeListener();
    }

    private void initShowDefaultView() {
        textFeeInBtc1.setText(R.string._0_00_eth);
        textFeeInBtc0.setText(R.string._0_00_eth);
        textFeeInBtc2.setText(R.string._0_00_eth);
    }

    private void initData() {
        mAppWalletViewModel = new ViewModelProvider(MyApplication.getInstance()).get(AppWalletViewModel.class);
        mAccountManager = new AccountManager(mContext);
        mSystemConfigManager = new SystemConfigManager(this);
        rxPermissions = new RxPermissions(this);
        hdWalletName = getIntent().getStringExtra(EXT_WALLET_NAME);
        preferences = getSharedPreferences("Preferences", MODE_PRIVATE);
        showWalletType = mAccountManager.getCurrentWalletAccurateType();
        currencySymbols = mSystemConfigManager.getCurrentFiatSymbol();
        currentWalletName = mAccountManager.getCurrentWalletName();
        localWalletByName = mAccountManager.getLocalWalletByName(currentWalletName);
        baseUnit = mSystemConfigManager.getCurrentBaseUnit(localWalletByName.getCoinType());
        setMinAmount();
    }

    private void showWatchTipDialog() {
        CustomCenterDialog centerDialog = new CustomCenterDialog(mContext, new CustomCenterDialog.onConfirmClick() {
            @Override
            public void onConfirm() {
                finish();
            }
        });
        centerDialog.setContent(getString(R.string.watch_wallet_tip));
        new XPopup.Builder(mContext).asCustom(centerDialog).show();
    }

    /***
     * init layout
     * @return
     */
    @Override
    public int getContentViewId() {
        return R.layout.activity_send_hd;
    }

    private void setMinAmount() {
        minAmount = BigDecimal.valueOf(0.000000000000000001);
        scale = 18;
    }

    @OnClick({R.id.img_back, R.id.switch_coin_type, R.id.text_max_amount, R.id.text_customize_fee_rate, R.id.linear_slow, R.id.linear_recommend, R.id.linear_fast, R.id.text_rollback, R.id.btn_next, R.id.paste_address, R.id.img_scan})
    @SingleClick
    public void onViewClicked(View view) {
        switch (view.getId()) {
            case R.id.img_back:
                finish();
                break;
            case R.id.switch_coin_type:
                // not support
                break;
            case R.id.text_max_amount:
                if (Strings.isNullOrEmpty(editReceiverAddress.getText().toString())) {
                    showToast(R.string.input_number);
                } else {
                    // 先取最大值再刷新View
                    isSetBig = true;
                    if (isCustom) {
                        Disposable disposable = getEthFeeRateObservable(String.valueOf(currentFeeRate))
                                .subscribeOn(Schedulers.io())
                                .doOnSubscribe(show -> showProgress())
                                .observeOn(AndroidSchedulers.mainThread())
                                .doOnNext(this::doDealMaxAmount)
                                .observeOn(Schedulers.io())
                                .flatMap((Function<PyResponse<TemporaryTxInfo>, ObservableSource<PyResponse<TemporaryTxInfo>>>)
                                        temporaryTxInfoPyResponse ->
                                                getEthFeeRateObservable(String.valueOf(currentFeeRate))).observeOn(AndroidSchedulers.mainThread())
                                .doFinally(this::dismissProgress)
                                .subscribe(this::dealWithCommonFeeInfo);
                        mCompositeDisposable.add(disposable);
                    } else {
                        Disposable subscribe = getEthFeeRateObservable(String.valueOf(currentFeeRate))
                                .subscribeOn(Schedulers.io())
                                .doOnSubscribe(show -> showProgress())
                                .observeOn(AndroidSchedulers.mainThread())
                                .doOnNext(this::doDealMaxAmount)
                                .observeOn(Schedulers.io())
                                .flatMap((Function<PyResponse<TemporaryTxInfo>, ObservableSource<PyResponse<String>>>) temporaryTxInfoPyResponse ->
                                        getDefaultObservable(false))
                                .observeOn(AndroidSchedulers.mainThread())
                                .doFinally(this::dismissProgress)
                                .subscribe(stringPyResponse ->
                                        doDefaultInfo(stringPyResponse, false));
                        mCompositeDisposable.add(subscribe);
                    }

                }
                break;
            case R.id.text_customize_fee_rate:
                if (mCurrentFeeDetails != null && mCurrentFeeDetails.getSlow() != null) {
                    double feeRate = 0;
                    if (selectFlag == RECOMMENDED_FEE_RATE) {
                        feeRate = mCurrentFeeDetails.getNormal().getGasPrice();
                    } else if (selectFlag == SLOW_FEE_RATE) {
                        feeRate = mCurrentFeeDetails.getSlow().getGasPrice();
                    } else if (selectFlag == FAST_FEE_RATE) {
                        feeRate = mCurrentFeeDetails.getFast().getGasPrice();
                    }
                    mCustomEthFeeDialog = new CustomEthFeeDialog(mContext, mGasLimit, mCurrentFeeDetails.getFast().getGasPrice() * 10, hdWalletName, feeRate);
                    mCustomEthFeeDialog.setOnCustomInterface(this);
                    mCustomEthFeeDialog.setSetBig(isSetBig);
                    mCustomEthFeeDialog.setAddress(editReceiverAddress.getText().toString().trim());
                    mCustomEthFeeDialog.setSendAmount(amount);
                    new XPopup.Builder(mContext).asCustom(mCustomEthFeeDialog).show();
                } else {
                    showToast(R.string.feerate_failure);
                }
                break;
            case R.id.linear_slow:
                viewSlow.setVisibility(View.VISIBLE);
                viewRecommend.setVisibility(View.GONE);
                viewFast.setVisibility(View.GONE);
                checkboxSlow.setVisibility(View.VISIBLE);
                checkboxRecommend.setVisibility(View.GONE);
                checkboxFast.setVisibility(View.GONE);
                if (mCurrentFeeDetails != null && mCurrentFeeDetails.getSlow() != null) {
                    currentFeeRate = mCurrentFeeDetails.getSlow().getGasPrice();
                    selectFlag = SLOW_FEE_RATE;
                    mGasLimit = mCurrentFeeDetails.getSlow().getGasLimit();
                } else {
                    showToast(R.string.feerate_failure);
                }
                break;
            case R.id.linear_recommend:
                viewSlow.setVisibility(View.GONE);
                viewRecommend.setVisibility(View.VISIBLE);
                viewFast.setVisibility(View.GONE);
                checkboxSlow.setVisibility(View.GONE);
                checkboxRecommend.setVisibility(View.VISIBLE);
                checkboxFast.setVisibility(View.GONE);
                if (mCurrentFeeDetails != null && mCurrentFeeDetails.getNormal() != null) {
                    currentFeeRate = mCurrentFeeDetails.getNormal().getGasPrice();
                    selectFlag = RECOMMENDED_FEE_RATE;
                    mGasLimit = mCurrentFeeDetails.getNormal().getGasLimit();
                } else {
                    showToast(R.string.feerate_failure);
                }
                break;
            case R.id.linear_fast:
                viewSlow.setVisibility(View.GONE);
                viewRecommend.setVisibility(View.GONE);
                viewFast.setVisibility(View.VISIBLE);
                checkboxSlow.setVisibility(View.GONE);
                checkboxRecommend.setVisibility(View.GONE);
                checkboxFast.setVisibility(View.VISIBLE);
                if (mCurrentFeeDetails != null && mCurrentFeeDetails.getFast() != null) {
                    currentFeeRate = mCurrentFeeDetails.getFast().getGasPrice();
                    selectFlag = FAST_FEE_RATE;
                    mGasLimit = mCurrentFeeDetails.getFast().getGasLimit();
                } else {
                    showToast(R.string.feerate_failure);
                }
                break;
            case R.id.text_rollback:
                turnOffCustomize();
                break;
            case R.id.paste_address:
                editReceiverAddress.setText(ClipboardUtils.pasteText(this));
                if (!mIsSoftKeyboardShowing) {
                    keyBoardHideRefresh();
                }
                break;
            case R.id.btn_next:
                if (signClickable) {
                    sendETH();
                }else {
                    showToast(R.string.confirm_hardware_msg);
                }
                break;
            case R.id.img_scan:
                subscribe = rxPermissions
                        .request(Manifest.permission.CAMERA)
                        .subscribe(granted -> {
                            if (granted) {
                                // If you have already authorized it, you can directly jump to the QR code scanning interface
                                Intent intent2 = new Intent(getActivity(), CaptureActivity.class);
                                ZxingConfig config = new ZxingConfig();
                                config.setPlayBeep(true);
                                config.setShake(true);
                                config.setDecodeBarCode(false);
                                config.setFullScreenScan(true);
                                config.setShowAlbum(false);
                                config.setShowbottomLayout(false);
                                intent2.putExtra(com.yzq.zxinglibrary.common.Constant.INTENT_ZXING_CONFIG, config);
                                startActivityForResult(intent2, REQUEST_SCAN_CODE);
                            } else {
                                Toast.makeText(getActivity(), R.string.photopersion, Toast.LENGTH_SHORT).show();
                            }
                        });
                break;
            default:
                break;
        }
    }

    /**
     * 取消自定义费率
     */
    private void turnOffCustomize() {
        isCustom = false;
        linearRateSelector.setVisibility(View.VISIBLE);
        linearCustomize.setVisibility(View.GONE);
        if (selectFlag == RECOMMENDED_FEE_RATE) {
            if (mCurrentFeeDetails != null && mCurrentFeeDetails.getNormal() != null) {
                currentFeeRate = mCurrentFeeDetails.getNormal().getFeerate();
                mGasLimit = mCurrentFeeDetails.getNormal().getGasLimit();
            }
        } else if (selectFlag == SLOW_FEE_RATE) {
            if (mCurrentFeeDetails != null && mCurrentFeeDetails.getSlow() != null) {
                currentFeeRate = mCurrentFeeDetails.getSlow().getFeerate();
                mGasLimit = mCurrentFeeDetails.getSlow().getGasLimit();
            }
        } else if (selectFlag == FAST_FEE_RATE) {
            if (mCurrentFeeDetails != null && mCurrentFeeDetails.getFast() != null) {
                currentFeeRate = mCurrentFeeDetails.getFast().getFeerate();
                mGasLimit = mCurrentFeeDetails.getFast().getGasLimit();
            }
        }
        keyBoardHideRefresh();
    }

    /**
     * 判断地址和金额是否正确填写完毕
     */
    private boolean sendReady() {
        if (Strings.isNullOrEmpty(editReceiverAddress.getText().toString())) {
            showToast(R.string.input_number);
            return false;
        }
        if (Strings.isNullOrEmpty(editAmount.getText().toString())) {
            showToast(R.string.input_out_number);
            return false;
        }
        return !(Double.parseDouble(amount) <= 0);
    }

    /**
     * 获取费率详情
     */
    private void getDefaultFee(boolean isDefault) {
        Disposable disposable = getDefaultObservable(isDefault)
                .doOnSubscribe(show -> showProgress())
                .doFinally(this::dismissProgress)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(data -> doDefaultInfo(data, isDefault));
        mCompositeDisposable.add(disposable);
    }

    private void doDefaultInfo(PyResponse<String> data, boolean isDefault) {
        String errors = data.getErrors();
        if (Strings.isNullOrEmpty(errors)) {
            mCurrentFeeDetails = CurrentFeeDetails.objectFromDate(data.getResult());
            initFeeSelectorStatus();
            mGasLimit = mCurrentFeeDetails.getNormal().getGasLimit();
            currentFeeRate = mCurrentFeeDetails.getNormal().getGasPrice();
            if (!isDefault) {
                isFeeValid = true;
                changeButton();
            }
        } else {
            mToast(errors);
        }
    }

    /**
     * 初始化三种等级手续费的默认视图
     */
    private void initFeeSelectorStatus() {
        textSpendTime0.setText(String.format("%s %s %s", getString(R.string.about_), mCurrentFeeDetails == null ? 0 : mCurrentFeeDetails.getSlow().getTime(), getString(R.string.minute)));
        textFeeInBtc0.setText(String.format(Locale.ENGLISH, "%s %s", mCurrentFeeDetails.getSlow().getFee(), baseUnit));
        slowRate = mCurrentFeeDetails.getSlow().getFeerate();
        textFeeInCash0.setText(String.format(Locale.ENGLISH, "%s %s", currencySymbols, mCurrentFeeDetails.getSlow().getFiat().substring(0, mCurrentFeeDetails.getSlow().getFiat().indexOf(" "))));
        textSpendTime1.setText(String.format("%s %s %s", getString(R.string.about_), mCurrentFeeDetails == null ? 0 : mCurrentFeeDetails.getNormal().getTime(), getString(R.string.minute)));
        textFeeInBtc1.setText(String.format(Locale.ENGLISH, "%s %s", mCurrentFeeDetails.getNormal().getFee(), baseUnit));
        normalRate = mCurrentFeeDetails.getNormal().getFeerate();
        textFeeInCash1.setText(String.format(Locale.ENGLISH, "%s %s", currencySymbols, mCurrentFeeDetails.getNormal().getFiat().substring(0, mCurrentFeeDetails.getNormal().getFiat().indexOf(" "))));
        textSpendTime2.setText(String.format("%s %s %s", getString(R.string.about_), mCurrentFeeDetails == null ? 0 : mCurrentFeeDetails.getFast().getTime(), getString(R.string.minute)));
        textFeeInBtc2.setText(String.format(Locale.ENGLISH, "%s %s", mCurrentFeeDetails.getFast().getFee(), baseUnit));
        fastRate = mCurrentFeeDetails.getFast().getFeerate();
        textFeeInCash2.setText(String.format(Locale.ENGLISH, "%s %s", currencySymbols, mCurrentFeeDetails.getFast().getFiat().substring(0, mCurrentFeeDetails.getFast().getFiat().indexOf(" "))));
    }

    /**
     * 发送交易
     */
    private void sendETH() {
        String sender = localWalletByName.getAddr();
        String receiver = editReceiverAddress.getText().toString().trim();
        String fee = "";

        if (isCustom) {
            fee = String.format(Locale.ENGLISH, "%s %s(%s)", mCusFee, baseUnit, mCusFeeRate);
        } else {
            if (selectFlag == RECOMMENDED_FEE_RATE) {
                fee = String.format(Locale.ENGLISH, "%s %s(%s)", mCurrentFeeDetails.getNormal().getFee(), baseUnit, mCurrentFeeDetails.getNormal().getFiat());
            } else if (selectFlag == SLOW_FEE_RATE) {
                fee = String.format(Locale.ENGLISH, "%s %s(%s)", mCurrentFeeDetails.getSlow().getFee(), baseUnit, mCurrentFeeDetails.getSlow().getFiat());
            } else if (selectFlag == FAST_FEE_RATE) {
                fee = String.format(Locale.ENGLISH, "%s %s(%s)", mCurrentFeeDetails.getFast().getFee(), baseUnit, mCurrentFeeDetails.getFast().getFiat());
            }
        }

        String mSendAmounts = String.format(Locale.ENGLISH, "%s %s", amount, baseUnit);
        Bundle bundle = new Bundle();
        bundle.putString(Constant.TRANSACTION_SENDER, sender);
        bundle.putString(Constant.TRANSACTION_RECEIVER, receiver);
        bundle.putString(Constant.TRANSACTION_AMOUNT, mSendAmounts);
        bundle.putString(Constant.TRANSACTION_FEE, fee);
        bundle.putString(Constant.WALLET_LABEL, hdWalletName);
        bundle.putInt(Constant.WALLET_TYPE, Constant.WALLET_TYPE_HARDWARE.equals(showWalletType) ?
                Constant.WALLET_TYPE_HARDWARE_PERSONAL : Constant.WALLET_TYPE_SOFTWARE);
        confirmDialog = new TransactionConfirmDialog();
        confirmDialog.setArguments(bundle);
        confirmDialog.show(getSupportFragmentManager(), "confirm");
    }

    @Subscribe
    public void onGotSoftPass(GotPassEvent event) {
        softSign(event.getPassword());
    }

    /**
     * 软件签名交易
     * path : NFC/android_usb/bluetooth as str, used by hardware
     */
    private void softSign(String password) {
        String path;
        if (Constant.WALLET_TYPE_HARDWARE.equals(showWalletType)) {
            path = Constant.WAY_MODE_BLE;
        } else {
            path = "";
        }
        Disposable disposable = Observable.create((ObservableOnSubscribe<PyResponse<String>>) emitter -> {
            PyResponse<String> response = PyEnv.signEthTX(editReceiverAddress.getText().toString().trim(), amount, path,
                    password, String.valueOf(currentFeeRate), String.valueOf(mGasLimit));
            emitter.onNext(response);
            emitter.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(show -> showProgress())
                .doFinally(this::dismissProgress)
                .subscribe(stringPyResponse -> {
                    if (Strings.isNullOrEmpty(stringPyResponse.getErrors())) {
                        String signText = stringPyResponse.getResult();
                        TransactionCompletion.start(mContext, signText, String.format(Locale.ENGLISH, "%s %s", amount, baseUnit));
                        finish();

                    } else {
                        mToast(stringPyResponse.getErrors());
                    }
                }, error -> {
                    dismissProgress();
                    mToast(error.toString());
                });
        mCompositeDisposable.add(disposable);
    }

    /**
     * ETH 签名加广播是一起的，不需要单独广播
     * 广播交易  BTC  后期也需要修改
     */
    private void broadcastTx(String signedTx) {
        PyResponse<Void> response = PyEnv.broadcast(signedTx);
        String errors = response.getErrors();
        if (Strings.isNullOrEmpty(errors)) {
            TransactionCompletion.start(mContext, signedTx, "mSendAmounts");
            finish();
        } else {
            showToast(errors);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onConfirm(ButtonRequestConfirmedEvent event) {
        if (Constant.WALLET_TYPE_HARDWARE.equals(showWalletType)) {
            TransactionInfoBean info = TransactionInfoBean.objectFromData(signedTx);
            broadcastTx(info.getTx());
        } else if (Constant.BTC_WATCH.equals(showWalletType)) {
            showWatchQrDialog();
        } else {
            // 获取主密码
            startActivity(new Intent(this, SoftPassActivity.class));
        }
    }

    private void showWatchQrDialog() {
        new XPopup.Builder(mContext).asCustom(new CustomWatchWalletDialog(mContext, rawTx)).show();
    }


    private String getTransferTime(double time) {
        if (time >= 1) {
            return String.format("%s%s%s", getString(R.string.about_), (int) time + "", getString(R.string.minute));
        } else {
            return String.format("%s%s%s", getString(R.string.about_), (int) (time * 60) + "", getString(R.string.second));
        }
    }

    /**
     * 改变发送按钮状态
     */
    private void changeButton() {
        if (addressInvalid && isFeeValid && !Strings.isNullOrEmpty(amount)) {
            BigDecimal decimal = BigDecimal.valueOf(Double.parseDouble(amount));
            if (decimal.compareTo(minAmount) < 0) {
                btnNext.setEnabled(false);
            } else {
                btnNext.setEnabled(true);
            }
        } else {
            btnNext.setEnabled(false);
        }
    }

    /**
     * 自定义费率确认响应
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCustomizeFee(CustomizeFeeRateEvent event) {
        isCustom = true;
        linearRateSelector.setVisibility(View.GONE);
        linearCustomize.setVisibility(View.VISIBLE);
        currentFeeRate = Double.parseDouble(event.getFeeRate());
        textFeeCustomizeInBtc.setText(String.format(Locale.ENGLISH, "%s %s", event.getFee(), mSystemConfigManager.getCurrentBaseUnit()));
        textFeeCustomizeInCash.setText(String.format(Locale.ENGLISH, "%s %s", mSystemConfigManager.getCurrentFiatSymbol(), event.getCash()));
        textCustomizeSpendTime.setText(String.format("%s%s%s", getString(R.string.about_), event.getTime(), getString(R.string.minute)));
        keyBoardHideRefresh();
    }

    /**
     * 自定义费率监听
     */
    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    public void onGetFee(GetFeeEvent event) {
        String feeRate = event.getFeeRate();
        getEthFeeRate(feeRate);
    }

    /**
     * 注册全局视图监听器
     */
    private void registerLayoutChangeListener() {
        DisplayMetrics metric = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metric);
        screenHeight = metric.heightPixels;
        mIsSoftKeyboardShowing = false;
        ViewTreeObserver.OnGlobalLayoutListener mLayoutChangeListener = () -> {
            //Determine the size of window visible area
            Rect r = new Rect();
            getWindow().getDecorView().getWindowVisibleDisplayFrame(r);
            //If the difference between screen height and window visible area height is greater than 1 / 3 of the whole screen height, it means that the soft keyboard is in display, otherwise, the soft keyboard is hidden.
            int heightDifference = screenHeight - (r.bottom - r.top);
            boolean isKeyboardShowing = heightDifference > screenHeight / 3;
            // If the status of the soft keyboard was previously displayed, it is now closed, or it was previously closed, it is now displayed, it means that the status of the soft keyboard has changed
            if ((mIsSoftKeyboardShowing && !isKeyboardShowing) || (!mIsSoftKeyboardShowing && isKeyboardShowing)) {
                mIsSoftKeyboardShowing = isKeyboardShowing;
                if (!mIsSoftKeyboardShowing && isResume) {
                    if (mCustomEthFeeDialog == null || !mCustomEthFeeDialog.isShow())
                        keyBoardHideRefresh();
                }
            }
        };
        // Register layout change monitoring
        getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(mLayoutChangeListener);
    }

    private void keyBoardHideRefresh() {
        if (editAmount.getText().toString().endsWith(".")) {
            amount = editAmount.getText().toString().substring(0, editAmount.getText().toString().length() - 1).trim();
        } else {
            amount = editAmount.getText().toString().trim();
        }
        if (Strings.isNullOrEmpty(editReceiverAddress.getText().toString().trim())) {
            showToast(R.string.input_address);
            btnNext.setEnabled(false);
            return;
        } else {
            // 收起键盘地址认为 focus，所以再次校验地址正确性
            getAddressIsValid();
        }
        if (Strings.isNullOrEmpty(editAmount.getText().toString().trim())) {
            showToast(R.string.inoutnum);
            btnNext.setEnabled(false);
            return;
        }
        refreshFeeView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        isResume = true;
    }

    /**
     * 判断验证 BTC 金额有效性，并去掉多余的 0
     *
     * @param amount BTC 金额
     * @return 返回处理过的 BTC 金额
     */
    @WorkerThread
    @Nullable
    private String checkAndConvertAmount(@Nullable String amount) {
        BigDecimal amountBigDecimal;
        if (TextUtils.isEmpty(amount)) {
            amountBigDecimal = new BigDecimal("0");
        } else {
            try {
                amountBigDecimal = new BigDecimal(amount);
            } catch (NumberFormatException e) {
                amountBigDecimal = new BigDecimal("0");
            }
        }
        if (amountBigDecimal.equals(BigDecimal.ZERO)) {
            return null;
        }
        int scale;
        switch (baseUnit) {
            case Constant.BTC_UNIT_BTC:
                scale = 8;
                break;
            case Constant.BTC_UNIT_M_BTC:
                scale = 5;
                break;
            case Constant.BTC_UNIT_M_BITS:
                scale = 2;
                break;
            default:
                scale = 0;
        }
        return amountBigDecimal.setScale(scale, RoundingMode.DOWN).stripTrailingZeros().toPlainString();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Scan QR code return
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SCAN_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                String content = data.getStringExtra(com.yzq.zxinglibrary.common.Constant.CODED_CONTENT);
                Disposable disposable = Observable
                        .create((ObservableOnSubscribe<MainSweepcodeBean.DataBean>) emitter -> {
                            MainSweepcodeBean.DataBean dataBean = new QRDecode().decodeAddress(content);
                            if (dataBean == null) {
                                emitter.onError(new RuntimeException("Parse failure"));
                            } else {
                                emitter.onNext(dataBean);
                            }
                            emitter.onComplete();
                        })
                        .map(dataBean -> {
                            String amountStr = checkAndConvertAmount(dataBean.getAmount());
                            if (!TextUtils.isEmpty(amountStr)) {
                                dataBean.setAmount(amountStr);
                            }
                            return dataBean;
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(output -> {
                            editReceiverAddress.setText(output.getAddress());
                            if (null != output && !TextUtils.isEmpty(output.getAmount())) {
                                editAmount.setText(output.getAmount());
                                keyBoardHideRefresh();
                            } else {
                                getAddressIsValid();
                            }
                        }, e -> {
                            e.printStackTrace();
                            showToast(R.string.invalid_address);
                        });
                mCompositeDisposable.add(disposable);
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        isResume = false;
    }

    /**
     * 校验收款地址是否有效
     */
    private void getAddressIsValid() {
        String address = editReceiverAddress.getText().toString();
        if (!Strings.isNullOrEmpty(address)) {
            PyResponse<Void> response = PyEnv.VerifyLegality(address, "address", walletType);
            if (Strings.isNullOrEmpty(response.getErrors())) {
                addressInvalid = true;
            } else {
                addressInvalid = false;
            }
            if (!addressInvalid) {
                editReceiverAddress.setText("");
                showToast(R.string.invalid_address);
                btnNext.setEnabled(false);
            }
        }
    }

    /**
     * 交易金额实时监听
     */
    @OnTextChanged(value = R.id.edit_amount, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    public void onTextChangeAmount(CharSequence sequence) {
        amount = sequence.toString();
        if (!String.valueOf(maxAmount).equals(amount)) {
            isSetBig = false;
        }
        // 金额以点开头
        if (amount.startsWith(".")) {
            editAmount.setText("");
        }
    }

    @OnTextChanged(value = R.id.edit_receiver_address, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    public void onAddress(CharSequence sequence) {
        if (isClickPaste) {
            isClickPaste = false;
            keyBoardHideRefresh();
        }
    }

    @OnFocusChange(value = R.id.edit_receiver_address)
    public void onFocusChanged(boolean focused) {
        if (!focused) {
            keyBoardHideRefresh();
        }
    }

    @OnFocusChange(value = R.id.edit_amount)
    public void onEditAmountFocusChange(boolean focused) {
        if (!focused) {
            if (!Strings.isNullOrEmpty(amount) && Double.parseDouble(amount) > 0) {
                BigDecimal decimal = BigDecimal.valueOf(Double.parseDouble(amount));
                if (decimal.compareTo(minAmount) < 0) {
                    String min = String.format(Locale.ENGLISH, "%s", minAmount.stripTrailingZeros().toPlainString());
                    editAmount.setText(min);
                    editAmount.setSelection(min.length());
                } else if (decimal.compareTo(decimalBalance) >= 0) {
                    if (addressInvalid) {
                        Disposable disposable = getEthFeeRateObservable(String.valueOf(currentFeeRate))
                                .observeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .doOnSubscribe(show -> showProgress())
                                .doFinally(this::dismissProgress)
                                .subscribe(this::doDealMaxAmount);
                    }
                }
            }
        }
    }

    /**
     * 获取三种不同费率对应的临时交易
     */
    private void refreshFeeView() {
        if (isCustom) {
            Disposable subscribe = getEthFeeRateObservable(String.valueOf(currentFeeRate))
                    .doOnSubscribe(show -> showProgress())
                    .doFinally(this::dismissProgress)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .doOnNext(this::dealWithCommonFeeInfo)
                    .subscribe();
            mCompositeDisposable.add(subscribe);
        } else {
            // 刷新3个view 并判断最大值
            Disposable disposable = getDefaultObservable(false)
                    .subscribeOn(Schedulers.io())
                    .doOnSubscribe(show -> showProgress())
                    .doFinally(this::dismissProgress)
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnNext(stringPyResponse -> doDefaultInfo(stringPyResponse, false))
                    .observeOn(Schedulers.io())
                    .flatMap((Function<PyResponse<String>, ObservableSource<PyResponse<TemporaryTxInfo>>>) stringPyResponse
                            -> getEthFeeRateObservable(String.valueOf(currentFeeRate)))
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::doDealMaxAmount);
            mCompositeDisposable.add(disposable);
        }
    }

    /**
     * 获取Eth FeeRate
     */
    private void getEthFeeRate(String feeRate) {
        Disposable disposable = Observable.create((ObservableOnSubscribe<PyResponse<TemporaryTxInfo>>) emitter -> {
            PyResponse<TemporaryTxInfo> pyResponse = PyEnv.getEthFeeByFeeRate(walletType, editReceiverAddress.getText().toString(), isSetBig ? "!" : amount, feeRate, String.valueOf(mGasLimit));
            emitter.onNext(pyResponse);
            emitter.onComplete();
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::dealWithCommonFeeInfo);

        mCompositeDisposable.add(disposable);
    }


    private Observable<PyResponse<TemporaryTxInfo>> getEthFeeRateObservable(String feeRate) {
        return Observable.create(emitter -> {
            PyResponse<TemporaryTxInfo> pyResponse = PyEnv.getEthFeeByFeeRate(walletType, editReceiverAddress.getText().toString(), isSetBig ? "!" : amount, feeRate, String.valueOf(mGasLimit));
            emitter.onNext(pyResponse);
            emitter.onComplete();
        });
    }

    /**
     * 获取默认费率 刷新3个view
     *
     * @param isDefault 首次刷新：true，地址和数量有值，就刷新 3 个view
     *
     * @return
     */
    private Observable<PyResponse<String>> getDefaultObservable(boolean isDefault) {
        return Observable.create(emitter -> {
            PyResponse<String> response = PyEnv.getFeeInfo(isDefault, walletType, pasteAddress.getText().toString().trim(), amount, String.valueOf(currentFeeRate));
            emitter.onNext(response);
            emitter.onComplete();
        });
    }


    /**
     * 处理Eth 自定义视图渲染
     *
     * @param pyResponse
     */
    private void dealWithCommonFeeInfo(PyResponse<TemporaryTxInfo> pyResponse) {
        String errorMsg = pyResponse.getErrors();
        if (Strings.isNullOrEmpty(errorMsg)) {
            TemporaryTxInfo temporaryTxInfo = pyResponse.getResult();
            mCusFee = BigDecimal.valueOf(temporaryTxInfo.getFee()).toPlainString();
            mCusFeeRate = temporaryTxInfo.getFiat();
            String time = getTransferTime(temporaryTxInfo.getTime());
            String temp = temporaryTxInfo.getTx();
            if (isCustom) {
                textCustomizeSpendTime.setText(time);
                textFeeCustomizeInBtc.setText(String.format(Locale.ENGLISH, "%s %s", mCusFee, baseUnit));
                textFeeCustomizeInCash.setText(String.format(Locale.ENGLISH, "%s %s", mSystemConfigManager.getCurrentFiatSymbol(), temporaryTxInfo.getFiat().substring(0, temporaryTxInfo.getFiat().indexOf(" "))));
            }
            isFeeValid = true;
            doDealMaxAmount(pyResponse);
            changeButton();
        } else {
            showToast(errorMsg);
        }
    }

    private void doDealMaxAmount(PyResponse<TemporaryTxInfo> pyResponse) {
        String errorMsg = pyResponse.getErrors();
        if (Strings.isNullOrEmpty(errorMsg)) {
            TemporaryTxInfo temporaryTxInfo = pyResponse.getResult();
            // 查看输入的值是否大于最大值
            maxAmount = BigDecimal.valueOf(Double.parseDouble(balance)).subtract(BigDecimal.valueOf(temporaryTxInfo.getFee()));
            if (isSetBig) {
                editAmount.setText(maxAmount.toPlainString());
            } else {
                // 不能大于最大值，
                if (!Strings.isNullOrEmpty(amount) && Double.parseDouble(amount) > 0) {
                    BigDecimal decimal = BigDecimal.valueOf(Double.parseDouble(amount));
                    if (decimal.compareTo(maxAmount) >= 0) {
                        editAmount.setText(maxAmount.toPlainString());
                    }
                }
            }
            editAmount.setFocusable(true);
        } else {
            mToast(errorMsg);
        }
    }

    /**
     * 硬件签名方法
     */
    private void hardwareSign(String rawTx) {
        new BusinessAsyncTask().setHelper(this).execute(BusinessAsyncTask.SIGN_TX, rawTx, MyApplication.getInstance().getDeviceWay());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onChangePin(ChangePinEvent event) {
        // 回写PIN码
        PyEnv.setPin(event.toString());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onButtonRequest(ButtonRequestEvent event) {
        switch (event.getType()) {
            case PyConstant.PIN_CURRENT:
                Intent intent = new Intent(this, VerifyPinActivity.class);
                startActivity(intent);
                break;
            case PyConstant.BUTTON_REQUEST_7:
                break;
            case PyConstant.BUTTON_REQUEST_8:
                EventBus.getDefault().post(new ExitEvent());
                sendETH();
                break;
            default:
        }
    }

    @Override
    public void onPreExecute() {
        signClickable = false;
    }

    @Override
    public void onException(Exception e) {
        showToast(e.getMessage());
        signClickable = true;
    }

    @Override
    public void onResult(String s) {
        if (!Strings.isNullOrEmpty(s)) {
            signedTx = s;
            if (confirmDialog != null && confirmDialog.getBtnConfirmPay() != null) {
                confirmDialog.getBtnConfirmPay().setEnabled(true);
            }
        } else {
            finish();
        }
        signClickable = true;
    }

    @Override
    public void onCancelled() {
        signClickable = true;
    }

    @Override
    public void currentMethod(String methodName) {
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    @Override
    public boolean needEvents() {
        return true;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void event(SecondEvent updataHint) {
        String msgVote = updataHint.getMsg();
        if ("finish".equals(msgVote)) {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        mCompositeDisposable.dispose();
        if (Objects.nonNull(subscribe)) {
            subscribe.dispose();
        }
    }

    @Override
    public void onPaste() {
        isClickPaste = true;
    }


    // 自定义费率完成的回调
    @Override
    public void onCustomComplete(CustomizeFeeRateEvent event) {
        isCustom = true;
        mGasLimit = event.getGasLimit();
        mCusFee = event.getFee();
        mCusFeeRate = event.getCash();
        linearRateSelector.setVisibility(View.GONE);
        linearCustomize.setVisibility(View.VISIBLE);
        currentFeeRate = Double.parseDouble(event.getFeeRate());
        textFeeCustomizeInBtc.setText(String.format(Locale.ENGLISH, "%s %s", event.getFee(), mSystemConfigManager.getCurrentBaseUnit(Vm.convertCoinType(Constant.ETH))));
        String ss = String.format(Locale.ENGLISH, "%s %s", mSystemConfigManager.getCurrentFiatSymbol(), event.getCash());
        textFeeCustomizeInCash.setText(String.format(Locale.ENGLISH, "%s %s", mSystemConfigManager.getCurrentFiatSymbol(), event.getCash().substring(0, event.getCash().indexOf(" "))));
        textCustomizeSpendTime.setText(event.getTime());
        keyBoardHideRefresh();
    }

}
