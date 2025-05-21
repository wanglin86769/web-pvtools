package pvwa.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonGenerator;

import org.epics.vtype.Array;
import org.epics.vtype.Scalar;
import org.epics.vtype.VByteArray;
import org.epics.vtype.VDouble;
import org.epics.vtype.VDoubleArray;
import org.epics.vtype.VEnum;
import org.epics.vtype.VFloat;
import org.epics.vtype.VFloatArray;
import org.epics.vtype.VInt;
import org.epics.vtype.VIntArray;
import org.epics.vtype.VShortArray;
import org.epics.vtype.VString;
import org.epics.vtype.VStringArray;
import org.epics.vtype.VType;
import org.phoebus.pv.PV;
import org.phoebus.pv.PVPool;
import org.springframework.web.bind.annotation.CrossOrigin;

import static pvwa.PVWebSocketContext.json_factory;
import pvwa.Utility;
import static pvwa.ws.Vtype2Json.handleDisplay;


/** 
 *  REST API to execute pvinfo
 */
@CrossOrigin
@RestController
public class PVInfoController
{   
    @GetMapping(value = "/pvinfo", produces="application/json")
    public String pvInfo(@RequestParam(required = true, name = "pv") String name) throws IOException, Exception
    {        
        final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        final JsonGenerator g = json_factory.createGenerator(buf);
        g.writeStartObject();
        g.writeStringField("name", name);
        
        PV pv = PVPool.getPV(name);
        VType value = Utility.basicRead(pv, 100, 50);
                
        if (value == null) {
            g.writeBooleanField("success", false);
            g.writeStringField("message", "PV " + name + " is not available");
            g.writeEndObject();
            g.flush();
            return buf.toString();
        }
        if (PV.isDisconnected(value)) {
            g.writeBooleanField("success", false);
            g.writeStringField("message", "PV " + name + " is disconnected");
            g.writeEndObject();
            g.flush();
            return buf.toString();
        }
        
        g.writeBooleanField("success", true);
        g.writeStringField("pv_type", pv.getClass().getSimpleName());
        g.writeStringField("vtype", VType.typeOf(value).getSimpleName());
        g.writeBooleanField("scalar", value instanceof Scalar);
        g.writeBooleanField("array", value instanceof Array);
        g.writeNumberField("size", value instanceof Array ? ((Array) value).getSizes().getInt(0) : 1);
        g.writeBooleanField("readonly", pv.isReadonly());
        
        if (value instanceof VInt) {
            handleDisplay(g, ((VInt) value).getDisplay());
        } else if (value instanceof VFloat) {
            handleDisplay(g, ((VFloat) value).getDisplay());
        } else if (value instanceof VDouble) {
            handleDisplay(g, ((VDouble) value).getDisplay());
        } else if (value instanceof VString) {
            
        } else if (value instanceof VEnum) {
            
        } else if (value instanceof VByteArray) {
            handleDisplay(g, ((VByteArray) value).getDisplay());
        } else if (value instanceof VShortArray) {
            handleDisplay(g, ((VShortArray) value).getDisplay());
        } else if (value instanceof VIntArray) {
            handleDisplay(g, ((VIntArray) value).getDisplay());
        } else if (value instanceof VFloatArray) {
            handleDisplay(g, ((VFloatArray) value).getDisplay());
        } else if (value instanceof VDoubleArray) {
            handleDisplay(g, ((VDoubleArray) value).getDisplay());
        } else if (value instanceof VStringArray) {
            
        } else {
            /* Other data types */
        }
        
        g.writeEndObject();
        g.flush();
        return buf.toString();
    }
}
