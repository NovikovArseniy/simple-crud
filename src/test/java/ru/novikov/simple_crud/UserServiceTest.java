package ru.novikov.simple_crud;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import ru.novikov.simple_crud.exceptions.UserIsAlreadyExistsException;
import ru.novikov.simple_crud.exceptions.UserNotFoundException;
import ru.novikov.simple_crud.model.User;
import ru.novikov.simple_crud.service.UserService;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class UserServiceTest {

    @Autowired
    UserService userService;

    @Test
    public void createOneHundredThousandsUsers(){
        AtomicInteger createdUsersCounter = new AtomicInteger();
        int numberOfCreations = 100000;
        CountDownLatch latch = new CountDownLatch(numberOfCreations);
        Runnable getUser = () -> {
            User user = createRandomUser();
            try {
                user = userService.createUser(user);
                if (user != null){
                    createdUsersCounter.incrementAndGet();
                }
            } catch (UserIsAlreadyExistsException e){
                System.out.println("Error!!!!!!!!!!!");
            }
            latch.countDown();
        };
        ExecutorService service = Executors.newFixedThreadPool(8);
        for (int i = 0; i < numberOfCreations; i++) {
            service.submit(getUser);
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Assertions.assertEquals(numberOfCreations, createdUsersCounter.get());
    }

    @Test
    public void doMultipleReadingsWithMultipleConnections(){
        for (int i = 0; i < 100000; i++) {
            User user = createRandomUser();
            try {
                userService.createUser(user);
            } catch (UserIsAlreadyExistsException e){
                System.out.println("Error!!!!!!!!!!!");
            }
        }
        List<Long> readingTimes = new ArrayList<>();
        AtomicInteger countReadings = new AtomicInteger();
        int expectedCountReadings = 1000000;
        CountDownLatch latch = new CountDownLatch(expectedCountReadings);
        int connectionNumber = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(connectionNumber);
        Runnable getRandomUserById = ()->{
            for (int i = 0; i < expectedCountReadings/connectionNumber; i++){
                User user = null;
                try {
                    long startTime = System.nanoTime();
                    user = userService.getUserById((long) (Math.random() * 100000 + 1));
                    long endTime = System.nanoTime();
                    long timeReading = endTime - startTime;
                    readingTimes.add(timeReading);
                } catch (UserNotFoundException e){
                    System.out.println("User not found");
                }
                if (user!= null){
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
        log.info("total time = " + (Long) readingTimes.stream().mapToLong(Long::longValue).sum() + " ns");
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
