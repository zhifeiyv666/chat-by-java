package chat;

import java.net.SocketAddress;


/**
 * 一个耦合了客户端ip地址和对应的昵称的类
 * @author 纸飞鱼
 *
 */
public class Node {
	private SocketAddress s;
	private  String nickName;
	
	public Node(SocketAddress s, String nickName) {
		this.s = s;
		this.nickName = nickName;
	}
	
	public SocketAddress getSocketAddress() {
		return s;
	}
	
	public void setName(String a) {
		nickName = a;
	}
	public String getName() {
		return nickName;
	}
	
	/**
	 * 为了能够用{@code ArrayList<T>} 的{@code contains()} 方法，根据ip
	 * 来判断是否属于同一个客户端；
	 * 值得注意的是，这意味着在处理请求时，由于不同客户端可能有一样的昵称，所
	 * 以可能出现想发给叫同名的其中一人，但服务器并不能理解，因此会发送给两个人；
	 * 如何改进？to be continued...
	 */
	@Override
	public boolean equals(Object x) {
		if(x instanceof Node) {
			return s.equals(((Node) x).s);
		}
		return false;
	}
	
}
