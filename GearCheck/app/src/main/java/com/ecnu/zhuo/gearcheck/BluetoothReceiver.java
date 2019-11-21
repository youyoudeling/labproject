package com.ecnu.zhuo.gearcheck;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.icu.text.IDNA;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;


public class BluetoothReceiver extends BroadcastReceiver{

    private static final int ACCESS_LOCATION = 10;
    String pin = "1234";  //此处为你要连接的蓝牙设备的初始密钥，一般为1234或0000
    public BluetoothReceiver() {

    }
    @Override
    public void onReceive(Context context, Intent intent) {


        String action = intent.getAction(); //得到action
        Log.e("tishi action1", action);
        BluetoothDevice btDevice=null;  //创建一个蓝牙device对象
        // 从Intent中获取设备对象
        btDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        if(BluetoothDevice.ACTION_FOUND.equals(action)){  //发现设备

            Log.e("tishi 发现设备:", "["+btDevice.getName()+"]"+":"+btDevice.getAddress());


            if(btDevice.getName()!=null && btDevice.getName().contains("HC-05"))//HC-05设备如果有多个，第一个搜到的那个会被尝试。
            {
                if (btDevice.getBondState() == BluetoothDevice.BOND_NONE) {

                    Log.e("tishi ywq", "attemp to bond:"+"["+btDevice.getName()+"]");
                    try {
                        //通过工具类ClsUtils,调用createBond方法
                        ClsUtils.createBond(btDevice.getClass(), btDevice);
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }else
                Log.e("tishi error", "Is faild");
        }else if(action.equals("android.bluetooth.device.action.PAIRING_REQUEST")) //再次得到的action，会等于PAIRING_REQUEST
        {
            Log.e("tishi action2=", action);
            if(btDevice.getName().contains("HC-05"))
            {
                Log.e("tishi here", "OKOKOK");

                try {


                    Log.e("tiship", "确认配对");

                    //1.确认配对 已经确认好了 所以不用确认了
                    //ClsUtils.setPairingConfirmation(btDevice.getClass(), btDevice, true);
                    //2.终止有序广播
                    Log.e("tiship", "终止有序广播");
                    Log.i("tiship...", "isOrderedBroadcast:"+isOrderedBroadcast()+",isInitialStickyBroadcast:"+isInitialStickyBroadcast());
                    abortBroadcast();//如果没有将广播终止，则会出现一个一闪而过的配对框。
                    //3.调用setPin方法进行配对...
                    boolean ret = ClsUtils.setPin(btDevice.getClass(), btDevice, pin);
                    if(ret){
                        Intent intent_toinfoshow=new Intent(context, InfoShowActivity.class);
                        intent_toinfoshow.putExtra("BLE_MAC",Constant.BL_MAC);
                        // i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent_toinfoshow);
                    }
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }else
                Log.e("tishi 提示信息", "这个设备不是目标蓝牙设备");

        }else if(action.equals("android.bluetooth.adapter.action.DISCOVERY_FINISHED")){
            Log.e("tishi","搜索结束");
        }
    }

}



    /*
	//跳弹窗版本
	@Override
	public void onReceive(Context context, Intent intent) {


		String action = intent.getAction(); //得到action
		Log.e("tishi action1", action);
		BluetoothDevice btDevice=null;  //创建一个蓝牙device对象
		// 从Intent中获取设备对象
		btDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

		if(BluetoothDevice.ACTION_FOUND.equals(action)){  //发现设备

			Log.e("tishi 发现设备:", "["+btDevice.getName()+"]"+":"+btDevice.getAddress());


			if(btDevice.getName()!=null && btDevice.getName().contains("HC-05"))//HC-05设备如果有多个，第一个搜到的那个会被尝试。
			{
				if (btDevice.getBondState() == BluetoothDevice.BOND_NONE) {

					Log.e("tishi ywq", "attemp to bond:"+"["+btDevice.getName()+"]");
					try {
						//通过工具类ClsUtils,调用createBond方法
						ClsUtils.createBond(btDevice.getClass(), btDevice);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}else
				Log.e("tishi error", "Is faild");
		}else if(action.equals("android.bluetooth.device.action.PAIRING_REQUEST")) //再次得到的action，会等于PAIRING_REQUEST
		{
			Log.e("tishi action2=", action);
			if(btDevice.getName().contains("HC-05"))
			{
				Log.e("tishi here", "OKOKOK");

				try {

					//1.确认配对
					ClsUtils.setPairingConfirmation(btDevice.getClass(), btDevice, true);
					//2.终止有序广播
					Log.i("tishi order...", "isOrderedBroadcast:"+isOrderedBroadcast()+",isInitialStickyBroadcast:"+isInitialStickyBroadcast());
					abortBroadcast();//如果没有将广播终止，则会出现一个一闪而过的配对框。
					//3.调用setPin方法进行配对...
					boolean ret = ClsUtils.setPin(btDevice.getClass(), btDevice, pin);

				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}else
				Log.e("tishi 提示信息", "这个设备不是目标蓝牙设备");

		}else if(action.equals("android.bluetooth.adapter.action.DISCOVERY_FINISHED")){
			Log.e("tishi","搜索结束");
		}
	}*/

