package pvwa;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.epics.vtype.VType;
import org.phoebus.pv.PV;

public final class Utility {
    public static final int TIMEOUT_IN_MS = 5000;

    public static VType basicRead(PV pv, int maxRetries, int retryDelayMs) throws InterruptedException
    {
        VType value = null;

        // Attempt to read the value from the PV, retrying up to maxRetries times
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            value = pv.read();
            if (value != null)
                break;
            Thread.sleep(retryDelayMs);
        }
        return value;
    }

    public static VType asyncRead(PV pv, int maxRetries, int retryDelayMs) throws InterruptedException
    {
        VType value = null;

        // Attempt to read the value from the PV, retrying up to maxRetries times
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                value = pv.asyncRead().get(TIMEOUT_IN_MS, TimeUnit.MILLISECONDS);
                // The value may be null for simulated PVs
                if (value != null)
                    break;
            } catch (Exception e) {
                value = null;
            }
            Thread.sleep(retryDelayMs);
        }
        return value;
    }

    public static void basicWrite(PV pv, Object new_value) throws Exception
    {
        pv.write(new_value);
    }
    
    public static void asyncWrite(PV pv, Object new_value) throws InterruptedException, ExecutionException, TimeoutException, Exception
    {
        pv.asyncWrite(new_value).get(TIMEOUT_IN_MS, TimeUnit.MILLISECONDS);
    }

}
