package controllers;

import org.junit.Test;
import play.Application;
import play.api.test.CSRFTokenHelper;
import play.inject.guice.GuiceApplicationBuilder;
import play.mvc.Http;
import play.mvc.Result;
import play.test.WithApplication;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static play.mvc.Http.Status.OK;
import static play.test.Helpers.*;

public class HomeControllerTest extends WithApplication {

    @Override
    protected Application provideApplication() {
        return new GuiceApplicationBuilder().build();
    }

    @Test
    public void testIndex() {
        Http.RequestBuilder request = new Http.RequestBuilder()
                .method(GET)
                .uri("/");

        Result result = route(app, request);
        assertEquals(OK, result.status());
    }

    @Test
    public void testIndexPostWithValidData() {
        // Test for POST request with valid data
        Map<String, String[]> formData = new HashMap<>();
        formData.put("search_terms", new String[]{"example term"});

        Http.RequestBuilder request = new Http.RequestBuilder()
                .method(POST)
                .uri("/")
                .bodyFormArrayValues(formData);

        CSRFTokenHelper.addCSRFToken(request);
        Result result = route(app, request);

        assertEquals(OK, result.status());
    }

    @Test
    public void testIndexPostWithMissingData() {
        // Test for POST request with missing form data
        Http.RequestBuilder request = new Http.RequestBuilder()
                .method(POST)
                .uri("/")
                .bodyForm(new HashMap<>()); // Empty form data

        CSRFTokenHelper.addCSRFToken(request);
        Result result = route(app, request);
        assertEquals(OK, result.status());
        // Verify it returns an empty list or specific handling message as per application design
    }

//    @Test
//    public void testIndexWithUnsupportedMethod() { // TODO
//        // Test for unsupported HTTP method (e.g., PUT)
//        Http.RequestBuilder request = new Http.RequestBuilder()
//                .method(PUT)
//                .uri("/");
//
//        Result result = route(app, request);
//        assertEquals(BAD_REQUEST, result.status());
//        // Check the error message if necessary
//    }

}
