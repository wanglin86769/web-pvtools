package pvwa.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;

import static pvwa.PVWebSocketContext.json_factory;
import pvwa.Utility;


/**
 * Servlet to handle requests for fetching PV values.
 * This servlet supports both single and multiple PV requests via the "pv" query parameter.
 */
@CrossOrigin
@RestController
public class PVReadController
{
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Handles GET requests to /pvget and writes the response in JSON format.
     * 
     * The method supports two input formats:
     * - A single PV name passed directly via the "pv" parameter.
     * - Multiple PV names passed as a JSON array of strings via the "pv" parameter.
     * 
     * @param pvParam Optional request parameter that specifies the PV(s) for which data is requested.
     *                If not provided, an error response is returned indicating no PVs were specified.
     * @return ResponseEntity containing either:
     *         - A JSON object (for a single PV),
     *         - A JSON array of objects (for multiple PVs), or
     *         - An error message if parsing or processing fails.
     * @throws IOException if there's an issue writing the JSON response
     * @throws Exception   for any other unexpected errors during execution
     */
    @GetMapping(value = "/pvget", produces = "application/json")
    public ResponseEntity<?> pvGet(@RequestParam(required = false, name = "pv") String pvParam) throws IOException, Exception
    {      
        if (pvParam == null || pvParam.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "No PVs have been specified.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        List<String> pvs = new ArrayList<>();
        try {
            // Try parsing as JSON array
            JsonNode node = objectMapper.readTree(pvParam);
            if (node.isArray()) {
                for (JsonNode item : node) {
                    pvs.add(item.asText());
                }
            } else {
                // Not an array, assume it's a single PV name
                pvs.add(pvParam);
            }
        } catch (JsonProcessingException e) {
            // Invalid JSON, treat it as a single PV name
            pvs.add(pvParam);
        }

        final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        final JsonGenerator g = json_factory.createGenerator(buf);
        
        // Handle single PV request.
        if (pvs.size() == 1) {
            try {
                // Generate a JSON object for the single PV and write it directly to the response.
                String jsonObject = generateJsonForPV(pvs.get(0));
                g.writeRawValue(jsonObject); // Write raw JSON string
            } catch (Exception e) {
                // If an exception occurs, return an error response for the single PV.
                Map<String, Object> response = new HashMap<>();
                response.put("name", pvs.get(0));
                response.put("success", false);
                response.put("message", "An exception occurs when fetching or formatting PV value.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        } else {
            // Handle multiple PV requests using a thread pool for concurrent processing.
            int numberOfThreads = 5;
            ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);

            // Create a list to store tasks for generating JSON for each PV.
            List<PVTask> pvTasks = new ArrayList<>();

            // Submit tasks to the thread pool to generate JSON for each PV concurrently.
            for (String pv : pvs) {
                Future<String> future = executorService.submit(() -> generateJsonForPV(pv));
                pvTasks.add(new PVTask(pv, future));
            }

            g.writeStartArray();
            for (PVTask pvTask : pvTasks) {
                try {
                    // Retrieve the JSON result for the PV from the Future and write it to the response.
                    String jsonObject = pvTask.future.get(5000, TimeUnit.MILLISECONDS);
                    g.writeRawValue(jsonObject); // Write raw JSON string
                } catch (TimeoutException  e) {
                    g.writeStartObject();
                    g.writeStringField("name", pvTask.pvName);
                    g.writeBooleanField("success", false);
                    g.writeStringField("message", "Timeout while generating JSON for the PV.");
                    g.writeEndObject();
                } catch (InterruptedException | ExecutionException e) {
                    g.writeStartObject();
                    g.writeStringField("name", pvTask.pvName);
                    g.writeBooleanField("success", false);
                    g.writeStringField("message", "Exceptions occur when reading PV values.");
                    g.writeEndObject();
                }
            }
            g.writeEndArray();

            // Shutdown the thread pool to release resources.
            executorService.shutdown();
        }

        g.flush();
        return ResponseEntity.ok(buf.toString());
    }


    /**
     * Generates a JSON representation of the PV with the given name.
     *
     * @param name The name of the PV to generate the JSON for.
     * 
     * @return A JSON-formatted string representing the PV's metadata and value.
     *         The JSON includes information such as:
     *         - Success status and error messages (if applicable).
     *         - PV type, value type, and other metadata.
     *         - Timestamps (seconds and nanoseconds) if available.
     *         - Alarm information (severity, status, name) if applicable.
     *         - The actual value of the PV, which can be a scalar, array, or enum.
     * 
     * @throws Exception If there is an issue retrieving the PV, reading its value, or generating the JSON.
     */
    private String generateJsonForPV(String name) throws Exception {
        PV pv = PVPool.getPV(name);
        VType value = Utility.asyncRead(pv, 100, 50);

        final ByteArrayOutputStream buf = new ByteArrayOutputStream();
        final JsonGenerator g = json_factory.createGenerator(buf);

        g.writeStartObject();
        g.writeStringField("name", name);
           
        if (value == null) {
            g.writeBooleanField("success", false);
            g.writeStringField("message", "PV is not found");
        } else {
            g.writeBooleanField("success", true);
            g.writeStringField("message", "PV is read successfully");

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
        }
        
        g.writeEndObject();
        g.flush();
        return buf.toString();
    }


    // Helper class to associate a PV name with its Future
    private static class PVTask {
        String pvName;
        Future<String> future;

        PVTask(String pvName, Future<String> future) {
            this.pvName = pvName;
            this.future = future;
        }
    }
}
