package support;

        import com.mashape.unirest.http.HttpResponse;
        import com.mashape.unirest.http.JsonNode;
        import com.mashape.unirest.http.Unirest;
        import com.mashape.unirest.http.exceptions.UnirestException;
        import com.mashape.unirest.request.GetRequest;
        import com.mashape.unirest.request.HttpRequestWithBody;
        import com.mashape.unirest.request.body.RequestBodyEntity;
        import org.json.JSONArray;
        import org.json.JSONObject;

        import java.text.SimpleDateFormat;
        import java.util.Date;
        import java.util.HashMap;
        import java.util.Set;

        import static org.assertj.core.api.Assertions.assertThat;
        import static support.TestContext.getJsonTestData;

public class SlavaRestWrapper {

    private String baseUrl = "https://skryabin.com/recruit/api/v1/";
    //private String baseUrl = "http://localhost:8090/recruit/api/v1/";
    private static String loginToken; //added static to make it accessible in all instances of class, across whole class

    private final String CONTENT_TYPE = "Content-Type";
    private final String JSON = "application/json";
    private final String TOKEN = "x-access-token";
    public static final String POSITIONS = "positions";
    public static final String POSITION = "position";

    public SlavaRestWrapper login(HashMap<String, String> credentials) throws UnirestException {
        JSONObject payload = new JSONObject(credentials);
        RequestBodyEntity request = Unirest.post(baseUrl + "login")
                .header(CONTENT_TYPE, JSON)
                .body(payload);

        HttpResponse<JsonNode> response = request.asJson();

        assertThat(response.getStatus()).isEqualTo(200);

        JSONObject body = response.getBody().getObject();
        loginToken = body.getString("token");
        System.out.println("Login successful! Token: " + loginToken);
        return this;
    }

    public JSONObject createPosition(HashMap<String, String> position) throws UnirestException {
        String dateOpen = position.get("dateOpen");
        String dateOpenISO = new SimpleDateFormat("yyyy-MM-dd").format(new Date(dateOpen));
        // workaround for issue that always displays time
        dateOpenISO = dateOpenISO + "T05:00:00.000Z";
        position.put("dateOpen", dateOpenISO);

        JSONObject positionJson = new JSONObject(position);
        RequestBodyEntity request = Unirest.post(baseUrl + POSITIONS)
                .header(CONTENT_TYPE, JSON)
                .header(TOKEN, loginToken)
                .body(positionJson);

        HttpResponse<JsonNode> response = request.asJson();
        assertThat(response.getStatus()).isEqualTo(201);

        JSONObject responsePositionJson = response.getBody().getObject();
        System.out.println("\n\nPosition created: " + responsePositionJson);

        TestContext.setTestData(POSITION, responsePositionJson);
        return responsePositionJson; //not used anywhere?
    }

    public void deletePosition(int positionId) throws Exception {
        HttpRequestWithBody request = Unirest.delete(baseUrl + POSITIONS + "/" + positionId)
                .header(TOKEN, loginToken);

        HttpResponse<JsonNode> response = request.asJson();
        assertThat(response.getStatus()).isEqualTo(204);
        System.out.println("\n\nSuccessfully deleted position id: " + positionId);

        GetRequest requestAfterDel = Unirest.get(baseUrl + POSITIONS + "/" + positionId);

        HttpResponse<JsonNode> responseAfterDel = requestAfterDel.asJson();
        assertThat(responseAfterDel.getStatus()).isEqualTo(400);

        JSONObject positionJson = responseAfterDel.getBody().getObject();
        assertThat(positionJson.get("errorMessage").equals("Incorrect positionId: " + positionId)).isTrue();
    }

    public JSONArray getPositions() throws Exception {
        GetRequest request = Unirest.get(baseUrl + POSITIONS);

        HttpResponse<JsonNode> response = request.asJson();

        assertThat(response.getStatus()).isBetween(200, 204);
        JSONArray positionsJson = response.getBody().getArray();
        return positionsJson;
    }

    public JSONObject updatePosition(HashMap<String, String> fields, int positionId) throws Exception {
        JSONObject fieldsJson = new JSONObject(fields);
        RequestBodyEntity request = Unirest.put(baseUrl + POSITIONS + "/" + positionId)
                .header(CONTENT_TYPE, JSON)
                .header(TOKEN, loginToken)
                .body(fieldsJson);

        HttpResponse<JsonNode> response = request.asJson();
        assertThat(response.getStatus()).isEqualTo(200);

        JSONObject responseFieldsJson = response.getBody().getObject();
        System.out.println("\n\nPosition " + positionId + " is updated: " + responseFieldsJson);
        return responseFieldsJson;
    }

    public JSONObject getPositionById(int positionId) throws Exception {
        GetRequest request = Unirest.get(baseUrl + POSITIONS + "/" + positionId);

        HttpResponse<JsonNode> response = request.asJson();
        assertThat(response.getStatus()).isEqualTo(200);

        JSONObject positionJson = response.getBody().getObject();
        System.out.println("Returned position: " + positionJson);
        return positionJson;
    }

    public void verifyPositionDetails() throws Exception {
        JSONObject expectedPosition = getJsonTestData(SlavaRestWrapper.POSITION);
        int positionId = expectedPosition.getInt("id");
        JSONObject actualPosition = new SlavaRestWrapper().getPositionById(positionId);

        Set<String> keys = expectedPosition.keySet();
        for (String key: keys) {
            //System.out.println("\n Actual: " + actualPosition.get(key));
            //System.out.println("\n Expected: " + expectedPosition.get(key));
            assertThat(actualPosition.get(key).equals(expectedPosition.get(key))).isTrue();
        }
    }
}
