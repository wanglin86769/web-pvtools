package pvwa.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.concurrent.TimeoutException;
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
import org.springframework.web.bind.annotation.RequestBody;

import static pvwa.PVWebSocketContext.json_factory;
import pvwa.Utility;


/** 
 *  REST API to execute pvput
 */
@CrossOrigin
@RestController
public class PVWriteController
{   
    @PutMapping(value = "/pvput", produces="application/json")
    public String pvPut(@RequestBody JsonNode payload) throws IOException, Exception
    {
        JsonNode pvNode = payload.path("pv");
        if (pvNode.isMissingNode())
            throw new Exception("Missing 'pv' in " + payload);
        final String name = pvNode.asText();
        
        JsonNode valueNode = payload.path("value");
        if (valueNode.isMissingNode())
            throw new Exception("Missing 'value' in " + payload);
                
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
        
        if (value instanceof Array && !valueNode.isArray()) {
            throw new Exception("Array PV is getting scalar value");
        }
        
        if (value instanceof Scalar && valueNode.isArray()) {
            throw new Exception("Scalar PV is getting array value");
        }
        
        g.writeStringField("pv_type", pv.getClass().getSimpleName());
        g.writeStringField("vtype", VType.typeOf(value).getSimpleName());
        g.writeBooleanField("scalar", value instanceof Scalar);
        g.writeBooleanField("array", value instanceof Array);
        g.writeNumberField("size", value instanceof Array ? ((Array) value).getSizes().getInt(0) : 1);
        g.writeBooleanField("readonly", pv.isReadonly());
        
        if (value instanceof VEnum) {
            g.writeArrayFieldStart("labels");
            for (final String label : ((VEnum) value).getDisplay().getChoices())
                g.writeString(label);
            g.writeEndArray();
        }
        
        final Time time = Time.timeOf(value);
        if (time != null)
        {
            g.writeNumberField("seconds", time.getTimestamp().getEpochSecond());
            g.writeNumberField("nanos", time.getTimestamp().getNano());
        }
        
        handleAlarm(g, value, "old_");
        handleValue(g, value, "old_");
        
        try {
            write(pv, value, valueNode);
        } catch (InterruptedException ex) {
            g.writeBooleanField("success", false);
            g.writeStringField("message", "Interrupted while writing to " + name);
            g.writeEndObject();
            g.flush();
            return buf.toString();
        } catch (Exception ex) {
            if (ex instanceof TimeoutException) {
                g.writeStringField("message", "Completion timeout while writing to " + name);
            } else {
                g.writeStringField("message", "Failed to write to " + name);
            }
            g.writeBooleanField("success", false);
            g.writeEndObject();
            g.flush();
            return buf.toString();
        }
        
        VType newValue;
        newValue = Utility.basicRead(pv);
        
        if (newValue == null) {
            g.writeBooleanField("success", false);
            g.writeStringField("message", "PV " + name + " is not available after writing value");
            g.writeEndObject();
            g.flush();
            return buf.toString();
        }
        if (PV.isDisconnected(newValue)) {
            g.writeBooleanField("success", false);
            g.writeStringField("message", "PV " + name + " is disconnected after writing value");
            g.writeEndObject();
            g.flush();
            return buf.toString();
        }
        
        handleAlarm(g, newValue, "new_");
        handleValue(g, newValue, "new_");
        
        g.writeBooleanField("success", true);
        
        g.writeEndObject();
        g.flush();
        return buf.toString();
    }
    
    
    private void handleAlarm(JsonGenerator g, VType value, String prefix) throws IOException
    {
        if(value == null) return;
        
        if (value instanceof Scalar) {
            g.writeStringField(prefix + "alarm", ((Scalar) value).getAlarm().toString());
            g.writeStringField(prefix + "alarm_severity", ((Scalar) value).getAlarm().getSeverity().toString());
            g.writeStringField(prefix + "alarm_status", ((Scalar) value).getAlarm().getStatus().toString());
            g.writeStringField(prefix + "alarm_name", ((Scalar) value).getAlarm().getName());
        } else if (value instanceof VNumberArray) {
            g.writeStringField(prefix + "alarm", ((VNumberArray) value).getAlarm().toString());
            g.writeStringField(prefix + "alarm_severity", ((VNumberArray) value).getAlarm().getSeverity().toString());
            g.writeStringField(prefix + "alarm_status", ((VNumberArray) value).getAlarm().getStatus().toString());
            g.writeStringField(prefix + "alarm_name", ((VNumberArray) value).getAlarm().getName());
        } else if (value instanceof VStringArray) {
            g.writeStringField(prefix + "alarm", ((VStringArray) value).getAlarm().toString());
            g.writeStringField(prefix + "alarm_severity", ((VStringArray) value).getAlarm().getSeverity().toString());
            g.writeStringField(prefix + "alarm_status", ((VStringArray) value).getAlarm().getStatus().toString());
            g.writeStringField(prefix + "alarm_name", ((VStringArray) value).getAlarm().getName());
        }
    }
    
    
    private void handleValue(JsonGenerator g, VType value, String prefix) throws IOException
    {
        if(value == null) return;
        
        if (value instanceof VInt) {
            g.writeNumberField(prefix + "value", ((VNumber) value).getValue().intValue());
        } else if (value instanceof VFloat) {
             g.writeNumberField(prefix + "value", ((VNumber) value).getValue().floatValue());
        } else if (value instanceof VDouble) {
             g.writeNumberField(prefix + "value", ((VNumber) value).getValue().doubleValue());
        } else if (value instanceof VString) {
            g.writeStringField(prefix + "text", ((VString) value).getValue());
        } else if (value instanceof VEnum) {
            g.writeNumberField(prefix + "value",  ((VEnum) value).getIndex());
            g.writeStringField(prefix + "text",  ((VEnum) value).getValue());
        } else if (value instanceof VByteArray) {
            final ListNumber data = ((VNumberArray) value).getData();
            final int N = data.size();
            g.writeArrayFieldStart(prefix + "value");
            for (int i=0; i<N; ++i)
                g.writeNumber(data.getByte(i));
            g.writeEndArray();
        } else if (value instanceof VShortArray) {
            final ListNumber data = ((VNumberArray) value).getData();
            final int N = data.size();
            g.writeArrayFieldStart(prefix + "value");
            for (int i=0; i<N; ++i)
                g.writeNumber(data.getShort(i));
            g.writeEndArray();
        } else if (value instanceof VIntArray) {
            final ListNumber data = ((VNumberArray) value).getData();
            final int N = data.size();
            g.writeArrayFieldStart(prefix + "value");
            for (int i=0; i<N; ++i)
                g.writeNumber(data.getInt(i));
            g.writeEndArray();
        } else if (value instanceof VFloatArray) {
            final ListNumber data = ((VNumberArray) value).getData();
            final int N = data.size();
            g.writeArrayFieldStart(prefix + "value");
            for (int i=0; i<N; ++i)
                g.writeNumber(data.getFloat(i));
            g.writeEndArray();
        } else if (value instanceof VDoubleArray) {
            final ListNumber data = ((VNumberArray) value).getData();
            final int N = data.size();
            g.writeArrayFieldStart(prefix + "value");
            for (int i=0; i<N; ++i)
                g.writeNumber(data.getDouble(i));
            g.writeEndArray();   
        } else if (value instanceof VStringArray) {
            final List<String> data = ((VStringArray) value).getData();
            final int N = data.size();
            g.writeArrayFieldStart(prefix + "value");
            for (int i=0; i<N; ++i)
                g.writeString(data.get(i));
            g.writeEndArray();
        } else {
            g.writeStringField(prefix + "text", value.toString());
        }
    }
    
    
    private void write(PV pv, VType type, JsonNode node) throws Exception {
        Object new_value = null;
        
        if (type instanceof VInt) {
            new_value = Integer.valueOf(node.asText());
        } else if (type instanceof VFloat) {
            new_value = Float.valueOf(node.asText());
        } else if (type instanceof VDouble) {
            new_value = Double.valueOf(node.asText());
        } else if (type instanceof VString) {
            new_value = node.asText();
        } else if (type instanceof VEnum) {
            new_value = Integer.valueOf(node.asText());
        } else if (type instanceof VByteArray) {
            byte[] byteArray = new byte[node.size()];
            for(int i = 0; i < node.size(); i++) {
                byteArray[i] = Byte.parseByte(node.get(i).asText());
            }
            new_value = byteArray;
        } else if (type instanceof VShortArray) {
            short[] shortArray = new short[node.size()];
            for(int i = 0; i < node.size(); i++) {
                shortArray[i] = Short.parseShort(node.get(i).asText());
            }
            new_value = shortArray;
        } else if (type instanceof VIntArray) {
            int[] intArray = new int[node.size()];
            for(int i = 0; i < node.size(); i++) {
                intArray[i] = Integer.parseInt(node.get(i).asText());
            }
            new_value = intArray;
        } else if (type instanceof VFloatArray) {
            float[] floatArray = new float[node.size()];
            for(int i = 0; i < node.size(); i++) {
                floatArray[i] = Float.parseFloat(node.get(i).asText());
            }
            new_value = floatArray;
        } else if (type instanceof VDoubleArray) {
            double[] doubleArray = new double[node.size()];
            for(int i = 0; i < node.size(); i++) {
                doubleArray[i] = Double.parseDouble(node.get(i).asText());
            }
            new_value = doubleArray;
        } else if (type instanceof VStringArray) {
            String[] stringArray = new String[node.size()];
            for(int i = 0; i < node.size(); i++) {
                stringArray[i] = node.get(i).asText();
            }
            new_value = stringArray;
        } else {
            /* Other data types */
            return;
        }
        
        Utility.asyncWrite(pv, new_value);
    }

}
