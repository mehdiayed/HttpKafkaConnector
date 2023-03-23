package tutorial.buildon.aws.streaming.kafka;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.data.SchemaBuilder;
import org.apache.kafka.connect.data.Struct;
import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import org.json.JSONObject;

import static tutorial.buildon.aws.streaming.kafka.MyFirstKafkaConnectorConfig.*;

public class MyFirstKafkaConnectorTask extends SourceTask {

    String egaugeUrl;
    String jwtToken;
    
    private static final String STRING_COLUMN = "string-column";
    private static final String NUMERIC_COLUMN = "numeric-column";
    private static final String BOOLEAN_COLUMN = "boolean-column";

    private final Random random = new Random(System.currentTimeMillis());
    private final Logger log = LoggerFactory.getLogger(MyFirstKafkaConnectorTask.class);

    private MyFirstKafkaConnectorConfig config;
    private int taskSleepTimeout;
    private List<String> sources;
    private Schema recordSchema;

    @Override
    public String version() {
        return PropertiesUtil.getConnectorVersion();
    }

    @Override
    public void start(Map<String, String> properties) {
        config = new MyFirstKafkaConnectorConfig(properties);
        taskSleepTimeout = config.getInt(TASK_SLEEP_TIMEOUT_CONFIG);
        String sourcesStr = properties.get("sources");
        sources = Arrays.asList(sourcesStr.split(","));
        recordSchema = SchemaBuilder.struct()
                .field(STRING_COLUMN, Schema.STRING_SCHEMA).required()
                .field(NUMERIC_COLUMN, Schema.INT32_SCHEMA).required()
                .field(BOOLEAN_COLUMN, Schema.OPTIONAL_BOOLEAN_SCHEMA)
                .build();

        String jwtUrl = properties.get("egauge.jwt.url");
        String jwtUsername = properties.get("egauge.jwt.username");
        String jwtPassword = properties.get("egauge.jwt.password");

        // send a POST request to the JWT server to obtain a JWT token
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(jwtUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("grant_type=password&username=" + jwtUsername + "&password=" + jwtPassword))
                .build();
        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        // Extract the JWT token from the response body
        String responseBody = response.body();
        JSONObject responseJson = new JSONObject(responseBody);
        String jwtToken = responseJson.getString("access_token");
    }

    @Override
    public List<SourceRecord> poll() throws InterruptedException {
        Thread.sleep(taskSleepTimeout);
        List<SourceRecord> records = new ArrayList<>();
        for (String source : sources) {
            log.info("Polling data from the source '" + source + "'");
            records.add(new SourceRecord(
                    Collections.singletonMap("source", source),
                    Collections.singletonMap("offset", 0),
                    source, null, null, null,
                    recordSchema, createStruct(recordSchema)));
        }
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(egaugeUrl + "/cgi-bin/egauge-show?m"))
                .header("Authorization", "Bearer " + jwtToken)
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            JSONObject dataJson = new JSONObject(response.body());
            double power = dataJson.getDouble("power");
            long timestamp = Instant.now().getEpochSecond();
            SourceRecord record = new SourceRecord(
                    Collections.singletonMap("egauge-data", "egauge-power-usage"),
                    null,
                    null,
                    null,
                    null,
                    null,
                    String.valueOf(power).getBytes(),
                    timestamp
            );
            return Collections.singletonList(record);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return records;
    }

    private Struct createStruct(Schema schema) {
        Struct struct = new Struct(schema);
        struct.put(STRING_COLUMN, randomString());
        struct.put(NUMERIC_COLUMN, random.nextInt(1000));
        struct.put(BOOLEAN_COLUMN, random.nextBoolean());
        return struct;
    }

    private String randomString() {
        int leftLimit = 48;
        int rightLimit = 122;
        int targetStringLength = 10;
        return random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    @Override
    public void stop() {
    }

}
