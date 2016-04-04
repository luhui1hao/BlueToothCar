package com.example.bluetoothcar;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity {

	private long lastClickTime = 0;
//	private Button up,down,left,right,clear;
	private BluetoothSocket socket;
	private InputStreamReader in = null;
	private OutputStreamWriter out = null;
	private Handler handler = null;
	private TextView textView ;
	private Bundle data = new Bundle();
	private Spinner spinner;
	private ArrayAdapter<String> arrayAdapter;
	private BluetoothAdapter adapter;
	private Set<BluetoothDevice> devices = new HashSet<>();
	private List<String> macAddress = new ArrayList();
	BluetoothBroadcast bluetoothBroadcast = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        	//获取蓝牙适配器
      		adapter = BluetoothAdapter.getDefaultAdapter();
      		//判断本机是否有蓝牙设备
      		if(adapter!=null) {
      			Toast.makeText(MainActivity.this, "本机拥有蓝牙设备" ,Toast.LENGTH_SHORT).show();
      			//判断本机蓝牙设备是否可用
      			if(!adapter.isEnabled()) {
      				//如果蓝牙设备不可用，就启动一个Activity提示用户打开蓝牙设备
      				Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
      				startActivity(intent);
      				
      				//注册一个广播监听蓝牙状态
      				IntentFilter intentFilter = new IntentFilter();
      				intentFilter.addAction("android.bluetooth.adapter.action.STATE_CHANGED");
      				bluetoothBroadcast = new BluetoothBroadcast();
      				registerReceiver(bluetoothBroadcast, intentFilter);
      			}
      			//得到所有已配对的蓝牙对象
      			devices = adapter.getBondedDevices();
      			/*if(devices.size()>0) {
      				for(Iterator iterator=devices.iterator();iterator.hasNext();) {
      					BluetoothDevice bd = (BluetoothDevice)iterator.next();
      				}
      			}*/
      		}
      		else {
      			Toast.makeText(MainActivity.this, "本机没有蓝牙设备" ,Toast.LENGTH_LONG).show();

      		}
      		
    		//获取Spinner组件
    		spinner = (Spinner)findViewById(R.id.spinner);
    		arrayAdapter = new ArrayAdapter<String>(this, R.layout.support_simple_spinner_dropdown_item);
    		arrayAdapter.add("断开连接");
    		if(devices.size()>0) {
    		for(Iterator<BluetoothDevice> iterator=devices.iterator();iterator.hasNext();) {
    			BluetoothDevice bd = iterator.next();
    			arrayAdapter.add(bd.getName() + "\n" + bd.getAddress());
    			macAddress.add(bd.getAddress());//保存蓝牙MAC地址
    		}
    		}
    		spinner.setAdapter(arrayAdapter);
      		
      	//数据接收区的初始化	
        textView = (TextView)findViewById(R.id.textView);
        textView.setMovementMethod(ScrollingMovementMethod.getInstance()); 
        findViewById(R.id.clear).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				textView.setText("");
			}
		});
        
        spinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				if(socket!=null)
				{
					try {
						socket.close();
					} catch (IOException e) {e.printStackTrace();}
				}
				if(position==0)
				{
					if(socket!=null)
					{
						try {
							socket.close();
						} catch (IOException e) {e.printStackTrace();}
					}
				}
				else {
				//测试接收到的position和id
				System.out.println("position is " + position + "id is " + id); 
				
				try {
			        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // UUID of server socket
			        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
			        BluetoothDevice device = adapter.getRemoteDevice(macAddress.get(position-1)); // BT MAC address of s erver
			        // 发现连接后会获取服务端 socket 套接字
			        socket = device.createRfcommSocketToServiceRecord(uuid);
			        // 与服务端建立连接
			        socket.connect();
			        //双方连接上后，就开始读写了
			        in = new InputStreamReader(socket.getInputStream(),"gbk");
			        out = new OutputStreamWriter(socket.getOutputStream(),"gbk");
		        }catch(Exception e){}
			    	
		        OnTouchListener btnListener = new BtnListener(); 
		        findViewById(R.id.up).setOnTouchListener(btnListener);
		        findViewById(R.id.down).setOnTouchListener(btnListener);
		        findViewById(R.id.left).setOnTouchListener(btnListener);
		        findViewById(R.id.right).setOnTouchListener(btnListener);
		        findViewById(R.id.trumpet).setOnTouchListener(btnListener);

		      //创建并开启线程
		        new ReceiveThread().start();
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// TODO 自动生成的方法存根
				
			}
		});
        
        
        
        handler = new Handler() {

			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				if(msg.what==1234)
				{
					Bundle data = msg.getData();
					textView.append(data.getString("receive"));
				}
			}
        	
        };
        
        
    }
   
    class BtnListener implements OnTouchListener {

    	@Override  
        public boolean onTouch(View v, MotionEvent event) {  
            if(event.getAction() == KeyEvent.ACTION_DOWN) {  
            	try {
    				switch(v.getId())
    				{
    					case R.id.up:out.write(0x01);break;
    					case R.id.down:out.write(0x02);break;
    					case R.id.left:out.write(0x03);break;
    					case R.id.right:out.write(0x04);break;
    					case R.id.trumpet:out.write(0x05);break;
    				}
    			} catch (IOException e) {e.printStackTrace();}
            }  
            if (event.getAction() == KeyEvent.ACTION_UP) {  
            	try {
					out.write(0x06);
				} catch (IOException e) {
					e.printStackTrace();
				}
            }  
            //return true;// 返回true的话，单击事件、长按事件不可以被触发  
             return false;  
        }  
	}
    
    
    
    class ReceiveThread extends Thread {
    	public void run()
    	{
    		//一次优化接收数据出错的尝试
    		char temp=0;
    		while(true)
    		{
    			StringBuffer sb = new StringBuffer();
    			try {
    				temp=(char)in.read();
    				while(temp!='\n')
    				{
    					sb.append(temp);
    					temp=(char)in.read();
    				}
    				sb.append('\n');
    				
    				Message msg = new Message();
					msg.what = 1234;
					
					data.putString("receive",sb.toString());
					msg.setData(data);
					handler.sendMessage(msg);
    				
				} catch (Exception e) {e.printStackTrace();}
    		}
    	}
    }
    
    

    
    @Override
	protected void onDestroy() {
		super.onDestroy();
		try {
			if(socket!=null)
			{
				in.close();
				out.close();
				socket.close();
			}
		} catch (IOException e) {e.printStackTrace();}
		
		if(bluetoothBroadcast!=null){
			unregisterReceiver(bluetoothBroadcast);
		}
	}
    
    public void onBackPressed() {
		if(lastClickTime==0)
		{
			Toast.makeText(this, "双击退出程序",Toast.LENGTH_SHORT ).show();
			lastClickTime = System.currentTimeMillis();
		}
		else
		{
			long currentClickTime = System.currentTimeMillis();
			if(currentClickTime-lastClickTime <1000)
			{
				finish();
			}
			else
			{
				Toast.makeText(this, "双击退出程序",Toast.LENGTH_SHORT ).show();
				lastClickTime = currentClickTime;
			}
		}
		
	}
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    class BluetoothBroadcast extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {
			devices = adapter.getBondedDevices();
			for(Iterator<BluetoothDevice> iterator=devices.iterator();iterator.hasNext();) {
    			BluetoothDevice bd = iterator.next();
    			arrayAdapter.add(bd.getName() + "\n" + bd.getAddress());
    			macAddress.add(bd.getAddress());//保存蓝牙MAC地址
    		}
//    		spinner.setAdapter(arrayAdapter);
		}
    	
    }
}
