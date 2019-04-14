package ChatRoom;

import java.net.Socket;

public class User {
	int userNumber;
	String userName;
	Socket socket = null;

	public User(int unum, String uname, Socket s) {
		userNumber = unum;
		userName = uname;
		socket = s;
	}

	public int getUserNumber() {
		return userNumber;
	}

	public String getUserName() {
		return userName;
	}

	public Socket getSocket() {
		return socket;
	}

	public void setUserNumber(int userNumber) {
		this.userNumber = userNumber;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public void setSocket(Socket socket) {
		this.socket = socket;
	}
}
