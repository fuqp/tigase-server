/*
 * Tigase Jabber/XMPP Server
 * Copyright (C) 2004-2007 "Artur Hefczyc" <artur.hefczyc@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
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

package tigase.stats;

//~--- non-JDK imports --------------------------------------------------------

import tigase.conf.ConfiguratorAbstract;

import tigase.disco.ServiceEntity;
import tigase.disco.ServiceIdentity;

import tigase.server.AbstractComponentRegistrator;
import tigase.server.Command;
import tigase.server.Iq;
import tigase.server.Packet;
import tigase.server.ServerComponent;

import tigase.sys.ShutdownHook;
import tigase.sys.TigaseRuntime;

import tigase.util.ElementUtils;

import tigase.xml.Element;
import tigase.xml.XMLUtils;

import tigase.xmpp.BareJID;
import tigase.xmpp.JID;
import tigase.xmpp.StanzaType;

//~--- JDK imports ------------------------------------------------------------

import java.lang.management.ManagementFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.ObjectName;

//~--- classes ----------------------------------------------------------------

/**
 * Class StatisticsCollector
 *
 *
 * Created: Tue Nov 22 07:07:11 2005
 *
 * @author <a href="mailto:artur.hefczyc@tigase.org">Artur Hefczyc</a>
 * @version $Rev$
 */
public class StatisticsCollector
				extends AbstractComponentRegistrator<StatisticsContainer>
				implements ShutdownHook {

	/** Field description */
	public static final String STATISTICS_MBEAN_NAME =
		"tigase.stats:type=StatisticsProvider";
	private static final String STATS_XMLNS = "http://jabber.org/protocol/stats";
	private static final Logger log = Logger.getLogger("tigase.stats.StatisticsCollector");

	//~--- fields ---------------------------------------------------------------

	private ServiceEntity serviceEntity = null;

	// private ServiceEntity stats_modules = null;
	private Level statsLevel = Level.INFO;

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param component
	 */
	@Override
	public void componentAdded(StatisticsContainer component) {
		ServiceEntity item = serviceEntity.findNode(component.getName());

		if (item == null) {
			item = new ServiceEntity(getName(),
															 component.getName(),
															 "Component: " + component.getName());
			item.addFeatures(CMD_FEATURES);
			item.addIdentities(new ServiceIdentity("automation",
							"command-node",
							"Component: " + component.getName()));
			serviceEntity.addItems(item);
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param component
	 */
	@Override
	public void componentRemoved(StatisticsContainer component) {}

	//~--- get methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public StatisticsList getAllStats() {
		StatisticsList list = new StatisticsList(Level.ALL);

		getAllStats(list);

		return list;
	}

	/**
	 * Method description
	 *
	 *
	 * @param list
	 */
	public void getAllStats(StatisticsList list) {
		for (StatisticsContainer comp : components.values()) {
			getComponentStats(comp.getName(), list);
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @param name
	 * @param list
	 */
	public void getComponentStats(String name, StatisticsList list) {
		List<StatRecord> result = null;
		StatisticsContainer stats = components.get(name);

		if (stats != null) {
			stats.getStatistics(list);
		}
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	public List<String> getComponentsNames() {
		return new ArrayList<String>(components.keySet());
	}

	/**
	 * Method description
	 *
	 *
	 * @param from
	 *
	 * @return
	 */
	@Override
	public List<Element> getDiscoFeatures(JID from) {
		return null;
	}

	/**
	 * Method description
	 *
	 *
	 * @param node
	 * @param jid
	 * @param from
	 *
	 * @return
	 */
	@Override
	public Element getDiscoInfo(String node, JID jid, JID from) {
		if ((jid != null) && getName().equals(jid.getLocalpart()) && isAdmin(from)) {
			return serviceEntity.getDiscoInfo(node);
		}

		return null;
	}

	/**
	 * Method description
	 *
	 *
	 * @param node
	 * @param jid
	 * @param from
	 *
	 * @return
	 */
	@Override
	public List<Element> getDiscoItems(String node, JID jid, JID from) {
		if (isAdmin(from)) {
			if (getName().equals(jid.getLocalpart()) || getComponentId().equals(jid)) {
				List<Element> items = serviceEntity.getDiscoItems(node, jid.toString());

				if (log.isLoggable(Level.FINEST)) {
					log.finest("Processing discoItems for node: " + node + ", result: "
										 + ((items == null) ? null : items.toString()));
				}

				return items;
			} else {
				if (node == null) {
					Element item = serviceEntity.getDiscoItem(null,
									BareJID.toString(getName(), jid.toString()));

					if (log.isLoggable(Level.FINEST)) {
						log.finest("Processing discoItems, result: "
											 + ((item == null) ? null : item.toString()));
					}

					return Arrays.asList(item);
				} else {
					return null;
				}
			}
		}

		return null;
	}

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String getName() {
		return super.getName();
	}

	/**
	 * Method description
	 *
	 *
	 * @param component
	 *
	 * @return
	 */
	@Override
	public boolean isCorrectType(ServerComponent component) {
		return component instanceof StatisticsContainer;
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param packet
	 * @param results
	 */
	@Override
	public void processPacket(final Packet packet, final Queue<Packet> results) {
		if (!packet.isCommand() || (packet.getType() == StanzaType.result)) {
			return;
		}

		if (log.isLoggable(Level.FINEST)) {
			log.finest(packet.getCommand().name() + " command received: " + packet);
		}

		Iq iqc = (Iq) packet;

		switch (iqc.getCommand()) {
			case GETSTATS : {

				// Element statistics = new Element("statistics");
				Element iq = ElementUtils.createIqQuery(iqc.getStanzaTo(),
								iqc.getStanzaFrom(),
								StanzaType.result,
								iqc.getStanzaId(),
								STATS_XMLNS);
				Element query = iq.getChild("query");
				StatisticsList stats = getAllStats();

				if (stats != null) {
					for (StatRecord record : stats) {
						Element item = new Element("stat");

						item.addAttribute("name",
															record.getComponent() + "/" + record.getDescription());
						item.addAttribute("units", record.getUnit());
						item.addAttribute("value", record.getValue());
						query.addChild(item);
					}    // end of for ()
				}      // end of if (stats != null && stats.count() > 0)

				Packet result = Packet.packetInstance(iq, iqc.getStanzaTo(), iqc.getStanzaFrom());

				// Command.setData(result, statistics);
				results.offer(result);

				break;
			}

			case OTHER : {
				if (iqc.getStrCommand() == null) {
					return;
				}

				String nick = iqc.getTo().getLocalpart();

				if (!getName().equals(nick)) {
					return;
				}

				Command.Action action = Command.getAction(iqc);

				if (action == Command.Action.cancel) {
					Packet result = iqc.commandResult(null);

					results.offer(result);

					return;
				}

				String tmp_val = Command.getFieldValue(iqc, "Stats level");

				if (tmp_val != null) {
					statsLevel = Level.parse(tmp_val);

					if (log.isLoggable(Level.FINEST)) {
						log.finest("statsLevel parsed to: " + statsLevel.getName());
					}
				}

				StatisticsList list = new StatisticsList(statsLevel);

				if (iqc.getStrCommand().equals("stats")) {
					if (log.isLoggable(Level.FINEST)) {
						log.finest("Getting all stats for level: " + statsLevel.getName());
					}

					getAllStats(list);

					if (log.isLoggable(Level.FINEST)) {
						log.finest("All stats for level loaded: " + statsLevel.getName());
					}
				} else {
					String[] spl = iqc.getStrCommand().split("/");

					if (log.isLoggable(Level.FINEST)) {
						log.finest("Getting stats for component: " + spl[1] + ", level: "
											 + statsLevel.getName());
					}

					getComponentStats(spl[1], list);

					if (log.isLoggable(Level.FINEST)) {
						log.finest("Stats loaded for component: " + spl[1] + ", level: "
											 + statsLevel.getName());
					}
				}

				Packet result = iqc.commandResult(Command.DataType.form);

				if (list != null) {
					for (StatRecord rec : list) {
						if (rec.getType() == StatisticType.LIST) {
							Command.addFieldMultiValue(result,
																				 XMLUtils.escape(rec.getComponent() + "/"
																				 + rec.getDescription()),
																				 rec.getListValue());
						} else {
							Command.addFieldValue(result,
																		XMLUtils.escape(rec.getComponent() + "/"
																		+ rec.getDescription()),
																		XMLUtils.escape(rec.getValue()));
						}
					}
				}

				Command.addFieldValue(result,
															"Stats level",
															statsLevel.getName(),
															"Stats level",
															new String[] { Level.INFO.getName(), Level.FINE.getName(),
								Level.FINER.getName(), Level.FINEST.getName() },
															new String[] { Level.INFO.getName(), Level.FINE.getName(),
								Level.FINER.getName(), Level.FINEST.getName() });
				results.offer(result);

				if (log.isLoggable(Level.FINEST)) {
					log.finest("Returning stats result: " + result);
				}

				break;
			}

			default :
				break;
		}    // end of switch (packet.getCommand())
	}

	//~--- set methods ----------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @param name
	 */
	@Override
	public void setName(String name) {
		super.setName(name);
		serviceEntity = new ServiceEntity(name, "stats", "Server statistics");
		serviceEntity.addIdentities(new ServiceIdentity("component",
						"stats",
						"Server statistics"),
																new ServiceIdentity("automation",
						"command-node",
						"All statistics"),
																new ServiceIdentity("automation",
						"command-list",
						"Statistics retrieving commands"));
		serviceEntity.addFeatures(DEF_FEATURES);
		serviceEntity.addFeatures(CMD_FEATURES);

		try {
			StatisticsProvider sp = new StatisticsProvider(this);
			String objName = STATISTICS_MBEAN_NAME;
			ObjectName on = new ObjectName(objName);

			ManagementFactory.getPlatformMBeanServer().registerMBean(sp, on);
			ConfiguratorAbstract.putMXBean(objName, sp);
		} catch (Exception ex) {
			log.log(Level.SEVERE, "Can not install Statistics MXBean: ", ex);
		}

		TigaseRuntime.getTigaseRuntime().addShutdownHook(this);
	}

	//~--- methods --------------------------------------------------------------

	/**
	 * Method description
	 *
	 *
	 * @return
	 */
	@Override
	public String shutdown() {
		StatisticsList allStats = getAllStats();
		StringBuilder sb = new StringBuilder();

		for (StatRecord statRecord : allStats) {
			sb.append(statRecord.toString()).append('\n');
		}

		return sb.toString();
	}
}


//~ Formatted in Sun Code Convention


//~ Formatted by Jindent --- http://www.jindent.com
