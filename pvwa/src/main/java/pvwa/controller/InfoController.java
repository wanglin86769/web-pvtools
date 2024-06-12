/*
 *  Original Author: Kay Kasemir
 *  Current Author:  Lin Wang
 *
 *  Modification History
 *  2024-05-22 Replace Servlet with Spring Boot REST API
 *  2024-06-03 Use key and value to display environment variables
 *
 */
package pvwa.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonGenerator;

import org.phoebus.util.time.TimestampFormats;
import org.springframework.web.bind.annotation.CrossOrigin;

import static pvwa.PVWebSocketContext.json_factory;
import pvwa.PVWebSocketContext;


/** 
 *  REST API to provide server info
 */
@CrossOrigin
@RestController
public class InfoController
{
    @GetMapping(value = "/info", produces="application/json")
    public String getServerInfo(@RequestParam(required = false) String env) throws IOException
    {
        final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        final JsonGenerator g = json_factory.createGenerator(buf);
        
        g.writeStartObject();
        g.writeStringField("start_time", TimestampFormats.SECONDS_FORMAT.format(PVWebSocketContext.start_time));
        g.writeStringField("jre", System.getProperty("java.vendor") + " " + System.getProperty("java.version"));
        
        final boolean with_env = Boolean.parseBoolean(env);
        if (with_env)
        {
            g.writeArrayFieldStart("env");
            for (final Map.Entry<String, String> entry : System.getenv().entrySet())
            {
                g.writeStartObject();
//                g.writeStringField(entry.getKey(), entry.getValue());
                g.writeStringField("key", entry.getKey());
                g.writeStringField("value", entry.getValue());
                g.writeEndObject();
            }
            g.writeEndArray();
        }
        g.writeEndObject();
        
        g.flush();
        return buf.toString();
    }
}
