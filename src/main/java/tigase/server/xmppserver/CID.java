/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2008 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */

package tigase.server.xmppserver;

//~--- classes ----------------------------------------------------------------

/**
 * Created: Jan 7, 2010 12:51:33 PM
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class CID {
	private String fromHost = null;
	private int hash = 3;
	private String toHost = null;
	private String to_string = null;

	//~--- constructors ---------------------------------------------------------

	/**
	 * Constructs ...
	 *
	 *
	 * @param cid
	 */
	public CID(String cid) {
		String[] cid_a = cid.split("@");

		this.fromHost = cid_a[0].intern();
		this.toHost = cid_a[1].intern();
		updateToString();
	}

	/**
	 * Constructs ...
	 *
	 *
	 * @param fromHost
	 * @param toHost
	 */
	public CID(String fromHost, String toHost) {
		this.fromHost = ((fromHost == null) ? null : fromHost.intern());
		this.toHost = ((toHost == null) ? null : toHost.intern());
		updateToString();
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param o
	 *
	 * @return
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof CID) {
			return (fromHost == ((CID) o).fromHost) && (toHost == ((CID) o).toHost);
		} else {
			return false;
		}
	}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public String getFromHost() {
		return fromHost;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public String getToHost() {
		return toHost;
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public int hashCode() {
		return hash;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String toString() {
		return to_string;
	}

	private void updateToString() {
		this.to_string = "" + this.fromHost + "@" + this.toHost;
		hash = 47 * hash + ((this.fromHost != null) ? this.fromHost.hashCode() : 0);
		hash = 47 * hash + ((this.toHost != null) ? this.toHost.hashCode() : 0);
	}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com