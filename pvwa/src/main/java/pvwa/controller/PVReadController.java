package pvwa.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonGenerator;
import java.util.List;
import org.epics.util.array.ListNumber;

import org.epics.vtype.Array;
import org.epics.vtype.Scalar;
import org.epics.vtype.Time;
import org.epics.vtype.VByteArray;
import org.epics.vtype.VDouble;
import org.epics.vtype.VDoubleArray;
import org.epics.vtype.VEnum;
import org.epics.vtype.VFloat;
import org.epics.vtype.VFloatArray;
import org.epics.vtype.VInt;
import org.epics.vtype.VIntArray;
import org.epics.vtype.VNumber;
import org.epics.vtype.VNumberArray;
import org.epics.vtype.VShortArray;
import org.epics.vtype.VString;
import org.epics.vtype.VStringArray;
import org.epics.vtype.VType;
import org.phoebus.pv.PV;
import org.phoebus.pv.PVPool;
import org.springframework.web.bind.annotation.CrossOrigin;

import static pvwa.PVWebSocketContext.json_factory;
import pvwa.Utility;


/** 
 *  REST API to execute pvget
 */
@CrossOrigin
@RestController
public class PVReadController
{
    @GetMapping(value = "/pvget", produces="application/json")
    public String pvGet(@RequestParam(required = true, name = "pv") String name) throws IOException, Exception
    {      
        final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        final JsonGenerator g = json_factory.createGenerator(buf);
        g.writeStartObject();
        g.writeStringField("name", name);
        
        PV pv = PVPool.getPV(name);
        VType value = Utility.asyncRead(pv);
           
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
        
        final Time time = Time.timeOf(value);
        if (time != null)
        {
            g.writeNumberField("seconds", time.getTimestamp().getEpochSecond());
            g.writeNumberField("nanos", time.getTimestamp().getNano());
        }
        
        if (value instanceof Scalar) {
            g.writeStringField("alarm", ((Scalar) value).getAlarm().toString());
            g.writeStringField("alarm_severity", ((Scalar) value).getAlarm().getSeverity().toString());
            g.writeStringField("alarm_status", ((Scalar) value).getAlarm().getStatus().toString());
            g.writeStringField("alarm_name", ((Scalar) value).getAlarm().getName());
        } else if (value instanceof VNumberArray) {
            g.writeStringField("alarm", ((VNumberArray) value).getAlarm().toString());
            g.writeStringField("alarm_severity", ((VNumberArray) value).getAlarm().getSeverity().toString());
            g.writeStringField("alarm_status", ((VNumberArray) value).getAlarm().getStatus().toString());
            g.writeStringField("alarm_name", ((VNumberArray) value).getAlarm().getName());
        } else if (value instanceof VStringArray) {
            g.writeStringField("alarm", ((VStringArray) value).getAlarm().toString());
            g.writeStringField("alarm_severity", ((VStringArray) value).getAlarm().getSeverity().toString());
            g.writeStringField("alarm_status", ((VStringArray) value).getAlarm().getStatus().toString());
            g.writeStringField("alarm_name", ((VStringArray) value).getAlarm().getName());
        }
        
        if (value instanceof VInt) {
            g.writeNumberField("value", ((VNumber) value).getValue().intValue());
        } else if (value instanceof VFloat) {
            g.writeNumberField("value", ((VNumber) value).getValue().floatValue());
        } else if (value instanceof VDouble) {
            g.writeNumberField("value", ((VNumber) value).getValue().doubleValue());
        } else if (value instanceof VString) {
            g.writeStringField("text", ((VString) value).getValue());
        } else if (value instanceof VEnum) {
            g.writeArrayFieldStart("labels");
            for (final String label : ((VEnum) value).getDisplay().getChoices())
                g.writeString(label);
            g.writeEndArray();
            g.writeNumberField("value",  ((VEnum) value).getIndex());
            g.writeStringField("text",  ((VEnum) value).getValue());
        } else if (value instanceof VByteArray) {
            final ListNumber data = ((VNumberArray) value).getData();
            final int N = data.size();
            g.writeArrayFieldStart("value");
            for (int i=0; i<N; ++i)
                g.writeNumber(data.getByte(i));
            g.writeEndArray();
        } else if (value instanceof VShortArray) {
            final ListNumber data = ((VNumberArray) value).getData();
            final int N = data.size();
            g.writeArrayFieldStart("value");
            for (int i=0; i<N; ++i)
                g.writeNumber(data.getShort(i));
            g.writeEndArray();
        } else if (value instanceof VIntArray) {
            final ListNumber data = ((VNumberArray) value).getData();
            final int N = data.size();
            g.writeArrayFieldStart("value");
            for (int i=0; i<N; ++i)
                g.writeNumber(data.getInt(i));
            g.writeEndArray();
        } else if (value instanceof VFloatArray) {
            final ListNumber data = ((VNumberArray) value).getData();
            final int N = data.size();
            g.writeArrayFieldStart("value");
            for (int i=0; i<N; ++i)
                g.writeNumber(data.getFloat(i));
            g.writeEndArray();
        } else if (value instanceof VDoubleArray) {
            final ListNumber data = ((VNumberArray) value).getData();
            final int N = data.size();
            g.writeArrayFieldStart("value");
            for (int i=0; i<N; ++i)
                g.writeNumber(data.getDouble(i));
            g.writeEndArray();
        } else if (value instanceof VStringArray) {
            final List<String> data = ((VStringArray) value).getData();
            final int N = data.size();
            g.writeArrayFieldStart("value");
            for (int i=0; i<N; ++i)
                g.writeString(data.get(i));
            g.writeEndArray();
        } else {
            g.writeStringField("text", value.toString());
        }
        
        g.writeEndObject();
        g.flush();
        return buf.toString();
    }
}
