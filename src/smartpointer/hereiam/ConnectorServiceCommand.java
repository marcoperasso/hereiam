package smartpointer.hereiam;

import java.io.Serializable;

public class ConnectorServiceCommand implements Serializable {
	User user;
	boolean connect;
	boolean silent;

	public ConnectorServiceCommand(User user, boolean connect, boolean silent) {
		this.user = user;
		this.connect = connect;
		this.silent = silent;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1159533083686192524L;

}
