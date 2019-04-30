package ChatRoom;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import javax.print.attribute.standard.MediaSize.Other;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.text.Highlighter.Highlight;

public class Client extends JFrame implements Runnable {
	String clientName = null;
	Vector<String> userVector = new Vector<>();
	Socket socket = null;
	PrintStream pStream = null;
	BufferedReader bReader = null;
	Client self = this;

	JList<String> jList = new JList<>(userVector);
	JScrollPane jScrollPane = new JScrollPane(jList);
	JPanel jPanel = new JPanel();
	JButton newGrChat = new JButton("新建群聊");

	HashMap<String, privateChat> priChatWindows = new HashMap<>();
	HashMap<String, groupChat> grChatWindows = new HashMap<>();

	public Client() {
		clientName = getClientName();
		try {
			Socket socket = new Socket("127.0.0.1", 9000);
			bReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			pStream = new PrintStream(socket.getOutputStream());
			pStream.println(Message.LoginMsg + "#" + clientName);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.setTitle(clientName);
		this.setSize(300, 700);
		this.setLocation(1200, 100);
		this.setVisible(true);
		this.add(jPanel);
		jPanel.setLayout(new BorderLayout());
		jPanel.add(jScrollPane, BorderLayout.CENTER);
		jPanel.add(newGrChat, BorderLayout.SOUTH);
		jPanel.setSize(300, 700);
		jScrollPane.setSize(300, 600);
		newGrChat.setSize(100, 30);
		jList.setSize(300, 600);
		jList.setFont(new Font("微软雅黑", Font.BOLD, 15));
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				pStream.println(Message.LogoutMsg + "#" + clientName);
				dispose();
			}
		});
		jList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				JList theList = (JList) e.getSource();
				if (e.getButton() == e.BUTTON1 && e.getClickCount() == 2) {
					int index = theList.locationToIndex(e.getPoint());
					if (index != -1 && !theList.getCellBounds(index, index).contains(e.getPoint())) {
						// 点击不在选项中
						// 取消选择状态
						jList.clearSelection();
					} else {
						// 新建私聊
						new privateChat(jList.getSelectedValue(), clientName, self);
					}
				} else if (e.getButton() == e.BUTTON1 && e.getClickCount() == 1) {
					int index = theList.locationToIndex(e.getPoint());
					if (index != -1 && !theList.getCellBounds(index, index).contains(e.getPoint())) {
						jList.clearSelection();
					}
				}
			}
		});
		newGrChat.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getButton() == e.BUTTON1) {
					if (jList.getSelectedValues().length < 2) {
						new JOptionPane().showMessageDialog(self, "请选择至少两个用户", "ERROR", JOptionPane.ERROR_MESSAGE);
					} else {
						String grName = getGroupName();
						setNewGrChat(grName, jList.getSelectedValues());
					}
				}
			}
		});
		new Thread(this).start();
	}

	@Override
	public void run() {
		while (true) {
			try {
				String msg = bReader.readLine();
				String[] msgs = msg.split("#");
				if (msgs[0].equals(Message.UserListMsg)) {
					UpdateList(msgs[1]);
				} else if (msgs[0].equals(Message.UserExsistMsg)) {
					ResetName();
				} else if (msgs[0].equals(Message.LogoutMsg)) {
					if (msgs[1].equals(clientName)) {
						SelfLogout();
					} else {
						OthersLogout(msgs[1]);
					}
				} else if (msgs[0].equals(Message.PrivateMsg)) {
					PrivateChatWith(msgs[2], msgs[3]);
				} else if (msgs[0].equals(Message.GroupExsistMsg)) {
					ResetGroupName(msgs[1], msgs[2]);
				} else if (msgs[0].equals(Message.GroupMsg)) {
					GroupChatIn(msgs[1], msgs[2], msgs[3]);
				} else if (msgs[0].equals(Message.UpdateGrLMsg)) {
					UpdateGrList(msgs[1], msgs[2]);
				} else if (msgs[0].equals(Message.BroadcastMsg)) {
					new JOptionPane().showMessageDialog(self, msgs[1], "系统广播", JOptionPane.PLAIN_MESSAGE);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				break;
			}

		}

	}

	public String getClientName() {
		String clientName = null;
		char[] specialChars = { '#', '@' };
		boolean flag = false;
		clientName = new JOptionPane().showInputDialog(this, "请输入用户昵称");
		while (true) {
			flag = false;
			if (clientName.length() == 0) {
				new JOptionPane().showMessageDialog(this, "昵称不能为空，请重新输入", "ERROR", JOptionPane.ERROR_MESSAGE);
			} else {
				for (char c : specialChars) {
					if (clientName.contains(String.valueOf(c))) {
						flag = true;
					}
				}
				if (flag == true) {
					new JOptionPane().showMessageDialog(this, "昵称不能包含字符" + specialChars + "，请重新输入", "ERROR",
							JOptionPane.ERROR_MESSAGE);
				} else {
					break;
				}
			}
			clientName = new JOptionPane().showInputDialog(this, "请输入用户昵称");
		}
		return clientName;
	}

	public void UpdateList(String nameList) {
		String[] names = nameList.split("@");
		userVector = new Vector<>();
		for (String name : names) {
			if (!name.equals(clientName)) {
				userVector.add(name);
			}
		}
		jList.setListData(userVector);
	}

	public void ResetName() {
		new JOptionPane().showMessageDialog(this, "昵称与其他用户重复，请重新输入", "ERROR", JOptionPane.ERROR_MESSAGE);
		clientName = getClientName();
		pStream.println(Message.LoginMsg + "#" + clientName);
		this.setTitle(clientName);
	}

	public void SelfLogout() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				dispose();
			}
		}).start();
		new JOptionPane().showMessageDialog(self, "系统强制下线，窗口将在5秒后关闭", "系统消息", JOptionPane.ERROR_MESSAGE);
		System.exit(0);
	}

	public void OthersLogout(String msg) {
		if (userVector.contains(msg)) {
			userVector.removeElement(msg);
			jList.setListData(userVector);
		}
		if (priChatWindows.containsKey(msg)) {
			new JOptionPane().showMessageDialog(priChatWindows.get(msg), msg + "已下线，聊天窗口即将关闭", "下线通知",
					JOptionPane.DEFAULT_OPTION);
			priChatWindows.get(msg).dispose();
		}
		// 处理群聊
	}

	public void PrivateChatWith(String otherName, String msg) {
		if (priChatWindows.containsKey(otherName)) {
			priChatWindows.get(otherName).outputArea.append(otherName + ":" + msg + "\n");
		} else {
			privateChat newWindow = new privateChat(otherName, clientName, this);
			newWindow.outputArea.append(otherName + ":" + msg + "\n");
		}
	}

	public void setNewGrChat(String grName, Object[] memList) {
		// 设置新群聊
		Vector<String> uVector = new Vector<>();
		String msg = Message.NewGrChatMsg + "#" + grName + "#" + clientName + "#";
		for (Object obj : memList) {
			msg = msg + (String) obj + "@";
			uVector.add((String) obj);
		}
		self.pStream.println(msg);
		grChatWindows.put(grName, new groupChat(uVector, grName, clientName, this));
	}

	public String getGroupName() {
		String groupName = null;
		char[] specialChars = { '#', '@' };
		boolean flag = false;
		groupName = new JOptionPane().showInputDialog(this, "请输入群聊名称");
		while (true) {
			flag = false;
			if (groupName.length() == 0) {
				new JOptionPane().showMessageDialog(this, "名称不能为空，请重新输入", "ERROR", JOptionPane.ERROR_MESSAGE);
			} else {
				for (char c : specialChars) {
					if (groupName.contains(String.valueOf(c))) {
						flag = true;
					}
				}
				if (flag == true) {
					new JOptionPane().showMessageDialog(this, "名称不能包含字符" + specialChars + "，请重新输入", "ERROR",
							JOptionPane.ERROR_MESSAGE);
				} else {
					break;
				}
			}
			groupName = new JOptionPane().showInputDialog(this, "请输入用户昵称");
		}
		return groupName;
	}

	public void ResetGroupName(String gn, String un) {
		groupChat temp = grChatWindows.get(gn);
		grChatWindows.remove(gn);
		new JOptionPane().showMessageDialog(this, "当前群聊已存在，请输入新的群聊名称", "ERROR", JOptionPane.ERROR_MESSAGE);
		gn = getGroupName();
		pStream.println(Message.NewGrChatMsg + "#" + gn + "#" + clientName + "#" + un);
		grChatWindows.put(gn, temp);
		temp.grChatName = gn;
		temp.setTitle(gn);
	}

	public void GroupChatIn(String gn, String sn, String msg) {
		if (grChatWindows.containsKey(gn)) {
			grChatWindows.get(gn).outputArea.append(sn + ":" + msg + "\n");
		} else {
			grChatWindows.put(gn, new groupChat(null, gn, clientName, self));
			grChatWindows.get(gn).outputArea.append(sn + ":" + msg + "\n");
		}
	}

	public void UpdateGrList(String gn, String ml) {
		Vector uVector = new Vector<>();
		String[] names = ml.split("@");
		for (String name : names) {
			if (!name.equals(clientName)) {
				uVector.add(name);
			}
		}
		System.out.println(ml);
		if (grChatWindows.containsKey(gn)) {
			System.out.println("lll");
			grChatWindows.get(gn).groupMembers = uVector;
			grChatWindows.get(gn).memList.setListData(uVector);
		}
	}

	public static void main(String[] args) {
		new Client();
	}
}

class privateChat extends JFrame {
	String otherName = null;
	String myName = null;
	Client client = null;
	JTextArea outputArea = new JTextArea(15, 30);
	JScrollPane osPane = new JScrollPane(outputArea);
	JTextArea inputArea = new JTextArea(3, 30);
	JScrollPane isPane = new JScrollPane(inputArea);
	JPanel jPanel = new JPanel();

	public privateChat(String on, String mn, Client c) {
		otherName = on;
		myName = mn;
		client = c;
		client.priChatWindows.put(otherName, this);
		this.setTitle("Chat With:" + otherName);
		this.setSize(700, 600);
		this.setLocation(500, 200);
		this.setVisible(true);
		this.add(jPanel);
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				if (client.priChatWindows.containsKey(otherName)) {
					client.priChatWindows.remove(otherName);
				}
				dispose();
			}
		});
		jPanel.setLayout(new BorderLayout());
		jPanel.add(osPane, BorderLayout.CENTER);
		jPanel.add(isPane, BorderLayout.SOUTH);
		osPane.setSize(700, 500);
		isPane.setSize(700, 100);
		outputArea.setEditable(false);
		outputArea.setFont(new Font("微软雅黑", Font.BOLD, 20));
		outputArea.setLineWrap(true);
		inputArea.setFont(new Font("微软雅黑", Font.BOLD, 20));
		inputArea.setLineWrap(true);
		inputArea.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent e) {
				if (e.getKeyChar() == e.VK_ENTER && inputArea.getText().length() != 0) {
					String input = inputArea.getText();
					input = input.replaceAll("\n", "");
					if (input.equals("")) {
						inputArea.setText("");
						return;
					}
					String tempMsg = Message.PrivateMsg + "#" + otherName + "#" + myName + "#" + input;
					client.pStream.println(tempMsg);
					outputArea.append(myName + ":" + input + "\n");
					inputArea.setText("");
				}
			}
		});
	}
}

class groupChat extends JFrame {
	Vector groupMembers = new Vector<>();
	String grChatName = null;
	String myName = null;
	Client client = null;
	JTextArea outputArea = new JTextArea(15, 25);
	JScrollPane osPane = new JScrollPane(outputArea);
	JTextArea inputArea = new JTextArea(3, 25);
	JScrollPane isPane = new JScrollPane(inputArea);
	JList<String> memList = new JList<>(groupMembers);
	JScrollPane listPane = new JScrollPane(memList);
	JPanel jPanel = new JPanel();
	JPanel ePanel = new JPanel();
	JPanel wPanel = new JPanel();

	public groupChat(Vector<String> gm, String gcn, String mn, Client c) {
		grChatName = gcn;
		myName = mn;
		client = c;
		if (gm == null) {
			c.pStream.println(Message.UpdateGrLMsg + "#" + gcn + "#" + mn);
		} else {
			groupMembers = gm;
		}
		this.setTitle("Group Chat:" + grChatName);
		this.setSize(700, 600);
		this.setLocation(500, 200);
		this.setVisible(true);
		this.add(jPanel);
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				client.pStream.println(Message.ExitGroupChatMsg + "#" + grChatName + "#" + myName);
			}
		});
		jPanel.setLayout(new BorderLayout());
		jPanel.add(wPanel, BorderLayout.WEST);
		jPanel.add(ePanel, BorderLayout.CENTER);
		wPanel.setLayout(new BorderLayout());
		ePanel.setLayout(new GridLayout(1, 1));
		wPanel.add(osPane, BorderLayout.CENTER);
		wPanel.add(isPane, BorderLayout.SOUTH);
		ePanel.add(listPane);
		wPanel.setSize(500, 600);
		ePanel.setSize(200, 600);
		outputArea.setEditable(false);
		outputArea.setFont(new Font("微软雅黑", Font.BOLD, 20));
		outputArea.setLineWrap(true);
		inputArea.setFont(new Font("微软雅黑", Font.BOLD, 20));
		inputArea.setLineWrap(true);
		memList.setFont(new Font("微软雅黑", Font.BOLD, 15));
		memList.setListData(groupMembers);
		inputArea.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent e) {
				if (e.getKeyChar() == e.VK_ENTER && inputArea.getText().length() != 0) {
					String input = inputArea.getText();
					input = input.replaceAll("\n", "");
					if (input.equals("")) {
						inputArea.setText("");
						return;
					}
					String tempMsg = Message.GroupMsg + "#" + grChatName + "#" + myName + "#" + input;
					client.pStream.println(tempMsg);
					inputArea.setText("");
					// 发送群聊信息
				}
			}
		});
	}
}
