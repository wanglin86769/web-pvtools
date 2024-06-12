/*
 *  Original Author: Kay Kasemir
 *  Current Author:  Lin Wang
 *
 *  Modification History
 *  2024-05-22 Replace Servlet with Spring Boot REST API
 *
 */
package pvwa.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;

import com.fasterxml.jackson.core.JsonGenerator;

import org.epics.vtype.VType;
import org.epics.vtype.Array;
import org.springframework.web.bind.annotation.CrossOrigin;

import pvwa.PVWebSocketContext;
import static pvwa.PVWebSocketContext.logger;
import static pvwa.PVWebSocketContext.json_factory;
import pvwa.ws.WebSocket;
import pvwa.ws.WebSocketPV;

/**
 * REST API to list Web Sockets and their PVs
 */
@CrossOrigin
@RestController
public class SocketController
{
    /* Return info about all sockets and their PVs */
    @GetMapping(value = "/socket", produces="application/json")
    public String getAllSockets() throws IOException
    {
        final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        final JsonGenerator g = json_factory.createGenerator(buf);
        
        g.writeStartObject();
        g.writeArrayFieldStart("sockets");
        for (final WebSocket socket : PVWebSocketContext.getSockets())
        {
            g.writeStartObject();
            g.writeStringField("id", socket.getId());
            g.writeNumberField("created", socket.getCreateTime());
            g.writeNumberField("last_client_message", socket.getLastClientMessage());
            g.writeNumberField("last_message_sent", socket.getLastMessageSent());
            g.writeNumberField("queued", socket.getQueuedMessageCount());

            g.writeArrayFieldStart("pvs");
            for (final WebSocketPV pv : socket.getPVs())
            {
                g.writeStartObject();
                g.writeStringField("name", pv.getName());
                // Add representation of value.
                final VType value = pv.getLastValue();
                if (value instanceof Array)
                {   // For arrays, show size, not actual elements
                    g.writeStringField("value", value.getClass().getName() + ", size " + ((Array) value).getSizes());
                }
                else
                    g.writeStringField("value", Objects.toString(pv.getLastValue()));
                g.writeEndObject();
            }
            g.writeEndArray();

            g.writeEndObject();
        }
        g.writeEndArray();
        g.writeEndObject();
        
        g.flush();
        return buf.toString();
    }
    
    /* Only return info about selected socket ID */
    @GetMapping(value = "/socket/{id}", produces="application/json")
    public String getSocketById(@PathVariable("id") String id) throws IOException
    {
        final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        final JsonGenerator g = json_factory.createGenerator(buf);
        
        g.writeStartObject();
        g.writeArrayFieldStart("sockets");
        for (final WebSocket socket : PVWebSocketContext.getSockets())
        {
            if (id != null  &&  !socket.getId().equals(id))
                continue;
                
            g.writeStartObject();
            g.writeStringField("id", socket.getId());
            g.writeNumberField("created", socket.getCreateTime());
            g.writeNumberField("last_client_message", socket.getLastClientMessage());
            g.writeNumberField("last_message_sent", socket.getLastMessageSent());
            g.writeNumberField("queued", socket.getQueuedMessageCount());

            g.writeArrayFieldStart("pvs");
            for (final WebSocketPV pv : socket.getPVs())
            {
                g.writeStartObject();
                g.writeStringField("name", pv.getName());
                // Add representation of value.
                final VType value = pv.getLastValue();
                if (value instanceof Array)
                {   // For arrays, show size, not actual elements
                    g.writeStringField("value", value.getClass().getName() + ", size " + ((Array) value).getSizes());
                }
                else
                    g.writeStringField("value", Objects.toString(pv.getLastValue()));
                g.writeEndObject();
            }
            g.writeEndArray();

            g.writeEndObject();
        }
        g.writeEndArray();
        g.writeEndObject();
        
        g.flush();
        return buf.toString();
    }
    
    /* Close socket and its PVs */
    @DeleteMapping(value = "/socket/{id}", produces="application/json")
    public String deleteSocket(@PathVariable("id") String id) throws IOException
    {
        final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        final JsonGenerator g = json_factory.createGenerator(buf);
        
        for (final WebSocket socket : PVWebSocketContext.getSockets())
            if (id.equals(socket.getId()))
            {
                logger.log(Level.INFO, "DELETE socket '" + id + "'");
                socket.dispose();
                g.writeStartObject();
                g.writeBooleanField("success", true);
                g.writeStringField("message", "Socket " + id + " has been deleted");
                g.writeEndObject();
                g.flush();
                return buf.toString();
            }
        logger.log(Level.WARNING, "Cannot DELETE socket '" + id + "'");
        
        g.writeStartObject();
        g.writeBooleanField("success", false);
        g.writeStringField("message", "Cannot DELETE socket " + id);
        g.writeEndObject();
        g.flush();
        return buf.toString();
    }
}
