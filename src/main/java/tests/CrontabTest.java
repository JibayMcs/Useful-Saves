package tests;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * @author ZeAmateis
 */
public class CrontabTest {
    public static void main(String[] args) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String expression = "0 15,30,45 * ? * *";
        expression = expression.replaceAll("\\s", "+");

        System.out.println(expression);

        String fromattedURL = String.format("https://cronexpressiondescriptor.azurewebsites.net/api/descriptor/?expression=%s&locale=%s", expression, "en-US");

        System.out.println(fromattedURL);

        URL url = new URL(fromattedURL);

        JsonObject descriptor = gson.fromJson(new InputStreamReader(url.openStream()), new TypeToken<JsonObject>() {
        }.getType());

        System.out.println(descriptor.get("description").getAsString());

    }

}
