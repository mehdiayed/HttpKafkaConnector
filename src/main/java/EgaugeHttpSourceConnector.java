
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.source.SourceConnector;
import org.apache.kafka.connect.source.SourceTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EgaugeHttpSourceConnector extends SourceConnector {

    private Map<String, String> configProperties;

    @Override
    public void start(Map<String, String> props) {
        configProperties = props;
    }

    @Override
    public Class<? extends Task> taskClass() {
        return EgaugeHttpSourceTask.class;
    }

    @Override
    public List<Map<String, String>> taskConfigs(int maxTasks) {
        List<Map<String, String>> taskConfigs = new ArrayList<>(maxTasks);
        for (int i = 0; i < maxTasks; i++) {
            taskConfigs.add(new HashMap<>(configProperties));
        }
        return taskConfigs;
    }


    @Override
    public ConfigDef config() {
        // Define the configuration options for your connector here
        return new ConfigDef()
                .define("egauge.jwt.url", ConfigDef.Type.STRING, ConfigDef.Importance.HIGH, "The URL of the eGauge JWT server")
                .define("egauge.jwt.username", ConfigDef.Type.STRING, ConfigDef.Importance.HIGH, "The username for the eGauge JWT server")
                .define("egauge.jwt.password", ConfigDef.Type.STRING, ConfigDef.Importance.HIGH, "The password for the eGauge JWT server");
    }

/

    @Override
    public void stop() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public String version() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
}
