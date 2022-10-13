package com.tws.enigmafeeder;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.json.BasicJsonTester;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class EnigmafeederApplicationTests {

    private final BasicJsonTester json = new BasicJsonTester(getClass());

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;
    @Autowired
    private RandomizerController randomizerController;

    @Test
    void contextLoads() throws Exception {
        assertThat(randomizerController).isNotNull();
    }

    @Test
    public void checkRange() throws Exception {
        int howmany = 15;
        int min = 240;
        int max = 1000;
        String url = String.format("http://localhost:%d/randomizer?howmany=%d&min=%d&max=%d", port, howmany, min, max);
        String response=this.restTemplate.getForObject(url, String.class);        
        
        assertThat(json.from(response)).hasJsonPathArrayValue("numbers");
        assertThat(json.from(response)).extractingJsonPathArrayValue("numbers")
            .isNotEmpty()
            .hasSize(howmany)
            .allMatch(i -> (int)i >= min)
            .allMatch(i -> (int)i <= max);            
    }


    @Test
    public void checkReversedRangeError() throws Exception {
        int howmany = 5;
        int min = 100;
        int max = 50;
        String url = String.format("http://localhost:%d/randomizer?howmany=%d&min=%d&max=%d", port, howmany, min, max);
        String response=this.restTemplate.getForObject(url, String.class);        
        assertThat(response).isEqualTo("bound must be greater than origin");
    }


    @Test
    public void checkParametersNotFound() throws Exception {
        String url = String.format("http://localhost:%d/randomizer?min=1&max=10", port);
        String response=this.restTemplate.getForObject(url, String.class);
        assertThat(response).isEqualTo("Required request parameter 'howmany' for method parameter type long is not present");

        url = String.format("http://localhost:%d/randomizer?howmany=5&max=10", port);
        response=this.restTemplate.getForObject(url, String.class);
        assertThat(response).isEqualTo("Required request parameter 'min' for method parameter type long is not present");

        url = String.format("http://localhost:%d/randomizer?howmany=5&min=1", port);
        response=this.restTemplate.getForObject(url, String.class);
        assertThat(response).isEqualTo("Required request parameter 'max' for method parameter type long is not present");

    }
    
}
