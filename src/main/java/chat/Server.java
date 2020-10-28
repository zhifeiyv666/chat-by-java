package chat;
import java.awt.Color;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.util.ArrayList;
import javax.swing.JFrame;

/**
 * 
 * 服务器端程序，用于中转信号，接收信号，并进行加工，将加工后的信号发送给目标客户端
 * <b>注意</b> 只能同时运行一个服务端，因为端口资源已经被使用了!,虽然第二次运行的服务器
 * 看上去有在运行，但是它并没有发挥作用，事实上，控制台还会给出报错信息：找不到端口！端口
 * 已使用！
 * <br>
 * 具体细节 ：{@link #run}
 * @author 纸飞鱼
 *
 */
public class Server extends JFrame implements Runnable{
	
	/**
	 * 程序的id，不是很懂，没什么用，好像和版本升级后的兼容性相关；
	 * 
	 */
	private static final long serialVersionUID = 1L;

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
	
	
	private DatagramSocket ds=null;
	
	/**
	 * {@code clients} 储存着当前在线的所以用户的昵称以及其ip地址；
	 * 
	 * @see Node
	 */
	private ArrayList<Node>clients=new ArrayList<Node>();
	public Server() throws Exception{
		this.setTitle("服务端");
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setBackground(Color.yellow);
		this.setSize(400,300);
		this.setLocation(500, 200);
		this.setVisible(true);
		try {
			ds=new DatagramSocket(9999);
			new Thread(this).start();
		}catch(Exception ex) {
			ex.printStackTrace();
		}
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * <br><br>在客户端中，该方法主要用于接收套接字，然后根据其信号头将相应的
	 * 套接字进行正确的处理，
	 * <br>
	 * 对于信号是{@ code LOGIN}的信息，{@code run}会将其中的用户信息和ip地址
	 * 添加到{@code clients}中，并把这个登陆信息发送到所有在线的客户端中；除此
	 * 之外，还会将当前在线的所以客户端的用户昵称信息加工成一个以{@code USERLIST}
	 * 为信号头的套接字，并发送到所有的客户端中；
	 * <br>对于信号是{@code LOGOUT}的信号，{@code run}会从{@code clients}
	 * 中移除对应的信号，并把这个登出信号发送给所有在线的客户端中；此外，还会将当前
	 * 在线的所有客户的用户昵称加工成一个以{@USERLIST}为信号头的套接字，并发送至
	 * 所有在线的客户端中；
	 * <br>对于信号是{@code CHAT}的信息，{@code run}方法会根据发出和接收的用户
	 * 去查找当前在线用户，并发送到正确的客户端中，值得一提的是，如果最后一个字段与
	 * {@code ALL}一致，那么所有在线的客户端都将接收到这个信息；若两个用户字段
	 * 的值一致，那么只会发送一条消息，给该客户端（其他消息总是最少有两个接收客户端）；
	 * 
	 * <br><br><br>
	 */
	@Override
	public void run() {
		try {
			while(true) {
				//接收套接字消息；
				byte[] data=new byte[225];
				DatagramPacket dp=new DatagramPacket(data,data.length);
				ds.receive(dp);
				
				byte[] bt = dp.getData();
				int type = Type(bt);//接收成功后先找出指导行为的信号字段
				
				/*
				 *     由于组装、接收套接字的过程中有空间浪费现象，故转换成字符串后应当
				 *  去除两端的空白字符，否则影响后续判断 ，因此使用trim方法；
				 */
				String message = (new String(bt)).trim();
				
				//提取除信号头外的其他信息
				message = message.substring(1,message.length());
				
				//根据信号头来确定行为
				if(type==LOGIN) {
					String nickName = message;
					clients.add(new Node(dp.getSocketAddress(),nickName));
					
					String messageWithFlag = String.valueOf(LOGIN) + nickName;
					bt = messageWithFlag.getBytes();
					byte[] userMessage = (USERLIST+getUserMessage()).getBytes();
					for(Node i : clients) {
						DatagramPacket  datagramPacket = new DatagramPacket(bt,bt.length,i.getSocketAddress());
						ds.send(datagramPacket);
						
						DatagramPacket user = new DatagramPacket(userMessage,userMessage.length,i.getSocketAddress());
						ds.send(user);
					}
				}else if(type==CHAT) {
					String[] nameMessageName = message.split(SPLIT);
					byte[] b = (CHAT+message).getBytes();
					if(nameMessageName[2].trim().equals(ALL)) {
						sendToAll(new DatagramPacket(b,b.length));
					}else {
						for(Node i : clients) {
							if(i.getName().equals(nameMessageName[0].trim())) {
								DatagramPacket  datagramPacket = new DatagramPacket(b,b.length,i.getSocketAddress());
								ds.send(datagramPacket);
							}
							if(i.getName().equals(nameMessageName[2].trim())) {
								DatagramPacket  datagramPacket = new DatagramPacket(b,b.length,i.getSocketAddress());
								ds.send(datagramPacket);
							}
						}
					}
				}else if(type==LOGOUT) {
					String nickName = message;
					String messageWithFlag = String.valueOf(LOGOUT) + nickName;
					//delete
					for(Node i : clients) {
						if(i.getName().equals(nickName)) {
							clients.remove(i);
							break;
						}
					}
					byte[] userMessage = (USERLIST+getUserMessage()).getBytes();
					bt = messageWithFlag.getBytes();
					for(Node i : clients) {
						DatagramPacket  datagramPacket = new DatagramPacket(bt,bt.length,i.getSocketAddress());
						ds.send(datagramPacket);
						DatagramPacket user = new DatagramPacket(userMessage,userMessage.length,i.getSocketAddress());
						ds.send(user);
					}
				}
			}
		}catch(Exception ex) {
			ex.printStackTrace();
		}
	}
	
	
	/**
	 * 将dp中的套接字数据发送给所有在线的客户端
	 * @param dp
	 * @throws Exception
	 */
	public void sendToAll(DatagramPacket dp)throws Exception{
		for(Node sa:clients) {
			DatagramPacket datagram=
					new DatagramPacket(dp.getData(),dp.getLength(),sa.getSocketAddress());
			ds.send(datagram);
			System.out.println(new String(datagram.getData()));
			//System.out.println("服务端"+sa.getName()+datagram.getSocketAddress()+new String(dp.getData()));
		}
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
	
	/**
	 * 将当前在线用户昵称连接成一个字符串，以{@code SPLIT} 作为分隔符
	 * @return 一个包含所有在线客户端的字符串
	 */
	public String getUserMessage() {
		String ans = new String();
		for(Node i : clients) {
			ans+=i.getName()+SPLIT;
		}
		
		return ans;
	}
	
	public static void main(String[] args) throws Exception{
		Server server=new Server();
	}
}