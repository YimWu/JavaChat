package chatClient;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

class List extends Thread {
	//向服务器上传个人信息、接收服务器下发用户列表
	DatagramSocket socket_list;//本地socket
	SocketAddress serverAddr_list;// 指定服务器地址

	public void run() {
		try {
			MulticastSocket socket = new MulticastSocket(7000);
			serverAddr_list = new InetSocketAddress("127.0.0.1", 9000);// 指定服务器地址
			socket_list = new DatagramSocket(0);//创建本地socket并由系统随机分配端口
			socket_list.connect(serverAddr_list);// 绑定服务器地址，之后发送的DatagramPacket的目的地址都为该地址
			InetAddress ia = InetAddress.getByName("225.0.1.2");//组播地址
			socket.joinGroup(ia);
			//构造个人信息，格式("name|ip|port")
			String msg = "F" + "|" + ChatRoomClient.s.getLocalAddress().toString() + "|" + ChatRoomClient.s.getLocalPort();
			DatagramPacket pout = new DatagramPacket(msg.getBytes(), msg.getBytes().length, serverAddr_list);
			socket_list.send(pout);//上传个人信息
			while (true) {
				DatagramPacket packet = new DatagramPacket(new byte[512], 512);
				socket.receive(packet);//接收服务器下发的用户列表，格式("name";"name")
				String str = new String(packet.getData(), 0, packet.getLength());
				String[] sou = str.split(";");
				ChatRoomClient.items.clear();
				//将分离后的字符串段中非";"的加入都用户列表中
				for (String s:sou) {
					if(!s.equals(";")) {
						ChatRoomClient.items.add(s);
					}
				}
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
}
class MulticastClient extends Thread{
	//监听组播消息
	public void run() {
		try (MulticastSocket socket = new MulticastSocket(4000)) {

			InetAddress ia = InetAddress.getByName("225.0.1.1");
			socket.joinGroup(ia);

			while (true) {
				DatagramPacket packet = new DatagramPacket(new byte[512], 512);
				socket.receive(packet);
				String msg = new String(packet.getData(), 0, packet.getLength());
				System.out.println(msg);
				if (msg.equals("bye")) {
					break;
				}
			}
			socket.leaveGroup(ia);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
}

class PrivateClient extends Thread{
	//监听私聊内容
	public void run() {
		try{
			while (true) {
				DatagramPacket pin = new DatagramPacket(new byte[512],512);
				ChatRoomClient.s.receive(pin);
				System.out.println(new String(pin.getData(),0, pin.getLength()));
			}
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
}

public class ChatRoomClient extends Application {
	TextArea ta_Input;//输入消息
	TextArea ta_Speaking;//聊天框
	Button btn_Send,btn_History,btn_Picture,btn_File;//发送按钮，查看历史记录按钮，发送图片按钮，发送文件按钮
	GridPane ChatRoom,Send,Btn;//聊天室面板
	BorderPane IPpane;
	BorderPane box,box1,box2;//拼合界面
	ListView<String> IPlist; //显示在线用户
	TitledPane pane_ip,pane_send;
	static DatagramSocket s;// 由系统随机分配一个可用端口，也可以像服务端一样绑定自己IP与端口
	SocketAddress serverAddr;//定义服务器地址
	String type = "2"; //定义协议参数/消息类型
	MulticastClient multicastclient = new MulticastClient(); //创建组播线程
	PrivateClient privateclient = new PrivateClient();//创建udp线程
	List list = new List();
	static ObservableList<String> items;//用户列表
	@Override
	
	public void start(Stage primaryStage) throws Exception {
		//设置聊天界面
		ChatRoom = new GridPane();//聊天界面
		ChatRoom.setHgap(10);//设置宽间距
		ChatRoom.setVgap(10);//设置高间距
		
		ta_Speaking = new TextArea("");//聊天框
		ta_Speaking.setPrefHeight(450);//聊天框高度
		ta_Speaking.setPrefWidth(648);
		
		ChatRoom.add(ta_Speaking,0,0);//聊天框在0列0行
		ChatRoom.setPrefSize(655, 455);//聊天界面宽750高531
		
		//设置发送界面
		Send = new GridPane();//聊天界面
		Send.setHgap(10);
		btn_Send = new Button("Send");//发送按钮
		btn_Send.setOnAction(this::btnSendHandler);
		
		ta_Input = new TextArea("");//输入框
		ta_Input.setPrefWidth(648);//输入框宽度
		ta_Input.setPrefHeight(10);//输入框高度
		
		Send.add(btn_Send,1,0);//发送按钮在1列1行
		Send.add(ta_Input,0,0);//输入框在0列1行
		
		//设置功能按钮界面
		Btn = new GridPane();//按钮
		Btn.setVgap(10);
		Btn.setHgap(10);
		Btn.setPrefHeight(35);
		
		btn_History = new Button("History");//历史记录按钮
		btn_History.setOnAction(this::btnHistoryHandler);
		
		btn_Picture = new Button("Picture");//发送图片按钮
		btn_Picture.setOnAction(this::btnPictureHandler);
		
		btn_File = new Button("File");//发送文件按钮
		btn_File.setOnAction(this::btnFileHandler);
		
		Btn.add(btn_Picture,0,0);
		Btn.add(btn_File,1,0);
		Btn.add(btn_History,2,0);
		
		//设置用户名单界面
		IPpane = new BorderPane();//在线用户名单界面
		IPlist = new ListView<>();//用户名单列表
		items =FXCollections.observableArrayList ();//用户名单内容
		IPlist.setItems(items);//显示内容
	    IPpane.setLeft(IPlist);//列表显示在界面左侧
	    IPpane.setPrefWidth(100);//用户列表界面宽度
	    IPpane.setPrefHeight(561);//用户列表界面高度
	    
		
		//拼合界面
	    box1 = new BorderPane();
	    box1.setTop(ChatRoom);
	    box1.setCenter(Btn);
	    box1.setBottom(Send);
	    
	    
		pane_ip = new TitledPane("OnLine",IPpane);//用户列表界面名
		pane_ip.setCollapsible(false);//收放功能为否
		
		pane_send = new TitledPane("Chat",box1);//聊天界面名
		pane_send.setCollapsible(false);//收放功能为否
		
		box = new BorderPane();//一个装两个界面的盒子
		box.setLeft(pane_ip);//左边是用户列表
		box.setRight(pane_send);//右边聊天界面
		Scene scene = new Scene(box);
		primaryStage.setScene(scene);
		primaryStage.setTitle("ChatRoom");//总界面名
		primaryStage.show();//显示界面

		s = new DatagramSocket(0);// 创建socket并由系统随机分配一个可用端口
		serverAddr = new InetSocketAddress("127.0.0.1", 8000);// 指定服务器地址
		s.connect(serverAddr);//绑定服务器地址，之后发送的DatagramPacket的目的地址都为该地址
		multicastclient.start();
		privateclient.start();
		list.start();
		System.out.println(s.getLocalAddress().toString()+"|"+s.getLocalPort());//测试
	}

	public void btnSendHandler(ActionEvent event){
		try {
			String msg = ta_Input.getText();//获取输入框内容
			String msg_send = type + "|" + msg + "|" + "127.0.0.1|52331";//构造聊天字符串
			DatagramPacket pout = new DatagramPacket(msg_send.getBytes(), msg_send.getBytes().length, serverAddr);
			s.send(pout);//发送DatagramPacket
		} catch (Exception e) {
			// TODO: handle exception
		}
		
	}
	
	public void btnHistoryHandler(ActionEvent event){
		
		
	}
	
	public void btnPictureHandler(ActionEvent event){
	
	
	}
	
	public void btnFileHandler(ActionEvent event){
	
	
	}
	
	
	
	public static void main(String[] args) {
		
		 Application.launch();
	}
}
