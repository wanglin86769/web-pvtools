package pvwa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

@ServletComponentScan /* Enable @WebListener annotation */
@SpringBootApplication
public class PvwaApplication {

    public static void main(String[] args) {
        SpringApplication.run(PvwaApplication.class, args);
    }

}
