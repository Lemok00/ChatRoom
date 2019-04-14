package ChatRoom;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class Server extends JFrame implements Runnable {
	ServerSocket serverSocket = null;
	HashMap<String, ChatThread> userMap = new HashMap<>();
	HashMap<String, ArrayList<String>> groupMap = new HashMap<>();
	JTextArea jTextArea = new JTextArea();
	JScrollPane jScrollPane = new JScrollPane(jTextArea);

	public Server() {
		this.setTitle("·þÎñÆ÷");
		this.setSize(500, 300);
		this.setLocation(500, 500);
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
		this.setVisible(true);
		this.add(jScrollPane);
		jTextArea.setEditable(false);
		new Thread(this).start();
	}

	@Override
	public void run() {
		try {
			serverSocket = new ServerSocket(9000);
			while (true) {
				Socket socket = serverSocket.accept();
				new ChatThread(this, socket);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		new Server();
	}
}

class ChatThread implements Runnable {
	Server server = null;
	Socket socket = null;
	BufferedReader bReader = null;
	PrintStream pStream = null;

	public ChatThread(Server server, Socket socket) {
		try {
			this.server = server;
			this.socket = socket;
			bReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			pStream = new PrintStream(socket.getOutputStream());
		} catch (Exception e) {
			// TODO: handle exception
		}
		new Thread(this).start();
	}

	@Override
	public void run() {
		while (true) {
			try {
				String msg = bReader.readLine();
				server.jTextArea.append(msg + "\n");
				String[] msgs = msg.split("#");
				if (msgs[0].equals(Message.LoginMsg)) {
					userLogin(msgs[1]);
				} else if (msgs[0].equals(Message.LogoutMsg)) {
					userLogout(msgs[1]);
					this.socket.close();
					break;
				} else if (msgs[0].equals(Message.PrivateMsg)) {
					if (server.userMap.containsKey(msgs[1])) {
						server.userMap.get(msgs[1]).pStream.println(msg);
					}
				} else if (msgs[0].equals(Message.NewGrChatMsg)) {
					setNewChatRoom(msgs[1], msgs[2], msgs[3]);
				}else if (msgs[0].equals(Message.GroupMsg)) {
					if(server.groupMap.containsKey(msgs[1])) {
						for(String mem:server.groupMap.get(msgs[1])) {
							server.userMap.get(mem).pStream.println(msg);
						}
					}
				}else if (msgs[0].equals(Message.UpdateGrLMsg)) {
					if(server.groupMap.containsKey(msgs[1])) {
						String s = msgs[0]+"#"+msgs[1]+"#";
						for(String name:server.groupMap.get(msgs[1])) {
							s=s+name+"@";
						}
						server.userMap.get(msgs[2]).pStream.println(s);
					}
				}else if (msgs[0].equals(Message.ExitGroupChatMsg)) {
					if(server.groupMap.containsKey(msgs[1])) {
						server.groupMap.get(msgs[1]).remove(msgs[2]);
						String s = Message.UpdateGrLMsg+"#"+msgs[1]+"#";
						for(String name:server.groupMap.get(msgs[1])) {
							s=s+name+"@";
						}
						for(String name:server.groupMap.get(msgs[1])) {
							server.userMap.get(name).pStream.println(s);
						}
					}
				}
			} catch (Exception e) {
				// TODO: handle exception
			}

		}
	}

	public void userLogin(String msg) {
		if (server.userMap.containsKey(msg)) {
			pStream.println(Message.UserExsistMsg);
			return;
		} else {
			server.userMap.put(msg, this);
			String userList = Message.UserListMsg + "#";
			for (String name : server.userMap.keySet()) {
				userList = userList + name + "@";
			}
			for (ChatThread ct : server.userMap.values()) {
				ct.pStream.println(userList);
			}
		}
	}

	public void userLogout(String msg) {
		if (!server.userMap.containsKey(msg)) {
			return;
		} else {
			for (ChatThread ct : server.userMap.values()) {
				ct.pStream.println(Message.LogoutMsg + "#" + msg);
			}
			server.userMap.remove(msg);
			for(String s:server.groupMap.keySet()) {
				ArrayList<String> a=server.groupMap.get(s);
				if(a.contains(msg)) {
					a.remove(msg);
					if(a.isEmpty()) {
						server.groupMap.remove(s);
					}
				}
			}
		}
	}

	private void setNewChatRoom(String gn, String stern, String un) {
		if (un.length() == 0)
			return;
		if (server.groupMap.containsKey(gn)) {
			server.userMap.get(stern).pStream.println(Message.GroupExsistMsg + "#" + gn + "#" + un);
			return;
		}
		String[] names = un.split("@");

		ArrayList<String> arrayList = new ArrayList<>();
		arrayList.add(stern);
		for (String name : names) {
			arrayList.add(name);
		}
		server.groupMap.put(gn, arrayList);
	}
}
