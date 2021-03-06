package org.lightfish.business.heartbeat.boundary;

import java.io.IOException;
import java.io.StringWriter;
import java.util.concurrent.CopyOnWriteArraySet;
import javax.annotation.PostConstruct;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Singleton;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.websocket.OnClose;
import javax.websocket.OnOpen;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import org.lightfish.business.heartbeat.control.Serializer;
import org.lightfish.business.logging.Log;
import org.lightfish.business.servermonitoring.boundary.Severity;
import org.lightfish.business.servermonitoring.entity.Snapshot;

/**
 * @author: adam-bien.com
 */
@Singleton
@ServerEndpoint("/snapshots/")
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class SnapshotsSocket {

    @Inject
    Log LOG;

    @Inject
    Serializer serializer;

    private CopyOnWriteArraySet<Session> sessions;

    @PostConstruct
    public void init() {
        this.sessions = new CopyOnWriteArraySet<>();
    }

    @OnOpen
    public void onOpen(Session session) {
        this.sessions.add(session);
    }

    @OnClose
    public void onClose(Session session) {
        this.sessions.remove(session);
    }

    public void onNewSnapshot(@Observes @Severity(Severity.Level.HEARTBEAT) Snapshot snapshot) {
        LOG.info("SnapshotsSocket.oneNewSnapshot: " + snapshot.getId());
        for (Session session : sessions) {
            if (session != null && session.isOpen()) {
                LOG.info("Sending: " + snapshot.getId() + " to " + session.getId());
                StringWriter writer = new StringWriter();
                this.serializer.serialize(snapshot, writer);
                try {
                    final RemoteEndpoint.Basic remote = session.getBasicRemote();
                    remote.sendText(writer.getBuffer().toString());
                } catch (IOException ex) {
                    LOG.error("Problem sending Snapshot", ex);
                }
            }
        }
    }
}
