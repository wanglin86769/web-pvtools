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

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonGenerator;

import org.epics.vtype.VType;
import org.epics.vtype.Array;
import org.epics.util.array.ListInteger;
import org.springframework.web.bind.annotation.CrossOrigin;

import pvwa.PVWebSocketContext;
import static pvwa.PVWebSocketContext.json_factory;
import pvwa.ws.WebSocket;
import pvwa.ws.WebSocketPV;


/** 
 *  REST API to list Web Sockets and summarize their PVs
 */
@CrossOrigin
@RestController
public class SummaryController
{
    @GetMapping(value = "/summary", produces="application/json")
    public String getSummary() throws IOException
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

            int pvs = 0, arrays = 0, max_size = 0;
            for (final WebSocketPV pv : socket.getPVs())
            {
                ++pvs;
                final VType value = pv.getLastValue();
                if (value instanceof Array)
                {
                    ++arrays;
                    final ListInteger sizes = ((Array) value).getSizes();
                    for (int i=0; i<sizes.size(); ++i) 
                        max_size = Math.max(max_size,  sizes.getInt(i));
                }
            }
            g.writeNumberField("pvs", pvs);
            g.writeNumberField("arrays", arrays);
            g.writeNumberField("max_size", max_size);

            g.writeEndObject();
        }
        g.writeEndArray();
        g.writeEndObject();
        
        g.flush();
        return buf.toString();
    }
}
