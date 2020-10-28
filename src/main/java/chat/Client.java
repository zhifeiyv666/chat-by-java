
package chat;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.*;
import javax.swing.*;

/**
 * 客户端，用于创建客户、发送信号、接收信号并回显；
 * 与服务器端不同，可以同时开启多个客户端；因为初始端口是从空闲端口
 * 中随机注册的，所有多个客户端不会发生冲突。
 * @author 纸飞鱼
 *
 */
public class Client extends JFrame implements ActionListener,Runnable{
	
	/**
	 * 一个信号，用于套接字数据字段开头，长度为1，用于指导如何处理该套接字数据，
	 * 当使用{@code LOGIN}时，意味着这个套接字发送或接收的是登陆信息，该信息
	 * 转换成字符串后有如下格式：第一位是信号位，剩下的是登陆上服务器的用户；
	 * 
	 * 它的值是 {@code int}型的数字0；
	 * @see #LOGOUT
	 * @see #CHAT
	 * @see #USERLIST
	 * 
	 * @see Server
	 * @see Client
	 */
	public static final int LOGIN = 0;
	
	/**
	 * 一个信号，用于套接字数据字段开头，长度为1，用于指导如何处理该套接字数据，
	 * 当使用{@code LOGOUT}时，意味着这个套接字发送或接收的是登出信息，该信息
	 * 转换成字符串后有如下格式：第一位是信号位，剩下的是登出服务器的用户；
	 * 
	 * 它的值是 {@code int}型的数字1；
	 * @see #LOGIN
	 * @see #CHAT
	 * @see #USERLIST
	 * 
	 * @see Server
	 * @see Client
	 */
	public static final int LOGOUT = 1;
	
	/**
	 * 一个信号，用于套接字数据字段开头，长度为1，用于指导如何处理该套接字数据，
	 * 当使用{@code CHAT}时，意味着这个套接字发送或接收的是聊天信息，该信息
	 * 转换成字符串后有如下格式：第一位是信号位，剩下的字符串分成五个部分（也有
	 * 可能是六个部分）：发送该消息的用户 、 一个分隔符{@code SPLIT}、发送的消息
	 * 、 一个分隔符{@code SPLIT}、接收该消息的用户（后面可能还有 一个分隔符
	 * {@code SPLIT}，分割符的作用是为了能够快速还原相应字段；
	 * 
	 * 它的值是 {@code int}型的数字2；
	 * @see #LOGIN
	 * @see #LOGOUT
	 * @see #USERLIST
	 * @see #SPLIT
	 * @see Server
	 * @see Client
	 */
	public static final int CHAT = 2;
	
	/**
	 * 一个信号，用于套接字数据字段开头，长度为1，用于指导如何处理该套接字数据，
	 * 当使用{@code CHAT}时，意味着这个套接字发送或接收的是目前在线的用户列表
	 * ，该信息转换成字符串后有如下格式：第一位是信号位，剩下的字符串由用户名和
	 * 分隔符{@code SPLIT}交叉组成,分割符的作用是为了能够快速还原相应字段；
	*
	 * 它的值是 {@code int}型的数字3；
	 * @see #LOGIN
	 * @see #LOGOUT
	 * @see #CHAT
	 * @see #SPLIT
	 * @see Server
	 * @see Client
	 */
	public static final int USERLIST = 3;
	
	/**
	 * 一个字符串常量，通常是在{@code CHAT}中使用它，
	 * 这通常意味着所有的在线用户，其值为{@code "All"}
	 * 
	 * @see #SPLIT
	 */
	public static final String ALL = "All";
	
	/**
	 * 一个字符串常量，用于分割不同字段；
	 * 其值为{@code "===="},这个方法一般用于分割用户和消息以及用户之间
	 * 这意味着很有可能会出现这样的bug:用户的昵称就是“====”或者用户想发的消
	 * 息就恰好包含{@code “====”}这种尴尬的局面，因此，请务必不要使用连续的
	 * 四个等号来组成昵称或者发送连续的四个等号给对方。
	 */
	public static final String SPLIT = "====";
	
	/**
	 * 显示聊天记录的文本域
	 */
	private JTextArea taMsg=new JTextArea("下面是消息\n");
	
	/**
	 * 输入的文本框
	 */
	private JTextField tfMsg=new JTextField();
	private DatagramSocket ds=null;
	
	/**
	 * 昵称
	 */
	private String nickName=null;
	
	/**
	 * 复选框，用于选择消息的接收者
	 */
	private JComboBox box;
	
	/**
	 * 用于记录从服务器上接收的{@code USERLIST}型的套接字数据中储存的所有在线
	 * 用户，以便正确布置复选框；
	 */
	private String[] nickNameList;
	
	/**
	 * 为了实现关闭客户端就登出，需要给该窗口注册一个窗口事件，这里继承了窗口
	 * 监听适配器，增加了一个属性，目的是破除内部类无法使用外部类的属性问题；
	 * 之所以要继承，也是出于这个原因
	 * @author 纸飞鱼
	 *
	 */
	private class myWindowAdapter extends WindowAdapter{
		DatagramSocket ds;
		public myWindowAdapter(DatagramSocket ds) {
			this.ds = ds;
		}
	}
	
	public Client(){
		this.setTitle("客户端");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.add(taMsg,BorderLayout.CENTER);
		tfMsg.setBackground(Color.yellow);
		this.add(tfMsg,BorderLayout.SOUTH);
		tfMsg.addActionListener(this);
		this.setSize(280,400);
		box = new JComboBox();
		box.addItem("All");
		this.add(box,BorderLayout.NORTH);
		this.setVisible(true);
		nickName=JOptionPane.showInputDialog("请输入");
		
		try{
			// 创建连接，连接到localhost:9999
			ds=new DatagramSocket();
			InetAddress add=InetAddress.getByName("127.0.0.1");
			ds.connect(add,9999);
			
			//启动客户端时包装一个{@codeLOGIN}信号发送给服务器
			String msg=String.valueOf(LOGIN)+nickName;
			byte[] data=msg.getBytes();
			DatagramPacket dp=new DatagramPacket(data,data.length);
			ds.send(dp);
			
			/*
			 * 注册窗口事件监听器；由于只需要在关闭的时候发送登出信号，故只重写
			 * {@code WindowClosing()}方法；该方法先包装一个{@code LOGOUT}信号
			 * 然后发送给服务器；
			 */
			this.addWindowListener(new myWindowAdapter(ds){
				public void windowClosing(WindowEvent e) {
					String msg=String.valueOf(LOGOUT)+nickName;
					byte[] data=msg.getBytes();
					DatagramPacket dp=new DatagramPacket(data,data.length);
					try {
						ds.send(dp);
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			});
			
			new Thread(this).start();
		}catch(Exception ex){}	
	}
	public void run(){
		try{
			while(true){
				byte[] data=new byte[255];
				DatagramPacket dp=new DatagramPacket(data,data.length);
				ds.receive(dp); 
				byte[] bt = dp.getData();
				int type = Type(bt);
				String message = (new String(bt));
				message = message.substring(1,message.length());
				if(type == LOGIN) {
					taMsg.append(message+"登陆\n");
				}else if (type == LOGOUT) {
					taMsg.append(message+"登出\n");
				}else if(type ==USERLIST) {
					nickNameList = message.split(SPLIT);
					changeJComboBox();
				}else if(type == CHAT) {
					String[] nameDataName = message.split(SPLIT);
					//System.out.println("here");
					if(nameDataName[0].trim().equals(nickName.trim())) {
						if(nameDataName[2].trim().equals(ALL)) {
							taMsg.append("You said in public :" + nameDataName[1]+"\n");
						}else if(nameDataName[2].trim().equals(nickName.trim())) {
							taMsg.append("You said in secret to :" + nameDataName[1]+"\n");
						}else {
							taMsg.append("You said in secret to "+nameDataName[2]+" : " + nameDataName[1]+"\n");
						}
					}
					else {
						if(nameDataName[2].trim().equals(ALL)) {
							taMsg.append(nameDataName[0]+ " said in public :" + nameDataName[1]+"\n");
						}
						else {
							taMsg.append(nameDataName[0]+ " said in secret to you :" + nameDataName[1]+"\n");
						}
					}
				}
			}
		}catch(Exception ex){}
	}
	
	/**
	 * 重新布置下拉列表，先清空，再添加
	 */
	public void changeJComboBox() {
		box.removeAllItems();
		
		/*该方法是立即生效的含义，鼠标放在removeAllItems方法上面，发现
		 * 其说到使用该方法需调用invalidate方法；
		 */
		invalidate();
		box.addItem(ALL);
		for(String i :nickNameList) {
			if(!i.isEmpty()) {
				box.addItem(i);
			}
		}
	}
	
	/**
	 * {@inheritDoc}偷懒，直接用接口的api
	 */
	@Override
	public void actionPerformed(ActionEvent e){
		try{
			String i = box.getSelectedItem().toString();
			String msg;
			msg=String.valueOf(CHAT) + nickName + SPLIT + tfMsg.getText()+SPLIT+ i;
			//System.out.println(msg);
			byte[] data=msg.getBytes();
			DatagramPacket dp=new DatagramPacket(data,data.length);
			ds.send(dp);
			tfMsg.setText(null);
		}catch(Exception ex){}
	}
	
	/**
	 * 从接受的字节数据中提取信号头
	 * @param bt
	 * @return 信号头，必定返回 {@code LOGIN}、{@code LOGOUT}、{@code CHAT}
	 * 、{@code USERLIST} 中的一个
	 */
	public int Type(byte[] bt) {
		return Integer.parseInt(new String(bt).substring(0, 1));
	}
	public static void main(String[] args) throws Exception{
		Client client=new Client();
	}
}