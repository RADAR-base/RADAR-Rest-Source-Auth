package hello;

import java.util.Scanner;

import com.github.scribejava.apis.FitbitApi20;
import com.github.scribejava.apis.fitbit.FitBitOAuth2AccessToken;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@Slf4j
public class GreetingController {
    private static final String template = "Hello, %s!";
    private final AtomicLong counter = new AtomicLong();
    
    final String clientId = "22CN3H";
    final String clientSecret = "7ae0f73f89645d9ce11f852a10f707f4";
    final String callBack = "http://localhost:8080/callback";
    final String fitbitScopes = "activity profile";
    final OAuth20Service service = new ServiceBuilder(clientId)
                .apiSecret(clientSecret)
                .scope(fitbitScopes) // replace with desired scope
                //your callback URL to store and handle the authorization code sent by Fitbit
                .callback(callBack)
                .state("state_params")
                //will replace with RADAR participant id?
                .build(FitbitApi20.instance());

    @RequestMapping("/auth")
    String auth() {
        final String authorizationUrl = service.getAuthorizationUrl();
        return "<a href=\"" + authorizationUrl + "\">Authorize!</a><br/>";
    }

    @RequestMapping(path="/callback", method=RequestMethod.GET)
    String callback(@RequestParam("code") String code){
        final OAuth2AccessToken oauth2AccessToken = service.getAccessToken(code);
        final FitBitOAuth2AccessToken accessToken = (FitBitOAuth2AccessToken) oauth2AccessToken;
        return code;
    }

    @RequestMapping("/activityHistory")
    String activity(@RequestBody Object request) {

        final OAuthRequest request = new OAuthRequest(Verb.GET, String.format("https://api.fitbit.com/1/user/-/activities/steps/date/today/1m.json\n", accessToken.getUserId()));
        request.addHeader("x-li-format", "json");

        service.signRequest(accessToken, request);

        final Response response = service.execute(request);
        return "Hello from activity";
    }

    @PostMapping()
    public void count(@RequestBody Object request) {
        log.info("Request: '{}'", request);
    }
}