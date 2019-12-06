package chatServer;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.HashMap;
import java.util.Map;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

class ClientList extends Thread{
	//用户列表控制
	DatagramPacket p_mul;//组播报文
	DatagramPacket pin;//接收客户端的报文
	String str;//报文原始字符串(含协议参数)
	String msg;//用户发送的消息内容
	int port_private;//私聊对应的目的端口
	InetAddress address_private;//私聊对应的目的地址
	MulticastSocket socket_mul;//组播socket
	DatagramSocket socket_private;//私聊报文
	InetAddress ia_mul;//本地组播地址
	InetAddress ia_private;//私聊本地地址
	String type, name, value, list_send = null;//定义协议参数/消息类型
	Map<String, String> list = new HashMap<String,String>();//存储用户列表
	
	public void run() {
		try {
			socket_mul = new MulticastSocket(7000); 
			ia_private = InetAddress.getByName("127.0.0.1");
			socket_private = new DatagramSocket(9000,ia_private);
			ia_mul = InetAddress.getByName("225.0.1.2");
			socket_mul.joinGroup(ia_mul);
			while (true) {
				list_send = null;
				pin = new DatagramPacket(new byte[512], 512);
				socket_private.receive(pin);//接收用户消息，格式("name|ip|port")
				str = new String(pin.getData(), 0, pin.getLength());
				String[] sou = str.split("\\|");//对str进行划分,"\\"为转义符
				name = sou[0];
				value = sou[1];//格式("ip|port")
				list.put(name, value);//把用户名以及对于的ip、port存储到map中
				//将用户列表构造成字符串，格式("name";"name")
				for(String key:list.keySet()) {
					if(list_send==null) {
						list_send = key + ";";
					}else {
						list_send += key + ";";
					}
				}
				ChatRoomServer.items.add(name);//修改用户列表
				
				p_mul = new DatagramPacket(list_send.getBytes(), list_send.getBytes().length, ia_mul, 7000);
				socket_mul.send(p_mul);//组播下发更新用户列表
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
}
class MsgServer extends Thread {
	DatagramPacket p_mul;//组播报文
	DatagramPacket pin;//接收客户端的报文
	String str;//报文原始字符串(含协议参数)
	String msg;//用户发送的消息内容
	int port_private;//私聊对应的目的端口
	InetAddress address_private;//私聊对应的目的地址
	MulticastSocket socket_mul;//组播socket
	DatagramSocket socket_private;//私聊报文
	InetAddress ia_mul;//本地组播地址
	InetAddress ia_private;//私聊本地地址
	String type = null;//定义协议参数/消息类型
	public void run() {
		try {
			socket_mul = new MulticastSocket(4000); 
			ia_private = InetAddress.getByName("127.0.0.1");
			socket_private = new DatagramSocket(8000,ia_private);
			ia_mul = InetAddress.getByName("225.0.1.1");
			socket_mul.joinGroup(ia_mul);
			while (true) {
				pin = new DatagramPacket(new byte[512], 512);
				socket_private.receive(pin);
				str = new String(pin.getData(), 0, pin.getLength());
				String[] sou = str.split("\\|");//对str进行划分,"\\"为转义符
				type = sou[0];//
				msg = sou[1];//
				if(type.equals("1")){//参数为1进行组播
					multicastServer();
				}else if(type.equals("2")){//参数为2进行私聊
					address_private = InetAddress.getByName(sou[2]);
					port_private = Integer.parseInt(sou[3]);
					privateServer(address_private, port_private);
				}
				
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	public void multicastServer() {
		//发送组播消息
		try {
			p_mul = new DatagramPacket(msg.getBytes(), msg.getBytes().length, ia_mul, 4000);
			socket_mul.send(p_mul);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void privateServer(InetAddress address,int port) {
		//发送私聊消息
		try {
			DatagramPacket pout = new DatagramPacket(msg.getBytes(), msg.getBytes().length,address,
					port);
			socket_private.send(pout);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

public class ChatRoomServer extends Application {
	TextArea ta_Speaking;//聊天框
	GridPane ChatRoom;//聊天室面板
	BorderPane IPpane;
	BorderPane box;//拼合界面
	ListView<String> IPlist; //显示在线用户
	TitledPane pane_ip,pane_send;
	static ObservableList<String> items;//用户名单内容
	MsgServer msgServer = new MsgServer();//创建信息处理线程
	ClientList clientlist = new ClientList();
	@Override
	public void start(Stage primaryStage) throws Exception {
		//设置聊天界面
		ChatRoom = new GridPane();//聊天界面
		ChatRoom.setHgap(10);//设置宽间距
		ChatRoom.setVgap(10);//设置高间距
		
		ta_Speaking = new TextArea("");//聊天框
		ta_Speaking.setPrefHeight(600);//聊天框高度
		
		ChatRoom.add(ta_Speaking,0,0);//聊天框在0列0行
		ChatRoom.setPrefSize(655, 561);//聊天界面宽750高531
		
		//设置用户名单界面
		IPpane = new BorderPane();//在线用户名单界面
		IPlist = new ListView<>();//用户名单列表
		items =FXCollections.observableArrayList ();//用户名单内容
		IPlist.setItems(items);//显示内容
	    IPpane.setLeft(IPlist);//列表显示在界面左侧
	    IPpane.setPrefWidth(100);//用户列表界面宽度
	    IPpane.setPrefHeight(561);//用户列表界面高度
	    
		
		//拼合界面 
		pane_ip = new TitledPane("OnLine",IPpane);//用户列表界面名
		pane_ip.setCollapsible(false);//收放功能为否
		
		pane_send = new TitledPane("Chat",ChatRoom);//聊天界面名
		pane_send.setCollapsible(false);//收放功能为否
		
		box = new BorderPane();//一个装两个界面的盒子
		box.setLeft(pane_ip);//左边是用户列表
		box.setRight(pane_send);//右边聊天界面
		Scene scene = new Scene(box);
		primaryStage.setScene(scene);
		primaryStage.setTitle("ChatServer");//总界面名
		primaryStage.show();//显示界面
	
		msgServer.start();
		clientlist.start();
	}
	
	
	public static void main(String[] args) {
		
		 Application.launch();
	}
}
