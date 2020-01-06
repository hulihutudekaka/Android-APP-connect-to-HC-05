package com.example.buletooth;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;


import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

public  class MainActivity extends Activity{


    private final static int REQUEST_CONNECT_DEVICE = 1;    //宏定义查询设备句柄

    private final static String MY_UUID = "00001101-0000-1000-8000-00805F9B34FB";   //SPP服务UUID号


    private  ScrollView scrollview;//翻页
    private TextView textview;//接受消息
    private InputStream acceptbluetooth;//输入流，用来接收蓝牙数据
    boolean bRun = true;
    boolean bThread = false;

    private String showdatacache="";
    private String savedatacache="";


    BluetoothDevice mdevice = null;
    BluetoothSocket msocket = null;

    private BluetoothAdapter mbluetooth = BluetoothAdapter.getDefaultAdapter();    //获取本地蓝牙适配器，即蓝牙设备

    @Override
    public void  onCreate(Bundle savedInstanceState){

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);   //设置画面为主画面 activity_main.xml

        final int MY_PERMISSION_ACCESS_COARSE_LOCATION = 11;
        final int MY_PERMISSION_ACCESS_FINE_LOCATION = 12;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(this.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION},MY_PERMISSION_ACCESS_COARSE_LOCATION);
                Log.e("11111","ACCESS_COARSE_LOCATION");
            }
            if(this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},MY_PERMISSION_ACCESS_FINE_LOCATION);
                Log.e("11111","ACCESS_FINE_LOCATION");
            }
        }
        ///---------------------------------------------------
        //text0 = (TextView)findViewById(R.id.Text0);  //得到提示栏句柄
        //edit0 = (EditText)findViewById(R.id.Edit0);   //得到输入框句柄
        scrollview = (ScrollView)findViewById(R.id.ScrollView01);  //得到翻页句柄
        textview = (TextView) findViewById(R.id.in);      //得到数据显示句柄

        //如果打开本地蓝牙设备不成功，提示信息，结束程序
        if (mbluetooth == null){
            Toast.makeText(this, "无法打开手机蓝牙，请确认手机是否有蓝牙功能！", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // 设置设备可以被搜索
        new Thread(){
            public void run(){
                if(mbluetooth.isEnabled()==false){
                    mbluetooth.enable();
                }
            }
        }.start();
    }


    //接收活动结果，响应startActivityForResult()
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode){
            case REQUEST_CONNECT_DEVICE:
                // 响应返回结果                            //连接结果，由DeviceListActivity设置返回
                if (resultCode == Activity.RESULT_OK) {   //连接成功，由DeviceListActivity设置返回
                    // MAC地址，由DeviceListActivity设置返回
                    String address = data.getExtras()
                            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    // 得到蓝牙设备句柄
                    mdevice = mbluetooth.getRemoteDevice(address);

                    // UUID得到socket
                    try{
                        msocket = mdevice.createRfcommSocketToServiceRecord(UUID.fromString(MY_UUID));
                    }catch(IOException e){
                        Toast.makeText(this, "连接失败！", Toast.LENGTH_SHORT).show();
                    }
                    //连接socket
                    Button btn = (Button) findViewById(R.id.BtnConnect);
                    try{
                        msocket.connect();
                        Toast.makeText(this, "连接"+mdevice.getName()+"成功！", Toast.LENGTH_SHORT).show();
                        btn.setText("断开");
                    }catch(IOException e){
                        try{
                            Toast.makeText(this, "连接失败！", Toast.LENGTH_SHORT).show();
                            msocket.close();
                            msocket = null;
                        }catch(IOException ee){
                            Toast.makeText(this, "连接失败！", Toast.LENGTH_SHORT).show();
                        }

                        return;
                    }

                    //打开接收线程
                    try{
                        acceptbluetooth = msocket.getInputStream();   //得到蓝牙数据输入流
                    }catch(IOException e){
                        Toast.makeText(this, "接收数据失败！", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if(bThread==false){
                        readThread.start();
                        bThread=true;
                    }else{
                        bRun = true;
                    }
                }
                break;
            default:break;
        }
    }

    //接收数据线程
    Thread readThread=new Thread(){

        public void run(){
            int num = 0;
            byte[] buffer = new byte[1024];
            byte[] buffer_new = new byte[1024];
            int i = 0;
            int n = 0;
            bRun = true;
            //接收线程
            while(true){
                try{
                    while(acceptbluetooth.available()==0){
                        while(bRun == false){}
                    }
                    while(true){
                        if(!bThread)//跳出循环
                            return;
                        num = acceptbluetooth.read(buffer);  //读入数据
                        n=0;

                        String s0 = new String(buffer,0,num);
                        savedatacache+=s0;    //保存收到数据 0x0d 回车
                        for(i=0;i<num;i++){
                            if((buffer[i] == 0x0d)&&(buffer[i+1]==0x0a)){
                                buffer_new[n] = 0x0a;
                                i++;
                            }else{
                                buffer_new[n] = buffer[i];
                            }
                            n++;
                        }
                        String s = new String(buffer_new,0,n);

                        showdatacache=str2HexStr(s);

                        //showdatacache+=s;   //写入接收缓存
                        if(acceptbluetooth.available()==0)break;  //短时间没有数据才跳出进行显示
                    }
                    //发送显示消息，进行显示刷新
                    handler.sendMessage(handler.obtainMessage());
                }catch(IOException e){
                }
            }
        }
    };


    public static String str2HexStr(String s)
    {

        char[] chars = "0123456789ABCDEF".toCharArray();
        StringBuilder sb = new StringBuilder("");
        byte[] bs = s.getBytes();
        int bit;

        for (int i = 0; i < bs.length; i++)
        {
            bit = (bs[i] & 0x0f0) >> 4;
            sb.append(chars[bit]);
            bit = bs[i] & 0x0f;
            sb.append(chars[bit]);
            sb.append(' ');
        }
        return sb.toString().trim();
    }


    //消息处理队列
    Handler handler= new Handler(){
        public void handleMessage(Message msg){
            super.handleMessage(msg);
            textview.setText(showdatacache);   //显示数据
            scrollview.scrollTo(0,textview.getMeasuredHeight()); //跳至数据最后一页
        }
    };

    //关闭程序掉用处理部分
    public void onDestroy(){
        super.onDestroy();
        if(msocket!=null)  //关闭连接socket
            try{
                msocket.close();
            }catch(IOException e){}
        //	_bluetooth.disable();  //关闭蓝牙服务
    }

    //连接按键响应函数
    public void onConnectButtonClicked(View v){
        if(mbluetooth.isEnabled()==false){  //如果蓝牙服务不可用则提示
            Toast.makeText(this, " 打开蓝牙中...", Toast.LENGTH_LONG).show();
            return;
        }
        //如未连接设备则打开DeviceListActivity进行设备搜索
        Button btn = (Button) findViewById(R.id.BtnConnect);
        if(msocket==null){
            Intent serverIntent = new Intent(this, DeviceListActivity.class); //跳转程序设置
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);  //设置返回宏定义
        }
        else{
            //关闭连接socket
            try{
                bRun = false;
                Thread.sleep(2000);

                acceptbluetooth.close();
                msocket.close();
                msocket = null;

                btn.setText("连接");
            }catch(IOException e){}
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return;
    }

    //保存按键响应函数
    /**public void onSaveButtonClicked(View v){
        Save();
    }*/

    //清除按键响应函数
    public void onClearButtonClicked(View v){
        showdatacache="";
        savedatacache="";
        textview.setText(savedatacache);
        return;
    }

    //退出按键响应函数
    public void onQuitButtonClicked(View v){

        //---安全关闭蓝牙连接再退出，避免报异常----//
        if(msocket!=null){
            //关闭连接socket
            try{
                bRun = false;
                Thread.sleep(2000);

                acceptbluetooth.close();
                msocket.close();
                msocket = null;
            }catch(IOException e){}
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        finish();
    }

    //保存功能实现,用不上，哈哈哈
   /** private void Save() {
        //显示对话框输入文件名
        LayoutInflater factory = LayoutInflater.from(BTClient.this);  //图层模板生成器句柄
        final View DialogView =  factory.inflate(R.layout.sname, null);  //用sname.xml模板生成视图模板
        new AlertDialog.Builder(BTClient.this)
                .setTitle("文件名")
                .setView(DialogView)   //设置视图模板
                .setPositiveButton("确定",
                        new DialogInterface.OnClickListener() //确定按键响应函数
                        {
                            public void onClick(DialogInterface dialog, int whichButton){
                                EditText text1 = (EditText)DialogView.findViewById(R.id.sname);  //得到文件名输入框句柄
                                filename = text1.getText().toString();  //得到文件名

                                try{
                                    if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){  //如果SD卡已准备好

                                        filename =filename+".txt";   //在文件名末尾加上.txt
                                        File sdCardDir = Environment.getExternalStorageDirectory();  //得到SD卡根目录
                                        File BuildDir = new File(sdCardDir, "/data");   //打开data目录，如不存在则生成
                                        if(BuildDir.exists()==false)BuildDir.mkdirs();
                                        File saveFile =new File(BuildDir, filename);  //新建文件句柄，如已存在仍新建文档
                                        FileOutputStream stream = new FileOutputStream(saveFile);  //打开文件输入流
                                        stream.write(fmsg.getBytes());
                                        stream.close();
                                        Toast.makeText(BTClient.this, "存储成功！\n\r"+saveFile, Toast.LENGTH_LONG).show();
                                    }else{
                                        Toast.makeText(BTClient.this, "没有存储卡！", Toast.LENGTH_LONG).show();
                                    }

                                }catch(IOException e){
                                    return;
                                }



                            }
                        })
                .setNegativeButton("取消",   //取消按键响应函数,直接退出对话框不做任何处理
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        }).show();  //显示对话框
    }
*/




}