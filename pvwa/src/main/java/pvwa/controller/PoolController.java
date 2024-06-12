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

import org.phoebus.pv.PV;
import org.phoebus.pv.PVPool;
import org.phoebus.pv.RefCountMap.ReferencedEntry;
import org.springframework.web.bind.annotation.CrossOrigin;

import static pvwa.PVWebSocketContext.json_factory;


/** 
 *  REST API to list PV Pool entries
 */
@CrossOrigin
@RestController
public class PoolController
{
    @GetMapping(value = "/pool", produces="application/json")
    public String getPoolEntries() throws IOException
    {
        final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        final JsonGenerator g = json_factory.createGenerator(buf);
        
        g.writeStartArray();
        for (final ReferencedEntry<PV> ref : PVPool.getPVReferences())
        {
            g.writeStartObject();
            g.writeNumberField("refs", ref.getReferences());
            g.writeStringField("pv", ref.getEntry().getName());
            g.writeEndObject();
        }
        g.writeEndArray();
        
        g.flush();
        return buf.toString();
    }
}
