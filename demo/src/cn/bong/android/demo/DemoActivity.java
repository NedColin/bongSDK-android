package cn.bong.android.demo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import cn.bong.android.sdk.BongConst;
import cn.bong.android.sdk.BongManager;
import cn.bong.android.sdk.config.Environment;
import cn.bong.android.sdk.event.BongEvent;
import cn.bong.android.sdk.event.DataEvent;
import cn.bong.android.sdk.event.TouchEvent;
import cn.bong.android.sdk.event.TouchEventListener;
import cn.bong.android.sdk.model.ble.ConnectState;
import cn.bong.android.sdk.model.ble.ConnectUiListener;
import cn.bong.android.sdk.model.http.auth.AuthError;
import cn.bong.android.sdk.model.http.auth.AuthInfo;
import cn.bong.android.sdk.model.http.auth.AuthUiListener;
import cn.bong.android.sdk.model.http.data.DataSyncError;
import cn.bong.android.sdk.model.http.data.DataSyncState;
import cn.bong.android.sdk.model.http.data.DataSyncUiListener;
import cn.bong.android.sdk.utils.DialogUtil;
import com.litesuits.android.log.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class DemoActivity extends Activity implements View.OnClickListener {

    private static final String TAG      = DemoActivity.class.getSimpleName();
    private static final int    MAX_WAIT = 6;// 10 有效控制时间
    private ListView listView;
    private ArrayList<BongEvent> events  = new ArrayList<BongEvent>();
    private EventAdapter         adapter = new EventAdapter();
    private int                  seconds = MAX_WAIT;
    private LinearLayout hideView;
    private Button       vibrate;
    private Button       light;
    private Button       startSensor;
    private Button       stopSensor;
    //private Button       userInfo;
    private Button       userAuth;
    private Button       clear;
    private Button       btStartScann;
    private Button       btSyncData;
    //private Button       btGetBongMac;
    private TextView     timeTips;
    private Activity activity = this;

    private ProgressDialog sensorProgressDialog;
    private ProgressDialog syncProgressDialog ;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    seconds--;
                    if (seconds > 0) {
                        timeTips.setText(seconds + ": 触摸后可以向bong发送指令。");
                        handler.sendEmptyMessageDelayed(0, 1000);
                    } else {
                        hideView.setVisibility(View.GONE);
                    }
                    break;
                case 1:
                    DataEvent e = (DataEvent) msg.obj;
                    sensorProgressDialog.setTitle("按back键停止");
                    sensorProgressDialog.setMessage("x: " + e.getX() + ", y: " + e.getY() + ", z: " + e.getZ());
            }
        }
    };

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        findViews();
        hideView.setVisibility(View.GONE);
        listView.setAdapter(adapter);
     	syncProgressDialog = new ProgressDialog(activity);
        sensorProgressDialog = new ProgressDialog(activity);

        // 测试用AppID（仅测试环境）
        //client_id  1419735044202
        //sign_key 7ae31974a95fec07ad3d047c075b11745d8ce989
        //client_secret  558860f5ba4546ddb31eafeee11dc8f4

        // 初始化sdk(接入api时需要AppID和AppSecret，且请注意AppSecret等信息的保密工作，防止被盗用)
        BongManager.initialize(this, "1419735044202", "", "558860f5ba4546ddb31eafeee11dc8f4");
        // 开启 调试模式，打印日志（默认关闭）
        BongManager.setDebuged(true);
        //设置 环境（默认线上）：Daily（测试）,  PreDeploy（预发，线上数据）, Product（线上）;
        BongManager.setEnvironment(Environment.Product);
        // 设置触摸Yes键时震动
        BongManager.setTouchVibrate(true);
        refreshButton();
    }


    @Override
    public void onClick(View v) {
        // 以下操作全部在触摸之后10秒内执行
        if (v == vibrate) {
            //  震动示例
            BongManager.bongVibrate(3, null);
        } else if (v == light) {
            // 亮灯示例
            BongManager.bongLight(3, null);
        } else if (v == startSensor) {
            // 开启传感器示例
            if (!sensorUiListener.isInConntecting()) {
                BongManager.bongStartSensorOutput(sensorUiListener);
            }
        } else if (v == stopSensor) {
            //  关闭传感器示例，请注意在合适的时候注销监听防止内存泄露
            BongManager.bongStopSensorOutput(sensorUiListener);
        } else if (v == clear) {
            // 清除事件日志 
            events.clear();
            adapter.notifyDataSetChanged();
            //
        } else if (v == userAuth) {
            if (BongManager.isSessionValid()) {
                // 取消授权
                BongManager.bongClearAuth();
                refreshButton();
            } else {
                // 开始授权
                BongManager.bongAuth(this, "demo", new AuthUiListener() {
                    @Override
                    public void onError(AuthError error) {
                        DialogUtil.showTips(activity, "授权失败", " code  : " + error.code
                                + "\nmsg   : " + error.message
                                + "\ndetail: " + error.errorDetail);
                    }

                    @Override
                    public void onSucess(AuthInfo result) {
                        DialogUtil.showTips(activity, "授权成功", " state : " + result.state
                                + "\ntoken : " + result.accessToken
                                + "\nexpire: " + result.expiresIn
                                + "\nuid   : " + result.uid
                                + "\nscope : " + result.scope
                                + "\nrefreh_expire: " + result.refreshTokenExpiration
                                + "\nrefreh_token : " + result.refreshToken
                                + "\ntokenType    : " + result.tokenType);
                        refreshButton();
                    }

                    @Override
                    public void onCancel() {
                        //DialogUtil.showTips(activity, "提示", "授权取消");
                    }
                });

            }

        } else if (v == btStartScann) {
            if (BongManager.isScanning()) {
                BongManager.turnOffTouchEventListen(this);
                btStartScann.setText("开始触摸监听");
            } else {
                BongManager.turnOnTouchEventListen(this, new TouchEventListener() {
                    @Override
                    public void onTouch(TouchEvent event) {
                        events.add(event);
                        adapter.notifyDataSetChanged();
                        showMoreActions();
                    }

                    @Override
                    public void onLongTouch(TouchEvent event) {
                        events.add(event);
                        adapter.notifyDataSetChanged();
                        showMoreActions();
                    }
                });
                btStartScann.setText("关闭触摸监听");
            }
        } else if (v == btSyncData) {
            if (BongManager.isDataSyncing()) {
                DialogUtil.showTips(activity, null, "正在同步...");
            } else {
                AlertDialog.Builder builder = DialogUtil.dialogBuilder(this, "选择同步方式", null);
                builder.setItems(new String[]{"增量同步：最后一次同步到现在",
                        "同步过去的24小时到现在", "同步指定时间内数据"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                // 增量同步：最后一次同步到现在
                                BongManager.bongDataSyncnizedByUpdate(listener);
                                break;
                            case 1:
                                // 同步过去的24小时到现在
                                BongManager.bongDataSyncnizedByHours(listener, System.currentTimeMillis(), 24);
                                break;
                            case 2:
                                int tenMinutes = 10 * 60000;
                                long endTime = System.currentTimeMillis();
                                long startTime = endTime - tenMinutes;
                                // 同步指定时间内数据
                                BongManager.bongDataSyncnizedByTime(listener, startTime, endTime);
                                break;
                        }
                        refreshButton();
                    }
                });
                builder.setPositiveButton("取消", null);
                builder.show();
            }
        }
    }

    ConnectUiListener sensorUiListener = new ConnectUiListener() {


        @Override
        public void onStateChanged(ConnectState state) {
            if (state == ConnectState.Scanning) {
                sensorProgressDialog.setTitle("连接中");
                sensorProgressDialog.setMessage("请触摸 Yes! 键...");
                sensorProgressDialog.setCancelable(true);
                sensorProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        BongManager.bongStopSensorOutput(sensorUiListener);
                    }
                });
            } else if (state == ConnectState.Connecting) {
                sensorProgressDialog.setMessage("连接中...");
            }
        }

        @Override
        public void onFailed(String msg) {
            if (sensorProgressDialog.isShowing()) sensorProgressDialog.dismiss();
            DialogUtil.showTips(activity, "读取失败", msg);
        }

        @Override
        public void onSucess() {
            if (sensorProgressDialog.isShowing()) sensorProgressDialog.dismiss();
        }

        @Override
        public void onDataReadInBackground(byte[] data) {
            Log.v(TAG, "data: " + Arrays.toString(data));
            if (data.length > 5) {
                DataEvent e = new DataEvent(System.currentTimeMillis(), data[1], data[3], data[5]);
                Message msg = handler.obtainMessage(1);
                msg.obj = e;
                msg.sendToTarget();
            }

        }
    };

    DataSyncUiListener listener = new DataSyncUiListener() {

        @Override
        public void onStateChanged(DataSyncState state) {
            if (state == DataSyncState.Scanning) {
                syncProgressDialog.setTitle("同步中");
                syncProgressDialog.setMessage("请触摸 Yes! 键...");
                syncProgressDialog.show();
            } else if (state == DataSyncState.Connecting) {
                syncProgressDialog.setMessage("发现设备，正在同步...");
            } else if (state == DataSyncState.Uploading) {
                syncProgressDialog.setMessage("同步完成，正在上传...");
            }
        }

        @Override
        public void onError(DataSyncError error) {
            if (syncProgressDialog.isShowing()) syncProgressDialog.dismiss();
            DialogUtil.showTips(activity, error.message, error.errorDetail);
            refreshButton();

            if (error.getCode() == DataSyncError.ErrorType.ERROR_CODE_UNBIND) {
                // 异步请求获取最新绑定的手环mac。
                BongManager.bongRefreshMacAsync(null);
            }
        }

        @Override
        public void onSucess() {
            if (syncProgressDialog.isShowing()) syncProgressDialog.dismiss();
            DialogUtil.showTips(activity, "同步成功", "数据已上传至云端");
            refreshButton();
        }
    };


    private void showMoreActions() {
        hideView.setVisibility(View.VISIBLE);
        seconds = MAX_WAIT;
        handler.removeMessages(0);
        handler.sendEmptyMessageDelayed(0, 1000);
    }

    private void findViews() {
        hideView = (LinearLayout) findViewById(R.id.llHideView);
        vibrate = (Button) findViewById(R.id.btVibrate);
        light = (Button) findViewById(R.id.btLight);
        startSensor = (Button) findViewById(R.id.btStartSensor);
        stopSensor = (Button) findViewById(R.id.btStopSensor);
        //userInfo = (Button) findViewById(R.id.btUserInfo);
        userAuth = (Button) findViewById(R.id.btUserAuth);
        clear = (Button) findViewById(R.id.btClear);
        btSyncData = (Button) findViewById(R.id.btSyncData);
        btStartScann = (Button) findViewById(R.id.btStartScann);
        listView = (ListView) findViewById(R.id.listView);
        timeTips = (TextView) findViewById(R.id.tvTimeTips);

        vibrate.setOnClickListener(this);
        light.setOnClickListener(this);
        startSensor.setOnClickListener(this);
        stopSensor.setOnClickListener(this);
        //userInfo.setOnClickListener(this);
        userAuth.setOnClickListener(this);
        clear.setOnClickListener(this);
        btSyncData.setOnClickListener(this);
        btStartScann.setOnClickListener(this);

    }

    private void refreshButton() {
        if (BongManager.isScanning()) {
            btStartScann.setText("关闭触摸监听");
        } else {
            btStartScann.setText("开始触摸监听");
        }
        if (BongManager.isDataSyncing()) {
            btSyncData.setText("正在同步...");
        } else {
            btSyncData.setText("开始同步");
        }
        if (BongManager.isSessionValid()) {
            userAuth.setText("取消授权");
        } else {
            userAuth.setText("开始授权");
        }
    }

    public void showTips(String tip) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setMessage(tip);
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    class EventAdapter extends BaseAdapter {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss:SSS");

        @Override
        public int getCount() {
            return events.size();
        }

        @Override
        public BongEvent getItem(int position) {
            return events.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_event, null);
            }
            TextView tv = (TextView) convertView;
            if (tv != null && events != null) {
                BongEvent event = getItem(position);
                switch (event.getEventType()) {
                    case BongConst.EVENT_YES_TOUCH:
                        // 短触：触摸 yes! 键 1秒左右
                        tv.setText(String.format("%-6s", (position + 1) + ".") + "短触 Yes! 键  " + format.format(new Date
                                (event.getTime())));
                        break;
                    case BongConst.EVENT_YES_LONG_TOUCH:
                        // 长触：触摸 yes! 键 3秒左右
                        tv.setText(String.format("%-6s", (position + 1) + ".") + "长触 Yes! 键  " + format.format(new Date
                                (event.getTime())));
                        break;
                    case BongConst.EVENT_DATA_XYZ:
                        // 数据：接收传感器 xyz 三轴原始数据：200秒连接时间，超时自动断开。
                        DataEvent de = (DataEvent) event;
                        tv.setText(String.format("%-6s", (position + 1) + ".") + "数据传输 X：" + String.format("%-4s",
                                de.getX())
                                + "  Y: " + String.format("%-4s", de.getY())
                                + "  Z: " + String.format("%-4s", de.getZ())
                                + "  " + format.format(new Date(event.getTime())));
                        showMoreActions();
                        break;
                    default:
                        break;
                }
            }
            return convertView;
        }
    }
}
