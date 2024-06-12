package pvwa;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.epics.vtype.VType;
import org.phoebus.pv.PV;

public final class Utility {
    public static final int TIMEOUT_IN_MS = 5000;
    private static final int TIMEOUT_FRAGMENT_IN_MS = 50;
    private static final int fragment; // Timeout
    private static final int number; // Iteration

    static
    {
        fragment = TIMEOUT_FRAGMENT_IN_MS; 
        number = TIMEOUT_IN_MS / TIMEOUT_FRAGMENT_IN_MS; 
    }

    public static VType basicRead(PV pv) throws InterruptedException
    {
        VType value = null;
        for (int i = 0; i < number; i++) {
            Thread.sleep(fragment);
            value = pv.read();
            if (value != null)
                break;
        }
        return value;
    }

    public static VType asyncRead(PV pv) throws InterruptedException
    {
        VType value = null;
        for (int i = 0; i < number; i++) {
            Thread.sleep(fragment);
            try {
                value = pv.asyncRead().get(TIMEOUT_IN_MS, TimeUnit.MILLISECONDS);
                break;
            } catch (Exception e) {
                value = null;
            }
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
