package ru.novikov.simple_crud;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import ru.novikov.simple_crud.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
public class UserControllerTest {
    @LocalServerPort
    int randomServerPort;
    @Autowired TestRestTemplate restTemplate;
    @Test
    public void createOneHundredThousandsUsers(){
        int numberOfCreations = 100000;
        AtomicInteger counterOfCreations= new AtomicInteger();
        CountDownLatch latch = new CountDownLatch(numberOfCreations);
        Runnable getUser = () -> {
            User user = createRandomUser();
            ResponseEntity<User> response = getUserResponseEntity(user);
            if(response.getStatusCode() == HttpStatus.OK){
                counterOfCreations.incrementAndGet();
            }
            latch.countDown();
        };
        ExecutorService executorService = Executors.newFixedThreadPool(8);

        for (int i = 0; i < numberOfCreations; i++) {
            executorService.submit(getUser);
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Assertions.assertEquals(numberOfCreations, counterOfCreations.get());
    }

    @Test
    public void doMultipleReadingsWithMultipleConnections(){
        for (int i = 0; i < 100000; i++) {
            User user = createRandomUser();
                getUserResponseEntity(user);
        }
        List<Long> readingTimes = new ArrayList<>();
        AtomicInteger countReadings = new AtomicInteger();
        int expectedCountReadings = 1000000;
        CountDownLatch latch = new CountDownLatch(expectedCountReadings);
        int connectionNumber = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(connectionNumber);
        Runnable getRandomUserById = ()->{
            for (int i = 0; i < expectedCountReadings/connectionNumber; i++){
                long startTime = System.nanoTime();
                long randomId = (long) (Math.random() * 100000 + 1);
                ResponseEntity<User> response = restTemplate.getForEntity("http://localhost:" + randomServerPort + "/api/getUser?id=" + randomId, User.class);
                long endTime = System.nanoTime();
                long timeReading = endTime - startTime;
                readingTimes.add(timeReading);
                if (response.getStatusCode() == HttpStatus.OK){
                    countReadings.incrementAndGet();
                }
                latch.countDown();
            }
        };

        for (int i = 0; i < connectionNumber; i++) {
            executorService.execute(getRandomUserById);
        }

        try {
            latch.await();
            executorService.shutdown();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        Assertions.assertEquals(expectedCountReadings, countReadings.get());
        log.info("total time = " + readingTimes.stream().mapToLong(Long::longValue).sum() + " ns");
        log.info("median time = " + readingTimes.stream().sorted().toList().get(readingTimes.size()/2)+ " ns");
        int lastElem = readingTimes.size() * 99 / 100;
        log.info("99s percentile = " + readingTimes.stream()
                .sorted()
                .toList()
                .subList(0, lastElem)
                .get(lastElem - 1)+ " ns");
        lastElem = readingTimes.size() * 95 / 100;
        log.info("95s percentile = " + readingTimes.stream()
                .sorted()
                .toList()
                .subList(0, lastElem)
                .get(lastElem - 1)+ " ns");
    }
    private ResponseEntity<User> getUserResponseEntity(User user) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        HttpEntity<User> request = new HttpEntity<>(user, headers);
        ResponseEntity<User> response = restTemplate.postForEntity("http://localhost:" + randomServerPort + "/api/createUser", request, User.class);
        return response;
    }

    private User createRandomUser() {
        User user = new User();
        user.setName(generateRandomString());
        user.setSurname(generateRandomString());
        user.setAge((int) (Math.random() * 100));
        return user;
    }

    public String generateRandomString(){
        int leftLimit = 97; // letter 'a'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 10;
        Random random = new Random();

        return random.ints(leftLimit, rightLimit + 1)
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }
}
