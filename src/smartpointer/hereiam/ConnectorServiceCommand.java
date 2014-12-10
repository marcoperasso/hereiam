package smartpointer.hereiam;

import java.io.Serializable;
enum CommandType { START_SENDING_MY_POSITION, STOP_SENDING_MY_POSITION, START_RECEIVING_USER_POSITION, STOP_RECEIVING_USER_POSITION}
public class ConnectorServiceCommand implements Serializable {
	User user;
	boolean silent;
	CommandType type;
	int timeout;

	public ConnectorServiceCommand(User user, boolean silent,CommandType type, int timeout) {
		this.user = user;
		this.silent = silent;
		this.type = type;
		this.timeout = timeout;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1159533083686192524L;

}
